package com.example.schedo.model

data class Group(
    val id: Int? = null,
    val name: String,
    val projects: List<Project> = emptyList(),
    val icon: String = "fas fa-users" // Default icon
)