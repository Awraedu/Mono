package com.awras.mono.features.tools.measurement

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awras.mono.ui.components.DatumToolTopBar
import com.awras.mono.ui.theme.LocalTactileFeedback
import com.kyant.capsule.ContinuousCapsule
import compose.icons.TablerIcons
import compose.icons.tablericons.Activity
import java.util.Locale
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeismographScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    // Live Vibration State
    var currentMagnitude by remember { mutableFloatStateOf(0f) }
    var peakMagnitude by remember { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }

    // Waveform History Buffer (rolling list of 150 high-res amplitude values)
    val waveformBuffer = remember { mutableStateListOf<Float>().apply { repeat(150) { add(0f) } } }

    // Inspection State when Paused
    var inspectedIndex by remember { mutableStateOf<Int?>(null) }
    var touchX by remember { mutableFloatStateOf(0f) }

    // Register Linear Acceleration Sensor
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (isPaused) return

                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]

                // Magnitude in g units
                val magnitude = sqrt((ax * ax + ay * ay + az * az).toDouble()).toFloat() / 9.81f

                currentMagnitude = magnitude
                if (magnitude > peakMagnitude) {
                    peakMagnitude = magnitude
                    if (magnitude > 0.15f) {
                        haptics.tick()
                    }
                }

                // Append to waveform buffer
                if (waveformBuffer.size >= 150) {
                    waveformBuffer.removeAt(0)
                }
                waveformBuffer.add(magnitude)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Intensity Rating Text
    val intensityRating = when {
        currentMagnitude < 0.02f -> "STATIONARY / MICRO"
        currentMagnitude < 0.08f -> "LIGHT VIBRATION"
        currentMagnitude < 0.20f -> "MODERATE SHAKE"
        currentMagnitude < 0.45f -> "STRONG SHOCK"
        else -> "SEVERE TREMOR"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Seismograph",
                description = if (isPaused && inspectedIndex != null) "Inspecting Paused Waveform" else "Real-time Vibration & Shock Meter",
                icon = TablerIcons.Activity,
                onBackClick = onBackClick,
                rightActionIcon = Icons.Default.Refresh,
                onRightActionClick = {
                    haptics.click()
                    peakMagnitude = 0f
                    inspectedIndex = null
                    waveformBuffer.clear()
                    repeat(150) { waveformBuffer.add(0f) }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(isPaused) {
                    if (isPaused) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                touchX = offset.x
                                val stepX = size.width / (waveformBuffer.size - 1).toFloat()
                                val index = (offset.x / stepX).roundToInt().coerceIn(0, waveformBuffer.size - 1)
                                inspectedIndex = index
                                haptics.tick()
                            },
                            onDrag = { change, _ ->
                                touchX = change.position.x
                                val stepX = size.width / (waveformBuffer.size - 1).toFloat()
                                val index = (change.position.x / stepX).roundToInt().coerceIn(0, waveformBuffer.size - 1)
                                inspectedIndex = index
                            }
                        )
                    }
                }
                .pointerInput(isPaused) {
                    if (isPaused) {
                        detectTapGestures(
                            onTap = { offset ->
                                touchX = offset.x
                                val stepX = size.width / (waveformBuffer.size - 1).toFloat()
                                val index = (offset.x / stepX).roundToInt().coerceIn(0, waveformBuffer.size - 1)
                                inspectedIndex = index
                                haptics.tick()
                            }
                        )
                    }
                }
        ) {
            // 1. FULL-SCREEN SMOOTH OSCILLOSCOPE WAVEFORM BACKGROUND CANVAS
            val guideColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val midY = height / 2f

                // Draw Subtle Grid Lines across full screen
                for (i in 1..8) {
                    val y = (height / 9f) * i
                    drawLine(
                        color = guideColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                }

                // Center Baseline
                drawLine(
                    color = guideColor.copy(alpha = 0.2f),
                    start = Offset(0f, midY),
                    end = Offset(width, midY),
                    strokeWidth = 1.5f
                )

                if (waveformBuffer.size > 1) {
                    val stepX = width / (waveformBuffer.size - 1).toFloat()
                    val path = Path()
                    val maxAmp = max(0.4f, peakMagnitude)

                    val points = mutableListOf<Offset>()
                    for (i in waveformBuffer.indices) {
                        val amp = waveformBuffer[i].coerceIn(0f, 2f)
                        val x = i * stepX
                        val y = midY - (amp / maxAmp) * (height * 0.35f)
                        points.add(Offset(x, y))
                    }

                    // Build Ultra-Smooth Cubic Bezier Path
                    if (points.isNotEmpty()) {
                        path.moveTo(points[0].x, points[0].y)
                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val controlPoint1 = Offset((p0.x + p1.x) / 2f, p0.y)
                            val controlPoint2 = Offset((p0.x + p1.x) / 2f, p1.y)
                            path.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                        }

                        // Draw Smooth Waveform Stroke
                        drawPath(
                            path = path,
                            color = vibrantOrange,
                            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                        )
                    }

                    // Draw Inspection Crosshair and Tooltip Point if Paused and Inspected
                    if (isPaused && inspectedIndex != null) {
                        val idx = inspectedIndex!!.coerceIn(0, points.size - 1)
                        val pt = points[idx]

                        // Vertical Dashed Crosshair Line
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.8f),
                            start = Offset(pt.x, 0f),
                            end = Offset(pt.x, height),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        // Selection Glow Circle on Waveform
                        drawCircle(
                            color =  vibrantOrange,
                            radius = 10f,
                            center = pt
                        )
                        drawCircle(
                            color = Color.Gray,
                            radius = 7f,
                            center = pt
                        )
                    }
                }
            }

            // 2. FLOATING OVERLAYS (PROMINENT ACCELERATION & BOTTOM CONTROLS)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Prominent Display (Shows inspected value when paused & inspected)
                val displayMag = if (isPaused && inspectedIndex != null) {
                    waveformBuffer[inspectedIndex!!.coerceIn(0, waveformBuffer.size - 1)]
                } else {
                    currentMagnitude
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.3f", displayMag),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 58.sp),
                            fontWeight = FontWeight.Black,
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            letterSpacing = (-1.5).sp,
                            color = if (isPaused && inspectedIndex != null) vibrantOrange else MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "g",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            color = vibrantOrange,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPaused && inspectedIndex != null) {
                                "INSPECTED SAMPLE #${inspectedIndex}"
                            } else {
                                String.format(Locale.US, "PEAK: %.3f g", peakMagnitude)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                        )
                        Text(
                            text = if (isPaused && inspectedIndex != null) "TOUCH TO SLIDE" else intensityRating,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = vibrantOrange,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom Pause / Resume Button
                Button(
                    onClick = {
                        haptics.click()
                        isPaused = !isPaused
                        if (!isPaused) {
                            inspectedIndex = null
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = ContinuousCapsule,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPaused) MaterialTheme.colorScheme.surface else vibrantOrange,
                        contentColor = if (isPaused) MaterialTheme.colorScheme.onSurface else Color.White
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null
                        )
                        Text(
                            text = if (isPaused) "RESUME MONITORING" else "PAUSE MONITORING",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}
