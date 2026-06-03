package com.example.agendei_pro.core.model

/**
 * Relaciona um Cliente a um Salão
 */
data class UserBinding(
    val userId: String = "",
    val salonId: String = "",
    val salonName: String = "",
    val salonCode: String = "",
    val isDefault: Boolean = false,
    val salonLogoUrl: String? = null,
    val salonLogoShape: String = "ROUND"
)
