package com.example.schedo.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    userId: Int? = null,
    onDismiss: () -> Unit // Parameter untuk menutup modal
) {
    val context = LocalContext.current
    val preferencesHelper = remember { PreferencesHelper(context) }
    val currentUserId = userId ?: preferencesHelper.getUserId()
    var groups by remember { mutableStateOf<List<Group>>(emptyList()) }
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var projectName by remember { mutableStateOf(project?.name ?: "") }
    var description by remember { mutableStateOf(project?.description ?: "") }
    var showProjectModal by remember { mutableStateOf(true) }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val isoSdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    val initialStartDate = project?.startDate?.let {
        try {
            val parsedDate = isoSdf.parse(it)
            parsedDate?.let { date -> sdf.format(date) } ?: sdf.format(Date())
        } catch (e: Exception) {
            e.printStackTrace()
            sdf.format(Date())
        }
    } ?: sdf.format(Date())

    val initialEndDate = project?.endDate?.let {
        try {
            val parsedDate = isoSdf.parse(it)
            parsedDate?.let { date -> sdf.format(date) } ?: sdf.format(Date())
        } catch (e: Exception) {
            e.printStackTrace()
            sdf.format(Date())
        }
    } ?: sdf.format(Date())

    var startDate by remember { mutableStateOf(initialStartDate) }
    var endDate by remember { mutableStateOf(initialEndDate) }

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
                    val userList = response.body()?.data ?: emptyList()
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
            endDate = endDate,
            groupId = if (projectId != null) selectedGroup?.id else null
        )
        val isiProject = mapOf(
            "nama user" to (users.find { it.id == currentUserId }?.name ?: "Default Name"),
            "taskgroup" to (selectedGroup?.name ?: "Unknown Group"),
            "projectName" to projectName,
            "description" to description,
            "startDate" to startDate,
            "endDate" to endDate,
            "groupId" to (selectedGroup?.id?.toString() ?: "null")
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
                    showProjectModal = false
                } else {
                    val error = response.errorBody()?.string()
                    errorMessage = when (response.code()) {
                        403 -> "Akses ditolak: Anda tidak memiliki izin untuk mengedit proyek di grup ini."
                        else -> "Gagal menyimpan proyek: $error"
                    }
                    println("Gagal menyimpan proyek: $error")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Error menyimpan proyek: ${e.message}"
                println("Error menyimpan proyek: ${e.message}")
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
                        onDismiss() // Tutup modal setelah menyimpan
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

    // Tambahkan Header dengan Tombol Kembali
    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.navigate("home") } // Navigasi ke "home"
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Kembali ke Home",
                        tint = Utama2
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Tambah Proyek",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            if (showProjectModal) {
                ModalBottomSheet(
                    onDismissRequest = { showProjectModal = false; onDismiss() },
                    sheetState = rememberModalBottomSheetState(),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrimColor = Color.Black.copy(alpha = 0.5f)
                ) {
                    BottomSheetProjectModal(
                        project = project,
                        projectName = projectName,
                        onProjectNameChange = { projectName = it },
                        description = description,
                        onDescriptionChange = { description = it },
                        startDate = startDate,
                        onStartDateChange = { startDate = it },
                        endDate = endDate,
                        onEndDateChange = { endDate = it },
                        groups = groups,
                        selectedGroup = selectedGroup,
                        onGroupSelected = { selectedGroup = it },
                        onAddGroup = { /* Handle add group */ },
                        onGroupsUpdated = { fetchGroups() },
                        onSave = { saveProject() },
                        onDismiss = { showProjectModal = false; onDismiss() },
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        userId = currentUserId
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetProjectModal(
    project: Project?,
    projectName: String,
    onProjectNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    startDate: String,
    onStartDateChange: (String) -> Unit,
    endDate: String,
    onEndDateChange: (String) -> Unit,
    groups: List<Group>,
    selectedGroup: Group?,
    onGroupSelected: (Group) -> Unit,
    onAddGroup: () -> Unit,
    onGroupsUpdated: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    userId: Int
) {
    var showDatePickerStart by remember { mutableStateOf(false) }
    var showDatePickerEnd by remember { mutableStateOf(false) }
    var showAddGroupDialog by remember { mutableStateOf(false) }

    if (showAddGroupDialog) {
        AddGroupDialog(
            userId = userId,
            onDismiss = { showAddGroupDialog = false },
            onGroupAdded = {
                onGroupsUpdated()
                showAddGroupDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = if (project == null) "Add Project" else "Edit Project",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ProjectTextField(
            label = "Project Name",
            value = projectName,
            onValueChange = onProjectNameChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        ProjectTextField(
            label = "Description",
            value = description,
            onValueChange = onDescriptionChange,
            isMultiline = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Group",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        GroupChipSelector(
            groups = groups,
            selectedGroup = selectedGroup,
            onGroupSelected = onGroupSelected,
            onAddGroup = { showAddGroupDialog = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Start Date",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                DatePickerField(
                    date = startDate,
                    onDateClick = { showDatePickerStart = true }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "End Date",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                DatePickerField(
                    date = endDate,
                    onDateClick = { showDatePickerEnd = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSave,
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
                Text(if (project == null) "Add" else "Simpan Perubahan", fontSize = 18.sp)
            }
        }
    }

    if (showDatePickerStart) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerStart = false },
            confirmButton = {
                TextButton(onClick = { showDatePickerStart = false }) { Text("OK") }
            }
        ) {
            val datePickerState = rememberDatePickerState()
            DatePicker(state = datePickerState)
            LaunchedEffect(datePickerState.selectedDateMillis) {
                datePickerState.selectedDateMillis?.let { millis ->
                    val formattedDate = formatDate(millis)
                    onStartDateChange(formattedDate)
                }
            }
        }
    }

    if (showDatePickerEnd) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerEnd = false },
            confirmButton = {
                TextButton(onClick = { showDatePickerEnd = false }) { Text("OK") }
            }
        ) {
            val datePickerState = rememberDatePickerState()
            DatePicker(state = datePickerState)
            LaunchedEffect(datePickerState.selectedDateMillis) {
                datePickerState.selectedDateMillis?.let { millis ->
                    val formattedDate = formatDate(millis)
                    onEndDateChange(formattedDate)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroupDialog(
    userId: Int,
    onDismiss: () -> Unit,
    onGroupAdded: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("fas fa-users") }
    var iconExpanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

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

    AlertDialog(
        onDismissRequest = onDismiss,
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

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
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
                            val groupRequest = GroupRequest(name = name, icon = selectedIcon)
                            Log.d("AddGroup", "Mengirim permintaan dengan: $groupRequest")
                            val response = RetrofitInstance.api.addGroupToUser(userId, groupRequest)
                            if (response.isSuccessful) {
                                onGroupAdded()
                                name = ""
                            } else {
                                val errorBody = response.errorBody()?.string()
                                Log.e("Retrofit Error", "Error: $errorBody")
                                errorMessage = "Gagal menambahkan grup: $errorBody"
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
                    Text("Tambah Grup")
                }
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun GroupChipSelector(
    groups: List<Group>,
    selectedGroup: Group?,
    onGroupSelected: (Group) -> Unit,
    onAddGroup: () -> Unit
) {
    val fontAwesomeIcons = listOf(
        Pair("fas fa-users", Icons.Default.Group),
        Pair("fas fa-folder", Icons.Default.Folder),
        Pair("fas fa-star", Icons.Default.Star),
        Pair("fas fa-home", Icons.Default.Home),
        Pair("fas fa-tasks", Icons.Default.List),
        Pair("fas fa-calendar", Icons.Default.CalendarMonth),
        Pair("fas fa-book", Icons.Default.Book),
        Pair("fas fa-bell", Icons.Default.Notifications)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        groups.forEach { group ->
            val icon = fontAwesomeIcons.find { it.first == group.icon }?.second ?: Icons.Default.Group
            GroupChip(
                text = group.name ?: "Unnamed",
                icon = icon,
                selected = selectedGroup?.id == group.id,
                onClick = { onGroupSelected(group) }
            )
        }

        GroupChip(
            text = "Add",
            icon = Icons.Default.Add,
            selected = false,
            onClick = onAddGroup
        )
    }
}

@Composable
fun GroupChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Utama2 else Color(0xFFEEEEEE))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else Color.DarkGray,
                modifier = Modifier.size(16.dp)
            )
            if (text.isNotEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = text,
                    color = if (selected) Color.White else Color.DarkGray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isMultiline: Boolean = false
) {
    Column {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF5F5F5)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Utama2,
                    unfocusedBorderColor = Color.Transparent,
                    containerColor = Color.Transparent
                ),
                singleLine = !isMultiline,
                maxLines = if (isMultiline) 4 else 1
            )
        }
    }
}

@Composable
fun DatePickerField(
    date: String,
    onDateClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF5F5F5)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onDateClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = date,
                fontSize = 14.sp
            )
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "Select Date",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(millis))
}