package com.example.schedo.response

import com.example.schedo.model.Group
import com.example.schedo.model.Project
import com.example.schedo.model.Task
import com.example.schedo.model.User

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
