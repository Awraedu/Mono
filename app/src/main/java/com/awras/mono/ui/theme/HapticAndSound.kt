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

    // 1. Apple-Style Micro Tick (For scrolling lists or drag sliders)
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
        playSubtleAudioClick()
    }

    // 2. Firm Click (For pressing a card or executing an action)
    @RequiresPermission(Manifest.permission.VIBRATE)
    fun click() {
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(20)
            }
        }
        playSubtleAudioClick()
    }

    // 3. Heavy Snap Pulse (For level lock, zero-alignment, or timer finish)
    @RequiresPermission(Manifest.permission.VIBRATE)
    fun heavySnap() {
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 15, 10, 15), -1)
            }
        }
        playSubtleAudioClick()
    }

    // 4. Synthesized Mechanical Micro-Sound (Generated in RAM, zero asset latency)
    fun playSubtleAudioClick() {
        val sampleRate = 44100
        val durationMs = 8 // Extremely short pulse
        val numSamples = durationMs * sampleRate / 1000
        val sample = DoubleArray(numSamples)
        val buffer = ByteArray(2 * numSamples)

        // Generate a fast 1kHz sine wave pulse that decays quickly
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-t * 500) // Fast exponential drop-off
            sample[i] = sin(2.0 * Math.PI * 1000.0 * t) * decay
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
        } catch (_: Exception) {
            // Fallback gracefully if audio hardware is occupied
        }
    }
}
