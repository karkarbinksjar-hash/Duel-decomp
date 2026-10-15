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
import com.yugioh.recomp.core.engine.DdsCampaignManager
import com.yugioh.recomp.ui.CampaignHubScreen
import com.yugioh.recomp.ui.DeckConstructionScreen
import com.yugioh.recomp.ui.DuelScreen
import com.yugioh.recomp.ui.RomImportScreen
import com.yugioh.recomp.ui.theme.DarkFieldBackground
import com.yugioh.recomp.ui.theme.YuGiOhRecompTheme

enum class AppScreen {
    ROM_IMPORT,
    CAMPAIGN_HUB,
    DECK_BUILDER,
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
                                        currentScreen = AppScreen.CAMPAIGN_HUB
                                    }
                                )
                            }
                            AppScreen.CAMPAIGN_HUB -> {
                                CampaignHubScreen(
                                    onStartDuel = { opponentName ->
                                        DdsCampaignManager.selectedOpponentName = opponentName
                                        currentScreen = AppScreen.DUEL_ARENA
                                    },
                                    onOpenDeckBuilder = {
                                        currentScreen = AppScreen.DECK_BUILDER
                                    },
                                    onReturnToRomImporter = {
                                        currentScreen = AppScreen.ROM_IMPORT
                                    }
                                )
                            }
                            AppScreen.DECK_BUILDER -> {
                                DeckConstructionScreen(
                                    onNavigateBack = {
                                        currentScreen = AppScreen.CAMPAIGN_HUB
                                    }
                                )
                            }
                            AppScreen.DUEL_ARENA -> {
                                DuelScreen(
                                    onNavigateBackToRom = {
                                        currentScreen = AppScreen.CAMPAIGN_HUB
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
