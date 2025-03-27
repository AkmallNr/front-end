package com.example.schedo.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Clock
import compose.icons.fontawesomeicons.solid.Male

enum class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    TODO("Home", "Home", Icons.Outlined.Home),
    JADWAL("Jadwal", "Jadwal", Icons.Outlined.DateRange),
    POMODORO("Pomodoro", "Pomodoro", FontAwesomeIcons.Solid.Clock),
    PROFILE("Profile", "Profile", FontAwesomeIcons.Solid.Male)
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(BottomNavItem.TODO, BottomNavItem.JADWAL, BottomNavItem.POMODORO, BottomNavItem.PROFILE)
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color(0xFFEDE7F6)),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = currentRoute == item.route
                    val animatedSize by animateFloatAsState(
                        targetValue = if (selected) 1.2f else 1.0f,
                        animationSpec = spring()
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                modifier = Modifier.size((24 * animatedSize).dp),
                                tint = if (selected) Color(0xFF673AB7) else Color.Gray
                            )
                        }
                        AnimatedVisibility(visible = selected) {
                            Text(text = item.title, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                navController.navigate("add_todo")
            },
            modifier = Modifier
                .size(64.dp)
                .offset(y = (-36).dp)
                .align(Alignment.Center),
            shape = CircleShape,
            containerColor = Color(0xFF673AB7)
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add", tint = Color.White)
        }
    }
}