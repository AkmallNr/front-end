package com.example.schedo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoScreen(navController: NavController) {
    var taskGroup by remember { mutableStateOf("Work") }
    var projectName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("01 May, 2022") }
    var endDate by remember { mutableStateOf("30 June, 2022") }

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
            CardField("Task Group", taskGroup, isDropdown = true) { taskGroup = it }
            CardField("Project Name", projectName) { projectName = it }
            CardField("Description", description, isMultiline = true) { description = it }
            CardField("Start Date", startDate, isDatePicker = true) { startDate = it }
            CardField("End Date", endDate, isDatePicker = true) { endDate = it }

            Button(
                onClick = { /* Save action */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
            ) {
                Text("Save Project", color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardField(
    label: String,
    value: String,
    isMultiline: Boolean = false,
    isDropdown: Boolean = false,
    isDatePicker: Boolean = false,
    onValueChange: (String) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Daftar task group default + ikon bawaan
    var taskGroups by remember {
        mutableStateOf(
            mutableListOf(
                "Work" to Icons.Default.Work,
                "Personal" to Icons.Default.Person,
                "Study" to Icons.Default.School
            )
        )
    }

    // Menampilkan dialog date picker jika dibuka
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
                }
            }
        }
    }

    // Menampilkan dialog untuk menambahkan Task Group baru
    if (showAddDialog) {
        AddTaskGroupDialog(
            onDismiss = { showAddDialog = false },
            onAddTaskGroup = { newName, newIcon ->
                taskGroups.add(newName to newIcon)
                onValueChange(newName) // Pilih otomatis yang baru ditambahkan
            }
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFCFD8DC)), // Warna seragam
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .background(Color(0xFFCFD8DC))
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))

            when {
                isDropdown -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val selectedIcon = taskGroups.find { it.first == value }?.second ?: Icons.Default.Help

                        Icon(imageVector = selectedIcon, contentDescription = "Task Group Icon")
                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = value,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }

                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            taskGroups.forEach { (name, icon) ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = icon, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(name)
                                        }
                                    },
                                    onClick = {
                                        onValueChange(name)
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
                        Text(
                            text = value,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
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
                            .background(Color(0xFFCFD8DC), shape = RoundedCornerShape(8.dp)), // Warna seragam
                        singleLine = !isMultiline,
                        maxLines = if (isMultiline) 5 else 1,
                        textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                            containerColor = Color(0xFFCFD8DC) // Warna seragam
                        )
                    )
                }
            }
        }
    }
}

// Dialog untuk menambahkan Task Group baru
@Composable
fun AddTaskGroupDialog(onDismiss: () -> Unit, onAddTaskGroup: (String, ImageVector) -> Unit) {
    var newTaskGroup by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(Icons.Default.Star) }
    val iconOptions = listOf(Icons.Default.Star, Icons.Default.Home, Icons.Default.Favorite, Icons.Default.Event)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                if (newTaskGroup.isNotBlank()) {
                    onAddTaskGroup(newTaskGroup, selectedIcon)
                    onDismiss()
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Add New Task Group") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newTaskGroup,
                    onValueChange = { newTaskGroup = it },
                    label = { Text("Task Group Name") }
                )

                Text("Choose an Icon:")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    iconOptions.forEach { icon ->
                        IconButton(onClick = { selectedIcon = icon }) {
                            Icon(imageVector = icon, contentDescription = "Icon Option")
                        }
                    }
                }
            }
        }
    )
}

fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}
