package com.awras.mono.features.tools.travel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awras.mono.ui.components.DatumToolTopBar
import com.awras.mono.ui.theme.LocalTactileFeedback
import com.kyant.capsule.ContinuousRoundedRectangle
import compose.icons.TablerIcons
import compose.icons.tablericons.Sun
import java.util.Calendar
import java.util.Locale
import kotlin.math.*

private val GoldenGold = Color(0xFFFFB300)
private val BlueHourIndigo = Color(0xFF3F51B5)

data class MoonPhaseData(
    val phaseName: String,
    val illuminationPct: Int,
    val phaseFraction: Float,
    val isWaxing: Boolean
)

fun calculateMoonPhase(): MoonPhaseData {
    val now = Calendar.getInstance()
    val year = now.get(Calendar.YEAR)
    val month = now.get(Calendar.MONTH) + 1
    val day = now.get(Calendar.DAY_OF_MONTH)

    val a = (14 - month) / 12
    val y = year + 4800 - a
    val m = month + 12 * a - 3
    val julianDay = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045.5

    val synodicMonth = 29.53058867
    val daysSinceNew = (julianDay - 2460320.9) % synodicMonth
    val phaseFraction = ((if (daysSinceNew < 0) daysSinceNew + synodicMonth else daysSinceNew) / synodicMonth).toFloat()

    val illumPct = ((1.0 - cos(2.0 * PI * phaseFraction)) / 2.0 * 100.0).roundToInt().coerceIn(0, 100)
    val isWaxing = phaseFraction < 0.5f

    val name = when {
        phaseFraction in 0.97f..1.00f || phaseFraction in 0.00f..0.03f -> "New Moon"
        phaseFraction in 0.03f..0.22f -> "Waxing Crescent"
        phaseFraction in 0.22f..0.28f -> "First Quarter"
        phaseFraction in 0.28f..0.47f -> "Waxing Gibbous"
        phaseFraction in 0.47f..0.53f -> "Full Moon"
        phaseFraction in 0.53f..0.72f -> "Waning Gibbous"
        phaseFraction in 0.72f..0.78f -> "Last Quarter"
        else -> "Waning Crescent"
    }

    return MoonPhaseData(name, illumPct, phaseFraction, isWaxing)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolarTrackerScreen(
    onBackClick: () -> Unit
) {
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    val scrollState = rememberScrollState()

    // Solar Calculation State
    val now = Calendar.getInstance()
    val hourOfDay = now.get(Calendar.HOUR_OF_DAY)
    val minute = now.get(Calendar.MINUTE)

    val dayOfYear = now.get(Calendar.DAY_OF_YEAR)
    val declination = 23.45 * sin(Math.toRadians((360.0 / 365.0) * (dayOfYear - 81)))
    val timeMinutes = hourOfDay * 60 + minute
    val solarTimeAngle = ((timeMinutes - 720.0) / 4.0)
    val elevationAngle = (90.0 - abs(declination) - abs(solarTimeAngle) * 0.45).coerceIn(-60.0, 85.0)
    val zenithAngle = 90.0 - elevationAngle
    val azimuthAngle = ((solarTimeAngle + 180.0) % 360.0)

    val moonData = remember { calculateMoonPhase() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Solar Tracker",
                description = "Sun, Moon & Golden Hour Inspector",
                icon = TablerIcons.Sun,
                onBackClick = onBackClick,
                rightActionIcon = Icons.Default.Refresh,
                onRightActionClick = {
                    haptics.click()
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. PROMINENT SOLAR POSITION READOUT
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%+.1f°", elevationAngle),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 60.sp),
                        fontWeight = FontWeight.Black,
                        fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                        letterSpacing = (-2).sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "ELEVATION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = vibrantOrange,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(Locale.US, "AZIMUTH: %.1f°  •  ZENITH: %.1f°", azimuthAngle, zenithAngle),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // 2. SKY DOME SOLAR PATH & GOLDEN HOUR ARC CANVAS
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ContinuousRoundedRectangle(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Solar Trajectory",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val horizonY = h * 0.70f
                            val arcH = horizonY - 16.dp.toPx()

                            // Horizon Line
                            drawLine(
                                color = Color.White.copy(alpha = 0.2f),
                                start = Offset(0f, horizonY),
                                end = Offset(w, horizonY),
                                strokeWidth = 1.5f
                            )

                            // Solar Path Curve
                            val path = Path()
                            val numSteps = 100
                            for (i in 0..numSteps) {
                                val t = i / numSteps.toFloat()
                                val x = t * w
                                val y = horizonY - sin(t * Math.PI.toFloat()) * arcH
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }

                            // Golden Hour Band Fill
                            val fillPath = Path().apply {
                                addPath(path)
                                lineTo(w, horizonY)
                                lineTo(0f, horizonY)
                                close()
                            }
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    listOf(GoldenGold.copy(alpha = 0.25f), Color.Transparent)
                                )
                            )

                            drawPath(
                                path = path,
                                color = GoldenGold,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Sun Position Node on Curve
                            val progress = (timeMinutes / 1440.0).toFloat()
                            val sunX = progress * w
                            val sunY = horizonY - sin(progress * Math.PI.toFloat()) * arcH

                            drawCircle(color = Color.White, radius = 10.dp.toPx(), center = Offset(sunX, sunY))
                            drawCircle(color = GoldenGold, radius = 7.dp.toPx(), center = Offset(sunX, sunY))
                        }
                    }
                }
            }

            // 3. GOLDEN HOUR & PHOTOGRAPHY TIMING CARD
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ContinuousRoundedRectangle(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = GoldenGold
                        )
                        Text(
                            text = "PHOTOGRAPHY LIGHTING WINDOWS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = GoldenGold,
                            letterSpacing = 1.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TimingPill(title = "Morning Golden Hour", timeWindow = "06:15 AM - 07:00 AM", color = GoldenGold)
                        TimingPill(title = "Evening Golden Hour", timeWindow = "05:45 PM - 06:30 PM", color = GoldenGold)
                    }
                }
            }

            // 4. ACCURATE LUNAR PHASE & ILLUMINATION CARD
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ContinuousRoundedRectangle(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "MOON PHASE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = BlueHourIndigo,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = moonData.phaseName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${moonData.illuminationPct}% Illuminated",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Canvas(modifier = Modifier.size(54.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val r = size.width / 2f

                        // Dark Moon Base
                        drawCircle(color = Color(0xFF2A394A), radius = r, center = center)

                        // Dynamic Illuminated Moon Arc
                        val startAngle = if (moonData.isWaxing) -90f else 90f
                        val sweepAngle = ((moonData.illuminationPct / 100f) * 180f).coerceAtLeast(10f)

                        drawArc(
                            color = Color(0xFFE8ECEF),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TimingPill(title: String, timeWindow: String, color: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = timeWindow,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
