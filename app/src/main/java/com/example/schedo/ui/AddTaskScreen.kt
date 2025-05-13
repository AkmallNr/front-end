package com.example.schedo.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.schedo.R
import com.example.schedo.model.Quote
import com.example.schedo.model.Task
import com.example.schedo.network.QuoteRequest
import com.example.schedo.network.RetrofitInstance
import com.example.schedo.network.TaskRequest
import com.example.schedo.ui.theme.Background
import com.example.schedo.ui.theme.Grey1
import com.example.schedo.ui.theme.Grey2
import com.example.schedo.ui.theme.Utama1
import com.example.schedo.ui.theme.Utama2
import com.example.schedo.ui.theme.Utama3
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import kotlinx.coroutines.launch
//import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.Permission
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.auth.http.HttpCredentialsAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File as JavaFile

suspend fun uploadFileToDrive(
    context: Context,
    fileUri: Uri,
    fileName: String,
    imageFolderId: String,
    pdfFolderId: String
): String? {
    android.util.Log.d("UploadFileToDrive", "Memulai uploadFileToDrive untuk file: $fileName")
    return withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("UploadFileToDrive", "Membaca file kredensial JSON")
            val inputStream = context.resources.openRawResource(R.raw.schedo_455511_9c59d241a7ce)
            android.util.Log.d("UploadFileToDrive", "Membuat kredensial Service Account")
            val credentials = ServiceAccountCredentials.fromStream(inputStream)
                .createScoped(listOf(DriveScopes.DRIVE_FILE))

            android.util.Log.d("UploadFileToDrive", "Membuat instance Drive")
            val driveService = Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                HttpCredentialsAdapter(credentials)
            ).setApplicationName("Schedo").build()

            // Menentukan folder tujuan berdasarkan jenis file
            val folderId = when {
                fileName.lowercase().endsWith(".pdf") -> pdfFolderId
                fileName.lowercase().endsWith(".jpg") ||
                        fileName.lowercase().endsWith(".jpeg") ||
                        fileName.lowercase().endsWith(".png") -> imageFolderId
                else -> imageFolderId
            }
            android.util.Log.d("UploadFileToDrive", "Mengunggah $fileName ke folder ID: $folderId")

            // Membuat metadata file tanpa permissions
            val fileMetadata = File()
                .setName(fileName)
                .setParents(listOf(folderId))

            // Mendapatkan file dari URI
            val contentResolver = context.contentResolver
            val inputStreamFile = contentResolver.openInputStream(fileUri)
            if (inputStreamFile == null) {
                android.util.Log.e("UploadFileToDrive", "Gagal membuka input stream untuk URI: $fileUri")
                return@withContext null
            }

            val tempFile = JavaFile(context.cacheDir, fileName)
            inputStreamFile.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            android.util.Log.d("UploadFileToDrive", "File sementara dibuat: ${tempFile.absolutePath}")

            val mediaContent = com.google.api.client.http.FileContent(getMimeType(fileName), tempFile)

            // Mengunggah file ke Google Drive
            val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, webViewLink")
                .execute()
            android.util.Log.d("UploadFileToDrive", "File berhasil diunggah: ${uploadedFile.webViewLink}")

            // Mengatur izin file agar dapat diakses oleh "anyone" dengan peran "reader"
            val permission = Permission()
                .setType("anyone")
                .setRole("reader")
            driveService.permissions().create(uploadedFile.id, permission).execute()
            android.util.Log.d("UploadFileToDrive", "Izin file berhasil diatur: anyone dengan peran reader")

            // Mengembalikan URL publik
            uploadedFile.webViewLink.also {
                tempFile.delete()
            }
        } catch (e: Exception) {
            android.util.Log.e("UploadFileToDrive", "Gagal mengunggah file: ${e.message}", e)
            null
        }
    }
}

// Utility functions
fun getFileNameFromUri(context: Context, uri: Uri): String? {
    return when (uri.scheme) {
        "content" -> {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getColumnIndex(OpenableColumns.DISPLAY_NAME).let { index ->
                        if (index >= 0) it.getString(index) else null
                    }
                } else null
            }
        }
        "file" -> JavaFile(uri.path).name
        else -> null
    }
}

