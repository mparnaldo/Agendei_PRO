package com.example.agendei_pro.core.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Appointment(
    val id: String = "",
    val clientName: String = "",
    val clientUid: String = "",
    val salonId: String = "",
    val serviceId: String = "",
    val serviceName: String = "",
    @ServerTimestamp
    val date: Date? = null,
    val status: String = "PENDING",
    val servicePrice: Double = 0.0,
    @get:PropertyName("loyaltyValidated")
    @field:PropertyName("loyaltyValidated")
    val loyaltyValidated: Boolean = false,
    @get:PropertyName("loyaltyRedeemed")
    @field:PropertyName("loyaltyRedeemed")
    val loyaltyRedeemed: Boolean = false,
    val professionalId: String = "",
    val professionalName: String = "",
    val cancelledBy: String? = null
)
