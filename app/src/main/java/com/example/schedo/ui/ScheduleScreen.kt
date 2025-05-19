package com.example.schedo.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ArrowBack
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
import com.example.schedo.model.Group
import com.example.schedo.model.Project
import com.example.schedo.model.Schedule
import com.example.schedo.model.Task
import com.example.schedo.network.RetrofitInstance
import com.example.schedo.network.TaskRequest
import com.example.schedo.util.PreferencesHelper
import com.example.schedo.ui.formatDateRange
import com.example.schedo.ui.theme.Background
import com.example.schedo.ui.theme.Utama2
import com.example.schedo.ui.theme.Utama3
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(navController: NavHostController, groupId: Int, projectId: Int) {
    val context = LocalContext.current
    val preferencesHelper = PreferencesHelper(context)
    val userId = preferencesHelper.getUserId()

    var showAddTodo by remember { mutableStateOf(false) }

    if (userId == -1) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "Silahkan login terlebih dahulu", Toast.LENGTH_LONG).show()
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
    val selectedTabColor = Utama2

    fun fetchSchedules() {
        coroutineScope.launch {
            isLoading = true
            try {
                val response = apiService.getSchedules(userId, currentWeekStart.timeInMillis).data
                schedules.clear()
                schedules.addAll(response)
                println("Jadwal berhasil diambil: ${schedules.size} jadwal untuk userId: $userId")
                println("Isi jadwal: $schedules")
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
                println("Grup berhasil diambil: ${groups.size} grup untuk userId: $userId")
            } catch (e: Exception) {
                println("Gagal mengambil grup untuk userId: $userId - Error: ${e.message}")
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
                println("Respon API mentah untuk userId: $userId - $response")
                projects.clear()
                projects.addAll(response)
                println("Proyek berhasil diambil: ${projects.size} proyek untuk userId: $userId")
                projects.forEach { project ->
                    println("Proyek: id=${project.id}, nama=${project.name}, groupId=${project.groupId}")
                }
            } catch (e: Exception) {
                println("Gagal mengambil proyek untuk userId: $userId - Error: ${e.message}")
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(key1 = Unit) {
        if (groups.isEmpty()) {
            fetchGroups()
        }
        if (projects.isEmpty()) {
            fetchProjects()
        }
        fetchSchedules()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor) // Background diterapkan di sini
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp), // Tambahkan padding bawah untuk menghindari tumpang tindih dengan BottomNavigationBar dari AppNavHost
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (selectedProject == null) {
                            navController.popBackStack()
                        } else {
                            selectedProject = null
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Utama3
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = if (selectedTab == 0) "All Project" else "Schedule",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = {}, enabled = false) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = null,
                            tint = Color.Transparent
                        )
                    }
                }

                when (selectedTab) {
                    0 -> ProjectContentWithData(
                        navController = navController,
                        onProjectClick = { project -> selectedProject = project },
                        onEditClick = { project ->
                            showAddTodo = true
                        },
                        userId = userId,
                        groupId = groupId,
                        groups = groups,
                        onShowAddTodo = { showAddTodo = it }
                    )
                    1 -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
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
                            val days = listOf("M", "S", "S", "R", "K", "J", "S")
                            days.forEachIndexed { index, day ->
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                        .clickable { },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (dates[index] == currentDate.get(Calendar.DAY_OF_MONTH))
                                            Color(0xFFFFC278) else Color.White
                                    )
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
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp)
                        )

                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Utama2)
                            }
                        } else if (schedules.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Tidak ada jadwal", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 16.dp),
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
                                .padding(bottom = 16.dp),
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
                    groupId = selectedProject!!.groupId
                )
            }
        }

        if (showAddTodo) {
            ModalBottomSheet(
                onDismissRequest = { showAddTodo = false },
                sheetState = rememberModalBottomSheetState(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                scrimColor = Color.Black.copy(alpha = 0.5f)
            ) {
                AddTodoScreen(
                    navController = navController,
                    projectId = projectId,
                    groupId = groupId,
                    userId = userId,
                    onDismiss = { showAddTodo = false }
                )
            }
        }
    }
}