fun isUrl(text: String): Boolean {
    return text.startsWith("http://") || text.startsWith("https://")
}


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
fun AddTaskScreen(
    navController: NavHostController,
    onTaskAdded: (Task) -> Unit,
    userId: Int,
    groupId: Int,
    projectId: Int,
    taskId: Int? = null,
    task: Task? = null,
    quote: Quote? = null
) {
    android.util.Log.d("AddTaskScreen", "Received taskId: $taskId, isNull: ${taskId == null}")
    val context = LocalContext.current
    var taskTitle by remember { mutableStateOf(TextFieldValue(task?.name ?: "")) }
    var note by remember { mutableStateOf(task?.description ?: "") }
    var reminder by remember { mutableStateOf(task?.reminder ?: "Tidak") }
    var priority by remember { mutableStateOf(task?.priority ?: "Normal") }
    var attachmentList by remember { mutableStateOf<List<Pair<String, Uri>>?>(null) }
    var selectedQuote by remember { mutableStateOf(quote) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showAttachmentDialog by remember { mutableStateOf(false) }
    var showDeadlineDatePicker by remember { mutableStateOf(false) }
    var showDeadlineTimePicker by remember { mutableStateOf(false) }
    var showReminderDatePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }
    var showPriorityDropdown by remember { mutableStateOf(false) }
    var showQuoteDropdown by remember { mutableStateOf(false) }
    var showAddQuoteDialog by remember { mutableStateOf(false) }

    var quotes by remember { mutableStateOf<List<Quote>>(emptyList()) }
    var isLoadingQuotes by remember { mutableStateOf(true) }

    var selectedDeadlineDate by remember {
        mutableStateOf(
            SimpleDateFormat("yyyy/MM/dd HH:mm").parse(task?.deadline ?: SimpleDateFormat("yyyy/MM/dd HH:mm").format(Calendar.getInstance().time))
        )
    }
    var selectedReminderDate by remember {
        mutableStateOf(
            if (task?.reminder != null && task.reminder != "Tidak") SimpleDateFormat("yyyy/MM/dd HH:mm").parse(task.reminder) else Calendar.getInstance().time
        )
    }

    val deadlineDatePickerState = rememberDatePickerState()
    val deadlineTimePickerState = rememberTimePickerState()
    val reminderDatePickerState = rememberDatePickerState()
    val reminderTimePickerState = rememberTimePickerState()

    val priorityOptions = listOf("Rendah", "Normal", "Tinggi")

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            android.util.Log.d("AttachmentDialog", "File dipilih: $uri")
            val fileName = getFileNameFromUri(context, it) ?: "UnnamedFile"
            attachmentList = (attachmentList ?: emptyList()) + listOf(fileName to it)
            android.util.Log.d("AttachmentDialog", "Lampiran ditambahkan: $fileName")
        } ?: android.util.Log.d("AttachmentDialog", "URI null, tidak ada file yang dipilih")
    }

    val scope = rememberCoroutineScope()

    // Fetch quotes when screen is displayed
    LaunchedEffect(userId) {
        isLoadingQuotes = true
        try {
            val quoteResponse = RetrofitInstance.api.getQuotes(userId).data
            quotes = quoteResponse
        } catch (e: Exception) {
            e.printStackTrace()
            // Show error toast
            Toast.makeText(
                context,
                "Gagal memuat quotes: ${e.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
        }
        isLoadingQuotes = false
    }

    // Dialog untuk menambahkan quote baru
    if (showAddQuoteDialog) {
        AddQuoteDialog(
            onDismiss = { showAddQuoteDialog = false },
            onAddQuote = { content ->
                scope.launch {
                    try {
                        RetrofitInstance.api.addQuotes(
                            userId = userId,
                            quoteRequest = QuoteRequest(content = content)
                        )

                        // Refresh quotes list
                        val quoteResponse = RetrofitInstance.api.getQuotes(userId).data
                        quotes = quoteResponse

                        Toast.makeText(
                            context,
                            "Quote berhasil ditambahkan!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            context,
                            "Gagal menambahkan quote: ${e.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    // ID folder untuk "image" dan "pdf" di Google Drive
    val imageFolderId = "1CVmSom-5q4DW5_WtHChVLq7JthcwQR7h" // Ganti dengan ID folder "image"
    val pdfFolderId = "1hUwlfEyg04tq8-Zlusmb2MFJOFqD1upo"     // Ganti dengan ID folder "pdf"


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (taskId == null) "Add Task" else "Edit Task", fontSize = 20.sp, fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Utama3)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = Color.Black
                )
            )
        }
    ) { paddingValues ->
        Box {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 20.sp),
                    placeholder = { Text("Masukkan judul tugas", fontSize = 18.sp, color = Grey1) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Utama2,
                        unfocusedBorderColor = Grey1
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Deadline section
                EnhancedTaskOptionRow(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Deadline", tint = Utama2) },
                    title = "Batas waktu",
                    value = SimpleDateFormat("yyyy/MM/dd HH:mm").format(selectedDeadlineDate),
                    onClick = { showDeadlineDatePicker = true }
                )

                // reminder section
                EnhancedTaskOptionRow(
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Reminder", tint = Utama2) },
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


                // note section
                if (note.isEmpty()) {
                    EnhancedTaskOptionRow(
                        icon = { Icon(Icons.Default.Note, contentDescription = "Note", tint = Utama2) },
                        title = "Catatan",
                        value = "+",
                        buttonStyle = true,
                        onClick = { showNoteDialog = true }
                    )
                } else {
                    NoteSection(note = note, onClick = { showNoteDialog = true })
                }

                // attachment section
                EnhancedTaskOptionRow(
                    icon = { Icon(Icons.Default.AttachFile, contentDescription = "Attachment", tint = Utama2) },
                    title = "Lampiran",
                    value = if (attachmentList.isNullOrEmpty()) "+" else "${attachmentList!!.size} item",
                    buttonStyle = attachmentList.isNullOrEmpty(),
                    onClick = { showAttachmentDialog = true }
                )

                if (!attachmentList.isNullOrEmpty()) {
                    val displayAttachments = attachmentList!!.map { it.first to it.second.toString() }
                    AttachmentSection(attachmentList = displayAttachments, onClick = { showAttachmentDialog = true })
                }

                PrioritySection(
                    priority = priority,
                    options = priorityOptions,
                    showDropdown = showPriorityDropdown,
                    onShowDropdownChange = { showPriorityDropdown = it },
                    onPrioritySelected = { priority = it }
                )

                // Add Quote section here after Priority
                if (isLoadingQuotes) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Utama2)
                    }
                } else {
                    QuoteSection(
                        selectedQuote = selectedQuote,
                        quotes = quotes,
                        showDropdown = showQuoteDropdown,
                        onShowDropdownChange = { showQuoteDropdown = it },
                        onQuoteSelected = { selectedQuote = it },
                        onAddNewQuoteClick = { showAddQuoteDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
                Button(
                    onClick = {
                        if (taskTitle.text.isBlank()) {
                            Toast.makeText(context, "Judul tugas tidak boleh kosong", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        println("user id : ${userId}, group id : ${groupId}, project id ${projectId}")

                        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                        val taskRequest = TaskRequest(
                            id = task?.id,
                            name = taskTitle.text.trim(),
                            description = note.trim(),
                            deadline = dateFormat.format(selectedDeadlineDate),
                            reminder = if (reminder == "Tidak") "Tidak" else dateFormat.format(selectedReminderDate),
                            priority = priority,
                            attachment = attachmentList?.map { it.first }?.filter { it.isNotBlank() },
                            status = task?.status ?: false
                        )

                        scope.launch {
                            android.util.Log.d("AddTaskScreen", "Di dalam coroutine scope")
                            // Mengunggah file ke Google Drive menggunakan Service Account
                            val uploadedAttachments = mutableListOf<String>()
                            android.util.Log.d("AddTaskScreen", "Jumlah lampiran: ${attachmentList?.size ?: 0}")
                            attachmentList?.forEach { (fileName, uri) ->
                                android.util.Log.d("AddTaskScreen", "Mengunggah file: $fileName dengan URI: $uri")
                                if (!isUrl(fileName)) {
                                    android.util.Log.d("AddTaskScreen", "File bukan URL, mengunggah ke Google Drive")
                                    val driveUrl = uploadFileToDrive(context, uri, fileName, imageFolderId, pdfFolderId)
                                    if (driveUrl != null) {
                                        uploadedAttachments.add(driveUrl)
                                        android.util.Log.d("AddTaskScreen", "Berhasil mengunggah $fileName: $driveUrl")
                                    } else {
                                        android.util.Log.e("AddTaskScreen", "Gagal mengunggah $fileName")
                                        Toast.makeText(
                                            context,
                                            "Gagal mengunggah $fileName. Periksa Logcat untuk detail.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } else {
                                    uploadedAttachments.add(fileName)
                                    android.util.Log.d("AddTaskScreen", "Lampiran adalah URL: $fileName")
                                }
                            }

                            android.util.Log.d("AddTaskScreen", "Semua lampiran selesai diunggah: $uploadedAttachments")
                            val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                            val taskRequest = TaskRequest(
                                name = taskTitle.text.trim(),
                                description = note.trim(),
                                deadline = dateFormat.format(selectedDeadlineDate),
                                reminder = if (reminder == "Tidak") "Tidak" else dateFormat.format(selectedReminderDate),
                                priority = priority,
                                attachment = uploadedAttachments,
                                status = false,
                                quoteId = selectedQuote?.id
                            )


                            try {
                                val response = if (taskId == null) {
                                    RetrofitInstance.api.addTaskToProject(userId, groupId, projectId, taskRequest)
                                } else {
                                    RetrofitInstance.api.updateTask(userId, groupId, projectId, taskId, taskRequest)
                                }
                                if (response.isSuccessful) {
                                    Toast.makeText(context, if (taskId == null) "Tugas berhasil disimpan!" else "Tugas berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                                    onTaskAdded(response.body()!!)
                                    navController.popBackStack()
                                } else {
                                    val errorMessage = "Gagal ${if (taskId == null) "menyimpan" else "memperbarui"} tugas: ${response.code()} - ${response.message()}"
                                    android.util.Log.e("AddTaskScreen", errorMessage)
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                val errorMessage = "Error: ${e.message}"
                                android.util.Log.e("AddTaskScreen", errorMessage, e)
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Utama2)
                ) {
                    Text(if (taskId == null) "Simpan Tugas" else "Simpan Perubahan", fontSize = 18.sp)
                }
            }

            // DatePicker Dialog
            if (showDeadlineDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDeadlineDatePicker = false },
                    confirmButton = {
                        Button(onClick = {
                            deadlineDatePickerState.selectedDateMillis?.let { millis ->
                                val calendar = Calendar.getInstance()
                                calendar.timeInMillis = millis
                                selectedDeadlineDate = calendar.time
                                showDeadlineDatePicker = false
                                showDeadlineTimePicker = true
                            }
                        }) {
                            Text("Pilih Jam")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showDeadlineDatePicker = false }) {
                            Text("Batal")
                        }
                    }
                ) {
                    DatePicker(state = deadlineDatePickerState)
                }
            }

            // TimePicker Dialog
            if (showDeadlineTimePicker) {
                TimePickerDialog(
                    onDismissRequest = { showDeadlineTimePicker = false },
                    confirmButton = {
                        Button(onClick = {
                            val calendar = Calendar.getInstance()
                            calendar.time = selectedDeadlineDate
                            calendar.set(Calendar.HOUR_OF_DAY, deadlineTimePickerState.hour)
                            calendar.set(Calendar.MINUTE, deadlineTimePickerState.minute)
                            selectedDeadlineDate = calendar.time
                            showDeadlineTimePicker = false
                        }) {
                            Text("Simpan")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showDeadlineTimePicker = false }) {
                            Text("Batal")
                        }
                    }
                ) {
                    TimePicker(state = deadlineTimePickerState)
                }
            }

            // Reminder DatePicker Dialog
            if (showReminderDatePicker) {
                DatePickerDialog(
                    onDismissRequest = {
                        showReminderDatePicker = false
                        reminder = "Tidak"
                    },
                    confirmButton = {
                        Button(onClick = {
                            reminderDatePickerState.selectedDateMillis?.let { millis ->
                                val calendar = Calendar.getInstance()
                                calendar.timeInMillis = millis
                                selectedReminderDate = calendar.time
                                showReminderDatePicker = false
                                showReminderTimePicker = true
                            }
                        }) {
                            Text("Pilih Jam")
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
                            showReminderDatePicker = false
                            reminder = "Tidak"
                        }) {
                            Text("Batal")
                        }
                    }
                ) {
                    DatePicker(state = reminderDatePickerState)
                }
            }

            // Reminder TimePicker Dialog
            if (showReminderTimePicker) {
                TimePickerDialog(
                    onDismissRequest = {
                        showReminderTimePicker = false
                        reminder = "Tidak"
                    },
                    confirmButton = {
                        Button(onClick = {
                            val calendar = Calendar.getInstance()
                            calendar.time = selectedReminderDate
                            calendar.set(Calendar.HOUR_OF_DAY, reminderTimePickerState.hour)
                            calendar.set(Calendar.MINUTE, reminderTimePickerState.minute)
                            selectedReminderDate = calendar.time
                            reminder = SimpleDateFormat("yyyy/MM/dd HH:mm").format(selectedReminderDate)
                            showReminderTimePicker = false
                        }) {
                            Text("Simpan")
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
                            showReminderTimePicker = false
                            reminder = "Tidak"
                        }) {
                            Text("Batal")
                        }
                    }
                ) {
                    TimePicker(state = reminderTimePickerState)
                }
            }

            // Note Dialog
            if (showNoteDialog) {
                AlertDialog(
                    onDismissRequest = { showNoteDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showNoteDialog = false }, colors = ButtonDefaults.buttonColors(
                            Utama2)
                        ) {
                            Text("Simpan")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNoteDialog = false }) {
                            Text("Batal", color = Utama2)
                        }
                    },
                    text = {
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Utama2)
                        )
                    }
                )
            }

            // Attachment Dialog
            if (showAttachmentDialog) {
                AttachmentDialogContent(
                    attachmentList = attachmentList,
                    onDismiss = { showAttachmentDialog = false },
                    onUpdate = { newList -> attachmentList = newList },
                    filePickerLauncher = filePickerLauncher,
                    context = context
                )
            }
        }
    }
}

