package com.example.schedo.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.app.TimePickerDialog
import android.os.Build
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
import com.example.schedo.receiver.ScheduleReminderReceiver
import com.example.schedo.ui.theme.Utama2
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import java.text.SimpleDateFormat
import java.util.*

// Definisikan format tanggal di luar @Composable
val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
val timeFormat = SimpleDateFormat("HH:mm", Locale("id", "ID"))
val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleScreen(
    navController: NavController,
    userId: Int,
    groupId: Int,
    currentWeekStart: Calendar,
    selectedDate: Calendar,
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
    var startTime by remember { mutableStateOf(scheduleToEdit?.startTime?.split(" ")?.getOrNull(1) ?: "") }
    var endTime by remember { mutableStateOf(scheduleToEdit?.endTime?.split(" ")?.getOrNull(1) ?: "") }
    var reminderTime by remember { mutableStateOf(scheduleToEdit?.startTime?.let { dateTimeFormat.parse(it)?.let { it1 -> Calendar.getInstance().apply { time = it1 }.apply { add(Calendar.MINUTE, -30) }.time } }?.let { dateTimeFormat.format(it) } ?: "") }
    var repeat by remember { mutableStateOf(scheduleToEdit?.repeat ?: false) }

    val formattedDate = dateFormat.format(selectedDate.time)

    LaunchedEffect(startTime) {
        if (startTime.isNotBlank() && timeFormat.parse(startTime) != null) {
            try {
                val startDateTime = "$formattedDate $startTime"
                val startCalendar = Calendar.getInstance().apply { time = dateTimeFormat.parse(startDateTime)!! }
                startCalendar.add(Calendar.MINUTE, -30)
                reminderTime = dateTimeFormat.format(startCalendar.time)
                Log.d("AddScheduleScreen", "Updated reminderTime to: $reminderTime")
            } catch (e: Exception) {
                Log.e("AddScheduleScreen", "Failed to parse startTime: $startTime", e)
                reminderTime = "" // Reset jika gagal
            }
        }
    }

    fun showTimePicker(onTimeSelected: (String) -> Unit) {
        Log.d("AddScheduleScreen", "showTimePicker called")
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, hour, minute ->
                Log.d("AddScheduleScreen", "Time selected: $hour:$minute")
                val formattedTime = String.format("%02d:%02d", hour, minute)
                onTimeSelected(formattedTime)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    fun scheduleNotification(context: Context, schedule: Schedule, reminderTime: String, notificationId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ScheduleReminderReceiver::class.java).apply {
            putExtra("SCHEDULE_NAME", schedule.name)
            putExtra("NOTIFICATION_ID", notificationId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val reminderCalendar = Calendar.getInstance().apply {
                time = dateTimeFormat.parse(reminderTime)!!
            }
            if (reminderCalendar.timeInMillis > System.currentTimeMillis()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            reminderCalendar.timeInMillis,
                            pendingIntent
                        )
                        Log.d("AddScheduleScreen", "Notification scheduled for $reminderTime with ID $notificationId")
                    } else {
                        Log.e("AddScheduleScreen", "Cannot schedule exact alarms due to permission restrictions")
                    }
                } else {
                    // Untuk API < 31, jadwalkan tanpa pemeriksaan
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        reminderCalendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d("AddScheduleScreen", "Notification scheduled for $reminderTime with ID $notificationId (API < 31)")
                }
            } else {
                Log.w("AddScheduleScreen", "Reminder time is in the past, notification not scheduled")
            }
        } catch (e: SecurityException) {
            Log.e("AddScheduleScreen", "SecurityException: Cannot schedule exact alarm", e)
        } catch (e: Exception) {
            Log.e("AddScheduleScreen", "Failed to schedule notification", e)
        }
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
                .fillMaxSize()
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

            Text("Date: $formattedDate", modifier = Modifier.padding(vertical = 8.dp))

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
                            showTimePicker { startTime = it }
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
                            showTimePicker { endTime = it }
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

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            Log.d("AddScheduleScreen", "Reminder clicked")
                            showTimePicker { time ->
                                val reminderCalendar = Calendar.getInstance().apply {
                                    setTime(selectedDate.time)
                                    val (hours, minutes) = time.split(":")
                                    set(Calendar.HOUR_OF_DAY, hours.toInt())
                                    set(Calendar.MINUTE, minutes.toInt())
                                    add(Calendar.MINUTE, -30)
                                }
                                reminderTime = dateTimeFormat.format(reminderCalendar.time)
                            }
                        },
                    color = Color.Transparent
                ) {
                    OutlinedTextField(
                        value = reminderTime.split(" ").getOrNull(1) ?: reminderTime,
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
                Spacer(modifier = Modifier.weight(1f))
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
                    val baseStartDateTime = "$formattedDate $startTime"
                    val baseEndDateTime = "$formattedDate $endTime"
                    val schedulesToUpdate = mutableListOf<Schedule>()

                    // Validasi format waktu sebelum parsing
                    if (timeFormat.parse(startTime) == null || timeFormat.parse(endTime) == null) {
                        Toast.makeText(context, "Invalid time format", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Buat jadwal dasar untuk diperbarui atau ditambahkan
                    val baseSchedule = Schedule(
                        id = scheduleToEdit?.id ?: 0,
                        name = name,
                        notes = notes,
                        day = selectedDate.get(Calendar.DAY_OF_WEEK).toString(),
                        startTime = baseStartDateTime,
                        endTime = baseEndDateTime,
                        repeat = repeat
                    )
                    schedulesToUpdate.add(baseSchedule)

                    // Jika repeat aktif dan mode edit, ambil semua jadwal berulang untuk diperbarui
                    if (repeat && scheduleToEdit != null) {
                        coroutineScope.launch {
                            val deferredRelatedSchedules = coroutineScope.async {
                                try {
                                    val startOfMonth = Calendar.getInstance().apply {
                                        time = dateTimeFormat.parse(scheduleToEdit.startTime)!!
                                        set(Calendar.DAY_OF_MONTH, 1)
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    val endOfMonth = Calendar.getInstance().apply {
                                        time = startOfMonth.time
                                        add(Calendar.MONTH, 1)
                                        add(Calendar.DAY_OF_MONTH, -1)
                                        set(Calendar.HOUR_OF_DAY, 23)
                                        set(Calendar.MINUTE, 59)
                                        set(Calendar.SECOND, 59)
                                        set(Calendar.MILLISECOND, 999)
                                    }
                                    val allSchedules = apiService.getSchedulesByDateRange(
                                        userId,
                                        dateFormat.format(startOfMonth.time),
                                        dateFormat.format(endOfMonth.time)
                                    ).body()?.data ?: emptyList()

                                    // Filter jadwal berulang berdasarkan pola (misalnya, setiap 7 hari dari tanggal awal)
                                    val baseDate = dateTimeFormat.parse(scheduleToEdit.startTime)!!
                                    allSchedules.filter { schedule ->
                                        val scheduleDate = dateTimeFormat.parse(schedule.startTime)
                                        scheduleDate?.let {
                                            val diffDays = ((scheduleDate.time - baseDate.time) / (1000 * 60 * 60 * 24)).toInt()
                                            diffDays % 7 == 0 && diffDays > 0 && schedule.repeat
                                        } ?: false
                                    }
                                } catch (e: Exception) {
                                    Log.e("AddScheduleScreen", "Failed to fetch related schedules", e)
                                    emptyList()
                                }
                            }

                            val relatedSchedules = deferredRelatedSchedules.await()
                            relatedSchedules.forEach { relatedSchedule ->
                                schedulesToUpdate.add(
                                    relatedSchedule.copy(
                                        name = name,
                                        notes = notes,
                                        startTime = relatedSchedule.startTime.split(" ")[0] + " $startTime",
                                        endTime = relatedSchedule.endTime.split(" ")[0] + " $endTime",
                                        repeat = true
                                    )
                                )
                            }
                        }
                    }

                    // Jika repeat aktif dan mode tambah baru, buat jadwal berulang
                    if (repeat && scheduleToEdit == null) {
                        val baseCalendar = Calendar.getInstance().apply {
                            time = dateTimeFormat.parse(baseStartDateTime) ?: selectedDate.time
                        }
                        for (i in 1..4) {
                            val repeatCalendar = Calendar.getInstance().apply { time = baseCalendar.time; add(Calendar.WEEK_OF_YEAR, i) }
                            val repeatStartDateTime = dateTimeFormat.format(repeatCalendar.time).replace(
                                timeFormat.format(repeatCalendar.time),
                                startTime
                            )
                            val repeatEndDateTime = dateTimeFormat.format(repeatCalendar.time).replace(
                                timeFormat.format(repeatCalendar.time),
                                endTime
                            )
                            schedulesToUpdate.add(
                                Schedule(
                                    id = 0,
                                    name = name,
                                    notes = notes,
                                    day = repeatCalendar.get(Calendar.DAY_OF_WEEK).toString(),
                                    startTime = repeatStartDateTime,
                                    endTime = repeatEndDateTime,
                                    repeat = true
                                )
                            )
                        }
                    }

                    coroutineScope.launch {
                        try {
                            if (scheduleToEdit != null) {
                                // Mode edit: update semua jadwal terkait
                                schedulesToUpdate.forEachIndexed { index, schedule ->
                                    Log.d("AddScheduleScreen", "Updating schedule with id ${schedule.id}: day=${schedule.day}, startTime=${schedule.startTime}, endTime=${schedule.endTime}")
                                    apiService.updateSchedule(userId, schedule.id, schedule)
                                    // Jadwalkan notifikasi untuk setiap jadwal
                                    if (reminderTime.isNotBlank()) {
                                        scheduleNotification(context, schedule, reminderTime, schedule.id + index)
                                    }
                                }
                            } else {
                                // Mode tambah baru: simpan semua jadwal termasuk yang berulang
                                schedulesToUpdate.forEachIndexed { index, schedule ->
                                    Log.d("AddScheduleScreen", "Saving schedule: day=${schedule.day}, startTime=${schedule.startTime}, endTime=${schedule.endTime}")
                                    apiService.addSchedule(userId, schedule)
                                    // Jadwalkan notifikasi untuk setiap jadwal
                                    if (reminderTime.isNotBlank()) {
                                        scheduleNotification(context, schedule, reminderTime, System.currentTimeMillis().toInt() + index)
                                    }
                                }
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