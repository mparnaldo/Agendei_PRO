package com.example.agendei_pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agendei_pro.core.model.Professional
import com.example.agendei_pro.core.model.Salon
import com.example.agendei_pro.core.model.Service
import com.example.agendei_pro.core.repository.SalonRepository
import com.example.agendei_pro.core.repository.ServiceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TeamViewModel(
    private val salonRepository: SalonRepository = SalonRepository(),
    private val serviceRepository: ServiceRepository = ServiceRepository()
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val salonId: String? get() = auth.currentUser?.uid

    private val _salon = MutableStateFlow<Salon?>(null)
    val salon: StateFlow<Salon?> = _salon.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        viewModelScope.launch {
            salonId?.let { id ->
                db.collection("salons").document(id).addSnapshotListener { snapshot, _ ->
                    _salon.value = snapshot?.toObject(Salon::class.java)
                }
            }
        }
    }

    val services: StateFlow<List<Service>> = serviceRepository.getServices()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val professionals: StateFlow<List<Professional>> = callbackFlow {
        val id = salonId
        if (id == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = db.collection("salons")
            .document(id)
            .collection("professionals")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.toObjects(Professional::class.java) ?: emptyList()
                trySend(list)
            }

        awaitClose { registration.remove() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addProfessional(
        name: String,
        specialties: List<String>,
        photoUrl: String?,
        photoUri: android.net.Uri?,
        onResult: (Boolean, String?) -> Unit
    ) {
        val id = salonId ?: return
        val currentSalon = _salon.value ?: return
        val currentList = professionals.value

        if (currentList.size >= currentSalon.maxProfessionals) {
            onResult(false, "Limite de profissionais atingido no seu plano!")
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            salonRepository.addProfessional(id, name, specialties, photoUrl)
                .onSuccess { newPro ->
                    if (photoUri != null && photoUri != android.net.Uri.EMPTY) {
                        salonRepository.uploadProfessionalPhoto(id, newPro.id, photoUri)
                            .onSuccess { downloadUrl ->
                                val updatedPro = newPro.copy(photoUrl = downloadUrl)
                                salonRepository.updateProfessional(id, updatedPro)
                                    .onSuccess { onResult(true, null) }
                                    .onFailure { onResult(false, "Foto enviada, mas erro ao salvar dados: ${it.message}") }
                            }
                            .onFailure { onResult(false, "Erro ao fazer upload da foto: ${it.message}") }
                    } else {
                        onResult(true, null)
                    }
                }
                .onFailure {
                    onResult(false, it.message)
                }
            _isSaving.value = false
        }
    }

    fun updateProfessional(
        professional: Professional,
        photoUri: android.net.Uri?,
        onResult: (Boolean, String?) -> Unit
    ) {
        val id = salonId ?: return
        viewModelScope.launch {
            _isSaving.value = true
            if (photoUri != null && photoUri != android.net.Uri.EMPTY) {
                salonRepository.uploadProfessionalPhoto(id, professional.id, photoUri)
                    .onSuccess { downloadUrl ->
                        val updatedPro = professional.copy(photoUrl = downloadUrl)
                        salonRepository.updateProfessional(id, updatedPro)
                            .onSuccess { onResult(true, null) }
                            .onFailure { onResult(false, it.message) }
                    }
                    .onFailure { onResult(false, "Erro ao fazer upload da foto: ${it.message}") }
            } else {
                salonRepository.updateProfessional(id, professional)
                    .onSuccess { onResult(true, null) }
                    .onFailure { onResult(false, it.message) }
            }
            _isSaving.value = false
        }
    }

    fun deleteProfessional(professionalId: String, onResult: (Boolean, String?) -> Unit) {
        val id = salonId ?: return
        viewModelScope.launch {
            salonRepository.deleteProfessional(id, professionalId)
                .onSuccess {
                    onResult(true, null)
                }
                .onFailure {
                    onResult(false, it.message)
                }
        }
    }

    fun fetchGoogleProfilePhoto(email: String, onResult: (String?, String?, String?) -> Unit) {
        if (email.isBlank()) {
            onResult(null, null, "E-mail inválido")
            return
        }
        viewModelScope.launch {
            try {
                val query = db.collection("profiles")
                    .whereEqualTo("email", email.trim().lowercase())
                    .get()
                    .await()
                val profileDoc = query.documents.firstOrNull()
                if (profileDoc != null) {
                    val photoUrl = profileDoc.getString("photoUrl")
                    val name = profileDoc.getString("name")
                    onResult(photoUrl, name, null)
                } else {
                    onResult(null, null, "Nenhum usuário do Agendei encontrado com este e-mail.")
                }
            } catch (e: Exception) {
                onResult(null, null, "Erro ao buscar: ${e.message}")
            }
        }
    }
}
