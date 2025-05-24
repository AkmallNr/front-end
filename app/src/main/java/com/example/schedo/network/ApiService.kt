package com.example.schedo.network

import com.example.schedo.model.User
import com.example.schedo.model.Group
import com.example.schedo.model.Project
import com.example.schedo.model.Schedule
import com.example.schedo.model.Task
import com.example.schedo.model.UserListResponse
import com.example.schedo.response.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Endpoint register baru
    @POST("register")
    suspend fun registerUser(@Body user: Map<String, String>): Response<User>

    @GET("users")
    suspend fun getUsers(): Response<UserListResponse>

    // 🔹 Endpoint baru: Mendapatkan semua proyek berdasarkan userId
    @GET("users/{userId}/projects")
    suspend fun getProjectsByUser(
        @Path("userId") userId: Int
    ): ProjectResponse

    @GET("users/{userId}/tasks")
    suspend fun getTaskByUser(
        @Path("userId") userId: Int
    ): TaskResponse

    // 🔹 Endpoint lama: Mendapatkan proyek berdasarkan groupId (tetap dipertahankan)
    @GET("users/{userId}/groups/{groupId}/projects")
    suspend fun getProjectsByGroup(
        @Path("userId") userId: Int,
        @Path("groupId") groupId: Int
    ): ProjectResponse

//    @POST("users")
//    suspend fun createUser(@Body user: User): Response<User>

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

    @GET("users/{userId}/groups/{groupId}/projects/{projectId}/tasks")
    suspend fun getTask(
        @Path("userId") userId: Int,
        @Path("groupId") groupId: Int,
        @Path("projectId") projectId: Int
    ): TaskResponse

    @GET("users/{userId}/groups/{groupId}/projects/{projectId}/tasks/{taskId}")
    suspend fun getTaskById(
        @Path("userId") userId: Int,
        @Path("groupId") groupId: Int,
        @Path("projectId") projectId: Int,
        @Path("taskId") taskId: Int
    ): TaskResponse

    @PUT("users/{userId}/groups/{groupId}/projects/{projectId}/tasks/{taskId}")
    suspend fun updateTask(
        @Path("userId") userId: Int,
        @Path("groupId") groupId: Int,
        @Path("projectId") projectId: Int,
        @Path("taskId") taskId: Int,
        @Body taskRequest: TaskRequest
    ): Response<Task>

    @GET("users/{userId}/groups/{groupId}/projects/{projectId}/tasks/completed-today")
    suspend fun getCompletedTasksCountToday(
        @Path("userId") userId: Int,
        @Path("groupId") groupId: Int,
        @Path("projectId") projectId: Int
    ): CompletedTasksResponse

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

    @GET("users/{userId}/quotes")
    suspend fun getQuotes(
        @Path("userId") userId: Int
    ): QuoteResponse

    @POST("users/{userId}/quotes")
    suspend fun addQuotes(
        @Path("userId") userId: Int,
        @Body quoteRequest : QuoteRequest
    )

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

    @Multipart
    @POST("upload-file")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part?,
        @Part("link") link: RequestBody?
    ): Response<FileUploadResponse>

    // 🔹 Endpoint baru: Login dengan Google
//  @POST("users/{userId}/google-login")
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

    @GET("users/{userId}/tasks/weekly-completed")
    suspend fun getWeeklyCompletedTasks(
        @Path("userId") userId: Int,
        @Query("week_start") weekStart: String? = null
    ): Response<WeeklyCompletedTasksResponse>
}

data class WeeklyCompletedTasksResponse(
    val data: WeeklyCompletedTasksData
)

data class WeeklyCompletedTasksData(
    val date_range: String,
    val tasks: Map<String, Int>,
    val week_start: String // Tambahkan untuk menyimpan tanggal mulai minggu
)

data class FileUploadResponse(
    val success: Boolean,
    val data: FileUploadData,
    val message: String
)

data class FileUploadData(
    val file_url: String?,
    val file_name: String?,
    val link: String?
)

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
    val groupId: Int? = null // Add groupId to allow updating the group
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

data class CompletedTasksResponse(
    val data: CompletedTasksData
)

data class CompletedTasksData(
    val completed_today: Int
)