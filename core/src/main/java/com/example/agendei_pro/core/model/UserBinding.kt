package com.example.agendei_pro.core.model

import com.google.firebase.firestore.PropertyName

/**
 * Relaciona um Cliente a um Salão
 */
data class UserBinding(
    val userId: String = "",
    val salonId: String = "",
    val salonName: String = "",
    val salonCode: String = "",
    @get:PropertyName("isDefault")
    @field:PropertyName("isDefault")
    val isDefault: Boolean = false,
    val salonLogoUrl: String? = null,
    val salonLogoShape: String = "ROUND",
    val isBlocked: Boolean = false
)
