package com.awras.mono

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import com.awras.mono.core.model.Tool
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
                var showSplash by remember { mutableStateOf(true) }
                var currentScreen by remember { mutableStateOf("home") }
                var activeTool by remember { mutableStateOf<Tool?>(null) }

                Crossfade(
                    targetState = Pair(showSplash, activeTool),
                    animationSpec = tween(durationMillis = 400),
                    label = "AppNavigation",
                ) { (isSplashVisible, currentTool) ->
                    if (isSplashVisible) {
                        SplashScreen { showSplash = false }
                    } else if (currentTool != null) {
                        currentTool.content { activeTool = null }
                    } else {
                        when (currentScreen) {
                            "home" -> HomeScreen(
                                onExploreClick = { currentScreen = "library" },
                                onToolClick = { activeTool = it }
                            )
                            "library" -> ToolLibraryScreen(
                                onBackClick = { currentScreen = "home" },
                                onToolClick = { activeTool = it }
                            )
                        }
                    }
                }
            }
        }
    }
}
