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
    val segment: String = "BARBEARIA" // BARBEARIA, CABELEIREIRO, MANICURE, ESTETICA
)
