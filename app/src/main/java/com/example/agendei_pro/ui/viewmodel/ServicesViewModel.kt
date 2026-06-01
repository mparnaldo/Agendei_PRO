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

    fun addService(name: String, price: Double, duration: Int, category: String, observation: String) {
        viewModelScope.launch {
            val newService = Service(
                name = name,
                price = price,
                durationMinutes = duration,
                category = category,
                observation = observation
            )
            repository.addService(newService)
        }
    }

    fun deleteService(serviceId: String) {
        viewModelScope.launch {
            repository.deleteService(serviceId)
        }
    }
}
