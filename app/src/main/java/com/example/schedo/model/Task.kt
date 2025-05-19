package com.example.schedo.model

data class Task (
    val id: Int? = null,
    val name: String,
    val description: String?= null,
    val deadline:String? =null,
    val reminder:String? =null,
    val priority:String,
    val attachment: List<String>? = null,
    val status: Boolean? =null,
    val quoteId: Int? = null
)

data class Attachment(
    val id: Int,
    val taskId: Int,
    val file_name: String,
    val file_url: String
)