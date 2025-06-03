package com.example.schedo.ui

import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.schedo.model.Group
import com.example.schedo.model.User
import com.example.schedo.network.GroupRequest
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
fun UserManagementScreen(navController: NavHostController) {
    val context = LocalContext.current
    val preferencesHelper = PreferencesHelper(context)
    val userId = preferencesHelper.getUserId()
    val coroutineScope = rememberCoroutineScope()

    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var showEditGroupDialog by remember { mutableStateOf(false) }
    var userGroups by remember { mutableStateOf<List<Group>>(emptyList()) }
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var showGroupOptionsDialog by remember { mutableStateOf(false) }

    // Launcher untuk memilih gambar
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                isUploading = true
                try {
                    Log.d("UserManagementScreen", "Mengunggah foto profil untuk userId: $userId")
                    val file = uriToFile(context, uri)
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("profile_picture", file.name, requestFile)
                    val response = RetrofitInstance.api.updateProfilePicture(userId, body)

                    if (response.isSuccessful) {
                        val updatedUser = response.body()?.data
                        if (updatedUser != null) {
                            user = updatedUser
                            if (updatedUser.profile_picture == null) {
                                delay(1000)
                                try {
                                    val refreshResponse = RetrofitInstance.api.getUsers()
                                    val refreshedUserList = refreshResponse.body()?.data ?: emptyList()
                                    val refreshedUser = refreshedUserList.find { it.id == userId }
                                    if (refreshedUser != null) {
                                        user = refreshedUser
                                    }
                                } catch (e: Exception) {
                                    Log.e("UserManagementScreen", "Gagal memuat ulang data user: ${e.message}", e)
                                    errorMessage = "Gagal memuat ulang data user"
                                }
                            }
                        } else {
                            errorMessage = "Data user tidak ditemukan dalam respons"
                        }
                    } else {
                        errorMessage = "Gagal mengunggah foto profil: ${response.code()} - ${response.message()}"
                    }
                } catch (e: Exception) {
                    Log.e("UserManagementScreen", "Error unggah: ${e.message}", e)
                    errorMessage = "Error mengunggah foto profil"
                } finally {
                    isUploading = false
                }
            }
        }
    }

    // Mengambil data user dan grup
    LaunchedEffect(Unit) {
        if (userId != -1) {
            coroutineScope.launch {
                try {
                    // Mengambil data user
                    val userResponse = RetrofitInstance.api.getUsers()
                    val userList = userResponse.body()?.data ?: emptyList()
                    user = userList.find { it.id == userId }

                    // Mengambil data grup
                    val groupsResponse = RetrofitInstance.api.getGroups(userId)
                    userGroups = groupsResponse.data ?: emptyList()

                    if (user == null) {
                        errorMessage = "User tidak ditemukan"
                    }
                } catch (e: Exception) {
                    errorMessage = "Gagal memuat data user: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        } else {
            isLoading = false
            errorMessage = "Tidak ada user yang login"
        }
    }

    // Fungsi logout
    val logout: () -> Unit = {
        coroutineScope.launch {
            preferencesHelper.clearUserId()
            navController.navigate("login") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // Layout utama
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF9DE))
            .padding(16.dp),
    ) {
        // Tombol kembali
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Kembali",
                tint = Color(0xFFE8A22A)
            )
        }

        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            errorMessage != null -> {
                Text(
                    text = errorMessage ?: "Error tidak diketahui",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            user != null -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(56.dp))

                    // Foto profil
                    Box(contentAlignment = Alignment.BottomEnd) {
                        if (!user?.profile_picture.isNullOrBlank()) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(user?.profile_picture)
                                        .crossfade(true)
                                        .placeholder(android.R.drawable.ic_menu_gallery)
                                        .error(android.R.drawable.ic_menu_gallery)
                                        .build()
                                ),
                                contentDescription = "Foto Profil",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                            )
                        } else {
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

                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8A22A))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Foto Profil",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Nama user
                    Text(
                        text = user?.name ?: "Tidak Diketahui",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Bagian email
                    UserInfoRow(
                        icon = Icons.Default.Email,
                        title = "Email",
                        value = user?.email ?: "johndoe@gmail.com"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bagian nama
                    UserInfoRow(
                        icon = Icons.Default.Person,
                        title = "Nama",
                        value = user?.name ?: "John Doe"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tombol Edit Grup
                    Button(
                        onClick = { showEditGroupDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE8A22A),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Edit Grup")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Tombol Logout di bagian bawah
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
                        text = "Logout",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        // Indikator unggah
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

        // Dialog Edit Grup
        if (showEditGroupDialog) {
            Dialog(onDismissRequest = { showEditGroupDialog = false }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Grup Anda",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (userGroups.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Tidak ada grup yang ditemukan")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(userGroups) { group ->
                                    GroupCard(
                                        group = group,
                                        onClick = {
                                            selectedGroup = group
                                            showGroupOptionsDialog = true
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showEditGroupDialog = false },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE8A22A),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Tutup")
                        }
                    }
                }
            }
        }

        // Dialog Pilihan Grup
        if (showGroupOptionsDialog && selectedGroup != null) {
            GroupOptionsDialog(
                group = selectedGroup!!,
                userId = userId,
                onEdit = { updatedGroup ->
                    coroutineScope.launch {
                        try {
                            // Refresh group list from server after update
                            val groupsResponse = RetrofitInstance.api.getGroups(userId)
                            userGroups = groupsResponse.data ?: emptyList()
                            Log.d("GroupOptionsDialog", "Refreshed group list after update: ${userGroups.size} groups")
                        } catch (e: Exception) {
                            errorMessage = "Gagal memperbarui grup: ${e.message}"
                            Log.e("GroupOptionsDialog", "Failed to refresh group list: ${e.message}", e)
                        }
                        showGroupOptionsDialog = false
                        showEditGroupDialog = false
                    }
                },
                onDelete = {
                    coroutineScope.launch {
                        try {
                            selectedGroup!!.id?.let { RetrofitInstance.api.deleteGroup(userId, it) }
                            val groupsResponse = RetrofitInstance.api.getGroups(userId)
                            userGroups = groupsResponse.data ?: emptyList()
                            Log.d("GroupOptionsDialog", "Refreshed group list after delete: ${userGroups.size} groups")
                        } catch (e: Exception) {
                            errorMessage = "Gagal menghapus grup: ${e.message}"
                            Log.e("GroupOptionsDialog", "Failed to refresh group list after delete: ${e.message}", e)
                        }
                        showGroupOptionsDialog = false
                    }
                },
                onDismiss = { showGroupOptionsDialog = false }
            )
        }
    }
}

