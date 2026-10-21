package com.awras.mono.core.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

data class Tool(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val category: ToolCategory,
    val route: String,
    val content: @Composable (onBackClick: () -> Unit) -> Unit
)