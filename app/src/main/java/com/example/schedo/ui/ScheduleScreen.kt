package com.example.schedo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.schedo.model.Project
import com.example.schedo.network.RetrofitInstance
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(navController: NavHostController, userId: Int, groupId: Int) {
    var selectedTab by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val projects = remember { mutableStateListOf<Project>() }
    var isLoading by remember { mutableStateOf(false) }
    val apiService = RetrofitInstance.api
    var selectedProject by remember { mutableStateOf<Project?>(null) }

    fun fetchProjects() {
        coroutineScope.launch {
            isLoading = true
            try {
                val response = apiService.getProjectsByGroup(userId = userId, groupId = groupId)
                projects.clear()
                projects.addAll(response)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(key1 = selectedTab) {
        if (selectedTab == 1 && projects.isEmpty()) {
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
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            if (selectedProject == null) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Jadwal Rutin") },
                        icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Jadwal Rutin") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Proyek") },
                        icon = { Icon(Icons.Default.Checklist, contentDescription = "Proyek") }
                    )
                }
                when (selectedTab) {
                    0 -> RoutineScheduleContent()
                    1 -> {
                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Memuat proyek...")
                            }
                        } else {
                            ProjectScheduleContent(projects, onProjectClick = { project ->
                                selectedProject = project
                            })
                        }
                    }
                }
            } else {
                ProjectDetailScreen(navController, selectedProject!!, userId, groupId)
            }
        }
    }
}

@Composable
fun RoutineScheduleContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Halaman Jadwal Rutin",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Composable
fun ProjectScheduleContent(projects: List<Project>, onProjectClick: (Project) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(projects) { project ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable { onProjectClick(project) },
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = project.name ?: "Tanpa Nama", style = MaterialTheme.typography.bodyLarge)
                    Text(text = "Start: ${project.startDate ?: "Tanpa Tanggal"}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "End: ${project.endDate ?: "Tanpa Tanggal"}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (projects.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada proyek yang dibuat.")
                }
            }
        }
    }
}

@Composable
fun ProjectDetailScreen(navController: NavHostController, project: Project, userId: Int, groupId: Int) {
    val projectId = project.id ?: 0 // Ambil projectId dari objek project, gunakan 0 sebagai default jika null

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Detail Proyek", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Nama: ${project.name}")
        Text(text = "Deskripsi: ${project.description ?: "Tidak ada deskripsi"}")
        Text(text = "Mulai: ${project.startDate ?: "-"}")
        Text(text = "Selesai: ${project.endDate ?: "-"}")

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            navController.navigate("add_task/$userId/$groupId/$projectId")
        }) {
            Icon(Icons.Default.Add, contentDescription = "Tambah Tugas")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Tambah Tugas")
        }
    }
}