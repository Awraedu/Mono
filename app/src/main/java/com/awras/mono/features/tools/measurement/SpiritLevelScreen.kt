package com.awras.mono.features.tools.measurement

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awras.mono.ui.components.DatumToolTopBar
import com.awras.mono.ui.theme.LocalTactileFeedback
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowLeft
import compose.icons.tablericons.ViewportWide
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpiritLevelScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    // Sensor State
    var rawPitch by remember { mutableFloatStateOf(0f) }
    var rawRoll by remember { mutableFloatStateOf(0f) }

    // Tare / Relative Zero State
    var tarePitch by remember { mutableFloatStateOf(0f) }
    var tareRoll by remember { mutableFloatStateOf(0f) }
    var isTared by remember { mutableStateOf(false) }

    // Lock State
    var isLocked by remember { mutableStateOf(false) }
    var lockedPitch by remember { mutableFloatStateOf(0f) }
    var lockedRoll by remember { mutableFloatStateOf(0f) }

    // Register Accelerometer & Magnetometer Sensors
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val accelValues = FloatArray(3)
        val magValues = FloatArray(3)
        var hasAccel = false
        var hasMag = false

        val rotationMatrix = FloatArray(9)
        val orientationValues = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (isLocked) return

                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, accelValues, 0, 3)
                        hasAccel = true
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, magValues, 0, 3)
                        hasMag = true
                    }
                }

                if (hasAccel && hasMag) {
                    val success = SensorManager.getRotationMatrix(rotationMatrix, null, accelValues, magValues)
                    if (success) {
                        SensorManager.getOrientation(rotationMatrix, orientationValues)
                        rawPitch = Math.toDegrees(orientationValues[1].toDouble()).toFloat()
                        rawRoll = Math.toDegrees(orientationValues[2].toDouble()).toFloat()
                    }
                } else if (hasAccel) {
                    val ax = accelValues[0]
                    val ay = accelValues[1]
                    val az = accelValues[2]

                    rawPitch = Math.toDegrees(atan2(-ax.toDouble(), sqrt((ay * ay + az * az).toDouble()))).toFloat()
                    rawRoll = Math.toDegrees(atan2(ay.toDouble(), az.toDouble())).toFloat()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        magSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Active Angles with Tare offset
    val activePitch = if (isLocked) lockedPitch else (rawPitch - tarePitch)
    val activeRoll = if (isLocked) lockedRoll else (rawRoll - tareRoll)

    // Smooth Inertial Motion Animation for Physical Bubble Ball Dynamics
    val smoothPitch by animateFloatAsState(
        targetValue = activePitch,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "SmoothPitch"
    )
    val smoothRoll by animateFloatAsState(
        targetValue = activeRoll,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "SmoothRoll"
    )

    // Total Tilt Angle
    val totalTilt = sqrt(smoothPitch * smoothPitch + smoothRoll * smoothRoll)
    val isLevel = totalTilt <= 0.5f
    val isXLevel = abs(smoothRoll) <= 0.5f
    val isYLevel = abs(smoothPitch) <= 0.5f

    // Haptic pulse when level is achieved
    var lastLevelState by remember { mutableStateOf(false) }
    LaunchedEffect(isLevel) {
        if (isLevel && !lastLevelState) {
            haptics.success()
        }
        lastLevelState = isLevel
    }

    // Dynamic Bubble Color
    val unlevelBubbleColor = MaterialTheme.colorScheme.onBackground
    val activeBubbleColor = if (isLevel) vibrantOrange else unlevelBubbleColor

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Spirit Level",
                description = if (isTared) "Relative Zero Active" else "Precision Multi-Axis Sensor",
                icon = TablerIcons.ViewportWide,
                onBackClick = onBackClick,
                rightActionIcon = if (isTared) Icons.Default.Refresh else Icons.Default.FilterCenterFocus,
                onRightActionClick = {
                    haptics.click()
                    if (isTared) {
                        tarePitch = 0f
                        tareRoll = 0f
                        isTared = false
                    } else {
                        tarePitch = rawPitch
                        tareRoll = rawRoll
                        isTared = true
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. TOP WIDE FLAT HORIZONTAL TUBE LEVEL WITH SCALED-UP BUBBLE & MULTI-SEGMENT GUIDELINES
                val horizTubeBubbleColor = if (isXLevel) vibrantOrange else unlevelBubbleColor
                val tubeGuideColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                val flatSurfaceBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = ContinuousCapsule,
                    color = flatSurfaceBg,
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val midX = size.width / 2f
                        val midY = size.height / 2f

                        // Multi-segment grid lines across the horizontal pill
                        val segmentWidth = size.width / 8f
                        for (i in 1 until 8) {
                            val lineX = i * segmentWidth
                            drawLine(
                                color = tubeGuideColor.copy(alpha = 0.10f),
                                start = Offset(lineX, 0f),
                                end = Offset(lineX, size.height),
                                strokeWidth = 1f
                            )
                        }

                        // Center Target Lines
                        val targetGapPx = 28f
                        drawLine(
                            color = tubeGuideColor,
                            start = Offset(midX - targetGapPx, 0f),
                            end = Offset(midX - targetGapPx, size.height),
                            strokeWidth = 1.2f
                        )
                        drawLine(
                            color = tubeGuideColor,
                            start = Offset(midX + targetGapPx, 0f),
                            end = Offset(midX + targetGapPx, size.height),
                            strokeWidth = 1.2f
                        )

                        // Center centerline
                        drawLine(
                            color = if (isXLevel) vibrantOrange.copy(alpha = 0.6f) else tubeGuideColor,
                            start = Offset(midX, 6f),
                            end = Offset(midX, size.height - 6f),
                            strokeWidth = 1.2f
                        )

                        // Scaled-up Horizontal Bubble Ball (Radius 24f to nearly fill parent bar height)
                        val maxOffsetX = (size.width / 2f) - 28f
                        val scaleX = maxOffsetX / 20f
                        val bubbleX = (midX + (smoothRoll * scaleX)).coerceIn(28f, size.width - 28f)

                        drawCircle(
                            color = horizTubeBubbleColor,
                            radius = 24.dp.toPx(),
                            center = Offset(bubbleX, midY)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. BIG FLAT CIRCULAR BULLSEYE LEVEL (NO SHADOW)
                val guideColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)

                Surface(
                    modifier = Modifier
                        .size(310.dp),
                    shape = CircleShape,
                    color = flatSurfaceBg,
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val outerRadiusPx = size.width / 2f
                        val midRadiusPx = outerRadiusPx * 0.55f
                        val innerRadiusPx = outerRadiusPx * 0.25f

                        // Outer Target Ring
                        drawCircle(
                            color = guideColor,
                            radius = outerRadiusPx - 4f,
                            center = center,
                            style = Stroke(width = 1f)
                        )

                        // Middle Target Ring
                        drawCircle(
                            color = guideColor,
                            radius = midRadiusPx,
                            center = center,
                            style = Stroke(width = 1f)
                        )

                        // Center Lock Ring
                        drawCircle(
                            color = if (isLevel) vibrantOrange.copy(alpha = 0.85f) else guideColor,
                            radius = innerRadiusPx,
                            center = center,
                            style = Stroke(width = 1.5f)
                        )

                        // Crosshair Alignment Lines
                        drawLine(
                            color = guideColor,
                            start = Offset(center.x - outerRadiusPx + 12f, center.y),
                            end = Offset(center.x + outerRadiusPx - 12f, center.y),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = guideColor,
                            start = Offset(center.x, center.y - outerRadiusPx + 12f),
                            end = Offset(center.x, center.y + outerRadiusPx - 12f),
                            strokeWidth = 1f
                        )

                        // Calculate Smooth 2D Bubble Offset
                        val maxBubbleOffsetPx = outerRadiusPx - innerRadiusPx - 8f
                        val scaleFactor = maxBubbleOffsetPx / 20f

                        val bubbleX = (center.x + (smoothRoll * scaleFactor)).coerceIn(
                            center.x - maxBubbleOffsetPx,
                            center.x + maxBubbleOffsetPx
                        )
                        val bubbleY = (center.y - (smoothPitch * scaleFactor)).coerceIn(
                            center.y - maxBubbleOffsetPx,
                            center.y + maxBubbleOffsetPx
                        )

                        // Clean Flat Bubble Ball (No shadow, no inner gloss)
                        drawCircle(
                            color = activeBubbleColor,
                            radius = innerRadiusPx - 2f,
                            center = Offset(bubbleX, bubbleY)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. BOTTOM READOUT BAR (X-AXIS, BIG GENERAL AVERAGE ANGLE, Y-AXIS)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // X-Axis Readout
                    Column {
                        Text(
                            text = "X-AXIS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f°", abs(smoothRoll)),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            color = if (isXLevel) vibrantOrange else MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Center Big General Average Angle Display
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "AVERAGE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f°", totalTilt),
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                            fontWeight = FontWeight.Black,
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            color = if (isLevel) vibrantOrange else MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Y-Axis Readout
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Y-AXIS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f°", abs(smoothPitch)),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            color = if (isYLevel) vibrantOrange else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Bottom-Left Subtle Lock Toggle Button (Icon only, low alpha)
            IconButton(
                onClick = {
                    if (!isLocked) {
                        lockedPitch = activePitch
                        lockedRoll = activeRoll
                    }
                    isLocked = !isLocked
                    if (isLocked) haptics.click() else haptics.tick()
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 28.dp)
                    .alpha(0.45f)
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Lock Toggle",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
