package com.example.schedo.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schedo.model.Task


@Composable
fun AddTaskScreen(onTaskAdded: (Task) -> Unit) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(TextFieldValue()) }
    var note by remember { mutableStateOf(TextFieldValue()) }
    var deadline by remember { mutableStateOf(TextFieldValue()) }
    var reminder by remember { mutableStateOf(TextFieldValue()) }
    var priority by remember { mutableStateOf("Low") }
    var status by remember { mutableStateOf(false) }
    var attachment by remember { mutableStateOf(listOf<String>()) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Tambah Task", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))

        TextField(value = name, onValueChange = { name = it }, label = { Text("Nama Task") })
        Spacer(modifier = Modifier.height(8.dp))

        TextField(value = note, onValueChange = { note = it }, label = { Text("Catatan") })
        Spacer(modifier = Modifier.height(8.dp))

        TextField(value = deadline, onValueChange = { deadline = it }, label = { Text("Deadline") })
        Spacer(modifier = Modifier.height(8.dp))

        TextField(value = reminder, onValueChange = { reminder = it }, label = { Text("Reminder") })
        Spacer(modifier = Modifier.height(8.dp))

        // Dropdown untuk prioritas
        var expanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { expanded = true }) {
                Text(priority)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf("Low", "Medium", "High").forEach {
                    DropdownMenuItem(onClick = {
                        priority = it
                        expanded = false
                    }) {
                        Text(it)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Checkbox untuk status
        Row {
            Checkbox(checked = status, onCheckedChange = { status = it })
            Text("Selesai")
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Tombol Simpan
        Button(onClick = {
            if (name.text.isNotEmpty()) {
                val task = Task(
                    id = null,
                    name = name.text,
                    note = note.text,
                    deadline = deadline.text,
                    reminder = reminder.text,
                    priority = priority,
                    attachment = attachment,
                    status = status
                )
                onTaskAdded(task)
                Toast.makeText(context, "Task Ditambahkan", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Nama Task tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Simpan")
        }
    }
}
