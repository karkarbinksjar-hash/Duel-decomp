package com.yugioh.recomp

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.yugioh.recomp.ui.DuelScreen
import com.yugioh.recomp.ui.RomImportScreen
import com.yugioh.recomp.ui.theme.DarkFieldBackground
import com.yugioh.recomp.ui.theme.YuGiOhRecompTheme

enum class AppScreen {
    ROM_IMPORT,
    DUEL_ARENA
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Strict Mobile UX Constraint: Force Portrait Mode
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContent {
            YuGiOhRecompTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.ROM_IMPORT) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkFieldBackground
                ) {
                    Crossfade(
                        targetState = currentScreen,
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            AppScreen.ROM_IMPORT -> {
                                RomImportScreen(
                                    onDuelReady = {
                                        currentScreen = AppScreen.DUEL_ARENA
                                    }
                                )
                            }
                            AppScreen.DUEL_ARENA -> {
                                DuelScreen(
                                    onNavigateBackToRom = {
                                        currentScreen = AppScreen.ROM_IMPORT
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
