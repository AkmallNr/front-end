package com.example.schedo.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.schedo.model.Project
import com.example.schedo.model.Task
import com.example.schedo.model.User
import com.example.schedo.network.RetrofitInstance
import com.example.schedo.ui.theme.Background
import com.example.schedo.util.PreferencesHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val preferencesHelper = remember { PreferencesHelper(context) }
    val userId = preferencesHelper.getUserId()
    var user by remember { mutableStateOf<User?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var shouldRefreshUserData by remember { mutableStateOf(false) }

    // All available projects
    val allProjects = remember { mutableStateListOf<Project>() }
    val allTasks = remember { mutableStateListOf<Task>() }

    // Projects selected for display on home screen
    val selectedProjects = remember { mutableStateListOf<Project>() }

    var isProjectsLoading by remember { mutableStateOf(false) }
    var showAllProjects by remember { mutableStateOf(false) }

    // State for project selection dialog
    var showProjectSelectionDialog by remember { mutableStateOf(false) }

    // Define fetchUserData function
    fun fetchUserData() {
        coroutineScope.launch {
            isLoading = true
            try {
                Log.d("HomeScreen", "Fetching user data for userId: $userId")
                val response = RetrofitInstance.api.getUsers()
                if (response.isSuccessful) {
                    val userData = response.body()?.data?.find { it.id == userId }
                    if (userData != null) {
                        user = userData
                        Log.d("HomeScreen", "Fetched user: $userData")
                        if (userData.profile_picture == null) {
                            Log.d("HomeScreen", "Profile picture is null, will refresh data later")
                            if (!shouldRefreshUserData) {
                                shouldRefreshUserData = true
                                delay(1000)
                                fetchUserData()
                            }
                        } else {
                            shouldRefreshUserData = false
                        }
                    } else {
                        Log.w("HomeScreen", "User not found for userId: $userId")
                        // Jika user tidak ditemukan, anggap sebagai guest
                        user = null
                    }
                } else {
                    Log.e("HomeScreen", "Failed to fetch users: ${response.errorBody()?.string()}")
                    user = null // Anggap sebagai guest jika gagal
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("HomeScreen", "Error fetching users: ${e.message}")
                user = null // Anggap sebagai guest jika error
            } finally {
                isLoading = false
            }
        }
    }

    // Define fetchProjects function
    fun fetchProjects() {
        coroutineScope.launch {
            isProjectsLoading = true
            try {
                val response = RetrofitInstance.api.getProjectsByUser(userId)
                allProjects.clear()
                allProjects.addAll(response.data)

                // Load saved selected projects or default to empty if none saved
                val savedProjectIds = preferencesHelper.getSelectedProjectIds().mapNotNull { it.toIntOrNull() }
                val initialSelected = allProjects.filter { project ->
                    project.id?.let { savedProjectIds.contains(it) } ?: false
                }
                selectedProjects.clear()
                selectedProjects.addAll(initialSelected)

                Log.d("HomeScreen", "Projects fetched successfully: ${allProjects.size} projects")
            } catch (e: Exception) {
                Log.e("HomeScreen", "Failed to fetch projects: ${e.message}")
                e.printStackTrace()
            } finally {
                isProjectsLoading = false
            }
        }
    }

    // Define fetchTask function
    fun fetchTask() {
        coroutineScope.launch {
            isLoading = true
            try {
                val response = RetrofitInstance.api.getTaskByUser(userId).data
                allTasks.clear()
                allTasks.addAll(response)
                println("Task fetched successfully: ${allTasks.size} task for userId: $userId")
                println("isi Task: ${allTasks}")
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    // Define logout function
    fun logout() {
        preferencesHelper.clearSession() // Hapus semua data sesi
        navController.navigate("login") {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
        }
    }

    // Load saved selected project IDs on initialization
    LaunchedEffect(Unit) {
        if (userId != -1) {
            fetchUserData()
            fetchProjects()
            fetchTask()
        }
    }

    // Save selected projects to preferences
    fun saveSelectedProjects(projects: List<Project>) {
        selectedProjects.clear()
        selectedProjects.addAll(projects)
        val projectIds = projects.mapNotNull { it.id }.joinToString(",")
        preferencesHelper.saveSelectedProjectIds(projectIds)
        Log.d("HomeScreen", "Saved ${selectedProjects.size} projects as shortcuts")
    }

    Scaffold { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Background)
            ) {
                // Top bar with greeting and icons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        } else if (user?.profile_picture != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(user?.profile_picture)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user?.name?.firstOrNull()?.toString() ?: "?",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Hello !", fontSize = 16.sp, color = Color.DarkGray)
                            if (isLoading) {
                                LinearProgressIndicator(
                                    modifier = Modifier.width(80.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else if (user != null && userId != -1) {
                                Text("${user!!.name}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Guest", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "Log in to access your projects",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.clickable {
                                        navController.navigate("login") {
                                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Row {
                        IconButton(onClick = { /* Handle notifications */ }) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.Black
                            )
                        }
                        IconButton(onClick = { logout() }) {
                            Icon(
                                imageVector = Icons.Filled.ExitToApp,
                                contentDescription = "Logout",
                                tint = Color.Black
                            )
                        }
                    }
                }

                // Task summary section
                Text(
                    "Ringkasan Tugas",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Menghitung tugas selesai dan tertunda
                val completedTasks = allTasks.count { it.status == true }
                val pendingTasks = allTasks.count { it.status == false || it.status == null }

                // Task summary cards in a more compact row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Completed tasks card - smaller version
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE6F1FA)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                completedTasks.toString(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                "Tugas Selesai",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Pending tasks card - smaller version
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE6F1FA)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                pendingTasks.toString(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                "Tugas Tertunda",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Project Groups section with + button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Project Groups",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "${selectedProjects.size}",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    IconButton(
                        onClick = { showProjectSelectionDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add Project Shortcut",
                            tint = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Project Group cards
                if (isProjectsLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (selectedProjects.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No projects selected", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(selectedProjects) { project ->
                            ProjectCard(
                                project = project,
                                onClick = {
                                    navController.navigate("schedule/${userId}/${project.groupId ?: 0}/${project.id ?: 0}")
                                }
                            )
                        }
                    }
                }
            }

            // Project Selection Dialog
            if (showProjectSelectionDialog) {
                ProjectSelectionDialog(
                    allProjects = allProjects,
                    currentlySelected = selectedProjects,
                    onDismiss = { showProjectSelectionDialog = false },
                    onConfirm = { selected ->
                        saveSelectedProjects(selected)
                        showProjectSelectionDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun ProjectSelectionDialog(
    allProjects: List<Project>,
    currentlySelected: List<Project>,
    onDismiss: () -> Unit,
    onConfirm: (List<Project>) -> Unit
) {
    val selectedIds = remember { mutableStateListOf<Int>() }

    // Initialize with currently selected project IDs
    LaunchedEffect(currentlySelected) {
        selectedIds.clear()
        currentlySelected.forEach { project ->
            project.id?.let { selectedIds.add(it) }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                // Dialog header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Select Projects",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Dynamic height with scrollable LazyColumn
                val maxHeight = 400.dp // Maximum height before scrolling
                val itemHeight = 48.dp // Estimated height per project item
                val dynamicHeight = minOf(maxHeight, itemHeight * allProjects.size.coerceAtMost(8)) // Limit to 8 items visible

                LazyColumn(
                    modifier = Modifier
                        .heightIn(min = 0.dp, max = dynamicHeight)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allProjects) { project ->
                        project.id?.let { projectId ->
                            val isSelected = selectedIds.contains(projectId)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSelected) {
                                            selectedIds.remove(projectId)
                                        } else {
                                            selectedIds.add(projectId)
                                        }
                                    }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    project.name ?: "Unnamed Project",
                                    fontSize = 16.sp
                                )

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFFFFA726)
                                    )
                                }
                            }

                            Divider()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val selectedProjects = allProjects.filter { project ->
                                project.id?.let { selectedIds.contains(it) } ?: false
                            }
                            onConfirm(selectedProjects)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFA726)
                        )
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    onClick: () -> Unit
) {
    val projectColor = when ((project.id ?: 0) % 3) {
        0 -> Color(0xFFFFA0A0) to Color(0xFFD0D0D0) // Red
        1 -> Color(0xFFA0C4FF) to Color(0xFFD0E0FF) // Blue
        else -> Color(0xFFA0FFA0) to Color(0xFFD0FFD0) // Green
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    .background(projectColor.second),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Laptop,
                    contentDescription = null,
                    tint = projectColor.first
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    project.name ?: "Unnamed Project",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Format the date with the helper function
                val formattedDate = formatDateRange(project.startDate, project.endDate)
                Text(
                    formattedDate,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            // Calculate the progress properly (or use project.progress if available)
//            val progress = project.progress?.toFloat()?.div(100f) ?: 0.01f
            Box(
                modifier = Modifier
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
//                    progress = { progress },
                    modifier = Modifier.size(40.dp),
                    color = projectColor.first,
                    strokeWidth = 3.dp,
                    trackColor = Color.LightGray.copy(alpha = 0.3f)
                )
                Text(
                    "2%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Date formatter helper function with Asia/Jakarta timezone
fun formatDateRange(startDate: String?, endDate: String?): String {
    if (startDate == null || endDate == null) {
        return "No date available"
    }

    return try {
        // Step 1: Determine the format pattern based on input
        val dateFormat = when {
            // ISO 8601 with milliseconds: 2025-05-11T00:00:00.000000Z
            startDate.contains("T") && startDate.contains(".") ->
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())

            // ISO 8601 without milliseconds: 2025-05-11T00:00:00Z
            startDate.contains("T") && !startDate.contains(".") ->
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())

            // Simple date time format: 2025-05-13 00:00:00
            startDate.contains(" ") ->
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            // Simple date only: 2025-05-13
            else ->
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        }

        // Step 2: Set timezone for parsing - use UTC for ISO format with Z
        if (startDate.endsWith("Z")) {
            dateFormat.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }

        // Step 3: Parse the dates
        val startDateTime = dateFormat.parse(startDate)
        val endDateTime = dateFormat.parse(endDate)

        // Step 4: Format with Asia/Jakarta timezone
        val outputFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
        outputFormat.timeZone = TimeZone.getTimeZone("Asia/Jakarta")

        // Step 5: Generate formatted output
        val formattedStart = outputFormat.format(startDateTime)
        val formattedEnd = outputFormat.format(endDateTime)

        "$formattedStart - $formattedEnd"
    } catch (e: Exception) {
        Log.e("DateFormat", "Error in primary formatter: ${e.message}")

        // Fallback method - try to extract parts directly
        try {
            // For format without T separator: 2025-05-13 00:00:00
            val cleanStartDate = startDate.replace("T", " ").substringBefore(".")
            val cleanEndDate = endDate.replace("T", " ").substringBefore(".")

            val fallbackFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val fallbackOutputFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())

            val parsedStart = fallbackFormat.parse(cleanStartDate)
            val parsedEnd = fallbackFormat.parse(cleanEndDate)

            if (parsedStart != null && parsedEnd != null) {
                "${fallbackOutputFormat.format(parsedStart)} - ${fallbackOutputFormat.format(parsedEnd)}"
            } else {
                "Invalid date format"
            }
        } catch (e2: Exception) {
            Log.e("DateFormat", "Both formatters failed: ${e2.message}")
            // If all parsing fails, return raw date strings
            "${startDate.replace("T", " ").substringBefore(".")} - ${endDate.replace("T", " ").substringBefore(".")}"
        }
    }
}