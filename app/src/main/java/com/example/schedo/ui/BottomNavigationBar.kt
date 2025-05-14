package com.example.schedo.ui

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.schedo.ui.theme.Background
import com.example.schedo.ui.theme.Utama2
import com.example.schedo.ui.theme.Utama3
import com.example.schedo.util.PreferencesHelper
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Clock
import compose.icons.fontawesomeicons.solid.Male

enum class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    TODO("Home", "Home", Icons.Outlined.Home),
    JADWAL("jadwal?groupId={groupId}&projectId={projectId}", "Jadwal", Icons.Outlined.DateRange),
    POMODORO("Pomodoro", "Pomodoro", FontAwesomeIcons.Solid.Clock),
    PROFILE("Profile", "Profile", FontAwesomeIcons.Solid.Male)
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val context = LocalContext.current
    val preferencesHelper = PreferencesHelper(context)
    val userId = preferencesHelper.getUserId() // Ambil userId dari PreferencesHelper

    val items = listOf(BottomNavItem.TODO, BottomNavItem.JADWAL, BottomNavItem.POMODORO, BottomNavItem.PROFILE)
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(Background),
            color = Utama2
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ikon 1: Home
                val selected1 = currentRoute == items[0].route
                val animatedSize1 by animateFloatAsState(
                    targetValue = if (selected1) 1.2f else 1.0f,
                    animationSpec = spring()
                )
                IconButton(onClick = {
                    if (currentRoute != items[0].route) {
                        navController.navigate(items[0].route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }) {
                    Icon(
                        imageVector = items[0].icon,
                        contentDescription = items[0].title,
                        modifier = Modifier.size((24 * animatedSize1).dp),
                        tint = if (selected1) Utama3 else Color.White
                    )
                }

                // Ikon 2: Jadwal
                val selected2 = currentRoute?.startsWith("jadwal") == true
                val animatedSize2 by animateFloatAsState(
                    targetValue = if (selected2) 1.2f else 1.0f,
                    animationSpec = spring()
                )
                IconButton(onClick = {
                    if (currentRoute != items[1].route) {
                        navController.navigate("jadwal?groupId=-1&projectId=-1") {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }) {
                    Icon(
                        imageVector = items[1].icon,
                        contentDescription = items[1].title,
                        modifier = Modifier.size((24 * animatedSize2).dp),
                        tint = if (selected2) Utama3 else Color.White
                    )
                }

                // Spacer untuk memberikan jarak di sekitar FAB
                Spacer(modifier = Modifier.width(48.dp))

                // Ikon 3: Pomodoro
                val selected3 = currentRoute == items[2].route
                val animatedSize3 by animateFloatAsState(
                    targetValue = if (selected3) 1.2f else 1.0f,
                    animationSpec = spring()
                )
                IconButton(onClick = {
                    if (currentRoute != items[2].route) {
                        navController.navigate(items[2].route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }) {
                    Icon(
                        imageVector = items[2].icon,
                        contentDescription = items[2].title,
                        modifier = Modifier.size((24 * animatedSize3).dp),
                        tint = if (selected3) Utama3 else Color.White
                    )
                }

                // Ikon 4: Profile
                val selected4 = currentRoute == items[3].route
                val animatedSize4 by animateFloatAsState(
                    targetValue = if (selected4) 1.2f else 1.0f,
                    animationSpec = spring()
                )
                IconButton(onClick = {
                    if (currentRoute != items[3].route) {
                        navController.navigate(items[3].route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }) {
                    Icon(
                        imageVector = items[3].icon,
                        contentDescription = items[3].title,
                        modifier = Modifier.size((24 * animatedSize4).dp),
                        tint = if (selected4) Utama3 else Color.White
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                if (userId == -1) {
                    Toast.makeText(context, "Silakan login terlebih dahulu", Toast.LENGTH_LONG).show()
                    navController.navigate("login")
                } else {
                    try {
                        navController.navigate("new_project/$userId/-1/-1")
                    } catch (e: IllegalArgumentException) {
                        println("Navigation error: ${e.message}")
                        Toast.makeText(context, "Gagal navigasi ke tambah proyek", Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier
                .size(64.dp)
                .offset(y = (-32).dp)
                .align(Alignment.Center),
            shape = CircleShape,
            containerColor = Utama3
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add", tint = Color.White)
        }
    }
}