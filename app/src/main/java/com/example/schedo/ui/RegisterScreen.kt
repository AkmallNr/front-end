package com.example.schedo.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.schedo.model.User
import com.example.schedo.network.RetrofitInstance
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Composable
fun RegisterScreen(navController: NavHostController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+".toRegex()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Register",
                style = MaterialTheme.typography.headlineMedium
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = name.isEmpty() && errorMessage.isNotEmpty()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                isError = (email.isEmpty() || !email.matches(emailPattern)) && errorMessage.isNotEmpty()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                isError = password.isEmpty() && errorMessage.isNotEmpty()
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                isError = (confirmPassword.isEmpty() || confirmPassword != password) && errorMessage.isNotEmpty()
            )

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Button(
                onClick = {
                    when {
                        name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() -> {
                            errorMessage = "Please fill all fields"
                        }
                        !email.matches(emailPattern) -> {
                            errorMessage = "Invalid email format"
                        }
                        password.length < 8 -> {
                            errorMessage = "Password must be at least 8 characters"
                        }
                        password != confirmPassword -> {
                            errorMessage = "Passwords do not match"
                        }
                        else -> {
                            coroutineScope.launch {
                                isLoading = true
                                try {
                                    val userData = mapOf(
                                        "name" to name,
                                        "email" to email,
                                        "password" to password,
                                        "password_confirmation" to confirmPassword
                                    )
                                    val response = RetrofitInstance.api.registerUser(userData)
                                    val rawJson = response.errorBody()?.string()
                                    Log.d("RegisterScreen", "raw error body: $rawJson")
                                    if (response.isSuccessful) {
                                        // Log data user yang disimpan
                                        val savedUser = response.body()
                                        Log.d("RegisterScreen", "savedUser: $savedUser")
                                        Log.d("RegisterScreen", "savedUser ID=${savedUser?.id} Name=${savedUser?.name} Email=${savedUser?.email}")
                                        navController.navigate("login") {
                                            popUpTo("register") { inclusive = true }
                                        }
                                    } else {
                                        val errorBody = response.errorBody()?.string()
                                        Log.d("RegisterScreen", "Error response body: $errorBody")
                                        val errorMapType = object : TypeToken<Map<String, Any>>() {}.type
                                        val errorMap: Map<String, Any>? = Gson().fromJson(errorBody, errorMapType)
                                        // Hindari unchecked cast dengan pengecekan tipe yang aman
                                        val errors = errorMap?.get("errors")
                                        val errorMessageFromResponse = if (errors is Map<*, *>) {
                                            errors.entries.firstOrNull()?.value?.let { value ->
                                                if (value is List<*>) value.firstOrNull()?.toString() else value.toString()
                                            }
                                        } else {
                                            null
                                        }
                                        errorMessage = errorMessageFromResponse ?: "Registration failed"
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Error: ${e.message}"
                                    Log.e("RegisterScreen", "Registration error: ${e.message}", e)
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Register")
                }
            }

            TextButton(
                onClick = { navController.navigate("login") }
            ) {
                Text("Already have an account? Login")
            }
        }
    }
}