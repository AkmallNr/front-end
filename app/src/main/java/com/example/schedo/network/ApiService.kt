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

    @GET("users/{userId}/groups/{groupId}/projects")
    suspend fun getProjectsByGroup(
        @Path("userId") userId: Int,
        @Path("groupId") groupId: Int
    ): List<Project>


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

    @GET("users/{userId}/groups")
    suspend fun getGroup(
        @Path("userId") userId: Int
    ): List<Group>

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

    @PUT("users/{userId}/groups/{groupId}/projects/{projectId}")
    suspend fun updateProject(
        @Path("userId") userId: Int,
        @Path("groupId") groupId: Int,
        @Path("projectId") projectId: Int,
        @Body projectRequest: ProjectRequest
    ): Response<Project>
}

// ✅ Model request untuk menambahkan grup ke user
data class GroupRequest(
    val name: String,
    val icon: String
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
    val description: String?= null,
    val deadline:String? = null,
    val reminder:String? = null,
    val priority:String,
    val attachment: List<String>? = null,
    val status: Boolean? = null
)
