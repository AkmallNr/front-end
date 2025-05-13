package com.example.schedo.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.SnackbarDefaults.backgroundColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import com.example.schedo.model.Group
import com.example.schedo.model.Project
import com.example.schedo.model.Task
import com.example.schedo.network.RetrofitInstance
import com.example.schedo.network.TaskRequest
import kotlinx.coroutines.launch
import retrofit2.HttpException
import com.example.schedo.util.PreferencesHelper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.example.schedo.model.Schedule
import com.example.schedo.ui.theme.Background
import com.example.schedo.ui.theme.Utama2
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(navController: NavHostController, groupId: Int, projectId: Int) {
    val context = LocalContext.current
    val preferencesHelper = PreferencesHelper(context)
    val userId = preferencesHelper.getUserId()

    if (userId == -1) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "Silakan login terlebih dahulu", Toast.LENGTH_LONG).show()
            navController.navigate("login")
        }
        return
    }

    var selectedTab by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val projects = remember { mutableStateListOf<Project>() }
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    val groups = remember { mutableStateListOf<Group>() }
    var selectedGroupId by remember { mutableStateOf(groupId) }
    val schedules = remember { mutableStateListOf<Schedule>() }
    var isLoading by remember { mutableStateOf(false) }
    val apiService = RetrofitInstance.api
    var showAddSchedule by remember { mutableStateOf(false) }
    var selectedSchedule by remember { mutableStateOf<Schedule?>(null) }
    val currentDate = remember { Calendar.getInstance() }
    var currentWeekStart by remember { mutableStateOf(getWeekStartDate(currentDate)) }

    val backgroundColor = Background
    val selectedTabColor = Color(0xFFFFC278)

    fun fetchSchedules() {
        coroutineScope.launch {
            isLoading = true
            try {
                val response = apiService.getSchedules(userId, currentWeekStart.timeInMillis).data
                schedules.clear()
                schedules.addAll(response)
                println("Schedule fetched successfully: ${schedules.size} groups for userId: $userId")
                println("isi Schedule: ${schedules}")
            } catch (e: Exception) {
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
                val response = apiService.getGroups(userId).data
                groups.clear()
                groups.addAll(response)
                println("Groups fetched successfully: ${groups.size} groups for userId: $userId")
            } catch (e: Exception) {
                println("Failed to fetch groups for userId: $userId - Error: ${e.message}")
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun fetchProjects() {
        coroutineScope.launch {
            isLoading = true
            try {
                val response = apiService.getProjectsByUser(userId).data
                println("Raw API response for userId: $userId - $response") // Log respons mentah
                projects.clear()
                projects.addAll(response)
                println("Projects fetched successfully: ${projects.size} projects for userId: $userId")
                // Log detail setiap proyek
                projects.forEach { project ->
                    println("Project: id=${project.id}, name=${project.name}, groupId=${project.groupId}")
                }
            } catch (e: Exception) {
                println("Failed to fetch projects for userId: $userId - Error: ${e.message}")
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
    LaunchedEffect(key1 = Unit) {
        if (groups.isEmpty()) {
            fetchGroups() // Ambil grup saat pertama kali dimuat
        }
        if (projects.isEmpty()) {
            fetchProjects() // Ambil proyek saat pertama kali dimuat
        }
        fetchSchedules()
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
                    containerColor = Utama2
                ),
                modifier = Modifier.statusBarsPadding()
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
                        selectedColor = selectedTabColor,
                        unSelectedColor = Color.White
                    )
                    TabButton(
                        text = "Schedule",
                        isSelected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f),
                        selectedColor = selectedTabColor,
                        unSelectedColor = Color.White
                    )
                }

                Text(
                    text = if (selectedTab == 0) "All Project" else "Schedule",
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
                            println("Edit clicked for poject ${project.id} group ${project.groupId}")
                            navController.navigate("add_todo/$userId/${project.groupId}/${project.id}")
                        },
                        userId = userId,
                        groupId = groupId
                    )
                    1 -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { currentWeekStart.add(Calendar.DAY_OF_MONTH, -7) }) {
                                Text("<")
                            }
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                Text(
                                    text = SimpleDateFormat("MMMM yyyy").format(currentWeekStart.time),
                                    modifier = Modifier.clickable { expanded = true }
                                )
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    val months = DateFormatSymbols().months
                                    months.forEachIndexed { index, month ->
                                        DropdownMenuItem(
                                            text = { Text(month) },
                                            onClick = {
                                                currentWeekStart.set(Calendar.MONTH, index)
                                                currentWeekStart.set(Calendar.DAY_OF_MONTH, 1)
                                                currentWeekStart = getWeekStartDate(currentWeekStart)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { currentWeekStart.add(Calendar.DAY_OF_MONTH, 7) }) {
                                Text(">")
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val dates = getWeekDates(currentWeekStart)
                            val days = listOf("S", "M", "T", "W", "T", "F", "S")
                            days.forEachIndexed { index, day ->
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                        .clickable { },
                                    colors = CardDefaults.cardColors(containerColor = if (dates[index] == currentDate.get(Calendar.DAY_OF_MONTH)) Color(0xFFFFC278) else Color.White)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(day)
                                        Text(dates[index].toString())
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Jadwal Hari Ini",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )

                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else if (schedules.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Tidak ada jadwal", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(schedules) { schedule ->
                                    ScheduleCard(schedule, userId, groupId) { action ->
                                        when (action) {
                                            "edit" -> selectedSchedule = schedule
                                            "delete" -> coroutineScope.launch {
                                                apiService.deleteSchedule(userId, schedule.id)
                                                fetchSchedules()
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { showAddSchedule = true },
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC278))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah Jadwal")
                            Text("Tambah Jadwal")
                        }

                        if (showAddSchedule || selectedSchedule != null) {
                            AddScheduleDialog(
                                navController,
                                userId,
                                groupId,
                                currentWeekStart,
                                onDismiss = { showAddSchedule = false; selectedSchedule = null },
                                scheduleToEdit = selectedSchedule,
                                onScheduleAdded = { fetchSchedules() }
                            )
                        }
                    }
                }
            } else {
                ProjectDetailScreen(
                    navController,
                    selectedProject!!,
                    userId,
                    groupId = selectedProject!!.groupId)
            }
        }
    }
}

