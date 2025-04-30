package com.example.schedo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.schedo.network.RetrofitInstance
import com.example.schedo.util.PreferencesHelper
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavHostController) {
    val context = LocalContext.current
    val preferencesHelper = remember { PreferencesHelper(context) }
    val coroutineScope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

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
                text = "Login",
                style = MaterialTheme.typography.headlineMedium
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = ""
                            try {
                                val credentials = mapOf(
                                    "email" to email,
                                    "password" to password
                                )
                                val response = RetrofitInstance.api.loginUser(credentials)
                                if (response.isSuccessful) {
                                    val loginResponse = response.body()
                                    if (loginResponse != null) {
                                        val user = loginResponse.data
                                        preferencesHelper.saveUserId(user.id)
                                        navController.navigate(BottomNavItem.TODO.route) {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    } else {
                                        errorMessage = "Login successful but no user data returned"
                                    }
                                } else {
                                    val errorBody = response.errorBody()?.string()
                                    errorMessage = if (errorBody?.contains("Invalid credentials") == true) {
                                        "Invalid email or password"
                                    } else {
                                        "Failed to login: $errorBody"
                                    }
                                }
                            } catch (e: Exception) {
                                errorMessage = "Error: ${e.message}"
                                e.printStackTrace()
                            } finally {
                                isLoading = false
                            }
                        }
                    } else {
                        errorMessage = "Please fill all fields"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Login")
                }
            }

            TextButton(
                onClick = { navController.navigate("register") },
                enabled = !isLoading
            ) {
                Text("Don't have an account? Register")
            }
        }
    }
}