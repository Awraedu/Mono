package com.awras.mono.features.tools.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awras.mono.ui.components.DatumToolTopBar
import com.awras.mono.ui.theme.LocalTactileFeedback
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import compose.icons.TablerIcons
import compose.icons.tablericons.WaveSine
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.*

enum class WaveformType(val label: String) {
    SINE("Sine"),
    SQUARE("Square"),
    TRIANGLE("Triangle"),
    SAWTOOTH("Saw")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HertzGeneratorScreen(
    onBackClick: () -> Unit
) {
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    var frequency by remember { mutableFloatStateOf(440f) } // Default A4 Concert pitch
    var selectedWaveform by remember { mutableStateOf(WaveformType.SINE) }
    var isPlaying by remember { mutableStateOf(false) }

    // Audio Generator Thread
    DisposableEffect(isPlaying, frequency, selectedWaveform) {
        if (!isPlaying) return@DisposableEffect onDispose {}

        var isRunning = true
        var audioTrack: AudioTrack? = null

        thread(name = "HertzGeneratorAudio", isDaemon = true) {
            val sampleRate = 44100
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ) * 2

            try {
                @Suppress("DEPRECATION")
                val track = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                )
                audioTrack = track
                track.play()

                val numSamples = 2048
                val buffer = ShortArray(numSamples)
                var angle = 0.0

                while (isRunning) {
                    val freq = frequency.toDouble().coerceIn(20.0, 20000.0)
                    val twoPiF = 2.0 * PI * freq

                    for (i in 0 until numSamples) {
                        val t = angle / sampleRate
                        val sampleVal = when (selectedWaveform) {
                            WaveformType.SINE -> sin(twoPiF * t)
                            WaveformType.SQUARE -> if (sin(twoPiF * t) >= 0) 1.0 else -1.0
                            WaveformType.TRIANGLE -> (2.0 / PI) * asin(sin(twoPiF * t))
                            WaveformType.SAWTOOTH -> (2.0 / PI) * atan(tan(twoPiF * t / 2.0))
                        }
                        buffer[i] = (sampleVal * 32767.0 * 0.75).toInt().toShort()
                        angle += 1.0
                    }

                    track.write(buffer, 0, numSamples)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        onDispose {
            isRunning = false
            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (_: Exception) {}
        }
    }

    // Common Presets
    val presets = listOf(
        100f to "100 Hz",
        440f to "440 Hz (A4)",
        1000f to "1 kHz",
        5000f to "5 kHz",
        10000f to "10 kHz",
        15000f to "15 kHz"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Hertz Generator",
                description = "Precision Audio Tone Synthesizer",
                icon = TablerIcons.WaveSine,
                onBackClick = onBackClick,
                rightActionIcon = Icons.Default.Refresh,
                onRightActionClick = {
                    haptics.click()
                    frequency = 440f
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. FULL-SCREEN EDGE-TO-EDGE OSCILLOSCOPE WAVEFORM BACKGROUND CANVAS
            val guideColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            val inactiveWaveColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)

            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midY = h / 2f

                // Subtle Grid Lines
                for (i in 1..8) {
                    val y = (h / 9f) * i
                    drawLine(
                        color = guideColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                // Center Baseline
                drawLine(
                    color = guideColor.copy(alpha = 0.2f),
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 1.5f
                )

                val path = Path()
                val numPoints = 250
                val stepX = w / numPoints.toFloat()
                val freqRatio = (frequency / 2000f).coerceIn(0.2f, 5f)

                for (i in 0..numPoints) {
                    val x = i * stepX
                    val t = i / numPoints.toFloat() * 4f * PI.toFloat() * freqRatio
                    val sample = when (selectedWaveform) {
                        WaveformType.SINE -> sin(t)
                        WaveformType.SQUARE -> if (sin(t) >= 0) 1f else -1f
                        WaveformType.TRIANGLE -> (2.0 / PI * asin(sin(t))).toFloat()
                        WaveformType.SAWTOOTH -> (2.0 / PI * atan(tan(t / 2.0))).toFloat()
                    }
                    val y = midY - sample * (h * 0.25f)

                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = if (isPlaying) vibrantOrange else inactiveWaveColor,
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
            }

            // 2. FLOATING OVERLAYS (PROMINENT FREQUENCY READOUT, CONTROLS & PRESET CHIPS)
            val flatSurfaceBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Prominent Frequency Readout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (frequency >= 1000f) String.format(Locale.US, "%.1f", frequency / 1000f) else String.format(Locale.US, "%.0f", frequency),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 58.sp),
                            fontWeight = FontWeight.Black,
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            letterSpacing = (-1.5).sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (frequency >= 1000f) "kHz" else "Hz",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            color = vibrantOrange,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${selectedWaveform.label.uppercase()} WAVE  •  AUDIBLE SPECTRUM",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = vibrantOrange,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom Controls & Preset FlowRow Container
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Waveform Selector Pill
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
                            WaveformType.entries.forEach { wf ->
                                val isSelected = selectedWaveform == wf
                                val bgColor = if (isSelected) vibrantOrange else Color.Transparent
                                val txtColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

                                Box(
                                    modifier = Modifier
                                        .clip(ContinuousCapsule)
                                        .background(bgColor)
                                        .clickable {
                                            haptics.tick()
                                            selectedWaveform = wf
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = wf.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = txtColor,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }

                    // Frequency Slider
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ContinuousRoundedRectangle(16.dp))
                            .background(flatSurfaceBg)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "20 Hz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text(text = "20 kHz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Slider(
                            value = frequency,
                            onValueChange = { freq ->
                                frequency = freq
                            },
                            valueRange = 20f..20000f,
                            colors = SliderDefaults.colors(
                                thumbColor = vibrantOrange,
                                activeTrackColor = vibrantOrange
                            )
                        )
                    }

                    // Preset Chips FlowRow (Ensures clean multi-row wrapping if screen is narrow)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { (freq, label) ->
                            val isSelected = abs(frequency - freq) < 10f
                            Surface(
                                modifier = Modifier
                                    .clip(ContinuousCapsule)
                                    .clickable {
                                        haptics.click()
                                        frequency = freq
                                    },
                                shape = ContinuousCapsule,
                                color = if (isSelected) vibrantOrange.copy(alpha = 0.25f) else flatSurfaceBg,
                                border = BorderStroke(1.dp, if (isSelected) vibrantOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) vibrantOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // Main Play / Stop Button
                    Button(
                        onClick = {
                            haptics.click()
                            isPlaying = !isPlaying
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(elevation = 8.dp, shape = ContinuousCapsule),
                        shape = ContinuousCapsule,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlaying) MaterialTheme.colorScheme.surfaceVariant else vibrantOrange,
                            contentColor = if (isPlaying) MaterialTheme.colorScheme.onSurface else Color.White
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Text(
                                text = if (isPlaying) "STOP TONE GENERATOR" else "PLAY TONE GENERATOR",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
