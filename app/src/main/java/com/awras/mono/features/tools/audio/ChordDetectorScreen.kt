package com.awras.mono.features.tools.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.kyant.capsule.ContinuousRoundedRectangle
import compose.icons.TablerIcons
import compose.icons.tablericons.Music
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.*

private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

data class ChordInfo(
    val name: String,
    val formula: String,
    val activeNotes: List<Int> // Indices in 0..11
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChordDetectorScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    // Live Note Energies (12 chromatic notes)
    val noteEnergies = remember { mutableStateListOf<Float>().apply { repeat(12) { add(0f) } } }
    var detectedChord by remember { mutableStateOf<ChordInfo?>(null) }
    var isRecording by remember { mutableStateOf(false) }

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

    // Audio Sampling & Chromagram Pitch Correlation Thread
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
                4096
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

                // 12 Chromatic Base Frequencies across Octaves 3 & 4
                val baseFreqs = floatArrayOf(
                    130.81f, 138.59f, 146.83f, 155.56f, 164.81f, 174.61f,
                    185.00f, 196.00f, 207.65f, 220.00f, 233.08f, 246.94f
                )

                while (isRecording) {
                    val readSize = recorder.read(buffer, 0, bufferSize)
                    if (readSize > 0) {
                        // Calculate energy for each of the 12 chromatic pitch bins
                        val tempEnergies = FloatArray(12)

                        for (n in 0 until 12) {
                            val f1 = baseFreqs[n].toDouble()
                            val f2 = f1 * 2.0 // Octave 4
                            val omega1 = 2.0 * PI * f1 / sampleRate
                            val omega2 = 2.0 * PI * f2 / sampleRate

                            var cos1 = 0.0
                            var sin1 = 0.0
                            var cos2 = 0.0
                            var sin2 = 0.0

                            for (i in 0 until readSize step 2) {
                                val s = buffer[i].toDouble()
                                cos1 += s * cos(omega1 * i)
                                sin1 += s * sin(omega1 * i)
                                cos2 += s * cos(omega2 * i)
                                sin2 += s * sin(omega2 * i)
                            }

                            val energyAcc = (sqrt(cos1 * cos1 + sin1 * sin1) + sqrt(cos2 * cos2 + sin2 * sin2)) / readSize
                            tempEnergies[n] = energyAcc.toFloat()
                        }

                        // Smooth note energies
                        val maxE = maxOf(10f, tempEnergies.maxOrNull() ?: 1f)
                        for (n in 0 until 12) {
                            val normE = (tempEnergies[n] / maxE).coerceIn(0f, 1f)
                            noteEnergies[n] = noteEnergies[n] + 0.3f * (normE - noteEnergies[n])
                        }

                        // Match Chords
                        var bestScore = 1.4f
                        var bestChord: ChordInfo? = null

                        for (r in 0 until 12) {
                            val rootE = noteEnergies[r]
                            if (rootE < 0.25f) continue

                            val rootName = NOTE_NAMES[r]

                            // Major Triad (0, 4, 7)
                            val maj3 = noteEnergies[(r + 4) % 12]
                            val p5 = noteEnergies[(r + 7) % 12]
                            val majScore = rootE + maj3 + p5
                            if (majScore > bestScore && maj3 > 0.2f && p5 > 0.2f) {
                                bestScore = majScore
                                bestChord = ChordInfo("$rootName Major", "1 - 3 - 5", listOf(r, (r + 4) % 12, (r + 7) % 12))
                            }

                            // Minor Triad (0, 3, 7)
                            val min3 = noteEnergies[(r + 3) % 12]
                            val minScore = rootE + min3 + p5
                            if (minScore > bestScore && min3 > 0.2f && p5 > 0.2f) {
                                bestScore = minScore
                                bestChord = ChordInfo("$rootName minor", "1 - b3 - 5", listOf(r, (r + 3) % 12, (r + 7) % 12))
                            }

                            // Dominant 7th (0, 4, 7, 10)
                            val dom7 = noteEnergies[(r + 10) % 12]
                            val dom7Score = rootE + maj3 + p5 + dom7
                            if (dom7Score > bestScore && dom7 > 0.2f) {
                                bestScore = dom7Score
                                bestChord = ChordInfo("${rootName}7", "1 - 3 - 5 - b7", listOf(r, (r + 4) % 12, (r + 7) % 12, (r + 10) % 12))
                            }
                        }

                        if (bestChord != null && bestChord != detectedChord) {
                            detectedChord = bestChord
                            haptics.tick()
                        }
                    }
                    Thread.sleep(50)
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Chord Detector",
                description = "Real-time Polyphonic Pitch & Piano Keyboard",
                icon = TablerIcons.Music,
                onBackClick = onBackClick,
                rightActionIcon = Icons.Default.Refresh,
                onRightActionClick = {
                    haptics.click()
                    detectedChord = null
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
                // 1. TOP PROMINENT DETECTED CHORD READOUT
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = detectedChord?.name ?: "PLAY CHORD",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 50.sp),
                        fontWeight = FontWeight.Black,
                        fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                        letterSpacing = (-1.5).sp,
                        color = if (detectedChord != null) vibrantOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format(Locale.US, "FORMULA: %s", detectedChord?.formula ?: "1 - 3 - 5"),
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
                            text = if (detectedChord != null) "STABLE MATCH" else "LISTENING...",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (detectedChord != null) vibrantOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. INTERACTIVE STUDIO PIANO KEYBOARD CANVAS (7 White Keys + 5 Black Keys)
                val flatSurfaceBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    shape = ContinuousRoundedRectangle(24.dp),
                    color = flatSurfaceBg,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        val w = size.width
                        val h = size.height

                        // 7 White Keys (C, D, E, F, G, A, B)
                        val whiteKeyIndices = intArrayOf(0, 2, 4, 5, 7, 9, 11)
                        val whiteKeyWidth = w / 7f

                        for (i in 0 until 7) {
                            val noteIdx = whiteKeyIndices[i]
                            val isActive = detectedChord?.activeNotes?.contains(noteIdx) == true
                            val x = i * whiteKeyWidth
                            val keyColor = if (isActive) vibrantOrange else Color.White.copy(alpha = 0.90f)

                            drawRoundRect(
                                color = keyColor,
                                topLeft = Offset(x + 2f, 0f),
                                size = Size(whiteKeyWidth - 4f, h),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )
                        }

                        // 5 Black Keys (C#, D#, F#, G#, A#)
                        val blackKeyOffsets = floatArrayOf(
                            0.70f * whiteKeyWidth,
                            1.70f * whiteKeyWidth,
                            3.70f * whiteKeyWidth,
                            4.70f * whiteKeyWidth,
                            5.70f * whiteKeyWidth
                        )
                        val blackKeyIndices = intArrayOf(1, 3, 6, 8, 10)
                        val blackKeyWidth = whiteKeyWidth * 0.60f
                        val blackKeyHeight = h * 0.58f

                        for (i in 0 until 5) {
                            val noteIdx = blackKeyIndices[i]
                            val isActive = detectedChord?.activeNotes?.contains(noteIdx) == true
                            val x = blackKeyOffsets[i]
                            val keyColor = if (isActive) vibrantOrange else Color(0xFF18181A)

                            drawRoundRect(
                                color = keyColor,
                                topLeft = Offset(x, 0f),
                                size = Size(blackKeyWidth, blackKeyHeight),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. CHROMATIC NOTE ENERGY BAR BREAKDOWN
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = ContinuousRoundedRectangle(16.dp),
                    color = flatSurfaceBg,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 12) {
                            val isActive = detectedChord?.activeNotes?.contains(i) == true
                            val noteName = NOTE_NAMES[i]

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = noteName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = if (isActive) FontWeight.Black else FontWeight.Bold,
                                    color = if (isActive) vibrantOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height((noteEnergies[i] * 20f).coerceIn(4f, 20f).dp)
                                        .clip(ContinuousCapsule)
                                        .background(if (isActive) vibrantOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
