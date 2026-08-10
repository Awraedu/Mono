package com.awras.mono.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun DatumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Defaults to system, dark recommended
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DatumDarkColorScheme else DatumLightColorScheme
    val view = LocalView.current
    val context = LocalContext.current
    val tactileFeedback = remember { TactileFeedback(context) }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalTactileFeedback provides tactileFeedback
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DatumTypography,
            content = content
        )
    }
}
