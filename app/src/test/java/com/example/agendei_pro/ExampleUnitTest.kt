package com.example.agendei_pro

import org.junit.Test
import org.junit.Assert.*
import com.example.agendei_pro.core.model.*
import java.text.SimpleDateFormat
import java.util.*

class ExampleUnitTest {

    data class TimeSlot(val time: String, val isAvailable: Boolean)

    private fun isProAvailableForSlotTest(
        pro: Professional,
        slotStartCal: Calendar,
        isToday: Boolean,
        now: Calendar,
        activeBookings: List<Appointment>,
        isIndividualized: Boolean,
        salon: Salon
    ): Boolean {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeStr = sdf.format(slotStartCal.time)

        val useIndividual = isIndividualized && pro.hasCustomSchedule
        val opTimeStr = if (useIndividual) pro.openingTime else salon.openingTime
        val clTimeStr = if (useIndividual) pro.closingTime else salon.closingTime
        val brStartStr = if (useIndividual) pro.breakStart else salon.breakStart
        val brEndStr = if (useIndividual) pro.breakEnd else salon.breakEnd
        val workDays = if (useIndividual) pro.workingDays else salon.workingDays

        val dayOfWeek = slotStartCal.get(Calendar.DAY_OF_WEEK)
        if (!workDays.contains(dayOfWeek)) return false

        return try {
            val opTime = sdf.parse(opTimeStr) ?: return false
            val clTime = sdf.parse(clTimeStr) ?: return false
            val brStart = if (!brStartStr.isNullOrBlank()) sdf.parse(brStartStr) else null
            val brEnd = if (!brEndStr.isNullOrBlank()) sdf.parse(brEndStr) else null
            val slotTime = sdf.parse(timeStr) ?: return false

            if (slotTime.before(opTime) || !slotTime.before(clTime)) return false

            if (brStart != null && brEnd != null) {
                if (!slotTime.before(brStart) && slotTime.before(brEnd)) return false
            }

            if (isToday && slotStartCal.before(now)) return false

            val isBlocked = activeBookings.any { appt ->
                val matchesTime = getFormattedTime(appt.date) == timeStr
                val isForThisPro = appt.professionalId == pro.id
                val isSalonWide = appt.professionalId == "ALL"
                matchesTime && (isForThisPro || isSalonWide)
            }
            !isBlocked
        } catch (e: Exception) {
            false
        }
    }

