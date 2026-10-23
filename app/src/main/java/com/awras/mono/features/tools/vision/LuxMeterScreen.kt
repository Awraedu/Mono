package com.awras.mono.features.tools.vision

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import compose.icons.tablericons.Sun
import java.util.Locale
import kotlin.math.*

enum class LightUnit(val symbol: String) {
    LUX("lx"),
    FOOT_CANDLE("fc")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LuxMeterScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    // Light Sensor State
    var rawLux by remember { mutableFloatStateOf(0f) }
    var minLux by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    var maxLux by remember { mutableFloatStateOf(0f) }
    var selectedUnit by remember { mutableStateOf(LightUnit.LUX) }

    // Register Light Sensor
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_LIGHT) {
                    val lux = event.values[0]
                    rawLux = lux
                    if (lux < minLux) minLux = lux
                    if (lux > maxLux) maxLux = lux
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        lightSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Smooth Lux Animation
    val smoothLux by animateFloatAsState(
        targetValue = rawLux,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "SmoothLux"
    )

    // Unit Conversion: 1 Foot-Candle = 10.764 Lux
    val displayValue = if (selectedUnit == LightUnit.LUX) smoothLux else (smoothLux / 10.764f)
    val displayMin = if (minLux == Float.MAX_VALUE) 0f else (if (selectedUnit == LightUnit.LUX) minLux else minLux / 10.764f)
    val displayMax = if (selectedUnit == LightUnit.LUX) maxLux else (maxLux / 10.764f)

    // Recommended Environment Status
    val environmentCategory = when {
        smoothLux < 20f -> "NIGHT / PITCH DARK"
        smoothLux < 100f -> "DIM INDOOR LIGHTING"
        smoothLux < 500f -> "NORMAL OFFICE / LIVING ROOM"
        smoothLux < 2500f -> "BRIGHT STUDIO / OVERCAST DAYLIGHT"
        else -> "DIRECT SUNLIGHT"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Lux Meter",
                description = "Precision Ambient Illuminance Meter",
                icon = TablerIcons.Sun,
                onBackClick = onBackClick,
                rightActionIcon = Icons.Default.Refresh,
                onRightActionClick = {
                    haptics.click()
                    minLux = Float.MAX_VALUE
                    maxLux = 0f
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
                // 1. TOP PROMINENT ILLUMINANCE READOUT
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.0f", displayValue),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 58.sp),
                            fontWeight = FontWeight.Black,
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            letterSpacing = (-1.5).sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = selectedUnit.symbol,
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
                            text = String.format(Locale.US, "MIN: %.0f  •  MAX: %.0f", displayMin, displayMax),
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
                            text = environmentCategory,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = vibrantOrange,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. RADIAL ILLUMINANCE ARC GAUGE CANVAS
                val flatSurfaceBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
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
                        val radius = (size.width / 2f) - 24f

                        // Background Arc (240 degree gauge)
                        drawArc(
                            color = guideColor,
                            startAngle = 150f,
                            sweepAngle = 240f,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2f, radius * 2f),
                            style = Stroke(width = 12f, cap = StrokeCap.Round)
                        )

                        // Logarithmic scale mapping for lux (0 to 10,000 lux)
                        val logLux = log10(max(1f, smoothLux)).coerceIn(0f, 4f)
                        val sweepAngle = (logLux / 4f) * 240f

                        // Active Glowing Illuminance Arc
                        drawArc(
                            color = vibrantOrange,
                            startAngle = 150f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2f, radius * 2f),
                            style = Stroke(width = 14f, cap = StrokeCap.Round)
                        )

                        // Center Sun Node
                        drawCircle(
                            color = vibrantOrange,
                            radius = 16f,
                            center = center
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 6f,
                            center = center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. UNIT SWITCHER PILL (LUX vs FOOT-CANDLE)
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
                        LightUnit.entries.forEach { unit ->
                            val isSelected = selectedUnit == unit
                            val bgColor = if (isSelected) vibrantOrange else Color.Transparent
                            val txtColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

                            Box(
                                modifier = Modifier
                                    .clip(ContinuousCapsule)
                                    .background(bgColor)
                                    .clickable {
                                        haptics.tick()
                                        selectedUnit = unit
                                    }
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${unit.name} (${unit.symbol})",
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

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
