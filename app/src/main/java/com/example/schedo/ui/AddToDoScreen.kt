package com.example.schedo.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.schedo.model.User
import com.example.schedo.network.RetrofitInstance
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoScreen(navController: NavController) {
    var taskGroup by remember { mutableStateOf("Pilih Group") }
    var projectName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("01 May, 2022") }
    var endDate by remember { mutableStateOf("30 June, 2022") }
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

                CardField(user = null, users = users,"Task Group", taskGroup, isDropdown = true) { taskGroup = it }
                CardField(user = null, users = users,"Project Name", projectName) { projectName = it }
                CardField(user = null, users = users,"Description", description, isMultiline = true) { description = it }
                CardField(user = null, users = users,"Start Date", startDate, isDatePicker = true) { startDate = it }
                CardField(user = null, users = users,"End Date", endDate, isDatePicker = true) { endDate = it }


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
    user: User?,
    users: List<User>,
    label: String,
    value: String,
    isMultiline: Boolean = false,
    isDropdown: Boolean = false,
    isDatePicker: Boolean = false,
    onValueChange: (String) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    // Ambil daftar grup dari semua user dan hilangkan duplikasi
//    val taskGroups = users.flatMap { it.groups }.map { it.name }.distinct()
    val taskGroups = users
        .find { it.id == 1 } // Cari user dengan ID 1
        ?.groups // Ambil grup dari user tersebut
        ?.map { it.name } // Ambil hanya nama grup
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
                }
            }
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
//                                        showAddDialog = true
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
    val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}
