package com.example.agendei_pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agendei_pro.core.model.*
import com.example.agendei_pro.core.repository.*
import com.example.agendei_pro.ui.theme.AgendeiTheme
import com.example.agendei_pro.ui.theme.ThemeLuxury
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    object AuthenticatedNoSalon : AuthState()
    object AuthenticatedNoBindings : AuthState()
    data class TrialExpired(val salon: Salon) : AuthState()
    data class AuthenticatedWithSalon(val salon: Salon, val daysRemaining: Int) : AuthState()
    data class AuthenticatedClient(
        val bindings: List<UserBinding>,
        val latestSalon: Salon?,
        val completedCount: Int,
        val lastCompletedAppointment: Appointment?,
        val loyaltyState: LoyaltyState = LoyaltyState()
    ) : AuthState()
}

class MainViewModel(
    private val salonRepository: SalonRepository = SalonRepository(),
    private val clientRepository: ClientRepository = ClientRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository(),
    private val appointmentRepository: AppointmentRepository = AppointmentRepository()
) : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    private val _selectedTheme = MutableStateFlow<AgendeiTheme>(ThemeLuxury)
    val selectedTheme: StateFlow<AgendeiTheme> = _selectedTheme

    private val _clientAppointments = MutableStateFlow<List<Appointment>>(emptyList())
    val clientAppointments = _clientAppointments.asStateFlow()

    private val _pendingSalonAppointments = MutableStateFlow<List<Appointment>>(emptyList())
    val pendingSalonAppointments = _pendingSalonAppointments.asStateFlow()

    private val _subscriptionPriceBronze = MutableStateFlow("110,00")
    val subscriptionPriceBronze = _subscriptionPriceBronze.asStateFlow()

    private val _subscriptionPricePrata = MutableStateFlow("150,00")
    val subscriptionPricePrata = _subscriptionPricePrata.asStateFlow()

    private val _subscriptionPriceOuro = MutableStateFlow("200,00")
    val subscriptionPriceOuro = _subscriptionPriceOuro.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    private var salonListener: com.google.firebase.firestore.ListenerRegistration? = null

    private val _globalAnnouncement = MutableStateFlow<Announcement?>(null)
    val globalAnnouncement = _globalAnnouncement.asStateFlow()

    private val _salonAnnouncement = MutableStateFlow<Announcement?>(null)
    val salonAnnouncement = _salonAnnouncement.asStateFlow()

    private var announcementListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var salonAnnouncementListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
    }

    fun checkUserStatus(isProVersion: Boolean) {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user == null) {
                _authState.value = AuthState.Unauthenticated
            } else {
                fetchSubscriptionPrice()
                val profile = profileRepository.getProfile()
                if (profile == null) {
                    auth.signOut()
                    _userProfile.value = null
                    _authState.value = AuthState.Unauthenticated
                } else {
                    _userProfile.value = profile
                    
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            viewModelScope.launch {
                                profileRepository.updateFcmToken(task.result)
                            }
                        }
                    }

                    announcementListener?.remove()
                    val configDocName = if (isProVersion) "announcement_salons" else "announcement_clients"
                    announcementListener = db.collection("config").document(configDocName).addSnapshotListener { snapshot, error ->
                        if (error == null && snapshot != null) {
                            _globalAnnouncement.value = snapshot.toObject(Announcement::class.java)
                        } else {
                            _globalAnnouncement.value = null
                        }
                    }

                    if (isProVersion) {
                        FirebaseMessaging.getInstance().subscribeToTopic("salons")
                        FirebaseMessaging.getInstance().unsubscribeFromTopic("clients")
                        observeSalonStatus(user.uid)
                    } else {
                        FirebaseMessaging.getInstance().subscribeToTopic("clients")
                        FirebaseMessaging.getInstance().unsubscribeFromTopic("salons")
                        loadClientStatus(user.uid)
                    }
                }
            }
        }
    }

    private fun observeSalonStatus(userId: String) {
        salonListener?.remove()
        salonListener = db.collection("salons").document(userId).addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val salon = snapshot?.toObject(Salon::class.java)
            if (salon == null) {
                _authState.value = AuthState.AuthenticatedNoSalon
            } else {
                val daysRemaining = salonRepository.getRemainingTrialDays(salon)
                if (daysRemaining <= 0 && !salon.isSubscribed) {
                    _authState.value = AuthState.TrialExpired(salon)
                } else {
                    _authState.value = AuthState.AuthenticatedWithSalon(salon, daysRemaining)
                    loadSalonPendingAppointments(salon.id)
                }
            }
        }
    }

    private fun loadClientStatus(userId: String) {
        viewModelScope.launch {
            val bindings = clientRepository.getMyBindings()
            if (bindings.isEmpty()) {
                _authState.value = AuthState.AuthenticatedNoBindings
                salonAnnouncementListener?.remove()
                _salonAnnouncement.value = null
            } else {
                val updatedBindings = bindings.map { binding ->
                    val latestSalon = salonRepository.getSalonById(binding.salonId)
                    if (latestSalon != null) {
                        binding.copy(salonName = latestSalon.name, salonLogoUrl = latestSalon.logoUrl, salonLogoShape = latestSalon.logoShape)
                    } else binding
                }
                
                val firstBinding = updatedBindings.firstOrNull()
                var latestSalon: Salon? = null
                var completedCount = 0
                var lastCompletedAppt: Appointment? = null
                var loyaltyState = LoyaltyState()
                
                if (firstBinding != null) {
                    FirebaseMessaging.getInstance().subscribeToTopic("salon_${firstBinding.salonId}")
                    latestSalon = salonRepository.getSalonById(firstBinding.salonId)
                    val history = appointmentRepository.getClientHistory(userId)
                    val salonHistory = history.filter { it.salonId == firstBinding.salonId && it.status == "CONFIRMED" && it.loyaltyValidated }
                    
                    if (latestSalon != null) {
                        loyaltyState = latestSalon.calculateLoyaltyState(salonHistory)
                        completedCount = loyaltyState.currentCardStampsCount
                    } else {
                        completedCount = salonHistory.filter { !it.loyaltyRedeemed }.size
                    }

                    lastCompletedAppt = history
                        .filter { it.salonId == firstBinding.salonId && (it.status == "SERVED" || it.status == "CONFIRMED") }
                        .filter {
                            val apptDate = it.date
                            apptDate != null && apptDate.before(java.util.Date())
                        }
                        .maxByOrNull { it.date?.time ?: 0L }

                    salonAnnouncementListener?.remove()
                    salonAnnouncementListener = db.collection("salons").document(firstBinding.salonId)
                        .collection("promotions").document("active")
                        .addSnapshotListener { snapshot, error ->
                            if (error == null && snapshot != null) {
                                _salonAnnouncement.value = snapshot.toObject(Announcement::class.java)
                            } else {
                                _salonAnnouncement.value = null
                            }
                        }
                } else {
                    salonAnnouncementListener?.remove()
                    _salonAnnouncement.value = null
                }
                
                _authState.value = AuthState.AuthenticatedClient(
                    bindings = updatedBindings,
                    latestSalon = latestSalon,
                    completedCount = completedCount,
                    lastCompletedAppointment = lastCompletedAppt,
                    loyaltyState = loyaltyState
                )
                loadClientAppointments()
            }
        }
    }

    private fun loadClientAppointments() {
        viewModelScope.launch {
            appointmentRepository.getMyAppointments().collect { _clientAppointments.value = it }
        }
    }

    private fun loadSalonPendingAppointments(salonId: String) {
        viewModelScope.launch {
            appointmentRepository.getAllSalonAppointments(salonId).collect { list ->
                _pendingSalonAppointments.value = list.filter { it.status == "PENDING" }
            }
        }
    }

    fun setTheme(theme: AgendeiTheme) { _selectedTheme.value = theme }

    fun logout() {
        FirebaseMessaging.getInstance().unsubscribeFromTopic("salons")
        FirebaseMessaging.getInstance().unsubscribeFromTopic("clients")
        val state = _authState.value
        if (state is AuthState.AuthenticatedClient) {
            state.bindings.forEach {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("salon_${it.salonId}")
            }
        }
        announcementListener?.remove()
        salonAnnouncementListener?.remove()
        _globalAnnouncement.value = null
        _salonAnnouncement.value = null
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
        _userProfile.value = null
        _clientAppointments.value = emptyList()
        _pendingSalonAppointments.value = emptyList()
    }

    fun updateProfile(newName: String, newPhoneNumber: String) {
        val current = _userProfile.value ?: return
        viewModelScope.launch {
            val updated = current.copy(name = newName, phoneNumber = newPhoneNumber)
            if (profileRepository.saveProfile(updated).isSuccess) _userProfile.value = updated
        }
    }

    fun deleteUserAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            if (profileRepository.deleteAccount().isSuccess) {
                logout()
                onDeleted()
            }
        }
    }

    fun unlinkCurrentSalon(salonId: String) {
        viewModelScope.launch {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("salon_$salonId")
            salonAnnouncementListener?.remove()
            _salonAnnouncement.value = null
            if (clientRepository.unlinkSalon(salonId).isSuccess) checkUserStatus(false)
        }
    }

    fun linkNewSalon(code: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val salon = salonRepository.getSalonByCode(code)
            if (salon != null) {
                if (clientRepository.linkSalon(salon.id, salon.name, salon.code, salon.logoUrl, salon.logoShape).isSuccess) {
                    checkUserStatus(false)
                    onResult(true, null)
                } else onResult(false, "Erro ao vincular")
            } else onResult(false, "Código não encontrado")
        }
    }

    private val _uploadProgress = MutableStateFlow<Int?>(null)
    val uploadProgress: StateFlow<Int?> = _uploadProgress.asStateFlow()

    fun registerSalon(name: String, address: String, phone: String, segment: String) {
        viewModelScope.launch {
            if (salonRepository.registerSalon(name, address, phone, segment).isSuccess) checkUserStatus(true)
        }
    }

    fun updateSalonSettings(
        name: String,
        opening: String,
        closing: String,
        breakStart: String,
        breakEnd: String,
        days: List<Int>,
        autoAccept: Boolean,
        logoShape: String,
        segment: String,
        hasLoyalty: Boolean,
        loyaltyRequired: Int,
        loyaltyReward: String,
        autoValidateLoyalty: Boolean,
        loyaltyRedemptionDays: Int,
        slotInterval: Int,
        isIndividualized: Boolean,
        hasWaitingList: Boolean,
        minBookingDelayHours: Int,
        minCancelDelayHours: Int
    ) {
        val state = _authState.value as? AuthState.AuthenticatedWithSalon ?: return
        viewModelScope.launch {
            val result = salonRepository.updateSalonSettings(
                name, opening, closing, breakStart, breakEnd, days, autoAccept, logoShape, segment,
                hasLoyalty, loyaltyRequired, loyaltyReward, autoValidateLoyalty, loyaltyRedemptionDays,
                slotInterval, isIndividualized, hasWaitingList, minBookingDelayHours, minCancelDelayHours
            )
            if (result.isSuccess) {
                val updatedSalon = state.salon.copy(
                    name = name,
                    openingTime = opening, 
                    closingTime = closing, 
                    breakStart = breakStart,
                    breakEnd = breakEnd,
                    workingDays = days,
                    autoAccept = autoAccept,
                    logoShape = logoShape,
                    segment = segment,
                    hasLoyaltyProgram = hasLoyalty,
                    loyaltyRequiredServices = loyaltyRequired,
                    loyaltyRewardDescription = loyaltyReward,
                    autoValidateLoyalty = autoValidateLoyalty,
                    loyaltyRedemptionDays = loyaltyRedemptionDays,
                    slotIntervalMinutes = slotInterval,
                    isConfigurationIndividualized = isIndividualized,
                    hasWaitingList = hasWaitingList,
                    minBookingDelayHours = minBookingDelayHours,
                    minCancelDelayHours = minCancelDelayHours
                )
                _authState.value = state.copy(salon = updatedSalon)
            }
        }
    }

    fun uploadSalonLogo(uri: android.net.Uri) {
        val state = _authState.value as? AuthState.AuthenticatedWithSalon ?: return
        if (uri == android.net.Uri.EMPTY) {
            viewModelScope.launch {
                salonRepository.removeLogo().onSuccess {
                    val updatedSalon = state.salon.copy(logoUrl = null)
                    _authState.value = state.copy(salon = updatedSalon)
                }
            }
            return
        }
        viewModelScope.launch {
            _uploadProgress.value = 0
            salonRepository.uploadLogoWithProgress(uri) { progress ->
                _uploadProgress.value = progress
            }.onSuccess { url ->
                val updatedSalon = state.salon.copy(logoUrl = url)
                _authState.value = state.copy(salon = updatedSalon)
                _uploadProgress.value = null
            }.onFailure {
                _uploadProgress.value = null
            }
        }
    }

    data class SalonClientItem(
        val profile: com.example.agendei_pro.core.model.UserProfile,
        val unredeemedStamps: Int,
        val isBlocked: Boolean = false
    )

    private val _salonClients = MutableStateFlow<List<SalonClientItem>>(emptyList())
    val salonClients: StateFlow<List<SalonClientItem>> = _salonClients

    fun loadSalonClients() {
        val state = _authState.value as? AuthState.AuthenticatedWithSalon ?: return
        viewModelScope.launch {
            try {
                val profiles = salonRepository.getSalonClients(state.salon.id)
                val items = profiles.map { profile ->
                    val bindingSnap = db.collection("user_bindings")
                        .document("${profile.uid}_${state.salon.id}")
                        .get().await()
                    val isBlocked = bindingSnap.getBoolean("isBlocked") ?: false
                    
                    val history = appointmentRepository.getClientHistory(profile.uid)
                    val salonHistory = history.filter { it.salonId == state.salon.id }
                    val loyaltyState = state.salon.calculateLoyaltyState(salonHistory)
                    val activeStamps = loyaltyState.activeRewardsCount * state.salon.loyaltyRequiredServices + loyaltyState.currentCardStampsCount
                    SalonClientItem(profile, activeStamps, isBlocked)
                }
                _salonClients.value = items
            } catch (e: Exception) {
                _salonClients.value = emptyList()
            }
        }
    }

    fun toggleClientBlockStatus(clientUid: String, isBlocked: Boolean) {
        val state = _authState.value as? AuthState.AuthenticatedWithSalon ?: return
        viewModelScope.launch {
            if (salonRepository.updateClientBlockStatus(state.salon.id, clientUid, isBlocked).isSuccess) {
                loadSalonClients()
            }
        }
    }

    fun redeemLoyaltyRewardForClient(clientUid: String) {
        val state = _authState.value as? AuthState.AuthenticatedWithSalon ?: return
        val loyaltyRequired = state.salon.loyaltyRequiredServices
        viewModelScope.launch {
            appointmentRepository.redeemLoyaltyRewards(clientUid, state.salon.id, loyaltyRequired).onSuccess {
                loadSalonClients()
            }
        }
    }

    fun updateAppointmentStatus(id: String, status: String) {
        val state = _authState.value
        val autoValidate = if (state is AuthState.AuthenticatedWithSalon) state.salon.autoValidateLoyalty else false
        viewModelScope.launch { 
            appointmentRepository.updateAppointmentStatus(id, status, validateLoyalty = (status == "CONFIRMED" && autoValidate)) 
        }
    }

    fun updateLoyaltyValidation(id: String, validated: Boolean) {
        viewModelScope.launch { appointmentRepository.updateLoyaltyValidation(id, validated) }
    }

    fun deleteAppointment(id: String) {
        viewModelScope.launch {
            appointmentRepository.deleteAppointment(id)
        }
    }

    fun subscribeToPlan(planName: String, maxProfessionals: Int) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            salonRepository.updateSubscriptionPlan(user.uid, planName, maxProfessionals)
        }
    }

    override fun onCleared() {
        salonListener?.remove()
        announcementListener?.remove()
        salonAnnouncementListener?.remove()
        super.onCleared()
    }

    private fun fetchSubscriptionPrice() {
        db.collection("config").document("pricing").get().addOnSuccessListener { doc ->
            val bronze = doc.getString("bronze")
            val prata = doc.getString("prata")
            val ouro = doc.getString("ouro")
            if (bronze != null) _subscriptionPriceBronze.value = bronze
            if (prata != null) _subscriptionPricePrata.value = prata
            if (ouro != null) _subscriptionPriceOuro.value = ouro
        }
    }
}
