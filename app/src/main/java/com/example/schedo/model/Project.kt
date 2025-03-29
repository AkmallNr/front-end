package com.example.schedo.model

data class Project(
    val id: Int? = null,
    val name: String,
    val description: String,
    val startDate: String,
    val endDate: String
)