@Composable
fun QuoteSection(
    selectedQuote: Quote?,
    quotes: List<Quote>,
    showDropdown: Boolean,
    onShowDropdownChange: (Boolean) -> Unit,
    onQuoteSelected: (Quote) -> Unit,
    onAddNewQuoteClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clickable { onShowDropdownChange(true) }
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Utama1,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                        Icon(Icons.Default.FormatQuote, contentDescription = "Quote", tint = Utama2)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Quote Motivasi",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = selectedQuote?.content ?: "Pilih Quote",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Dropdown"
                    )
                }
            }
        }
        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { onShowDropdownChange(false) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .widthIn(max = 300.dp)
        ) {
            quotes.forEach { quote ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = quote.content.take(40).let { if (it.length == 40) "$it..." else it },
                            fontSize = 16.sp
                        )
                    },
                    onClick = {
                        onQuoteSelected(quote)
                        onShowDropdownChange(false)
                    }
                )
            }

            Divider()

            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add New Quote"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tambah Quote Baru",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                onClick = {
                    onAddNewQuoteClick()
                    onShowDropdownChange(false)
                }
            )
        }
    }
}

@Composable
fun PrioritySection(
    priority: String,
    options: List<String>,
    showDropdown: Boolean,
    onShowDropdownChange: (Boolean) -> Unit,
    onPrioritySelected: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clickable { onShowDropdownChange(true) }
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Utama1,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                        Icon(Icons.Default.Flag, contentDescription = "Priority", tint = Utama2)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Prioritas",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = priority,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Dropdown"
                    )
                }
            }
        }
        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { onShowDropdownChange(false) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-8).dp, y = 8.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 16.sp) },
                    onClick = {
                        onPrioritySelected(option)
                        onShowDropdownChange(false)
                    }
                )
            }
        }
    }
}

