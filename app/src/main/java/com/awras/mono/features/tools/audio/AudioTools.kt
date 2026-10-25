package com.awras.mono.features.tools.audio

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import com.awras.mono.core.model.Tool
import com.awras.mono.core.model.ToolCategory
import compose.icons.TablerIcons
import compose.icons.tablericons.Activity
import compose.icons.tablericons.Music
import compose.icons.tablericons.Volume
import compose.icons.tablericons.WaveSine

val audioTools = listOf(
    Tool(
        id = "decibel-meter",
        name = "Decibel Meter",
        description = "Acoustic Sound Pressure Level & Noise Hazard Meter",
        tagline = "SOUND LEVEL",
        icon = TablerIcons.Volume,
        category = ToolCategory.AUDIO,
        route = "decibel_meter",
        content = { onBackClick -> DecibelMeterScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "chord-detector",
        name = "Chord Detector",
        description = "Real-time Polyphonic Chord & Chromagram Pitch Analyzer",
        tagline = "POLYPHONIC CHORD",
        icon = TablerIcons.Music,
        category = ToolCategory.AUDIO,
        route = "chord_detector",
        content = { onBackClick -> ChordDetectorScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "metronome",
        name = "Metronome",
        description = "Precision Audio Tempo Engine with Tap Tempo & Pendulum",
        tagline = "TEMPO ENGINE",
        icon = Icons.Default.GraphicEq,
        category = ToolCategory.AUDIO,
        route = "metronome",
        content = { onBackClick -> MetronomeScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "hertz-generator",
        name = "Hertz Generator",
        description = "Precision Audio Tone & Waveform Synthesizer",
        tagline = "TONE SYNTH",
        icon = TablerIcons.WaveSine,
        category = ToolCategory.AUDIO,
        route = "hertz_generator",
        content = { onBackClick -> HertzGeneratorScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "bpm-detector",
        name = "BPM Detector",
        description = "Automatic Microphone & Manual Tap Tempo Detector",
        tagline = "BEAT COUNTER",
        icon = TablerIcons.Activity,
        category = ToolCategory.AUDIO,
        route = "bpm_detector",
        content = { onBackClick -> BpmDetectorScreen(onBackClick = onBackClick) }
    )
)
