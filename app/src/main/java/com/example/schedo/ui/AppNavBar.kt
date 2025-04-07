package com.example.schedo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.schedo.model.Task
import com.example.schedo.network.RetrofitInstance
import com.example.schedo.ui.theme.UserManagementScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(navController: NavHostController, userId: Int, groupId: Int) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomNav = currentRoute !in listOf("OnLoad", "add_task/{userId}/{groupId}/{projectId}/{taskId}")

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
            composable(
                "add_todo",
                arguments = listOf(
                    navArgument("userId") { type = NavType.IntType; defaultValue = userId },
                    navArgument("groupId") { type = NavType.IntType; defaultValue = groupId }
                )
            ) { backStackEntry ->
                val args = backStackEntry.arguments
                val receivedUserId = args?.getInt("userId") ?: userId
                val receivedGroupId = args?.getInt("groupId") ?: groupId
                AddTodoScreen(navController, receivedUserId, receivedGroupId)
            }
            composable(
                "add_task/{userId}/{groupId}/{projectId}/{taskId}",
                arguments = listOf(
                    navArgument("userId") { type = NavType.IntType },
                    navArgument("groupId") { type = NavType.IntType },
                    navArgument("projectId") { type = NavType.IntType },
                    navArgument("taskId") { type = NavType.IntType; defaultValue = -1 }
                )
            ) { backStackEntry ->
                val args = backStackEntry.arguments

                val receivedUserId = args?.getInt("userId") ?: userId
                val receivedGroupId = args?.getInt("groupId") ?: groupId
                val receivedProjectId = args?.getInt("projectId") ?: 0
                val taskId = args?.getInt("taskId") ?: -1

                var task by remember { mutableStateOf<Task?>(null) }
                var isLoading by remember { mutableStateOf(true) }

                val coroutineScope = rememberCoroutineScope()

                LaunchedEffect(key1 = taskId) {
                    isLoading = true
                    if (taskId != -1) {
                        try {
                            val tasks = RetrofitInstance.api.getTask(receivedUserId, receivedGroupId, receivedProjectId)
                            task = tasks.find { it.id == taskId }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    isLoading = false
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    AddTaskScreen(
                        navController = navController,
                        onTaskAdded = { task ->
                            println("Task ${if (taskId == -1) "added" else "updated"}: $task")
                            navController.popBackStack()
                        },
                        userId = receivedUserId,
                        groupId = receivedGroupId,
                        projectId = receivedProjectId,
                        taskId = if (taskId != -1) taskId else null,
                        task = task
                    )
                }
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