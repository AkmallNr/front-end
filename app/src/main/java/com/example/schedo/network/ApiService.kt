package com.example.schedo.network

import com.example.schedo.model.User
import com.example.schedo.model.Group
import com.example.schedo.model.Project
import retrofit2.Response
import retrofit2.http.*

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

    @POST("users/{id}/groups/{id}/projects")
    suspend fun addprojecttoGroup(
        @Path("id") id: Int,
        @Body name: ProjectRequest
    ): Response<Project>
}

// ✅ Model request untuk menambahkan grup ke user
data class GroupRequest(
    val name: String
)

data class ProjectRequest(
    val name: String
)
