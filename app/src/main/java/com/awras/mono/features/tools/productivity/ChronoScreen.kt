package com.awras.mono.features.tools.productivity

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awras.mono.ui.components.DatumToolTopBar
import com.awras.mono.ui.theme.Geist
import com.awras.mono.ui.theme.GeistMono
import com.awras.mono.ui.theme.LocalTactileFeedback
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import compose.icons.TablerIcons
import compose.icons.tablericons.*
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

enum class ChronoMode(val displayName: String) {
    STOPWATCH("Chronograph"),
    COUNTDOWN("Countdown")
}

data class LapItem(
    val lapNumber: Int,
    val lapTimeMs: Long,
    val splitTimeMs: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChronoScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    var mode by remember { mutableStateOf(ChronoMode.STOPWATCH) }

    // ==================== STOPWATCH STATE ====================
    var isStopwatchRunning by remember { mutableStateOf(false) }
    var stopwatchElapsedMs by remember { mutableLongStateOf(0L) }
    var lapList by remember { mutableStateOf(listOf<LapItem>()) }
    var lastLapSplitMs by remember { mutableLongStateOf(0L) }

    // High speed 100fps timer loop
    LaunchedEffect(isStopwatchRunning) {
        if (isStopwatchRunning) {
            val baseTime = System.currentTimeMillis() - stopwatchElapsedMs
            while (isStopwatchRunning) {
                stopwatchElapsedMs = System.currentTimeMillis() - baseTime
                delay(10)
            }
        }
    }

    // ==================== COUNTDOWN TIMER STATE ====================
    var isTimerRunning by remember { mutableStateOf(false) }
    var timerTotalMs by remember { mutableLongStateOf(5 * 60 * 1000L) } // Default 5 mins
    var timerRemainingMs by remember { mutableLongStateOf(5 * 60 * 1000L) }
    var timerEndTimeMs by remember { mutableLongStateOf(0L) }
    var isTimerFinished by remember { mutableStateOf(false) }

    // Countdown Timer Loop
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            isTimerFinished = false
            while (isTimerRunning && timerRemainingMs > 0) {
                delay(20)
                timerRemainingMs = (timerEndTimeMs - System.currentTimeMillis()).coerceAtLeast(0L)
                if (timerRemainingMs == 0L) {
                    isTimerRunning = false
                    isTimerFinished = true
                    triggerTimerAlarm(context)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Chrono",
                description = "Luxury Adaptive Horology Chronograph",
                icon = Icons.Outlined.Timer,
                onBackClick = onBackClick,
                rightActionIcon = TablerIcons.RotateClockwise,
                onRightActionClick = {
                    haptics.click()
                    if (mode == ChronoMode.STOPWATCH) {
                        isStopwatchRunning = false
                        stopwatchElapsedMs = 0L
                        lapList = emptyList()
                        lastLapSplitMs = 0L
                    } else {
                        isTimerRunning = false
                        timerRemainingMs = timerTotalMs
                        isTimerFinished = false
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ==================== 1. MODE SELECTOR CROWN ====================
            Surface(
                shape = ContinuousCapsule,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.height(52.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChronoMode.entries.forEach { itemMode ->
                        val isSelected = mode == itemMode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(ContinuousCapsule)
                                .background(if (isSelected) vibrantOrange else Color.Transparent)
                                .clickable {
                                    if (mode != itemMode) {
                                        haptics.click()
                                        mode = itemMode
                                    }
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (itemMode == ChronoMode.STOPWATCH) Icons.Outlined.Timer else Icons.Outlined.HourglassTop,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = itemMode.displayName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // ==================== 2. CLEAN ON-SCREEN ANALOG CHRONOGRAPH ====================
            val activeElapsedMs = if (mode == ChronoMode.STOPWATCH) stopwatchElapsedMs else (timerTotalMs - timerRemainingMs)
            val isRunning = if (mode == ChronoMode.STOPWATCH) isStopwatchRunning else isTimerRunning
            val isMinutesScale = if (mode == ChronoMode.STOPWATCH) (activeElapsedMs >= 60_000L) else (timerTotalMs >= 60_000L)

            // Scale Badge
            Surface(
                shape = ContinuousCapsule,
                color = vibrantOrange.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, vibrantOrange.copy(alpha = 0.3f))
            ) {
                Text(
                    text = if (isMinutesScale) "MAIN DIAL: MINUTES (CASCADING SECONDS & 1/100s)" else "MAIN DIAL: SECONDS (CASCADING MINS & 1/100s)",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = vibrantOrange,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            // Precision Analog Horology Canvas (Clean Screen Layout, No Card)
            AnalogChronographWatch(
                elapsedMs = activeElapsedMs,
                isMinutesScale = isMinutesScale,
                isRunning = isRunning,
                isFinished = isTimerFinished && mode == ChronoMode.COUNTDOWN,
                vibrantOrange = vibrantOrange,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .aspectRatio(1f)
            )

            // ==================== 3. CHRONOGRAPH MECHANICAL PUSHERS ====================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Flyback Reset Pusher
                IconButton(
                    onClick = {
                        haptics.click()
                        if (mode == ChronoMode.STOPWATCH) {
                            isStopwatchRunning = false
                            stopwatchElapsedMs = 0L
                            lapList = emptyList()
                            lastLapSplitMs = 0L
                        } else {
                            isTimerRunning = false
                            timerRemainingMs = timerTotalMs
                            isTimerFinished = false
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                ) {
                    Icon(
                        imageVector = TablerIcons.RotateClockwise,
                        contentDescription = "Reset Pusher",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Primary Chronograph Pusher: Start / Stop
                Surface(
                    onClick = {
                        haptics.click()
                        if (mode == ChronoMode.STOPWATCH) {
                            isStopwatchRunning = !isStopwatchRunning
                        } else {
                            if (isTimerFinished) {
                                isTimerFinished = false
                                timerRemainingMs = timerTotalMs
                            }
                            if (!isTimerRunning && timerRemainingMs > 0) {
                                timerEndTimeMs = System.currentTimeMillis() + timerRemainingMs
                                isTimerRunning = true
                            } else {
                                isTimerRunning = false
                            }
                        }
                    },
                    shape = CircleShape,
                    color = vibrantOrange,
                    shadowElevation = 10.dp,
                    modifier = Modifier.size(76.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (mode == ChronoMode.STOPWATCH) {
                                if (isStopwatchRunning) TablerIcons.PlayerPause else TablerIcons.PlayerPlay
                            } else {
                                if (isTimerRunning) TablerIcons.PlayerPause else TablerIcons.PlayerPlay
                            },
                            contentDescription = "Start/Pause Pusher",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }
                }

                // Right Pusher: Lap Split (Stopwatch) or +1m Quick Add (Countdown)
                IconButton(
                    onClick = {
                        haptics.click()
                        if (mode == ChronoMode.STOPWATCH) {
                            if (isStopwatchRunning) {
                                val lapTime = stopwatchElapsedMs - lastLapSplitMs
                                val newLap = LapItem(
                                    lapNumber = lapList.size + 1,
                                    lapTimeMs = lapTime,
                                    splitTimeMs = stopwatchElapsedMs
                                )
                                lastLapSplitMs = stopwatchElapsedMs
                                lapList = listOf(newLap) + lapList
                            }
                        } else {
                            timerTotalMs += 60 * 1000L
                            timerRemainingMs += 60 * 1000L
                            if (isTimerRunning) {
                                timerEndTimeMs += 60 * 1000L
                            }
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                ) {
                    if (mode == ChronoMode.STOPWATCH) {
                        Icon(
                            imageVector = TablerIcons.Flag,
                            contentDescription = "Lap Pusher",
                            tint = if (isStopwatchRunning) vibrantOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    } else {
                        Text(
                            text = "+1m",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = vibrantOrange
                        )
                    }
                }
            }

            // ==================== 4. MODE SPECIFIC BOTTOM SECTION ====================
            if (mode == ChronoMode.STOPWATCH) {
                // ------------ LAP SPLITS VAULT ------------
                if (lapList.isNotEmpty()) {
                    val (fastestLapNum, slowestLapNum) = remember(lapList) {
                        if (lapList.size >= 2) {
                            val fastest = lapList.minByOrNull { it.lapTimeMs }?.lapNumber
                            val slowest = lapList.maxByOrNull { it.lapTimeMs }?.lapNumber
                            Pair(fastest, slowest)
                        } else Pair(null, null)
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "HOROLOGY LAP SPLITS (${lapList.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = vibrantOrange,
                                letterSpacing = 1.sp
                            )

                            TextButton(onClick = {
                                lapList = emptyList()
                                lastLapSplitMs = 0L
                            }) {
                                Text("Clear Laps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }

                        lapList.forEach { lap ->
                            val isFastest = lap.lapNumber == fastestLapNum
                            val isSlowest = lap.lapNumber == slowestLapNum

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = ContinuousRoundedRectangle(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "LAP ${String.format(Locale.ENGLISH, "%02d", lap.lapNumber)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        if (isFastest) {
                                            Surface(
                                                shape = ContinuousCapsule,
                                                color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "BEST",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF4CAF50),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else if (isSlowest) {
                                            Surface(
                                                shape = ContinuousCapsule,
                                                color = MaterialTheme.colorScheme.errorContainer
                                            ) {
                                                Text(
                                                    text = "SLOWEST",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = formatStopwatchTime(lap.lapTimeMs),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = GeistMono,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Total: ${formatStopwatchTime(lap.splitTimeMs)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = GeistMono,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ------------ COUNTDOWN DURATION PRESETS & ADJUSTMENT ------------
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "TACTILE PRESETS & ADJUSTMENTS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = vibrantOrange,
                        letterSpacing = 1.sp
                    )

                    val presets = listOf(
                        Pair("1m Quick", 1 * 60 * 1000L),
                        Pair("3m Focus", 3 * 60 * 1000L),
                        Pair("5m Tea", 5 * 60 * 1000L),
                        Pair("10m Coffee", 10 * 60 * 1000L),
                        Pair("15m Break", 15 * 60 * 1000L),
                        Pair("25m Pomodoro", 25 * 60 * 1000L),
                        Pair("30m Sprint", 30 * 60 * 1000L),
                        Pair("1h Deep Work", 60 * 60 * 1000L)
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(presets) { (label, durationMs) ->
                            val isSelected = timerTotalMs == durationMs && !isTimerRunning
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    haptics.click()
                                    isTimerRunning = false
                                    isTimerFinished = false
                                    timerTotalMs = durationMs
                                    timerRemainingMs = durationMs
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                shape = ContinuousCapsule,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = vibrantOrange,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Manual Incremental Adjustment Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                haptics.click()
                                if (!isTimerRunning && timerTotalMs > 60 * 1000L) {
                                    timerTotalMs -= 60 * 1000L
                                    timerRemainingMs = timerTotalMs
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = ContinuousCapsule
                        ) {
                            Text("-1 Min", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                haptics.click()
                                if (!isTimerRunning) {
                                    timerTotalMs += 60 * 1000L
                                    timerRemainingMs = timerTotalMs
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = ContinuousCapsule
                        ) {
                            Text("+1 Min", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                haptics.click()
                                if (!isTimerRunning) {
                                    timerTotalMs += 5 * 60 * 1000L
                                    timerRemainingMs = timerTotalMs
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = ContinuousCapsule
                        ) {
                            Text("+5 Mins", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==================== ADAPTIVE LUXURY ANALOG CHRONOGRAPH WATCH CANVAS ====================
@Composable
private fun AnalogChronographWatch(
    elapsedMs: Long,
    isMinutesScale: Boolean,
    isRunning: Boolean,
    isFinished: Boolean,
    vibrantOrange: Color,
    modifier: Modifier = Modifier
) {
    val totalSecs = elapsedMs / 1000
    val hours = totalSecs / 3600
    val minutes = (totalSecs % 3600) / 60
    val seconds = totalSecs % 60
    val hundredths = (elapsedMs % 1000) / 10

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val watchRadius = (size.minDimension / 2f) - 10.dp.toPx()

        val dialBg = Color(0xFF14161B)
        val bezelColor = Color(0xFF2B2E38)
        val textPaintColor = android.graphics.Color.WHITE

        // 1. Outer Watch Case & Stainless Bezel
        drawCircle(
            color = bezelColor,
            radius = watchRadius + 8.dp.toPx(),
            center = center,
            style = Stroke(width = 6.dp.toPx())
        )

        // 2. Watch Face Main Plate
        drawCircle(
            color = dialBg,
            radius = watchRadius,
            center = center
        )

        // Outer Accent Ring
        drawCircle(
            color = if (isFinished) Color.Red else vibrantOrange.copy(alpha = 0.5f),
            radius = watchRadius - 2.dp.toPx(),
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // 3. Draw 60 Dial Ticks & Hour Batons
        for (i in 0 until 60) {
            val angleRad = Math.toRadians((i * 6 - 90).toDouble())
            val isHourMark = i % 5 == 0

            val tickLength = if (isHourMark) 14.dp.toPx() else 6.dp.toPx()
            val tickWidth = if (isHourMark) 3.5.dp.toPx() else 1.5.dp.toPx()

            val outerX = center.x + (watchRadius - 6.dp.toPx()) * cos(angleRad).toFloat()
            val outerY = center.y + (watchRadius - 6.dp.toPx()) * sin(angleRad).toFloat()
            val innerX = center.x + (watchRadius - 6.dp.toPx() - tickLength) * cos(angleRad).toFloat()
            val innerY = center.y + (watchRadius - 6.dp.toPx() - tickLength) * sin(angleRad).toFloat()

            drawLine(
                color = if (isHourMark) vibrantOrange else Color.Gray.copy(alpha = 0.5f),
                start = Offset(innerX, innerY),
                end = Offset(outerX, outerY),
                strokeWidth = tickWidth
            )
        }

        // ==================== CASCADING ADAPTIVE SUB-DIALS (COMPLICATIONS) ====================
        val subRadius = watchRadius * 0.26f

        if (isMinutesScale) {
            // WHEN IN MINUTES MODE:
            // SUB-DIAL 1 (LEFT - 9 o'clock): SECONDS SUB-DIAL (0 to 60s)
            val sub1Center = Offset(center.x - watchRadius * 0.38f, center.y - watchRadius * 0.05f)
            drawSubDial(
                center = sub1Center,
                radius = subRadius,
                title = "SEC",
                labels = listOf("0", "15", "30", "45"),
                handAngle = ((seconds % 60) + hundredths / 100f) / 60f * 360f - 90f,
                handColor = Color.White,
                vibrantOrange = vibrantOrange
            )

            // SUB-DIAL 2 (RIGHT - 3 o'clock): HUNDREDTHS COMPLICATION (0 to 100)
            val sub2Center = Offset(center.x + watchRadius * 0.38f, center.y - watchRadius * 0.05f)
            drawSubDial(
                center = sub2Center,
                radius = subRadius,
                title = "MSEC",
                labels = listOf(".00", ".25", ".50", ".75"),
                handAngle = (hundredths / 100f) * 360f - 90f,
                handColor = if (isFinished) Color.Red else vibrantOrange,
                vibrantOrange = vibrantOrange
            )

            // SUB-DIAL 3 (BOTTOM - 6 o'clock): HOURS SUB-DIAL (0 to 12h)
            val sub3Center = Offset(center.x, center.y + watchRadius * 0.42f)
            drawSubDial(
                center = sub3Center,
                radius = subRadius,
                title = "HRS",
                labels = listOf("12", "3", "6", "9"),
                handAngle = ((hours % 12) + minutes / 60f) / 12f * 360f - 90f,
                handColor = Color.White,
                vibrantOrange = vibrantOrange
            )
        } else {
            // WHEN IN SECONDS MODE (< 1 MIN):
            // SUB-DIAL 1 (LEFT - 9 o'clock): MINUTES ACCUMULATED SUB-DIAL (0 to 30m)
            val sub1Center = Offset(center.x - watchRadius * 0.38f, center.y - watchRadius * 0.05f)
            drawSubDial(
                center = sub1Center,
                radius = subRadius,
                title = "MIN",
                labels = listOf("0", "10", "20", "30"),
                handAngle = ((minutes % 30) + seconds / 60f) / 30f * 360f - 90f,
                handColor = Color.White,
                vibrantOrange = vibrantOrange
            )

            // SUB-DIAL 2 (RIGHT - 3 o'clock): HUNDREDTHS COMPLICATION (0 to 100)
            val sub2Center = Offset(center.x + watchRadius * 0.38f, center.y - watchRadius * 0.05f)
            drawSubDial(
                center = sub2Center,
                radius = subRadius,
                title = "MSEC",
                labels = listOf(".00", ".25", ".50", ".75"),
                handAngle = (hundredths / 100f) * 360f - 90f,
                handColor = if (isFinished) Color.Red else vibrantOrange,
                vibrantOrange = vibrantOrange
            )

            // SUB-DIAL 3 (BOTTOM - 6 o'clock): SECONDS COMPLICATION (0 to 60s)
            val sub3Center = Offset(center.x, center.y + watchRadius * 0.42f)
            drawSubDial(
                center = sub3Center,
                radius = subRadius,
                title = "SEC",
                labels = listOf("0", "15", "30", "45"),
                handAngle = ((seconds % 60) + hundredths / 100f) / 60f * 360f - 90f,
                handColor = Color.White,
                vibrantOrange = vibrantOrange
            )
        }

        // ==================== APERTURE DIGITAL WINDOW (12 o'clock) ====================
        val textPaint = Paint().apply {
            color = textPaintColor
            textSize = 12.sp.toPx()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }

        val windowRectWidth = 90.dp.toPx()
        val windowRectHeight = 24.dp.toPx()
        val windowY = center.y - watchRadius * 0.52f

        drawRoundRect(
            color = Color(0xFF22252E),
            topLeft = Offset(center.x - windowRectWidth / 2f, windowY - windowRectHeight / 2f),
            size = androidx.compose.ui.geometry.Size(windowRectWidth, windowRectHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
        )
        drawRoundRect(
            color = vibrantOrange.copy(alpha = 0.5f),
            topLeft = Offset(center.x - windowRectWidth / 2f, windowY - windowRectHeight / 2f),
            size = androidx.compose.ui.geometry.Size(windowRectWidth, windowRectHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
            style = Stroke(width = 1.dp.toPx())
        )

        val formattedDigital = if (isFinished) "00:00.00" else formatStopwatchTime(elapsedMs)
        drawContext.canvas.nativeCanvas.drawText(
            formattedDigital,
            center.x,
            windowY + 4.dp.toPx(),
            textPaint
        )

        // ==================== MAIN CHRONOGRAPH SWEEP HAND (ADAPTIVE) ====================
        val mainHandAngleRad = if (isMinutesScale) {
            Math.toRadians((((minutes % 60) + seconds / 60f) / 60f * 360f - 90).toDouble())
        } else {
            Math.toRadians((((seconds % 60) + hundredths / 100f) / 60f * 360f - 90).toDouble())
        }

        val mainHandLength = watchRadius * 0.82f
        val mainHandEndX = center.x + mainHandLength * cos(mainHandAngleRad).toFloat()
        val mainHandEndY = center.y + mainHandLength * sin(mainHandAngleRad).toFloat()

        // Counterweight Tail
        val tailX = center.x - (watchRadius * 0.18f) * cos(mainHandAngleRad).toFloat()
        val tailY = center.y - (watchRadius * 0.18f) * sin(mainHandAngleRad).toFloat()

        // Draw Main Needle Line
        drawLine(
            color = if (isFinished) Color.Red else vibrantOrange,
            start = Offset(tailX, tailY),
            end = Offset(mainHandEndX, mainHandEndY),
            strokeWidth = 3.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Center Pivot Hub
        drawCircle(
            color = Color(0xFF22252E),
            radius = 8.dp.toPx(),
            center = center
        )
        drawCircle(
            color = if (isFinished) Color.Red else vibrantOrange,
            radius = 5.dp.toPx(),
            center = center
        )
        drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = center
        )
    }
}

// Helper to draw a cascading sub-dial complication on the watch face
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSubDial(
    center: Offset,
    radius: Float,
    title: String,
    labels: List<String>,
    handAngle: Float,
    handColor: Color,
    vibrantOrange: Color
) {
    // Sub-dial rim & face
    drawCircle(
        color = Color(0xFF1B1D24),
        radius = radius,
        center = center
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.15f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )

    // Sub-dial Unit Title Tag (MIN, SEC, MSEC, HRS)
    val unitPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#FF8C00")
        textSize = 8.sp.toPx()
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText(
        title.uppercase(),
        center.x,
        center.y + radius * 0.48f,
        unitPaint
    )

    // Sub-dial ticks (12 ticks around sub-dial)
    for (i in 0 until 12) {
        val tickAngleRad = Math.toRadians((i * 30 - 90).toDouble())
        val isMajor = i % 3 == 0
        val len = if (isMajor) 4.dp.toPx() else 2.dp.toPx()

        val outerX = center.x + (radius - 2.dp.toPx()) * cos(tickAngleRad).toFloat()
        val outerY = center.y + (radius - 2.dp.toPx()) * sin(tickAngleRad).toFloat()
        val innerX = center.x + (radius - 2.dp.toPx() - len) * cos(tickAngleRad).toFloat()
        val innerY = center.y + (radius - 2.dp.toPx() - len) * sin(tickAngleRad).toFloat()

        drawLine(
            color = if (isMajor) vibrantOrange else Color.Gray.copy(alpha = 0.4f),
            start = Offset(innerX, innerY),
            end = Offset(outerX, outerY),
            strokeWidth = 1.dp.toPx()
        )
    }

    // Sub-dial Hand Needle
    val handRad = Math.toRadians(handAngle.toDouble())
    val handLen = radius * 0.75f
    val endX = center.x + handLen * cos(handRad).toFloat()
    val endY = center.y + handLen * sin(handRad).toFloat()

    drawLine(
        color = handColor,
        start = center,
        end = Offset(endX, endY),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Sub-dial Center Cap
    drawCircle(
        color = Color.White,
        radius = 2.5.dp.toPx(),
        center = center
    )
}

// Split stopwatch time into MM:SS.ss
private fun formatStopwatchTime(ms: Long): String {
    val totalSecs = ms / 1000
    val minutes = totalSecs / 60
    val seconds = totalSecs % 60
    val hundredths = (ms % 1000) / 10

    return if (minutes >= 60) {
        val hours = minutes / 60
        val remMins = minutes % 60
        String.format(Locale.ENGLISH, "%02d:%02d:%02d.%02d", hours, remMins, seconds, hundredths)
    } else {
        String.format(Locale.ENGLISH, "%02d:%02d.%02d", minutes, seconds, hundredths)
    }
}

// Format countdown remaining milliseconds to MM:SS or HH:MM:SS
private fun formatCountdownTime(ms: Long): String {
    val totalSecs = (ms + 999) / 1000 // Round up
    val minutes = totalSecs / 60
    val seconds = totalSecs % 60

    return if (minutes >= 60) {
        val hours = minutes / 60
        val remMins = minutes % 60
        String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, remMins, seconds)
    } else {
        String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds)
    }
}

// Trigger haptic alarm on timer completion
private fun triggerTimerAlarm(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300, 200, 500), -1))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300, 200, 500), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(1000)
            }
        }
        Toast.makeText(context, "Timer Completed!", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        // Fallback toast
        Toast.makeText(context, "Timer Completed!", Toast.LENGTH_LONG).show()
    }
}
