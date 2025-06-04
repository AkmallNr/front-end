package com.example.schedo.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.schedo.model.Schedule
import com.example.schedo.network.RetrofitInstance
import com.example.schedo.ui.theme.Utama2
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleScreen(
    navController: NavController,
    userId: Int,
    groupId: Int,
    currentWeekStart: Calendar,
    onDismiss: () -> Unit,
    scheduleToEdit: Schedule? = null,
    onScheduleAdded: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    val apiService = RetrofitInstance.api

    var name by remember { mutableStateOf(scheduleToEdit?.name ?: "") }
    var notes by remember { mutableStateOf(scheduleToEdit?.notes ?: "") }
    var startTime by remember { mutableStateOf(scheduleToEdit?.startTime ?: "") }
    var endTime by remember { mutableStateOf(scheduleToEdit?.endTime ?: "") }
    var reminderTime by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf(scheduleToEdit?.repeat ?: false) }

    // Mendapatkan hari saat ini sebagai angka (1 = Minggu, 2 = Senin, ..., 7 = Sabtu)
    val currentDate = Calendar.getInstance()
    val day = scheduleToEdit?.day?.toIntOrNull() ?: currentDate.get(Calendar.DAY_OF_WEEK)
    // Menampilkan nama hari dalam UI
    val dayFormat = SimpleDateFormat("EEEE", Locale("id", "ID"))
    val dayDisplay = dayFormat.format(currentDate.time)

    // Formatter untuk DateTimePicker
    val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))

    // Efek untuk memperbarui reminderTime 30 menit sebelum startTime
    LaunchedEffect(startTime) {
        if (startTime.isNotBlank()) {
            try {
                val startDate = dateTimeFormat.parse(startTime)
                val calendar = Calendar.getInstance().apply {
                    time = startDate
                    add(Calendar.MINUTE, -30) // Kurangi 30 menit
                }
                reminderTime = dateTimeFormat.format(calendar.time)
                Log.d("AddScheduleScreen", "Updated reminderTime to: $reminderTime")
            } catch (e: Exception) {
                Log.e("AddScheduleScreen", "Failed to parse startTime: $startTime", e)
            }
        }
    }

    // Fungsi untuk menampilkan DateTimePicker (DatePicker diikuti TimePicker)
    fun showDateTimePicker(onDateTimeSelected: (String) -> Unit) {
        Log.d("AddScheduleScreen", "showDateTimePicker called")
        val calendar = Calendar.getInstance()

        // Tampilkan DatePickerDialog
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                Log.d("AddScheduleScreen", "Date selected: $dayOfMonth/${month + 1}/$year")
                calendar.set(year, month, dayOfMonth)
                // Setelah memilih tanggal, tampilkan TimePickerDialog
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        Log.d("AddScheduleScreen", "Time selected: $hour:$minute")
                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                        calendar.set(Calendar.MINUTE, minute)
                        val formattedDateTime = dateTimeFormat.format(calendar.time)
                        Log.d("AddScheduleScreen", "Formatted DateTime: $formattedDateTime")
                        onDateTimeSelected(formattedDateTime)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = if (scheduleToEdit == null) "Add Schedule" else "Edit Schedule",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Schedule Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                maxLines = 5
            )

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = dayDisplay, // Menampilkan nama hari dalam UI
                    onValueChange = {},
                    label = { Text("Day") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    readOnly = true
                )
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            Log.d("AddScheduleScreen", "Reminder clicked")
                            showDateTimePicker { reminderTime = it }
                        },
                    color = Color.Transparent
                ) {
                    OutlinedTextField(
                        value = reminderTime,
                        onValueChange = {},
                        label = { Text("Reminder") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        readOnly = true,
                        enabled = false,
                        trailingIcon = {
                            Icon(Icons.Default.Notifications, contentDescription = "Reminder Icon")
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            Log.d("AddScheduleScreen", "Start Time clicked")
                            showDateTimePicker { startTime = it }
                        },
                    color = Color.Transparent
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = {},
                        label = { Text("Start Time") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        readOnly = true,
                        enabled = false
                    )
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            Log.d("AddScheduleScreen", "End Time clicked")
                            showDateTimePicker { endTime = it }
                        },
                    color = Color.Transparent
                ) {
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = {},
                        label = { Text("End Time") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        readOnly = true,
                        enabled = false
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = repeat, onCheckedChange = { repeat = it })
                Text("Repeat weekly")
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isBlank() || startTime.isBlank() || endTime.isBlank()) {
                        Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val schedule = Schedule(
                        id = scheduleToEdit?.id ?: 0,
                        name = name,
                        notes = notes,
                        day = day.toString(), // Kirim day sebagai string angka (misalnya, "3" untuk Rabu)
                        startTime = startTime,
                        endTime = endTime,
                        repeat = repeat
                    )
                    coroutineScope.launch {
                        try {
                            Log.d("AddScheduleScreen", "Saving schedule with day: $day, startTime: $startTime, endTime: $endTime, reminder: $reminderTime")
                            if (scheduleToEdit == null) {
                                apiService.addSchedule(userId, schedule)
                            } else {
                                apiService.updateSchedule(userId, schedule.id, schedule)
                            }
                            onScheduleAdded()
                            onDismiss()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            Log.e("AddScheduleScreen", "Failed to save schedule", e)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF914D))
            ) {
                Text(if (scheduleToEdit == null) "Add" else "Save", color = Color.White)
            }
        }
    }
}