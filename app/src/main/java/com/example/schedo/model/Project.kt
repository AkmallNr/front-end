package com.example.schedo.model

data class Project(
    val id: Int?,
    val name: String?,
    val description: String?,
    val startDate: String?,
    val endDate: String?,
    val groupId: Int // Pastikan ini ada
)