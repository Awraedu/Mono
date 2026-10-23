package com.awras.mono.features.tools.vision

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awras.mono.ui.components.DatumCameraView
import com.awras.mono.ui.components.DatumToolTopBar
import com.awras.mono.ui.theme.LocalTactileFeedback
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import compose.icons.TablerIcons
import compose.icons.tablericons.Stack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemCounterScreen(
    onBackClick: () -> Unit
) {
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    // Live detected items set
    val detectedItems = remember { mutableStateMapOf<String, String>() } // value -> format
    var isFlashOn by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Item Counter",
                description = "Multi-Barcode & QR Object Tracking Counter",
                icon = TablerIcons.Stack,
                onBackClick = onBackClick,
                rightActionIcon = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                onRightActionClick = {
                    haptics.click()
                    isFlashOn = !isFlashOn
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. LIVE COUNT SUMMARY CARD AT TOP (NO SHADOW)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ContinuousRoundedRectangle(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = null,
                                tint = vibrantOrange
                            )
                            Text(
                                text = "LIVE OBJECT COUNTER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = vibrantOrange,
                                letterSpacing = 1.sp
                            )
                        }

                        Button(
                            onClick = {
                                haptics.click()
                                detectedItems.clear()
                            },
                            modifier = Modifier.height(36.dp),
                            shape = ContinuousCapsule,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text("RESET", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${detectedItems.size}",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp),
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "items counted",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = vibrantOrange,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    if (detectedItems.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Column(
                            modifier = Modifier
                                .heightIn(max = 120.dp)
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            detectedItems.forEach { (value, format) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = value,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = format,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = vibrantOrange
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 2. DATUM CAMERA VIEWPORT AT BOTTOM (HEIGHT 500.DP)
            DatumCameraView(
                isCameraEnabled = true,
                isFlashOn = isFlashOn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp),
                cornerRadius = 28.dp,
                onBarcodeScanned = { raw ->
                    if (!detectedItems.containsKey(raw)) {
                        haptics.tick()
                        detectedItems[raw] = "Barcode / QR"
                    }
                }
            ) {
                // Viewfinder Framing Box
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val boxW = w * 0.85f
                    val boxH = h * 0.75f
                    val left = (w - boxW) / 2f
                    val top = (h - boxH) / 2f
                    val right = left + boxW
                    val bottom = top + boxH
                    val cornerLen = 24f

                    val dimColor = Color.Black.copy(alpha = 0.35f)
                    drawRect(dimColor, Offset(0f, 0f), Size(w, top))
                    drawRect(dimColor, Offset(0f, bottom), Size(w, h - bottom))
                    drawRect(dimColor, Offset(0f, top), Size(left, boxH))
                    drawRect(dimColor, Offset(right, top), Size(w - right, boxH))

                    // Reticle Frame Border
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.25f),
                        topLeft = Offset(left, top),
                        size = Size(boxW, boxH),
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                        style = Stroke(width = 1.5f)
                    )

                    // Corner Accents
                    val strokeW = 4f
                    drawLine(vibrantOrange, Offset(left, top), Offset(left + cornerLen, top), strokeWidth = strokeW, cap = StrokeCap.Round)
                    drawLine(vibrantOrange, Offset(left, top), Offset(left, top + cornerLen), strokeWidth = strokeW, cap = StrokeCap.Round)
                    drawLine(vibrantOrange, Offset(right, top), Offset(right - cornerLen, top), strokeWidth = strokeW, cap = StrokeCap.Round)
                    drawLine(vibrantOrange, Offset(right, top), Offset(right, top + cornerLen), strokeWidth = strokeW, cap = StrokeCap.Round)
                    drawLine(vibrantOrange, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeWidth = strokeW, cap = StrokeCap.Round)
                    drawLine(vibrantOrange, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeWidth = strokeW, cap = StrokeCap.Round)
                    drawLine(vibrantOrange, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeWidth = strokeW, cap = StrokeCap.Round)
                    drawLine(vibrantOrange, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeWidth = strokeW, cap = StrokeCap.Round)
                }

                // Inside Viewport: Bottom-Right Flashlight Toggle Button
                IconButton(
                    onClick = {
                        haptics.click()
                        isFlashOn = !isFlashOn
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 20.dp)
                        .alpha(if (isFlashOn) 0.95f else 0.60f)
                ) {
                    Icon(
                        imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flashlight Toggle",
                        tint = if (isFlashOn) vibrantOrange else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
