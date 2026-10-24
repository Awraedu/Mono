package com.awras.mono.features.tools.vision

import com.awras.mono.core.model.Tool
import com.awras.mono.core.model.ToolCategory
import compose.icons.TablerIcons
import compose.icons.tablericons.Bulb
import compose.icons.tablericons.ColorSwatch
import compose.icons.tablericons.FileText
import compose.icons.tablericons.Qrcode
import compose.icons.tablericons.Stack

val visionTools = listOf(
    Tool(
        id = "qr-scanner",
        name = "QR Scanner",
        description = "Instant QR code & barcode reader with URL actions",
        icon = TablerIcons.Qrcode,
        category = ToolCategory.VISION,
        route = "qr_scanner",
        content = { onBackClick -> QrScannerScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "text-extractor",
        name = "Text Extractor",
        description = "AI Optical Character Recognition (OCR) from Photos",
        icon = TablerIcons.FileText,
        category = ToolCategory.VISION,
        route = "text_extractor",
        content = { onBackClick -> TextExtractorScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "object-counter",
        name = "Object Counter",
        description = "AI Real-time Object Detection & Tracking Counter",
        icon = TablerIcons.Stack,
        category = ToolCategory.VISION,
        route = "object_counter",
        content = { onBackClick -> ObjectCounterScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "color-picker",
        name = "Color Picker",
        description = "Real-time surface color inspector with Hex & RGB breakdown",
        icon = TablerIcons.ColorSwatch,
        category = ToolCategory.VISION,
        route = "color_picker",
        content = { onBackClick -> ColorPickerScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "document-scanner",
        name = "Document Scanner",
        description = "AI Document Scanner with Edge Detection & PDF Export",
        icon = TablerIcons.FileText,
        category = ToolCategory.VISION,
        route = "document_scanner",
        content = { onBackClick -> DocumentScannerScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "lux-meter",
        name = "Lux Meter",
        description = "Precision ambient illuminance meter in Lux & Foot-Candle",
        icon = TablerIcons.Bulb,
        category = ToolCategory.VISION,
        route = "lux_meter",
        content = { onBackClick -> LuxMeterScreen(onBackClick = onBackClick) }
    )
)
