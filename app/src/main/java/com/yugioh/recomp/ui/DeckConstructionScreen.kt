package com.yugioh.recomp.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugioh.recomp.core.audio.ChiptuneSynthesizer
import com.yugioh.recomp.core.engine.Card
import com.yugioh.recomp.core.engine.DdsCampaignManager
import com.yugioh.recomp.core.engine.GbaCampaignManager
import com.yugioh.recomp.core.rom.RomAssetCache
import com.yugioh.recomp.core.rom.RomPlatform
import com.yugioh.recomp.ui.theme.*

enum class DeckBuilderTab {
    ACTIVE_DECK,
    TRUNK
}

@Composable
fun DeckConstructionScreen(
    onNavigateBack: () -> Unit
) {
    val isGba = RomAssetCache.activeTarget?.platform == RomPlatform.GBA

    var selectedTab by remember { mutableStateOf(DeckBuilderTab.ACTIVE_DECK) }
    var inspectingCard by remember { mutableStateOf<Card?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var updateCounter by remember { mutableStateOf(0) }
    fun refresh() { updateCounter++ }

    val activeDeck = if (isGba) GbaCampaignManager.activeDeck else DdsCampaignManager.activeDeck
    val trunk = if (isGba) GbaCampaignManager.trunk else DdsCampaignManager.trunk
    val totalCost = if (isGba) 0 else DdsCampaignManager.totalDeckCost()
    val maxDc = if (isGba) 0 else DdsCampaignManager.deckCapacity
    val duelistLevel = if (isGba) 0 else DdsCampaignManager.duelistLevel
    val isDeckValid = if (isGba) activeDeck.size in 40..60 else (activeDeck.size == 40 && totalCost <= maxDc)

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
                .padding(8.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MillenniumGold)
                }
                Text(
                    text = if (isGba) "GBA DECK CONSTRUCTION" else "DECK CONSTRUCTION",
                    color = MillenniumGold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (isGba) "TCG EXPERT" else "LV $duelistLevel",
                        color = Color(0xFFFDE047),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Deck Capacity & Card Count Meter
            Surface(
                color = Color(0xFF111827),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isDeckValid) Color(0xFF374151) else AttackRed, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isGba) "Format: Battle City TCG" else "Deck Capacity: $totalCost / $maxDc DC",
                            color = if (isGba || totalCost <= maxDc) MillenniumGold else AttackRed,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Cards: ${activeDeck.size} / 40",
                            color = if (activeDeck.size in 40..60) LifePointGreen else AttackRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (!isDeckValid) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (activeDeck.size < 40) {
                                "⚠ A Deck must contain at least 40 cards! (${40 - activeDeck.size} needed)"
                            } else if (activeDeck.size > 60) {
                                "⚠ A Deck cannot exceed 60 cards!"
                            } else {
                                "⚠ Total Cost exceeds Deck Capacity ($maxDc DC)!"
                            },
                            color = AttackRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Status flash message
            statusMessage?.let { msg ->
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color(0xFF7F1D1D),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tab Selector: Active Deck vs Trunk
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TabButton(
                    title = "Active Deck (${activeDeck.size})",
                    isSelected = selectedTab == DeckBuilderTab.ACTIVE_DECK,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedTab = DeckBuilderTab.ACTIVE_DECK
                        statusMessage = null
                    }
                )
                TabButton(
                    title = "Trunk / Chest (${trunk.size})",
                    isSelected = selectedTab == DeckBuilderTab.TRUNK,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedTab = DeckBuilderTab.TRUNK
                        statusMessage = null
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Card Grid View
            val displayedCards = if (selectedTab == DeckBuilderTab.ACTIVE_DECK) activeDeck else trunk

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(displayedCards) { card ->
                    DeckCardItem(
                        card = card,
                        duelistLevel = if (isGba) 999 else duelistLevel,
                        isFromDeck = selectedTab == DeckBuilderTab.ACTIVE_DECK,
                        onClick = {
                            if (selectedTab == DeckBuilderTab.ACTIVE_DECK) {
                                val removed = if (isGba) GbaCampaignManager.removeCardFromDeck(card) else DdsCampaignManager.removeCardFromDeck(card)
                                if (removed) {
                                    ChiptuneSynthesizer.playSfx(ChiptuneSynthesizer.createCardDrawSfx())
                                    statusMessage = "Moved [${card.name}] to Trunk."
                                    refresh()
                                }
                            } else {
                                if (isGba) {
                                    val added = GbaCampaignManager.addCardToDeck(card)
                                    if (added) {
                                        ChiptuneSynthesizer.playSfx(ChiptuneSynthesizer.createCardDrawSfx())
                                        statusMessage = "Added [${card.name}] to Deck."
                                        refresh()
                                    } else {
                                        statusMessage = "Cannot add more than 3 copies of [${card.name}] or exceed 60 cards!"
                                    }
                                } else {
                                    val (canAdd, reason) = DdsCampaignManager.canAddCard(card)
                                    if (canAdd) {
                                        DdsCampaignManager.addCardToDeck(card)
                                        ChiptuneSynthesizer.playSfx(ChiptuneSynthesizer.createCardDrawSfx())
                                        statusMessage = "Added [${card.name}] to Deck."
                                        refresh()
                                    } else {
                                        statusMessage = reason
                                    }
                                }
                            }
                        },
                        onLongClick = {
                            inspectingCard = card
                        }
                    )
                }
            }
        }

        // Optional GBC LCD Shader
        if (!isGba && DdsCampaignManager.isGbcLcdShaderEnabled) {
            GbcShaderOverlay(alpha = 0.18f)
        }

        // Card Detail Dialog
        inspectingCard?.let { card ->
            CardDetailDialog(card = card, onDismiss = { inspectingCard = null })
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MillenniumGold else Color(0xFF1E293B)
        ),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier.height(38.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.Black else Color(0xFF94A3B8),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeckCardItem(
    card: Card,
    duelistLevel: Int,
    isFromDeck: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isOverLevel = card.deckCost > duelistLevel
    val fullCardBitmap = BitmapUtils.spriteToImageBitmap(RomAssetCache.getCardFullTexture(card.id))

    Surface(
        color = Color(0xFF131722),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .border(
                width = 1.dp,
                color = if (isOverLevel) AttackRed else Color(0xFF334155),
                shape = RoundedCornerShape(6.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier.padding(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Card Thumbnail
            Image(
                bitmap = fullCardBitmap,
                contentDescription = card.name,
                modifier = Modifier
                    .width(46.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Card Name
            Text(
                text = card.name,
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // ATK/DEF or Spell/Trap label
            if (card.isMonster) {
                Text(
                    text = "${card.atk}/${card.def}",
                    color = MillenniumGold,
                    fontSize = 7.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = if (card.isSpell) "SPELL" else "TRAP",
                    color = if (card.isSpell) Color(0xFF34D399) else Color(0xFFF472B6),
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
