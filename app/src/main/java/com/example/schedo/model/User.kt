package com.example.schedo.model

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val quotes: List<Quote>,
    val groups: List<Group>,
    val profile_picture: String? = null
)