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
            val preferencesHelper = PreferencesHelper(this)
            val userId = preferencesHelper.getUserId()
            var groupId by remember { mutableStateOf(-1) }
            var projectId by remember { mutableStateOf(-1) }

            val startDestination = if (userId != -1) "Home" else "login"

            AppNavHost(
                navController = navController,
                groupId = groupId,
                projectId = projectId,
                startDestination = startDestination
            )
        }
    }
}