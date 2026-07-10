package com.example.agendei_pro.core.model

import java.util.Date

data class Review(
    val id: String = "",
    val appointmentId: String = "",
    val salonId: String = "",
    val professionalId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val timestamp: Date? = null
)
