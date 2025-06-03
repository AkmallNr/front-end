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
import com.example.schedo.model.Schedule
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
import android.app.TimePickerDialog
import android.widget.Toast

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
    var repeat by remember { mutableStateOf(scheduleToEdit?.repeat ?: false) }

    val day = scheduleToEdit?.day
        ?: currentWeekStart.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()).orEmpty()

    // TimePicker dialog handler
    fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                onTimeSelected(formattedTime)
            },
            hour,
            minute,
            true
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
                    value = day,
                    onValueChange = {},
                    label = { Text("Day") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    readOnly = true
                )
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = { Text("Reminder") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    readOnly = true,
                    trailingIcon = {
                        Icon(Icons.Default.Notifications, contentDescription = "Reminder Icon")
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = {},
                    label = { Text("Start Time") },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showTimePicker { startTime = it } },
                    singleLine = true,
                    readOnly = true
                )
                OutlinedTextField(
                    value = endTime,
                    onValueChange = {},
                    label = { Text("End Time") },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showTimePicker { endTime = it } },
                    singleLine = true,
                    readOnly = true
                )
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
                        day = day,
                        startTime = startTime,
                        endTime = endTime,
                        repeat = repeat
                    )
                    coroutineScope.launch {
                        if (scheduleToEdit == null) {
                            apiService.addSchedule(userId, schedule)
                        } else {
                            apiService.updateSchedule(userId, schedule)
                        }
                        onScheduleAdded()
                        onDismiss()
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



