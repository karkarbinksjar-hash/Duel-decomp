package com.yugioh.recomp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugioh.recomp.core.engine.DdsCampaignManager
import com.yugioh.recomp.core.engine.GbaCampaignManager
import com.yugioh.recomp.core.rom.RomAssetCache
import com.yugioh.recomp.core.rom.RomPlatform
import com.yugioh.recomp.ui.theme.*

data class OpponentProfile(
    val name: String,
    val title: String,
    val quote: String,
    val avatarColor: Color,
    val difficulty: String,
    val keyCard: String
)

@Composable
fun CampaignHubScreen(
    onStartDuel: (String) -> Unit,
    onOpenDeckBuilder: () -> Unit,
    onReturnToRomImporter: () -> Unit
) {
    val isGba = RomAssetCache.activeTarget?.platform == RomPlatform.GBA

    var selectedOpponent by remember {
        mutableStateOf(if (isGba) GbaCampaignManager.selectedOpponentName else DdsCampaignManager.selectedOpponentName)
    }
    var isShaderEnabled by remember {
        mutableStateOf(if (isGba) false else DdsCampaignManager.isGbcLcdShaderEnabled)
    }

    val ddsOpponents = remember {
        listOf(
            OpponentProfile(
                name = "Tristan Taylor",
                title = "Fellow Novice",
                quote = "I'm still learning, but my Shadow Specter won't back down!",
                avatarColor = Color(0xFF64748B),
                difficulty = "★☆☆☆☆",
                keyCard = "Oscillo Hero #2"
            ),
            OpponentProfile(
                name = "Joey Wheeler",
                title = "School's 2nd Best",
                quote = "I'm JOEY! If ya wanna face the best, ya gotta get through me first!",
                avatarColor = Color(0xFFF59E0B),
                difficulty = "★★☆☆☆",
                keyCard = "Red-Eyes B. Dragon"
            ),
            OpponentProfile(
                name = "Mai Valentine",
                title = "Harpie Duelist",
                quote = "A rookie like you won't survive the talons of my Harpie Lady!",
                avatarColor = Color(0xFFEC4899),
                difficulty = "★★★☆☆",
                keyCard = "Harpie Lady"
            ),
            OpponentProfile(
                name = "Bakura",
                title = "Occult Mystery",
                quote = "My Millennium Ring senses a dark duel approaching...",
                avatarColor = Color(0xFF8B5CF6),
                difficulty = "★★★☆☆",
                keyCard = "White Magical Hat"
            ),
            OpponentProfile(
                name = "Seto Kaiba",
                title = "KaibaCorp President",
                quote = "I am a level above your standard duelist. This is your end!",
                avatarColor = Color(0xFFDC2626),
                difficulty = "★★★★★",
                keyCard = "Blue-Eyes White Dragon"
            )
        )
    }

    val opponents = if (isGba) GbaCampaignManager.opponents else ddsOpponents

    val totalCost = if (isGba) 0 else DdsCampaignManager.totalDeckCost()
    val maxDc = if (isGba) 0 else DdsCampaignManager.deckCapacity
    val duelistLevel = if (isGba) 0 else DdsCampaignManager.duelistLevel
    val activeDeckSize = if (isGba) GbaCampaignManager.activeDeck.size else DdsCampaignManager.activeDeck.size
    val isDeckReady = if (isGba) activeDeckSize in 40..60 else (activeDeckSize == 40 && totalCost <= maxDc)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkFieldBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Header: Title & GBA/GBC Mode Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "DUEL-DECOMP",
                            color = MillenniumGold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = if (isGba) Color(0xFF4338CA) else Color(0xFF047857),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (isGba) " GBA 16-COLOR " else " GBC 2BPP ",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = if (isGba) "BATTLE CITY TOURNAMENT - TCG EXPERT RULES" else "STAGE 1: DOMINO TOWN (DDS RULES)",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Screen Shader Toggle Button
                Button(
                    onClick = {
                        isShaderEnabled = !isShaderEnabled
                        DdsCampaignManager.isGbcLcdShaderEnabled = isShaderEnabled
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isShaderEnabled) Color(0xFF047857) else Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isShaderEnabled) "📺 LCD: ON" else "📺 LCD: OFF",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Player Profile Bar
            Surface(
                color = Color(0xFF131826),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isGba) Color(0xFF6366F1) else MillenniumGold, RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isGba) Color(0xFF4F46E5) else Color(0xFF2563EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Y", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Yugi Moto",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (isGba) "Rules: TCG Expert (Tributes Enabled)" else "Duelist Level: LV $duelistLevel",
                                color = if (isGba) Color(0xFFA5B4FC) else MillenniumGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        if (isGba) {
                            Text(
                                text = "Format: Battle City",
                                color = Color(0xFF86EFAC),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            )
                            Text(
                                text = "Cards: $activeDeckSize / 40",
                                color = if (activeDeckSize in 40..60) Color(0xFF93C5FD) else AttackRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        } else {
                            Text(
                                text = "Deck: $totalCost / $maxDc DC",
                                color = if (totalCost <= maxDc) Color(0xFF86EFAC) else AttackRed,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Cards: $activeDeckSize / 40",
                                color = if (activeDeckSize == 40) Color(0xFF93C5FD) else AttackRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Quick Buttons: Deck Construction & Change ROM
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenDeckBuilder,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color(0xFF475569), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = MillenniumGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isGba) "GBA Deck Construction" else "Deck Construction", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onReturnToRomImporter,
                    modifier = Modifier
                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF475569), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reload ROM", tint = Color(0xFF94A3B8))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isGba) "SELECT BATTLE CITY OPPONENT:" else "SELECT OPPONENT:",
                color = Color(0xFFCBD5E1),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Opponent Selection List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(opponents) { opp ->
                    val isSelected = selectedOpponent == opp.name
                    val wins = if (isGba) GbaCampaignManager.opponentWins[opp.name] ?: 0 else DdsCampaignManager.opponentWins[opp.name] ?: 0

                    Surface(
                        color = if (isSelected) Color(0xFF1E2538) else Color(0xFF111420),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) (if (isGba) Color(0xFF818CF8) else MillenniumGold) else Color(0xFF242C40),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedOpponent = opp.name
                                if (isGba) {
                                    GbaCampaignManager.selectedOpponentName = opp.name
                                } else {
                                    DdsCampaignManager.selectedOpponentName = opp.name
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(opp.avatarColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = opp.name.first().toString(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = opp.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Wins: $wins",
                                        color = if (wins >= 3) LifePointGreen else MillenniumGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = opp.quote,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 9.5.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    maxLines = 1
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Tier: ${opp.title} (${opp.difficulty})",
                                        color = Color(0xFFF59E0B),
                                        fontSize = 9.sp
                                    )
                                    Text(
                                        text = "Trump: ${opp.keyCard}",
                                        color = Color(0xFF60A5FA),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Start Duel Button
            Button(
                onClick = {
                    if (isDeckReady) {
                        onStartDuel(selectedOpponent)
                    }
                },
                enabled = isDeckReady,
                colors = ButtonDefaults.buttonColors(containerColor = AttackRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(1.5.dp, if (isDeckReady) MillenniumGold else Color.Transparent, RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = if (isDeckReady) "⚔ START DUEL vs $selectedOpponent" else "Fix 40-Card Deck to Duel",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
        }

        // Optional LCD Shader overlay
        if (isShaderEnabled) {
            GbcShaderOverlay(alpha = 0.18f)
        }
    }
}
