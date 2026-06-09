package com.example.agendei_pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agendei_pro.core.model.Service
import com.example.agendei_pro.core.repository.ServiceRepository
import com.example.agendei_pro.core.repository.SalonRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ServicesViewModel(
    private val repository: ServiceRepository = ServiceRepository(),
    private val salonRepository: SalonRepository = SalonRepository()
) : ViewModel() {

    private val _salonSegment = MutableStateFlow("BARBEARIA")
    val salonSegment: StateFlow<String> = _salonSegment.asStateFlow()

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    init {
        viewModelScope.launch {
            salonRepository.getSalon()?.let {
                _salonSegment.value = it.segment
            }
        }
    }

    val services: StateFlow<List<Service>> = repository.getServices()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addService(
        name: String,
        price: Double,
        duration: Int,
        category: String,
        observation: String,
        imageUrl: String,
        localImageUri: android.net.Uri? = null
    ) {
        viewModelScope.launch {
            var finalImageUrl = imageUrl
            if (localImageUri != null && localImageUri != android.net.Uri.EMPTY) {
                _isUploadingImage.value = true
                repository.uploadServiceImage(localImageUri).onSuccess {
                    finalImageUrl = it
                }.onFailure {
                    // Fallback para string vazia ou URL padrão
                }
                _isUploadingImage.value = false
            }
            val newService = Service(
                name = name,
                price = price,
                durationMinutes = duration,
                category = category,
                observation = observation,
                imageUrl = finalImageUrl
            )
            repository.addService(newService)
        }
    }

    fun deleteService(serviceId: String) {
        viewModelScope.launch {
            repository.deleteService(serviceId)
        }
    }

    fun updateService(
        serviceId: String,
        name: String,
        price: Double,
        duration: Int,
        category: String,
        observation: String,
        imageUrl: String,
        localImageUri: android.net.Uri? = null
    ) {
        viewModelScope.launch {
            var finalImageUrl = imageUrl
            if (localImageUri != null && localImageUri != android.net.Uri.EMPTY) {
                _isUploadingImage.value = true
                repository.uploadServiceImage(localImageUri).onSuccess {
                    finalImageUrl = it
                }.onFailure {}
                _isUploadingImage.value = false
            }
            val updatedService = Service(
                id = serviceId,
                name = name,
                price = price,
                durationMinutes = duration,
                category = category,
                observation = observation,
                imageUrl = finalImageUrl
            )
            repository.updateService(updatedService)
        }
    }
}
