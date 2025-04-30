package com.example.schedo.model

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val groups: List<Group> // karena groups kosong, buat dulu Any, nanti bisa disesuaikan
)

