package com.awras.mono.features.tools.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awras.mono.ui.components.DatumToolTopBar
import com.awras.mono.ui.theme.LocalTactileFeedback
import com.kyant.capsule.ContinuousCapsule
import compose.icons.TablerIcons
import compose.icons.tablericons.Clock
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread
import kotlin.math.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val SAMPLE_RATE = 44100
private const val MIN_BPM = 30
private const val MAX_BPM = 300
private const val MAX_SWING_DEG = 22f
private const val BEATS_PER_BAR = 4

private val LN_MIN = ln(MIN_BPM.toDouble())
private val LN_MAX = ln(MAX_BPM.toDouble())

private fun bpmForT(t: Float): Int =
    exp(LN_MAX - t.coerceIn(0f, 1f) * (LN_MAX - LN_MIN)).roundToInt().coerceIn(MIN_BPM, MAX_BPM)

private fun tForBpm(bpm: Int): Float =
    ((LN_MAX - ln(bpm.toDouble())) / (LN_MAX - LN_MIN)).toFloat().coerceIn(0f, 1f)

private val TEMPO_MARKS = listOf(
    40, 42, 44, 46, 48, 50, 52, 54, 56, 58, 60, 63, 66, 69, 72, 76, 80, 84, 88, 92, 96, 100,
    104, 108, 112, 116, 120, 126, 132, 138, 144, 152, 160, 168, 176, 184, 192, 200, 208
)
private val MAJOR_MARKS = setOf(40, 60, 80, 100, 120, 144, 168, 200)

private fun roundedPolygon(points: List<Offset>, radius: Float): Path {
    val path = Path()
    val n = points.size
    for (i in 0 until n) {
        val prev = points[(i + n - 1) % n]
        val cur = points[i]
        val next = points[(i + 1) % n]
        val toPrev = prev - cur
        val toNext = next - cur
        val r = min(radius, min(toPrev.getDistance(), toNext.getDistance()) / 2f)
        val a = cur + toPrev / toPrev.getDistance() * r
        val b = cur + toNext / toNext.getDistance() * r
        if (i == 0) path.moveTo(a.x, a.y) else path.lineTo(a.x, a.y)
        path.quadraticTo(cur.x, cur.y, b.x, b.y)
    }
    path.close()
    return path
}

private class PendulumGeometry(
    width: Float,
    height: Float,
    margin: Float,
    pivotInset: Float,
    bobRadius: Float,
    baseHeight: Float
) {
    val topY = margin
    val botY = height - margin
    val plinthHeight = baseHeight
    val plinthTop = botY - baseHeight
    val bodyTop = topY + (plinthTop - topY) * 0.14f
    val bodyBotW = min(width * 0.9f, (plinthTop - bodyTop) * 0.78f)
    val bodyTopW = bodyBotW * 0.34f
    val pivot = Offset(width / 2f, plinthTop - pivotInset)
    val rodLength = (pivot.y - topY) * 0.97f
    val minDist = rodLength * 0.20f
    val maxDist = rodLength - bobRadius * 0.6f

    fun distForT(t: Float) = minDist + t * (maxDist - minDist)
    fun tForDist(d: Float) = ((d - minDist) / (maxDist - minDist)).coerceIn(0f, 1f)
}

private class MetronomeEngine(@Volatile var bpm: Int) {
    @Volatile private var running = false
    @Volatile private var track: AudioTrack? = null
    private val beatStarts = ConcurrentLinkedQueue<Long>()

    fun start() {
        if (running) return
        running = true
        thread(name = "MetronomeAudio", isDaemon = true) { loop() }
    }

    fun stop() {
        running = false
    }

    fun headFrames(): Long = try {
        (track?.playbackHeadPosition ?: 0).toLong() and 0xFFFFFFFFL
    } catch (_: Exception) {
        0L
    }

