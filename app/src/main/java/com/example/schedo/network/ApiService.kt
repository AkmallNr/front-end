package com.example.schedo.network

import com.example.schedo.model.User
import com.example.schedo.model.Group
import com.example.schedo.model.Project
import com.example.schedo.model.Task
import retrofit2.Response
import retrofit2.http.*
import java.sql.Date
import java.time.LocalDate

interface ApiService {
    @GET("users")
    suspend fun getUsers(): List<User>

    @POST("users")
    suspend fun createUser(@Body user: User): User

    @DELETE("users/{userId}")
    suspend fun deleteUser(@Path("userId") id: Int)

    @DELETE("users/{userId}/groups/{groupId}")
    suspend fun deleteGroup(@Path("userId") userId: Int, @Path("groupId")groupId: Int)

    // ✅ API untuk menambahkan grup ke user
    @POST("users/{id}/groups")
    suspend fun addGroupToUser(
        @Path("id") id: Int,
        @Body name: GroupRequest
    ): Response<Group>

    @POST("users/{userId}/groups/{groupId}/projects")
    suspend fun addProjectToGroup(
        @Path("userId") userId: Int,
        @Path("groupId") groupId: Int,
        @Body projectRequest: ProjectRequest
    ): Response<Project>

    @POST("users/{userId}/groups/{groupId}/projects/{projectId}/tasks")
    suspend fun addTaskToProject(
        @Path("userId") userId: Int,
        @Path("groupId") groupId: Int,
        @Path("projectId") projectId : Int,
        @Body tasksRequest: TaskRequest
    ): Response<Task>
}

// ✅ Model request untuk menambahkan grup ke user
data class GroupRequest(
    val name: String
)

data class ProjectRequest(
    val name: String,
    val description: String,
    val startDate: String,
    val endDate: String
)

data class TaskRequest(
    val id: Int? = null,
    val name: String,
    val note :String,
    val deadline:String,
    val reminder:String,
    val priority:String,
    val attachment: List<String>? = null,
    val status: Boolean
)
