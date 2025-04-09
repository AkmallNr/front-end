package com.example.schedo.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.schedo.model.Project
import com.example.schedo.model.Task
import com.example.schedo.network.RetrofitInstance
import com.example.schedo.network.TaskRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(navController: NavHostController, userId: Int, groupId: Int, projectId: Int) {
    var selectedTab by remember { mutableStateOf(0) } // Default to Schedule tab
    val coroutineScope = rememberCoroutineScope()
    val projects = remember { mutableStateListOf<Project>() }
    var isLoading by remember { mutableStateOf(false) }
    val apiService = RetrofitInstance.api
    var selectedProject by remember { mutableStateOf<Project?>(null) }

    val backgroundColor = Color(0xFFFFFBEB)
    val selectedTabColor = Color(0xFFFFC278)

    fun fetchProjects() {
        coroutineScope.launch {
            isLoading = true
            try {
                val response = apiService.getProjectsByGroup(userId, groupId)
                println("userid : ${userId}, groupid : ${groupId}")
                projects.clear()
                projects.addAll(response)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(key1 = Unit) {
        if (projects.isEmpty()) {
            fetchProjects()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedProject == null) "Jadwal" else "Detail Proyek") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedProject == null) {
                            navController.popBackStack()
                        } else {
                            selectedProject = null
                        }
                    }) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Top
        ) {
            if (selectedProject == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    TabButton(
                        text = "Project",
                        isSelected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f),
                        selectedColor = selectedTabColor
                    )
                    TabButton(
                        text = "Schedule",
                        isSelected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f),
                        selectedColor = selectedTabColor
                    )
                }

                Text(
                    text = if (selectedTab == 0) "All Project" else "All Schedule",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.CenterHorizontally)
                )

                when (selectedTab) {
                    0 -> ProjectContentWithData(
                        onProjectClick = { project -> selectedProject = project },
                        onEditClick = { project ->
                                println("Edit clicked for poject ${project.id} group ${groupId}")
                                        navController.navigate("add_todo/$userId/$groupId/${project.id}")
                                },
                        userId = userId,
                        groupId = groupId
                    )
                    1 -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada jadwal",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                        }
                    }
                }
            } else {
                ProjectDetailScreen(
                    navController = navController,
                    project = selectedProject!!,
                    userId = userId,
                    groupId = groupId,
                    onBackClick = { selectedProject = null } // Berikan parameter onBackClick
                )
            }
        }
    }
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = Color(0xFFFFC278)
) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .background(
                color = if (isSelected) selectedColor else Color.Transparent,
                shape = MaterialTheme.shapes.medium
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.Black else Color.Gray
        )
    }
}

@Composable
fun ProjectContentWithData(
    onProjectClick: (Project) -> Unit,
    onEditClick: (Project) -> Unit, // Tambahkan callback edit
    userId: Int,
    groupId: Int
) {
    val coroutineScope = rememberCoroutineScope()
    val projects = remember { mutableStateListOf<Project>() }
    var isLoading by remember { mutableStateOf(false) }
    val apiService = RetrofitInstance.api

    fun fetchProjects() {
        coroutineScope.launch {
            isLoading = true
            try {
                val response = apiService.getProjectsByGroup(userId, groupId)
                projects.clear()
                projects.addAll(response)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(key1 = Unit) {
        fetchProjects()
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (projects.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Tidak ada proyek",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(projects) { project ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProjectClick(project) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = project.name ?: "Tanpa Nama",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Start: ${project.startDate ?: "Tanpa Tanggal"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Text(
                                text = "End: ${project.endDate ?: "Tanpa Tanggal"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }

                        IconButton(onClick = { onEditClick(project) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Project",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    navController: NavHostController,
    project: Project,
    userId: Int,
    groupId: Int,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val projectId = project.id ?: 0
    val coroutineScope = rememberCoroutineScope()
    val tasks = remember { mutableStateListOf<Task>() }
    var isLoading by remember { mutableStateOf(false) }
    val backgroundColor = Color(0xFFFFFBEB)

    // Fetch tasks when projectId changes
    LaunchedEffect(key1 = projectId) {
        isLoading = true
        try {
            val response = RetrofitInstance.api.getTask(userId, groupId, projectId)
            tasks.clear()
            tasks.addAll(response)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal memuat tugas: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    navController.navigate("add_task/$userId/$groupId/$projectId/-1")
                },
                containerColor = Color(0xFFFFC278), // Warna oranye
                contentColor = Color.Black, // Warna ikon dan teks
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp) // Jarak dari sisi kiri dan kanan
                    .padding(bottom = 8.dp), // Jarak dari bawah untuk berada di atas bottom navigation bar
                shape = MaterialTheme.shapes.medium // Sudut membulat
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah Tugas",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tambah Tugas",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center, // Posisi di tengah bawah
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp) // Padding horizontal untuk seluruh konten
        ) {
            // Project Detail Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp), // Kurangi spasi di atas card
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = project.name ?: "Tanpa Nama",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Deskripsi: ${project.description ?: "Tidak ada deskripsi"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Mulai",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Gray
                            )
                            Text(
                                text = project.startDate ?: "-",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Column {
                            Text(
                                text = "Selesai",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Gray
                            )
                            Text(
                                text = project.endDate ?: "-",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp)) // Kurangi spasi antara card dan judul

            // Task List Title
            Text(
                text = "Daftar Tugas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp)) // Kurangi spasi antara judul dan daftar

            // Task List Content
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada tugas",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Maksimalkan tinggi daftar tugas
                        .padding(bottom = 8.dp), // Kurangi padding bawah
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks) { task ->
                        TaskCard(
                            task = task,
                            onStatusChange = { updatedTask ->
                                coroutineScope.launch {
                                    try {
                                        val response = RetrofitInstance.api.updateTask(
                                            userId, groupId, projectId, task.id!!, TaskRequest(
                                                id = task.id,
                                                name = task.name ?: "",
                                                description = task.description,
                                                deadline = task.deadline,
                                                reminder = task.reminder,
                                                priority = task.priority ?: "Normal",
                                                attachment = task.attachment,
                                                status = updatedTask.status
                                            )
                                        )
                                        if (response.isSuccessful) {
                                            tasks[tasks.indexOf(task)] = response.body()!!
                                            Toast.makeText(context, "Status tugas diperbarui!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Gagal memperbarui status: ${response.code()}", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onEditClick = {
                                println("Edit clicked for taskId: ${task.id}")
                                navController.navigate("add_task/$userId/$groupId/$projectId/${task.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    onStatusChange: (Task) -> Unit,
    onEditClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick)
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = task.status ?: false,
                    onCheckedChange = { isChecked ->
                        val updatedTask = task.copy(status = isChecked)
                        onStatusChange(updatedTask)
                    },
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = task.name ?: "Tanpa Nama",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Batas Waktu: ${task.deadline ?: "Tanpa Batas"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Task",
                modifier = Modifier.clickable { onEditClick() }
            )
        }
    }
}