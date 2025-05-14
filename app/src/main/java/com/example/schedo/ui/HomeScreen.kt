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
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.schedo.model.Project
import com.example.schedo.model.User
import com.example.schedo.network.RetrofitInstance
import com.example.schedo.ui.theme.Background
import com.example.schedo.ui.theme.Utama2
import com.example.schedo.util.PreferencesHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val preferencesHelper = remember { PreferencesHelper(context) }
    val userId = preferencesHelper.getUserId().toInt()
    var user by remember { mutableStateOf<User?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var shouldRefreshUserData by remember { mutableStateOf(false) }

    // All available projects
    val allProjects = remember { mutableStateListOf<Project>() }

    // Projects selected for display on home screen
    val selectedProjects = remember { mutableStateListOf<Project>() }

    var isProjectsLoading by remember { mutableStateOf(false) }
    var showAllProjects by remember { mutableStateOf(false) }

    // State for project selection dialog
    var showProjectSelectionDialog by remember { mutableStateOf(false) }

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

                        // Check if profile picture is null and set a flag to refresh later
                        if (userData.profile_picture == null) {
                            Log.d("HomeScreen", "Profile picture is null, will refresh data later")
                            if (!shouldRefreshUserData) {
                                shouldRefreshUserData = true
                                // Give the server some time to process any pending updates
                                delay(1000)
                                fetchUserData() // Recursive call to refresh data
                            }
                        } else {
                            shouldRefreshUserData = false
                        }
                    } else {
                        Log.w("HomeScreen", "User not found for userId: $userId")
                    }
                } else {
                    Log.e("HomeScreen", "Failed to fetch users: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("HomeScreen", "Error fetching users: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    fun fetchProjects() {
        coroutineScope.launch {
            isProjectsLoading = true
            try {
                val response = RetrofitInstance.api.getProjectsByUser(userId)
                allProjects.clear()
                allProjects.addAll(response.data)

                // Initially, select up to 3 projects to display
                selectedProjects.clear()
                selectedProjects.addAll(allProjects.take(3))

                Log.d("HomeScreen", "Projects fetched successfully: ${allProjects.size} projects")
            } catch (e: Exception) {
                Log.e("HomeScreen", "Failed to fetch projects: ${e.message}")
                e.printStackTrace()
            } finally {
                isProjectsLoading = false
            }
        }
    }

    // Function to save selected projects (in a real app, this would persist to local storage or backend)
    fun saveSelectedProjects(projects: List<Project>) {
        selectedProjects.clear()
        selectedProjects.addAll(projects)
        // In a real implementation, you might want to save this to SharedPreferences or your backend
        Log.d("HomeScreen", "Saved ${selectedProjects.size} projects as shortcuts")
    }

    LaunchedEffect(Unit) {
        fetchUserData()
        fetchProjects()
    }

    Scaffold(
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Background) // Light cream background like in the image
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
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Fallback if no profile picture
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
                            } else if (user != null) {
                                Text("${user!!.name}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Guest", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    IconButton(onClick = { /* Handle notifications */ }) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.Black
                        )
                    }
                }

                // Task progress card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFA726)) // Orange color from image
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "Your today's task",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "almost done!",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { 0.83f },
                                    modifier = Modifier.size(60.dp),
                                    color = Color.White,
                                    strokeWidth = 4.dp
                                )
                                Text(
                                    "83%",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { /* Navigate to task view */ },
                            modifier = Modifier
                                .align(Alignment.Start)
                                .height(36.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("View Task", fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                            "Group Task",
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
                            tint = Utama2
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
                        // Display only selected projects
                        items(selectedProjects) { project ->
                            ProjectCard(
                                project = project,
                                onClick = {
                                    // Navigate to project detail
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

                // Project selection list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
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
        0 -> Color(0xFFFFA0A0) to Color(0xFFFFD0D0) // Red
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
            // Icon placeholder
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

                val startDate = project.startDate ?: "No start date"
                val endDate = project.endDate ?: "No end date"
                Text(
                    "$startDate - $endDate",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            // Progress indicator (using a random progress for now)
            val progress = ((project.id ?: 0) % 100) / 100f
            Box(
                modifier = Modifier
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
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