@Composable
fun UserInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun GroupCard(group: Group, onClick: () -> Unit) {
    val fontAwesomeIcons = listOf(
        Pair("fas fa-users", Icons.Default.Group),
        Pair("fas fa-folder", Icons.Default.Folder),
        Pair("fas fa-star", Icons.Default.Star),
        Pair("fas fa-home", Icons.Default.Home),
        Pair("fas fa-tasks", Icons.Default.List),
        Pair("fas fa-calendar", Icons.Default.CalendarMonth),
        Pair("fas fa-book", Icons.Default.Book),
        Pair("fas fa-bell", Icons.Default.Notifications),
        Pair("fas fa-heart", Icons.Default.Favorite),
        Pair("fas fa-check", Icons.Default.CheckCircle),
        Pair("fas fa-envelope", Icons.Default.Email),
        Pair("fas fa-image", Icons.Default.Image),
        Pair("fas fa-file", Icons.Default.Description),
        Pair("fas fa-clock", Icons.Default.AccessTime),
        Pair("fas fa-cog", Icons.Default.Settings),
        Pair("fas fa-shopping-cart", Icons.Default.ShoppingCart),
        Pair("fas fa-tag", Icons.Default.LocalOffer),
        Pair("fas fa-link", Icons.Default.Link),
        Pair("fas fa-map", Icons.Default.Place),
        Pair("fas fa-music", Icons.Default.MusicNote),
        Pair("fas fa-phone", Icons.Default.Call),
        Pair("fas fa-camera", Icons.Default.PhotoCamera),
        Pair("fas fa-search", Icons.Default.Search),
        Pair("fas fa-cloud", Icons.Default.Cloud),
        Pair("fas fa-person", Icons.Default.Person)
    )

    val icon = fontAwesomeIcons.find { it.first == group.icon }?.second ?: Icons.Default.Group

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8A22A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = group.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = "Lihat Grup",
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun GroupOptionsDialog(
    group: Group,
    userId: Int,
    onEdit: (Group) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pilihan Grup",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A4A4A)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Pilih aksi untuk ${group.name}",
                    fontSize = 16.sp,
                    color = Color(0xFF757575)
                )
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White,
        confirmButton = {
            Button(
                onClick = { showEditDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE8A22A),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("Edit")
            }
        },
        dismissButton = {
            Row {
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE57373),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Hapus")
                }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE8A22A),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Batal")
                }
            }
        }
    )

    if (showEditDialog) {
        EditGroupDialog(
            group = group,
            userId = userId,
            onDismiss = { showEditDialog = false },
            onGroupUpdated = { updatedGroup ->
                onEdit(updatedGroup)
                showEditDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGroupDialog(
    group: Group,
    userId: Int,
    onDismiss: () -> Unit,
    onGroupUpdated: (Group) -> Unit
) {
    var name by remember { mutableStateOf(group.name) }
    var selectedIcon by remember { mutableStateOf(group.icon) }
    var iconExpanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val fontAwesomeIcons = listOf(
        Pair("fas fa-users", Icons.Default.Group),
        Pair("fas fa-folder", Icons.Default.Folder),
        Pair("fas fa-star", Icons.Default.Star),
        Pair("fas fa-home", Icons.Default.Home),
        Pair("fas fa-tasks", Icons.Default.List),
        Pair("fas fa-calendar", Icons.Default.CalendarMonth),
        Pair("fas fa-book", Icons.Default.Book),
        Pair("fas fa-bell", Icons.Default.Notifications),
        Pair("fas fa-heart", Icons.Default.Favorite),
        Pair("fas fa-check", Icons.Default.CheckCircle),
        Pair("fas fa-envelope", Icons.Default.Email),
        Pair("fas fa-image", Icons.Default.Image),
        Pair("fas fa-file", Icons.Default.Description),
        Pair("fas fa-clock", Icons.Default.AccessTime),
        Pair("fas fa-cog", Icons.Default.Settings),
        Pair("fas fa-shopping-cart", Icons.Default.ShoppingCart),
        Pair("fas fa-tag", Icons.Default.LocalOffer),
        Pair("fas fa-link", Icons.Default.Link),
        Pair("fas fa-map", Icons.Default.Place),
        Pair("fas fa-music", Icons.Default.MusicNote),
        Pair("fas fa-phone", Icons.Default.Call),
        Pair("fas fa-camera", Icons.Default.PhotoCamera),
        Pair("fas fa-search", Icons.Default.Search),
        Pair("fas fa-cloud", Icons.Default.Cloud),
        Pair("fas fa-person", Icons.Default.Person)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Grup",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A4A4A)
            )
        },
        text = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                modifier = Modifier.padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Grup", color = Color(0xFF757575)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFFE8A22A),
                            unfocusedBorderColor = Color(0xFFB0BEC5)
                        ),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { keyboardController?.hide() })
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        val currentIcon = fontAwesomeIcons.find { it.first == selectedIcon }?.second ?: Icons.Default.Group
                        OutlinedButton(
                            onClick = { iconExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF757575)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = currentIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pilih Ikon", modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = iconExpanded,
                            onDismissRequest = { iconExpanded = false },
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                                .heightIn(max = 300.dp)
                                .width(280.dp)
                        ) {
                            val rows = 5
                            val columns = 5
                            for (rowIndex in 0 until rows) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    for (colIndex in 0 until columns) {
                                        val index = rowIndex * columns + colIndex
                                        if (index < fontAwesomeIcons.size) {
                                            val (iconId, materialIcon) = fontAwesomeIcons[index]
                                            IconButton(
                                                onClick = {
                                                    selectedIcon = iconId
                                                    iconExpanded = false
                                                },
                                                modifier = Modifier
                                                    .padding(4.dp)
                                                    .size(40.dp)
                                                    .background(
                                                        if (selectedIcon == iconId) Color(0xFFE8A22A).copy(alpha = 0.1f)
                                                        else Color.Transparent,
                                                        RoundedCornerShape(8.dp)
                                                    )
                                            ) {
                                                Icon(
                                                    imageVector = materialIcon,
                                                    contentDescription = null,
                                                    tint = if (selectedIcon == iconId) Color(0xFFE8A22A) else Color(0xFF757575),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.size(40.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFD32F2F),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty() && !isLoading) {
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                val groupRequest = GroupRequest(name = name, icon = selectedIcon)
                                Log.d("EditGroupDialog", "Attempting to update group ID: ${group.id}, User ID: $userId, Name: $name, Icon: $selectedIcon")
                                val response = group.id?.let {
                                    RetrofitInstance.api.updateGroup(userId, it, groupRequest)
                                }
                                if (response != null) {
                                    if (response.isSuccessful) {
                                        val updatedGroup = response.body()!!
                                        Log.d("EditGroupDialog", "Group updated successfully: ${updatedGroup.id}, Name: ${updatedGroup.name}, Icon: ${updatedGroup.icon}")
                                        onGroupUpdated(updatedGroup)
                                    } else {
                                        val errorBody = response.errorBody()?.string() ?: "No error body"
                                        Log.e("EditGroupDialog", "Failed to update group. HTTP Code: ${response.code()}, Message: ${response.message()}, Error Body: $errorBody")
                                        errorMessage = "Gagal memperbarui grup: ${response.message()} (Code: ${response.code()})"
                                    }
                                } else {
                                    Log.e("EditGroupDialog", "Group ID is null, cannot update group")
                                    errorMessage = "Gagal memperbarui grup: ID grup tidak valid"
                                }
                            } catch (e: Exception) {
                                Log.e("EditGroupDialog", "Exception during group update: ${e.message}", e)
                                errorMessage = "Error: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                        keyboardController?.hide()
                    } else {
                        Log.w("EditGroupDialog", "Group name is empty or update is already in progress")
                        errorMessage = "Nama grup tidak boleh kosong"
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE8A22A),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Simpan")
                }
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE8A22A),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Batal")
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color(0xFFF5F5F5)
    )
}

private fun uriToFile(context: Context, uri: android.net.Uri): File {
    val file = File(context.cacheDir, "profile_picture_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }
    }
    return file
}