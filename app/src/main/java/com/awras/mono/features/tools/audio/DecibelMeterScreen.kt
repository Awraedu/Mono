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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.awras.mono.ui.components.DatumToolTopBar
import com.awras.mono.ui.theme.LocalTactileFeedback
import com.kyant.capsule.ContinuousRoundedRectangle
import compose.icons.TablerIcons
import compose.icons.tablericons.Volume
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.*

private val HazardRed = Color(0xFFFF3B30)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecibelMeterScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    // Live Decibel State
    var rawDb by remember { mutableFloatStateOf(30.0f) }
    var minDb by remember { mutableFloatStateOf(120.0f) }
    var maxDb by remember { mutableFloatStateOf(0.0f) }
    var isRecording by remember { mutableStateOf(false) }

    // 48-Band Audio Energy Array & Peak Hold Array
    val bandEnergies = remember { mutableStateListOf<Float>().apply { repeat(48) { add(0f) } } }
    val peakCaps = remember { mutableStateListOf<Float>().apply { repeat(48) { add(0f) } } }

    // Rolling dB History Buffer
    val dbHistory = remember { mutableStateListOf<Float>().apply { repeat(80) { add(30f) } } }

    // Animated Orbit Phase for Rotational Energy Transfer
    val infiniteTransition = rememberInfiniteTransition(label = "OrbitPhase")
    val orbitRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitRotation"
    )

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

    // AudioRecord Thread Sampling with Grouped Frequency Energy Calculation
    DisposableEffect(hasMicPermission) {
        if (!hasMicPermission) return@DisposableEffect onDispose {}

        isRecording = true
        var audioRecord: AudioRecord? = null

        thread {
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = maxOf(
                AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat),
                2048
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

                while (isRecording) {
                    val readSize = recorder.read(buffer, 0, bufferSize)
                    if (readSize > 0) {
                        var sum = 0.0
                        for (i in 0 until readSize) {
                            val sample = buffer[i].toDouble()
                            sum += sample * sample
                        }
                        val rms = sqrt(sum / readSize)
                        val db = if (rms > 0) (20 * log10(rms) + 25.0).toFloat().coerceIn(20f, 120f) else 20f

                        rawDb = db
                        if (db < minDb) minDb = db
                        if (db > maxDb) maxDb = db

                        val normVol = ((db - 20f) / 100f).coerceIn(0f, 1f)

                        // 6 Grouped Frequency Band Energies mapped across 48 radial bars
                        val numGroups = 6
                        val groupSize = 48 / numGroups
                        val chunkSize = maxOf(1, readSize / numGroups)

                        for (g in 0 until numGroups) {
                            var groupSum = 0.0
                            val startIdx = g * chunkSize
                            val endIdx = minOf(readSize, startIdx + chunkSize)
                            for (j in startIdx until endIdx) {
                                groupSum += abs(buffer[j].toDouble())
                            }
                            val groupAvg = if (endIdx > startIdx) (groupSum / (endIdx - startIdx) / 32768.0).toFloat() else 0f

                            for (b in 0 until groupSize) {
                                val barIdx = g * groupSize + b
                                val jitter = 0.85f + 0.30f * sin((barIdx * 0.7f + db * 0.15f).toDouble()).toFloat()
                                val targetEnergy = (normVol * 0.40f + groupAvg * 4.0f * jitter).coerceIn(0.05f, 1.0f)

                                val prevEnergy = bandEnergies[barIdx]
                                val smoothed = prevEnergy + 0.35f * (targetEnergy - prevEnergy)
                                bandEnergies[barIdx] = smoothed

                                // Peak Cap Decay
                                val currentCap = peakCaps[barIdx]
                                if (smoothed > currentCap) {
                                    peakCaps[barIdx] = smoothed
                                } else {
                                    peakCaps[barIdx] = maxOf(0.05f, currentCap - 0.03f)
                                }
                            }
                        }

                        // Append to history buffer
                        if (dbHistory.size >= 80) {
                            dbHistory.removeAt(0)
                        }
                        dbHistory.add(db)
                    }
                    Thread.sleep(30)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        onDispose {
            isRecording = false
            try {
                audioRecord?.stop()
                audioRecord?.release()
            } catch (_: Exception) {}
        }
    }

    // Smooth dB Motion
    val smoothDb by animateFloatAsState(
        targetValue = rawDb,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "SmoothDb"
    )

    val isHazard = smoothDb >= 85f

    // Haptic warning when exceeding 85dB
    var lastHazardState by remember { mutableStateOf(false) }
    LaunchedEffect(isHazard) {
        if (isHazard && !lastHazardState) {
            haptics.click()
        }
        lastHazardState = isHazard
    }

    // Noise Status Category
    val noiseCategory = when {
        smoothDb < 40f -> "QUIET / WHISPER"
        smoothDb < 65f -> "MODERATE CONVERSATION"
        smoothDb < 85f -> "BUSY STREET / LOUD NOISE"
        else -> "HEARING HAZARD DANGER!"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Decibel Meter",
                description = "Acoustic Sound Pressure Level Meter",
                icon = TablerIcons.Volume,
                onBackClick = onBackClick,
                rightActionIcon = Icons.Default.Refresh,
                onRightActionClick = {
                    haptics.click()
                    minDb = 120f
                    maxDb = 0f
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
                // 1. TOP PROMINENT DECIBEL READOUT
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.0f", smoothDb),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 58.sp),
                            fontWeight = FontWeight.Black,
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            letterSpacing = (-1.5).sp,
                            color = if (isHazard) HazardRed else MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "dB",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            color = if (isHazard) HazardRed else vibrantOrange,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format(Locale.US, "MIN: %.0f  •  MAX: %.0f", if (minDb == 120f) 0f else minDb, maxDb),
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
                            text = noiseCategory,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isHazard) HazardRed else vibrantOrange,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. PERFECT CIRCULAR SPECTRAL RTA VISUALIZER WITH ORBITING ENERGY & ZERO INNER STROKE OVERLAP
                val flatSurfaceBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)

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
                        val innerCircleRadius = size.width * 0.22f
                        val lineStartRadius = innerCircleRadius + 6f // Clean gap to avoid intersecting inner circle stroke
                        val maxBarLen = size.width * 0.22f

                        val activeColor = if (isHazard) HazardRed else vibrantOrange

                        // Subwoofer Bass Pulse Ring
                        val pulseFactor = ((smoothDb - 20f) / 100f).coerceIn(0.05f, 1f)
                        drawCircle(
                            color = activeColor.copy(alpha = 0.18f * pulseFactor),
                            radius = innerCircleRadius + (maxBarLen * pulseFactor * 0.35f),
                            center = center
                        )

                        // 48 Perfectly Aligned Radial Equalizer Bars with Orbiting Rotational Energy Transfer
                        val numBars = 48
                        val orbitOffsetBars = (orbitRotation / (360f / numBars)).toInt()

                        for (i in 0 until numBars) {
                            val angleRad = Math.toRadians((i * (360.0 / numBars) - 90.0))
                            val cosVal = cos(angleRad).toFloat()
                            val sinVal = sin(angleRad).toFloat()

                            // Orbiting energy mapping
                            val sampledBarIdx = (i + orbitOffsetBars) % numBars

                            val energy = bandEnergies[sampledBarIdx]
                            val capEnergy = peakCaps[sampledBarIdx]

                            val barLen = (maxBarLen * energy).coerceIn(4f, maxBarLen)
                            val capLen = (maxBarLen * capEnergy).coerceIn(4f, maxBarLen)

                            val startX = center.x + lineStartRadius * cosVal
                            val startY = center.y + lineStartRadius * sinVal
                            val endX = center.x + (lineStartRadius + barLen) * cosVal
                            val endY = center.y + (lineStartRadius + barLen) * sinVal

                            // Equalizer Bar (starts cleanly outside inner circle)
                            drawLine(
                                color = activeColor,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = 3f,
                                cap = StrokeCap.Round
                            )

                            // Peak Hold Decay Dot Cap
                            val capX = center.x + (lineStartRadius + capLen + 5f) * cosVal
                            val capY = center.y + (lineStartRadius + capLen + 5f) * sinVal
                            drawCircle(
                                color = Color.White.copy(alpha = 0.95f),
                                radius = 2f,
                                center = Offset(capX, capY)
                            )
                        }

                        // Center Speaker Cone Circle (Clean stroke, no line overlap)
                        drawCircle(
                            color = activeColor,
                            radius = innerCircleRadius,
                            center = center,
                            style = Stroke(width = 2f)
                        )
                        drawCircle(
                            color = activeColor.copy(alpha = 0.85f),
                            radius = 8f,
                            center = center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. LIVE ROLLING DECIBEL WAVEFORM GRAPH
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    shape = ContinuousRoundedRectangle(16.dp),
                    color = flatSurfaceBg,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        if (dbHistory.size > 1) {
                            val stepX = w / (dbHistory.size - 1).toFloat()
                            val path = Path()

                            for (i in dbHistory.indices) {
                                val dbVal = dbHistory[i].coerceIn(20f, 120f)
                                val x = i * stepX
                                val y = h - ((dbVal - 20f) / 100f) * h

                                if (i == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }

                            drawPath(
                                path = path,
                                color = if (isHazard) HazardRed else vibrantOrange,
                                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
