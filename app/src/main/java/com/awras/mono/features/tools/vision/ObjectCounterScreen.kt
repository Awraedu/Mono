package com.awras.mono.features.tools.vision

import android.graphics.Rect
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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

class SmoothedTrackedObject(
    var id: Int,
    var label: String,
    var left: Float,
    var top: Float,
    var right: Float,
    var bottom: Float,
    var timeSinceUpdate: Int = 0
) {
    fun update(newLeft: Float, newTop: Float, newRight: Float, newBottom: Float, alpha: Float = 0.35f) {
        // Exponential Moving Average (EMA) smoothing for zero bounding box jitter
        left += alpha * (newLeft - left)
        top += alpha * (newTop - top)
        right += alpha * (newRight - right)
        bottom += alpha * (newBottom - bottom)
        timeSinceUpdate = 0
    }
}

private fun calculateIoU(r1: Rect, r2: SmoothedTrackedObject): Float {
    val xA = maxOf(r1.left.toFloat(), r2.left)
    val yA = maxOf(r1.top.toFloat(), r2.top)
    val xB = minOf(r1.right.toFloat(), r2.right)
    val yB = minOf(r1.bottom.toFloat(), r2.bottom)

    val interArea = maxOf(0f, xB - xA) * maxOf(0f, yB - yA)
    if (interArea == 0f) return 0f

    val box1Area = (r1.right - r1.left) * (r1.bottom - r1.top)
    val box2Area = (r2.right - r2.left) * (r2.bottom - r2.top)

    return interArea / (box1Area + box2Area - interArea)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectCounterScreen(
    onBackClick: () -> Unit
) {
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    var isFlashOn by remember { mutableStateOf(false) }
    val trackedObjects = remember { mutableStateMapOf<Int, SmoothedTrackedObject>() }
    val nextId = remember { mutableIntStateOf(1) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Object Counter",
                description = "Advanced AI IoU Tracking & EMA Smoothing",
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
            // 1. LIVE COUNT SUMMARY CARD AT TOP
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
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = vibrantOrange
                            )
                            Text(
                                text = "ADVANCED IOU TRACKER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = vibrantOrange,
                                letterSpacing = 1.sp
                            )
                        }

                        Button(
                            onClick = {
                                haptics.click()
                                trackedObjects.clear()
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
                            text = "${trackedObjects.size}",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp),
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "objects tracked",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = vibrantOrange,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 2. DATUM CAMERA VIEWPORT WITH ADVANCED IOU FILTER & SMOOTH BOUNDING BOXES
            DatumCameraView(
                isCameraEnabled = true,
                isFlashOn = isFlashOn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp),
                cornerRadius = 28.dp,
                onObjectsDetected = { rawObjects ->
                    // Age existing objects
                    for (obj in trackedObjects.values) {
                        obj.timeSinceUpdate++
                    }

                    // Match incoming detections via IoU
                    for (raw in rawObjects) {
                        val bounds = raw.boundingBox
                        val label = raw.labels.firstOrNull()?.text ?: "Object"

                        var bestMatchId: Int? = null
                        var bestIoU = 0.3f // Minimum IoU threshold for matching

                        for (tracked in trackedObjects.values) {
                            val iou = calculateIoU(bounds, tracked)
                            if (iou > bestIoU) {
                                bestIoU = iou
                                bestMatchId = tracked.id
                            }
                        }

                        if (bestMatchId != null) {
                            // Update existing tracked object with EMA smoothing
                            trackedObjects[bestMatchId]?.update(
                                bounds.left.toFloat(),
                                bounds.top.toFloat(),
                                bounds.right.toFloat(),
                                bounds.bottom.toFloat()
                            )
                        } else {
                            // Register new tracked object
                            val newId = nextId.intValue++
                            trackedObjects[newId] = SmoothedTrackedObject(
                                id = newId,
                                label = label,
                                left = bounds.left.toFloat(),
                                top = bounds.top.toFloat(),
                                right = bounds.right.toFloat(),
                                bottom = bounds.bottom.toFloat()
                            )
                        }
                    }

                    // Remove stale untracked objects that haven't been seen for 10 frames
                    val staleKeys = trackedObjects.filter { it.value.timeSinceUpdate > 10 }.keys
                    for (key in staleKeys) {
                        trackedObjects.remove(key)
                    }
                }
            ) {
                // Draw Smooth EMA Bounding Boxes on Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasW = size.width
                    val canvasH = size.height

                    for (obj in trackedObjects.values) {
                        val scaleX = canvasW / 1080f
                        val scaleY = canvasH / 1920f

                        val left = obj.left * scaleX
                        val top = obj.top * scaleY
                        val right = obj.right * scaleX
                        val bottom = obj.bottom * scaleY

                        drawRect(
                            color = vibrantOrange,
                            topLeft = Offset(left, top),
                            size = Size(right - left, bottom - top),
                            style = Stroke(width = 3.5f)
                        )
                    }
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
