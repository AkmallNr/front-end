package com.example.schedo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.example.schedo.ui.AppNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            var userId by remember { mutableStateOf(1) } // Nilai default
            var groupId by remember { mutableStateOf(1) } // Nilai default
            val projectId by remember { mutableStateOf(1) }


            // Logika untuk mengatur userId dan groupId (misalnya dari sesi login)
            // userId = getUserIdFromSession()
            // groupId = getGroupIdFromSession()

            AppNavHost(navController = navController, userId = userId, groupId = groupId, projectId = projectId)
        }
    }
}