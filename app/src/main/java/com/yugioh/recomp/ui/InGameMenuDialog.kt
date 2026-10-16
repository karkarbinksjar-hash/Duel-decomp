package com.yugioh.recomp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yugioh.recomp.core.engine.DuelPhase
import com.yugioh.recomp.ui.theme.AttackRed
import com.yugioh.recomp.ui.theme.LifePointGreen
import com.yugioh.recomp.ui.theme.MillenniumGold

/**
 * Authentic Game Boy Advance (GBA EDS) styled In-Game Pause & Duel Menu.
 * Allows the player to pause at any time, surrender/concede, restart the match,
 * view combat logs, check graveyards, toggle sound FX & CRT filters, or exit to hub.
 */
@Composable
fun InGameMenuDialog(
    opponentName: String,
    turnNumber: Int,
    currentPhase: DuelPhase,
    playerLp: Int,
    opponentLp: Int,
    playerDeckCount: Int,
    opponentDeckCount: Int,
    playerGyCount: Int,
    opponentGyCount: Int,
    combatLogs: List<String>,
    isGba: Boolean,
    isSfxEnabled: Boolean,
    onToggleSfx: () -> Unit,
    isShaderEnabled: Boolean,
    onToggleShader: () -> Unit,
    isFastAiEnabled: Boolean,
    onToggleFastAi: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onSurrender: () -> Unit,
    onOpenPlayerGy: () -> Unit,
    onOpenOpponentGy: () -> Unit,
    onExitToHub: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("MENU") } // "MENU", "LOG", "SETTINGS"
    var confirmAction by remember { mutableStateOf<String?>(null) } // "RESTART", "SURRENDER", "EXIT_HUB"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color(0xF2090D18),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .border(
                    BorderStroke(1.5.dp, Brush.linearGradient(listOf(MillenniumGold, Color(0xFF64748B), MillenniumGold))),
                    RoundedCornerShape(16.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar with Millennium Eye Emblem and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Brush.radialGradient(listOf(MillenniumGold, Color(0xFF92400E))), CircleShape)
                                .border(1.dp, Color(0xFFFEF08A), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👁", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isGba) "BATTLE CITY MENU" else "DUEL MENU",
                            color = MillenniumGold,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = onResume,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("✕", color = Color(0xFF94A3B8), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scoreboard Summary Panel
                Surface(
                    color = Color(0xFF131A2B),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF26324D), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Player Info
                        Column {
                            Text("YUGI MUTO", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "$playerLp LP",
                                color = if (playerLp > 3000) LifePointGreen else if (playerLp > 1500) Color(0xFFF59E0B) else AttackRed,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black
                            )
                            Text("🎴 $playerDeckCount • GY $playerGyCount", color = Color(0xFF94A3B8), fontSize = 9.sp)
                        }

                        // Match Status
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚔ VS ⚔", color = MillenniumGold, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text("Turn $turnNumber", color = Color(0xFFE2E8F0), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(currentPhase.shortName, color = Color(0xFF38BDF8), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                        }

                        // Opponent Info
                        Column(horizontalAlignment = Alignment.End) {
                            Text(opponentName.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "$opponentLp LP",
                                color = if (opponentLp > 3000) LifePointGreen else if (opponentLp > 1500) Color(0xFFF59E0B) else AttackRed,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black
                            )
                            Text("🎴 $opponentDeckCount • GY $opponentGyCount", color = Color(0xFF94A3B8), fontSize = 9.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = listOf("MENU" to "☰ Options", "LOG" to "📜 Duel Log", "SETTINGS" to "⚙ Settings")
                    for ((tabKey, tabLabel) in tabs) {
                        val isSelected = selectedTab == tabKey
                        Surface(
                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFF162035),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = tabKey }
                                .border(1.dp, if (isSelected) Color.White else Color(0xFF26324D), RoundedCornerShape(6.dp))
                        ) {
                            Text(
                                text = tabLabel,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content or Confirmation
                if (confirmAction != null) {
                    // Confirmation View
                    Surface(
                        color = Color(0xFF1E1420),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AttackRed, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val (title, desc) = when (confirmAction) {
                                "RESTART" -> Pair("Restart Match?", "The duel will be reset against $opponentName with fresh 8000 LP and 5 cards in hand.")
                                "SURRENDER" -> Pair("Surrender Duel?", "You will forfeit this match against $opponentName and return to the Campaign Hub.")
                                "EXIT_HUB" -> Pair("Exit to Campaign Hub?", "Abandon current match progress and return to the Campaign Hub?")
                                else -> Pair("Confirm Action", "Are you sure?")
                            }
                            Text(title, color = AttackRed, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(desc, color = Color(0xFFE2E8F0), fontSize = 11.5.sp, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { confirmAction = null },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        when (confirmAction) {
                                            "RESTART" -> onRestart()
                                            "SURRENDER" -> onSurrender()
                                            "EXIT_HUB" -> onExitToHub()
                                        }
                                        confirmAction = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (confirmAction == "RESTART") Color(0xFFD97706) else AttackRed),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Confirm", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    when (selectedTab) {
                        "MENU" -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Resume Button
                                Button(
                                    onClick = onResume,
                                    colors = ButtonDefaults.buttonColors(containerColor = MillenniumGold),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(42.dp)
                                ) {
                                    Text("▶ RESUME DUEL", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                }

                                // Restart Button
                                Button(
                                    onClick = { confirmAction = "RESTART" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(38.dp)
                                ) {
                                    Text("🔄 RESTART DUEL", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // Graveyards Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onResume()
                                            onOpenPlayerGy()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2842)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Text("⚰ Player GY ($playerGyCount)", color = Color(0xFF93C5FD), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            onResume()
                                            onOpenOpponentGy()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2842)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Text("⚰ Opponent GY ($opponentGyCount)", color = Color(0xFFFCA5A5), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Surrender Button
                                Button(
                                    onClick = { confirmAction = "SURRENDER" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(38.dp)
                                ) {
                                    Text("🏳 SURRENDER MATCH", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // Exit to Hub Button
                                Button(
                                    onClick = { confirmAction = "EXIT_HUB" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(38.dp)
                                ) {
                                    Text("🚪 RETURN TO CAMPAIGN HUB", color = Color(0xFFE2E8F0), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        "LOG" -> {
                            Surface(
                                color = Color(0xFF090D18),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                if (combatLogs.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("No combat actions logged yet.", color = Color.Gray, fontSize = 11.sp)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(combatLogs.size) { idx ->
                                            val log = combatLogs[idx]
                                            val logColor = when {
                                                log.startsWith("---") -> MillenniumGold
                                                log.contains("destroyed") || log.contains("damage") -> Color(0xFFFCA5A5)
                                                log.contains("Summon") -> Color(0xFF86EFAC)
                                                log.contains("FLIP") -> Color(0xFF38BDF8)
                                                else -> Color(0xFFE2E8F0)
                                            }
                                            Text(
                                                text = "• $log",
                                                color = logColor,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                lineHeight = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        "SETTINGS" -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // SFX Toggle
                                Surface(
                                    color = Color(0xFF131A2B),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleSfx() }
                                        .border(1.dp, Color(0xFF26324D), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Sound Effects (SFX)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("8-bit retro audio for cards & combat", color = Color(0xFF94A3B8), fontSize = 9.5.sp)
                                        }
                                        Text(
                                            text = if (isSfxEnabled) "🔊 ON" else "🔇 OFF",
                                            color = if (isSfxEnabled) LifePointGreen else Color(0xFFEF4444),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                // Fast AI Battle Speed Toggle
                                Surface(
                                    color = Color(0xFF131A2B),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleFastAi() }
                                        .border(1.dp, Color(0xFF26324D), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("AI Battle Speed", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("Speed up opponent turn animations", color = Color(0xFF94A3B8), fontSize = 9.5.sp)
                                        }
                                        Text(
                                            text = if (isFastAiEnabled) "⚡ 2x FAST" else "⏳ 1x NORMAL",
                                            color = if (isFastAiEnabled) Color(0xFF38BDF8) else Color(0xFFE2E8F0),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                // Retro Scanlines Shader Toggle
                                Surface(
                                    color = Color(0xFF131A2B),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleShader() }
                                        .border(1.dp, Color(0xFF26324D), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("CRT / Dot-Matrix Filter", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("Retro scanline display simulation", color = Color(0xFF94A3B8), fontSize = 9.5.sp)
                                        }
                                        Text(
                                            text = if (isShaderEnabled) "📺 ON" else "📺 OFF",
                                            color = if (isShaderEnabled) LifePointGreen else Color(0xFF64748B),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
