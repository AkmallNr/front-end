package com.example.schedo.ui

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.schedo.model.*
import com.example.schedo.network.RetrofitInstance
import com.example.schedo.ui.theme.Background
import com.example.schedo.ui.theme.Utama2
import com.example.schedo.util.PreferencesHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AppNavHost(
    navController: NavHostController,
    groupId: Int = -1,
    projectId: Int = -1,
    startDestination: String = "onboarding"
) {
    val context = LocalContext.current
    val preferencesHelper = remember { PreferencesHelper(context) }
    val onboardingCompleted = preferencesHelper.isOnboardingCompleted()
    val userId = preferencesHelper.getUserId()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val actualStartDestination = remember {
        when {
            !onboardingCompleted -> "onboarding"
            userId == -1 -> "login"
            else -> BottomNavItem.TODO.route
        }
    }

    // List of routes where bottom navigation should be hidden
    val noBottomNavRoutes = listOf(
        "onboarding",
        "login",
        "register",
        "OnLoad",
        "add_task/{userId}/{groupId}/{projectId}/{taskId}",
        "add_todo/{userId}/{groupId}/{projectId}",
        "new_project/{userId}/{groupId}/{projectId}"
    )

    val showBottomNav = currentRoute !in noBottomNavRoutes
    var showAddTodo by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            if (showBottomNav) {
                BottomNavigationBar(
                    navController = navController,
                    onShowAddTodo = {
                        val userId = preferencesHelper.getUserId()
                        if (userId == -1) {
                            Toast.makeText(context, "Silakan login terlebih dahulu", Toast.LENGTH_LONG).show()
                            navController.navigate("login")
                        } else {
                            showAddTodo = true
                        }
                    }
                )
            }
        }
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()

        ) {
            NavHost(
                navController = navController,
                startDestination = actualStartDestination,
                modifier = Modifier.fillMaxSize()
            ) {
                composable("onboarding") {
                    OnBoardingScreen(
                        onFinished = {
                            preferencesHelper.setOnboardingCompleted(true)
                            navController.navigate("login") {
                                popUpTo("onboarding") {inclusive = true}
                            }
                        }
                    )
                }

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
                    val receivedProjectId = args?.getInt("projectId") ?: -1

                    var project by remember { mutableStateOf<Project?>(null) }
                    var isLoading by remember { mutableStateOf(true) }
                    val scope = rememberCoroutineScope()

                    LaunchedEffect(key1 = receivedProjectId) {
                        isLoading = true
                        if (receivedProjectId != -1) {
                            scope.launch {
                                try {
                                    val projects = RetrofitInstance.api
                                        .getProjectsByGroup(receivedUserId, receivedGroupId)
                                        .data
                                    project = projects.find { it.id == receivedProjectId }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isLoading = false
                                }
                            }
                        } else {
                            isLoading = false
                        }
                    }

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Utama2)
                        }
                    } else {
                        AddTodoScreen(
                            navController = navController,
                            projectId = if (receivedProjectId != -1) receivedProjectId else null,
                            project = project,
                            groupId = receivedGroupId,
                            userId = receivedUserId,
                            onDismiss = { navController.popBackStack() }
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

                    var task by remember { mutableStateOf<Task?>(null) }
                    var quote by remember { mutableStateOf<Quote?>(null) }
                    var isLoading by remember { mutableStateOf(true) }
                    val scope = rememberCoroutineScope()

                    LaunchedEffect(key1 = taskId) {
                        isLoading = true
                        if (taskId != -1) {
                            scope.launch {
                                try {
                                    val tasks = RetrofitInstance.api
                                        .getTask(receivedUserId, receivedGroupId, receivedProjectId)
                                        .data
                                    task = tasks.find { it.id == taskId }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isLoading = false
                                }
                            }
                        } else {
                            isLoading = false
                        }
                    }

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Utama2)
                        }
                    } else {
                        AddTaskScreen(
                            navController = navController,
                            onTaskAdded = { navController.popBackStack() },
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
                    val receivedProjectId = args?.getInt("projectId") ?: -1

                    AddTodoScreen(
                        navController = navController,
                        projectId = if (receivedProjectId != -1) receivedProjectId else null,
                        project = null,
                        groupId = if (receivedGroupId != -1) receivedGroupId else null,
                        userId = receivedUserId,
                        onDismiss = { navController.popBackStack() }
                    )
                }

                composable(
                    "project_detail/{userId}/{groupId}/{projectId}",
                    arguments = listOf(
                        navArgument("userId") { type = NavType.IntType },
                        navArgument("groupId") { type = NavType.IntType },
                        navArgument("projectId") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val args = backStackEntry.arguments
                    val userId = args?.getInt("userId") ?: -1
                    val groupId = args?.getInt("groupId") ?: 0
                    val projectId = args?.getInt("projectId") ?: 0

                    var project by remember { mutableStateOf<Project?>(null) }
                    var isLoading by remember { mutableStateOf(true) }
                    val scope = rememberCoroutineScope()

                    LaunchedEffect(key1 = projectId) {
                        isLoading = true
                        scope.launch {
                            try {
                                val projects = RetrofitInstance.api
                                    .getProjectsByGroup(userId, groupId)
                                    .data
                                project = projects.find { it.id == projectId }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Failed to load project: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    }

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Utama2)
                        }
                    } else if (project != null) {
                        ProjectDetailScreen(
                            navController = navController,
                            selectedProject = project!!,
                            userId = userId,
                            groupId = groupId
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Project not found", color = Color.Gray)
                        }
                    }
                }

                composable(BottomNavItem.POMODORO.route) {
                    PomodoroScreen(navController)
                }

                composable(BottomNavItem.PROFILE.route) {
                    UserManagementScreen(navController = navController)
                }
            }

            // Add Todo Modal Bottom Sheet
            if (showAddTodo) {
                val userId = preferencesHelper.getUserId()
                if (userId != -1) {
                    AddTodoModalBottomSheet(
                        userId = userId,
                        groupId = -1,
                        projectId = -1,
                        navController = navController,
                        onDismiss = { showAddTodo = false }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoModalBottomSheet(
    userId: Int,
    groupId: Int,
    projectId: Int,
    navController: NavHostController,
    onDismiss: () -> Unit
) {
    var project by remember { mutableStateOf<Project?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = projectId) {
        isLoading = true
        if (projectId != -1) {
            scope.launch {
                try {
                    val projects = RetrofitInstance.api
                        .getProjectsByGroup(userId, groupId)
                        .data
                    project = projects.find { it.id == projectId }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isLoading = false
                }
            }
        } else {
            isLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Utama2)
            }
        } else {
            AddTodoScreen(
                navController = navController,
                projectId = if (projectId != -1) projectId else null,
                project = project,
                groupId = groupId,
                userId = userId,
                onDismiss = onDismiss
            )
        }
    }
}