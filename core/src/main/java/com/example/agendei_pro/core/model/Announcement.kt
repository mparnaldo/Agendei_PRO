package com.example.agendei_pro.core.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Announcement(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val displayType: String = "BANNER", // BANNER, POPUP, NONE
    val duration: String = "PERMANENT", // ONCE, DAY, WEEK, PERMANENT
    @ServerTimestamp
    val timestamp: Date? = null,
    val expiresAt: Date? = null
)
