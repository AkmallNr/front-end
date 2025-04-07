package com.example.schedo.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.schedo.model.Project
import com.example.schedo.model.User
import com.example.schedo.network.GroupRequest
import com.example.schedo.network.ProjectRequest
import com.example.schedo.network.RetrofitInstance
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoScreen(navController: NavController, userId: Int, groupId: Int) {
    var taskGroup by remember { mutableStateOf("Choose Group") }
    var projectName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("Choose Start Date") }
    var endDate by remember { mutableStateOf("Choose End Date") }
    val coroutineScope = rememberCoroutineScope()
    var users = remember { mutableStateListOf<User>() }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    fun fetchUsers() {
        coroutineScope.launch {
            isLoading = true
            try {
                val response = RetrofitInstance.api.getUsers()
                users.clear()
                users.addAll(response)
                println("Fetched users: $response")
            } catch (e: Exception) {
                e.printStackTrace()
                println("Error fetching users: ${e.message}")
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        fetchUsers()
    }

    fun saveProject() {
        val projectData = ProjectRequest(
            name = projectName,
            description = description,
            startDate = startDate,
            endDate = endDate
        )
        val isiProject = mapOf(
            "nama user" to (users.find { it.id == 1 }?.name ?: "Default Name"),
            "taskgroup" to taskGroup,
            "projectName" to projectName,
            "description" to description,
            "startDate" to startDate,
            "endDate" to endDate
        )
        println("Isi project : $isiProject")
        println("Saving project with data: $projectData")

        coroutineScope.launch {
            val user = users.find { it.id == 1 } ?: User(1, "Default Name", "ab123", "cb12433")
            println("Task Group yang dicari: '$taskGroup'")
            user.groups.forEach { println("Grup tersedia: '${it.name}'") }

            val groupIdFromUser = user.groups.find { it.name == taskGroup }?.id ?: groupId
            val userIdFromParam = userId
            println("user id : ${userIdFromParam}")
            println("Grup yang dimiliki user: ${user.groups.map { it.name }}")
            println("grup id : ${groupIdFromUser}")
            try {
                val response = RetrofitInstance.api.addProjectToGroup(userIdFromParam, groupIdFromUser, projectData)
                if (response.isSuccessful) {
                    println("Project saved successfully!")
                    showSuccessDialog = true
                } else {
                    println("Failed to save project: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("Error saving project: ${e.message}")
            }
        }
    }

    // Dialog Sukses
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Sukses") },
            text = { Text("Project berhasil disimpan!") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        navController.navigate(BottomNavItem.JADWAL.route) { // Changed to route to Schedule screen
                            popUpTo("add_todo") { inclusive = true }
                        }
                        taskGroup = "Choose Group"
                        projectName = ""
                        description = ""
                        startDate = "Choose Start Date"
                        endDate = "Choose End Date"
                        fetchUsers()
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSuccessDialog = false }
                ) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Add Project", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Handle notifications */ }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifikasi")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val user = users.find { it.id == 1 } ?: User(1, "Default Name", "ab123", "cb12433")

            CardField(
                user = user,
                users = users,
                label = "Task Group",
                value = taskGroup,
                onAddTaskGroup = { name ->
                    coroutineScope.launch {
                        try {
                            if (name.isNotEmpty()) {
                                user.id?.let { fetchUsers() }
                            } else {
                                println("Nama Grup tidak boleh kosong: $name")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                isDropdown = true,
                onValueChange = { taskGroup = it }
            )

            CardField(
                user = user,
                users = users,
                label = "Project Name",
                value = projectName,
                onAddTaskGroup = { /* Tidak digunakan */ },
                onValueChange = { projectName = it }
            )

            CardField(
                user = user,
                users = users,
                label = "Description",
                value = description,
                onAddTaskGroup = { /* Tidak digunakan */ },
                isMultiline = true,
                onValueChange = { description = it }
            )

            CardField(
                user = user,
                users = users,
                label = "Start Date",
                value = startDate,
                onAddTaskGroup = { /* Tidak digunakan */ },
                isDatePicker = true,
                onValueChange = { startDate = it }
            )

            CardField(
                user = user,
                users = users,
                label = "End Date",
                value = endDate,
                onAddTaskGroup = { /* Tidak digunakan */ },
                isDatePicker = true,
                onValueChange = { endDate = it }
            )

            Button(
                onClick = { saveProject() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
            ) {
                Text("Add Project", color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardField(
    user: User,
    users: List<User>,
    label: String,
    value: String,
    isMultiline: Boolean = false,
    isDropdown: Boolean = false,
    isDatePicker: Boolean = false,
    onAddTaskGroup: (String) -> Unit,
    onValueChange: (String) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("fas fa-users") }
    var iconExpanded by remember { mutableStateOf(false) }
    var pengguna = remember { mutableStateListOf<User>() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val currentOnAddGroup = rememberUpdatedState(onAddTaskGroup)

    val fontAwesomeIcons = listOf(
        Pair("fas fa-users", Icons.Default.Group),
        Pair("fas fa-folder", Icons.Default.Folder),
        Pair("fas fa-star", Icons.Default.Star),
        Pair("fas fa-home", Icons.Default.Home),
        Pair("fas fa-tasks", Icons.Default.List),
        Pair("fas fa-calendar", Icons.Default.CalendarMonth),
        Pair("fas fa-book", Icons.Default.Book),
        Pair("fas fa-bell", Icons.Default.Notifications),
        Pair("fas fa-heart", Icons.Default.Favorite),
        Pair("fas fa-check", Icons.Default.CheckCircle),
        Pair("fas fa-envelope", Icons.Default.Email),
        Pair("fas fa-image", Icons.Default.Image),
        Pair("fas fa-file", Icons.Default.Description),
        Pair("fas fa-clock", Icons.Default.AccessTime),
        Pair("fas fa-cog", Icons.Default.Settings),
        Pair("fas fa-shopping-cart", Icons.Default.ShoppingCart),
        Pair("fas fa-tag", Icons.Default.LocalOffer),
        Pair("fas fa-link", Icons.Default.Link),
        Pair("fas fa-map", Icons.Default.Place),
        Pair("fas fa-music", Icons.Default.MusicNote),
        Pair("fas fa-phone", Icons.Default.Call),
        Pair("fas fa-camera", Icons.Default.PhotoCamera),
        Pair("fas fa-search", Icons.Default.Search),
        Pair("fas fa-cloud", Icons.Default.Cloud),
        Pair("fas fa-person", Icons.Default.Person)
    )

    fun fetchUsers() {
        coroutineScope.launch {
            isLoading = true
            try {
                val response = RetrofitInstance.api.getUsers()
                pengguna.clear()
                pengguna.addAll(response)
                println("Fetched users: $response")
            } catch (e: Exception) {
                e.printStackTrace()
                println("Error fetching users: ${e.message}")
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        fetchUsers()
    }

    val taskGroups = users
        .find { it.id == 1 }
        ?.groups
        ?.map { it.name }
        ?.distinct() ?: emptyList()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("OK") }
            }
        ) {
            val datePickerState = rememberDatePickerState()
            DatePicker(state = datePickerState)
            LaunchedEffect(datePickerState.selectedDateMillis) {
                datePickerState.selectedDateMillis?.let { millis ->
                    val formattedDate = formatDate(millis)
                    onValueChange(formattedDate)
                    println("Tanggal dipilih : ${formattedDate}")
                }
            }
        }
    }
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tambah Grup") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Grup") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { keyboardController?.hide() })
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        val currentIcon = fontAwesomeIcons.find { it.first == selectedIcon }?.second ?: Icons.Default.Group

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { iconExpanded = true }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = currentIcon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pilih Ikon", modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = iconExpanded,
                            onDismissRequest = { iconExpanded = false },
                            modifier = Modifier
                                .heightIn(max = 300.dp)
                                .width(280.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                val rows = 5
                                val columns = 5
                                val itemsPerRow = fontAwesomeIcons.size / rows + (if (fontAwesomeIcons.size % rows > 0) 1 else 0)

                                for (rowIndex in 0 until rows) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        for (colIndex in 0 until columns) {
                                            val index = rowIndex * columns + colIndex
                                            if (index < fontAwesomeIcons.size) {
                                                val (iconId, materialIcon) = fontAwesomeIcons[index]
                                                IconButton(
                                                    onClick = {
                                                        selectedIcon = iconId
                                                        iconExpanded = false
                                                    },
                                                    modifier = Modifier
                                                        .padding(4.dp)
                                                        .size(40.dp)
                                                        .background(
                                                            if (selectedIcon == iconId) Color.LightGray.copy(alpha = 0.3f)
                                                            else Color.Transparent,
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                ) {
                                                    Icon(
                                                        imageVector = materialIcon,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.size(40.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotEmpty() && !isLoading) {
                        isLoading = true
                        Log.d("AddGroup", "Nama Grup: $name, Ikon: $selectedIcon")
                        coroutineScope.launch {
                            try {
                                user.id?.let { userId ->
                                    val groupRequest = GroupRequest(name = name, icon = selectedIcon)
                                    Log.d("AddGroup", "Mengirim permintaan dengan: $groupRequest")
                                    val response = RetrofitInstance.api.addGroupToUser(userId, groupRequest)
                                    if (response.isSuccessful) {
                                        currentOnAddGroup.value(name)
                                        showAddDialog = false
                                    } else {
                                        val errorBody = response.errorBody()?.string()
                                        Log.e("Retrofit Error", "Error: $errorBody")
                                    }
                                }
                            } catch (e: HttpException) {
                                Log.e("HttpException", "Error body: ${e.response()?.errorBody()?.string()}")
                            } catch (e: Exception) {
                                Log.e("Exception", "Error tak terduga: ${e.message}")
                            } finally {
                                isLoading = false
                            }
                        }
                        keyboardController?.hide()
                    } else {
                        Log.d("AddGroup", "Nama Grup kosong!")
                        errorMessage = "Nama grup tidak boleh kosong"
                    }
                }) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Tambah Grup")
                    }
                }
            },
            dismissButton = {
                Button(onClick = { showAddDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
            Spacer(modifier = Modifier.height(4.dp))

            when {
                isDropdown -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val selectedGroup = users.find { it.id == 1 }?.groups?.find { it.name == value }
                        if (selectedGroup != null) {
                            val icon = fontAwesomeIcons.find { it.first == selectedGroup.icon }?.second ?: Icons.Default.Group
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(text = value, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            taskGroups.forEach { groupName ->
                                val groupIcon = users.find { it.id == 1 }?.groups?.find { it.name == groupName }?.icon ?: "fas fa-users"
                                val materialIcon = fontAwesomeIcons.find { it.first == groupIcon }?.second ?: Icons.Default.Group
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = materialIcon,
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(groupName)
                                        }
                                    },
                                    onClick = {
                                        onValueChange(groupName)
                                        expanded = false
                                    }
                                )
                            }
                            Divider()
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Add, contentDescription = "Tambah")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Tambah Grup Tugas Baru")
                                    }
                                },
                                onClick = {
                                    showAddDialog = true
                                    expanded = false
                                    fetchUsers()
                                }
                            )
                        }
                    }
                }
                isDatePicker -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = value, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Pilih Tanggal")
                        }
                    }
                }
                else -> {
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent, shape = RoundedCornerShape(8.dp)),
                        singleLine = !isMultiline,
                        maxLines = if (isMultiline) 5 else 1,
                        readOnly = false,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                        )
                    )
                }
            }
        }
    }
}

fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(millis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(navController: NavController, userId: Int, groupId: Int) {
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                // Assuming there's an API call to get projects
                val response = RetrofitInstance.api.getProjectsByGroup(userId, groupId)
                projects = response
            } catch (e: Exception) {
                e.printStackTrace()
                println("Error fetching projects: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Schedule", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Your Scheduled Projects",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                if (projects.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No projects scheduled yet")
                    }
                } else {
                    LazyColumn {
                        items(projects.size) { index ->
                            val project = projects[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = project.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Start: ${project.startDate}",
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "End: ${project.endDate}",
                                        fontSize = 14.sp
                                    )
                                    project.description?.let {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = it,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { navController.navigate("add_todo?userId=$userId&groupId=$groupId") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                ) {
                    Text("Add New Project", color = Color.White)
                }
            }
        }
    }
}