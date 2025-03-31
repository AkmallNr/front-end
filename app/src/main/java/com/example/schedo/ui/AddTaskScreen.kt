package com.example.schedo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.schedo.model.Task
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        text = { content() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(navController: NavHostController, onTaskAdded: (Task) -> Unit) {
    var taskTitle by remember { mutableStateOf(TextFieldValue()) }
    var note by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("2025/03/27 23:59") }
    var reminder by remember { mutableStateOf("Tidak") }
    var priority by remember { mutableStateOf("Normal") }
    var attachmentList by remember { mutableStateOf<List<String>?>(null) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showAttachmentDialog by remember { mutableStateOf(false) }
    var showDeadlineDatePicker by remember { mutableStateOf(false) }
    var showDeadlineTimePicker by remember { mutableStateOf(false) }
    var showReminderDatePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }
    var showPriorityDropdown by remember { mutableStateOf(false) }

    // State untuk menyimpan tanggal dan jam
    var selectedDeadlineDate by remember { mutableStateOf(Calendar.getInstance().time) }
    var selectedDeadlineTime by remember { mutableStateOf(Calendar.getInstance().time) }
    var selectedReminderDate by remember { mutableStateOf(Calendar.getInstance().time) }
    var selectedReminderTime by remember { mutableStateOf(Calendar.getInstance().time) }

    // State untuk DatePicker dan TimePicker
    val deadlineDatePickerState = rememberDatePickerState()
    val deadlineTimePickerState = rememberTimePickerState()
    val reminderDatePickerState = rememberDatePickerState()
    val reminderTimePickerState = rememberTimePickerState()

    // List prioritas
    val priorityOptions = listOf("Rendah", "Normal", "Tinggi")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Add Task",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Input Judul Tugas (name)
            OutlinedTextField(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = 20.sp),
                placeholder = { Text("Masukkan judul tugas", fontSize = 18.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Opsi Batas Waktu (Tanggal + Jam)
            EnhancedTaskOptionRow(
                icon = { Icon(Icons.Default.DateRange, contentDescription = "Deadline") },
                title = "Batas waktu",
                value = SimpleDateFormat("yyyy/MM/dd HH:mm").format(selectedDeadlineDate),
                onClick = { showDeadlineDatePicker = true }
            )

            // Opsi Pengingat (Tanggal + Jam)
            EnhancedTaskOptionRow(
                icon = { Icon(Icons.Default.Notifications, contentDescription = "Reminder") },
                title = "Pengingat",
                value = if (reminder == "Tidak") "Tidak" else SimpleDateFormat("yyyy/MM/dd HH:mm").format(selectedReminderDate),
                chipStyle = reminder != "Tidak",
                onClick = {
                    if (reminder == "Tidak") {
                        reminder = "Setel Pengingat"
                        showReminderDatePicker = true
                    } else {
                        showReminderDatePicker = true
                    }
                }
            )

            // Opsi Prioritas dengan Dropdown
            Box {
                EnhancedTaskOptionRow(
                    icon = { Icon(Icons.Default.Flag, contentDescription = "Priority") },
                    title = "Prioritas",
                    value = priority,
                    chipStyle = true,
                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown") },
                    onClick = { showPriorityDropdown = true }
                )

                DropdownMenu(
                    expanded = showPriorityDropdown,
                    onDismissRequest = { showPriorityDropdown = false },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    priorityOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, fontSize = 16.sp) },
                            onClick = {
                                priority = option
                                showPriorityDropdown = false
                            }
                        )
                    }
                }
            }

            // Opsi Catatan
            EnhancedTaskOptionRow(
                icon = { Icon(Icons.Default.Note, contentDescription = "Note") },
                title = "Catatan",
                value = if (note.isEmpty()) "TAMBAH" else "UBAH",
                buttonStyle = true,
                onClick = { showNoteDialog = true }
            )

            // Opsi Lampiran
            EnhancedTaskOptionRow(
                icon = { Icon(Icons.Default.AttachFile, contentDescription = "Attachment") },
                title = "Lampiran",
                value = if (attachmentList.isNullOrEmpty()) "TAMBAH" else "${attachmentList?.size} item",
                buttonStyle = true,
                onClick = { showAttachmentDialog = true }
            )

            // Tombol Simpan
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = {
                    val newTask = Task(
                        name = taskTitle.text,
                        note = note,
                        deadline = SimpleDateFormat("yyyy/MM/dd HH:mm").format(selectedDeadlineDate),
                        reminder = if (reminder == "Tidak") "Tidak" else SimpleDateFormat("yyyy/MM/dd HH:mm").format(selectedReminderDate),
                        priority = priority,
                        attachment = attachmentList
                    )
                    onTaskAdded(newTask)
                    navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Simpan Tugas", fontSize = 18.sp)
            }
        }

        // Dialogs and Pickers
        if (showDeadlineDatePicker) {
            DatePickerDialogContent(
                deadlineDatePickerState,
                onDismiss = { showDeadlineDatePicker = false },
                onConfirm = {
                    deadlineDatePickerState.selectedDateMillis?.let { millis ->
                        selectedDeadlineDate = Calendar.getInstance().apply { timeInMillis = millis }.time
                        showDeadlineDatePicker = false
                        showDeadlineTimePicker = true
                    }
                }
            )
        }

        if (showDeadlineTimePicker) {
            TimePickerDialogContent(
                deadlineTimePickerState,
                onDismiss = { showDeadlineTimePicker = false },
                onConfirm = {
                    val calendar = Calendar.getInstance()
                    calendar.time = selectedDeadlineDate
                    calendar.set(Calendar.HOUR_OF_DAY, deadlineTimePickerState.hour)
                    calendar.set(Calendar.MINUTE, deadlineTimePickerState.minute)
                    selectedDeadlineDate = calendar.time
                    showDeadlineTimePicker = false
                }
            )
        }

        if (showReminderDatePicker) {
            DatePickerDialogContent(
                reminderDatePickerState,
                onDismiss = {
                    showReminderDatePicker = false
                    reminder = "Tidak"
                },
                onConfirm = {
                    reminderDatePickerState.selectedDateMillis?.let { millis ->
                        selectedReminderDate = Calendar.getInstance().apply { timeInMillis = millis }.time
                        showReminderDatePicker = false
                        showReminderTimePicker = true
                    }
                }
            )
        }

        if (showReminderTimePicker) {
            TimePickerDialogContent(
                reminderTimePickerState,
                onDismiss = {
                    showReminderTimePicker = false
                    reminder = "Tidak"
                },
                onConfirm = {
                    val calendar = Calendar.getInstance()
                    calendar.time = selectedReminderDate
                    calendar.set(Calendar.HOUR_OF_DAY, reminderTimePickerState.hour)
                    calendar.set(Calendar.MINUTE, reminderTimePickerState.minute)
                    selectedReminderDate = calendar.time
                    reminder = SimpleDateFormat("yyyy/MM/dd HH:mm").format(selectedReminderDate)
                    showReminderTimePicker = false
                }
            )
        }

        // Dialog untuk Catatan
        if (showNoteDialog) {
            NoteDialogContent(
                note = note,
                onDismiss = { showNoteDialog = false },
                onConfirm = { newNote ->
                    note = newNote
                    showNoteDialog = false
                }
            )
        }

        // Dialog untuk Lampiran
        if (showAttachmentDialog) {
            AttachmentDialogContent(
                attachmentList = attachmentList,
                onDismiss = { showAttachmentDialog = false },
                onUpdate = { newList ->
                    attachmentList = newList
                }
            )
        }
    }
}

