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
    private val salonRepository: SalonRepository = SalonRepository(),
    private val waitingRepository: WaitingRepository = WaitingRepository()
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val _services = MutableStateFlow<List<Service>>(emptyList())
    val services: StateFlow<List<Service>> = _services.asStateFlow()

    private val _selectedService = MutableStateFlow<Service?>(null)
    val selectedService = _selectedService.asStateFlow()

    private val _salon = MutableStateFlow<Salon?>(null)
    val salon = _salon.asStateFlow()

    private val _professionals = MutableStateFlow<List<Professional>>(emptyList())
    val professionals: StateFlow<List<Professional>> = _professionals.asStateFlow()

    private val _isLoadingProfs = MutableStateFlow(true)
    val isLoadingProfs: StateFlow<Boolean> = _isLoadingProfs.asStateFlow()

    private val _selectedProfessional = MutableStateFlow<Professional?>(null)
    val selectedProfessional = _selectedProfessional.asStateFlow()

    private val _availableSlots = MutableStateFlow<List<TimeSlot>>(emptyList())
    val availableSlots = _availableSlots.asStateFlow()

    private val _isLoyaltyEligible = MutableStateFlow(false)
    val isLoyaltyEligible: StateFlow<Boolean> = _isLoyaltyEligible.asStateFlow()

    private var loyaltyRequired: Int = 10

    private val _statusMessage = MutableSharedFlow<String>()
    val statusMessage = _statusMessage.asSharedFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess = _isSuccess.asStateFlow()

    fun loadServices(salonId: String) {
        viewModelScope.launch {
            val salonObj = salonRepository.getSalonById(salonId)
            _salon.value = salonObj
            serviceRepository.getServices(salonId).collect { _services.value = it }
        }
    }

    fun loadProfessionals(salonId: String) {
        viewModelScope.launch {
            _isLoadingProfs.value = true
            _professionals.value = salonRepository.getProfessionals(salonId)
            _isLoadingProfs.value = false
        }
    }

    fun selectProfessional(professional: Professional?) {
        _selectedProfessional.value = professional
        val salonVal = _salon.value
        if (salonVal != null) {
            checkLoyaltyEligibility(salonVal.id, professional?.id)
        }
    }

    fun checkLoyaltyEligibility(salonId: String, professionalId: String? = null) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            val salonObj = salonRepository.getSalonById(salonId) ?: return@launch
            if (salonObj.isConfigurationIndividualized && professionalId != null) {
                val professionalsList = _professionals.value
                val selectedPro = professionalsList.find { it.id == professionalId }
                if (selectedPro != null && selectedPro.hasCustomLoyalty) {
                    if (selectedPro.hasLoyaltyProgram) {
                        loyaltyRequired = selectedPro.loyaltyRequiredServices
                        val history = appointmentRepository.getClientHistory(user.uid)
                        val proHistory = history.filter { it.salonId == salonId && it.professionalId == professionalId }
                        val loyaltyState = selectedPro.calculateLoyaltyState(proHistory)
                        _isLoyaltyEligible.value = loyaltyState.activeRewardsCount > 0
                    } else {
                        _isLoyaltyEligible.value = false
                    }
                    return@launch
                }
            }

            if (salonObj.hasLoyaltyProgram) {
                loyaltyRequired = salonObj.loyaltyRequiredServices
                val history = appointmentRepository.getClientHistory(user.uid)
                val salonHistory = history.filter { it.salonId == salonId }
                val loyaltyState = salonObj.calculateLoyaltyState(salonHistory)
                _isLoyaltyEligible.value = loyaltyState.activeRewardsCount > 0
            } else {
                _isLoyaltyEligible.value = false
            }
        }
    }

    fun selectService(service: Service) {
        _selectedService.value = service
        _selectedProfessional.value = null // reset pro selection on service change
        val salonVal = _salon.value
        if (salonVal != null) {
            checkLoyaltyEligibility(salonVal.id, null)
        }
    }

    fun loadAvailableSlots(salonId: String, date: Date) {
        val selectedServiceId = _selectedService.value?.id ?: ""
        val selectedProId = _selectedProfessional.value?.id
        viewModelScope.launch {
            val salon = salonRepository.getSalonById(salonId) ?: return@launch
            appointmentRepository.getAppointmentsForDay(date, salonId).collect { appointments ->
                val qualifiedProfs = _professionals.value.filter { pro ->
                    pro.specialties.isEmpty() || pro.specialties.contains(selectedServiceId)
                }
                _availableSlots.value = generateSlotsForTeam(salon, appointments, qualifiedProfs, selectedProId, date)
            }
        }
    }

    private fun timeToMinutes(timeStr: String): Int? {
        val parts = timeStr.split(":")
        if (parts.size != 2) return null
        val hours = parts[0].trim().toIntOrNull() ?: return null
        val minutes = parts[1].trim().toIntOrNull() ?: return null
        return hours * 60 + minutes
    }

    private fun isProAvailableForSlot(
        pro: Professional,
        slotStartCal: Calendar,
        bookingLimitCal: Calendar,
        activeBookings: List<Appointment>,
        isIndividualized: Boolean,
        salon: Salon
    ): Boolean {
        val useIndividual = isIndividualized && pro.hasCustomSchedule
        val opTimeStr = if (useIndividual) pro.openingTime else salon.openingTime
        val clTimeStr = if (useIndividual) pro.closingTime else salon.closingTime
        val brStartStr = if (useIndividual) pro.breakStart else salon.breakStart
        val brEndStr = if (useIndividual) pro.breakEnd else salon.breakEnd
        val workDays = if (useIndividual) pro.workingDays else salon.workingDays

        val dayOfWeek = slotStartCal.get(Calendar.DAY_OF_WEEK)
        if (!workDays.contains(dayOfWeek)) return false

        val slotMin = slotStartCal.get(Calendar.HOUR_OF_DAY) * 60 + slotStartCal.get(Calendar.MINUTE)
        val opMin = timeToMinutes(opTimeStr) ?: return false
        val clMin = timeToMinutes(clTimeStr) ?: return false

        if (slotMin < opMin || slotMin >= clMin) return false

        if (!brStartStr.isNullOrBlank() && !brEndStr.isNullOrBlank()) {
            val brStartMin = timeToMinutes(brStartStr)
            val brEndMin = timeToMinutes(brEndStr)
            if (brStartMin != null && brEndMin != null) {
                if (slotMin >= brStartMin && slotMin < brEndMin) return false
            }
        }

        if (slotStartCal.before(bookingLimitCal)) return false

        val timeStr = String.format(Locale.US, "%02d:%02d", slotStartCal.get(Calendar.HOUR_OF_DAY), slotStartCal.get(Calendar.MINUTE))
        val isBlocked = activeBookings.any { appt ->
            val matchesTime = getFormattedTime(appt.date) == timeStr
            val isForThisPro = appt.professionalId == pro.id
            val isSalonWide = appt.professionalId == "ALL"
            matchesTime && (isForThisPro || isSalonWide)
        }
        return !isBlocked
    }

    private fun generateSlotsForTeam(
        salon: Salon,
        appointments: List<Appointment>,
        qualifiedProfs: List<Professional>,
        selectedProId: String?,
        selectedDate: Date
    ): List<TimeSlot> {
        val slots = mutableListOf<TimeSlot>()
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        val calendar = Calendar.getInstance()
        
        try {
            val startTime = sdf.parse(salon.openingTime) ?: return emptyList()
            val endTime = sdf.parse(salon.closingTime) ?: return emptyList()
            
            val bookingLimitCal = Calendar.getInstance().apply {
                add(Calendar.HOUR_OF_DAY, salon.minBookingDelayHours)
            }
            
            val activeBookings = appointments.filter { it.status == "CONFIRMED" || it.status == "PENDING" || it.status == "BLOCKED" }
            val interval = if (salon.slotIntervalMinutes > 0) salon.slotIntervalMinutes else 30
            val serviceDuration = _selectedService.value?.durationMinutes ?: 30
            val slotsNeeded = Math.ceil(serviceDuration.toDouble() / interval.toDouble()).toInt()

            calendar.time = startTime
            while (calendar.time.before(endTime)) {
                val timeStr = sdf.format(calendar.time)
                var isSlotAvailable = false
                
                if (selectedProId != null) {
                    val pro = qualifiedProfs.find { it.id == selectedProId }
                    if (pro != null) {
                        var consecutiveAvailable = true
                        val checkCal = Calendar.getInstance()
                        for (k in 0 until slotsNeeded) {
                            checkCal.time = calendar.time
                            checkCal.add(Calendar.MINUTE, k * interval)
                            
                            val slotCalToCheck = Calendar.getInstance().apply {
                                time = selectedDate
                                set(Calendar.HOUR_OF_DAY, checkCal.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, checkCal.get(Calendar.MINUTE))
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            
                            if (!isProAvailableForSlot(pro, slotCalToCheck, bookingLimitCal, activeBookings, salon.isConfigurationIndividualized, salon)) {
                                consecutiveAvailable = false
                                break
                            }
                        }
                        isSlotAvailable = consecutiveAvailable
                    }
                } else {
                    if (qualifiedProfs.isEmpty()) {
                        var consecutiveAvailable = true
                        val checkCal = Calendar.getInstance()
                        for (k in 0 until slotsNeeded) {
                            checkCal.time = calendar.time
                            checkCal.add(Calendar.MINUTE, k * interval)
                            val checkTimeStr = String.format(Locale.US, "%02d:%02d", checkCal.get(Calendar.HOUR_OF_DAY), checkCal.get(Calendar.MINUTE))
                            
                            val checkMin = checkCal.get(Calendar.HOUR_OF_DAY) * 60 + checkCal.get(Calendar.MINUTE)
                            val bStartMin = timeToMinutes(salon.breakStart)
                            val bEndMin = timeToMinutes(salon.breakEnd)
                            
                            val isBreak = if (bStartMin != null && bEndMin != null) {
                                checkMin >= bStartMin && checkMin < bEndMin
                            } else false

                            val slotCal = Calendar.getInstance().apply {
                                time = selectedDate
                                set(Calendar.HOUR_OF_DAY, checkCal.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, checkCal.get(Calendar.MINUTE))
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val isRetroOrDelay = slotCal.before(bookingLimitCal)

                            val isBlocked = activeBookings.any { appt ->
                                getFormattedTime(appt.date) == checkTimeStr
                            }

                            val endMin = timeToMinutes(salon.closingTime) ?: (18 * 60)
                            if (isBreak || isRetroOrDelay || isBlocked || checkMin >= endMin) {
                                consecutiveAvailable = false
                                break
                            }
                        }
                        isSlotAvailable = consecutiveAvailable
                    } else {
                        val anyAvailable = qualifiedProfs.any { pro ->
                            var consecutiveAvailable = true
                            val checkCal = Calendar.getInstance()
                            for (k in 0 until slotsNeeded) {
                                checkCal.time = calendar.time
                                checkCal.add(Calendar.MINUTE, k * interval)
                                
                                val slotCalToCheck = Calendar.getInstance().apply {
                                    time = selectedDate
                                    set(Calendar.HOUR_OF_DAY, checkCal.get(Calendar.HOUR_OF_DAY))
                                    set(Calendar.MINUTE, checkCal.get(Calendar.MINUTE))
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }

                                if (!isProAvailableForSlot(pro, slotCalToCheck, bookingLimitCal, activeBookings, salon.isConfigurationIndividualized, salon)) {
                                    consecutiveAvailable = false
                                    break
                                }
                            }
                            consecutiveAvailable
                        }
                        isSlotAvailable = anyAvailable
                    }
                }

                slots.add(TimeSlot(timeStr, isSlotAvailable))
                calendar.add(Calendar.MINUTE, interval)
            }
        } catch (e: Exception) {}
        return slots
    }

    private fun getFormattedTime(date: Date?): String {
        if (date == null) return ""
        val c = Calendar.getInstance().apply { time = date }
        return String.format(Locale.US, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    }

    private fun isSameDay(d1: Date, d2: Date): Boolean {
        val c1 = Calendar.getInstance().apply { time = d1 }
        val c2 = Calendar.getInstance().apply { time = d2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    fun createAppointment(salonId: String, date: Date, isLoyaltyRedemption: Boolean = false) {
        val user = auth.currentUser ?: return
        val service = _selectedService.value ?: return
        viewModelScope.launch {
            val salon = salonRepository.getSalonById(salonId)
            val isAutoAccept = salon?.autoAccept ?: false
            val isLoyaltyAutoValidate = salon?.autoValidateLoyalty ?: false
            
            val existingAppts = appointmentRepository.getAppointmentsForDay(date, salonId).first()
            val activeBookings = existingAppts.filter { it.status == "CONFIRMED" || it.status == "PENDING" || it.status == "BLOCKED" }
            val interval = if ((salon?.slotIntervalMinutes ?: 30) > 0) salon!!.slotIntervalMinutes else 30
            val serviceDuration = service.durationMinutes
            val slotsNeeded = Math.ceil(serviceDuration.toDouble() / interval.toDouble()).toInt()
            
            val bookingLimitCal = Calendar.getInstance().apply {
                add(Calendar.HOUR_OF_DAY, salon?.minBookingDelayHours ?: 0)
            }
            
            // Auto-assign professional if "Tanto faz" is selected
            val assignedPro = if (_selectedProfessional.value != null) {
                _selectedProfessional.value
            } else {
                val qualified = _professionals.value.filter { pro ->
                    pro.specialties.isEmpty() || pro.specialties.contains(service.id)
                }
                qualified.find { pro ->
                    var isProAvailable = true
                    for (k in 0 until slotsNeeded) {
                        val slotCal = Calendar.getInstance().apply {
                            time = date
                            add(Calendar.MINUTE, k * interval)
                        }
                        if (salon != null && !isProAvailableForSlot(pro, slotCal, bookingLimitCal, activeBookings, salon.isConfigurationIndividualized, salon)) {
                            isProAvailable = false
                            break
                        }
                    }
                    isProAvailable
                }
            }

            if (assignedPro == null) {
                _statusMessage.emit("Horário ou profissional indisponível para este serviço.")
                return@launch
            }

            // double check if the selected professional is actually available at this time
            if (_selectedProfessional.value != null) {
                val pro = _selectedProfessional.value!!
                var isAvailable = true
                for (k in 0 until slotsNeeded) {
                    val slotCal = Calendar.getInstance().apply {
                        time = date
                        add(Calendar.MINUTE, k * interval)
                    }
                    if (salon != null && !isProAvailableForSlot(pro, slotCal, bookingLimitCal, activeBookings, salon.isConfigurationIndividualized, salon)) {
                        isAvailable = false
                        break
                    }
                }
                if (!isAvailable) {
                    _statusMessage.emit("O profissional escolhido não está mais disponível neste horário.")
                    return@launch
                }
            }

            if (isLoyaltyRedemption) {
                val limit = if (salon?.isConfigurationIndividualized == true && assignedPro != null && assignedPro.hasCustomLoyalty) {
                    assignedPro.loyaltyRequiredServices
                } else {
                    salon?.loyaltyRequiredServices ?: 10
                }
                val targetProId = if (salon?.isConfigurationIndividualized == true && assignedPro != null && assignedPro.hasCustomLoyalty) {
                    assignedPro.id
                } else {
                    null
                }
                appointmentRepository.redeemLoyaltyRewards(user.uid, salonId, limit, targetProId)
            }

            val appt = Appointment(
                clientName = user.displayName ?: "Cliente",
                clientUid = user.uid,
                salonId = salonId,
                serviceId = service.id,
                serviceName = service.name,
                servicePrice = if (isLoyaltyRedemption) {
                    0.0
                } else {
                    if (salon?.isConfigurationIndividualized == true && assignedPro != null) {
                        assignedPro.servicePrices[service.id] ?: service.price
                    } else {
                        service.price
                    }
                },
                date = date,
                status = if (isAutoAccept) "CONFIRMED" else "PENDING",
                loyaltyValidated = (isAutoAccept && isLoyaltyAutoValidate),
                loyaltyRedeemed = isLoyaltyRedemption,
                professionalId = assignedPro?.id ?: "",
                professionalName = assignedPro?.name ?: ""
            )

            if (appointmentRepository.createAppointment(appt).isSuccess) {
                val message = if (isLoyaltyRedemption) {
                    "Agendamento via Fidelidade solicitado (Serviço Grátis)!"
                } else if (isAutoAccept) {
                    "Agendamento confirmado automaticamente!"
                } else {
                    "Agendamento solicitado!"
                }
                _statusMessage.emit(message)
                _isSuccess.value = true
            }
        }
    }

    fun joinWaitingList(
        salonId: String,
        date: Date,
        proId: String?,
        proName: String?,
        serviceId: String,
        serviceName: String,
        clientPhone: String
    ) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            val entry = WaitingEntry(
                clientUid = user.uid,
                clientName = user.displayName ?: "Cliente",
                clientPhone = clientPhone,
                salonId = salonId,
                professionalId = proId ?: "ANY",
                professionalName = proName ?: "Tanto faz",
                serviceId = serviceId,
                serviceName = serviceName,
                date = date,
                status = "WAITING"
            )
            if (waitingRepository.createWaitingEntry(entry).isSuccess) {
                _statusMessage.emit("Você entrou na Fila de Espera!")
                _isSuccess.value = true
            } else {
                _statusMessage.emit("Erro ao entrar na Fila de Espera.")
            }
        }
    }
}
