package com.awras.mono.features.tools.travel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
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
        tagline = "WEATHER FORECAST",
        icon = TablerIcons.Cloud,
        category = ToolCategory.TRAVEL,
        route = "climate_forecast",
        content = { onBackClick -> ClimateScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "compass",
        name = "Compass",
        description = "Precision Magnetic & True North Navigation",
        tagline = "MAGNETIC NAV",
        icon = TablerIcons.Compass,
        category = ToolCategory.TRAVEL,
        route = "compass",
        content = { onBackClick -> CompassScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "altimeter",
        name = "Altimeter",
        description = "Precision Sea-Level Elevation, Barometer & DEM Height",
        tagline = "ELEVATION BAROMETER",
        icon = Icons.Outlined.Terrain,
        category = ToolCategory.TRAVEL,
        route = "altimeter",
        content = { onBackClick -> AltimeterScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "solar-tracker",
        name = "Solar Tracker",
        description = "Sun, Moon & Golden Hour Photography Inspector with Azimuth & Zenith",
        tagline = "SUN AZIMUTH",
        icon = TablerIcons.Sun,
        category = ToolCategory.TRAVEL,
        route = "solar_tracker",
        content = { onBackClick -> SolarTrackerScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "translator",
        name = "Translator",
        description = "Offline Neural Translation (ML Kit)",
        tagline = "NEURAL TRANSLATE",
        icon = TablerIcons.Language,
        category = ToolCategory.TRAVEL,
        route = "translator",
        content = { onBackClick -> TranslatorScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "timezone",
        name = "Time Zone",
        description = "Global Timezone Inspector & Interactive Time Converter",
        tagline = "GLOBAL TIME",
        icon = Icons.Outlined.Public,
        category = ToolCategory.TRAVEL,
        route = "timezone",
        content = { onBackClick -> TimeZoneScreen(onBackClick = onBackClick) }
    )
)
