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

class AgendaViewModel(private val repository: AppointmentRepository = AppointmentRepository()) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val salonId: String? get() = auth.currentUser?.uid

    private val _selectedDate = MutableStateFlow(Date())
    val selectedDate: StateFlow<Date> = _selectedDate

    private val _viewMode = MutableStateFlow(AgendaViewMode.LIST)
    val viewMode: StateFlow<AgendaViewMode> = _viewMode

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
    val allAppointments: StateFlow<List<Appointment>> = _selectedDate
        .flatMapLatest { _ ->
            val id = salonId
            if (id != null) {
                repository.getAllSalonAppointments(id)
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

    fun setViewMode(mode: AgendaViewMode) {
        _viewMode.value = mode
    }

    fun updateStatus(appointmentId: String, status: String) {
        viewModelScope.launch {
            repository.updateAppointmentStatus(appointmentId, status)
        }
    }

    fun deleteAppointment(id: String) {
        viewModelScope.launch {
            repository.deleteAppointment(id)
        }
    }

    fun blockTimeSlot(timeStr: String, reason: String) {
        val id = salonId ?: return
        val date = _selectedDate.value
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
                    status = "BLOCKED"
                )
                repository.createAppointment(appt)
            } catch (e: Exception) {}
        }
    }
}
