package com.example.schedo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.example.schedo.ui.AppNavHost
import com.example.schedo.util.PreferencesHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val preferencesHelper = PreferencesHelper(this) // Inisialisasi PreferencesHelper
            var groupId by remember { mutableStateOf(-1) } // Nilai default
            var projectId by remember { mutableStateOf(-1) } // Nilai default

            // Logika untuk mengatur groupId dan projectId (misalnya dari sesi atau navigasi)
            // groupId = getGroupIdFromSession()
            // projectId = getProjectIdFromSession()

            AppNavHost(
                navController = navController,
                groupId = groupId,
                projectId = projectId
            )
        }
    }
}