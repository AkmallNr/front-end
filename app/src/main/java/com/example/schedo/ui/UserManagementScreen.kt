package com.example.schedo.ui

import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.schedo.model.User
import com.example.schedo.network.RetrofitInstance
import com.example.schedo.util.PreferencesHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

@Composable
fun UserManagementScreen() {
    val context = LocalContext.current
    val preferencesHelper = PreferencesHelper(context)
    val userId = preferencesHelper.getUserId()

    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    // State for tracking if we need to refresh user data after upload
    var shouldRefreshUserData by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                isUploading = true
                uploadError = null
                try {
                    Log.d("UserManagementScreen", "Starting profile picture upload for userId: $userId")
                    Log.d("UserManagementScreen", "Selected image URI: $uri")

                    // Convert URI to File
                    val file = uriToFile(context, uri)
                    Log.d("UserManagementScreen", "Converted URI to file: ${file.absolutePath}, size: ${file.length()} bytes")

                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("profile_picture", file.name, requestFile)
                    Log.d("UserManagementScreen", "Created multipart body with file name: ${file.name}")

                    // Upload profile picture
                    val response = RetrofitInstance.api.updateProfilePicture(userId, body)
                    Log.d("UserManagementScreen", "Upload response code: ${response.code()}")
                    Log.d("UserManagementScreen", "Upload response message: ${response.message()}")
                    Log.d("UserManagementScreen", "Raw response body: ${response.body()}")

                    if (response.isSuccessful) {
                        // Ambil data pengguna dari UserResponse
                        val updatedUser = response.body()?.data
                        if (updatedUser != null) {
                            user = updatedUser
                            Log.d("UserManagementScreen", "Upload successful, updated user: $user")

                            // If profile picture is still null, we need to refresh user data
                            if (updatedUser.profilePicture == null) {
                                Log.d("UserManagementScreen", "Profile picture still null, will refresh user data")
                                shouldRefreshUserData = true
                                // Give the server some time to process the image
                                delay(1000)

                                try {
                                    Log.d("UserManagementScreen", "Refreshing user data for userId: $userId")
                                    val refreshResponse = RetrofitInstance.api.getUsers()
                                    Log.d("UserManagementScreen", "Get users refresh response code: ${refreshResponse.code()}")

                                    val refreshedUser = refreshResponse.body()?.data?.find { it.id == userId }
                                    if (refreshedUser != null) {
                                        user = refreshedUser
                                        Log.d("UserManagementScreen", "Refreshed user data: $refreshedUser")
                                    } else {
                                        Log.w("UserManagementScreen", "User not found in refresh for userId: $userId")
                                    }
                                } catch (e: Exception) {
                                    Log.e("UserManagementScreen", "Failed to refresh user data: ${e.message}", e)
                                } finally {
                                    shouldRefreshUserData = false
                                }
                            }
                        } else {
                            uploadError = "Pengguna tidak ditemukan dalam respons"
                            Log.w("UserManagementScreen", "User not found in response for userId: $userId")
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        uploadError = "Gagal mengunggah foto profil: ${response.message()}"
                        Log.e("UserManagementScreen", "Upload failed: ${response.message()}, error body: $errorBody")
                    }
                } catch (e: Exception) {
                    uploadError = "Error saat unggah: ${e.message}"
                    Log.e("UserManagementScreen", "Upload exception: ${e.message}", e)
                } finally {
                    isUploading = false
                    Log.d("UserManagementScreen", "Upload process completed, isUploading: $isUploading")
                }
            }
        } ?: Log.d("UserManagementScreen", "No image selected (URI is null)")
    }

    // Fetch user data
    LaunchedEffect(Unit) {
        if (userId != -1) {
            coroutineScope.launch {
                try {
                    Log.d("UserManagementScreen", "Fetching user data for userId: $userId")
                    val response = RetrofitInstance.api.getUsers()
                    Log.d("UserManagementScreen", "Get users response code: ${response.code()}")
                    user = response.body()?.data?.find { it.id == userId }
                    if (user == null) {
                        errorMessage = "User not found"
                        Log.w("UserManagementScreen", "User not found for userId: $userId")
                    } else {
                        Log.d("UserManagementScreen", "User data fetched: $user")
                    }
                } catch (e: Exception) {
                    errorMessage = "Failed to load user data: ${e.message}"
                    Log.e("UserManagementScreen", "Failed to fetch user data: ${e.message}", e)
                } finally {
                    isLoading = false
                    Log.d("UserManagementScreen", "Fetch user data completed, isLoading: $isLoading")
                }
            }
        } else {
            isLoading = false
            errorMessage = "No user logged in"
            Log.w("UserManagementScreen", "No user logged in, userId: $userId")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator()
            }
            errorMessage != null -> {
                Text(
                    text = errorMessage ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 16.sp
                )
            }
            user != null -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Profile",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Display profile picture
                    if (!user?.profilePicture.isNullOrEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(user?.profilePicture),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = "No profile picture",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Button to upload profile picture
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        enabled = !isUploading && !shouldRefreshUserData
                    ) {
                        Text("Change Profile Picture")
                    }
                    // Show upload error if any
                    uploadError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Name: ${user?.name ?: "N/A"}",
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Email: ${user?.email ?: "N/A"}",
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Groups:",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (user?.groups?.isNotEmpty() == true) {
                        user?.groups?.forEach { group ->
                            Text(
                                text = "- ${group.name}",
                                fontSize = 16.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "No groups joined",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
        // Show uploading indicator
        if (isUploading || shouldRefreshUserData) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                CircularProgressIndicator()
                if (shouldRefreshUserData) {
                    Text(
                        text = "Refreshing profile data...",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

// Helper function to convert URI to File
private fun uriToFile(context: Context, uri: android.net.Uri): File {
    val file = File(context.cacheDir, "profile_picture_${System.currentTimeMillis()}.jpg")
    Log.d("UserManagementScreen", "Creating file at: ${file.absolutePath}")
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output ->
            val bytesCopied = input.copyTo(output)
            Log.d("UserManagementScreen", "Copied $bytesCopied bytes from URI to file")
        }
    }
    return file
}