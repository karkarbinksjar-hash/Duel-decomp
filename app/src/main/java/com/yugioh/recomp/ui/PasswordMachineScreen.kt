package com.yugioh.recomp.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugioh.recomp.core.audio.ChiptuneSynthesizer
import com.yugioh.recomp.core.engine.*
import com.yugioh.recomp.core.rom.RomAssetCache
import com.yugioh.recomp.core.rom.RomPlatform
import com.yugioh.recomp.core.rom.rememberCardDisplayBitmap
import com.yugioh.recomp.ui.theme.*

@Composable
fun PasswordMachineScreen(
    onNavigateBack: () -> Unit
) {
    val isGba = RomAssetCache.activeTarget?.platform == RomPlatform.GBA

    var passcode by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessMessage by remember { mutableStateOf(false) }
    var updateCounter by remember { mutableStateOf(0) }
    fun refresh() { updateCounter++ }

    val currentStarchips = if (isGba) GbaCampaignManager.starchips else DdsCampaignManager.starchips

    val matchedCardName = remember(passcode) {
        if (passcode.length == 8) {
            CardPasscodeRegistry.findCardNameByPasscode(passcode)
        } else null
    }

    val matchedCard: Card? = remember(matchedCardName) {
        if (matchedCardName != null) {
            if (isGba) {
                GbaCardCatalog.getCardByName(matchedCardName)
                    ?: DdsCardCatalog.getCardByName(matchedCardName)
                    ?: Card(id = 9999, name = matchedCardName, atk = 1500, def = 1200, level = 4, attribute = CardAttribute.EARTH, cardType = CardType.MONSTER, monsterType = MonsterType.WARRIOR)
            } else {
                DdsCardCatalog.getCardByName(matchedCardName)
                    ?: Card(id = 8888, name = matchedCardName, atk = 1500, def = 1200, level = 4, attribute = CardAttribute.EARTH, cardType = CardType.MONSTER, monsterType = MonsterType.WARRIOR)
            }
        } else null
    }

    val starchipCost = remember(matchedCard) {
        matchedCard?.let { c ->
            if (isGba) (c.level * 5).coerceIn(5, 40)
            else (c.deckCost * 2).coerceIn(5, 50)
        } ?: 0
    }

    val samplePasscodes = remember {
        listOf(
            "89631139" to "Blue-Eyes White Dragon",
            "46986414" to "Dark Magician",
            "74677422" to "Red-Eyes B. Dragon",
            "83764718" to "Monster Reborn",
            "53129443" to "Dark Hole",
            "12580477" to "Raigeki",
            "44095762" to "Mirror Force",
            "04206964" to "Trap Hole",
            "55144522" to "Pot of Greed",
            "77585513" to "Jinzo",
            "71625222" to "Time Wizard",
            "04031928" to "Change of Heart"
        )
    }

    fun submitUnlock() {
        if (passcode.length != 8) {
            statusMessage = "Please enter an 8-digit passcode."
            isSuccessMessage = false
            return
        }

        val (success, message) = if (isGba) {
            GbaCampaignManager.unlockCardViaPasscode(passcode)
        } else {
            DdsCampaignManager.unlockCardViaPasscode(passcode)
        }

        statusMessage = message
        isSuccessMessage = success
        if (success) {
            ChiptuneSynthesizer.playSfx(ChiptuneSynthesizer.createSpellActivationSfx())
            passcode = ""
            refresh()
        } else {
            ChiptuneSynthesizer.playSfx(ChiptuneSynthesizer.createDamageTickSfx())
        }
    }

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
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MillenniumGold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PASSWORD TERMINAL",
                        color = MillenniumGold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isGba) "Battle City Card Unlock Machine" else "Dark Duel Stories Starchip Exchange",
                        color = Color(0xFF94A3B8),
                        fontSize = 9.5.sp
                    )
                }
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.border(1.dp, MillenniumGold, RoundedCornerShape(6.dp))
                ) {
                    Text(
                        text = "⭐ $currentStarchips",
                        color = MillenniumGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Passcode Display Box
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, if (passcode.length == 8) MillenniumGold else Color(0xFF334155), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ENTER 8-DIGIT CARD PASSCODE",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 8) {
                            val digit = passcode.getOrNull(i)?.toString() ?: "-"
                            val isFilled = i < passcode.length
                            Surface(
                                color = if (isFilled) Color(0xFF1E293B) else Color(0xFF0B0F19),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .size(32.dp, 40.dp)
                                    .border(
                                        width = if (i == passcode.length) 2.dp else 1.dp,
                                        color = if (i == passcode.length) MillenniumGold else Color(0xFF334155),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = digit,
                                        color = if (isFilled) MillenniumGold else Color(0xFF475569),
                                        fontSize = 18.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Status message banner
            statusMessage?.let { msg ->
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = if (isSuccessMessage) Color(0xFF064E3B) else Color(0xFF7F1D1D),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, if (isSuccessMessage) Color(0xFF10B981) else AttackRed, RoundedCornerShape(6.dp))
                ) {
                    Text(
                        text = msg,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Card Match Preview Box
            if (matchedCard != null) {
                Surface(
                    color = Color(0xFF131A2B),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, MillenniumGold, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val bitmap = rememberCardDisplayBitmap(matchedCard, isFullCard = true)
                        Image(
                            bitmap = bitmap,
                            contentDescription = matchedCard.name,
                            modifier = Modifier
                                .width(46.dp)
                                .height(64.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = matchedCard.name,
                                color = MillenniumGold,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (matchedCard.isMonster) "★${matchedCard.level} • ${matchedCard.attribute.displayName} • ATK ${matchedCard.atk} / DEF ${matchedCard.def}" else "${matchedCard.cardType.name} Card",
                                color = Color.White,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "Cost: $starchipCost Starchips ⭐ (Balance: $currentStarchips ⭐)",
                                color = if (currentStarchips >= starchipCost) LifePointGreen else AttackRed,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { submitUnlock() },
                            enabled = currentStarchips >= starchipCost,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MillenniumGold,
                                disabledContainerColor = Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = "UNLOCK",
                                color = if (currentStarchips >= starchipCost) Color.Black else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else if (passcode.length == 8) {
                Surface(
                    color = Color(0xFF1A1D2B),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Unknown Passcode: No card found for [$passcode].",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Tactile Keypad
            Surface(
                color = Color(0xFF0F1320),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val keyRows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("CLEAR", "0", "DEL")
                    )

                    keyRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row.forEach { key ->
                                Button(
                                    onClick = {
                                        ChiptuneSynthesizer.playSfx(ChiptuneSynthesizer.createCardDrawSfx())
                                        when (key) {
                                            "CLEAR" -> {
                                                passcode = ""
                                                statusMessage = null
                                            }
                                            "DEL" -> {
                                                if (passcode.isNotEmpty()) {
                                                    passcode = passcode.dropLast(1)
                                                    statusMessage = null
                                                }
                                            }
                                            else -> {
                                                if (passcode.length < 8) {
                                                    passcode += key
                                                    statusMessage = null
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = when (key) {
                                            "CLEAR" -> Color(0xFF7F1D1D)
                                            "DEL" -> Color(0xFF334155)
                                            else -> Color(0xFF1E293B)
                                        }
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                ) {
                                    Text(
                                        text = key,
                                        color = if (key == "CLEAR") Color.White else MillenniumGold,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Reference Catalog: Tap to autofill
            Text(
                text = "POPULAR CARD PASSCODES (TAP TO INSERT):",
                color = Color(0xFF94A3B8),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(samplePasscodes) { (code, name) ->
                    Surface(
                        color = Color(0xFF131826),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                passcode = code
                                statusMessage = null
                                ChiptuneSynthesizer.playSfx(ChiptuneSynthesizer.createCardDrawSfx())
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = name,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = code,
                                color = MillenniumGold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