    private fun getFormattedTime(date: Date?): String {
        if (date == null) return ""
        val c = Calendar.getInstance().apply { time = date }
        return String.format(Locale.getDefault(), "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    }

    private fun generateSlotsForTeamTest(
        salon: Salon,
        appointments: List<Appointment>,
        qualifiedProfs: List<Professional>,
        selectedProId: String?,
        selectedDate: Date,
        serviceDuration: Int,
        nowTime: Date
    ): List<TimeSlot> {
        val slots = mutableListOf<TimeSlot>()
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val calendar = Calendar.getInstance()
        
        try {
            val startTime = sdf.parse(salon.openingTime) ?: return emptyList()
            val endTime = sdf.parse(salon.closingTime) ?: return emptyList()
            val bStart = if (!salon.breakStart.isNullOrBlank()) sdf.parse(salon.breakStart) else null
            val bEnd = if (!salon.breakEnd.isNullOrBlank()) sdf.parse(salon.breakEnd) else null
            
            val now = Calendar.getInstance().apply { time = nowTime }
            val isToday = false // set to false for unit test predictability
            
            val activeBookings = appointments.filter { it.status == "CONFIRMED" || it.status == "PENDING" || it.status == "BLOCKED" }
            val interval = if (salon.slotIntervalMinutes > 0) salon.slotIntervalMinutes else 30
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
                            
                            val slotIsToday = isToday
                            val slotCalToCheck = Calendar.getInstance().apply {
                                time = selectedDate
                                set(Calendar.HOUR_OF_DAY, checkCal.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, checkCal.get(Calendar.MINUTE))
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            
                            if (!isProAvailableForSlotTest(pro, slotCalToCheck, slotIsToday, now, activeBookings, salon.isConfigurationIndividualized, salon)) {
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
                            val checkTimeStr = sdf.format(checkCal.time)
                            
                            val isBreak = if (bStart != null && bEnd != null) {
                                !checkCal.time.before(bStart) && checkCal.time.before(bEnd)
                            } else false

                            val isRetro = if (isToday) {
                                val slotCal = Calendar.getInstance().apply {
                                    time = selectedDate
                                    set(Calendar.HOUR_OF_DAY, checkCal.get(Calendar.HOUR_OF_DAY))
                                    set(Calendar.MINUTE, checkCal.get(Calendar.MINUTE))
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                slotCal.before(now)
                            } else false

                            val isBlocked = activeBookings.any { appt ->
                                getFormattedTime(appt.date) == checkTimeStr
                            }

                            if (isBreak || isRetro || isBlocked || checkCal.time.after(endTime) || checkCal.time.equals(endTime)) {
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
                                
                                val slotIsToday = isToday
                                val slotCalToCheck = Calendar.getInstance().apply {
                                    time = selectedDate
                                    set(Calendar.HOUR_OF_DAY, checkCal.get(Calendar.HOUR_OF_DAY))
                                    set(Calendar.MINUTE, checkCal.get(Calendar.MINUTE))
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }

                                if (!isProAvailableForSlotTest(pro, slotCalToCheck, slotIsToday, now, activeBookings, salon.isConfigurationIndividualized, salon)) {
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

    @Test
    fun runComprehensiveTests() {
        val selectedDate = SimpleDateFormat("yyyy-MM-dd").parse("2026-06-08")
        val now = Calendar.getInstance().apply {
            time = selectedDate
            add(Calendar.DAY_OF_YEAR, -1)
        }.time

        println("=== TEST 1: Fallback (qualifiedProfs is empty) ===")
        val salon1 = Salon(
            openingTime = "08:00",
            closingTime = "18:00",
            breakStart = "11:30",
            breakEnd = "13:00",
            slotIntervalMinutes = 30,
            isConfigurationIndividualized = false
        )
        val slots1 = generateSlotsForTeamTest(salon1, emptyList(), emptyList(), null, selectedDate, 60, now)
        val slot11_t1 = slots1.find { it.time == "11:00" }
        assertNotNull(slot11_t1)
        println("Fallback 11:00 slot isAvailable: ${slot11_t1?.isAvailable}")

        println("=== TEST 2: Individualized configuration is enabled, but professional has hasCustomSchedule = false ===")
        val salon2 = Salon(
            openingTime = "08:00",
            closingTime = "18:00",
            breakStart = "11:30",
            breakEnd = "13:00",
            slotIntervalMinutes = 30,
            isConfigurationIndividualized = true
        )
        val pro2 = Professional(
            id = "pro1",
            name = "Pro 1",
            hasCustomSchedule = false,
            breakStart = "12:00", // even if this is defined, it shouldn't be used since hasCustomSchedule is false
            breakEnd = "13:00"
        )
        val slots2 = generateSlotsForTeamTest(salon2, emptyList(), listOf(pro2), "pro1", selectedDate, 60, now)
        val slot11_t2 = slots2.find { it.time == "11:00" }
        assertNotNull(slot11_t2)
        println("Pro1 (no custom schedule, salon break 11:30) 11:00 slot isAvailable: ${slot11_t2?.isAvailable}")

        println("=== TEST 3: Individualized config enabled, pro has custom schedule enabled, pro break is 12:00 to 13:00 (default) ===")
        val salon3 = Salon(
            openingTime = "08:00",
            closingTime = "18:00",
            breakStart = "11:30",
            breakEnd = "13:00",
            slotIntervalMinutes = 30,
            isConfigurationIndividualized = true
        )
        val pro3 = Professional(
            id = "pro1",
            name = "Pro 1",
            hasCustomSchedule = true,
            breakStart = "12:00", // pro has custom break at 12:00
            breakEnd = "13:00"
        )
        val slots3 = generateSlotsForTeamTest(salon3, emptyList(), listOf(pro3), "pro1", selectedDate, 60, now)
        val slot11_t3 = slots3.find { it.time == "11:00" }
        assertNotNull(slot11_t3)
        println("Pro1 (custom schedule, break 12:00-13:00) 11:00 slot isAvailable: ${slot11_t3?.isAvailable}")

        println("=== TEST 4: Individualized config enabled, pro has custom schedule enabled, pro break is 11:30 to 13:00 ===")
        val salon4 = Salon(
            openingTime = "08:00",
            closingTime = "18:00",
            breakStart = "11:30",
            breakEnd = "13:00",
            slotIntervalMinutes = 30,
            isConfigurationIndividualized = true
        )
        val pro4 = Professional(
            id = "pro1",
            name = "Pro 1",
            hasCustomSchedule = true,
            breakStart = "11:30",
            breakEnd = "13:00"
        )
        val slots4 = generateSlotsForTeamTest(salon4, emptyList(), listOf(pro4), "pro1", selectedDate, 60, now)
        val slot11_t4 = slots4.find { it.time == "11:00" }
        assertNotNull(slot11_t4)
        println("Pro1 (custom schedule, break 11:30-13:00) 11:00 slot isAvailable: ${slot11_t4?.isAvailable}")
    }
}