package com.awras.mono.features.tools.productivity

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import com.awras.mono.core.model.Tool
import com.awras.mono.core.model.ToolCategory
import compose.icons.TablerIcons
import compose.icons.tablericons.ClipboardList
import compose.icons.tablericons.Lock

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
    ),
    Tool(
        id = "password-generator",
        name = "Password Generator",
        description = "Cryptographically Secure Password & Key Generator (PassGen)",
        tagline = "SECURE PASSGEN",
        icon = TablerIcons.Lock,
        category = ToolCategory.PRODUCTIVITY,
        route = "password_generator",
        content = { onBackClick -> PasswordGeneratorScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "chrono",
        name = "Chrono",
        description = "Luxury Adaptive Horology Chronograph",
        tagline = "STOPWATCH & TIMER",
        icon = Icons.Outlined.Timer,
        category = ToolCategory.PRODUCTIVITY,
        route = "chrono",
        content = { onBackClick -> ChronoScreen(onBackClick = onBackClick) }
    )
)
