package com.example.schedo.network

import com.example.schedo.model.User
import com.example.schedo.model.Group
import com.example.schedo.model.Project
import com.example.schedo.model.Task
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @GET("users")
    suspend fun getUsers(): List<User>

    // 🔹 Endpoint baru: Mendapatkan semua proyek berdasarkan userId
    @GET("users/{userId}/projects")
    suspend fun getProjectsByUser(
        @Path("userId") userId: Int
    ): List<Project>

    // 🔹 Endpoint lama: Mendapatkan proyek berdasarkan groupId (tetap dipertahankan)
    @GET("users/{userId}/groups/{groupId}/projects")
    suspend fun getProjectsByGroup(
        @Path("userId") userId: Int,
        @Path("groupId") groupId: Int
    ): List<Project>

    @POST("users")
    suspend fun createUser(@Body user: User): User

    @DELETE("users/{userId}")
    suspend fun deleteUser(@Path("userId") id: Int)

    @GET("users/{userId}/groups")
    suspend fun getGroups(
        @Path("userId") userId: Int
    ): List<Group>

    @DELETE("users/{userId}/groups/{groupId}")
    suspend fun deleteGroup(
        @Path("userId") userId: Int,
        @Path("groupId") groupId: Int
    )

    @POST("users/{id}/groups")
    suspend fun addGroupToUser(
        @Path("id") id: Int,
        @Body groupRequest: GroupRequest
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
        @Path("projectId") projectId: Int,
        @Body taskRequest: TaskRequest
    ): Response<Task>

    @GET("users/{userId}/groups/{groupId}/projects/{projectId}/tasks")
    suspend fun getTask(
        @Path("userId") userId: Int,
        @Path("groupId") groupId: Int,
        @Path("projectId") projectId: Int
    ): List<Task>

    @PUT("users/{userId}/groups/{groupId}/projects/{projectId}/tasks/{taskId}")
    suspend fun updateTask(
        @Path("userId") userId: Int,
        @Path("groupId") groupId: Int,
        @Path("projectId") projectId: Int,
        @Path("taskId") taskId: Int,
        @Body taskRequest: TaskRequest
    ): Response<Task>

    @DELETE("users/{userId}/groups/{groupId}/projects/{projectId}")
    suspend fun deleteProject(
        @Path("userId") userId: Int,
        @Path("groupId") groupId: Int,
        @Path("projectId") projectId: Int
    )

    @DELETE("users/{userId}/groups/{groupId}/projects/{projectId}/tasks/{taskId}")
    suspend fun deleteTask(
        @Path("userId") userId: Int,
        @Path("groupId") groupId: Int,
        @Path("projectId") projectId: Int,
        @Path("taskId") taskId: Int
    )
}

data class GroupRequest(
    val name: String,
    val icon: String
)

data class ProjectRequest(
    val name: String,
    val description: String,
    val startDate: String?,
    val endDate: String
)

data class TaskRequest(
    val id: Int? = null,
    val name: String,
    val description: String? = null,
    val deadline: String? = null,
    val reminder: String? = null,
    val priority: String,
    val attachment: List<String>? = null,
    val status: Boolean? = null
)