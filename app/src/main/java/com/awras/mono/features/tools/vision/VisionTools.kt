package com.awras.mono.features.tools.vision

import com.awras.mono.core.model.Tool
import com.awras.mono.core.model.ToolCategory
import compose.icons.TablerIcons
import compose.icons.tablericons.Qrcode

val visionTools = listOf(
    Tool(
        id = "qr-scanner",
        name = "QR Scanner",
        description = "Instant QR code & barcode reader with URL actions",
        icon = TablerIcons.Qrcode,
        category = ToolCategory.VISION,
        route = "qr_scanner",
        content = { onBackClick -> QrScannerScreen(onBackClick = onBackClick) }
    )
)
