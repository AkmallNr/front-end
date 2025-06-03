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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.schedo.model.Group
import com.example.schedo.model.Project
import com.example.schedo.model.Task
import com.example.schedo.model.User
import com.example.schedo.network.ApiService
import com.example.schedo.network.RetrofitInstance
import com.example.schedo.ui.theme.Background
import com.example.schedo.util.PreferencesHelper
import com.example.schedo.utils.calculateTaskProgress
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

    // All available projects, groups, and tasks
    val allProjects = remember { mutableStateListOf<Project>() }
    val allTasks = remember { mutableStateListOf<Task>() }
    val groups = remember { mutableStateListOf<Group>() }

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
                        user = null
                    }
                } else {
                    Log.e("HomeScreen", "Failed to fetch users: ${response.errorBody()?.string()}")
                    user = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("HomeScreen", "Error fetching users: ${e.message}")
                user = null
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

    // Define fetchGroups function
    fun fetchGroups() {
        coroutineScope.launch {
            isLoading = true
            try {
                val response = RetrofitInstance.api.getGroups(userId).data
                groups.clear()
                groups.addAll(response)
                Log.d("HomeScreen", "Groups fetched successfully: ${groups.size} groups")
            } catch (e: Exception) {
                Log.e("HomeScreen", "Failed to fetch groups: ${e.message}")
                e.printStackTrace()
            } finally {
                isLoading = false
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
                Log.d("HomeScreen", "Tasks fetched successfully: ${allTasks.size} tasks for userId: $userId")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("HomeScreen", "Error fetching tasks: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    // Define logout function
    fun logout() {
        preferencesHelper.clearSession()
        navController.navigate("login") {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
        }
    }

    // Load data on initialization
    LaunchedEffect(Unit) {
        if (userId != -1) {
            fetchUserData()
            fetchProjects()
            fetchGroups()
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
                    // Completed tasks card
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

                    // Pending tasks card
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
                        modifier = Modifier.size(36.dp)
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
                            val group = groups.find { it.id == project.groupId } ?: Group(
                                id = -1,
                                name = "Unknown",
                                icon = "fas fa-users"
                            )
                            ProjectCard1(
                                project = project,
                                group = group,
                                userId = userId, // Lewatkan userId dari HomeScreen
                                groupId = project.groupId ?: 0, // Gunakan groupId dari proyek
                                apiService = RetrofitInstance.api, // Lewatkan instance ApiService
                                onClick = {
                                    val groupId = project.groupId ?: 0
                                    val projectId = project.id ?: 0
                                    navController.navigate("project_detail/$userId/$groupId/$projectId")
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
                val maxHeight = 400.dp
                val itemHeight = 48.dp
                val dynamicHeight = minOf(maxHeight, itemHeight * allProjects.size.coerceAtMost(8))

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
fun ProjectCard1(
    project: Project,
    group: Group,
    userId: Int,
    groupId: Int,
    apiService: ApiService,
    onClick: () -> Unit
) {
    val projectColor = when ((project.id ?: 0) % 3) {
        0 -> Color(0xFFFFA0A0) to Color(0xFFD0D0D0) // Red
        1 -> Color(0xFFA0C4FF) to Color(0xFFD0E0FF) // Blue
        else -> Color(0xFFA0FFA0) to Color(0xFFD0FFD0) // Green
    }

    val iconMapping = mapOf(
        "fas fa-users" to Icons.Default.Group,
        "fas fa-folder" to Icons.Default.Folder,
        "fas fa-star" to Icons.Default.Star,
        "fas fa-home" to Icons.Default.Home,
        "fas fa-tasks" to Icons.Default.List,
        "fas fa-calendar" to Icons.Default.CalendarMonth,
        "fas fa-book" to Icons.Default.Book,
        "fas fa-bell" to Icons.Default.Notifications,
        "fas fa-heart" to Icons.Default.Favorite,
        "fas fa-check" to Icons.Default.CheckCircle,
        "fas fa-envelope" to Icons.Default.Email,
        "fas fa-image" to Icons.Default.Image,
        "fas fa-file" to Icons.Default.Description,
        "fas fa-clock" to Icons.Default.AccessTime,
        "fas fa-cog" to Icons.Default.Settings,
        "fas fa-shopping-cart" to Icons.Default.ShoppingCart,
        "fas fa-tag" to Icons.Default.LocalOffer,
        "fas fa-link" to Icons.Default.Link,
        "fas fa-map" to Icons.Default.Place,
        "fas fa-music" to Icons.Default.MusicNote,
        "fas fa-phone" to Icons.Default.Call,
        "fas fa-camera" to Icons.Default.PhotoCamera,
        "fas fa-search" to Icons.Default.Search,
        "fas fa-cloud" to Icons.Default.Cloud,
        "fas fa-person" to Icons.Default.Person
    )

    val selectedIcon = iconMapping[group.icon?.lowercase() ?: "fas fa-users"] ?: Icons.Default.Laptop

    // State untuk menyimpan daftar tugas
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    // Ambil data tugas saat composable dirender
    LaunchedEffect(project.id) {
        coroutineScope.launch {
            try {
                val response = apiService.getTaskByProject(userId, groupId, project.id ?: 0)
                tasks = response.data
            } catch (e: Exception) {
                // Tangani error (misalnya, log atau tampilkan pesan)
                tasks = emptyList()
            }
        }
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
                    imageVector = selectedIcon,
                    contentDescription = "${group.name} Icon",
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

                Text(
                    formatDateRange(project.startDate, project.endDate),
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            // Progress indicator berdasarkan tugas yang selesai
            Box(
                modifier = Modifier
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                val progress = calculateTaskProgress(tasks) / 100f
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(40.dp),
                    color = projectColor.first,
                    strokeWidth = 3.dp,
                    trackColor = Color.LightGray.copy(alpha = 0.3f)
                )
                Text(
                    "${(progress * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

