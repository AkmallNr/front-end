package com.example.schedo.model

data class Task (
    val id: Int? = null,
    val name: String,
    val note :String,
    val deadline:String,
    val reminder:String,
    val priority:String,
    val attachment: List<String>? = null,
    val status: Boolean? =null
)