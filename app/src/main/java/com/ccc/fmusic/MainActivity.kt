package com.ccc.fmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Estado para controlar qué pantalla se muestra actualmente ("control" o "settings")
            var currentScreen by remember { mutableStateOf("control") }

            MaterialTheme {
                // Cambia de pantalla según el estado
                when (currentScreen) {
                    "control" -> MusicControllerScreen(
                        onNavigateToSettings = { currentScreen = "settings" }
                    )
                    "settings" -> SettingsScreen(
                        onNavigateBack = { currentScreen = "control" }
                    )
                }
            }
        }
    }
}