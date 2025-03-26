package com.example.schedo.model

data class User(
    val id: Int? = null,
    val name: String,
    val email: String,
    val pass: String,
    val groups: List<Group> = emptyList() // Tambahkan field groups
)


