package com.awras.mono.features.tools.vision

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import compose.icons.tablericons.Barcode
import compose.icons.tablericons.Qrcode

enum class ScanMode {
    QR_CODE,
    BARCODE
}

sealed class QrResultType {
    data class WebLink(val url: String) : QrResultType()
    data class Wifi(val ssid: String, val pass: String) : QrResultType()
    data class Email(val address: String) : QrResultType()
    data class Phone(val number: String) : QrResultType()
    data class Text(val content: String) : QrResultType()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    var scannedResult by remember { mutableStateOf<String?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }
    var scanMode by remember { mutableStateOf(ScanMode.QR_CODE) }

    // Result Type Parsing
    val resultType = remember(scannedResult) {
        val res = scannedResult ?: return@remember null
        when {
            res.startsWith("WIFI:", ignoreCase = true) -> {
                val ssidMatch = Regex("S:([^;]+)").find(res)?.groupValues?.get(1) ?: "Unknown Wi-Fi"
                val passMatch = Regex("P:([^;]+)").find(res)?.groupValues?.get(1) ?: ""
                QrResultType.Wifi(ssidMatch, passMatch)
            }
            res.startsWith("mailto:", ignoreCase = true) -> QrResultType.Email(res.removePrefix("mailto:"))
            res.startsWith("tel:", ignoreCase = true) -> QrResultType.Phone(res.removePrefix("tel:"))
            res.startsWith("http://", ignoreCase = true) || res.startsWith("https://", ignoreCase = true) || res.contains(".") -> {
                QrResultType.WebLink(res)
            }
            else -> QrResultType.Text(res)
        }
    }

    // Larger Viewfinder Size Animation
    val targetBoxWidth = if (scanMode == ScanMode.QR_CODE) 500f else 520f
    val targetBoxHeight = if (scanMode == ScanMode.QR_CODE) 500f else 260f

    val animatedBoxWidth by animateFloatAsState(
        targetValue = targetBoxWidth,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "BoxWidth"
    )
    val animatedBoxHeight by animateFloatAsState(
        targetValue = targetBoxHeight,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "BoxHeight"
    )

