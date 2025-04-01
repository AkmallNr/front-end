package com.example.schedo.model

data class Task (
    val id: Int? = null,
    val name: String,
    val note :String? =null,
    val deadline:String? =null,
    val reminder:String? =null,
    val priority:String,
    val attachment: List<String>? = null,
    val status: Boolean? =null
)