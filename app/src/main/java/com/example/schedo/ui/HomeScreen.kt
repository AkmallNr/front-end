package com.example.schedo.ui

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.schedo.model.Group
import com.example.schedo.model.Project
import com.example.schedo.model.Task
import com.example.schedo.model.User
import com.example.schedo.network.ApiService
import com.example.schedo.network.RetrofitInstance
import com.example.schedo.network.WeeklyCompletedTasksData
import com.example.schedo.ui.theme.Background
import com.example.schedo.ui.theme.Utama2
import com.example.schedo.util.PreferencesHelper
import com.example.schedo.utils.calculateTaskProgress
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.provider.Settings
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.schedo.ui.theme.Utama1
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun logout(
    context: Context,
    navController: NavController,
    googleSignInClient: GoogleSignInClient?,
    coroutineScope: CoroutineScope,
    onLogoutComplete: () -> Unit // Callback untuk memberi tahu saat logout selesai
) {
    val preferencesHelper = PreferencesHelper(context)

    coroutineScope.launch {
        try {
            // Hapus data sesi autentikasi manual
            preferencesHelper.clearUserId()
            preferencesHelper.clearSession() // Jika Anda menyimpan token autentikasi

            // Logout dari Google Sign-In jika autentikasi Google digunakan
            googleSignInClient?.let {
                it.signOut().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        println("Google Sign-Out successful")
                    } else {
                        println("Google Sign-Out failed: ${task.exception?.message}")
                    }
                    onLogoutComplete() // Panggil callback setelah proses selesai
                }
            } ?: run {
                // Jika tidak ada GoogleSignInClient, langsung selesai
                onLogoutComplete()
            }
        } catch (e: Exception) {
            println("Logout error: ${e.message}")
            onLogoutComplete() // Pastikan callback dipanggil meskipun ada error
        }
    }
}