@Composable
fun ScheduleCard(schedule: Schedule, userId: Int, groupId: Int, onAction: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(schedule.name, fontWeight = FontWeight.Bold)
                Text("Waktu: ${schedule.startTime} - ${schedule.endTime}")
                Text("Catatan: ${schedule.notes}")
                Text("Ulangi: ${if (schedule.repeat) "Ya" else "Tidak"}")
            }
            Row {
                IconButton(onClick = { onAction("edit") }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = { onAction("delete") }) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red)
                }
            }
        }
    }
}

@Composable
fun AddScheduleDialog(
    navController: NavHostController,
    userId: Int,
    groupId: Int,
    currentWeekStart: Calendar,
    onDismiss: () -> Unit,
    scheduleToEdit: Schedule?,
    onScheduleAdded: () -> Unit
) {
    var name by remember { mutableStateOf(scheduleToEdit?.name ?: "") }
    var notes by remember { mutableStateOf(scheduleToEdit?.notes ?: "") }
    var repeat by remember { mutableStateOf(scheduleToEdit?.repeat ?: false) }
    var startTime by remember { mutableStateOf(scheduleToEdit?.startTime ?: "") }
    var endTime by remember { mutableStateOf(scheduleToEdit?.endTime ?: "") }
    val coroutineScope = rememberCoroutineScope()
    val apiService = RetrofitInstance.api

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (scheduleToEdit == null) "Tambah Jadwal" else "Edit Jadwal") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Jadwal") })
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Catatan") })
                OutlinedTextField(value = startTime, onValueChange = { startTime = it }, label = { Text("Waktu Mulai") })
                OutlinedTextField(value = endTime, onValueChange = { endTime = it }, label = { Text("Waktu Selesai") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = repeat, onCheckedChange = { repeat = it })
                    Text("Ulangi jadwal ini setiap minggu")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val schedule = Schedule(
                    id = scheduleToEdit?.id ?: 0,
                    name = name,
                    notes = notes,
                    repeat = repeat,
                    day = currentWeekStart.get(Calendar.DAY_OF_WEEK).toString(),
                    startTime = startTime,
                    endTime = endTime
                )
                coroutineScope.launch {
                    if (scheduleToEdit == null) {
                        apiService.addSchedule(userId, schedule)
                    } else {
                        apiService.updateSchedule(userId, schedule)
                    }
                    onScheduleAdded()
                    onDismiss()
                }
            }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

fun getWeekStartDate(calendar: Calendar): Calendar {
    val c = calendar.clone() as Calendar
    c.set(Calendar.DAY_OF_WEEK, c.firstDayOfWeek)
    return c
}

