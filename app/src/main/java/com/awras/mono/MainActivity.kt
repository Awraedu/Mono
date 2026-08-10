package com.awras.mono

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import com.awras.mono.features.home.HomeScreen
import com.awras.mono.features.library.ToolLibraryScreen
import com.awras.mono.features.splash.SplashScreen
import com.awras.mono.ui.theme.DatumTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DatumTheme {
                var showSplash by remember { mutableStateOf(value = true) }
                var currentScreen by remember { mutableStateOf("home") }

                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(durationMillis = 800),
                    label = "AppNavigation",
                ) { isSplashVisible ->
                    if (isSplashVisible) {
                        SplashScreen { showSplash = false }
                    } else {
                        when (currentScreen) {
                            "home" -> HomeScreen(
                                onExploreClick = { currentScreen = "library" }
                            )
                            "library" -> ToolLibraryScreen(
                                onBackClick = { currentScreen = "home" }
                            )
                        }
                    }
                }
            }
        }
    }
}
