package com.example.schedo.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.vector.ImageVector
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoScreen(navController: NavController) {
    var taskGroup by remember { mutableStateOf("Pilih Group") }
    var projectName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("Pilih Tanggal Mulai") }
    var endDate by remember { mutableStateOf("Pilih Tanggal Selesai") }
    val coroutineScope = rememberCoroutineScope()
    var users = remember { mutableStateListOf<User>() }
    var isLoading by remember { mutableStateOf(false) }


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

    // Memuat data saat pertama kali dibuka
    LaunchedEffect(Unit) {
        fetchUsers()
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Project", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Handle notifications */ }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
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


            users.forEach { user ->
                CardField(
                    user = users.find { it.id == 1 } ?: User(
                        1, "Default Name", "ab123",
                        "cb12433"
                    ),
                    users = users,
                    "Task Group", taskGroup,
                    onAddTaskGroup = { name ->
                        coroutineScope.launch {
                            try {
                                if (name.isNotEmpty()) {
                                    user.id?.let { userId ->
                                        fetchUsers()
                                    }
                                } else {
                                    println("Nama Grup tidak boleh kosong: $name")
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    isDropdown = true
                ) { taskGroup = it }
                CardField(
                    user = users.find { it.id == 1 } ?: User(
                        1,
                        "Default Name",
                        "ab123",
                        "cb12433"
                    ), users = users, "Project Name", projectName,
                    onAddTaskGroup = { name ->
                        coroutineScope.launch {
                            try {
                                if (name.isNotEmpty()) {
                                    user.id?.let { userId ->
                                        fetchUsers()
                                    }
                                } else {
                                    println("Nama Grup tidak boleh kosong: $name")
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                ) { projectName = it }
                CardField(
                    user = users.find { it.id == 1 } ?: User(
                        1,
                        "Default Name",
                        "ab123",
                        "cb12433"
                    ), users = users, "Description", description,
                    onAddTaskGroup = { name ->
                        coroutineScope.launch {
                            try {
                                if (name.isNotEmpty()) {
                                    user.id?.let { userId ->
                                        fetchUsers()
                                    }
                                } else {
                                    println("Nama Grup tidak boleh kosong: $name")
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    isMultiline = true
                ) { description = it }
                CardField(
                    user = users.find { it.id == 1 } ?: User(
                        1,
                        "Default Name",
                        "ab123",
                        "cb12433"
                    ), users = users, "Start Date", startDate,
                    onAddTaskGroup = { name ->
                        coroutineScope.launch {
                            try {
                                if (name.isNotEmpty()) {
                                    user.id?.let { userId ->
                                        fetchUsers()
                                    }
                                } else {
                                    println("Nama Grup tidak boleh kosong: $name")
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    isDatePicker = true
                ) { startDate = it }
                CardField(
                    user = users.find { it.id == 1 } ?: User(
                        1,
                        "Default Name",
                        "ab123",
                        "cb12433"
                    ), users = users, "End Date", endDate,
                    onAddTaskGroup = { name ->
                        coroutineScope.launch {
                            try {
                                if (name.isNotEmpty()) {
                                    user.id?.let { userId ->
                                        fetchUsers()
                                    }
                                } else {
                                    println("Nama Grup tidak boleh kosong: $name")
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    isDatePicker = true
                ) { endDate = it }

                fun saveProject() {
                    val projectData = ProjectRequest(name = projectName, description = description, startDate = startDate, endDate = endDate)
                    val isiProject = mapOf(
                        "nama user" to user.name,
                        "taskgroup" to taskGroup,
                        "projectName" to projectName,
                        "description" to description,
                        "startDate" to startDate,
                        "endDate" to endDate)
                    println("Isi project : $isiProject")
                    println("Saving project with data: $projectData")


                    coroutineScope.launch {
                        println("Task Group yang dicari: '$taskGroup'")
                        user.groups.forEach { println("Grup tersedia: '${it.name}'") }

                        val groupId = user.groups.find { it.name == taskGroup }?.id ?: 0
                        val userId = user.id ?: 0
                        println("user id : ${userId}")
                        println("Grup yang dimiliki user: ${user.groups.map { it.name }}")
                        println("grup id : ${groupId}")
                        try {
                            val response = RetrofitInstance.api.addProjectToGroup(userId, groupId, projectData)
                            if (response.isSuccessful) {
                                println("Project saved successfully!")
                            } else {
                                println("Failed to save project: ${response.errorBody()?.string()}")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            println("Error saving project: ${e.message}")
                        }
                    }
                }

                Button(
                    onClick = { saveProject()
                              taskGroup = taskGroup
                              projectName = " "
                              description = " "
                              startDate = "Pilih Tanggal Mulai"
                              endDate = "Pilih Tanggal Selesai"
                                fetchUsers()
                              },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                ) {
                    Text("Save Project", color = Color.White)
                }



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
    var name by remember { mutableStateOf(" ") }
    var pengguna = remember { mutableStateListOf<User>() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val currentOnAddGroup = rememberUpdatedState(onAddTaskGroup)




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

//     Memuat data saat pertama kali dibuka
    LaunchedEffect(Unit) {
        fetchUsers()
    }
    val taskGroups = users
        .find { it.id == 1 } // Cari user dengan ID 1
        ?.groups // Ambil grup dari user tersebut
        ?.map { it.name } // Ambil hanya nama grup
        ?.distinct() ?: emptyList()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false }) { Text("OK") }
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

    // Menampilkan dialog untuk menambahkan Task Group baru
    if (showAddDialog) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween){
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Group") },
                text = {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Group Name") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done // Menutup keyboard saat "Done" ditekan
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide() // Menutup keyboard saat "Done"
                            }
                        )
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        // Pastikan input nama tidak kosong
                        if (name.isNotEmpty() && !isLoading) {
                            isLoading = true // Mengatur status loading saat operasi dimulai
                            Log.d("AddGroup", "Group Name: $name")  // Menambahkan log untuk debugging

                            coroutineScope.launch {
                                try {
                                    user.id?.let { userId ->
                                        val groupRequest = GroupRequest(name)
                                        Log.d("AddGroup", "Sending request with: $groupRequest") // Log request body

                                        val response = RetrofitInstance.api.addGroupToUser(userId, groupRequest)
                                        if (response.isSuccessful) {
                                            currentOnAddGroup.value(name) // Gunakan nilai yang selalu terbaru
                                            showAddDialog = false  // Menutup dialog setelah berhasil
                                        } else {
                                            val errorBody = response.errorBody()?.string()
                                            Log.e("Retrofit Error", "Error: $errorBody")
                                        }
                                    }
                                } catch (e: HttpException) {
                                    val errorResponse = e.response()?.errorBody()?.string()
                                    Log.e("HttpException", "Error body: $errorResponse")
                                } catch (e: Exception) {
                                    Log.e("Exception", "Unexpected error: ${e.message}")
                                } finally {
                                    isLoading = false // Mengubah status loading menjadi false setelah operasi selesai
                                }
                            }
                            keyboardController?.hide() // 🔥 Tutup keyboard setelah submit
                        } else {
                            // Jika nama grup kosong
                            Log.d("AddGroup", "Group Name is empty!")  // Log jika name kosong
                            errorMessage = "Nama grup tidak boleh kosong"
                        }
                    }) {
                        if (isLoading) {
                            CircularProgressIndicator() // Menampilkan indikator loading saat mengirim request
                        } else {
                            Text("Tambah Group")
                        }
                    }
                },
                dismissButton = {
                    Button(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }


    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Light, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))

            when {
                isDropdown -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = value, fontSize = 16.sp, modifier = Modifier.weight(1f))

                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }

                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            taskGroups.forEach { groupName ->
                                DropdownMenuItem(
                                    text = { Text(groupName) },
                                    onClick = {
                                        onValueChange(groupName) // Simpan nama grup yang dipilih
                                        expanded = false
                                    }
                                )
                            }
                            Divider()

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Add, contentDescription = "Add")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Add New Task Group")
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
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date")
                        }
                    }
                }

                else -> {
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.LightGray, shape = RoundedCornerShape(8.dp)),
                        singleLine = !isMultiline,
                        maxLines = if (isMultiline) 5 else 1,
                        readOnly = false,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                            containerColor = Color.LightGray
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
