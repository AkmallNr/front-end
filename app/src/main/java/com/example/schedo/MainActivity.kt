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
import com.example.schedo.ui.BottomNavItem
import com.example.schedo.util.PreferencesHelper
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel() // Panggil metode kelas

        setContent {
            val navController = rememberNavController()
            val preferencesHelper = PreferencesHelper(this)
            val userId = preferencesHelper.getUserId()
            val onboardingCompleted = preferencesHelper.isOnboardingCompleted()
            var groupId by remember { mutableStateOf(-1) }
            var projectId by remember { mutableStateOf(-1) }

            val startDestination = when {
                !onboardingCompleted -> "onboarding"
                userId == -1 -> "login"
                else -> BottomNavItem.TODO.route
            }

            AppNavHost(
                navController = navController,
                groupId = groupId,
                projectId = projectId,
                startDestination = startDestination
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            if (notificationManager.getNotificationChannel("reminder_channel") == null) {
                val channel = NotificationChannel(
                    "reminder_channel",
                    "Reminder Channel",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel untuk notifikasi pengingat"
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}