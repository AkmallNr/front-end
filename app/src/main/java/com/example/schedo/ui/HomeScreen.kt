package com.example.schedo.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.schedo.model.User
import com.example.schedo.network.RetrofitInstance
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(navController: NavHostController) {
    var users = remember { mutableStateListOf<User>() }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }



    fun fetchUsers() {
        coroutineScope.launch {
            isLoading = true
            try {
                val response = RetrofitInstance.api.getUsers().data
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
//        floatingActionButton = {
//            FloatingActionButton(
//                onClick = { navController.navigate("add_todo") },
//                containerColor = MaterialTheme.colorScheme.primary
//            ) {
//                Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
//            }
//        }
    )
    { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFEDE7F6), Color.White)
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 🔹 Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val user = users.find { it.id == 1 }


                    Text("Hello!", fontSize = 20.sp, color = Color.Gray)
                    if (user != null) {
                        Text("${user.name}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "Notifications",
                    modifier = Modifier.size(28.dp)
                )
            }

            // 🔹 Task Progress Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF673AB7))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Your today's task almost done!", color = Color.White, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator(progress = { 0.85f }, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { /* Aksi View Task */ }) {
                        Text("View Task")
                    }
                }
            }

            // 🔹 In Progress Section
            Text("In Progress", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskCard("Office Project", "Grocery shopping app design", Color(0xFFBBDEFB), Color.Blue)
                TaskCard("Personal Project", "Uber Eats redesign challenge", Color(0xFFFFCDD2), Color.Red)
            }

            // 🔹 Task Groups Section
            Text("Task Groups", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                users.forEach { user ->
                    val group = user.groups.orEmpty().find { it.name == " ngamprah plosok" }

                    if (group == null) {
                        Log.e("DEBUG", "Group 'ngamprah plosok' tidak ditemukan untuk user ${user.id}")
                        Log.d("DEBUG", "Total users: ${users.size}")
                        Log.d("DEBUG", "User ${user.id} memiliki grup: ${user.groups.orEmpty().map { it.name }}")
                    } else {
                        Log.d("DEBUG", "Group ditemukan: ${group.name}")
                    }

                    val projects = group?.projects.orEmpty()

                    if (projects.isEmpty()) {
                        Log.e("DEBUG", "Tidak ada proyek dalam grup '${group?.name}'")
                    } else {
                        projects.forEach { project ->
                            Log.d("DEBUG", "Project ditemukan: ${project.name}, Deskripsi: ${project.description}")

                            TaskGroupCard(
                                project.name.toString(),
                                project.description.toString(),
                                { 0.7f },
                                Color(0xFFFFC1E3)
                            )
                        }
                    }
                }
            }

        }
    }
}


@Composable
fun TaskCard(category: String, task: String, bgColor: Color, progressColor: Color) {
    Card(
        modifier = Modifier
            .height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(category, color = Color.DarkGray, fontSize = 12.sp)
            Text(task, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(progress = { 0.5f }, color = progressColor, modifier = Modifier.fillMaxWidth())
        }
    }
}

// 🔹 Task Group Component
@Composable
fun TaskGroupCard(title: String, taskCount: String, progress: () -> Float, progressColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(progressColor),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "${(progress() * 100).toInt()}%", fontSize = 12.sp, color = Color.Black)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(taskCount, fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.weight(1f))
            LinearProgressIndicator(progress = progress, color = progressColor, modifier = Modifier.width(80.dp))
        }
    }
}
