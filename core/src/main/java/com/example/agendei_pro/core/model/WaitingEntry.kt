package com.example.agendei_pro.core.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class WaitingEntry(
    val id: String = "",
    val clientUid: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val salonId: String = "",
    val professionalId: String = "", // "ANY" or specific professional ID
    val professionalName: String = "",
    val serviceId: String = "",
    val serviceName: String = "",
    @ServerTimestamp
    val date: Date? = null, // The date the client is waiting for
    @ServerTimestamp
    val createdAt: Date? = null,
    val status: String = "WAITING" // WAITING, NOTIFIED, COMPLETED, EXPIRED
)
