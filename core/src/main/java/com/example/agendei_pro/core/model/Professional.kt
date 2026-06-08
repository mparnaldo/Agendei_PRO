package com.example.agendei_pro.core.model

import com.google.firebase.firestore.PropertyName

data class Professional(
    val id: String = "",
    val salonId: String = "",
    val name: String = "",
    val specialties: List<String> = emptyList(), // List of serviceIds they can perform. Empty = performs all.
    @get:PropertyName("isActive")
    @field:PropertyName("isActive")
    val isActive: Boolean = true,
    val photoUrl: String? = null,
    val openingTime: String = "08:00",
    val closingTime: String = "18:00",
    val breakStart: String = "12:00",
    val breakEnd: String = "13:00",
    val workingDays: List<Int> = listOf(2, 3, 4, 5, 6, 7),
    val servicePrices: Map<String, Double> = emptyMap(), // Map of serviceId to overridden price
    
    @get:PropertyName("hasCustomSchedule")
    @field:PropertyName("hasCustomSchedule")
    val hasCustomSchedule: Boolean = false,
    
    @get:PropertyName("hasCustomLoyalty")
    @field:PropertyName("hasCustomLoyalty")
    val hasCustomLoyalty: Boolean = false,
    
    @get:PropertyName("hasLoyaltyProgram")
    @field:PropertyName("hasLoyaltyProgram")
    val hasLoyaltyProgram: Boolean = false,
    
    val loyaltyRequiredServices: Int = 10,
    val loyaltyRewardDescription: String = "Corte Grátis"
)
