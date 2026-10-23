package com.awras.mono.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.kyant.capsule.ContinuousRoundedRectangle
import java.util.concurrent.Executors

@Composable
fun DatumCameraView(
    isCameraEnabled: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    isFlashOn: Boolean = false,
    onBarcodeScanned: ((String) -> Unit)? = null,
    onObjectsDetected: ((List<DetectedObject>) -> Unit)? = null,
    onColorSampled: ((Color) -> Unit)? = null,
    imageCaptureRef: MutableState<ImageCapture?>? = null,
    overlayContent: @Composable BoxScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }

    // Control Torch State when isFlashOn changes
    LaunchedEffect(isFlashOn, cameraInstance) {
        try {
            cameraInstance?.cameraControl?.enableTorch(isFlashOn)
        } catch (_: Exception) {}
    }

    // Runtime Camera Permission State
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(isCameraEnabled) {
        if (isCameraEnabled && !hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Surface(
        modifier = modifier
            .clip(ContinuousRoundedRectangle(cornerRadius)),
        shape = ContinuousRoundedRectangle(cornerRadius),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Live Camera Preview or Neutral Fallback Surface
            if (isCameraEnabled && hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val imageCapture = ImageCapture.Builder().build()
                            if (imageCaptureRef != null) {
                                imageCaptureRef.value = imageCapture
                            }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()

                                if (onBarcodeScanned != null) {
                                    val barcodeScanner = BarcodeScanning.getClient()
                                    val executor = Executors.newSingleThreadExecutor()
                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()

                                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                        @OptIn(ExperimentalGetImage::class)
                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null) {
                                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                            barcodeScanner.process(image)
                                                .addOnSuccessListener { barcodes: MutableList<Barcode> ->
                                                    for (b in barcodes) {
                                                        val raw = b.rawValue
                                                        if (raw != null) {
                                                            onBarcodeScanned(raw)
                                                            break
                                                        }
                                                    }
                                                }
                                                .addOnCompleteListener {
                                                    imageProxy.close()
                                                }
                                        } else {
                                            imageProxy.close()
                                        }
                                    }

                                    val cam = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture,
                                        imageAnalysis
                                    )
                                    cameraInstance = cam
                                    cam.cameraControl.enableTorch(isFlashOn)
                                } else if (onObjectsDetected != null) {
                                    val objectDetector = ObjectDetection.getClient(
                                        ObjectDetectorOptions.Builder()
                                            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                                            .enableMultipleObjects()
                                            .enableClassification()
                                            .build()
                                    )
                                    val executor = Executors.newSingleThreadExecutor()
                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()

                                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                        @OptIn(ExperimentalGetImage::class)
                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null) {
                                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                            objectDetector.process(image)
                                                .addOnSuccessListener { objects ->
                                                    onObjectsDetected(objects)
                                                }
                                                .addOnCompleteListener {
                                                    imageProxy.close()
                                                }
                                        } else {
                                            imageProxy.close()
                                        }
                                    }

                                    val cam = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture,
                                        imageAnalysis
                                    )
                                    cameraInstance = cam
                                    cam.cameraControl.enableTorch(isFlashOn)
                                } else if (onColorSampled != null) {
                                    val executor = Executors.newSingleThreadExecutor()
                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()

                                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                        try {
                                            val bitmap = imageProxy.toBitmap()
                                            val cx = bitmap.width / 2
                                            val cy = bitmap.height / 2
                                            val pixel = bitmap.getPixel(cx, cy)
                                            val color = Color(pixel)
                                            onColorSampled(color)
                                        } catch (_: Exception) {}
                                        imageProxy.close()
                                    }

                                    val cam = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture,
                                        imageAnalysis
                                    )
                                    cameraInstance = cam
                                    cam.cameraControl.enableTorch(isFlashOn)
                                } else {
                                    val cam = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture
                                    )
                                    cameraInstance = cam
                                    cam.cameraControl.enableTorch(isFlashOn)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Neutral Flat Instrument Surface Fallback
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                )
            }

            // Custom Measurement Overlay Slot
            overlayContent()
        }
    }
}
