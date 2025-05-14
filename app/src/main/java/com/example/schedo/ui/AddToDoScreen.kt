package com.example.schedo.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.schedo.model.Group
import com.example.schedo.model.Project
import com.example.schedo.model.User
import com.example.schedo.network.GroupRequest
import com.example.schedo.network.ProjectRequest
import com.example.schedo.network.RetrofitInstance
import com.example.schedo.ui.theme.Utama2
import com.example.schedo.util.PreferencesHelper
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoScreen(
    navController: NavController,
    projectId: Int? = null,
    project: Project? = null,
    groupId: Int? = null,
    userId: Int? = null
) {
    val context = LocalContext.current
    val preferencesHelper = remember { PreferencesHelper(context) }
    val currentUserId = userId ?: preferencesHelper.getUserId() // Gunakan userId dari SharedPreferences jika tidak ada parameter
    var groups by remember { mutableStateOf<List<Group>>(emptyList()) }
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var projectName by remember { mutableStateOf(project?.name ?: "") }
    var description by remember { mutableStateOf(project?.description ?: "") }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var startDate by remember { mutableStateOf(project?.startDate ?: sdf.format(Date())) }
    var endDate by remember { mutableStateOf(project?.endDate ?: sdf.format(Date())) }

    val coroutineScope = rememberCoroutineScope()
    var users = remember { mutableStateListOf<User>() }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(groups, groupId) {
        if (groups.isNotEmpty() && groupId != null) {
            selectedGroup = groups.find { it.id == groupId }
        }
    }

    fun fetchUsers() {
        coroutineScope.launch {
            isLoading = true
            try {
                val response = RetrofitInstance.api.getUsers()
                if (response.isSuccessful) {
                    val userList = response.body()?.data ?: emptyList() // Ambil data dari UserListResponse
                    users.clear()
                    users.addAll(userList)
                    println("Fetched users: $userList")
                } else {
                    val error = response.errorBody()?.string()
                    errorMessage = "Failed to fetch users: $error"
                    println("Failed to fetch users: $error")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Error fetching users: ${e.message}"
                println("Error fetching users: ${e.message}")
            }
            isLoading = false
        }
    }

    fun fetchGroups() {
        if (currentUserId == -1) {
            errorMessage = "No user logged in"
            println("Error: No user logged in")
            return
        }
        coroutineScope.launch {
            try {
                val response = RetrofitInstance.api.getGroups(currentUserId)
                groups = response.data ?: emptyList()
                println("Fetched groups: $groups")
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Error fetching groups: ${e.message}"
                println("Error fetching groups: ${e.message}")
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchGroups()
        fetchUsers()
    }

    fun saveProject() {
        if (selectedGroup == null) {
            errorMessage = "Please select a task group"
            println("Error: No group selected")
            return
        }

        val projectData = ProjectRequest(
            name = projectName,
            description = description,
            startDate = startDate,
            endDate = endDate
        )
        val isiProject = mapOf(
            "nama user" to (users.find { it.id == currentUserId }?.name ?: "Default Name"),
            "taskgroup" to (selectedGroup?.name ?: "Unknown Group"),
            "projectName" to projectName,
            "description" to description,
            "startDate" to startDate,
            "endDate" to endDate
        )
        println("Isi project: $isiProject")
        println("Saving project with data: $projectData")

        coroutineScope.launch {
            val user = users.find { it.id == currentUserId }
            if (user == null) {
                errorMessage = "User not found for userId $currentUserId"
                println("Error: User not found for userId $currentUserId")
                return@launch
            }

            val groupIdToSave = selectedGroup?.id ?: 0
            val userIdToSave = user.id
            println("user id: $userIdToSave")
            println("grup id: $groupIdToSave")

            try {
                val response = if (projectId == null) {
                    RetrofitInstance.api.addProjectToGroup(userIdToSave, groupIdToSave, projectData)
                } else {
                    RetrofitInstance.api.updateProject(userIdToSave, groupIdToSave, projectId, projectData)
                }
                if (response.isSuccessful) {
                    println("Project saved successfully!")
                    showSuccessDialog = true
                } else {
                    val error = response.errorBody()?.string()
                    errorMessage = "Failed to save project: $error"
                    println("Failed to save project: $error")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Error saving project: ${e.message}"
                println("Error saving project: ${e.message}")
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Sukses") },
            text = { Text("Project berhasil disimpan!") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        navController.navigate(BottomNavItem.JADWAL.route) {
                            popUpTo("add_todo") { inclusive = true }
                        }
                        selectedGroup = null
                        projectName = ""
                        description = ""
                        startDate = sdf.format(Date())
                        endDate = sdf.format(Date())
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
            // Tampilkan pesan error jika ada
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            val user = users.find { it.id == currentUserId } ?: User(currentUserId, "Default Name", "default@example.com", emptyList(), emptyList())

            CardField(
                user = user,
                groups = groups,
                label = "Project Name",
                value = projectName,
                onAddTaskGroup = { /* Tidak digunakan */ },
                onGroupsUpdated = { /* Tidak digunakan */ },
                onValueChange = { newValue ->
                    projectName = newValue as String
                }
            )

            CardField(
                user = user,
                groups = groups,
                label = "Description",
                value = description,
                onAddTaskGroup = { /* Tidak digunakan */ },
                onGroupsUpdated = { /* Tidak digunakan */ },
                isMultiline = true,
                onValueChange = { newValue ->
                    description = newValue as String
                }
            )

            CardField(
                user = user,
                groups = groups,
                label = "Label",
                value = selectedGroup,
                onAddTaskGroup = { },
                onGroupsUpdated = { fetchGroups() },
                isDropdown = true,
                onValueChange = { newValue ->
                    selectedGroup = newValue as? Group
                }
            )

            CardField(
                user = user,
                groups = groups,
                label = "Start Date",
                value = startDate,
                onAddTaskGroup = { /* Tidak digunakan */ },
                onGroupsUpdated = { /* Tidak digunakan */ },
                isDatePicker = true,
                onValueChange = { newValue ->
                    startDate = newValue as String
                }
            )

            CardField(
                user = user,
                groups = groups,
                label = "End Date",
                value = endDate,
                onAddTaskGroup = { /* Tidak digunakan */ },
                onGroupsUpdated = { /* Tidak digunakan */ },
                isDatePicker = true,
                onValueChange = { newValue ->
                    endDate = newValue as String
                }
            )

            Button(
                onClick = { saveProject() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Utama2),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (projectId == null) "Simpan Project" else "Simpan Perubahan", fontSize = 18.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> CardField(
    user: User,
    groups: List<Group>,
    label: String,
    value: T,
    isMultiline: Boolean = false,
    isDropdown: Boolean = false,
    isDatePicker: Boolean = false,
    onAddTaskGroup: (String) -> Unit,
    onGroupsUpdated: () -> Unit,
    onValueChange: (T) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("fas fa-users") }
    var iconExpanded by remember { mutableStateOf(false) }
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
                    @Suppress("UNCHECKED_CAST")
                    onValueChange(formattedDate as T)
                    println("Tanggal dipilih: $formattedDate")
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Label") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Label Name") },
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
                            Text("Choose Icon", modifier = Modifier.weight(1f))
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
                                user.id.let { userId ->
                                    val groupRequest = GroupRequest(name = name, icon = selectedIcon)
                                    Log.d("AddGroup", "Mengirim permintaan dengan: $groupRequest")
                                    val response = RetrofitInstance.api.addGroupToUser(userId, groupRequest)
                                    if (response.isSuccessful) {
                                        currentOnAddGroup.value(name)
                                        onGroupsUpdated()
                                        showAddDialog = false
                                        name = "" // Reset field
                                    } else {
                                        val errorBody = response.errorBody()?.string()
                                        Log.e("Retrofit Error", "Error: $errorBody")
                                        errorMessage = "Gagal menambahkan grup: $errorBody"
                                    }
                                }
                            } catch (e: HttpException) {
                                Log.e("HttpException", "Error body: ${e.response()?.errorBody()?.string()}")
                                errorMessage = "HTTP Error: ${e.message}"
                            } catch (e: Exception) {
                                Log.e("Exception", "Error tak terduga: ${e.message}")
                                errorMessage = "Error: ${e.message}"
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
                        Text("Add Label")
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
                        @Suppress("UNCHECKED_CAST")
                        val selectedGroupValue = value as Group?
                        val displayText = selectedGroupValue?.name ?: "Choose Label"
                        val icon = selectedGroupValue?.let {
                            fontAwesomeIcons.find { pair -> pair.first == it.icon }?.second ?: Icons.Default.Group
                        } ?: Icons.Default.Group

                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = displayText, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            groups.forEach { groupItem ->
                                val materialIcon = fontAwesomeIcons.find { it.first == groupItem.icon }?.second ?: Icons.Default.Group
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = materialIcon,
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(groupItem.name ?: "Unnamed Group")
                                        }
                                    },
                                    onClick = {
                                        @Suppress("UNCHECKED_CAST")
                                        onValueChange(groupItem as T)
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
                                        Text("Add New Label")
                                    }
                                },
                                onClick = {
                                    showAddDialog = true
                                    expanded = false
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
                        @Suppress("UNCHECKED_CAST")
                        val dateValue = value as String
                        Text(text = dateValue, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Pilih Tanggal")
                        }
                    }
                }
                else -> {
                    @Suppress("UNCHECKED_CAST")
                    val textValue = value as String
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { newValue ->
                            @Suppress("UNCHECKED_CAST")
                            onValueChange(newValue as T)
                        },
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