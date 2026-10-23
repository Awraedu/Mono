package com.awras.mono.features.tools.travel

import com.awras.mono.core.model.Tool
import com.awras.mono.core.model.ToolCategory
import compose.icons.TablerIcons
import compose.icons.tablericons.Cloud

val travelTools = listOf(
    Tool(
        id = "climate-forecast",
        name = "Climate Forecast",
        description = "High-Precision Free Meteorological Forecast, Sunrise & Pollen",
        icon = TablerIcons.Cloud,
        category = ToolCategory.TRAVEL,
        route = "climate_forecast",
        content = { onBackClick -> ClimateScreen(onBackClick = onBackClick) }
    )
)