    fun consumeBeats(head: Long): Int {
        if (head <= 0L) return 0
        var count = 0
        while (true) {
            val start = beatStarts.peek() ?: break
            if (start <= head) {
                beatStarts.poll()
                count++
            } else break
        }
        return count
    }

    private fun loop() {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        } catch (_: Exception) {}

        val accent = synthClick(accent = true)
        val normal = synthClick(accent = false)
        val silence = ShortArray(CHUNK_FRAMES)

        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val t = try {
            buildTrack(max(minBuf * 2, 4096))
        } catch (e: Exception) {
            e.printStackTrace()
            running = false
            return
        }
        track = t

        try {
            t.play()
            var written = 0L
            var beatStart = 0.0
            var beat = 0

            while (running) {
                val click = if (beat % BEATS_PER_BAR == 0) accent else normal
                beatStarts.add(written)
                if (t.write(click, 0, click.size) < 0) break
                written += click.size

                var fpb = SAMPLE_RATE * 60.0 / bpm
                while (running) {
                    fpb = SAMPLE_RATE * 60.0 / bpm
                    val target = (beatStart + fpb).roundToLong()
                    val remaining = target - written
                    if (remaining <= 0) break
                    val len = min(remaining, CHUNK_FRAMES.toLong()).toInt()
                    if (t.write(silence, 0, len) < 0) { running = false; break }
                    written += len
                }
                beatStart = max(beatStart + fpb, written.toDouble())
                beat++
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            track = null
            try {
                t.pause()
                t.flush()
            } catch (_: Exception) {}
            try {
                t.release()
            } catch (_: Exception) {}
        }
    }

    private fun buildTrack(bufferBytes: Int): AudioTrack {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val format = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val builder = AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
        return builder.build()
    }