@Composable
fun EnhancedTaskOptionRow(
    icon: @Composable () -> Unit,
    title: String,
    value: String,
    chipStyle: Boolean = false,
    buttonStyle: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left section with icon and title
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                        icon()
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Title
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Right section with value
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    chipStyle -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = if (trailingIcon != null) 4.dp else 0.dp)
                        ) {
                            Text(
                                text = value,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    buttonStyle -> {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(end = if (trailingIcon != null) 4.dp else 0.dp)
                        ) {
                            Text(
                                text = value,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = value,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = if (trailingIcon != null) 4.dp else 0.dp)
                        )
                    }
                }

                if (trailingIcon != null) {
                    trailingIcon()
                }
            }
        }

        Divider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialogContent(
    datePickerState: DatePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Pilih Jam")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Batal")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialogContent(
    timePickerState: TimePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    TimePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Simpan")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Batal")
            }
        }
    ) {
        TimePicker(state = timePickerState)
    }
}

@Composable
fun NoteDialogContent(
    note: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var tempNote by remember { mutableStateOf(note) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Masukkan Catatan") },
        text = {
            OutlinedTextField(
                value = tempNote,
                onValueChange = { tempNote = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(tempNote) }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun AttachmentDialogContent(
    attachmentList: List<String>?,
    onDismiss: () -> Unit,
    onUpdate: (List<String>?) -> Unit
) {
    var tempAttachment by remember { mutableStateOf("") }
    var currentList by remember { mutableStateOf(attachmentList ?: emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Lampiran") },
        text = {
            Column {
                OutlinedTextField(
                    value = tempAttachment,
                    onValueChange = { tempAttachment = it },
                    label = { Text("Masukkan nama file/link (PDF, Foto, atau Link)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    if (tempAttachment.isNotEmpty()) {
                        currentList = currentList + tempAttachment
                        onUpdate(currentList)
                        tempAttachment = ""
                    }
                }) {
                    Text("Tambah")
                }
                if (currentList.isNotEmpty()) {
                    Text("Lampiran saat ini:")
                    currentList.forEach { attachment ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = attachment,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                currentList = currentList - attachment
                                onUpdate(currentList)
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Hapus")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Selesai")
            }
        }
    )
}