@Composable
fun NoteSection(note: String, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Utama1,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                        Icon(Icons.Default.Note, contentDescription = "Note", tint = Utama2)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Catatan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Text(
                text = if (note.length > 100) note.take(100) + "..." else note,
                fontSize = 16.sp,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        Divider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp
        )
    }
}

@Composable
fun AttachmentSection(attachmentList: List<Pair<String, String>>, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
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
                        Icon(Icons.Default.AttachFile, contentDescription = "Attachment")
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Lampiran",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Column {
                attachmentList.forEach { (attachmentName, _) ->
                    Text(
                        text = if (isUrl(attachmentName)) {
                            if (attachmentName.length > 30) "${attachmentName.take(27)}..." else attachmentName
                        } else {
                            if (attachmentName.length > 30) "${attachmentName.take(27)}..." else attachmentName
                        },
                        fontSize = 16.sp,
                        modifier = Modifier.padding(8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isUrl(attachmentName)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Divider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp
        )
    }
}

@Composable
fun AddQuoteDialog(
    onDismiss: () -> Unit,
    onAddQuote: (String) -> Unit
) {
    var quoteContent by remember { mutableStateOf(TextFieldValue("")) }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Quote Baru") },
        text = {
            Column {
                OutlinedTextField(
                    value = quoteContent,
                    onValueChange = { quoteContent = it },
                    label = { Text("Isi Quote") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (quoteContent.text.isNotBlank()) {
                        onAddQuote(quoteContent.text)
                        onDismiss()
                    }
                },
                enabled = quoteContent.text.isNotBlank()
            ) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Utama1,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                        icon()
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    chipStyle -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Utama1,
                            modifier = Modifier.padding(end = if (trailingIcon != null) 4.dp else 0.dp)
                        ) {
                            Text(
                                text = value,
                                fontSize = 16.sp,
                                color = Utama2,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    buttonStyle -> {
                        Surface(
                            shape = CircleShape,
                            color = Background,
                            modifier = Modifier.padding(end = if (trailingIcon != null) 4.dp else 0.dp)
                        ) {
                            Text(
                                text = value,
                                fontSize = 16.sp,
                                color = Utama2,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = value,
                            fontSize = 16.sp,
                            color = Utama2,
                            modifier = Modifier.padding(end = if (trailingIcon != null) 4.dp else 0.dp)
                        )
                    }
                }
                trailingIcon?.invoke()
            }
        }
        Divider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp
        )
    }
}

