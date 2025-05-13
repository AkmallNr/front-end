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
import com.example.schedo.model.*
import com.example.schedo.network.RetrofitInstance
import com.example.schedo.util.PreferencesHelper
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(
    navController: NavHostController,
    groupId: Int = -1,
    projectId: Int = -1,
    startDestination: String = "login" // Parameter startDestination
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomNav = currentRoute !in listOf(
        "login",
        "register",
        "OnLoad",
        "add_task/{userId}/{groupId}/{projectId}/{taskId}",
        "add_todo/{userId}/{groupId}/{projectId}",
        "new_project/{userId}/{groupId}/{projectId}"
    )

    Scaffold(
        bottomBar = {
            if (showBottomNav) BottomNavigationBar(navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("login") {
                LoginScreen(navController)
            }
            composable("register") {
                RegisterScreen(navController)
            }
            composable(BottomNavItem.TODO.route) {
                HomeScreen(navController)
            }
            composable(
                "add_todo/{userId}/{groupId}/{projectId}",
                arguments = listOf(
                    navArgument("userId") { type = NavType.IntType },
                    navArgument("groupId") { type = NavType.IntType },
                    navArgument("projectId") { type = NavType.IntType; defaultValue = -1 }
                )
            ) { backStackEntry ->
                val args = backStackEntry.arguments
                val receivedUserId = args?.getInt("userId") ?: -1
                val receivedGroupId = args?.getInt("groupId") ?: -1
                val projectId = args?.getInt("projectId") ?: -1

                var group by remember { mutableStateOf<List<Group>?>(null) }
                var project by remember { mutableStateOf<Project?>(null) }
                var isLoading by remember { mutableStateOf(true) }

                val coroutineScope = rememberCoroutineScope()

                LaunchedEffect(key1 = projectId) {
                    isLoading = true
                    if (projectId != -1) {
                        try {
                            val projects = RetrofitInstance.api.getProjectsByGroup(receivedUserId, receivedGroupId).data
                            project = projects.find { it.id == projectId }
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
                    AddTodoScreen(
                        navController = navController,
                        projectId = if (projectId != -1) projectId else null,
                        project = project,
                        groupId = receivedGroupId,
                        userId = receivedUserId
                    )
                }
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
                val receivedUserId = args?.getInt("userId") ?: -1
                val receivedGroupId = args?.getInt("groupId") ?: -1
                val receivedProjectId = args?.getInt("projectId") ?: 0
                val taskId = args?.getInt("taskId") ?: -1
                var quote by remember { mutableStateOf<Quote?>(null) }
                var task by remember { mutableStateOf<Task?>(null) }
                var isLoading by remember { mutableStateOf(true) }

                val coroutineScope = rememberCoroutineScope()

                LaunchedEffect(key1 = taskId) {
                    isLoading = true
                    if (taskId != -1) {
                        try {
                            val tasks = RetrofitInstance.api.getTask(receivedUserId, receivedGroupId, receivedProjectId).data
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
                        task = task,
                        quote = quote
                    )
                }
            }
            composable(
                BottomNavItem.JADWAL.route,
                arguments = listOf(
                    navArgument("groupId") { type = NavType.IntType; defaultValue = -1 },
                    navArgument("projectId") { type = NavType.IntType; defaultValue = -1 }
                )
            ) { backStackEntry ->
                val args = backStackEntry.arguments
                val receivedGroupId = args?.getInt("groupId") ?: -1
                val receivedProjectId = args?.getInt("projectId") ?: -1

                ScheduleScreen(
                    navController = navController,
                    groupId = receivedGroupId,
                    projectId = receivedProjectId
                )
            }
            composable(
                "new_project/{userId}/{groupId}/{projectId}",
                arguments = listOf(
                    navArgument("userId") { type = NavType.IntType },
                    navArgument("groupId") { type = NavType.IntType; defaultValue = -1 },
                    navArgument("projectId") { type = NavType.IntType; defaultValue = -1 }
                )
            ) { backStackEntry ->
                val args = backStackEntry.arguments
                val receivedUserId = args?.getInt("userId") ?: -1
                val receivedGroupId = args?.getInt("groupId") ?: -1
                val projectId = args?.getInt("projectId") ?: -1

                var project by remember { mutableStateOf<Project?>(null) }

                AddTodoScreen(
                    navController = navController,
                    projectId = if (projectId != -1) projectId else null,
                    project = project,
                    groupId = if (receivedGroupId != -1) receivedGroupId else null,
                    userId = receivedUserId
                )
            }
            composable(BottomNavItem.POMODORO.route) {
                PomodoroScreen(navController)
            }
            composable(BottomNavItem.PROFILE.route) {
                UserManagementScreen(navController = navController)
            }
        }
    }
}