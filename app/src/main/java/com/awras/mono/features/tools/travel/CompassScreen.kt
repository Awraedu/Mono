package com.awras.mono.features.tools.travel

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awras.mono.ui.components.DatumToolTopBar
import com.awras.mono.ui.theme.LocalTactileFeedback
import compose.icons.TablerIcons
import compose.icons.tablericons.Compass
import java.util.Locale
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompassScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer
    val textMeasurer = rememberTextMeasurer()

    // Hardware Compass Sensor Heading
    var cumulativeHeading by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: @Suppress("DEPRECATION") sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)

        val listener = object : SensorEventListener {
            val rotationMatrix = FloatArray(9)
            val orientation = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent?) {
                event ?: return
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val rawDeg = (Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0

                    var diff = (rawDeg.toFloat() - (cumulativeHeading % 360f))
                    diff = (diff + 540f) % 360f - 180f
                    cumulativeHeading += diff
                } else if (@Suppress("DEPRECATION") event.sensor.type == Sensor.TYPE_ORIENTATION) {
                    val rawDeg = (event.values[0] + 360f) % 360f
                    var diff = (rawDeg - (cumulativeHeading % 360f))
                    diff = (diff + 540f) % 360f - 180f
                    cumulativeHeading += diff
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (sensorManager != null && rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    // Smooth Heading Animation
    val animatedHeading by animateFloatAsState(
        targetValue = cumulativeHeading,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "AnimatedHeading"
    )

    val currentHeadingDeg = ((animatedHeading % 360f) + 360f) % 360f

    val cardinalDirection = when {
        currentHeadingDeg in 337.5..360.0 || currentHeadingDeg in 0.0..22.5 -> "NORTH"
        currentHeadingDeg in 22.5..67.5 -> "NORTH EAST"
        currentHeadingDeg in 67.5..112.5 -> "EAST"
        currentHeadingDeg in 112.5..157.5 -> "SOUTH EAST"
        currentHeadingDeg in 157.5..202.5 -> "SOUTH"
        currentHeadingDeg in 202.5..247.5 -> "SOUTH WEST"
        currentHeadingDeg in 247.5..292.5 -> "WEST"
        else -> "NORTH WEST"
    }

    val isTrueNorth = currentHeadingDeg in 359.0..360.0 || currentHeadingDeg in 0.0..1.0

    // True North Haptic
    var lastNorthState by remember { mutableStateOf(false) }
    LaunchedEffect(isTrueNorth) {
        if (isTrueNorth && !lastNorthState) {
            haptics.success()
        }
        lastNorthState = isTrueNorth
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Compass",
                description = "Precision Navigation",
                icon = TablerIcons.Compass,
                onBackClick = onBackClick,
                rightActionIcon = Icons.Default.CompassCalibration,
                onRightActionClick = {
                    haptics.click()
                    Toast.makeText(context, "Rotate device in figure-8 pattern to calibrate", Toast.LENGTH_SHORT).show()
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 1. MINIMALIST ROTATING COMPASS DIAL WITH FLUSH TOP POINTER & CENTER NUMBER READOUT
            val guideColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
            val textOnSurface = MaterialTheme.colorScheme.onSurface

            Box(
                modifier = Modifier
                    .size(340.dp)
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Rotating Scale Dial (Tick marks and Cardinal Labels)
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(-animatedHeading)
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = (size.width / 2f) - 12f

                    // 180 Scale Ticks (Every 2 degrees)
                    for (i in 0 until 180) {
                        val deg = i * 2
                        val rad = Math.toRadians((deg - 90.0))
                        val cosVal = cos(rad).toFloat()
                        val sinVal = sin(rad).toFloat()

                        val isMajor = deg % 30 == 0
                        val isMedium = deg % 10 == 0
                        val tickLen = if (isMajor) 22f else if (isMedium) 14f else 8f

                        val x1 = center.x + (radius - tickLen) * cosVal
                        val y1 = center.y + (radius - tickLen) * sinVal
                        val x2 = center.x + radius * cosVal
                        val y2 = center.y + radius * sinVal

                        val tickColor = if (deg == 0) vibrantOrange else if (isMajor) textOnSurface else guideColor

                        drawLine(
                            color = tickColor,
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = if (isMajor) 3.5f else if (isMedium) 2.2f else 1.2f,
                            cap = StrokeCap.Round
                        )
                    }

                    // Cardinal Directions (N, E, S, W)
                    val cardinalLabels = listOf("N" to 0, "E" to 90, "S" to 180, "W" to 270)
                    for ((label, deg) in cardinalLabels) {
                        val rad = Math.toRadians((deg - 90.0))
                        val textX = center.x + (radius - 42f) * cos(rad).toFloat()
                        val textY = center.y + (radius - 42f) * sin(rad).toFloat()

                        val labelColor = if (label == "N") vibrantOrange else textOnSurface

                        val layoutResult = textMeasurer.measure(
                            text = label,
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = labelColor
                            )
                        )
                        drawText(
                            textLayoutResult = layoutResult,
                            topLeft = Offset(textX - layoutResult.size.width / 2f, textY - layoutResult.size.height / 2f)
                        )
                    }
                }

                // DOWNWARD POINTING INDEX ARROW IN BRAND SIGNAL ORANGE (FLUSH ON TOP OF COMPASS CIRCLE)
                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                        .width(18.dp)
                        .height(18.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val arrowPath = Path().apply {
                        moveTo(w / 2f, h) // Downward sharp tip touching top of compass circle
                        lineTo(0f, 0f)    // Top-left
                        lineTo(w, 0f)     // Top-right
                        close()
                    }
                    drawPath(path = arrowPath, color = vibrantOrange)
                }

                // CENTER HEADING READOUT (LARGE DEGREE NUMBER + CARDINAL TEXT AT DIAL CENTER)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.0f", currentHeadingDeg),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 68.sp),
                            fontWeight = FontWeight.Black,
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            letterSpacing = (-2).sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "°",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            color = vibrantOrange,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = cardinalDirection,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = vibrantOrange,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
