package com.example.agendei_pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agendei_pro.core.model.Appointment
import com.example.agendei_pro.core.repository.AppointmentRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

enum class AgendaViewMode {
    LIST, CALENDAR_DAY, CALENDAR_MONTH
}

enum class AgendaSortMode {
    DATE_ASC, DATE_DESC
}

class AgendaViewModel(private val repository: AppointmentRepository = AppointmentRepository()) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val salonId: String? get() = auth.currentUser?.uid

    private val _selectedDate = MutableStateFlow(Date())
    val selectedDate: StateFlow<Date> = _selectedDate

    private val _salonHasLoyalty = MutableStateFlow(false)
    val salonHasLoyalty: StateFlow<Boolean> = _salonHasLoyalty

    private val _salon = MutableStateFlow<com.example.agendei_pro.core.model.Salon?>(null)
    val salon: StateFlow<com.example.agendei_pro.core.model.Salon?> = _salon

    private val _services = MutableStateFlow<List<com.example.agendei_pro.core.model.Service>>(emptyList())
    val services: StateFlow<List<com.example.agendei_pro.core.model.Service>> = _services

    init {
        val id = salonId
        if (id != null) {
            viewModelScope.launch {
                val salonObj = com.example.agendei_pro.core.repository.SalonRepository().getSalonById(id)
                _salon.value = salonObj
                _salonHasLoyalty.value = salonObj?.hasLoyaltyProgram ?: false
            }
            viewModelScope.launch {
                com.example.agendei_pro.core.repository.ServiceRepository().getServices(id).collect { list ->
                    _services.value = list
                }
            }
        }
    }

    private val _viewMode = MutableStateFlow(AgendaViewMode.LIST)
    val viewMode: StateFlow<AgendaViewMode> = _viewMode

    private val _sortMode = MutableStateFlow(AgendaSortMode.DATE_ASC)
    val sortMode: StateFlow<AgendaSortMode> = _sortMode

    private val _currentMonth = MutableStateFlow(Calendar.getInstance())
    val currentMonth: StateFlow<Calendar> = _currentMonth

    // Agendamentos do dia selecionado
    @OptIn(ExperimentalCoroutinesApi::class)
    val appointments: StateFlow<List<Appointment>> = _selectedDate
        .flatMapLatest { date -> 
            val id = salonId
            if (id != null) {
                repository.getAppointmentsForDay(date, id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val allAppointments: StateFlow<List<Appointment>> = combine(_sortMode, _selectedDate) { sort, _ -> sort }
        .flatMapLatest { sort ->
            val id = salonId
            if (id != null) {
                repository.getAllSalonAppointments(id).map { list ->
                    when (sort) {
                        AgendaSortMode.DATE_ASC -> list.sortedBy { it.date }
                        AgendaSortMode.DATE_DESC -> list.sortedByDescending { it.date }
                    }
                }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onDateSelected(date: Date) {
        _selectedDate.value = date
    }

    fun setSortMode(mode: AgendaSortMode) {
        _sortMode.value = mode
    }

    fun navigateMonth(delta: Int) {
        val newCal = (_currentMonth.value.clone() as Calendar).apply {
            add(Calendar.MONTH, delta)
        }
        _currentMonth.value = newCal
    }

    fun setViewMode(mode: AgendaViewMode) {
        _viewMode.value = mode
    }

    fun updateStatus(appointmentId: String, status: String) {
        val id = salonId
        viewModelScope.launch {
            val autoValidate = if (id != null) {
                com.example.agendei_pro.core.repository.SalonRepository().getSalonById(id)?.autoValidateLoyalty ?: false
            } else false
            repository.updateAppointmentStatus(appointmentId, status, validateLoyalty = (status == "CONFIRMED" && autoValidate))
        }
    }

    fun updateLoyaltyValidation(id: String, validated: Boolean) {
        viewModelScope.launch {
            repository.updateLoyaltyValidation(id, validated)
        }
    }

    fun deleteAppointment(id: String) {
        viewModelScope.launch {
            repository.deleteAppointment(id)
        }
    }

    fun blockTimeSlot(date: Date, timeStr: String, reason: String, professionalId: String, professionalName: String) {
        val id = salonId ?: return
        viewModelScope.launch {
            try {
                val timeParts = timeStr.split(":")
                val cal = Calendar.getInstance().apply {
                    time = date
                    set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                    set(Calendar.MINUTE, timeParts[1].toInt())
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val appt = Appointment(
                    clientName = "Bloqueado: $reason",
                    clientUid = "BLOCKAGE",
                    salonId = id,
                    serviceId = "BLOCKAGE",
                    serviceName = "Ausência",
                    servicePrice = 0.0,
                    date = cal.time,
                    status = "BLOCKED",
                    professionalId = professionalId,
                    professionalName = professionalName
                )
                repository.createAppointment(appt)
            } catch (e: Exception) {}
        }
    }
}
