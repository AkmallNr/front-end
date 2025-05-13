package com.example.schedo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.schedo.ui.theme.Background
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(navController: NavHostController) {
    var totalSeconds by remember { mutableLongStateOf(20 * 60L) } // Default 20 menit
    var isRunning by remember { mutableStateOf(false) }
    var angle by remember { mutableFloatStateOf(totalSeconds * 360f / (60 * 60)) } // Konversi waktu ke sudut

    // Warna
    val backgroundColor = Background
    val timerColor = Color(0xFFFFCC80)
    val handleColor = Color.White
    val progressColor = Color.White
    val textColor = Color.Black

    // Logika timer
    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (totalSeconds > 0) {
                delay(1000L)
                totalSeconds--
                angle = totalSeconds * 360f / (60 * 60) // Update sudut berdasarkan waktu
            }
            isRunning = false
        }
    }

    // Format waktu
    val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds)
    val seconds = totalSeconds % 60
    val timeText = String.format("%02d:%02d", minutes, seconds)

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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            // Lingkaran Timer dengan shadow
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

                        // Progress arc
                        val sweepAngle = (totalSeconds.toFloat() / (60 * 60)) * 360f
                        drawArc(
                            color = progressColor,
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = 10f, cap = StrokeCap.Round)
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            if (!isRunning) { // Hanya bisa drag ketika timer tidak berjalan
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val touchPoint = change.position

                                // Hitung sudut dari titik tengah ke posisi sentuhan
                                val touchAngle = (Math.toDegrees(
                                    atan2(
                                        (touchPoint.y - center.y).toDouble(),
                                        (touchPoint.x - center.x).toDouble()
                                    )
                                ).toFloat() + 90) % 360

                                angle = if (touchAngle < 0) touchAngle + 360f else touchAngle
                                totalSeconds = (angle / 360f * 60 * 60).toLong()
                                totalSeconds = min(totalSeconds, 60 * 60) // Maks 60 menit
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Teks timer
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                // Pegangan putih untuk drag
                val handleSize = 16.dp
                val radius = 140f // Ubah ke Float untuk perhitungan
                val handleAngle = Math.toRadians(angle.toDouble() - 90) // Konversi ke radian
                val handleCos = cos(handleAngle).toFloat() // Konversi ke Float
                val handleSin = sin(handleAngle).toFloat() // Konversi ke Float

                Box(
                    modifier = Modifier
                        .size(handleSize)
                        .offset(
                            x = (radius * handleCos).dp,
                            y = (radius * handleSin).dp
                        )
                        .shadow(2.dp, CircleShape)
                        .background(handleColor, CircleShape)
                )
            }

            // Tombol Play/Pause
            IconButton(
                onClick = { isRunning = !isRunning },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
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