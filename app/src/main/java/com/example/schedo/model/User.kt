package com.example.schedo.model

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val groups: List<Group>,
    val profile_picture: String? = null
)