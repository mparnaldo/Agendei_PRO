package com.example.agendei_pro.core.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Salon(
    val id: String = "",
    val name: String = "",
    val ownerUid: String = "",
    val code: String = "",
    val address: String = "",
    val phoneNumber: String = "",
    @ServerTimestamp
    val trialStartDate: Date? = null,
    @get:PropertyName("isSubscribed")
    @field:PropertyName("isSubscribed")
    val isSubscribed: Boolean = false,
    val openingTime: String = "08:00",
    val closingTime: String = "18:00",
    val breakStart: String = "12:00",
    val breakEnd: String = "13:00",
    val workingDays: List<Int> = listOf(2, 3, 4, 5, 6, 7),
    val autoAccept: Boolean = false,
    val logoUrl: String? = null,
    val logoShape: String = "ROUND",
    val segment: String = "BARBEARIA", // BARBEARIA, CABELEIREIRO, MANICURE, ESTETICA
    @get:PropertyName("hasLoyaltyProgram")
    @field:PropertyName("hasLoyaltyProgram")
    val hasLoyaltyProgram: Boolean = false,
    val loyaltyRequiredServices: Int = 10,
    val loyaltyRewardDescription: String = "Corte Grátis",
    @get:PropertyName("autoValidateLoyalty")
    @field:PropertyName("autoValidateLoyalty")
    val autoValidateLoyalty: Boolean = false,
    val loyaltyRedemptionDays: Int = 30,
    val subscriptionPlan: String = "TRIAL", // TRIAL, BRONZE, PRATA, OURO
    val maxProfessionals: Int = 2,
    val slotIntervalMinutes: Int = 30,
    @get:PropertyName("isConfigurationIndividualized")
    @field:PropertyName("isConfigurationIndividualized")
    val isConfigurationIndividualized: Boolean = false
)

data class LoyaltyState(
    val activeRewardsCount: Int = 0,
    val expiredRewardsCount: Int = 0,
    val currentCardStampsCount: Int = 0,
    val nextRewardExpirationDate: java.util.Date? = null
)

fun Salon.calculateLoyaltyState(appointments: List<Appointment>): LoyaltyState {
    if (!this.hasLoyaltyProgram || this.loyaltyRequiredServices <= 0) {
        return LoyaltyState(0, 0, 0, null)
    }

    val unredeemed = appointments.filter {
        it.status == "CONFIRMED" && it.loyaltyValidated && !it.loyaltyRedeemed
    }.sortedBy { it.date ?: java.util.Date(0) }

    val totalCompletedCards = unredeemed.size / this.loyaltyRequiredServices
    val remainingStamps = unredeemed.size % this.loyaltyRequiredServices

    var activeRewards = 0
    var expiredRewards = 0
    var oldestActiveExpiration: java.util.Date? = null

    val now = java.util.Date()
    val cal = java.util.Calendar.getInstance()

    for (i in 0 until totalCompletedCards) {
        val completionAppt = unredeemed[(i + 1) * this.loyaltyRequiredServices - 1]
        val completionDate = completionAppt.date ?: java.util.Date(0)

        if (this.loyaltyRedemptionDays > 0) {
            cal.time = completionDate
            cal.add(java.util.Calendar.DAY_OF_YEAR, this.loyaltyRedemptionDays)
            val expirationDate = cal.time
            if (now.after(expirationDate)) {
                expiredRewards++
            } else {
                activeRewards++
                if (oldestActiveExpiration == null || expirationDate.before(oldestActiveExpiration)) {
                    oldestActiveExpiration = expirationDate
                }
            }
        } else {
            activeRewards++
        }
    }

    return LoyaltyState(
        activeRewardsCount = activeRewards,
        expiredRewardsCount = expiredRewards,
        currentCardStampsCount = remainingStamps,
        nextRewardExpirationDate = oldestActiveExpiration
    )
}

fun Professional.calculateLoyaltyState(appointments: List<Appointment>): LoyaltyState {
    if (!this.hasLoyaltyProgram || this.loyaltyRequiredServices <= 0) {
        return LoyaltyState(0, 0, 0, null)
    }

    val unredeemed = appointments.filter {
        it.status == "CONFIRMED" && it.loyaltyValidated && !it.loyaltyRedeemed
    }.sortedBy { it.date ?: java.util.Date(0) }

    val totalCompletedCards = unredeemed.size / this.loyaltyRequiredServices
    val remainingStamps = unredeemed.size % this.loyaltyRequiredServices

    var activeRewards = 0
    val expiredRewards = 0
    val oldestActiveExpiration: java.util.Date? = null

    for (i in 0 until totalCompletedCards) {
        activeRewards++
    }

    return LoyaltyState(
        activeRewardsCount = activeRewards,
        expiredRewardsCount = expiredRewards,
        currentCardStampsCount = remainingStamps,
        nextRewardExpirationDate = oldestActiveExpiration
    )
}

