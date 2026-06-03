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

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    object AuthenticatedNoSalon : AuthState()
    object AuthenticatedNoBindings : AuthState()
    data class TrialExpired(val salon: Salon) : AuthState()
    data class AuthenticatedWithSalon(val salon: Salon, val daysRemaining: Int) : AuthState()
    data class AuthenticatedClient(val bindings: List<UserBinding>) : AuthState()
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

    private val _subscriptionPrice = MutableStateFlow("49,90")
    val subscriptionPrice = _subscriptionPrice.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    private var salonListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun checkUserStatus(isProVersion: Boolean) {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user == null) {
                _authState.value = AuthState.Unauthenticated
            } else {
                fetchSubscriptionPrice()
                _userProfile.value = profileRepository.getProfile()
                
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        viewModelScope.launch {
                            profileRepository.updateFcmToken(task.result)
                        }
                    }
                }

                if (isProVersion) {
                    observeSalonStatus(user.uid)
                } else {
                    loadClientStatus(user.uid)
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
            } else {
                val updatedBindings = bindings.map { binding ->
                    val latestSalon = salonRepository.getSalonById(binding.salonId)
                    if (latestSalon != null) {
                        binding.copy(salonName = latestSalon.name, salonLogoUrl = latestSalon.logoUrl, salonLogoShape = latestSalon.logoShape)
                    } else binding
                }
                _authState.value = AuthState.AuthenticatedClient(updatedBindings)
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
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
        _userProfile.value = null
        _clientAppointments.value = emptyList()
        _pendingSalonAppointments.value = emptyList()
    }

    fun updateProfileName(newName: String) {
        val current = _userProfile.value ?: return
        viewModelScope.launch {
            val updated = current.copy(name = newName)
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

    fun updateSalonSettings(name: String, opening: String, closing: String, breakStart: String, breakEnd: String, days: List<Int>, autoAccept: Boolean, logoShape: String, segment: String) {
        val state = _authState.value as? AuthState.AuthenticatedWithSalon ?: return
        viewModelScope.launch {
            val result = salonRepository.updateSalonSettings(name, opening, closing, breakStart, breakEnd, days, autoAccept, logoShape, segment)
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
                    segment = segment
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

    fun updateAppointmentStatus(id: String, status: String) {
        viewModelScope.launch { appointmentRepository.updateAppointmentStatus(id, status) }
    }

    fun deleteAppointment(id: String) {
        viewModelScope.launch { appointmentRepository.deleteAppointment(id) }
    }

    override fun onCleared() {
        salonListener?.remove()
        super.onCleared()
    }

    private fun fetchSubscriptionPrice() {
        db.collection("config").document("pricing").get().addOnSuccessListener { doc ->
            val price = doc.getString("value")
            if (price != null) _subscriptionPrice.value = price
        }
    }
}
