package com.example.schedo.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import com.example.schedo.model.Task
import com.example.schedo.network.RetrofitInstance
import com.example.schedo.network.TaskRequest
import com.example.schedo.ui.formatDateRange
import com.example.schedo.ui.theme.Background
import com.example.schedo.ui.theme.Grey2
import com.example.schedo.ui.theme.Utama2
import com.example.schedo.ui.theme.Utama3
import kotlinx.coroutines.launch
import retrofit2.HttpException
import com.example.schedo.util.PreferencesHelper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.schedo.model.Schedule
import com.example.schedo.model.Attachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    // State untuk tanggal saat ini dan refresh trigger
    var currentDate by remember { mutableStateOf(Calendar.getInstance()) }
    var schedules = remember { mutableStateListOf<Schedule>() }
    var refreshTrigger by remember { mutableStateOf(0) } // Trigger untuk refresh data

    // Formatter untuk nama hari dan bulan
    val dayFormat = SimpleDateFormat("EEEE", Locale("id", "ID"))
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    var selectedTab by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val projects = remember { mutableStateListOf<Project>() }
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    val groups = remember { mutableStateListOf<Group>() }
    var selectedGroupId by remember { mutableStateOf(groupId) }
    var isLoading by remember { mutableStateOf(false) }
    val apiService = RetrofitInstance.api
    var showAddSchedule by remember { mutableStateOf(false) }
    var selectedSchedule by remember { mutableStateOf<Schedule?>(null) }
    var currentWeekStart by remember { mutableStateOf(getWeekStartDate(currentDate)) }
    val currentDay = currentDate.get(Calendar.DAY_OF_MONTH)
    var selectedDayIndex by remember { mutableStateOf(currentDay - 1) }

    val backgroundColor = Background
    val selectedTabColor = Utama2

    // Fungsi untuk mengambil jadwal dari server
    suspend fun fetchSchedulesFromServer() {
        try {
            isLoading = true
            val startOfMonth = Calendar.getInstance().apply {
                time = currentDate.time
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endOfMonth = Calendar.getInstance().apply {
                time = currentDate.time
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val isoFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            Log.d("ScheduleScreen", "Fetching schedules for userId: $userId, start: ${isoFormat.format(startOfMonth.time)}, end: ${isoFormat.format(endOfMonth.time)}")
            val response = apiService.getSchedulesByDateRange(
                userId,
                isoFormat.format(startOfMonth.time),
                isoFormat.format(endOfMonth.time)
            )
            if (response.isSuccessful) {
                val scheduleResponse = response.body()
                schedules.clear()
                scheduleResponse?.data?.let { schedules.addAll(it) }
                Log.d("ScheduleScreen", "Fetched schedules: ${schedules.size} items")
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ScheduleScreen", "Failed to fetch schedules: HTTP ${response.code()} ${response.message()} - Error: $errorBody")
                Toast.makeText(context, "Gagal mengambil jadwal: HTTP ${response.code()} ${response.message()}\n$errorBody", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("ScheduleScreen", "Failed to fetch schedules: ${e.message}", e)
            Toast.makeText(context, "Gagal mengambil jadwal: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            isLoading = false
        }
    }

    // Fetch schedules saat tanggal berubah atau refreshTrigger berubah
    LaunchedEffect(currentDate, refreshTrigger) {
        fetchSchedulesFromServer()
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
                val response = apiService.getProjectsByUser(userId)
                println("Respon API mentah untuk userId: $userId - ${response.body()}") // Log respons lengkap
                val data = response.body()?.data // Pastikan data diambil dari response.body()
                if (data != null) {
                    projects.clear()
                    projects.addAll(data)
                    println("Proyek berhasil diambil: ${projects.size} proyek untuk userId: $userId")
                    projects.forEach { project ->
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

    LaunchedEffect(key1 = Unit) {
        if (groups.isEmpty()) fetchGroups()
        if (projects.isEmpty()) fetchProjects()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 56.dp), // Padding dikurangi dari 72dp menjadi 56dp
            verticalArrangement = Arrangement.Top
        ) {
            if (selectedProject == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 50.dp)
                ) {
                    TabButton(
                        text = "Group",
                        isSelected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f),
                        selectedColor = selectedTabColor,
                        unSelectedColor = Color.White
                    )
                    TabButton(
                        text = "Schedule",
                        isSelected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            Log.d("ScheduleScreen", "Switched to Schedule tab, selectedTab = $selectedTab")
                        },
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
                    IconButton(onClick = { if (selectedProject == null) navController.popBackStack() else selectedProject = null }) {
                        Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = "Kembali", tint = Utama3)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (selectedTab == 0) "All Group" else "Schedule",
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
                    0 ->ProjectContentWithData(
                        navController = navController,
                        onProjectClick = { project -> selectedProject = project },
                        onEditClick = { project ->
                            showAddTodo = true
                        },
                        userId = userId,
                        groupId = selectedGroupId, // Gunakan selectedGroupId sebagai default
                        groups = groups,
                        onShowAddTodo = { showAddTodo = it }
                    )
                    1 -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { currentDate.add(Calendar.MONTH, -1); selectedDayIndex = 0 }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Bulan Sebelumnya")
                                }
                                Text(
                                    text = monthFormat.format(currentDate.time),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { currentDate.add(Calendar.MONTH, 1); selectedDayIndex = 0 }) {
                                    Icon(Icons.Default.ArrowForward, contentDescription = "Bulan Berikutnya")
                                }
                            }

                            val monthStart = Calendar.getInstance().apply { time = currentDate.time; set(Calendar.DAY_OF_MONTH, 1) }
                            val daysInMonth = currentDate.getActualMaximum(Calendar.DAY_OF_MONTH)
                            val lazyRowState = rememberLazyListState()
                            LaunchedEffect(currentDate.timeInMillis) {
                                val newSelectedDay = if (selectedDayIndex >= daysInMonth) 0 else selectedDayIndex
                                selectedDayIndex = newSelectedDay
                                if (newSelectedDay >= 2) lazyRowState.animateScrollToItem(maxOf(0, newSelectedDay - 2))
                            }
                            LazyRow(
                                state = lazyRowState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(daysInMonth) { index ->
                                    val dayCalendar = Calendar.getInstance().apply { time = monthStart.time; add(Calendar.DAY_OF_YEAR, index) }
                                    val dayOfMonth = dayCalendar.get(Calendar.DAY_OF_MONTH)
                                    val dayName = when (dayCalendar.get(Calendar.DAY_OF_WEEK)) {
                                        Calendar.MONDAY -> "Sen"; Calendar.TUESDAY -> "Sel"; Calendar.WEDNESDAY -> "Rab"
                                        Calendar.THURSDAY -> "Kam"; Calendar.FRIDAY -> "Jum"; Calendar.SATURDAY -> "Sab"
                                        Calendar.SUNDAY -> "Min"; else -> ""
                                    }
                                    val isSelected = selectedDayIndex == index
                                    Card(
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clickable { selectedDayIndex = index; currentDate.set(Calendar.DAY_OF_MONTH, dayOfMonth) }
                                            .scale(if (isSelected) 1.1f else 1.0f),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFFF914D) else Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(text = dayName, fontSize = 12.sp, color = if (isSelected) Color.White else Color.Gray, fontWeight = FontWeight.Medium)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = dayOfMonth.toString(), fontSize = 18.sp, color = if (isSelected) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Text(text = "Jadwal Hari Ini", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp))
                            val selectedDate = Calendar.getInstance().apply { time = monthStart.time; add(Calendar.DAY_OF_YEAR, selectedDayIndex) }
                            val todaySchedules = schedules.filter { schedule ->
                                try {
                                    val scheduleDate = dateFormat.parse(schedule.startTime.split(" ")[0])
                                    scheduleDate?.let { date ->
                                        val scheduleCal = Calendar.getInstance().apply { time = date }
                                        scheduleCal.get(Calendar.DAY_OF_MONTH) == selectedDate.get(Calendar.DAY_OF_MONTH) &&
                                                scheduleCal.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH) &&
                                                scheduleCal.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR)
                                    } ?: false
                                } catch (e: Exception) {
                                    Log.e("ScheduleScreen", "Error parsing schedule date: ${e.message}", e)
                                    false
                                }
                            }
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (todaySchedules.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "Tidak ada jadwal untuk hari ini", color = Color.Gray, fontSize = 16.sp)
                                        }
                                    }
                                } else {
                                    items(todaySchedules) { schedule ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            shape = RoundedCornerShape(12.dp),
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
                                                        .size(4.dp, 40.dp)
                                                        .background(color = Color(0xFFFF914D), shape = RoundedCornerShape(2.dp))
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = schedule.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    val timeText = try {
                                                        val startTimeParts = schedule.startTime.split(" ")
                                                        val endTimeParts = schedule.endTime.split(" ")
                                                        val startTime = if (startTimeParts.size > 1) startTimeParts[1] else schedule.startTime
                                                        val endTime = if (endTimeParts.size > 1) endTimeParts[1] else schedule.endTime
                                                        "$startTime - $endTime"
                                                    } catch (e: Exception) {
                                                        "${schedule.startTime} - ${schedule.endTime}"
                                                    }
                                                    Text(text = timeText, fontSize = 14.sp, color = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Tombol Tambah Jadwal
                        Button(
                            onClick = {
                                showAddSchedule = true
                                Log.d("ScheduleScreen", "Show Add Schedule clicked, showAddSchedule = $showAddSchedule")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF914D))
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Jadwal", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Tambah Jadwal", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            } else {
                selectedProject?.groupId?.let {
                    ProjectDetailScreen(
                        navController,
                        selectedProject!!,
                        userId,
                        groupId = it
                    )
                }
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

        if (showAddSchedule || selectedSchedule != null) {
            AddScheduleScreen(
                navController = navController,
                userId = userId,
                groupId = groupId,
                currentWeekStart = currentWeekStart,
                onDismiss = { showAddSchedule = false; selectedSchedule = null },
                scheduleToEdit = selectedSchedule,
                onScheduleAdded = {
                    // Memaksa refresh dengan mengubah refreshTrigger
                    refreshTrigger++
                    Log.d("ScheduleScreen", "Schedule added, refreshing data with trigger $refreshTrigger")
                }
            )
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
                        apiService.updateSchedule(userId, schedule.id, schedule)
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
                val response = apiService.getProjectsByUser(userId)
                println("Respon API mentah untuk userId: $userId - ${response.body()}") // Log respons lengkap
                val data = response.body()?.data // Pastikan data diambil dari response.body()
                if (data != null) {
                    projects.clear()
                    projects.addAll(data)
                    println("Proyek berhasil diambil: ${projects.size} proyek untuk userId: $userId")
                    projects.forEach { project ->
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

    LaunchedEffect(key1 = Unit) {
        fetchProjects()
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Utama2)
        }
    } else if (projects.isEmpty() || groups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Tidak ada proyek atau grup", color = Color.Gray)
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
                val group = groups.find { it.id == project.groupId } ?: Group(id = -1, name = "No Label", icon = null.toString())
                ProjectCard(
                    project = project,
                    group = group,
                    onClick = { onProjectClick(project) }
                )
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
                text = "Label: ${group.name ?: "Unknown"}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "See Details",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                color = Utama2,
                modifier = Modifier.clickable { onClick() }
            )
        }
    }
}

@Composable
fun ProjectDetailScreen(navController: NavHostController, selectedProject: Project?, userId: Int, groupId: Int) {
    // Menangani groupId yang bisa null
    val effectiveGroupId = groupId ?: selectedProject?.groupId // Biarkan null jika keduanya null, hindari -1 sebagai default
    val context = LocalContext.current
    val projectId = selectedProject?.id ?: 0 // Hindari !! untuk keamanan null
    val coroutineScope = rememberCoroutineScope()
    val tasks = remember { mutableStateListOf<Task>() }
    var isLoading by remember { mutableStateOf(false) }
    val backgroundColor = Color(0xFFFFFBDA) // Latar belakang kuning muda
    val apiService = RetrofitInstance.api
    val groups = remember { mutableStateListOf<Group>() }

    // Palet warna
    val Utama1 = Color(0xFFFFC278) // Oranye untuk tombol
    val Utama2 = Color(0xFFFFBB70) // Oranye cerah untuk elemen aktif
    val Utama3 = Color(0xFFED9455) // Oranye gelap untuk titik dan tanggal
    val Grey1 = Color(0xFFd1d1d1) // Abu-abu untuk teks sekunder
    val Grey2 = Color(0xFFEDF0F2) // Abu-abu sangat terang

    // Fungsi untuk mengambil tugas dengan penanganan groupId null
    fun fetchTasks() {
        coroutineScope.launch {
            if (projectId == 0) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Project ID tidak valid", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            isLoading = true
            try {
                // Jika effectiveGroupId null, coba panggil API tanpa groupId atau dengan logika khusus
                val response = if (effectiveGroupId == null) {
                    apiService.getTaskWithoutGroup(userId, projectId).data // Asumsi ada endpoint alternatif
                } else {
                    apiService.getTask(userId, effectiveGroupId, projectId).data
                }
                tasks.clear()
                tasks.addAll(response)
                println("Tugas berhasil diambil: ${tasks.size} tugas untuk projectId: $projectId")
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Gagal memuat tugas: ${e.message ?: "Tidak ada detail error"}", Toast.LENGTH_SHORT).show()
                }
            }finally {
                isLoading = false
            }
        }
    }

    // Ambil data grup untuk mendapatkan ikon grup
    LaunchedEffect(Unit) {
        try {
            val response = apiService.getGroups(userId).data
            groups.clear()
            groups.addAll(response)
            println("Grup berhasil diambil: ${groups.size} grup untuk userId: $userId")
        } catch (e: Exception) {
            println("Gagal mengambil grup untuk userId: $userId - Error: ${e.message}")
            e.printStackTrace()
        }
    }

    // Ambil tugas saat layar dimuat
    LaunchedEffect(key1 = projectId) {
        fetchTasks()
    }

    // Temukan grup berdasarkan effectiveGroupId, jika null gunakan default
    val group = if (effectiveGroupId != null) {
        groups.find { it.id == effectiveGroupId } ?: Group(id = -1, name = "No Group", icon = "fas fa-users")
    } else {
        Group(id = -1, name = "No Group", icon = "fas fa-users") // Default jika tidak ada group
    }

    // Lanjutkan rendering meskipun groupId null, asalkan selectedProject ada
    if (selectedProject == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Data proyek tidak valid", color = Color.Gray)
        }
        return
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
            // Header dengan tombol kembali dan judul
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.navigate("jadwal?groupId=$effectiveGroupId&projectId=$projectId") }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Utama3,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    color = Color.Black,
                    text = "Detail Group",
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

            // Detail Group Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .clickable {
                        navController.navigate("add_todo/$userId/${effectiveGroupId ?: -1}/$projectId")
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = selectedProject.name ?: "Tanpa Nama",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = Color.Black
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Description: ${selectedProject.description ?: "Tidak ada deskripsi"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Grey1
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Tanggal dengan titik oranye
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            color = Utama3,
                                            shape = CircleShape
                                        )
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = formatDateRange(selectedProject.startDate, selectedProject.endDate),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Utama3,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Ikon Grup dari proyek
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
                        Row {
                            IconButton(onClick = {
                                navController.navigate("add_todo/$userId/${effectiveGroupId ?: -1}/$projectId")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Group",
                                    tint = Utama2
                                )
                            }
                            IconButton(onClick = {
                                println("Mencoba menghapus proyek dengan userId: $userId, groupId: $effectiveGroupId, projectId: $projectId")
                                coroutineScope.launch {
                                    try {
                                        val response = apiService.deleteProject(userId, projectId)

                                        if (response == Unit) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Proyek berhasil dihapus", Toast.LENGTH_SHORT).show()
                                            }
                                            navController.popBackStack()
                                        }
                                    } catch (e: HttpException) {
                                        when (e.code()) {
                                            403 -> {
                                                println("HTTP 403 Forbidden: ${e.message} - Kemungkinan groupId tidak sesuai")
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(
                                                        context,
                                                        "Akses ditolak: Anda tidak memiliki izin untuk menghapus proyek ini. GroupId mungkin tidak sesuai.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                            else -> withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Gagal memuat tugas: ${e.message ?: "Tidak ada detail error"}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        e.printStackTrace()
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Gagal memuat tugas: ${e.message ?: "Tidak ada detail error"}", Toast.LENGTH_SHORT).show()
                                        }
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

                        val selectedIcon = iconMapping[group.icon?.lowercase() ?: "fas fa-users"] ?: Icons.Default.Group
                        val tint = when (group.name?.lowercase() ?: group.icon?.lowercase() ?: "") {
                            "office project", "laptop" -> Color(0xFFFF6F61)
                            "personal project", "clipboard" -> Color(0xFF4FC3F7)
                            "daily study", "chart" -> Color(0xFF81C784)
                            else -> Utama2
                        }

                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(
                                    color = Color(0xFFFFE5E5),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = selectedIcon,
                                contentDescription = "Ikon Grup",
                                tint = tint,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            // Judul Bagian Tugas
            Text(
                color = Color.Black,
                text = "Task List",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Utama2
                    )
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
                            groupId = effectiveGroupId,
                            projectId = projectId,
                            navController = navController,
                            onStatusChange = { updatedTask ->
                                coroutineScope.launch {
                                    try {
                                        val response =
                                            apiService.updateTask(userId, groupId, projectId, task.id!!, TaskRequest(
                                                id = task.id,
                                                name = task.name ?: "",
                                                description = task.description,
                                                deadline = task.deadline,
                                                reminder = task.reminder,
                                                priority = task.priority ?: "Normal",
                                                attachment = task.attachment,
                                                status = updatedTask.status
                                            ))

                                        if (response.isSuccessful) {
                                            fetchTasks()
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Status tugas diperbarui!", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Gagal memperbarui status: ${response.code()}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Gagal memuat tugas: ${e.message ?: "Tidak ada detail error"}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            onDeleteClick = {
                                println("Mencoba menghapus tugas dengan userId: $userId, groupId: $effectiveGroupId, projectId: $projectId, taskId: ${task.id}")
                                coroutineScope.launch {
                                    try {
                                        val response = (task.id ?: null)?.let {
                                            apiService.deleteTask(userId, projectId,
                                                it
                                            )
                                        }
                                        fetchTasks()
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Tugas berhasil dihapus", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Gagal memuat tugas: ${e.message ?: "Tidak ada detail error"}", Toast.LENGTH_SHORT).show()
                                        }
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
                if (userId != -1 && projectId != 0) {
                    println("Navigasi ke add_task dengan userId: $userId, groupId: $effectiveGroupId, projectId: $projectId")
                    navController.navigate("add_task/$userId/${effectiveGroupId ?: -1}/$projectId/-1")
                } else {
                        Toast.makeText(
                            context,
                            "Gagal menambah tugas: Parameter tidak valid (userId: $userId, groupId: $effectiveGroupId, projectId: $projectId)",
                            Toast.LENGTH_LONG
                        ).show()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 40.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Utama1
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = "Tambah Tugas")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Task")
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    userId: Int,
    groupId: Int?,
    projectId: Int,
    navController: NavHostController,
    onStatusChange: (Task) -> Unit,
    onDeleteClick: () -> Unit
) {
    val Utama3 = Color(0xFFED9455)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = RetrofitInstance.api

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                println("Edit diklik untuk taskId: ${task.id}")
                navController.navigate("add_task/$userId/$groupId/$projectId/${task.id}")
            }
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp)
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
                    colors = CheckboxDefaults.colors(
                        checkedColor = Utama3,
                        uncheckedColor = Color.Gray
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = task.name ?: "Tanpa Nama",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    task.deadline?.let { deadline ->
                        val formattedDateTime = formatTime(deadline)
                        Text(
                            text = "⏰: $formattedDateTime",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Utama3,
                            fontSize = 12.sp
                        )
                    } ?: Text(
                        text = "⏰: No Deadline",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Utama3,
                        fontSize = 12.sp
                    )
                }
            }
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = Color.Red,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus Tugas",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

fun formatTime(deadline: String): String {
    return try {
        val parts = deadline.split(" ")
        val datePart = parts[0].split("/")
        val timePart = parts[1] // HH:MM

        val day = datePart[2].toInt()
        val month = datePart[1].toInt()
        val year = datePart[0].toInt()

        val monthNames = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val monthName = monthNames[month - 1]

        val time = timePart.substring(0, 5)

        "$day $monthName $year, $time"
    } catch (e: Exception) {
        "Invalid Date"
    }
}