fun getWeekDates(calendar: Calendar): List<Int> {
    val dates = mutableListOf<Int>()
    val c = calendar.clone() as Calendar
    for (i in 0 until 7) {
        dates.add(c.get(Calendar.DAY_OF_MONTH))
        c.add(Calendar.DAY_OF_MONTH, 1)
    }
    return dates
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = Utama2,
    unSelectedColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .background(
                color = if (isSelected) selectedColor else unSelectedColor,
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
            color = if (isSelected) Color.White else Utama2
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
                val response = apiService.getProjectsByUser(userId).data
                println("Raw API response for userId: $userId - $response") // Log respons mentah
                projects.clear()
                projects.addAll(response)
                println("Projects fetched successfully: ${projects.size} projects for userId: $userId")
                // Log detail setiap proyek
                projects.forEach { project ->
                    println("Project: id=${project.id}, name=${project.name}, groupId=${project.groupId}")
                }
            } catch (e: Exception) {
                println("Failed to fetch projects for userId: $userId - Error: ${e.message}")
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
                        .padding(vertical = 4.dp)
                        .clickable {
                            // Log projectId dan groupId saat proyek diklik
                            println("Project clicked - projectId: ${project.id}, groupId: ${project.groupId}, userId: $userId")
                            onProjectClick(project) // Mengirim seluruh objek Project ke callback
                        },
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

@Composable
fun ProjectDetailScreen(navController: NavHostController, project: Project, userId: Int, groupId: Int) {
    val context = LocalContext.current
    val projectId = project.id ?: 0
    val coroutineScope = rememberCoroutineScope()
    val tasks = remember { mutableStateListOf<Task>() }
    var isLoading by remember { mutableStateOf(false) }
    val backgroundColor = Color(0xFFFFFBEB)
    val apiService = RetrofitInstance.api

    // Log untuk memverifikasi parameter saat komponen dimuat
    LaunchedEffect(Unit) {
        println("ProjectDetailScreen loaded with userId: $userId, groupId: $groupId, projectId: $projectId")
    }

    LaunchedEffect(key1 = projectId) {
        isLoading = true
        try {
            val response = apiService.getTask(userId, groupId, projectId).data
            tasks.clear()
            tasks.addAll(response)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal memuat tugas: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(bottom = 72.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = project.name ?: "Tanpa Nama",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = {
                            // Log untuk memverifikasi nilai sebelum penghapusan
                            println("Attempting to delete project with userId: $userId, groupId: $groupId, projectId: $projectId")
                            coroutineScope.launch {
                                try {
                                    val response = apiService.deleteProject(userId, groupId, projectId)
                                    if (response == Unit) { // Asumsi deleteProject mengembalikan Unit
                                        navController.popBackStack()
                                        Toast.makeText(context, "Proyek berhasil dihapus", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: HttpException) {
                                    when (e.code()) {
                                        403 -> {
                                            println("HTTP 403 Forbidden: ${e.message()} - Possible groupId mismatch")
                                            Toast.makeText(context, "Akses ditolak: Anda tidak memiliki izin untuk menghapus proyek ini. GroupId mungkin tidak sesuai.", Toast.LENGTH_LONG).show()
                                        }
                                        else -> Toast.makeText(context, "Gagal menghapus proyek: ${e.message()}", Toast.LENGTH_SHORT).show()
                                    }
                                    e.printStackTrace()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Gagal menghapus proyek: ${e.message}", Toast.LENGTH_SHORT).show()
                                    e.printStackTrace()
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Project",
                                tint = Color.Red
                            )
                        }
                    }
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

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Daftar Tugas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks) { task ->
                        TaskCard(
                            task = task,
                            userId = userId,
                            groupId = groupId,
                            projectId = projectId,
                            onStatusChange = { updatedTask ->
                                coroutineScope.launch {
                                    try {
                                        val response = apiService.updateTask(
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
                            },
                            onDeleteClick = {
                                println("Attempting to delete task with userId: $userId, groupId: $groupId, projectId: $projectId, taskId: ${task.id}")
                                coroutineScope.launch {
                                    try {
                                        apiService.deleteTask(userId, groupId, projectId, task.id ?: 0)
                                        tasks.remove(task)
                                        Toast.makeText(context, "Tugas berhasil dihapus", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "Gagal menghapus tugas: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                navController.navigate("add_task/$userId/$groupId/$projectId/-1")
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC278)),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = "Tambah Tugas")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Tambah Tugas")
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    userId: Int,
    groupId: Int,
    projectId: Int,
    onStatusChange: (Task) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = RetrofitInstance.api

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
            Row {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Task",
                    modifier = Modifier.clickable { onEditClick() }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Task",
                    modifier = Modifier.clickable { onDeleteClick() },
                    tint = Color.Red
                )
            }
        }
    }
}
