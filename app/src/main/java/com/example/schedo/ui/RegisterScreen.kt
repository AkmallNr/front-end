package com.example.schedo.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.schedo.model.User
import com.example.schedo.network.RetrofitInstance
import com.example.schedo.ui.theme.Grey1
import com.example.schedo.ui.theme.Grey2
import com.example.schedo.ui.theme.Utama2
import com.example.schedo.R
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
                text = "Sign Up",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(
                modifier = Modifier.size(160.dp).padding(bottom = 16.dp),
            ){
                Image(
                    painter = painterResource(id = R.drawable.logofix),
                    contentDescription = "Schedo Logo",
                    modifier = Modifier.size(160.dp).padding(bottom = 16.dp)
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Grey2,
                        unfocusedLabelColor = Color.Black,
                        unfocusedContainerColor = Grey2,
                        focusedBorderColor = Utama2,
                        focusedLabelColor = Utama2),
                isError = name.isEmpty() && errorMessage.isNotEmpty()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Grey2,
                        unfocusedLabelColor = Color.Black,
                        unfocusedContainerColor = Grey2,
                        focusedBorderColor = Utama2,
                        focusedLabelColor = Utama2),
                isError = (email.isEmpty() || !email.matches(emailPattern)) && errorMessage.isNotEmpty()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Grey2,
                        unfocusedLabelColor = Color.Black,
                        unfocusedContainerColor = Grey2,
                        focusedBorderColor = Utama2,
                        focusedLabelColor = Utama2),
                isError = password.isEmpty() && errorMessage.isNotEmpty()
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Grey2,
                        unfocusedLabelColor = Color.Black,
                        unfocusedContainerColor = Grey2,
                        focusedBorderColor = Utama2,
                        focusedLabelColor = Utama2),
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
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Utama2)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Continue")
                }
            }

            //text already have an account
            TextButton(
                onClick = { navController.navigate("login") }
//                enabled = !isLoading
            ) {
                Text(buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Grey1)){
                        append("Already have an account?")
                    }
                    withStyle(style = SpanStyle(color = Utama2)){
                        append(" Login")
                    }
                })
            }

            // text terms & condition
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(18.dp)
            ){
                Text(buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Grey1)){
                        append("By Continuing I agree with Privacy Policy")
                    }
                })
                Text(buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Grey1)){
                        append("and Terms & Conditions")
                    }
                })

            }

        }
    }
}