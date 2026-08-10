package com.awras.mono

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.awras.mono.features.home.HomeScreen
import com.awras.mono.features.library.ToolLibraryScreen
import com.awras.mono.ui.theme.DatumTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DatumTheme {
                var currentScreen by remember { mutableStateOf("home") }

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
