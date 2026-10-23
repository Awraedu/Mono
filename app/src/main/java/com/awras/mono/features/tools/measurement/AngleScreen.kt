package com.awras.mono.features.tools.measurement

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awras.mono.ui.components.DatumToolTopBar
import com.awras.mono.ui.theme.LocalTactileFeedback
import com.kyant.capsule.ContinuousCapsule
import compose.icons.TablerIcons
import compose.icons.tablericons.Compass
import java.util.Locale
import kotlin.math.*

enum class AngleMode {
    SENSOR,
    TOUCH
}

private val VibrantOrange = Color(0xFFFF5700)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AngleScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current

    var angleMode by remember { mutableStateOf(AngleMode.SENSOR) }

    // Sensor State for Inclinometer
    var rawPitch by remember { mutableFloatStateOf(0f) }
    var rawRoll by remember { mutableFloatStateOf(0f) }
    var tareAngle by remember { mutableFloatStateOf(0f) }
    var isTared by remember { mutableStateOf(false) }

    // Touch Protractor State: Two independent touch handles (angles in degrees 0° to 360°, 0° = top)
    var handle1Deg by remember { mutableFloatStateOf(0.0f) }
    var handle2Deg by remember { mutableFloatStateOf(90.0f) }

    // Register Accelerometer & Magnetometer Sensors with low-pass dampening
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
                if (angleMode != AngleMode.SENSOR) return

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

        accelSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        magSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Active Sensor Angle (0° at top vertical using rawPitch)
    val activeSensorAngle = if (isTared) (rawPitch - tareAngle) else rawPitch

    // Super smooth spring animation for sensor angle to eliminate jitter
    val smoothSensorAngle by animateFloatAsState(
        targetValue = activeSensorAngle,
        animationSpec = spring(stiffness = Spring.StiffnessVeryLow, dampingRatio = Spring.DampingRatioHighBouncy),
        label = "SmoothSensorAngle"
    )

    // Calculated Touch Angles
    val diffDeg = abs(handle2Deg - handle1Deg) % 360f
    val internalTouchAngle = if (diffDeg > 180f) 360f - diffDeg else diffDeg
    val outsideTouchAngle = 360f - internalTouchAngle

    val primaryDisplayAngle = if (angleMode == AngleMode.SENSOR) abs(smoothSensorAngle) else internalTouchAngle
    val secondaryDisplayAngle = if (angleMode == AngleMode.SENSOR) (360f - abs(smoothSensorAngle)) % 360f else outsideTouchAngle

    val isZeroAligned = primaryDisplayAngle <= 0.5f || abs(primaryDisplayAngle - 90f) <= 0.5f || abs(primaryDisplayAngle - 180f) <= 0.5f

    var lastAlignedState by remember { mutableStateOf(false) }
    LaunchedEffect(isZeroAligned) {
        if (isZeroAligned && !lastAlignedState) {
            haptics.success()
        }
        lastAlignedState = isZeroAligned
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Protractor",
                description = if (angleMode == AngleMode.SENSOR) "Diagonal Surface Inclinometer" else "Dual-Handle Touch Angle",
                icon = TablerIcons.Compass,
                onBackClick = onBackClick,
                rightActionIcon = if (angleMode == AngleMode.SENSOR) Icons.Default.FilterCenterFocus else Icons.Default.Refresh,
                onRightActionClick = {
                    haptics.click()
                    if (angleMode == AngleMode.SENSOR) {
                        if (isTared) {
                            tareAngle = 0f
                            isTared = false
                        } else {
                            tareAngle = rawPitch
                            isTared = true
                        }
                    } else {
                        handle1Deg = 0f
                        handle2Deg = 90f
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
                // 1. TOP PROMINENT ANGLE DISPLAY AT TOP CENTER
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f", primaryDisplayAngle),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 58.sp),
                            fontWeight = FontWeight.Black,
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            letterSpacing = (-1.5).sp,
                            color = if (isZeroAligned) VibrantOrange else MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "°",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            color = if (isZeroAligned) VibrantOrange else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (angleMode == AngleMode.SENSOR) {
                                String.format(Locale.US, "SLOPE: %.1f%%", abs(tan(primaryDisplayAngle * PI / 180.0) * 100.0))
                            } else {
                                String.format(Locale.US, "OUTSIDE: %.1f°", secondaryDisplayAngle)
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
                            text = String.format(Locale.US, "RADIAN: %.2f rad", abs(primaryDisplayAngle * PI / 180.0)),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. CENTER INTERACTIVE ROTATED PROTRACTOR DIAL CANVAS (0° at top)
                val flatSurfaceBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                val guideColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)

                Box(
                    modifier = Modifier
                        .size(340.dp)
                        .clip(CircleShape)
                        .background(flatSurfaceBg)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)), CircleShape)
                        .pointerInput(angleMode) {
                            if (angleMode == AngleMode.TOUCH) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val pointerPos = change.position
                                    val dx = pointerPos.x - center.x
                                    val dy = pointerPos.y - center.y
                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                                    if (angle < 0f) angle += 360f

                                    // Snap to major angles (0, 90, 180, 270) within tight 0.8° threshold for precision
                                    val cardinalAngles = listOf(0f, 90f, 180f, 270f, 360f)
                                    var snapped = false
                                    for (cardinal in cardinalAngles) {
                                        if (abs(angle - cardinal) <= 0.8f) {
                                            angle = if (cardinal == 360f) 0f else cardinal
                                            snapped = true
                                            break
                                        }
                                    }

                                    val dist1 = abs(angle - handle1Deg)
                                    val dist2 = abs(angle - handle2Deg)
                                    if (dist1 < dist2) {
                                        if (snapped && handle1Deg != angle) {
                                            haptics.click()
                                        }
                                        handle1Deg = angle
                                    } else {
                                        if (snapped && handle2Deg != angle) {
                                            haptics.click()
                                        }
                                        handle2Deg = angle
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = (size.width / 2f) - 16f

                        // Draw Protractor Degree Ticks (0° at top)
                        for (deg in 0 until 360 step 5) {
                            val rad = Math.toRadians((deg - 90).toDouble())
                            val isMajor = deg % 30 == 0
                            val tickLen = if (isMajor) 16f else 8f
                            val strokeW = if (isMajor) 2f else 1f

                            val cosVal = cos(rad).toFloat()
                            val sinVal = sin(rad).toFloat()

                            val startX = center.x + (radius - tickLen) * cosVal
                            val startY = center.y + (radius - tickLen) * sinVal
                            val endX = center.x + radius * cosVal
                            val endY = center.y + radius * sinVal

                            drawLine(
                                color = guideColor.copy(alpha = if (isMajor) 0.6f else 0.3f),
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = strokeW
                            )
                        }

                        // Crosshair
                        drawLine(
                            color = guideColor,
                            start = Offset(center.x, center.y - radius + 24f),
                            end = Offset(center.x, center.y + radius - 24f),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = guideColor,
                            start = Offset(center.x - radius + 24f, center.y),
                            end = Offset(center.x + radius - 24f, center.y),
                            strokeWidth = 1f
                        )

                        if (angleMode == AngleMode.SENSOR) {
                            // Sensor Inclinometer Mode (0° at top)
                            val pointerRad = Math.toRadians((activeSensorAngle - 90.0))
                            val pointerEndX = center.x + (radius - 36f) * cos(pointerRad).toFloat()
                            val pointerEndY = center.y + (radius - 36f) * sin(pointerRad).toFloat()

                            drawLine(
                                color = VibrantOrange,
                                start = center,
                                end = Offset(pointerEndX, pointerEndY),
                                strokeWidth = 4f,
                                cap = StrokeCap.Round
                            )
                        } else {
                            // Touch Protractor Mode: Two handles (both VibrantOrange), filled sector between them
                            val rad1 = Math.toRadians((handle1Deg - 90.0))
                            val rad2 = Math.toRadians((handle2Deg - 90.0))

                            val x1 = center.x + (radius - 32f) * cos(rad1).toFloat()
                            val y1 = center.y + (radius - 32f) * sin(rad1).toFloat()
                            val x2 = center.x + (radius - 32f) * cos(rad2).toFloat()
                            val y2 = center.y + (radius - 32f) * sin(rad2).toFloat()

                            // Fill sector / area between handle1 and handle2
                            val sweepAngle = if (handle2Deg >= handle1Deg) handle2Deg - handle1Deg else 360f - (handle1Deg - handle2Deg)
                            drawArc(
                                color = VibrantOrange.copy(alpha = 0.18f),
                                startAngle = handle1Deg - 90f,
                                sweepAngle = sweepAngle,
                                useCenter = true,
                                topLeft = Offset(center.x - radius + 32f, center.y - radius + 32f),
                                size = Size((radius - 32f) * 2f, (radius - 32f) * 2f)
                            )

                            // Ray 1
                            drawLine(
                                color = VibrantOrange,
                                start = center,
                                end = Offset(x1, y1),
                                strokeWidth = 3.5f,
                                cap = StrokeCap.Round
                            )
                            // Ray 2
                            drawLine(
                                color = VibrantOrange,
                                start = center,
                                end = Offset(x2, y2),
                                strokeWidth = 3.5f,
                                cap = StrokeCap.Round
                            )

                            // Handle 1 Knob (Both VibrantOrange)
                            drawCircle(
                                color = VibrantOrange,
                                radius = 14f,
                                center = Offset(x1, y1)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 5f,
                                center = Offset(x1, y1)
                            )

                            // Handle 2 Knob (Both VibrantOrange)
                            drawCircle(
                                color = VibrantOrange,
                                radius = 14f,
                                center = Offset(x2, y2)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 5f,
                                center = Offset(x2, y2)
                            )
                        }

                        // Center Pivot Disc
                        drawCircle(
                            color = VibrantOrange,
                            radius = 8f,
                            center = center
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3f,
                            center = center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. BOTTOM MODE SWITCHER PILL (SENSOR vs TOUCH)
                Surface(
                    modifier = Modifier.clip(ContinuousCapsule),
                    shape = ContinuousCapsule,
                    color = flatSurfaceBg,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AngleMode.entries.forEach { mode ->
                            val isSelected = angleMode == mode
                            val bgColor = if (isSelected) VibrantOrange else Color.Transparent
                            val txtColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

                            Box(
                                modifier = Modifier
                                    .clip(ContinuousCapsule)
                                    .background(bgColor)
                                    .clickable {
                                        haptics.tick()
                                        angleMode = mode
                                    }
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                                    color = txtColor,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
