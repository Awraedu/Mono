package com.awras.mono.features.tools.vision

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.awras.mono.ui.components.DatumCameraView
import com.awras.mono.ui.components.DatumToolTopBar
import com.awras.mono.ui.theme.LocalTactileFeedback
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import compose.icons.TablerIcons
import compose.icons.tablericons.FileText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextExtractorScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    var extractedText by remember { mutableStateOf<String?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    val imageCaptureRef = remember { mutableStateOf<ImageCapture?>(null) }
    val scrollState = rememberScrollState()

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Extracted Text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun captureAndExtractText() {
        val imageCapture = imageCaptureRef.value
        if (imageCapture == null) {
            Toast.makeText(context, "Camera not ready", Toast.LENGTH_SHORT).show()
            return
        }

        isProcessing = true
        haptics.click()

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val buffer = imageProxy.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    imageProxy.close()

                    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    capturedBitmap = rotatedBitmap

                    // Run ML Kit Text Recognition
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    val image = InputImage.fromBitmap(rotatedBitmap, 0)

                    recognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            isProcessing = false
                            val result = visionText.text
                            if (result.isNotBlank()) {
                                haptics.success()
                                extractedText = result
                            } else {
                                Toast.makeText(context, "No text found in photo", Toast.LENGTH_SHORT).show()
                                capturedBitmap = null
                            }
                        }
                        .addOnFailureListener {
                            isProcessing = false
                            Toast.makeText(context, "Text extraction failed", Toast.LENGTH_SHORT).show()
                            capturedBitmap = null
                        }
                }

                override fun onError(exception: ImageCaptureException) {
                    isProcessing = false
                    Toast.makeText(context, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Text Extractor",
                description = "AI Optical Character Recognition (OCR)",
                icon = TablerIcons.FileText,
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
                .padding(top = innerPadding.calculateTopPadding())
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. EXTRACTED RESULT CARD AT TOP (NO SHADOW, SCROLLABLE, SELECTABLE, EXPANDABLE)
            if (extractedText != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isExpanded) Modifier.weight(1f) else Modifier.heightIn(max = 240.dp)),
                    shape = ContinuousRoundedRectangle(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = vibrantOrange
                                )
                                Text(
                                    text = "EXTRACTED TEXT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = vibrantOrange,
                                    letterSpacing = 1.sp
                                )
                            }

                            IconButton(
                                onClick = {
                                    haptics.click()
                                    isExpanded = !isExpanded
                                }
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Expand",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        SelectionContainer {
                            Text(
                                text = extractedText!!,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    haptics.click()
                                    copyToClipboard(extractedText!!)
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
                            text = "Frame text in view & press shutter to extract",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            if (!isExpanded) {
                Spacer(modifier = Modifier.weight(1f))

                // 2. DATUM CAMERA VIEWPORT AT THE BOTTOM (HEIGHT 500.DP, NO FRAMING BOX, FREEZE ON CAPTURE)
                DatumCameraView(
                    isCameraEnabled = extractedText == null,
                    isFlashOn = isFlashOn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp),
                    cornerRadius = 28.dp,
                    imageCaptureRef = imageCaptureRef
                ) {
                    // If captured, show frozen bitmap snapshot
                    if (capturedBitmap != null) {
                        Image(
                            bitmap = capturedBitmap!!.asImageBitmap(),
                            contentDescription = "Captured Snapshot",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Bottom-Center Shutter Capture Button
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(56.dp),
                                color = vibrantOrange,
                                strokeWidth = 4.dp
                            )
                        } else if (extractedText == null) {
                            FloatingActionButton(
                                onClick = { captureAndExtractText() },
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                containerColor = vibrantOrange,
                                contentColor = Color.White
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Camera,
                                    contentDescription = "Capture Photo",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    // Inside Viewport: Bottom-Left Reset / Scan Another Button
                    if (extractedText != null) {
                        IconButton(
                            onClick = {
                                haptics.click()
                                extractedText = null
                                capturedBitmap = null
                            },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 16.dp, bottom = 20.dp)
                                .alpha(0.85f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Scan Another",
                                tint = vibrantOrange,
                                modifier = Modifier.size(26.dp)
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
}
