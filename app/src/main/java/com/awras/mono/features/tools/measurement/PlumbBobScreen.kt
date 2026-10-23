package com.awras.mono.features.tools.measurement

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awras.mono.ui.components.DatumCameraView
import com.awras.mono.ui.components.DatumToolTopBar
import com.awras.mono.ui.theme.LocalTactileFeedback
import compose.icons.TablerIcons
import compose.icons.tablericons.Anchor
import compose.icons.tablericons.Camera
import compose.icons.tablericons.CameraOff
import java.util.Locale
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlumbBobScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    // Sensor State
    var rawRoll by remember { mutableFloatStateOf(0f) }

    // Tare State
    var tareRoll by remember { mutableFloatStateOf(0f) }
    var isTared by remember { mutableStateOf(false) }

    // Lock State
    var isLocked by remember { mutableStateOf(false) }
    var lockedRoll by remember { mutableFloatStateOf(0f) }

    // Camera Mode Toggle State
    var isCameraEnabled by remember { mutableStateOf(false) }

    // Register Accelerometer Sensor
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (isLocked) return

                val ax = event.values[0]
                val ay = event.values[1]

                // Vertical plumb angle along roll X-axis
                val rollDeg = Math.toDegrees(atan2(-ax.toDouble(), ay.toDouble())).toFloat()
                rawRoll = rollDeg
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Active Angle with Tare and Lock handling
    val activeRoll = if (isLocked) lockedRoll else (if (isTared) rawRoll - tareRoll else rawRoll)

    // Smooth Pendulum Physics Motion
    val smoothPlumbAngle by animateFloatAsState(
        targetValue = activeRoll,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "SmoothPlumbAngle"
    )

    val absPlumbAngle = abs(smoothPlumbAngle)
    val isPlumbAligned = absPlumbAngle <= 0.5f

    // Haptic confirmation
    var lastPlumbState by remember { mutableStateOf(false) }
    LaunchedEffect(isPlumbAligned) {
        if (isPlumbAligned && !lastPlumbState) {
            haptics.success()
        }
        lastPlumbState = isPlumbAligned
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Plumb Bob",
                description = if (isTared) "Relative Zero Active" else "Gravity Vertical Alignment",
                icon = TablerIcons.Anchor,
                onBackClick = onBackClick,
                rightActionIcon = if (isTared) Icons.Default.Refresh else Icons.Default.FilterCenterFocus,
                onRightActionClick = {
                    haptics.click()
                    if (isTared) {
                        tareRoll = 0f
                        isTared = false
                    } else {
                        tareRoll = rawRoll
                        isTared = true
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. TOP PROMINENT ANGLE READOUT
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f", absPlumbAngle),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 58.sp),
                        fontWeight = FontWeight.Black,
                        fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                        letterSpacing = (-1.5).sp,
                        color = if (isPlumbAligned) vibrantOrange else MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "°",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                        color = if (isPlumbAligned) vibrantOrange else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(Locale.US, "DEVIATION: %+.1f°", smoothPlumbAngle),
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
                        text = if (isPlumbAligned) "PERFECTLY PLUMB" else "UNALIGNED",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isPlumbAligned) vibrantOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. REUSABLE DATUM CAMERA VIEW CONTAINER (EVEN 12.DP MARGINS & EXTENDED BOTTOM)
            val defaultBobColor = if (isCameraEnabled) Color.White else MaterialTheme.colorScheme.onBackground
            val bobColor = if (isPlumbAligned) vibrantOrange else defaultBobColor

            DatumCameraView(
                isCameraEnabled = isCameraEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                cornerRadius = 28.dp
            ) {
                // Measurement Overlay Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, 40f)
                    val stringLength = size.height - 110f

                    // True Vertical Center Guideline (High-visibility outlined line)
                    val lineAccentColor = if (isPlumbAligned) vibrantOrange else Color.White
                    val lineOutlineColor = Color.Black.copy(alpha = 0.85f)

                    // Black Outer Border Line for light camera backgrounds
                    drawLine(
                        color = lineOutlineColor,
                        start = Offset(center.x, center.y),
                        end = Offset(center.x, size.height - 20f),
                        strokeWidth = 3.5f
                    )
                    // Bright Inner Core Line for dark camera backgrounds
                    drawLine(
                        color = lineAccentColor,
                        start = Offset(center.x, center.y),
                        end = Offset(center.x, size.height - 20f),
                        strokeWidth = 1.5f
                    )

                    // Calculate Pendulum String End Position
                    val rad = Math.toRadians((smoothPlumbAngle + 90.0))
                    val bobX = center.x + stringLength * cos(rad).toFloat()
                    val bobY = center.y + stringLength * sin(rad).toFloat()

                    // Hanging String
                    drawLine(
                        color = bobColor,
                        start = center,
                        end = Offset(bobX, bobY),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )

                    // Top Pivot Ring
                    drawCircle(
                        color = vibrantOrange,
                        radius = 8f,
                        center = center
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3f,
                        center = center
                    )

                    // Plumb Bob Body
                    drawCircle(
                        color = bobColor,
                        radius = 20f,
                        center = Offset(bobX, bobY)
                    )

                    // Pointed Tip at Bottom
                    val tipPath = Path().apply {
                        moveTo(bobX - 16f, bobY + 8f)
                        lineTo(bobX + 16f, bobY + 8f)
                        lineTo(bobX, bobY + 36f)
                        close()
                    }
                    drawPath(path = tipPath, color = bobColor)
                }

                // Inside Viewport: Bottom-Left Lock Button (with navigation bar clearance)
                IconButton(
                    onClick = {
                        if (!isLocked) {
                            lockedRoll = activeRoll
                        }
                        isLocked = !isLocked
                        if (isLocked) haptics.click() else haptics.tick()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .navigationBarsPadding()
                        .padding(start = 16.dp, bottom = 16.dp)
                        .alpha(0.60f)
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Lock Toggle",
                        tint = if (isCameraEnabled) Color.White else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Inside Viewport: Bottom-Right Camera Toggle Button (with navigation bar clearance)
                IconButton(
                    onClick = {
                        haptics.click()
                        isCameraEnabled = !isCameraEnabled
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 16.dp, bottom = 16.dp)
                        .alpha(if (isCameraEnabled) 0.95f else 0.60f)
                ) {
                    Icon(
                        imageVector = if (isCameraEnabled) TablerIcons.CameraOff else TablerIcons.Camera,
                        contentDescription = "Camera Toggle",
                        tint = if (isCameraEnabled) vibrantOrange else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
