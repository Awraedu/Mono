package com.awras.mono.features.tools.productivity

import com.awras.mono.core.model.Tool
import com.awras.mono.core.model.ToolCategory
import compose.icons.TablerIcons
import compose.icons.tablericons.ClipboardList

val productivityTools = listOf(
    Tool(
        id = "clipboard-manager",
        name = "Clipboard Manager",
        description = "Adaptive Smart Clipboard Tracking, Link Previews & Hidden OTPs",
        tagline = "SMART CLIPBOARD",
        icon = TablerIcons.ClipboardList,
        category = ToolCategory.PRODUCTIVITY,
        route = "clipboard_manager",
        content = { onBackClick -> ClipboardManagerScreen(onBackClick = onBackClick) }
    )
)
