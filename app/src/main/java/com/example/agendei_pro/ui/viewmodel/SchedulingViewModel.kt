package com.example.agendei_pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agendei_pro.core.model.*
import com.example.agendei_pro.core.repository.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class TimeSlot(val time: String, val isAvailable: Boolean)

class SchedulingViewModel(
    private val serviceRepository: ServiceRepository = ServiceRepository(),
    private val appointmentRepository: AppointmentRepository = AppointmentRepository(),
    private val salonRepository: SalonRepository = SalonRepository()
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val _services = MutableStateFlow<List<Service>>(emptyList())
    val services: StateFlow<List<Service>> = _services.asStateFlow()

    private val _selectedService = MutableStateFlow<Service?>(null)
    val selectedService = _selectedService.asStateFlow()

    private val _availableSlots = MutableStateFlow<List<TimeSlot>>(emptyList())
    val availableSlots = _availableSlots.asStateFlow()

    private val _statusMessage = MutableSharedFlow<String>()
    val statusMessage = _statusMessage.asSharedFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess = _isSuccess.asStateFlow()

    fun loadServices(salonId: String) {
        viewModelScope.launch {
            serviceRepository.getServices(salonId).collect { _services.value = it }
        }
    }

    fun selectService(service: Service) { _selectedService.value = service }

    fun loadAvailableSlots(salonId: String, date: Date) {
        viewModelScope.launch {
            val salon = salonRepository.getSalonById(salonId) ?: return@launch
            appointmentRepository.getAppointmentsForDay(date, salonId).collect { appointments ->
                val occupied = appointments.map {
                    val c = Calendar.getInstance().apply { time = it.date ?: Date() }
                    String.format(Locale.getDefault(), "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
                }
                _availableSlots.value = generateSlots(salon, occupied, date)
            }
        }
    }

    private fun generateSlots(salon: Salon, occupied: List<String>, selectedDate: Date): List<TimeSlot> {
        val slots = mutableListOf<TimeSlot>()
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val calendar = Calendar.getInstance()
        
        try {
            val startTime = sdf.parse(salon.openingTime) ?: return emptyList()
            val endTime = sdf.parse(salon.closingTime) ?: return emptyList()
            val bStart = sdf.parse(salon.breakStart)
            val bEnd = sdf.parse(salon.breakEnd)
            
            val now = Calendar.getInstance()
            val isToday = isSameDay(selectedDate, now.time)

            calendar.time = startTime
            while (calendar.time.before(endTime)) {
                val timeStr = sdf.format(calendar.time)
                
                // Verifica se está no intervalo de almoço 🍱
                val isBreak = if (bStart != null && bEnd != null) {
                    !calendar.time.before(bStart) && calendar.time.before(bEnd)
                } else false

                val isRetroactive = if (isToday) {
                    val slotCal = Calendar.getInstance().apply {
                        time = selectedDate
                        set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY))
                        set(Calendar.MINUTE, calendar.get(Calendar.MINUTE))
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    slotCal.before(now)
                } else false

                slots.add(TimeSlot(timeStr, !occupied.contains(timeStr) && !isRetroactive && !isBreak))
                calendar.add(Calendar.MINUTE, 30)
            }
        } catch (e: Exception) {}
        return slots
    }

    private fun isSameDay(d1: Date, d2: Date): Boolean {
        val c1 = Calendar.getInstance().apply { time = d1 }
        val c2 = Calendar.getInstance().apply { time = d2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    fun createAppointment(salonId: String, date: Date) {
        val user = auth.currentUser ?: return
        val service = _selectedService.value ?: return
        viewModelScope.launch {
            val salon = salonRepository.getSalonById(salonId)
            val isAutoAccept = salon?.autoAccept ?: false
            val appt = Appointment(
                clientName = user.displayName ?: "Cliente",
                clientUid = user.uid,
                salonId = salonId,
                serviceId = service.id,
                serviceName = service.name,
                servicePrice = service.price,
                date = date,
                status = if (isAutoAccept) "CONFIRMED" else "PENDING"
            )
            if (appointmentRepository.createAppointment(appt).isSuccess) {
                val message = if (isAutoAccept) "Agendamento confirmado automaticamente!" else "Agendamento solicitado!"
                _statusMessage.emit(message)
                _isSuccess.value = true
            }
        }
    }
}
