package com.example.schedo.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.schedo.ui.theme.UserManagementScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomNav = currentRoute !in listOf("OnLoad")

    Scaffold(
        bottomBar = {
            if (showBottomNav) BottomNavigationBar(navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.TODO.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(BottomNavItem.TODO.route) {
                HomeScreen(navController)
            }
            composable("add_todo") {
                AddTodoScreen(navController)
            }
            composable(BottomNavItem.JADWAL.route) {
                ScheduleScreen(navController)
            }
            composable(BottomNavItem.POMODORO.route) {
                PomodoroScreen(navController)
            }
            composable(BottomNavItem.PROFILE.route) {
                UserManagementScreen()
            }
        }
    }
}
