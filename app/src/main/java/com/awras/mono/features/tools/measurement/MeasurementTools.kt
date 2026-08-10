package com.awras.mono.features.tools.measurement

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material3.Text
import com.awras.mono.core.model.Tool
import com.awras.mono.core.model.ToolCategory

val measurementTools = listOf(
    Tool(
        id = "unit-converter",
        name = "Converter",
        description = "Convert between various units of measurement",
        icon = Icons.Default.SquareFoot,
        category = ToolCategory.MEASUREMENT,
        route = "unit_converter",
        content = { Text("Unit Converter Tool") }
    )
)