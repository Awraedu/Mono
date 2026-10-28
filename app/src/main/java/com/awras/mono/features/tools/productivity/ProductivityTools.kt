package com.awras.mono.features.tools.productivity

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Timer
import com.awras.mono.core.model.Tool
import com.awras.mono.core.model.ToolCategory
import compose.icons.TablerIcons
import compose.icons.tablericons.ClipboardList
import compose.icons.tablericons.DeviceMobile
import compose.icons.tablericons.Lock
import compose.icons.tablericons.Plus

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
    ),
    Tool(
        id = "stepper-tally",
        name = "Stepper",
        description = "Tactile Multi-Counter & Tally Engine with Custom Step Sizes",
        tagline = "TALLY COUNTER",
        icon = TablerIcons.Plus,
        category = ToolCategory.PRODUCTIVITY,
        route = "stepper_tally",
        content = { onBackClick -> StepperScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "hardware-inspector",
        name = "Hardware",
        description = "System & Hardware Inspector with Real-time Diagnostics",
        tagline = "SYSTEM MANAGER",
        icon = TablerIcons.DeviceMobile,
        category = ToolCategory.PRODUCTIVITY,
        route = "hardware_inspector",
        content = { onBackClick -> HardwareScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "random-flip-dice",
        name = "Random",
        description = "Physics 3D Coin Flip & Multi-Dice Rolling Engine",
        tagline = "COIN & DICE",
        icon = Icons.Outlined.Casino,
        category = ToolCategory.PRODUCTIVITY,
        route = "random_flip_dice",
        content = { onBackClick -> RandomScreen(onBackClick = onBackClick) }
    )
)
