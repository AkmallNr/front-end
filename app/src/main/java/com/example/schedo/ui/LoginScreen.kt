package com.example.schedo.ui

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.schedo.network.RetrofitInstance
import com.example.schedo.util.PreferencesHelper
import com.example.schedo.ui.*
import com.example.schedo.ui.theme.Background
import com.example.schedo.ui.theme.Grey1
import com.example.schedo.ui.theme.Grey2
import com.example.schedo.ui.theme.Utama1
import com.example.schedo.ui.theme.Utama2
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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

    // Inisialisasi Firebase Auth
    val firebaseAuth = FirebaseAuth.getInstance()

    // Konfigurasi Google Sign-In
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("1089309830161-mg7gircvhlr3ltge0qnge4ntnt5sg4gd.apps.googleusercontent.com") // Ambil dari google-services.json
        .requestEmail()
        .build()

    val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    // Launcher untuk menangani hasil Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            Log.d("GoogleSignIn", "ID Token: $idToken, Email: ${account.email}, Name: ${account.displayName}")

            // Autentikasi dengan Firebase
            coroutineScope.launch {
                isLoading = true
                errorMessage = ""
                try {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    firebaseAuth.signInWithCredential(credential)
                        .addOnCompleteListener { authResult ->
                            if (authResult.isSuccessful) {
                                val firebaseUser = firebaseAuth.currentUser
                                firebaseUser?.let {
                                    // Kirim token ke backend untuk sinkronisasi
                                    coroutineScope.launch {
                                        try {
                                            val firebaseToken = it.getIdToken(false).result.token
                                            // Gunakan userId dari PreferencesHelper atau fallback (misalnya, 0 untuk pengujian)
                                            val userId = preferencesHelper.getUserId() ?: 0
                                            val response = RetrofitInstance.api.loginWithGoogle(
                                                userId,
                                                mapOf("token" to firebaseToken)
                                            )
                                            if (response.isSuccessful) {
                                                val user = response.body()
                                                user?.let { backendUser ->
                                                    preferencesHelper.saveUserId(backendUser.id)
                                                    Log.d("GoogleSignIn", "Backend sync berhasil: ${backendUser.name}")
                                                    navController.navigate(BottomNavItem.TODO.route) {
                                                        popUpTo("login") { inclusive = true }
                                                    }
                                                } ?: run {
                                                    errorMessage = "No user data returned from backend"
                                                    Log.w("GoogleSignIn", "Tidak ada data pengguna dari backend")
                                                }
                                            } else {
                                                errorMessage = "Sinkronisasi backend gagal: ${response.errorBody()?.string()}"
                                                Log.e("GoogleSignIn", "Backend sync gagal: ${response.errorBody()?.string()}")
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Error sinkronisasi backend: ${e.message}"
                                            Log.e("GoogleSignIn", "Backend sync error: ${e.message}", e)
                                            // Fallback ke UID Firebase jika backend gagal
                                            preferencesHelper.saveUserId(firebaseUser.uid.hashCode())
                                            navController.navigate(BottomNavItem.TODO.route) {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                } ?: run {
                                    errorMessage = "Google login berhasil tetapi pengguna Firebase tidak ditemukan"
                                    Log.w("GoogleSignIn", "Pengguna Firebase tidak ditemukan")
                                    isLoading = false
                                }
                            } else {
                                errorMessage = "Google login gagal: ${authResult.exception?.message}"
                                Log.e("GoogleSignIn", "Firebase login gagal: ${authResult.exception?.message}")
                                isLoading = false
                            }
                        }
                } catch (e: Exception) {
                    errorMessage = "Error: ${e.message}"
                    Log.e("GoogleSignIn", "Exception: ${e.message}", e)
                    isLoading = false
                }
            }
        } catch (e: ApiException) {
            errorMessage = "Google Sign-In gagal: ${e.message}"
            Log.e("GoogleSignIn", "Google Sign-In gagal: ${e.statusCode}", e)
            isLoading = false
        }
    }

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
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Utama2)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Continue")
                }
            }

            //button forgot password
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Grey2, contentColor = Utama2)
            ) { Text("Forgot Password")}

            //text dont have account
            TextButton(
                onClick = { navController.navigate("register") },
                enabled = !isLoading
            ) {
                Text(buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Grey1)){
                        append("Dont have an account?")
                    }
                    withStyle(style = SpanStyle(color = Utama2)){
                        append("Sign up")
                    }
                })
            }

            // Tombol Sign in with Google
            Button(
                onClick = {
                    val signInIntent = googleSignInClient.signInIntent
                    googleSignInLauncher.launch(signInIntent)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Grey2,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                } else {
                    Text("Sign in with Google")
                }
            }


            Text(text = "By Continuing I Agree with Privacy Policy and Terms & Conditions", modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally))


        }
    }
}