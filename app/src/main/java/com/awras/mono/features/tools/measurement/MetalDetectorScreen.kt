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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awras.mono.ui.components.DatumToolTopBar
import com.awras.mono.ui.theme.LocalTactileFeedback
import compose.icons.TablerIcons
import compose.icons.tablericons.ToggleLeft
import java.util.Locale
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetalDetectorScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    // Magnetometer Field Sensor State
    var rawFluxMagnitude by remember { mutableFloatStateOf(45.0f) }
    var baselineFlux by remember { mutableFloatStateOf(45.0f) }
    var isCalibrated by remember { mutableStateOf(false) }

    // Register Magnetic Field Sensor
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    val bx = event.values[0]
                    val by = event.values[1]
                    val bz = event.values[2]

                    val mag = sqrt((bx * bx + by * by + bz * bz).toDouble()).toFloat()
                    rawFluxMagnitude = mag

                    // Calibrate initial ambient baseline automatically if not calibrated
                    if (!isCalibrated && mag > 0f) {
                        baselineFlux = mag
                        isCalibrated = true
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        magSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Calculated Relative Metal Detection Strength
    val relativeFlux = max(0f, rawFluxMagnitude - baselineFlux)
    val smoothRelativeFlux by animateFloatAsState(
        targetValue = relativeFlux,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "SmoothFlux"
    )

    // Metal Proximity Status Category
    val (statusText, statusColor) = when {
        smoothRelativeFlux < 10f -> "NO METAL DETECTED" to MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        smoothRelativeFlux < 30f -> "WEAK METAL PROXIMITY" to vibrantOrange.copy(alpha = 0.8f)
        smoothRelativeFlux < 70f -> "FERROMAGNETIC OBJECT NEARBY" to vibrantOrange
        else -> "STRONG METAL DETECTED!" to vibrantOrange
    }

    // Haptic geiger feedback when metal is near
    var lastHapticTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(smoothRelativeFlux) {
        if (smoothRelativeFlux > 15f) {
            val now = System.currentTimeMillis()
            val interval = (300f - (smoothRelativeFlux * 2.5f)).coerceIn(50f, 300f).toLong()
            if (now - lastHapticTime > interval) {
                haptics.tick()
                lastHapticTime = now
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Metal Detector",
                description = "Ferromagnetic Flux Sensor",
                icon = TablerIcons.ToggleLeft,
                onBackClick = onBackClick,
                rightActionIcon = Icons.Default.FilterCenterFocus,
                onRightActionClick = {
                    haptics.click()
                    baselineFlux = rawFluxMagnitude
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
                // 1. TOP PROMINENT MAGNETIC FLUX READOUT
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f", rawFluxMagnitude),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 58.sp),
                            fontWeight = FontWeight.Black,
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            letterSpacing = (-1.5).sp,
                            color = if (smoothRelativeFlux > 30f) vibrantOrange else MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "μT",
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
                            text = String.format(Locale.US, "RELATIVE: +%.1f μT", smoothRelativeFlux),
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
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = statusColor,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. RADIAL MAGNETIC FLUX RADAR CANVAS
                val flatSurfaceBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                val guideColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                val defaultNodeColor = MaterialTheme.colorScheme.onBackground
                val nodeColor = if (smoothRelativeFlux > 20f) vibrantOrange else defaultNodeColor

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
                        val outerRadius = size.width / 2f

                        // Static Concentric Target Rings
                        for (i in 1..4) {
                            val r = (outerRadius / 4f) * i
                            drawCircle(
                                color = guideColor,
                                radius = r,
                                center = center,
                                style = Stroke(width = 1f)
                            )
                        }

                        // Expanding Dynamic Magnetic Field Pulse Ring based on metal proximity
                        val pulseFactor = (smoothRelativeFlux / 100f).coerceIn(0f, 1f)
                        val activeRadius = (outerRadius * 0.25f) + (outerRadius * 0.70f * pulseFactor)

                        drawCircle(
                            color = vibrantOrange.copy(alpha = 0.20f),
                            radius = activeRadius,
                            center = center
                        )

                        drawCircle(
                            color = vibrantOrange,
                            radius = activeRadius,
                            center = center,
                            style = Stroke(width = 2.5f)
                        )

                        // Center Magnetic Node
                        drawCircle(
                            color = nodeColor,
                            radius = 10f,
                            center = center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
