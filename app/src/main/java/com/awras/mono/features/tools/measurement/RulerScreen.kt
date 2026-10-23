package com.awras.mono.features.tools.measurement

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.awras.mono.ui.components.AppBarButton
import com.awras.mono.ui.components.DatumToolTopBar
import com.awras.mono.ui.theme.LocalTactileFeedback
import androidx.compose.ui.draw.alpha
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowLeft
import compose.icons.tablericons.Ruler
import compose.icons.tablericons.X
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class RulerUnit(val symbol: String, val displayName: String) {
    CM("cm", "Metric (cm / mm)"),
    INCH("in", "Imperial (in)")
}

enum class RulerMode {
    MEASURING,
    CALIBRATING
}

private const val PREFS_NAME = "mono_ruler_settings"
private const val KEY_CALIBRATION_FACTOR = "calibration_factor"
private const val CREDIT_CARD_HEIGHT_MM = 85.60f // Standard ISO/IEC 7810 ID-1 vertical height

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulerScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    // Persistent calibration factor (1.0 = native DPI)
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var calibrationFactor by remember {
        mutableFloatStateOf(prefs.getFloat(KEY_CALIBRATION_FACTOR, 1.0f))
    }

    // Physical screen DPI specs
    val displayMetrics = context.resources.displayMetrics
    val rawYdpi = if (displayMetrics.ydpi > 0) displayMetrics.ydpi else displayMetrics.xdpi
    val nativePixelsPerMm = (if (rawYdpi > 0) rawYdpi else 160f) / 25.4f

    // Save calibration factor
    fun saveCalibration(newFactor: Float) {
        calibrationFactor = newFactor.coerceIn(0.60f, 1.40f)
        prefs.edit { putFloat(KEY_CALIBRATION_FACTOR, calibrationFactor) }
    }

    // Effective pixels per unit along vertical Y-axis
    val effectiveYdpi = (if (rawYdpi > 0) rawYdpi else 160f) * calibrationFactor
    val pixelsPerInch = effectiveYdpi
    val pixelsPerMm = pixelsPerInch / 25.4f

    // Screen state modes
    var rulerMode by remember { mutableStateOf(RulerMode.MEASURING) }
    var selectedUnit by remember { mutableStateOf(RulerUnit.CM) }
    var isLocked by remember { mutableStateOf(false) }

    // Caliper Y-positions in pixels from top (y = 0)
    var caliperStartYPx by remember { mutableFloatStateOf(250f) }
    var caliperEndYPx by remember { mutableFloatStateOf(750f) }

    // Calculated vertical measurement in measuring mode
    val measuredPx = abs(caliperEndYPx - caliperStartYPx)
    val measuredMm = measuredPx / pixelsPerMm
    val measuredCm = measuredMm / 10f
    val measuredInches = measuredPx / pixelsPerInch

    // Calibration calculation when in CALIBRATING mode:
    // User aligns Caliper A & B to their physical Credit Card's top and bottom edges (85.60 mm)
    val liveCalibratedPixelsPerMm = measuredPx / CREDIT_CARD_HEIGHT_MM
    val liveCalibrationFactor = liveCalibratedPixelsPerMm / nativePixelsPerMm

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = if (rulerMode == RulerMode.MEASURING) "Onscreen Ruler" else "Card Calibration",
                description = if (rulerMode == RulerMode.MEASURING) "Precision cm/inch ruler" else "Align handles to card edges",
                icon = TablerIcons.Ruler,
                onBackClick = {
                    if (rulerMode == RulerMode.CALIBRATING) {
                        rulerMode = RulerMode.MEASURING
                    } else {
                        onBackClick()
                    }
                },
                rightActionIcon = if (rulerMode == RulerMode.MEASURING) Icons.Default.Tune else TablerIcons.X,
                onRightActionClick = {
                    haptics.click()
                    if (rulerMode == RulerMode.MEASURING) {
                        rulerMode = RulerMode.CALIBRATING
                    } else {
                        rulerMode = RulerMode.MEASURING
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
            // 1. VERTICAL RULER CANVAS WITH DIAGONAL EDGE TICKS
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val heightPx = with(density) { maxHeight.toPx() }

                val activeStartY = caliperStartYPx.coerceIn(40f, heightPx - 40f)
                val activeEndY = caliperEndYPx.coerceIn(40f, heightPx - 40f)

                val tickColor = MaterialTheme.colorScheme.onBackground
                val accentColor = if (rulerMode == RulerMode.MEASURING) {
                    vibrantOrange
                } else {
                    vibrantOrange
                }
                val regionFillColor = accentColor.copy(alpha = 0.12f)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val minY = minOf(activeStartY, activeEndY)
                    val maxY = maxOf(activeStartY, activeEndY)

                    // Measurement region translucent fill
                    drawRect(
                        color = regionFillColor,
                        topLeft = Offset(0f, minY),
                        size = Size(size.width, maxY - minY)
                    )

                    // Draw Vertical Edge Diagonal Ticks
                    if (selectedUnit == RulerUnit.CM) {
                        drawVerticalDiagonalMetricTicks(
                            heightPx = size.height,
                            widthPx = size.width,
                            pixelsPerMm = pixelsPerMm,
                            tickColor = tickColor
                        )
                    } else {
                        drawVerticalDiagonalImperialTicks(
                            heightPx = size.height,
                            widthPx = size.width,
                            pixelsPerInch = pixelsPerInch,
                            tickColor = tickColor
                        )
                    }

                    // Caliper Horizontal Lines
                    drawLine(
                        color = accentColor,
                        start = Offset(0f, activeStartY),
                        end = Offset(size.width, activeStartY),
                        strokeWidth = 3.5f
                    )

                    drawLine(
                        color = accentColor,
                        start = Offset(0f, activeEndY),
                        end = Offset(size.width, activeEndY),
                        strokeWidth = 3.5f
                    )

                    // Vertical Center Guideline
                    val midX = size.width / 2f
                    drawLine(
                        color = accentColor,
                        start = Offset(midX, minY),
                        end = Offset(midX, maxY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                }

                // 2. CREDIT CARD VISUAL REFERENCE IN CALIBRATING MODE
                if (rulerMode == RulerMode.CALIBRATING) {
                    val minY = minOf(activeStartY, activeEndY)
                    val maxY = maxOf(activeStartY, activeEndY)
                    val cardHeightPx = maxY - minY
                    val cardWidthPx = cardHeightPx * (53.98f / 85.60f)

                    val cardHeightDp = with(density) { cardHeightPx.toDp() }
                    val cardWidthDp = with(density) { cardWidthPx.toDp() }
                    val topMarginDp = with(density) { minY.toDp() }

                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Surface(
                            modifier = Modifier
                                .padding(top = topMarginDp)
                                .width(cardWidthDp.coerceAtLeast(100.dp))
                                .height(cardHeightDp.coerceAtLeast(120.dp)),
                            shape = ContinuousRoundedRectangle(16.dp),
                            color = vibrantOrange.copy(alpha = 0.12f),
                            border = BorderStroke(2.dp, vibrantOrange)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CreditCard,
                                        contentDescription = null,
                                        tint = vibrantOrange
                                    )
                                    Text(
                                        text = "CREDIT CARD",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = vibrantOrange,
                                        fontFamily = MaterialTheme.typography.labelSmall.fontFamily
                                    )
                                }

                                Text(
                                    text = "85.60 mm",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = MaterialTheme.typography.labelSmall.fontFamily
                                )

                                Text(
                                    text = "Align top & bottom edges",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // Interactive Horizontal Caliper Handles (Start Y & End Y)
                if (!isLocked || rulerMode == RulerMode.CALIBRATING) {
                    VerticalCaliperHandle(
                        positionYPx = activeStartY,
                        label = if (rulerMode == RulerMode.MEASURING) "A" else "TOP",
                        accentColor = accentColor,
                        onDrag = { deltaY ->
                            val oldCm = (caliperStartYPx / (pixelsPerMm * 10f)).toInt()
                            caliperStartYPx = (caliperStartYPx + deltaY).coerceIn(20f, heightPx - 20f)
                            val newCm = (caliperStartYPx / (pixelsPerMm * 10f)).toInt()
                            if (oldCm != newCm) haptics.tick()
                        }
                    )

                    VerticalCaliperHandle(
                        positionYPx = activeEndY,
                        label = if (rulerMode == RulerMode.MEASURING) "B" else "BOTTOM",
                        accentColor = accentColor,
                        onDrag = { deltaY ->
                            val oldCm = (caliperEndYPx / (pixelsPerMm * 10f)).toInt()
                            caliperEndYPx = (caliperEndYPx + deltaY).coerceIn(20f, heightPx - 20f)
                            val newCm = (caliperEndYPx / (pixelsPerMm * 10f)).toInt()
                            if (oldCm != newCm) haptics.tick()
                        }
                    )
                }
            }

            // 3. PROMINENT FLOATING NUMBERS AT TOP CENTER (NO CONTAINER CARD / SURFACE)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (rulerMode == RulerMode.MEASURING) {
                    // Large Prominent Number (Floating directly without background card)
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (selectedUnit == RulerUnit.CM) {
                                String.format(Locale.US, "%.2f", measuredCm)
                            } else {
                                String.format(Locale.US, "%.2f", measuredInches)
                            },
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 54.sp),
                            fontWeight = FontWeight.Black,
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            letterSpacing = (-1.5).sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = selectedUnit.symbol,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            color = vibrantOrange,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Tiny Precision Subtitle
                    Text(
                        text = if (selectedUnit == RulerUnit.CM) {
                            String.format(Locale.US, "%.1f mm  •  ±0.1 mm PRECISION", measuredMm)
                        } else {
                            "${formatFractionalInches(measuredInches)}  •  ±1/16 in PRECISION"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f),
                        letterSpacing = 0.5.sp
                    )
                } else {
                    // Calibrating Mode Floating Header
                    Text(
                        text = "85.60 mm",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp),
                        fontWeight = FontWeight.Black,
                        fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                        color = vibrantOrange
                    )
                    Text(
                        text = String.format(Locale.US, "SCALE MULTIPLIER: %.3fx", liveCalibrationFactor),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // 4. FLOATING BOTTOM-RIGHT UNIT SWITCHER (cm / in)
            if (rulerMode == RulerMode.MEASURING) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 28.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = ContinuousCapsule
                        )
                ) {
                    FloatingUnitSwitcher(
                        selectedUnit = selectedUnit,
                        onUnitSelected = { unit ->
                            haptics.tick()
                            selectedUnit = unit
                        }
                    )
                }

                // Bottom-Left Subtle Lock Toggle Button (Icon only, low alpha)
                IconButton(
                    onClick = {
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

            // 5. APPLY CALIBRATION BUTTON AT THE BOTTOM IN CALIBRATING MODE
            if (rulerMode == RulerMode.CALIBRATING) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            saveCalibration(liveCalibrationFactor)
                            haptics.success()
                            rulerMode = RulerMode.MEASURING
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = ContinuousCapsule,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = vibrantOrange,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Text(
                                text = "APPLY CALIBRATION",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            haptics.click()
                            rulerMode = RulerMode.MEASURING
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = ContinuousCapsule,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "CANCEL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Floating Unit Switcher in Bottom Right Corner
 */
@Composable
private fun FloatingUnitSwitcher(
    selectedUnit: RulerUnit,
    onUnitSelected: (RulerUnit) -> Unit
) {
    Surface(
        modifier = Modifier.clip(ContinuousCapsule),
        shape = ContinuousCapsule,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RulerUnit.entries.forEach { unit ->
                val isSelected = selectedUnit == unit
                val backgroundColor = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                }
                val contentColor = if (isSelected) {
                    Color.White
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }

                Box(
                    modifier = Modifier
                        .clip(ContinuousCapsule)
                        .background(backgroundColor)
                        .clickable { onUnitSelected(unit) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = unit.symbol.uppercase(Locale.US),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = contentColor,
                        fontFamily = MaterialTheme.typography.labelSmall.fontFamily
                    )
                }
            }
        }
    }
}

/**
 * Vertical Caliper Drag Handle (Horizontal bar extending across with end knobs)
 */
@Composable
private fun VerticalCaliperHandle(
    positionYPx: Float,
    label: String,
    accentColor: Color,
    onDrag: (Float) -> Unit
) {
    val density = LocalDensity.current
    val positionYDp = with(density) { positionYPx.toDp() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = positionYDp - 20.dp)
            .height(40.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.y)
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Left Knob
        Surface(
            modifier = Modifier
                .padding(start = 8.dp)
                .height(32.dp)
                .shadow(elevation = 6.dp, shape = ContinuousCapsule),
            shape = ContinuousCapsule,
            color = accentColor,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Vertical Diagonal Metric Ticks along Screen Side Edges:
 * Left Edge: Top-to-Bottom (0 at top)
 * Right Edge: Bottom-to-Top (0 at bottom)
 */
private fun DrawScope.drawVerticalDiagonalMetricTicks(
    heightPx: Float,
    widthPx: Float,
    pixelsPerMm: Float,
    tickColor: Color
) {
    val textMeasurer = VerticalTextMeasurer()

    // 1. Left Edge Scale (Top to Bottom)
    var mmLeft = 0
    while (true) {
        val yLeft = mmLeft * pixelsPerMm
        if (yLeft > heightPx) break

        val isCm = (mmLeft % 10 == 0)
        val isHalfCm = (mmLeft % 5 == 0) && !isCm

        val dx = when {
            isCm -> 32f
            isHalfCm -> 20f
            else -> 12f
        }
        val dy = 6f // Diagonal slant

        val strokeWidth = if (isCm) 2.5f else 1.5f
        val alpha = if (isCm) 0.9f else 0.45f
        val color = tickColor.copy(alpha = alpha)

        // Left Edge Tick (slanted inwards-downward)
        drawLine(
            color = color,
            start = Offset(0f, yLeft),
            end = Offset(dx, yLeft + dy),
            strokeWidth = strokeWidth
        )

        if (isCm && mmLeft > 0) {
            val cmVal = mmLeft / 10
            textMeasurer.drawTextLeft(
                drawScope = this,
                text = cmVal.toString(),
                x = dx + 12f,
                y = yLeft,
                color = tickColor
            )
        }

        mmLeft++
    }

    // 2. Right Edge Scale (Bottom to Top)
    var mmRight = 0
    while (true) {
        val yRight = heightPx - (mmRight * pixelsPerMm)
        if (yRight < 0f) break

        val isCm = (mmRight % 10 == 0)
        val isHalfCm = (mmRight % 5 == 0) && !isCm

        val dx = when {
            isCm -> 32f
            isHalfCm -> 20f
            else -> 12f
        }
        val dy = 6f // Diagonal slant

        val strokeWidth = if (isCm) 2.5f else 1.5f
        val alpha = if (isCm) 0.9f else 0.45f
        val color = tickColor.copy(alpha = alpha)

        // Right Edge Tick (slanted inwards-upward)
        drawLine(
            color = color,
            start = Offset(widthPx, yRight),
            end = Offset(widthPx - dx, yRight - dy),
            strokeWidth = strokeWidth
        )

        if (isCm && mmRight > 0) {
            val cmVal = mmRight / 10
            textMeasurer.drawTextRight(
                drawScope = this,
                text = cmVal.toString(),
                x = widthPx - dx - 12f,
                y = yRight,
                color = tickColor
            )
        }

        mmRight++
    }
}

/**
 * Vertical Diagonal Imperial Ticks along Screen Side Edges:
 * Left Edge: Top-to-Bottom (0 at top)
 * Right Edge: Bottom-to-Top (0 at bottom)
 */
private fun DrawScope.drawVerticalDiagonalImperialTicks(
    heightPx: Float,
    widthPx: Float,
    pixelsPerInch: Float,
    tickColor: Color
) {
    val pixelsPer16th = pixelsPerInch / 16f
    val textMeasurer = VerticalTextMeasurer()

    // 1. Left Edge Scale (Top to Bottom)
    var sixteenthLeft = 0
    while (true) {
        val yLeft = sixteenthLeft * pixelsPer16th
        if (yLeft > heightPx) break

        val isFullInch = (sixteenthLeft % 16 == 0)
        val isHalfInch = (sixteenthLeft % 8 == 0) && !isFullInch
        val isQuarterInch = (sixteenthLeft % 4 == 0) && !isHalfInch && !isFullInch
        val isEighthInch = (sixteenthLeft % 2 == 0) && !isQuarterInch && !isHalfInch && !isFullInch

        val dx = when {
            isFullInch -> 34f
            isHalfInch -> 24f
            isQuarterInch -> 18f
            isEighthInch -> 14f
            else -> 10f
        }
        val dy = 6f

        val strokeWidth = if (isFullInch) 2.5f else 1.5f
        val alpha = if (isFullInch) 0.9f else 0.45f
        val color = tickColor.copy(alpha = alpha)

        // Left Edge Tick
        drawLine(
            color = color,
            start = Offset(0f, yLeft),
            end = Offset(dx, yLeft + dy),
            strokeWidth = strokeWidth
        )

        if (isFullInch && sixteenthLeft > 0) {
            val inchVal = sixteenthLeft / 16
            textMeasurer.drawTextLeft(
                drawScope = this,
                text = "$inchVal\"",
                x = dx + 12f,
                y = yLeft,
                color = tickColor
            )
        }

        sixteenthLeft++
    }

    // 2. Right Edge Scale (Bottom to Top)
    var sixteenthRight = 0
    while (true) {
        val yRight = heightPx - (sixteenthRight * pixelsPer16th)
        if (yRight < 0f) break

        val isFullInch = (sixteenthRight % 16 == 0)
        val isHalfInch = (sixteenthRight % 8 == 0) && !isFullInch
        val isQuarterInch = (sixteenthRight % 4 == 0) && !isHalfInch && !isFullInch
        val isEighthInch = (sixteenthRight % 2 == 0) && !isQuarterInch && !isHalfInch && !isFullInch

        val dx = when {
            isFullInch -> 34f
            isHalfInch -> 24f
            isQuarterInch -> 18f
            isEighthInch -> 14f
            else -> 10f
        }
        val dy = 6f

        val strokeWidth = if (isFullInch) 2.5f else 1.5f
        val alpha = if (isFullInch) 0.9f else 0.45f
        val color = tickColor.copy(alpha = alpha)

        // Right Edge Tick (slanted inwards-upward)
        drawLine(
            color = color,
            start = Offset(widthPx, yRight),
            end = Offset(widthPx - dx, yRight - dy),
            strokeWidth = strokeWidth
        )

        if (isFullInch && sixteenthRight > 0) {
            val inchVal = sixteenthRight / 16
            textMeasurer.drawTextRight(
                drawScope = this,
                text = "$inchVal\"",
                x = widthPx - dx - 12f,
                y = yRight,
                color = tickColor
            )
        }

        sixteenthRight++
    }
}

private class VerticalTextMeasurer {
    fun drawTextLeft(
        drawScope: DrawScope,
        text: String,
        x: Float,
        y: Float,
        color: Color
    ) {
        val paint = Paint().apply {
            this.color = color.toArgb()
            this.textSize = 26f
            this.typeface = Typeface.MONOSPACE
            this.textAlign = Paint.Align.LEFT
            this.isAntiAlias = true
        }
        drawScope.drawContext.canvas.nativeCanvas.drawText(text, x, y + 8f, paint)
    }

    fun drawTextRight(
        drawScope: DrawScope,
        text: String,
        x: Float,
        y: Float,
        color: Color
    ) {
        val paint = Paint().apply {
            this.color = color.toArgb()
            this.textSize = 26f
            this.typeface = Typeface.MONOSPACE
            this.textAlign = Paint.Align.RIGHT
            this.isAntiAlias = true
        }
        drawScope.drawContext.canvas.nativeCanvas.drawText(text, x, y + 8f, paint)
    }
}

private fun formatFractionalInches(inches: Float): String {
    val wholeInches = inches.toInt()
    val remainder = inches - wholeInches
    val sixteenths = (remainder * 16).roundToInt()

    if (sixteenths == 0) return "$wholeInches in"
    if (sixteenths == 16) return "${wholeInches + 1} in"

    var num = sixteenths
    var den = 16
    while (num % 2 == 0 && den % 2 == 0) {
        num /= 2
        den /= 2
    }

    return if (wholeInches > 0) {
        "$wholeInches $num/$den in"
    } else {
        "$num/$den in"
    }
}
