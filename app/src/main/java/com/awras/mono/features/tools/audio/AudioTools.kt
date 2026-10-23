package com.awras.mono.features.tools.audio

import com.awras.mono.core.model.Tool
import com.awras.mono.core.model.ToolCategory
import compose.icons.TablerIcons
import compose.icons.tablericons.Activity
import compose.icons.tablericons.Clock
import compose.icons.tablericons.Music
import compose.icons.tablericons.Volume
import compose.icons.tablericons.WaveSine

val audioTools = listOf(
    Tool(
        id = "decibel-meter",
        name = "Decibel Meter",
        description = "Acoustic Sound Pressure Level & Noise Hazard Meter",
        icon = TablerIcons.Volume,
        category = ToolCategory.AUDIO,
        route = "decibel_meter",
        content = { onBackClick -> DecibelMeterScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "chord-detector",
        name = "Chord Detector",
        description = "Real-time Polyphonic Chord & Chromagram Pitch Analyzer",
        icon = TablerIcons.Music,
        category = ToolCategory.AUDIO,
        route = "chord_detector",
        content = { onBackClick -> ChordDetectorScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "metronome",
        name = "Metronome",
        description = "Precision Audio Tempo Engine with Tap Tempo & Pendulum",
        icon = TablerIcons.Clock,
        category = ToolCategory.AUDIO,
        route = "metronome",
        content = { onBackClick -> MetronomeScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "hertz-generator",
        name = "Hertz Generator",
        description = "Precision Audio Tone & Waveform Synthesizer",
        icon = TablerIcons.WaveSine,
        category = ToolCategory.AUDIO,
        route = "hertz_generator",
        content = { onBackClick -> HertzGeneratorScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "bpm-detector",
        name = "BPM Detector",
        description = "Automatic Microphone & Manual Tap Tempo Detector",
        icon = TablerIcons.Activity,
        category = ToolCategory.AUDIO,
        route = "bpm_detector",
        content = { onBackClick -> BpmDetectorScreen(onBackClick = onBackClick) }
    )
)
