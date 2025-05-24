package com.example.schedo.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

// Enum class untuk kategori Pomodoro
enum class PomodoroCategory(val title: String, val icon: ImageVector, val color: Color) {
    STUDY("Study", Icons.Outlined.Book, Color(0xFF2196F3)),
    WORK("Work", Icons.Outlined.Work, Color(0xFFF44336)),
    REST("Rest", Icons.Outlined.Favorite, Color(0xFF4CAF50)),
    SPORT("Sport", Icons.Outlined.SportsBasketball, Color(0xFFFF9800)),
    ENTERTAINMENT("Entertainment", Icons.Outlined.Movie, Color(0xFF9C27B0)),
    OTHER("Other", Icons.Outlined.Favorite, Color(0xFF607D8B))
}

// List motivational quotes for display
val motivationalQuotes = listOf(
    "Focus on being productive instead of busy.",
    "The key is not to prioritize what's on your schedule, but to schedule your priorities.",
    "Don't count the time, make the time count.",
    "Take a deep breath, stay focused, you're doing great!",
    "Stay focused, go after your dreams and keep moving toward your goals."
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun PomodoroScreen(navController: NavHostController) {
    var totalSeconds by remember { mutableLongStateOf(20 * 60L) } // Default 20 menit
    var isRunning by remember { mutableStateOf(false) }
    var angle by remember { mutableFloatStateOf(totalSeconds * 360f / (60 * 60)) } // Konversi waktu ke sudut
    var selectedCategory by remember { mutableStateOf(PomodoroCategory.STUDY) }
    var currentQuoteIndex by remember { mutableIntStateOf(0) }
    var showMinuteSelector by remember { mutableStateOf(false) } // State untuk menampilkan dropdown menit
    var previousAngle by remember { mutableFloatStateOf(angle) } // Track previous angle for drag direction

    // Warna
    val backgroundColor = Color(0xFFFFF3E0)
    val timerColor = Color(0xFFFFB74D)
    val handleColor = Color.White
    val progressColor = Color.White
    val textColor = Color.Black

    // Logika timer dan quotes
    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (totalSeconds > 0) {
                delay(1000L)
                totalSeconds--
                angle = totalSeconds * 360f / (60 * 60)

                if (totalSeconds % 30L == 0L) {
                    currentQuoteIndex = (currentQuoteIndex + 1) % motivationalQuotes.size
                }
            }
            isRunning = false
        }
    }

    // Format waktu
    val timeText = if (totalSeconds == 3600L) {
        "60:00"
    } else {
        val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds)
        val seconds = totalSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pomodoro") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isRunning) {
                Text(
                    text = motivationalQuotes[currentQuoteIndex],
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    ),
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(56.dp))
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .wrapContentWidth(Alignment.CenterHorizontally)
            ) {
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    timerColor,
                                    timerColor.copy(alpha = 0.95f)
                                )
                            )
                        )
                        .drawWithContent {
                            drawContent()
                            val sweepAngle = (totalSeconds.toFloat() / (60 * 60)) * 360f
                            drawArc(
                                color = progressColor,
                                startAngle = -90f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 40f, cap = StrokeCap.Round)
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                if (!isRunning) {
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val touchPoint = change.position
                                    var touchAngle = (Math.toDegrees(
                                        atan2(
                                            (touchPoint.y - center.y).toDouble(),
                                            (touchPoint.x - center.x).toDouble()
                                        )
                                    ).toFloat() + 90) % 360
                                    if (touchAngle < 0) touchAngle += 360f

                                    // Determine drag direction (clockwise or counterclockwise)
                                    val angleDiff = (touchAngle - previousAngle + 360) % 360
                                    val isDraggingBackward = angleDiff > 180

                                    // Allow dragging only if:
                                    // 1. Not at max (angle < 359.9) and not at min (angle > 0.1), or
                                    // 2. At max but dragging backward, or
                                    // 3. At min but dragging forward
                                    if ((angle < 359.9f && angle > 0.1f) ||
                                        (angle >= 359.9f && isDraggingBackward) ||
                                        (angle <= 0.1f && !isDraggingBackward)
                                    ) {
                                        // Prevent angle from jumping past 360 or below 0
                                        if (!isDraggingBackward && touchAngle < previousAngle - 180) {
                                            // Ignore if trying to drag forward past 360
                                            return@detectDragGestures
                                        }
                                        if (isDraggingBackward && touchAngle > previousAngle + 180) {
                                            // Ignore if trying to drag backward past 0
                                            return@detectDragGestures
                                        }
                                        angle = touchAngle
                                        totalSeconds = (angle / 360f * 60 * 60).toLong()
                                        totalSeconds = min(totalSeconds, 60 * 60)
                                        totalSeconds = maxOf(totalSeconds, 0)
                                        previousAngle = angle
                                    }
                                }
                            }
                        }
                        .wrapContentHeight(Alignment.CenterVertically)
                        .wrapContentWidth(Alignment.CenterHorizontally)
                ) {
                    Box(
                        modifier = Modifier
                            .clickable(enabled = !isRunning) { showMinuteSelector = true } // Klik untuk membuka dropdown
                    ) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Dropdown untuk memilih menit
                    DropdownMenu(
                        expanded = showMinuteSelector,
                        onDismissRequest = { showMinuteSelector = false },
                        modifier = Modifier
                            .width(120.dp)
                            .background(Color.White)
                    ) {
                        // Opsi menit: 20, 30, 60
                        listOf(20, 30, 60).forEach { minute ->
                            DropdownMenuItem(
                                text = {
                                    Text(text = "$minute min")
                                },
                                onClick = {
                                    totalSeconds = minute * 60L
                                    angle = totalSeconds * 360f / (60 * 60)
                                    showMinuteSelector = false
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                var expanded by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .clickable { expanded = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(selectedCategory.color, CircleShape)
                    )

                    Text(
                        text = selectedCategory.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = selectedCategory.color,
                        fontWeight = FontWeight.Medium
                    )

                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Select Category",
                        tint = selectedCategory.color,
                        modifier = Modifier.size(24.dp)
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .width(180.dp)
                        .background(Color.White)
                ) {
                    PomodoroCategory.values().forEach { category ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(category.color, CircleShape)
                                    )

                                    Text(text = category.title)
                                }
                            },
                            onClick = {
                                selectedCategory = category
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            IconButton(
                onClick = { isRunning = !isRunning },
                modifier = Modifier
                    .padding(bottom = 48.dp)
                    .size(56.dp)
                    .shadow(3.dp, CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                timerColor,
                                timerColor.copy(alpha = 0.9f)
                            )
                        ),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}