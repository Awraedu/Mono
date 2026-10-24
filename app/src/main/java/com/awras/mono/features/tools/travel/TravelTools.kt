package com.awras.mono.features.tools.travel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Terrain
import com.awras.mono.core.model.Tool
import com.awras.mono.core.model.ToolCategory
import compose.icons.TablerIcons
import compose.icons.tablericons.Cloud
import compose.icons.tablericons.Compass
import compose.icons.tablericons.Language
import compose.icons.tablericons.Sun

val travelTools = listOf(
    Tool(
        id = "climate-forecast",
        name = "Climate Forecast",
        description = "High-Precision Free Meteorological Forecast, Sunrise & Pollen",
        icon = TablerIcons.Cloud,
        category = ToolCategory.TRAVEL,
        route = "climate_forecast",
        content = { onBackClick -> ClimateScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "compass",
        name = "Compass",
        description = "Precision Magnetic & True North Navigation",
        icon = TablerIcons.Compass,
        category = ToolCategory.TRAVEL,
        route = "compass",
        content = { onBackClick -> CompassScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "altimeter",
        name = "Altimeter",
        description = "Precision Sea-Level Elevation, Barometer & DEM Height",
        icon = Icons.Outlined.Terrain,
        category = ToolCategory.TRAVEL,
        route = "altimeter",
        content = { onBackClick -> AltimeterScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "solar-tracker",
        name = "Solar Tracker",
        description = "Sun, Moon & Golden Hour Photography Inspector with Azimuth & Zenith",
        icon = TablerIcons.Sun,
        category = ToolCategory.TRAVEL,
        route = "solar_tracker",
        content = { onBackClick -> SolarTrackerScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "translator",
        name = "Translator",
        description = "Offline Neural Translation (ML Kit)",
        icon = TablerIcons.Language,
        category = ToolCategory.TRAVEL,
        route = "translator",
        content = { onBackClick -> TranslatorScreen(onBackClick = onBackClick) }
    )
)