package com.awras.mono.features.tools.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.awras.mono.ui.components.DatumToolTopBar
import com.awras.mono.ui.theme.LocalTactileFeedback
import com.kyant.capsule.ContinuousCapsule
import compose.icons.TablerIcons
import compose.icons.tablericons.Activity
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.*

enum class BpmMode(val label: String) {
    AUTOMATIC("Automatic (Mic)"),
    MANUAL("Manual (Tap)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BpmDetectorScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    var bpmMode by remember { mutableStateOf(BpmMode.AUTOMATIC) }
    var detectedBpm by remember { mutableFloatStateOf(0f) }
    var isListening by remember { mutableStateOf(false) }
    var isBeatPulsing by remember { mutableStateOf(false) }

    // Tap Tempo State
    val tapTimestamps = remember { mutableStateListOf<Long>() }

    // Audio Permission State
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Advanced Automatic Mic Peak Detection & Median Filtering Thread
    DisposableEffect(isListening, bpmMode) {
        if (!isListening || bpmMode != BpmMode.AUTOMATIC || !hasMicPermission) {
            return@DisposableEffect onDispose {}
        }

        var isRunning = true
        var audioRecord: AudioRecord? = null

        thread(name = "BpmAutoDetector", isDaemon = true) {
            val sampleRate = 22050
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = maxOf(
                AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat),
                1024
            )

            try {
                @SuppressLint("MissingPermission")
                val recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
                audioRecord = recorder
                recorder.startRecording()

                val buffer = ShortArray(bufferSize)
                val peakIntervals = mutableListOf<Long>()
                var lastPeakTime = 0L
                var runningAvgEnergy = 0.0

                while (isRunning) {
                    val readSize = recorder.read(buffer, 0, bufferSize)
                    if (readSize > 0) {
                        var currentEnergy = 0.0
                        for (i in 0 until readSize) {
                            val s = buffer[i].toDouble()
                            currentEnergy += s * s
                        }
                        currentEnergy = sqrt(currentEnergy / readSize)

                        // Smooth running average energy baseline
                        runningAvgEnergy = runningAvgEnergy * 0.92 + currentEnergy * 0.08

                        val now = System.currentTimeMillis()
                        // 300ms refractory period prevents double-triggering octave errors
                        if (currentEnergy > runningAvgEnergy * 1.65 && currentEnergy > 600.0 && (now - lastPeakTime) >= 300L) {
                            if (lastPeakTime > 0L) {
                                val interval = now - lastPeakTime
                                // Valid musical tempo range: 60 BPM (1000ms) to 200 BPM (300ms)
                                if (interval in 300L..1000L) {
                                    peakIntervals.add(interval)
                                    if (peakIntervals.size > 6) peakIntervals.removeAt(0)

                                    // Median filtering to discard outlier noise spikes
                                    val sortedIntervals = peakIntervals.sorted()
                                    val medianInterval = sortedIntervals[sortedIntervals.size / 2]

                                    if (medianInterval > 0) {
                                        val calcBpm = (60000.0 / medianInterval).toFloat().coerceIn(60f, 200f)
                                        detectedBpm = calcBpm
                                        isBeatPulsing = true
                                    }
                                }
                            }
                            lastPeakTime = now
                        } else {
                            if (now - lastPeakTime > 500L) {
                                isBeatPulsing = false
                            }
                        }
                    }
                    Thread.sleep(25)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        onDispose {
            isRunning = false
            try {
                audioRecord?.stop()
                audioRecord?.release()
            } catch (_: Exception) {}
        }
    }

    // Manual Tap Tempo Handler with Median Filtering
    fun handleManualTap() {
        val now = System.currentTimeMillis()
        haptics.click()
        isBeatPulsing = true

        if (tapTimestamps.isNotEmpty() && (now - tapTimestamps.last() > 2000)) {
            tapTimestamps.clear()
        }

        tapTimestamps.add(now)

        if (tapTimestamps.size >= 2) {
            val intervals = mutableListOf<Long>()
            for (i in 1 until tapTimestamps.size) {
                intervals.add(tapTimestamps[i] - tapTimestamps[i - 1])
            }
            val sortedIntervals = intervals.sorted()
            val medianInterval = sortedIntervals[sortedIntervals.size / 2]

            if (medianInterval > 0) {
                val calcBpm = (60000.0 / medianInterval).toFloat().coerceIn(40f, 220f)
                detectedBpm = calcBpm
            }
        }

        if (tapTimestamps.size > 6) {
            tapTimestamps.removeAt(0)
        }
    }

    // Smooth BPM Animation
    val smoothBpm by animateFloatAsState(
        targetValue = detectedBpm,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "SmoothBpm"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "BPM Detector",
                description = "Automatic & Manual Tempo Analyzer",
                icon = TablerIcons.Activity,
                onBackClick = onBackClick,
                rightActionIcon = Icons.Default.Refresh,
                onRightActionClick = {
                    haptics.click()
                    detectedBpm = 0f
                    tapTimestamps.clear()
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
                // 1. TOP PROMINENT BPM READOUT
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (smoothBpm > 0f) String.format(Locale.US, "%.0f", smoothBpm) else "---",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                            fontWeight = FontWeight.Black,
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            letterSpacing = (-1.5).sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "BPM",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            color = vibrantOrange,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (bpmMode == BpmMode.AUTOMATIC) "MIC TRANSIENT DETECTION" else "MANUAL TAP TEMPO",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = vibrantOrange,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. PULSING BEAT STROBE RADIAL CANVAS
                val flatSurfaceBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                val guideColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                val defaultNodeColor = MaterialTheme.colorScheme.onBackground
                val nodeColor = if (isBeatPulsing) vibrantOrange else defaultNodeColor

                Surface(
                    modifier = Modifier
                        .size(280.dp),
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

                        // Concentric Rings
                        for (i in 1..3) {
                            drawCircle(
                                color = guideColor,
                                radius = (radius / 3f) * i,
                                center = center,
                                style = Stroke(width = 1f)
                            )
                        }

                        // Pulsing Beat Ring when beat detected
                        if (isBeatPulsing) {
                            drawCircle(
                                color = vibrantOrange.copy(alpha = 0.35f),
                                radius = radius * 0.90f,
                                center = center
                            )
                        }

                        // Center Pulsing Node
                        drawCircle(
                            color = nodeColor,
                            radius = if (isBeatPulsing) 22f else 14f,
                            center = center
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 6f,
                            center = center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. MODE SWITCHER PILL (AUTOMATIC vs MANUAL)
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
                        BpmMode.entries.forEach { mode ->
                            val isSelected = bpmMode == mode
                            val bgColor = if (isSelected) vibrantOrange else Color.Transparent
                            val txtColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

                            Box(
                                modifier = Modifier
                                    .clip(ContinuousCapsule)
                                    .background(bgColor)
                                    .clickable {
                                        haptics.tick()
                                        bpmMode = mode
                                    }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = txtColor,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4. ACTION BUTTON (START/STOP MIC OR TAP PAD)
                if (bpmMode == BpmMode.AUTOMATIC) {
                    Button(
                        onClick = {
                            haptics.click()
                            isListening = !isListening
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(elevation = 8.dp, shape = ContinuousCapsule),
                        shape = ContinuousCapsule,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isListening) MaterialTheme.colorScheme.surfaceVariant else vibrantOrange,
                            contentColor = if (isListening) MaterialTheme.colorScheme.onSurface else Color.White
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = null
                            )
                            Text(
                                text = if (isListening) "STOP LISTENING" else "START MIC DETECTION",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { handleManualTap() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(elevation = 8.dp, shape = ContinuousCapsule),
                        shape = ContinuousCapsule,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = vibrantOrange,
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null
                            )
                            Text(
                                text = "TAP TO DETECT BPM",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
