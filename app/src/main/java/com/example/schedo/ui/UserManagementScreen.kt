package com.example.schedo.ui

import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person

@Composable
fun UserManagementScreen(navController: NavHostController) {
    // Inisialisasi konteks dan helper untuk preferensi
    val context = LocalContext.current
    val preferencesHelper = PreferencesHelper(context)
    val userId = preferencesHelper.getUserId()

    // State untuk menyimpan data pengguna, status loading, pesan error, dan status upload
    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    // Coroutine scope untuk menjalankan operasi asinkronus
    val coroutineScope = rememberCoroutineScope()

    // Launcher untuk memilih gambar dari galeri
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                isUploading = true
                try {
                    Log.d("UserManagementScreen", "Memulai unggah gambar profil untuk userId: $userId")

                    // Konversi URI ke File
                    val file = uriToFile(context, uri)

                    // Siapkan body untuk unggah multipart
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("profile_picture", file.name, requestFile)

                    // Unggah gambar profil
                    val response = RetrofitInstance.api.updateProfilePicture(userId, body)

                    if (response.isSuccessful) {
                        // Ambil data pengguna yang diperbarui
                        val updatedUser = response.body()?.data
                        if (updatedUser != null) {
                            user = updatedUser
                            // Jika gambar profil masih null, refresh data pengguna
                            if (updatedUser.profile_picture == null) {
                                delay(1000)
                                try {
                                    val refreshResponse = RetrofitInstance.api.getUsers()
                                    val refreshedUser = refreshResponse.body()?.data?.find { it.id == userId }
                                    if (refreshedUser != null) {
                                        user = refreshedUser
                                    }
                                } catch (e: Exception) {
                                    Log.e("UserManagementScreen", "Gagal refresh data pengguna: ${e.message}", e)
                                    errorMessage = "Gagal memuat ulang data pengguna"
                                }
                            }
                        }
                    } else {
                        errorMessage = "Gagal mengunggah gambar profil"
                    }
                } catch (e: Exception) {
                    Log.e("UserManagementScreen", "Error saat unggah: ${e.message}", e)
                    errorMessage = "Error saat mengunggah gambar profil"
                } finally {
                    isUploading = false
                }
            }
        }
    }

    // Ambil data pengguna saat composable diinisialisasi
    LaunchedEffect(Unit) {
        if (userId != -1) {
            coroutineScope.launch {
                try {
                    val response = RetrofitInstance.api.getUsers()
                    user = response.body()?.data?.find { it.id == userId }
                    if (user == null) {
                        errorMessage = "Pengguna tidak ditemukan"
                    }
                } catch (e: Exception) {
                    errorMessage = "Gagal memuat data pengguna"
                } finally {
                    isLoading = false
                }
            }
        } else {
            isLoading = false
            errorMessage = "Tidak ada pengguna yang login"
        }
    }

    // Fungsi logout dengan tipe eksplisit
    val logout: () -> Unit = {
        coroutineScope.launch {
            preferencesHelper.clearUserId()
            navController.navigate("login") {
                // Bersihkan seluruh back stack dan jadikan login sebagai rute awal
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // Layout utama
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF9DE)) // Latar belakang krem/kuning
            .padding(16.dp),
    ) {
        // Tombol kembali di kiri atas
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Kembali",
                tint = Color(0xFFE8A22A) // Warna oranye/kuning
            )
        }

        when {
            isLoading -> {
                // Tampilkan indikator loading
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            errorMessage != null -> {
                // Tampilkan pesan error
                Text(
                    text = errorMessage ?: "Error tidak diketahui",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            user != null -> {
                // Tampilkan konten profil pengguna
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(56.dp)) // Ruang untuk tombol kembali

                    // Gambar profil dengan tombol edit
                    Box(contentAlignment = Alignment.BottomEnd) {
                        if (!user?.profile_picture.isNullOrBlank()) {
                            // Tampilkan gambar profil dari URL
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(user?.profile_picture)
                                        .crossfade(true)
                                        .placeholder(android.R.drawable.ic_menu_gallery)
                                        .error(android.R.drawable.ic_menu_gallery)
                                        .build()
                                ),
                                contentDescription = "Gambar Profil",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            // Tampilkan placeholder jika tidak ada gambar
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user?.name?.firstOrNull()?.toString()?.uppercase() ?: "U",
                                    color = Color.White,
                                    fontSize = 40.sp
                                )
                            }
                        }

                        // Tombol edit gambar profil
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8A22A))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Gambar Profil",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Nama pengguna
                    Text(
                        text = user?.name ?: "Tidak Diketahui",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Bagian email
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "Email",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = user?.email ?: "johndoe@gmail.com",
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Bagian nama
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "Nama",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Nama",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = user?.name ?: "John Doe",
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Tombol logout di bagian bawah
                Button(
                    onClick = logout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFFE8A22A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE8A22A))
                    )
                ) {
                    Text(
                        text = "Keluar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        // Indikator saat mengunggah gambar
        if (isUploading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

// Fungsi pembantu untuk mengonversi URI ke File
private fun uriToFile(context: Context, uri: android.net.Uri): File {
    val file = File(context.cacheDir, "profile_picture_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }
    }
    return file
}