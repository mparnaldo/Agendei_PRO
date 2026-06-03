package com.example.agendei_pro.core.model

data class Service(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val durationMinutes: Int = 30,
    val category: String = "GERAL",
    val observation: String = ""
)
