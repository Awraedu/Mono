package com.awras.mono.features.tools.measurement

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SquareFoot
import com.awras.mono.core.model.Tool
import com.awras.mono.core.model.ToolCategory
import com.awras.mono.features.tools.maths.ConverterScreen
import compose.icons.TablerIcons
import compose.icons.tablericons.Activity
import compose.icons.tablericons.Anchor
import compose.icons.tablericons.Ruler
import compose.icons.tablericons.Ruler2
import compose.icons.tablericons.ToggleLeft
import compose.icons.tablericons.ViewportWide

val measurementTools = listOf(
    Tool(
        id = "onscreen-ruler",
        name = "Onscreen Ruler",
        description = "Precision cm/inch ruler with per-device DPI calibration",
        tagline = "LINEAR SCALE",
        icon = TablerIcons.Ruler,
        category = ToolCategory.MEASUREMENT,
        route = "onscreen_ruler",
        content = { onBackClick -> RulerScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "spirit-level",
        name = "Level",
        description = "2D Bullseye & Surface Inclination Level with Relative Zero",
        tagline = "BULLSEYE LEVEL",
        icon = TablerIcons.ViewportWide,
        category = ToolCategory.MEASUREMENT,
        route = "spirit_level",
        content = { onBackClick -> SpiritLevelScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "angle-protractor",
        name = "Protractor",
        description = "Surface Inclinometer & Touch Screen Angle Finder",
        tagline = "ANGLE FINDER",
        icon = TablerIcons.Ruler2,
        category = ToolCategory.MEASUREMENT,
        route = "angle_protractor",
        content = { onBackClick -> AngleScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "plumb-bob",
        name = "Plumb Bob",
        description = "Gravity Vertical Alignment & Structure Inspector",
        tagline = "GRAVITY PLUMB",
        icon = TablerIcons.Anchor,
        category = ToolCategory.MEASUREMENT,
        route = "plumb_bob",
        content = { onBackClick -> PlumbBobScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "seismograph",
        name = "Seismograph",
        description = "Real-time Vibration & Shock Accelerometer Meter",
        tagline = "SHOCK VIBRATION",
        icon = TablerIcons.Activity,
        category = ToolCategory.MEASUREMENT,
        route = "seismograph",
        content = { onBackClick -> SeismographScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "metal-detector",
        name = "Metal Detector",
        description = "Ferromagnetic Flux Proximity Sensor",
        tagline = "MAGNETIC FLUX",
        icon = TablerIcons.ToggleLeft,
        category = ToolCategory.MEASUREMENT,
        route = "metal_detector",
        content = { onBackClick -> MetalDetectorScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "unit-converter",
        name = "Converter",
        description = "Universal Unit Scale & Dimensional Converter with Built-in Numpad",
        tagline = "UNIT SCALE",
        icon = Icons.Default.SquareFoot,
        category = ToolCategory.MEASUREMENT,
        route = "unit_converter",
        content = { onBackClick -> ConverterScreen(onBackClick = onBackClick) }
    )
)
