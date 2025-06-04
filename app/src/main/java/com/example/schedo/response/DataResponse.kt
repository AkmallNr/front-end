package com.example.schedo.response

import com.example.schedo.model.*
import com.google.gson.annotations.SerializedName

data class UserResponse(
    val data: List<User>
)

data class ProjectResponse(
    val data: List<Project>
)

data class GroupResponse(
    val data: List<Group>
)

data class TaskResponse(
    val data: List<Task>
)

data class LoginResponse(
    val data: User
)

data class QuoteResponse(
    val data: List<Quote>
)

data class ScheduleResponse(
    val message: String,
    val data: List<Schedule>
)

data class UserResponse2(
    @SerializedName("data")
    val data: User
)