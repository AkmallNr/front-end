package com.example.schedo.ui.theme

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.schedo.model.User
import com.example.schedo.network.GroupRequest
import com.example.schedo.network.RetrofitInstance
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun UserManagementScreen() {
    val coroutineScope = rememberCoroutineScope()
    var users = remember { mutableStateListOf<User>() }
    var newUserName by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showAddUser by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Fungsi fetchUsers didefinisikan di sini
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("User List", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(users) { user ->
                    val onDeleteGroup: (Int?) -> Unit = { groupId ->
                        groupId?.let { id ->
                            user.id?.let { userId ->
                                coroutineScope.launch {
                                    try {
                                        println("Menghapus grup dengan ID: $id untuk user ID: $userId")
                                        RetrofitInstance.api.deleteGroup(userId, id)
                                        fetchUsers()
                                    } catch (e: Exception) {
                                        println("Error: ${e.message}")
                                    }
                                }
                            } ?: println("Error: User ID is null")
                        } ?: println("Error: Group ID is null")
                    }

                    UserItem(
                        user = user,
                        onDelete = {
                            coroutineScope.launch {
                                try {
                                    user.id?.let {
                                        RetrofitInstance.api.deleteUser(it)
                                        fetchUsers()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        onDeleteGroup = onDeleteGroup,
                        onAddGroup = { name ->
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
                        onFetchUsers = { fetchUsers() } // Oper fetchUsers sebagai parameter
                    )
                    Divider()
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { showAddUser = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Add User")
        }

        if (showAddUser) {
            AlertDialog(
                onDismissRequest = { showAddUser = false },
                title = { Text("Add User") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newUserName,
                            onValueChange = { newUserName = it },
                            label = { Text("Enter Name") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { keyboardController?.hide() })
                        )
                        OutlinedTextField(
                            value = newEmail,
                            onValueChange = { newEmail = it },
                            label = { Text("Enter Email") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { keyboardController?.hide() })
                        )
                        OutlinedTextField(
                            value = newPass,
                            onValueChange = { newPass = it },
                            label = { Text("Enter Pass") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        coroutineScope.launch {
                            try {
                                val newUser = User(name = newUserName, email = newEmail, pass = newPass)
                                val createdUser = RetrofitInstance.api.createUser(newUser)
                                println("User created: $createdUser")
                                newUserName = ""
                                newEmail = ""
                                newPass = ""
                                fetchUsers()
                                showAddUser = false
                            } catch (e: Exception) {
                                e.printStackTrace()
                                println("Error creating user: ${e.message}")
                            }
                        }
                    }) {
                        if (isLoading) {
                            CircularProgressIndicator()
                        } else {
                            Text("Tambah User")
                        }
                    }
                },
                dismissButton = {
                    Button(onClick = { showAddUser = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun UserItem(
    user: User,
    onDelete: () -> Unit,
    onAddGroup: (String) -> Unit,
    onDeleteGroup: (Int?) -> Unit,
    onFetchUsers: () -> Unit // Tambahkan parameter untuk fetchUsers
) {
    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("fal fa-users") }
    var iconExpanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val currentOnAddGroup = rememberUpdatedState(onAddGroup)

    val fontAwesomeIcons = listOf(
        "fal fa-users" to "Uses",
        "fal fa-folder" to "Folder",
        "fal fa-star" to "Star",
        "fal fa-home" to "Home",
        "fal fa-tasks" to "Tasks"
    )

    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Text(text = "Name: ${user.name}", style = MaterialTheme.typography.titleMedium)

        if (user.groups.isNotEmpty()) {
            Text(text = "Groups:", style = MaterialTheme.typography.bodyLarge)
            Column {
                user.groups.forEach { group ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "- ${group.name} (${group.icon})",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = { onDeleteGroup(group.id) }) {
                            Text("Delete Group")
                        }
                    }
                }
            }
        } else {
            Text(text = "No groups", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { showDialog = true }, modifier = Modifier.weight(1f)) {
                Text("Add Group")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onDelete, modifier = Modifier.weight(1f)) {
                Text("Delete User")
            }
        }

        if (errorMessage != null) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = { Button(onClick = { errorMessage = null }) { Text("Tutup") } }
            ) {
                Text(text = errorMessage ?: "")
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Add Group") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Group Name") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { keyboardController?.hide() })
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Icon: ${fontAwesomeIcons.find { it.first == selectedIcon }?.second ?: "Users"}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = { iconExpanded = true }) {
                                Text("Pilih Ikon")
                            }
                            DropdownMenu(
                                expanded = iconExpanded,
                                onDismissRequest = { iconExpanded = false }
                            ) {
                                fontAwesomeIcons.forEach { (iconClass, iconName) ->
                                    DropdownMenuItem(
                                        text = { Text(iconName) },
                                        onClick = {
                                            selectedIcon = iconClass
                                            iconExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (name.isNotEmpty() && !isLoading) {
                            isLoading = true
                            Log.d("AddGroup", "Group Name: $name, Icon: $selectedIcon")

                            coroutineScope.launch {
                                try {
                                    user.id?.let { userId ->
                                        val groupRequest = GroupRequest(name = name, icon = selectedIcon)
                                        Log.d("AddGroup", "Sending request with: $groupRequest")
                                        val response = RetrofitInstance.api.addGroupToUser(userId, groupRequest)
                                        if (response.isSuccessful) {
                                            currentOnAddGroup.value(name)
                                            showDialog = false
                                            onFetchUsers() // Panggil fetchUsers dari parameter
                                        } else {
                                            val errorBody = response.errorBody()?.string()
                                            Log.e("Retrofit Error", "Error: $errorBody")
                                            errorMessage = "Gagal menambah grup: $errorBody"
                                        }
                                    }
                                } catch (e: HttpException) {
                                    val errorResponse = e.response()?.errorBody()?.string()
                                    Log.e("HttpException", "Error body: $errorResponse")
                                    errorMessage = "HTTP Error: $errorResponse"
                                } catch (e: Exception) {
                                    Log.e("Exception", "Unexpected error: ${e.message}")
                                    errorMessage = "Error: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                            keyboardController?.hide()
                        } else {
                            Log.d("AddGroup", "Group Name is empty!")
                            errorMessage = "Nama grup tidak boleh kosong"
                        }
                    }) {
                        if (isLoading) {
                            CircularProgressIndicator()
                        } else {
                            Text("Tambah Group")
                        }
                    }
                },
                dismissButton = {
                    Button(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}


//
//
//package com.example.schedo.ui
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import androidx.compose.ui.graphics.vector.ImageVector
//import java.text.SimpleDateFormat
//import java.util.*
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun AddTodoScreen(navController: NavController) {
//    var taskGroup by remember { mutableStateOf("Work") }
//    var projectName by remember { mutableStateOf("") }
//    var description by remember { mutableStateOf("") }
//    var startDate by remember { mutableStateOf("01 May, 2022") }
//    var endDate by remember { mutableStateOf("30 June, 2022") }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = {
//                    Box(
//                        modifier = Modifier.fillMaxWidth(),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Text("Add Project", fontSize = 20.sp, fontWeight = FontWeight.Bold)
//                    }
//                },
//                navigationIcon = {
//                    IconButton(onClick = { navController.popBackStack() }) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                },
//                actions = {
//                    IconButton(onClick = { /* Handle notifications */ }) {
//                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
//                    }
//                }
//            )
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//                .padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            CardField("Task Group", taskGroup, isDropdown = true) { taskGroup = it }
//            CardField("Project Name", projectName) { projectName = it }
//            CardField("Description", description, isMultiline = true) { description = it }
//            CardField("Start Date", startDate, isDatePicker = true) { startDate = it }
//            CardField("End Date", endDate, isDatePicker = true) { endDate = it }
//
//            Button(
//                onClick = { /* Save action */ },
//                modifier = Modifier.fillMaxWidth(),
//                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
//            ) {
//                Text("Save Project", color = Color.White)
//            }
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun CardField(
//    label: String,
//    value: String,
//    isMultiline: Boolean = false,
//    isDropdown: Boolean = false,
//    isDatePicker: Boolean = false,
//    onValueChange: (String) -> Unit
//) {
//    var showDatePicker by remember { mutableStateOf(false) }
//    var expanded by remember { mutableStateOf(false) }
//    var showAddDialog by remember { mutableStateOf(false) }
//
//    // Daftar task group default + ikon bawaan
//    var taskGroups by remember {
//        mutableStateOf(
//            mutableListOf(
//                "Work" to Icons.Default.Work,
//                "Personal" to Icons.Default.Person,
//                "Study" to Icons.Default.School
//            )
//        )
//    }
//
//    // Menampilkan dialog date picker jika dibuka
//    if (showDatePicker) {
//        DatePickerDialog(
//            onDismissRequest = { showDatePicker = false },
//            confirmButton = {
//                TextButton(onClick = { showDatePicker = false }) { Text("OK") }
//            }
//        ) {
//            val datePickerState = rememberDatePickerState()
//            DatePicker(state = datePickerState)
//            LaunchedEffect(datePickerState.selectedDateMillis) {
//                datePickerState.selectedDateMillis?.let { millis ->
//                    val formattedDate = formatDate(millis)
//                    onValueChange(formattedDate)
//                }
//            }
//        }
//    }
//
//    // Menampilkan dialog untuk menambahkan Task Group baru
//    if (showAddDialog) {
//        AddTaskGroupDialog(
//            onDismiss = { showAddDialog = false },
//            onAddTaskGroup = { newName, newIcon ->
//                taskGroups.add(newName to newIcon)
//                onValueChange(newName) // Pilih otomatis yang baru ditambahkan
//            }
//        )
//    }
//
//    Card(
//        shape = RoundedCornerShape(16.dp),
//        elevation = CardDefaults.cardElevation(4.dp),
//        colors = CardDefaults.cardColors(containerColor = Color(0xFFCFD8DC)), // Warna seragam
//        modifier = Modifier.fillMaxWidth()
//    ) {
//        Column(
//            modifier = Modifier
//                .padding(16.dp)
//                .background(Color(0xFFCFD8DC))
//        ) {
//            Text(
//                text = label,
//                fontSize = 14.sp,
//                fontWeight = FontWeight.Medium,
//                color = Color.Black
//            )
//            Spacer(modifier = Modifier.height(4.dp))
//
//            when {
//                isDropdown -> {
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        val selectedIcon = taskGroups.find { it.first == value }?.second ?: Icons.Default.Help
//
//                        Icon(imageVector = selectedIcon, contentDescription = "Task Group Icon")
//                        Spacer(modifier = Modifier.width(8.dp))
//
//                        Text(
//                            text = value,
//                            fontSize = 16.sp,
//                            fontWeight = FontWeight.Bold,
//                            modifier = Modifier.weight(1f)
//                        )
//
//                        IconButton(onClick = { expanded = true }) {
//                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
//                        }
//
//                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
//                            taskGroups.forEach { (name, icon) ->
//                                DropdownMenuItem(
//                                    text = {
//                                        Row(verticalAlignment = Alignment.CenterVertically) {
//                                            Icon(imageVector = icon, contentDescription = null)
//                                            Spacer(modifier = Modifier.width(8.dp))
//                                            Text(name)
//                                        }
//                                    },
//                                    onClick = {
//                                        onValueChange(name)
//                                        expanded = false
//                                    }
//                                )
//                            }
//
//                            Divider()
//
//                            DropdownMenuItem(
//                                text = {
//                                    Row(verticalAlignment = Alignment.CenterVertically) {
//                                        Icon(Icons.Default.Add, contentDescription = "Add")
//                                        Spacer(modifier = Modifier.width(8.dp))
//                                        Text("Add New Task Group")
//                                    }
//                                },
//                                onClick = {
//                                    showAddDialog = true
//                                    expanded = false
//                                }
//                            )
//                        }
//                    }
//                }
//
//                isDatePicker -> {
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(
//                            text = value,
//                            fontSize = 16.sp,
//                            fontWeight = FontWeight.Bold,
//                            modifier = Modifier.weight(1f)
//                        )
//                        IconButton(onClick = { showDatePicker = true }) {
//                            Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date")
//                        }
//                    }
//                }
//
//                else -> {
//                    OutlinedTextField(
//                        value = value,
//                        onValueChange = onValueChange,
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .background(Color(0xFFCFD8DC), shape = RoundedCornerShape(8.dp)), // Warna seragam
//                        singleLine = !isMultiline,
//                        maxLines = if (isMultiline) 5 else 1,
//                        textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold),
//                        colors = TextFieldDefaults.outlinedTextFieldColors(
//                            focusedBorderColor = Color.Transparent,
//                            unfocusedBorderColor = Color.Transparent,
//                            disabledBorderColor = Color.Transparent,
//                            containerColor = Color(0xFFCFD8DC) // Warna seragam
//                        )
//                    )
//                }
//            }
//        }
//    }
//}
//
//// Dialog untuk menambahkan Task Group baru
//@Composable
//fun AddTaskGroupDialog(onDismiss: () -> Unit, onAddTaskGroup: (String, ImageVector) -> Unit) {
//    var newTaskGroup by remember { mutableStateOf("") }
//    var selectedIcon by remember { mutableStateOf(Icons.Default.Star) }
//    val iconOptions = listOf(Icons.Default.Star, Icons.Default.Home, Icons.Default.Favorite, Icons.Default.Event)
//
//    AlertDialog(
//        onDismissRequest = onDismiss,
//        confirmButton = {
//            TextButton(onClick = {
//                if (newTaskGroup.isNotBlank()) {
//                    onAddTaskGroup(newTaskGroup, selectedIcon)
//                    onDismiss()
//                }
//            }) {
//                Text("Add")
//            }
//        },
//        dismissButton = {
//            TextButton(onClick = onDismiss) { Text("Cancel") }
//        },
//        title = { Text("Add New Task Group") },
//        text = {
//            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
//                OutlinedTextField(
//                    value = newTaskGroup,
//                    onValueChange = { newTaskGroup = it },
//                    label = { Text("Task Group Name") }
//                )
//
//                Text("Choose an Icon:")
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceAround
//                ) {
//                    iconOptions.forEach { icon ->
//                        IconButton(onClick = { selectedIcon = icon }) {
//                            Icon(imageVector = icon, contentDescription = "Icon Option")
//                        }
//                    }
//                }
//            }
//        }
//    )
//}
//
//fun formatDate(millis: Long): String {
//    val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
//    return sdf.format(Date(millis))
//}
