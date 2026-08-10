package com.awras.mono.ui.theme

import android.Manifest
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.math.exp
import kotlin.math.sin

val LocalTactileFeedback = staticCompositionLocalOf<TactileFeedback> {
    error("No TactileFeedback provided")
}

class TactileFeedback(context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    // 1. Light Tick: Fast, high-frequency "mechanical" switch
    @RequiresPermission(Manifest.permission.VIBRATE)
    fun tick() {
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(10)
            }
        }
        playSynthesizedClick(frequency = 2000.0, durationMs = 5, decayRate = 800.0)
    }

    // 2. Standard Click: Solid button engagement
    @RequiresPermission(Manifest.permission.VIBRATE)
    fun click() {
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(25)
            }
        }
        playSynthesizedClick(frequency = 1200.0, durationMs = 12, decayRate = 400.0)
    }

    // 3. Heavy Impact: For major actions or "locking" into state
    @RequiresPermission(Manifest.permission.VIBRATE)
    fun impact() {
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 40), -1)
            }
        }
        playSynthesizedClick(frequency = 600.0, durationMs = 25, decayRate = 150.0)
    }

    // 4. Success Pulse: Dual confirmation
    @RequiresPermission(Manifest.permission.VIBRATE)
    fun success() {
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 10, 50, 10), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 10, 50, 10), -1)
            }
        }
        playSynthesizedClick(frequency = 1500.0, durationMs = 10, decayRate = 300.0)
    }

    private fun playSynthesizedClick(
        frequency: Double,
        durationMs: Int,
        decayRate: Double
    ) {
        val sampleRate = 44100
        val numSamples = durationMs * sampleRate / 1000
        val sample = DoubleArray(numSamples)
        val buffer = ByteArray(2 * numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-t * decayRate)
            sample[i] = sin(2.0 * Math.PI * frequency * t) * decay
        }

        var idx = 0
        for (dVal in sample) {
            val shortVal = (dVal * 32767).toInt().toShort()
            buffer[idx++] = (shortVal.toInt() and 0x00ff).toByte()
            buffer[idx++] = (shortVal.toInt() and 0xff00 shr 8).toByte()
        }

        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
        } catch (_: Exception) {}
    }
}
