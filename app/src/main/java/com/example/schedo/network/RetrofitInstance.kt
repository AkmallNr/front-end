package com.example.schedo.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS) // waktu koneksi ke server
        .readTimeout(60, TimeUnit.SECONDS)    // waktu nunggu respon
        .writeTimeout(60, TimeUnit.SECONDS)   // waktu kirim data ke server
        .build()


    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000/api/")
            .client(client)// Emulator Android localhost
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
