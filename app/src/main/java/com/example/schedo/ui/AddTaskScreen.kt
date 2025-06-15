package com.example.schedo.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.navigation.NavHostController
import com.example.schedo.R
import com.example.schedo.model.Quote
import com.example.schedo.model.Task
import com.example.schedo.network.QuoteRequest
import com.example.schedo.network.RetrofitInstance
import com.example.schedo.network.TaskRequest
import com.example.schedo.receiver.NotificationReceiver
import com.example.schedo.ui.theme.Background
import com.example.schedo.ui.theme.Grey1
import com.example.schedo.ui.theme.Utama1
import com.example.schedo.ui.theme.Utama2
import com.example.schedo.ui.theme.Utama3
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.Permission
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File as JavaFile
import java.text.SimpleDateFormat
import java.util.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

// Fungsi utilitas untuk upload file ke Google Drive
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
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
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

fun getFileNameFromUrl(url: String): String {
    return url.substringAfterLast("/", "UnnamedFile")
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
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
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
    val uploadedAttachments = remember { mutableStateListOf<String>() }
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
    var showPriorityPopup by remember { mutableStateOf(false) }
    var showQuoteDialog by remember { mutableStateOf(false) }
    var showAddQuoteDialog by remember { mutableStateOf(false) }

    var quotes by remember { mutableStateOf<List<Quote>>(emptyList()) }
    var isLoadingQuotes by remember { mutableStateOf(true) }
    var refreshQuotes by remember { mutableStateOf(0) }

    val calendar = Calendar.getInstance()
    calendar.set(2025, 4, 21, 19, 48) // Set to 07:48 PM WIB, May 21, 2025
    var selectedDeadlineDate by remember {
        mutableStateOf(
            SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).parse(
                task?.deadline ?: SimpleDateFormat("yyyy/MM/dd HH:mm").format(Calendar.getInstance().time)
            ) ?: Calendar.getInstance().time
        )
    }
    var selectedReminderDate by remember {
        mutableStateOf(
            if (task?.reminder != null && task.reminder != "Tidak") {
                SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).parse(task.reminder) ?: Calendar.getInstance().time
            } else {
                Calendar.getInstance().time
            }
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
            val fileName = getFileNameFromUri(context, it) ?: "UnnamedFile_${System.currentTimeMillis()}"
            attachmentList = (attachmentList ?: emptyList()) + listOf(fileName to it)
            android.util.Log.d("AttachmentDialog", "Lampiran ditambahkan: $fileName")
        } ?: android.util.Log.d("AttachmentDialog", "URI null, tidak ada file yang dipilih")
    }

    val scope = rememberCoroutineScope()
    val apiService = RetrofitInstance.api

    LaunchedEffect(taskId) {
        if (taskId != null && taskId != -1) {
            try {
                val response = apiService.getTaskById(userId, groupId, projectId, taskId).data
                android.util.Log.d("AddTaskScreen", "Mencoba getTaskById untuk taskId: $taskId")

                android.util.Log.d("AddTaskScreen", "Respons API: ${Gson().toJson(response)}")

                val taskToEdit = if (response is List<*>) {
                    (response as List<Task>).find { it.id == taskId }
                } else {
                    response as Task?
                }

                if (taskToEdit != null) {
                    taskTitle = TextFieldValue(taskToEdit.name ?: "")
                    note = taskToEdit.description ?: ""
                    priority = taskToEdit.priority ?: "Normal"
                    reminder = taskToEdit.reminder ?: "Tidak"
                    taskToEdit.deadline?.let {
                        selectedDeadlineDate = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).parse(it) ?: Calendar.getInstance().time
                    }
                    taskToEdit.reminder?.let {
                        if (it != "Tidak") {
                            selectedReminderDate = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).parse(it) ?: Calendar.getInstance().time
                        }
                    }
                    taskToEdit.attachment?.let { urls ->
                        attachmentList = urls.map { url ->
                            Pair(getFileNameFromUrl(url), Uri.parse(url))
                        }
                    }
                    android.util.Log.d("AddTaskScreen", "Tugas dimuat: $taskToEdit, Lampiran: ${taskToEdit.attachment}")
                } else {
                    android.util.Log.e("AddTaskScreen", "Tugas dengan ID $taskId tidak ditemukan dalam respons")
                    Toast.makeText(context, "Tugas tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("AddTaskScreen", "Gagal memuat tugas: ${e.message}", e)
                Toast.makeText(context, "Gagal memuat tugas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(userId, refreshQuotes) {
        isLoadingQuotes = true
        try {
            val quoteResponse = apiService.getQuotes(userId).data
            quotes = quoteResponse
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Gagal memuat quotes: ${e.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
        }
        isLoadingQuotes = false
    }

    if (showAddQuoteDialog) {
        AddQuoteDialog(
            onDismiss = { showAddQuoteDialog = false },
            onAddQuote = { content ->
                scope.launch {
                    try {
                        apiService.addQuotes(userId = userId, quoteRequest = QuoteRequest(content = content))
                        val quoteResponse = apiService.getQuotes(userId).data
                        quotes = quoteResponse
                        Toast.makeText(context, "Quote berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Gagal menambahkan quote: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (taskId == null) "Add Task" else "Edit Task", fontSize = 20.sp, fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Utama3)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(titleContentColor = Color.Black)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 20.sp),
                    placeholder = { Text("Masukkan judul tugas", fontSize = 18.sp, color = Grey1) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Utama2, unfocusedBorderColor = Grey1),
                    singleLine = true
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                EnhancedTaskOptionRow(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Deadline", tint = Utama2) },
                    title = "Batas waktu",
                    value = SimpleDateFormat("yyyy/MM/dd HH:mm").format(selectedDeadlineDate),
                    onClick = { showDeadlineDatePicker = true }
                )
            }

            item {
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
            }

            item {
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
            }

            item {
                EnhancedTaskOptionRow(
                    icon = { Icon(Icons.Default.AttachFile, contentDescription = "Attachment", tint = Utama2) },
                    title = "Lampiran",
                    value = if (attachmentList.isNullOrEmpty()) "+" else "${attachmentList!!.size} item",
                    buttonStyle = attachmentList.isNullOrEmpty(),
                    onClick = { showAttachmentDialog = true }
                )
            }

            item {
                if (!attachmentList.isNullOrEmpty()) {
                    val displayAttachments = attachmentList!!.map { it.first to it.second.toString() }
                    AttachmentSection(attachmentList = displayAttachments, onClick = { showAttachmentDialog = true })
                }
            }

            item {
                PrioritySection(
                    priority = priority,
                    options = priorityOptions,
                    showPopup = showPriorityPopup,
                    onShowPopupChange = { showPriorityPopup = it },
                    onPrioritySelected = { priority = it }
                )
            }

            item {
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
                        showDialog = showQuoteDialog,
                        onShowDialogChange = { showQuoteDialog = it },
                        onQuoteSelected = { selectedQuote = it },
                        onAddNewQuoteClick = { showAddQuoteDialog = true },
                        userId = userId,
                        onQuotesUpdated = { updatedQuotes ->
                            quotes = updatedQuotes
                            refreshQuotes += 1
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }

            item {
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
                            status = task?.status ?: false,
                            quoteId = selectedQuote?.id
                        )

                        scope.launch {
                            android.util.Log.d("AddTaskScreen", "Di dalam coroutine scope")
                            uploadedAttachments.clear()
                            android.util.Log.d("AddTaskScreen", "Jumlah lampiran: ${attachmentList?.size ?: 0}")
                            attachmentList?.forEach { (fileName, uri) ->
                                android.util.Log.d("AddTaskScreen", "Mengunggah: $fileName dengan URI: $uri")
                                if (isUrl(fileName) || uri.toString().startsWith("https://") || uri.toString().startsWith("http://")) {
                                    val link = if (isUrl(fileName)) fileName else uri.toString()
                                    try {
                                        val linkRequestBody = link.toRequestBody("text/plain".toMediaTypeOrNull())
                                        val response = apiService.uploadFile(file = null, link = linkRequestBody)
                                        if (response.isSuccessful && response.body()?.success == true) {
                                            response.body()?.data?.link?.let { uploadedAttachments.add(it) }
                                            android.util.Log.d("AddTaskScreen", "Link berhasil diunggah: $link")
                                        } else {
                                            val errorMessage = "Gagal mengunggah link: ${response.code()} - ${response.message()}"
                                            android.util.Log.e("AddTaskScreen", errorMessage)
                                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("AddTaskScreen", "Error mengunggah link: ${e.message}", e)
                                        Toast.makeText(context, "Error mengunggah link: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    try {
                                        val contentResolver = context.contentResolver
                                        val inputStream = contentResolver.openInputStream(uri)
                                        if (inputStream == null) {
                                            android.util.Log.e("AddTaskScreen", "Gagal membuka input stream untuk URI: $uri")
                                            Toast.makeText(context, "Gagal membuka file: $fileName", Toast.LENGTH_LONG).show()
                                            return@forEach
                                        }

                                        val tempFile = JavaFile(context.cacheDir, fileName)
                                        inputStream.use { input ->
                                            tempFile.outputStream().use { output ->
                                                input.copyTo(output)
                                            }
                                        }

                                        val requestFile = tempFile.asRequestBody(getMimeType(fileName)?.toMediaTypeOrNull())
                                        val filePart = MultipartBody.Part.createFormData("file", fileName, requestFile)
                                        val response = apiService.uploadFile(file = filePart, link = null)

                                        if (response.isSuccessful && response.body()?.success == true) {
                                            response.body()?.data?.file_url?.let { uploadedAttachments.add(it) }
                                            android.util.Log.d("AddTaskScreen", "File berhasil diunggah: $fileName, URL: ${response.body()?.data?.file_url}")
                                        } else {
                                            val errorMessage = "Gagal mengunggah file: ${response.code()} - ${response.message()}"
                                            android.util.Log.e("AddTaskScreen", errorMessage)
                                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                        }
                                        tempFile.delete()
                                    } catch (e: Exception) {
                                        android.util.Log.e("AddTaskScreen", "Error mengunggah file: ${e.message}", e)
                                        Toast.makeText(context, "Error mengunggah file: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }

                            android.util.Log.d("AddTaskScreen", "Semua lampiran selesai diunggah: $uploadedAttachments")
                            val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                            val taskRequest = TaskRequest(
                                id = taskId,
                                name = taskTitle.text.trim(),
                                description = note.trim().takeIf { it.isNotBlank() },
                                deadline = dateFormat.format(selectedDeadlineDate),
                                reminder = if (reminder == "Tidak") null else dateFormat.format(selectedReminderDate),
                                priority = priority,
                                attachment = uploadedAttachments.takeIf { it.isNotEmpty() },
                                status = task?.status ?: false,
                                quoteId = selectedQuote?.id
                            )

                            try {
                                val response = if (taskId == null) {
                                    apiService.addTaskToProject(userId, groupId, projectId, taskRequest)
                                } else {
                                    apiService.updateTask(userId, projectId, taskId, taskRequest)
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
        }

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
                    Button(
                        onClick = { showDeadlineDatePicker = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Utama2)
                    ) {
                        Text("Batal", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            ) {
                DatePicker(state = deadlineDatePickerState)
            }
        }

        if (showDeadlineTimePicker) {
            TimePickerDialog(
                onDismissRequest = { showDeadlineTimePicker = false },
                confirmButton = {
                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            calendar.time = selectedDeadlineDate
                            calendar.set(Calendar.HOUR_OF_DAY, deadlineTimePickerState.hour)
                            calendar.set(Calendar.MINUTE, deadlineTimePickerState.minute)
                            selectedDeadlineDate = calendar.time
                            showDeadlineTimePicker = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Utama2)
                    ) {
                        Text("Simpan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showDeadlineTimePicker = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Utama2)
                    ) {
                        Text("Batal", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            ) {
                TimePicker(state = deadlineTimePickerState)
            }
        }

        if (showReminderDatePicker) {
            DatePickerDialog(
                onDismissRequest = {
                    showReminderDatePicker = false
                    reminder = "Tidak"
                },
                confirmButton = {
                    Button(
                        onClick = {
                            reminderDatePickerState.selectedDateMillis?.let { millis ->
                                val calendar = Calendar.getInstance()
                                calendar.timeInMillis = millis
                                selectedReminderDate = calendar.time
                                showReminderDatePicker = false
                                showReminderTimePicker = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Utama2)
                    ) {
                        Text("Pilih Jam", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showReminderDatePicker = false
                            reminder = "Tidak"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Utama2)
                    ) {
                        Text("Batal", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            ) {
                DatePicker(state = reminderDatePickerState)
            }
        }

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

                        scheduleReminderNotification(
                            context = context,
                            reminderTime = selectedReminderDate,
                            title = "Pengingat To-Do",
                            message = "Tugasmu '${taskTitle.text}' perlu dikerjakan!\n\nQuote hari ini:\n\"${selectedQuote?.content}\""
                        )
                    }) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showReminderTimePicker = false
                            reminder = "Tidak"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Utama2)
                    ) {
                        Text("Batal", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            ) {
                TimePicker(state = reminderTimePickerState)
            }
        }

        if (showNoteDialog) {
            AlertDialog(
                onDismissRequest = { showNoteDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = { showNoteDialog = false },
                        colors = ButtonDefaults.textButtonColors(containerColor = Utama2)
                    ) {
                        Text("Simpan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
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

fun scheduleReminderNotification(
    context: Context,
    reminderTime: Date,
    title: String,
    message: String,
    notificationId: Int = Random().nextInt()
) {
    val intent = Intent(context, NotificationReceiver::class.java).apply {
        putExtra("title", title)
        putExtra("message", message)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        notificationId,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        reminderTime.time,
        pendingIntent
    )
}

@Composable
fun QuoteSection(
    selectedQuote: Quote?,
    quotes: List<Quote>,
    showDialog: Boolean,
    onShowDialogChange: (Boolean) -> Unit,
    onQuoteSelected: (Quote) -> Unit,
    onAddNewQuoteClick: () -> Unit,
    userId: Int,
    onQuotesUpdated: (List<Quote>) -> Unit
) {
    var selectedQuoteForOptions by remember { mutableStateOf<Quote?>(null) }
    var showQuoteOptionsDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clickable { onShowDialogChange(true) }
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
                    Button(
                        onClick = { onShowDialogChange(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Utama2),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = selectedQuote?.content?.take(20)?.let { if (it.length == 20) "$it..." else it } ?: "Pilih Quote",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
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
    }

    if (showDialog) {
        Dialog(onDismissRequest = { onShowDialogChange(false) }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Pilih Quote",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (quotes.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Tidak ada quote yang ditemukan")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            items(quotes) { quote ->
                                QuoteCard(
                                    quote = quote,
                                    onClick = {
                                        selectedQuoteForOptions = quote
                                        showQuoteOptionsDialog = true
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onAddNewQuoteClick()
                                onShowDialogChange(false)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Utama2,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Tambah Quote Baru")
                        }

                        Button(
                            onClick = { onShowDialogChange(false) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Utama2,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Tutup")
                        }
                    }
                }
            }
        }
    }

    if (showQuoteOptionsDialog && selectedQuoteForOptions != null) {
        QuoteOptionsDialog(
            quote = selectedQuoteForOptions!!,
            userId = userId,
            onSelect = {
                onQuoteSelected(selectedQuoteForOptions!!)
                showQuoteOptionsDialog = false
                onShowDialogChange(false)
            },
            onEdit = { updatedQuote ->
                val updatedQuotes = quotes.map { if (it.id == updatedQuote.id) updatedQuote else it }
                onQuotesUpdated(updatedQuotes)
                showQuoteOptionsDialog = false
            },
            onDelete = {
                val updatedQuotes = quotes.filter { it.id != selectedQuoteForOptions!!.id }
                onQuotesUpdated(updatedQuotes)
                showQuoteOptionsDialog = false
            },
            onDismiss = { showQuoteOptionsDialog = false }
        )
    }
}

@Composable
fun QuoteCard(quote: Quote, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Utama2, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FormatQuote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = quote.content.take(40).let { if (it.length == 40) "$it..." else it },
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = "Pilih Quote",
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun QuoteOptionsDialog(
    quote: Quote,
    userId: Int,
    onSelect: () -> Unit,
    onEdit: (Quote) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pilihan Quote",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A4A4A)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Pilih aksi untuk quote ini",
                    fontSize = 16.sp,
                    color = Color(0xFF757575)
                )
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White,
        confirmButton = {
            Button(
                onClick = onSelect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Utama2,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("Pilih")
            }
        },
        dismissButton = {
            Row {
                Button(
                    onClick = { showEditDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Utama2,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Edit")
                }
                Button(
                    onClick = {
                        if (quote.id != null) {
                            coroutineScope.launch {
                                val response = RetrofitInstance.api.deleteQuote(userId, quote.id!!)
                                if (response.isSuccessful) {
                                    onDelete()
                                } else {
                                    android.util.Log.e("QuoteOptionsDialog", "Delete failed: ${response.message()}")
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE57373),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Hapus")
                }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Utama2,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Batal")
                }
            }
        }
    )

    if (showEditDialog) {
        EditQuoteDialog(
            quote = quote,
            userId = userId,
            onDismiss = { showEditDialog = false },
            onQuoteUpdated = { updatedQuote ->
                onEdit(updatedQuote)
                showEditDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditQuoteDialog(
    quote: Quote,
    userId: Int,
    onDismiss: () -> Unit,
    onQuoteUpdated: (Quote) -> Unit
) {
    var content by remember { mutableStateOf(quote.content) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Quote",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A4A4A)
            )
        },
        text = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                modifier = Modifier.padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Isi Quote", color = Color(0xFF757575)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Utama2,
                            unfocusedBorderColor = Color(0xFFB0BEC5)
                        ),
                        maxLines = 5,
                        textStyle = TextStyle(fontWeight = FontWeight.Bold)
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFD32F2F),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.isNotEmpty() && !isLoading && quote.id != null) {
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                val quoteRequest = QuoteRequest(content = content)
                                val response = RetrofitInstance.api.updateQuote(userId, quote.id!!, quoteRequest)
                                if (response.isSuccessful) {
                                    val updatedQuote = response.body()!!
                                    onQuoteUpdated(updatedQuote)
                                    onDismiss()
                                } else {
                                    errorMessage = "Gagal memperbarui quote: ${response.message()} (Code: ${response.code()})"
                                }
                            } catch (e: Exception) {
                                errorMessage = "Error: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    } else {
                        errorMessage = "Isi quote tidak boleh kosong atau ID tidak valid"
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Utama2,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Simpan")
                }
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Utama2,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Batal")
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color(0xFFF5F5F5)
    )
}

@Composable
fun PrioritySection(
    priority: String,
    options: List<String>,
    showPopup: Boolean,
    onShowPopupChange: (Boolean) -> Unit,
    onPrioritySelected: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clickable { onShowPopupChange(true) }
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
                    Icon(Icons.Default.Flag, contentDescription = "Priority", tint = Utama2)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Prioritas",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onShowPopupChange(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Utama2),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = priority,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
        if (showPopup) {
            Popup(
                onDismissRequest = { onShowPopupChange(false) },
                alignment = Alignment.TopEnd
            ) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 8.dp
                ) {
                    Column {
                        options.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .clickable {
                                        onPrioritySelected(option)
                                        onShowPopupChange(false)
                                    }
                                    .padding(16.dp)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = option,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteSection(
    note: String,
    onClick: () -> Unit
) {
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
                    Icon(Icons.Default.Note, contentDescription = "Note", tint = Utama2)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Catatan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
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
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold
            )
        }
        Divider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp
        )
    }
}

@Composable
fun AttachmentSection(
    attachmentList: List<Pair<String, String>>,
    onClick: () -> Unit
) {
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
                    Icon(Icons.Default.AttachFile, contentDescription = "Attachment", tint = Utama2)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Lampiran",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
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
                attachmentList.forEach { (attachmentName, uri) ->
                    Text(
                        text = if (attachmentName.length > 30) "${attachmentName.take(27)}..." else attachmentName,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isUrl(uri)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
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
        title = { Text("Tambah Quote Baru", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = quoteContent,
                    onValueChange = { quoteContent = it },
                    label = { Text("Isi Quote", fontWeight = FontWeight.Bold) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    maxLines = 5,
                    textStyle = TextStyle(fontWeight = FontWeight.Bold)
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
                enabled = quoteContent.text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Utama2)
            ) {
                Text("Simpan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Utama2)
            ) {
                Text("Batal", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
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
                    icon()
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
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
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontWeight = FontWeight.Bold
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
                            modifier = Modifier.padding(end = if (trailingIcon != null) 4.dp else 0.dp),
                            fontWeight = FontWeight.Bold
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
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Utama2)
            ) {
                Text("Selesai", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
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
                            .padding(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Utama2)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
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
                            .padding(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Utama2)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
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
                            .padding(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Utama2)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
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
                            label = { Text("Masukkan URL (contoh: https://example.com)", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                            textStyle = TextStyle(fontWeight = FontWeight.Bold)
                        )
                    }
                    isPhoto -> {
                        Button(
                            onClick = { filePickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Utama2)
                        ) {
                            Text("Pilih Foto", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    isPdf -> {
                        Button(
                            onClick = { filePickerLauncher.launch("application/pdf") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Utama2)
                        ) {
                            Text("Pilih PDF", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (tempAttachment.isNotEmpty()) {
                            currentList = currentList + (getFileNameFromUrl(tempAttachment) to Uri.parse(tempAttachment))
                            onUpdate(currentList)
                            tempAttachment = ""
                            isLink = false
                            isPhoto = false
                            isPdf = false
                        }
                    },
                    enabled = tempAttachment.isNotEmpty() || (isPhoto || isPdf),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Utama2)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah",
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (currentList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Lampiran saat ini:", fontWeight = FontWeight.Bold)
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
                                        isUrl(attachmentName) -> Icons.Default.Link
                                        attachmentName.endsWith(".pdf") -> Icons.Default.PictureAsPdf
                                        attachmentName.endsWith(".png") || attachmentName.endsWith(".jpg") || attachmentName.endsWith(".jpeg") -> Icons.Default.Image
                                        else -> Icons.Default.Image
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
                                color = if (isUrl(attachmentName)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = {
                                currentList = currentList - (attachmentName to uri)
                                onUpdate(currentList)
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus")
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
               