package com.example.schedo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@Composable
fun HomeScreen(navController: NavHostController) {
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
                    Text("Hello!", fontSize = 20.sp, color = Color.Gray)
                    Text("Livia Vaccaro", fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
                TaskGroupCard("Office Project", "23 Tasks", { 0.7f }, Color(0xFFFFC1E3))
                TaskGroupCard("Personal Project", "30 Tasks", { 0.52f }, Color(0xFFB3E5FC))
                TaskGroupCard("Daily Study", "30 Tasks", { 0.87f }, Color(0xFFFFF59D))
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