// Fungsi lainnya (ScheduleCard, AddScheduleDialog, getWeekStartDate, getWeekDates, TabButton, dll.) tetap sama seperti sebelumnya
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
    navController: NavHostController,
    onProjectClick: (Project) -> Unit,
    onEditClick: (Project) -> Unit,
    userId: Int,
    groupId: Int,
    groups: List<Group>,
    onShowAddTodo: (Boolean) -> Unit
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
                println("Respon API mentah untuk userId: $userId - $response")
                projects.clear()
                projects.addAll(response)
                println("Proyek berhasil diambil: ${projects.size} proyek untuk userId: $userId")
                projects.forEach { project ->
                    println("Proyek: id=${project.id}, nama=${project.name}, groupId=${project.groupId}")
                }
            } catch (e: Exception) {
                println("Gagal mengambil proyek untuk userId: $userId - Error: ${e.message}")
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
            CircularProgressIndicator(color = Utama2)
        }
    } else if (projects.isEmpty() || groups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onShowAddTodo(true)
                    },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFC278))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah Proyek",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            items(projects.size) { index ->
                val project = projects[index]
                val group = groups.find { it.id == project.groupId }
                if (group != null) {
                    ProjectCard(
                        project = project,
                        group = group,
                        onClick = { onProjectClick(project) }
                    )
                } else {
                    ProjectCard(
                        project = project,
                        group = Group(id = -1, name = "Unknown", icon = null.toString()),
                        onClick = { onProjectClick(project) }
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onShowAddTodo(true)
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFC278))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah Proyek",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    group: Group,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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

            val selectedIcon = iconMapping[group.icon?.lowercase() ?: "fas fa-users"] ?: Icons.Default.Edit
            val tint = when (group.name?.lowercase() ?: group.icon?.lowercase() ?: "") {
                "office project", "laptop" -> Color(0xFFFF6F61)
                "personal project", "clipboard" -> Color(0xFF4FC3F7)
                "daily study", "chart" -> Color(0xFF81C784)
                else -> Utama2
            }

            Icon(
                imageVector = selectedIcon,
                contentDescription = "${group.name} Icon",
                tint = tint,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = project.name ?: "Unknown Project",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Group: ${group.name ?: "Unknown"}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Lihat Detail",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                color = Utama2,
                modifier = Modifier.clickable { onClick() }
            )
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
    val backgroundColor = Background
    val apiService = RetrofitInstance.api

    LaunchedEffect(Unit) {
        println("ProjectDetailScreen dimuat dengan userId: $userId, groupId: $groupId, projectId: $projectId")
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
            // Custom Top Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Utama3
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Detail Proyek",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = {}, enabled = false) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = null,
                        tint = Color.Transparent
                    )
                }

            }

            // Detail Proyek Card
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
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            IconButton(onClick = {
                                navController.navigate("add_todo/$userId/$groupId/$projectId")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Proyek",
                                    tint = Utama2
                                )
                            }
                            IconButton(onClick = {
                                println("Mencoba menghapus proyek dengan userId: $userId, groupId: $groupId, projectId: $projectId")
                                coroutineScope.launch {
                                    try {
                                        val response = apiService.deleteProject(userId, groupId, projectId)
                                        if (response == Unit) {
                                            navController.popBackStack()
                                            Toast.makeText(context, "Proyek berhasil dihapus", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: HttpException) {
                                        when (e.code()) {
                                            403 -> {
                                                println("HTTP 403 Forbidden: ${e.message()} - Kemungkinan groupId tidak sesuai")
                                                Toast.makeText(
                                                    context,
                                                    "Akses ditolak: Anda tidak memiliki izin untuk menghapus proyek ini. GroupId mungkin tidak sesuai.",
                                                    Toast.LENGTH_LONG
                                                ).show()
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
                                    contentDescription = "Hapus Proyek",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Deskripsi: ${project.description ?: "Tidak ada deskripsi"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatDateRange(project.startDate, project.endDate),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Daftar Tugas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Utama2)
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
                                println("Edit diklik untuk taskId: ${task.id}")
                                navController.navigate("add_task/$userId/$groupId/$projectId/${task.id}")
                            },
                            onDeleteClick = {
                                println("Mencoba menghapus tugas dengan userId: $userId, groupId: $groupId, projectId: $projectId, taskId: ${task.id}")
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
                if (userId != -1 && groupId != -1 && projectId != 0) {
                    println("Navigasi ke add_task dengan userId: $userId, groupId: $groupId, projectId: $projectId")
                    navController.navigate("add_task/$userId/$groupId/$projectId/-1")
                } else {
                    Toast.makeText(
                        context,
                        "Gagal menambah tugas: Parameter tidak valid (userId: $userId, groupId: $groupId, projectId: $projectId)",
                        Toast.LENGTH_LONG
                    ).show()
                }
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
                        text = task.deadline?.let { formatDateRange(it, it) }?.split(" - ")?.get(0) ?: "Tanpa Batas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
            Row {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Tugas",
                    modifier = Modifier.clickable { onEditClick() }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus Tugas",
                    modifier = Modifier.clickable { onDeleteClick() },
                    tint = Color.Red
                )
            }
        }
    }
}