@Composable
fun AttachmentDialogContent(
    attachmentList: List<Pair<String, Uri>>?,
    onDismiss: () -> Unit,
    onUpdate: (List<Pair<String, Uri>>?) -> Unit,
    filePickerLauncher: ManagedActivityResultLauncher<String, Uri?>,
    context: Context
) {
    var tempAttachment by remember { mutableStateOf("") }
    var currentList by remember { mutableStateOf(attachmentList ?: emptyList()) }
    var isLink by remember { mutableStateOf(false) }
    var isPhoto by remember { mutableStateOf(false) }
    var isPdf by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Selesai")
            }
        },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            isLink = true
                            isPhoto = false
                            isPdf = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Link,
                            contentDescription = "Tambah URL",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Button(
                        onClick = {
                            isLink = false
                            isPhoto = true
                            isPdf = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = "Tambah Foto",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Button(
                        onClick = {
                            isLink = false
                            isPhoto = false
                            isPdf = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PictureAsPdf,
                            contentDescription = "Tambah PDF",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                when {
                    isLink -> {
                        OutlinedTextField(
                            value = tempAttachment,
                            onValueChange = { tempAttachment = it },
                            label = { Text("Masukkan URL (contoh: https://example.com)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
                        )
                    }
                    isPhoto -> {
                        Button(
                            onClick = { filePickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pilih Foto")
                        }
                    }
                    isPdf -> {
                        Button(
                            onClick = { filePickerLauncher.launch("application/pdf") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pilih PDF")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (tempAttachment.isNotEmpty()) {
                            currentList = currentList + (tempAttachment to Uri.parse(tempAttachment))
                            onUpdate(currentList)
                            tempAttachment = ""
                            isLink = false
                            isPhoto = false
                            isPdf = false
                        }
                    },
                    enabled = tempAttachment.isNotEmpty() || (isPhoto || isPdf),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Tambah",
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (currentList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Lampiran saat ini:")
                    currentList.forEach { (attachmentName, uri) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, getMimeType(attachmentName))
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "Tidak ada aplikasi yang dapat membuka file ini", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when {
                                        isUrl(attachmentName) -> Icons.Filled.Link
                                        attachmentName.endsWith(".pdf") -> Icons.Filled.PictureAsPdf
                                        attachmentName.endsWith(".png") || attachmentName.endsWith(".jpg") || attachmentName.endsWith(".jpeg") -> Icons.Filled.Image
                                        else -> Icons.Filled.Image
                                    },
                                    contentDescription = attachmentName,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (attachmentName.length > 30) "${attachmentName.take(27)}..." else attachmentName,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f),
                                color = if (isUrl(attachmentName)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = {
                                currentList = currentList - (attachmentName to uri)
                                onUpdate(currentList)
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Hapus")
                            }
                        }
                    }
                }
            }
        }
    )
}



private fun getMimeType(url: String): String? {
    val extension = url.substringAfterLast(".", "").lowercase()
    return when (extension) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "pdf" -> "application/pdf"
        else -> "*/*"
    }
}