@Composable
fun WeeklyTasksBarChart(data: WeeklyCompletedTasksData) {
    val daysOrder = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val tasksCount = daysOrder.map { data.tasks[it] ?: 0 }
    val maxTasks = (tasksCount.maxOrNull()?.toFloat() ?: 1f).let { if (it == 0f) 1f else it }

    val chartHeight = 200.dp
    val cornerRadius = 8.dp
    val barColor = Utama1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Chart Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .background(Color(0xFFF5F7FA), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val barWidth = canvasWidth / 10f
                val spacing = canvasWidth / 7f
                val maxHeightPx = canvasHeight * 0.7f
                val bottomPadding = canvasHeight * 0.15f

                // Draw grid lines
                val gridLines = 5
                for (i in 0..gridLines) {
                    val yPos = canvasHeight - bottomPadding - (i * (maxHeightPx / gridLines))
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.2f),
                        start = Offset(0f, yPos),
                        end = Offset(canvasWidth, yPos),
                        strokeWidth = 1f
                    )
                }

                // Draw Y-axis labels
                for (i in 0..gridLines) {
                    val yValue = (maxTasks * i / gridLines).toInt()
                    val yPos = canvasHeight - bottomPadding - (i * (maxHeightPx / gridLines))
                    drawContext.canvas.nativeCanvas.drawText(
                        yValue.toString(),
                        -20f,
                        yPos + 5f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }
                    )
                }

                // Draw bars
                tasksCount.forEachIndexed { index, count ->
                    val barHeightPx = if (count > 0) {
                        (count / maxTasks * maxHeightPx).coerceAtLeast(8f)
                    } else {
                        0f
                    }
                    val left = index * spacing + (spacing - barWidth) / 2
                    val top = canvasHeight - bottomPadding - barHeightPx

                    if (barHeightPx > 0) {
                        // Draw rounded rectangle bar
                        val cornerRadiusPx = with(this) { cornerRadius.toPx() }
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeightPx),
                            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                        )
                    }
                }

                // Draw day labels
                dayLabels.forEachIndexed { index, label ->
                    val xPos = index * spacing + spacing / 2
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        xPos,
                        canvasHeight - 10f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 32f
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
    val groups = remember { mutableStateListOf<Group>() }

    val selectedProjects = remember { mutableStateListOf<Project>() }

    var isProjectsLoading by remember { mutableStateOf(false) }
    var showAllProjects by remember { mutableStateOf(false) }

    var showProjectSelectionDialog by remember { mutableStateOf(false) }

    var weeklyCompletedTasks by remember { mutableStateOf<WeeklyCompletedTasksData?>(null) }
    var isWeeklyTasksLoading by remember { mutableStateOf(false) }
    var currentWeekStart by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
        )
    }

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)

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
            isLoading = true
            try {
                val response = RetrofitInstance.api.getProjectsByUser(userId)
                println("Respon API mentah untuk userId: $userId - ${response.body()}")
                val data = response.body()?.data
                if (data != null) {
                    allProjects.clear()
                    allProjects.addAll(data)
                    println("Proyek berhasil diambil: ${allProjects.size} proyek untuk userId: $userId")
                    allProjects.forEach { project ->
                        println("Proyek: id=${project.id}, nama=${project.name}, groupId=${project.groupId}")
                    }
                } else {
                    println("Data proyek kosong atau respons tidak valid untuk userId: $userId")
                }
            } catch (e: Exception) {
                println("Gagal mengambil proyek untuk userId: $userId - Error: ${e.message}")
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

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

    fun fetchTask() {
        coroutineScope.launch {
            isLoading = true
            try {
                val response = RetrofitInstance.api.getTaskByUser(userId).data
                allTasks.clear()
                allTasks.addAll(response)
                Log.d(
                    "HomeScreen",
                    "Tasks fetched successfully: ${allTasks.size} tasks for userId: $userId"
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("HomeScreen", "Error fetching tasks: ${e.message}")
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
                    Log.e(
                        "HomeScreen",
                        "Failed to fetch weekly completed tasks: ${response.code()}"
                    )
                    Toast.makeText(
                        context,
                        "Gagal memuat tugas selesai mingguan",
                        Toast.LENGTH_SHORT
                    ).show()
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

    fun saveSelectedProjects(projects: List<Project>) {
        selectedProjects.clear()
        selectedProjects.addAll(projects)
        val projectIds = projects.mapNotNull { it.id }.joinToString(",")
        preferencesHelper.saveSelectedProjectIds(projectIds)
        Log.d("HomeScreen", "Saved ${selectedProjects.size} projects as shortcuts")
    }

    LaunchedEffect(Unit) {
        if (userId != -1) {
            fetchUserData()
            fetchProjects()
            fetchGroups()
            fetchTask()
            fetchWeeklyCompletedTasks(currentWeekStart)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent)
            }
        }
    }

    LaunchedEffect(currentWeekStart) {
        if (userId != -1) {
            fetchWeeklyCompletedTasks(currentWeekStart)
        }
    }

    Scaffold { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Background)
                    .padding(vertical = 50.dp)
            ) {
                item {
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
                                    Text(
                                        "${user!!.name}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text("Guest", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Log in to access your projects",
                                        fontSize = 14.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.clickable {
                                            navController.navigate("login") {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    inclusive = true
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Row {
                            IconButton(onClick = {
                                logout(
                                    context = context,
                                    navController = navController,
                                    googleSignInClient = googleSignInClient,
                                    coroutineScope = coroutineScope,
                                    onLogoutComplete = {
                                        navController.navigate("login") {
                                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                        }
                                    }
                                )
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.ExitToApp,
                                    contentDescription = "Logout",
                                    tint = Color.Black
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                item {
                    Text(
                        "Task Summary",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
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
                                    "Task Completed",
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
                                    "Pending Tasks",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Group List",
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
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                if (isProjectsLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (selectedProjects.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No projects selected", color = Color.Gray)
                        }
                    }
                } else {
                    items(selectedProjects) { project ->
                        val group = groups.find { it.id == project.groupId } ?: Group(
                            id = -1,
                            name = "Unknown",
                            icon = "fas fa-users"
                        )
                        ProjectCard1(
                            project = project,
                            group = group,
                            userId = userId,
                            groupId = project.groupId ?: 0,
                            apiService = RetrofitInstance.api,
                            onClick = {
                                val groupId = project.groupId ?: 0
                                val projectId = project.id ?: 0
                                navController.navigate("project_detail/$userId/$groupId/$projectId")
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                item {
                    Text(
                        "Tasks Completed in this week",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (isWeeklyTasksLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (weeklyCompletedTasks == null) {
                    item {
                        Text(
                            "Gagal memuat tugas selesai",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = " ${weeklyCompletedTasks!!.date_range} ",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            WeeklyTasksBarChart(data = weeklyCompletedTasks!!)
                        }
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

    // Icon mapping seperti di ScheduleScreen
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
            .padding(16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
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

            // Calculate the progress properly (or use project.progress if available)
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