    private fun synthClick(accent: Boolean): ShortArray {
        val n = (SAMPLE_RATE * 0.045).toInt()
        val f1 = if (accent) 1800.0 else 1250.0
        val f2 = f1 * 2.4
        val gain = if (accent) 0.95 else 0.65
        val rnd = Random(42)
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val body = sin(2.0 * PI * f1 * t) * exp(-t / 0.006)
            val overtone = 0.5 * sin(2.0 * PI * f2 * t) * exp(-t / 0.003)
            val transient = (rnd.nextDouble() * 2.0 - 1.0) * exp(-t / 0.0008) * 0.6
            val s = (body + overtone + transient) * gain * 0.55
            out[i] = (s * Short.MAX_VALUE).roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private companion object {
        const val CHUNK_FRAMES = 512
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetronomeScreen(
    onBackClick: () -> Unit
) {
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    var tempoT by remember { mutableFloatStateOf(tForBpm(120)) }
    val bpm by remember { derivedStateOf { bpmForT(tempoT) } }
    val currentBpm by rememberUpdatedState(bpm)

    var isPlaying by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    var swingPos by remember { mutableFloatStateOf(0f) }
    val swingBlend = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            swingBlend.animateTo(0f, tween(350))
            return@LaunchedEffect
        }

        val engine = MetronomeEngine(currentBpm)
        engine.start()
        launch { swingBlend.animateTo(1f, tween(200)) }

        var totalBeats = 0
        var lastHead = 0L
        var pos = 0.0
        try {
            while (isActive) {
                withFrameNanos {
                    engine.bpm = currentBpm
                    val head = engine.headFrames()
                    val framesPerBeat = SAMPLE_RATE * 60.0 / currentBpm

                    if (totalBeats > 0) {
                        pos = min(pos + (head - lastHead) / framesPerBeat, (totalBeats - 1) + 0.999)
                    }
                    lastHead = head

                    val newBeats = engine.consumeBeats(head)
                    if (newBeats > 0) {
                        totalBeats += newBeats
                        pos = (totalBeats - 1).toDouble()
                    }
                    swingPos = (pos % 2.0).toFloat()
                }
            }
        } finally {
            engine.stop()
        }
    }

    val tempoMarking = when {
        bpm < 40 -> "GRAVE / VERY SLOW"
        bpm < 60 -> "LARGO / SLOW"
        bpm < 66 -> "LARGHETTO / BROAD"
        bpm < 76 -> "ADAGIO / LEISURELY"
        bpm < 108 -> "ANDANTE / WALKING TEMPO"
        bpm < 120 -> "MODERATO / MODERATE"
        bpm < 156 -> "ALLEGRO / BRISK"
        bpm < 176 -> "VIVACE / LIVELY"
        bpm < 200 -> "PRESTO / FAST"
        else -> "PRESTISSIMO / VERY FAST"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Metronome",
                description = "Precision Mechanical Tempo Engine",
                icon = androidx.compose.material.icons.Icons.Default.GraphicEq,
                onBackClick = onBackClick,
                rightActionIcon = Icons.Default.Refresh,
                onRightActionClick = {
                    haptics.click()
                    tempoT = tForBpm(120)
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$bpm",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 58.sp),
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
                        text = tempoMarking,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = vibrantOrange,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. TRAPEZOID HOUSING, PENDULUM ROD & DRAGGABLE WEIGHT
                val flatSurfaceBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                val guideColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                val inactiveArmColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                val inactiveBobColor = MaterialTheme.colorScheme.onBackground

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 12.dp)
                        .pointerInput(Unit) {
                            val bobRadius = 28.dp.toPx()
                            val margin = 16.dp.toPx()
                            val pivotInset = 44.dp.toPx()
                            val baseHeight = 22.dp.toPx()
                            var grabOffset = 0f

                            fun geometry() = PendulumGeometry(
                                size.width.toFloat(), size.height.toFloat(),
                                margin, pivotInset, bobRadius, baseHeight
                            )

                            fun applyDrag(position: Offset) {
                                val g = geometry()
                                val dist = (position - g.pivot).getDistance() + grabOffset
                                val newT = g.tForDist(dist)
                                val oldBpm = bpm
                                tempoT = newT
                                val newBpm = bpmForT(newT)
                                if (newBpm / 5 != oldBpm / 5) haptics.tick()
                            }

                            detectDragGestures(
                                onDragStart = { start ->
                                    val g = geometry()
                                    val fingerDist = (start - g.pivot).getDistance()
                                    val bobDist = g.distForT(tempoT)
                                    grabOffset = if (abs(fingerDist - bobDist) < bobRadius * 2.2f) {
                                        bobDist - fingerDist
                                    } else 0f
                                    isDragging = true
                                    applyDrag(start)
                                },
                                onDragEnd = { isDragging = false },
                                onDragCancel = { isDragging = false },
                                onDrag = { change, _ ->
                                    change.consume()
                                    applyDrag(change.position)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val bobRadius = 28.dp.toPx()
                        val g = PendulumGeometry(
                            w, h, 16.dp.toPx(), 44.dp.toPx(), bobRadius, 22.dp.toPx()
                        )
                        val cx = g.pivot.x

                        // --- Plinth (base) ---
                        val plinthW = g.bodyBotW * 1.1f
                        drawRoundRect(
                            color = guideColor.copy(alpha = 0.28f),
                            topLeft = Offset(cx - plinthW / 2f, g.plinthTop),
                            size = Size(plinthW, g.plinthHeight),
                            cornerRadius = CornerRadius(8.dp.toPx())
                        )
                        drawRoundRect(
                            color = guideColor.copy(alpha = 0.45f),
                            topLeft = Offset(cx - plinthW / 2f, g.plinthTop),
                            size = Size(plinthW, g.plinthHeight),
                            cornerRadius = CornerRadius(8.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx())
                        )

                        // --- Pyramid body (Solid flat background, NO gradient) ---
                        val bodyPath = roundedPolygon(
                            listOf(
                                Offset(cx - g.bodyTopW / 2f, g.bodyTop),
                                Offset(cx + g.bodyTopW / 2f, g.bodyTop),
                                Offset(cx + g.bodyBotW / 2f, g.plinthTop),
                                Offset(cx - g.bodyBotW / 2f, g.plinthTop)
                            ),
                            10.dp.toPx()
                        )
                        drawPath(path = bodyPath, color = flatSurfaceBg)
                        drawPath(
                            path = bodyPath,
                            color = guideColor.copy(alpha = 0.45f),
                            style = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round)
                        )

                        // --- Inset face plate ---
                        fun halfW(y: Float) = (g.bodyTopW +
                                (g.bodyBotW - g.bodyTopW) * ((y - g.bodyTop) / (g.plinthTop - g.bodyTop))) / 2f
                        val y1 = g.bodyTop + 18.dp.toPx()
                        val y2 = g.plinthTop - 16.dp.toPx()
                        val inset = 12.dp.toPx()
                        val facePath = roundedPolygon(
                            listOf(
                                Offset(cx - halfW(y1) + inset, y1),
                                Offset(cx + halfW(y1) - inset, y1),
                                Offset(cx + halfW(y2) - inset, y2),
                                Offset(cx - halfW(y2) + inset, y2)
                            ),
                            6.dp.toPx()
                        )
                        drawPath(path = facePath, color = flatSurfaceBg)
                        drawPath(
                            path = facePath,
                            color = guideColor.copy(alpha = 0.3f),
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // --- Static Tempo Scale Ticks (Fixed to background along vertical centerline) ---
                        for (mark in TEMPO_MARKS) {
                            val markDist = g.distForT(tForBpm(mark))
                            val tickY = g.pivot.y - markDist
                            val half = if (mark in MAJOR_MARKS) 9.dp.toPx() else 5.dp.toPx()
                            drawLine(
                                color = inactiveArmColor.copy(alpha = 0.65f),
                                start = Offset(cx - half, tickY),
                                end = Offset(cx + half, tickY),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }

                        // --- Swing Rod & Draggable Weight Bob ---
                        val angleDeg = MAX_SWING_DEG * cos(PI * swingPos).toFloat() * swingBlend.value
                        val rad = Math.toRadians(angleDeg.toDouble())
                        val dir = Offset(sin(rad).toFloat(), -cos(rad).toFloat())

                        val active = isPlaying || swingBlend.value > 0.01f
                        val rodColor = if (active) vibrantOrange else inactiveArmColor
                        val bobColor = if (active || isDragging) vibrantOrange else inactiveBobColor

                        // Rod
                        drawLine(
                            color = rodColor,
                            start = g.pivot - dir * 14.dp.toPx(),
                            end = g.pivot + dir * g.rodLength,
                            strokeWidth = 3.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Fixed counterweight below pivot
                        drawCircle(
                            color = rodColor,
                            radius = 9.dp.toPx(),
                            center = g.pivot - dir * 18.dp.toPx()
                        )

                        // Draggable weight bob
                        val bobCenter = g.pivot + dir * g.distForT(tempoT)
                        if (isDragging) {
                            drawCircle(
                                color = bobColor.copy(alpha = 0.18f),
                                radius = bobRadius * 1.45f,
                                center = bobCenter
                            )
                        }
                        drawCircle(color = bobColor, radius = bobRadius, center = bobCenter)
                        drawCircle(
                            color = Color.White.copy(alpha = 0.35f),
                            radius = bobRadius * 0.72f,
                            center = bobCenter,
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.9f),
                            radius = bobRadius * 0.30f,
                            center = bobCenter
                        )

                        // Pivot cap
                        drawCircle(color = vibrantOrange, radius = 9.dp.toPx(), center = g.pivot)
                        drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 3.dp.toPx(), center = g.pivot)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. START / STOP BUTTON
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
                            text = if (isPlaying) "STOP METRONOME" else "START METRONOME",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