    // Animated Scanner Laser Line Y-position
    val infiniteTransition = rememberInfiniteTransition(label = "ScannerLaser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserProgress"
    )

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Scanned Result", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun openUrl(url: String) {
        try {
            val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl))
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Cannot open URL", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Scanned Content"))
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = if (scanMode == ScanMode.QR_CODE) "QR Code Scanner" else "Barcode Reader",
                description = "Tap to switch scan mode",
                icon = if (scanMode == ScanMode.QR_CODE) TablerIcons.Qrcode else TablerIcons.Barcode,
                onBackClick = onBackClick,
                rightActionIcon = if (scanMode == ScanMode.QR_CODE) TablerIcons.Barcode else TablerIcons.Qrcode,
                onRightActionClick = {
                    haptics.click()
                    scanMode = if (scanMode == ScanMode.QR_CODE) ScanMode.BARCODE else ScanMode.QR_CODE
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. ADVANCED RESULT VIEWER (NO SHADOW, ADAPTS TO RESULT TYPE)
            if (scannedResult != null && resultType != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ContinuousRoundedRectangle(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = when (resultType) {
                                    is QrResultType.WebLink -> Icons.Default.Language
                                    is QrResultType.Wifi -> Icons.Default.Wifi
                                    is QrResultType.Email -> Icons.Default.Email
                                    is QrResultType.Phone -> Icons.Default.Phone
                                    is QrResultType.Text -> Icons.Default.Description
                                },
                                contentDescription = null,
                                tint = vibrantOrange
                            )
                            Text(
                                text = when (resultType) {
                                    is QrResultType.WebLink -> "WEBSITE / LINK DETECTED"
                                    is QrResultType.Wifi -> "WI-FI NETWORK DETECTED"
                                    is QrResultType.Email -> "EMAIL ADDRESS DETECTED"
                                    is QrResultType.Phone -> "PHONE NUMBER DETECTED"
                                    is QrResultType.Text -> "PLAIN TEXT DETECTED"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = vibrantOrange,
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = when (resultType) {
                                is QrResultType.Wifi -> "SSID: ${resultType.ssid}\nPassword: ${resultType.pass}"
                                else -> scannedResult!!
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 4
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    haptics.click()
                                    copyToClipboard(scannedResult!!)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = ContinuousCapsule,
                                colors = ButtonDefaults.buttonColors(containerColor = vibrantOrange)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("COPY", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }

                            when (resultType) {
                                is QrResultType.WebLink -> {
                                    Button(
                                        onClick = {
                                            haptics.click()
                                            openUrl(resultType.url)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp),
                                        shape = ContinuousCapsule,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface)
                                    ) {
                                        Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("OPEN", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                                is QrResultType.Phone -> {
                                    Button(
                                        onClick = {
                                            haptics.click()
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${resultType.number}"))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp),
                                        shape = ContinuousCapsule,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("CALL", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                                else -> {}
                            }

                            IconButton(
                                onClick = {
                                    haptics.click()
                                    shareText(scannedResult!!)
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }

                            IconButton(
                                onClick = {
                                    haptics.click()
                                    scannedResult = null
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Scan Again")
                            }
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ContinuousRoundedRectangle(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(vibrantOrange)
                        ) {}
                        Text(
                            text = "Align code inside the viewfinder frame to scan",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 2. SHORTER DATUM CAMERA VIEWPORT AT THE BOTTOM
            DatumCameraView(
                isCameraEnabled = true,
                isFlashOn = isFlashOn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp),
                cornerRadius = 28.dp,
                onBarcodeScanned = { result ->
                    if (scannedResult == null) {
                        haptics.success()
                        scannedResult = result
                    }
                }
            ) {
                // Viewfinder Reticle & Laser Overlay
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val boxW = animatedBoxWidth
                    val boxH = animatedBoxHeight
                    val left = (w - boxW) / 2f
                    val top = (h - boxH) / 2f
                    val right = left + boxW
                    val bottom = top + boxH
                    val cornerLen = 28f

                    // Dimmed Surroundings
                    val dimColor = Color.Black.copy(alpha = 0.45f)
                    drawRect(dimColor, Offset(0f, 0f), Size(w, top))
                    drawRect(dimColor, Offset(0f, bottom), Size(w, h - bottom))
                    drawRect(dimColor, Offset(0f, top), Size(left, boxH))
                    drawRect(dimColor, Offset(right, top), Size(w - right, boxH))

                    // Reticle Frame Border
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.3f),
                        topLeft = Offset(left, top),
                        size = Size(boxW, boxH),
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                        style = Stroke(width = 1.5f)
                    )

                    // Corner Accents in Vibrant Orange
                    val strokeW = 4f
                    drawLine(vibrantOrange, Offset(left, top), Offset(left + cornerLen, top), strokeWidth = strokeW, cap = StrokeCap.Round)
                    drawLine(vibrantOrange, Offset(left, top), Offset(left, top + cornerLen), strokeWidth = strokeW, cap = StrokeCap.Round)

                    drawLine(vibrantOrange, Offset(right, top), Offset(right - cornerLen, top), strokeWidth = strokeW, cap = StrokeCap.Round)
                    drawLine(vibrantOrange, Offset(right, top), Offset(right, top + cornerLen), strokeWidth = strokeW, cap = StrokeCap.Round)

                    drawLine(vibrantOrange, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeWidth = strokeW, cap = StrokeCap.Round)
                    drawLine(vibrantOrange, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeWidth = strokeW, cap = StrokeCap.Round)

                    drawLine(vibrantOrange, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeWidth = strokeW, cap = StrokeCap.Round)
                    drawLine(vibrantOrange, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeWidth = strokeW, cap = StrokeCap.Round)

                    // Scanning Laser Line
                    if (scannedResult == null) {
                        val laserY = top + (boxH * laserProgress)
                        drawLine(
                            color = vibrantOrange,
                            start = Offset(left + 12f, laserY),
                            end = Offset(right - 12f, laserY),
                            strokeWidth = 2.5f,
                            cap = StrokeCap.Round
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
                        .padding(end = 16.dp, bottom = 16.dp)
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
