package com.example.schedo.network

import com.example.schedo.model.User
import com.example.schedo.model.Group
import com.example.schedo.model.Project
import com.example.schedo.model.Quote
import com.example.schedo.model.Schedule
import com.example.schedo.model.Task
import com.example.schedo.response.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Existing endpoints remain unchanged, adding quote-related endpoints
    @GET("users/{userId}/quotes")
    suspend fun getQuotes(
        @Path("userId") userId: Int
    ): QuoteResponse

    @POST("users/{userId}/quotes")
    suspend fun addQuotes(
        @Path("userId") userId: Int,
        @Body quoteRequest: QuoteRequest
    ): Response<Quote>

    @PUT("users/{userId}/quotes/{quoteId}")
    suspend fun updateQuote(
        @Path("userId") userId: Int,
        @Path("quoteId") quoteId: Int,
        @Body quoteRequest: QuoteRequest
    ): Response<Quote>

    @DELETE("users/{userId}/quotes/{quoteId}")
    suspend fun deleteQuote(
        @Path("userId") userId: Int,
        @Path("quoteId") quoteId: Int
    ): Response<Unit>

    // Other existing endpoints (unchanged)
    @POST("register")
    suspend fun registerUser(@Body user: Map<String, String>): Response<User>

    @GET("users")
    suspend fun getUsers(): Response<UserResponse>

    @GET("users/{userId}/projects")
    suspend fun getProjectsByUser(
        @Path("userId") userId: Int
    ): ProjectResponse

    @GET("users/{userId}/tasks")
    suspend fun getTaskByUser(
        @Path("userId") userId: Int
    ): TaskResponse

    @GET("users/{userId}/groups/{groupId}/projects")
    suspend fun getProjectsByGroup(
        @Path("userId") userId: Int,
        @Path("groupId") groupId: Int
    ): ProjectResponse

    @DELETE("users/{userId}")
    suspend fun deleteUser(@Path("userId") id: Int)

    @POST("login")
    suspend fun loginUser(@Body credentials: Map<String, String>): Response<LoginResponse>

    @GET("users/{userId}/groups")
    suspend fun getGroups(
        @Path("userId") userId: Int
    ): GroupResponse

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

    @GET("users/{userId}/groups/{groupId}/projects/{projectId}/taskProject")
    suspend fun getTask(
        @Path("userId") userId: Int,
        @Path("groupId") groupId: Int,
        @Path("projectId") projectId: Int
    ): TaskResponse

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

    @Multipart
    @POST("users/{userId}/profile-picture")
    suspend fun updateProfilePicture(
        @Path("userId") userId: Int,
        @Part profilePicture: MultipartBody.Part
    ): Response<UserResponse2>

    @GET("users/{userId}/schedules")
    suspend fun getSchedules(
        @Path("userId") userId: Int,
        @Query("startTime") startTime: Long
    ): ScheduleResponse

    @POST("users/{userId}/schedules")
    suspend fun addSchedule(
        @Path("userId") userId: Int,
        @Body schedule: Schedule
    )

    @PUT("users/{userId}/schedules/{scheduleId}")
    suspend fun updateSchedule(
        @Path("userId") userId: Int,
        @Body schedule: Schedule
    )

    @DELETE("users/{userId}/schedules/{scheduleId}")
    suspend fun deleteSchedule(
        @Path("userId") userId: Int,
        @Path("id") id: Int
    ): Response<Unit>

    @POST("google-login")
    suspend fun loginWithGoogle(
        @Body token: Map<String, String>
    ): Response<LoginResponse>

    @PUT("users/{userId}/groups/{groupId}")
    suspend fun updateGroup(
        @Path("userId") userId: Int,
        @Path("groupId") groupId: Int,
        @Body groupRequest: GroupRequest
    ): Response<Group>

    @GET("users/{userId}/groups/{groupId}/projects/{projectId}/taskProject")
    suspend fun getTaskByProject(
        @Path("userId") userId: Int,
        @Path("groupId") groupId: Int,
        @Path("projectId") projectId: Int
    ): TaskResponse
}

// Data classes remain unchanged
data class ScheduleRequest(
    val name: String,
    val notes: String,
    val repeat: Boolean,
    val day: String,
    val startTime: String,
    val endTime: String
)

data class GroupRequest(
    val name: String,
    val icon: String
)

data class QuoteRequest(
    val content: String
)

data class ProjectRequest(
    val name: String,
    val description: String,
    val startDate: String?,
    val endDate: String,
    val groupId: Int? = null
)

data class TaskRequest(
    val id: Int? = null,
    val name: String,
    val description: String? = null,
    val deadline: String? = null,
    val reminder: String? = null,
    val priority: String,
    val attachment: List<String>? = null,
    val status: Boolean? = null,
    val quoteId: Int? = null
)