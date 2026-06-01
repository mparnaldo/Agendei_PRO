package com.example.agendei_pro.core.model

/**
 * Perfil do usuário (pode ser dono de salão ou cliente)
 */
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val fcmToken: String = "", // Token para as notificações
    val photoUrl: String = "" // URL da foto do perfil (Google)
)
