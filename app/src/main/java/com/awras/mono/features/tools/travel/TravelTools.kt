package com.awras.mono.features.tools.travel

import com.awras.mono.core.model.Tool
import com.awras.mono.core.model.ToolCategory
import compose.icons.TablerIcons
import compose.icons.tablericons.Cloud
import compose.icons.tablericons.Compass

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
        icon = TablerIcons.Compass,
        category = ToolCategory.TRAVEL,
        route = "altimeter",
        content = { onBackClick -> AltimeterScreen(onBackClick = onBackClick) }
    )
)
