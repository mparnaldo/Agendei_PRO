package com.example.agendei_pro.core.model

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
    val servicePrice: Double = 0.0
)
