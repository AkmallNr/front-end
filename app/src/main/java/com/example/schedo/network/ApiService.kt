package com.example.schedo.network

import com.example.schedo.model.User
import com.example.schedo.model.Group
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @GET("users")
    suspend fun getUsers(): List<User>

    @POST("users")
    suspend fun createUser(@Body user: User): User

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: Int)

    // ✅ API untuk menambahkan grup ke user
    @POST("users/{id}/groups")
    suspend fun addGroupToUser(
        @Path("id") id: Int,
        @Body name: GroupRequest
    ): Response<Group>
}

// ✅ Model request untuk menambahkan grup ke user
data class GroupRequest(
    val name: String
)
