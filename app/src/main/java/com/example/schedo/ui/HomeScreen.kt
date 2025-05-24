package com.example.schedo.ui

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.example.schedo.network.WeeklyCompletedTasksData
import com.example.schedo.ui.theme.Background
import com.example.schedo.util.PreferencesHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import java.text.SimpleDateFormat
import java.util.*


@Composable
fun WeeklyTasksBarChart(data: WeeklyCompletedTasksData) {
    val daysOrder = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val dayLabels = listOf("S", "S", "R", "K", "J", "S", "M")
    val tasksCount = daysOrder.map { data.tasks[it] ?: 0 }
    val maxTasks = (tasksCount.maxOrNull()?.toFloat() ?: 1f).let { if (it == 0f) 1f else it }

    val barColors = listOf(
        Color(0xFFFF7043), Color(0xFF388E3C), Color(0xFF66BB6A),
        Color(0xFFFFCA28), Color(0xFFEC407A), Color(0xFF1976D2),
        Color(0xFF42A5F5)
    )

    val chartHeight = 200.dp
    val minBarHeight = 10.dp

    val minBarHeightPx = with(LocalDensity.current) { minBarHeight.toPx() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
        }
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .background(Color.White, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val barWidth = canvasWidth / 10f
                val spacing = canvasWidth / 14f
                val maxHeightPx = canvasHeight * 0.8f

                val yStep = if (maxTasks <= 1) 1f else (maxTasks / 4f).coerceAtLeast(1f)
                val yMax = (maxTasks / yStep).toInt().coerceAtLeast(1) * yStep

                for (i in 0..(yMax / yStep).toInt()) {
                    val yValue = i * yStep
                    val yPos = canvasHeight - (yValue / yMax * maxHeightPx)
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(0f, yPos),
                        end = Offset(canvasWidth, yPos),
                        strokeWidth = 1f
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        String.format("%.1f", yValue),
                        0f,
                        yPos + 15f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 20f
                        }
                    )
                }

                tasksCount.forEachIndexed { index, count ->
                    val barHeightPx = (count / maxTasks * maxHeightPx).coerceAtLeast(minBarHeightPx)
                    val left = index * spacing + (spacing - barWidth) / 2
                    drawRect(
                        color = barColors[index % barColors.size],
                        topLeft = Offset(left, canvasHeight - barHeightPx),
                        size = Size(barWidth, barHeightPx)
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        count.toString(),
                        left + barWidth / 2,
                        canvasHeight - barHeightPx - 10f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 20f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }

                dayLabels.forEachIndexed { index, label ->
                    val xPos = index * spacing + spacing / 2
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        xPos,
                        canvasHeight - 10f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 20f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val preferencesHelper = remember { PreferencesHelper(context) }
    val userId = preferencesHelper.getUserId()
    var user by remember { mutableStateOf<User?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var shouldRefreshUserData by remember { mutableStateOf(false) }

    val allProjects = remember { mutableStateListOf<Project>() }
    val allTasks = remember { mutableStateListOf<Task>() }
    val selectedProjects = remember { mutableStateListOf<Project>() }

    var isProjectsLoading by remember { mutableStateOf(false) }
    var showAllProjects by remember { mutableStateOf(false) }
    var showProjectSelectionDialog by remember { mutableStateOf(false) }

    var weeklyCompletedTasks by remember { mutableStateOf<WeeklyCompletedTasksData?>(null) }
    var isWeeklyTasksLoading by remember { mutableStateOf(false) }
    var currentWeekStart by remember { mutableStateOf(Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time) }

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

    fun fetchProjects() {
        coroutineScope.launch {
            isProjectsLoading = true
            try {
                val response = RetrofitInstance.api.getProjectsByUser(userId)
                allProjects.clear()
                allProjects.addAll(response.data)
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

    fun fetchWeeklyCompletedTasks(weekStart: Date) {
        coroutineScope.launch {
            isWeeklyTasksLoading = true
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val weekStartString = dateFormat.format(weekStart)
                val response = RetrofitInstance.api.getWeeklyCompletedTasks(userId, weekStartString)
                if (response.isSuccessful) {
                    weeklyCompletedTasks = response.body()?.data
                    Log.d("HomeScreen", "Weekly completed tasks fetched: ${weeklyCompletedTasks}")
                } else {
                    Log.e("HomeScreen", "Failed to fetch weekly completed tasks: ${response.code()}")
                    Toast.makeText(context, "Gagal memuat tugas selesai mingguan", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("HomeScreen", "Error fetching weekly completed tasks: ${e.message}")
                e.printStackTrace()
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isWeeklyTasksLoading = false
            }
        }
    }

    fun logout() {
        preferencesHelper.clearSession()
        navController.navigate("login") {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
        }
    }

    LaunchedEffect(Unit) {
        if (userId != -1) {
            fetchUserData()
            fetchProjects()
            fetchTask()
            fetchWeeklyCompletedTasks(currentWeekStart)
        }
    }

    LaunchedEffect(currentWeekStart) {
        if (userId != -1) {
            fetchWeeklyCompletedTasks(currentWeekStart)
        }
    }

    fun saveSelectedProjects(projects: List<Project>) {
        selectedProjects.clear()
        selectedProjects.addAll(projects)
        val projectIds = projects.mapNotNull { it.id }.joinToString(",")
        preferencesHelper.saveSelectedProjectIds(projectIds)
        Log.d("HomeScreen", "Saved ${selectedProjects.size} projects as shortcuts")
    }

    Scaffold { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 50.dp)
            ) {
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

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Ringkasan Tugas",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                val completedTasks = allTasks.count { it.status == true }
                val pendingTasks = allTasks.count { it.status == false || it.status == null }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F1FA))
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
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F1FA))
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

                // Bagian "Tugas Selesai di Minggu Ini" dipindahkan ke sini
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Tugas Selesai di Minggu Ini",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (isWeeklyTasksLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (weeklyCompletedTasks == null) {
                    Text(
                        "Gagal memuat tugas selesai",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val calendar = Calendar.getInstance().apply { time = currentWeekStart }
                                    calendar.add(Calendar.WEEK_OF_YEAR, -1)
                                    currentWeekStart = calendar.time
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = "Previous Week",
                                    tint = Color.Black
                                )
                            }
                            Text(
                                text = "< ${weeklyCompletedTasks!!.date_range} >",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray
                            )
                            IconButton(
                                onClick = {
                                    val calendar = Calendar.getInstance().apply { time = currentWeekStart }
                                    calendar.add(Calendar.WEEK_OF_YEAR, 1)
                                    currentWeekStart = calendar.time
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowForward,
                                    contentDescription = "Next Week",
                                    tint = Color.Black
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        WeeklyTasksBarChart(data = weeklyCompletedTasks!!)
                    }
                }
            }

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
        0 -> Color(0xFFFFA0A0) to Color(0xFFD0D0D0)
        1 -> Color(0xFFA0C4FF) to Color(0xFFD0E0FF)
        else -> Color(0xFFA0FFA0) to Color(0xFFD0FFD0)
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

                val formattedDate = formatDateRange(project.startDate, project.endDate)
                Text(
                    formattedDate,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
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

//fun formatDateRange(startDate: String?, endDate: String?): String {
//    return if (startDate != null && endDate != null) {
//        "$startDate - $endDate"
//    } else {
//        "No Date"
//    }
//}