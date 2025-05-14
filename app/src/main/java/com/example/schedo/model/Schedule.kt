package com.example.schedo.model

data class Schedule(
    val id: Int,
    val name: String,
    val notes: String,
    val repeat: Boolean,
    val day: String,
    val startTime: String,
    val endTime: String
)