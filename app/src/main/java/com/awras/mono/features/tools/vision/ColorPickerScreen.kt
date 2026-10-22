package com.awras.mono.features.tools.vision

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awras.mono.ui.components.DatumCameraView
import com.awras.mono.ui.components.DatumToolTopBar
import com.awras.mono.ui.theme.LocalTactileFeedback
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import compose.icons.TablerIcons
import compose.icons.tablericons.ColorSwatch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    var sampledColor by remember { mutableStateOf(Color(0xFFFF5700)) }
    var isFlashOn by remember { mutableStateOf(false) }
    var isFrozen by remember { mutableStateOf(false) }

    val hexCode = String.format(
        Locale.US,
        "#%02X%02X%02X",
        (sampledColor.red * 255).toInt(),
        (sampledColor.green * 255).toInt(),
        (sampledColor.blue * 255).toInt()
    )

    val redVal = (sampledColor.red * 255).toInt()
    val greenVal = (sampledColor.green * 255).toInt()
    val blueVal = (sampledColor.blue * 255).toInt()

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Color Hex", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied $text", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Color Picker",
                description = "Real-time Surface Color Inspector",
                icon = TablerIcons.ColorSwatch,
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
            // 1. COLOR SWATCH & BREAKDOWN CARD AT TOP
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ContinuousRoundedRectangle(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = ContinuousRoundedRectangle(12.dp),
                                color = sampledColor,
                                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.5f))
                            ) {}

                            Column {
                                Text(
                                    text = hexCode,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = String.format(Locale.US, "RGB: (%d, %d, %d)", redVal, greenVal, blueVal),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                haptics.click()
                                copyToClipboard(hexCode)
                                haptics.success()
                            },
                            modifier = Modifier.height(44.dp),
                            shape = ContinuousCapsule,
                            colors = ButtonDefaults.buttonColors(containerColor = vibrantOrange)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("COPY", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 2. DATUM CAMERA VIEWPORT AT BOTTOM (HEIGHT 500.DP) WITH CROSSHAIR
            DatumCameraView(
                isCameraEnabled = true,
                isFlashOn = isFlashOn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp),
                cornerRadius = 28.dp,
                onColorSampled = { color ->
                    if (!isFrozen) {
                        sampledColor = color
                    }
                }
            ) {
                // Center Crosshair Reticle Overlay
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Outer Target Circle
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = 28f,
                        center = center,
                        style = Stroke(width = 2f)
                    )
                    // Inner Swatch Dot
                    drawCircle(
                        color = sampledColor,
                        radius = 12f,
                        center = center
                    )
                }

                // Freeze / Hold Button at Bottom Center
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                ) {
                    Button(
                        onClick = {
                            haptics.click()
                            isFrozen = !isFrozen
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .shadow(elevation = 6.dp, shape = ContinuousCapsule),
                        shape = ContinuousCapsule,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFrozen) vibrantOrange else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isFrozen) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = if (isFrozen) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFrozen) "RESUME" else "HOLD / FREEZE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
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
