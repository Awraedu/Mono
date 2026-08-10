package com.awras.mono.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awras.mono.ui.theme.LocalTactileFeedback
import com.kyant.capsule.ContinuousRoundedRectangle

@Composable
fun ToolCard(
    category: String,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalTactileFeedback.current
    val shape = ContinuousRoundedRectangle(24.dp)
    
    Box(
        modifier = modifier
            .aspectRatio(1.2f)
            .shadow(
                elevation = 2.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clip(shape)
            .background(
                // Use surface with slightly higher contrast
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            )
            .clickable {
                haptics.click()
                onClick()
            }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            
            Column {
                Text(
                    text = category.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), // Increased contrast
                    letterSpacing = 1.sp,
                    fontSize = 11.sp // Slightly larger
                )
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MaterialTheme.typography.labelSmall.fontFamily // GeistMono
                )
            }
        }
    }
}
