package com.example.schedo.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.compose.ui.Modifier
import com.example.schedo.model.Task
import com.example.schedo.ui.theme.UserManagementScreen

@Composable
fun AppNavHost(navController: NavHostController, userId: Int, groupId: Int) {
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
            composable(
                "add_task/{userId}/{groupId}/{projectId}",
                arguments = listOf(
                    navArgument("userId") { type = NavType.IntType },
                    navArgument("groupId") { type = NavType.IntType },
                    navArgument("projectId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val args = backStackEntry.arguments
                val receivedUserId = args?.getInt("userId") ?: userId
                val receivedGroupId = args?.getInt("groupId") ?: groupId
                val receivedProjectId = args?.getInt("projectId") ?: 0

                AddTaskScreen(
                    navController = navController,
                    onTaskAdded = { task ->
                        println("Task added: $task")
                        navController.popBackStack()
                    },
                    userId = receivedUserId,
                    groupId = receivedGroupId,
                    projectId = receivedProjectId
                )
            }
            composable(BottomNavItem.JADWAL.route) {
                ScheduleScreen(navController, userId, groupId)
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