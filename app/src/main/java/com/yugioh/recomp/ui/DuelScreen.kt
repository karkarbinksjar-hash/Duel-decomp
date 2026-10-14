package com.yugioh.recomp.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugioh.recomp.core.audio.ChiptuneSynthesizer
import com.yugioh.recomp.core.engine.*
import com.yugioh.recomp.core.rom.RomAssetCache
import com.yugioh.recomp.ui.theme.*

@Composable
fun DuelScreen(
    onNavigateBackToRom: () -> Unit
) {
    val engine = remember {
        DuelEngine().apply {
            startDuel()
        }
    }

    var updateCounter by remember { mutableStateOf(0) }
    fun refresh() { updateCounter++ }

    var selectedCardHandIndex by remember { mutableStateOf<Int?>(null) }
    var selectedPlayerMonsterSlot by remember { mutableStateOf<Int?>(null) }
    var inspectingCard by remember { mutableStateOf<Card?>(null) }

    // Sound effect trigger helper
    fun playSfx(type: String) {
        val sfx = when (type) {
            "draw" -> ChiptuneSynthesizer.createCardDrawSfx()
            "summon" -> ChiptuneSynthesizer.createSummonSfx()
            "attack" -> ChiptuneSynthesizer.createAttackClashSfx()
            "damage" -> ChiptuneSynthesizer.createDamageTickSfx()
            "victory" -> ChiptuneSynthesizer.createVictoryFanfare()
            else -> ChiptuneSynthesizer.createCardDrawSfx()
        }
        ChiptuneSynthesizer.playSfx(sfx)
    }

    val selectedCard = selectedCardHandIndex?.let { idx ->
        if (idx in engine.player.hand.indices) engine.player.hand[idx].card else null
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
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ==============================================================
            // 1. TOP SECTION: OPPONENT PLAYFIELD (LP, MONSTER & SPELL ZONES)
            // ==============================================================
            OpponentSection(
                opponent = engine.opponent,
                selectedPlayerMonsterSlot = selectedPlayerMonsterSlot,
                currentPhase = engine.currentPhase,
                onOpponentMonsterClick = { slotIdx ->
                    if (engine.isPlayerTurn && engine.currentPhase == DuelPhase.BATTLE_PHASE && selectedPlayerMonsterSlot != null) {
                        playSfx("attack")
                        engine.declareAttack(selectedPlayerMonsterSlot!!, slotIdx)
                        selectedPlayerMonsterSlot = null
                        refresh()
                    }
                },
                onInspectCard = { inspectingCard = it }
            )

            // ==============================================================
            // 2. MIDDLE SECTION: ACTIVE FIELD / DUEL ANIMATION STAGE
            // ==============================================================
            MiddleAnimationStage(
                currentPhase = engine.currentPhase,
                turnNumber = engine.turnNumber,
                isPlayerTurn = engine.isPlayerTurn,
                clashState = engine.clashState,
                combatLogs = engine.combatLogs,
                selectedAttacker = selectedPlayerMonsterSlot?.let { engine.player.monsterZones[it]?.card },
                canDirectAttack = engine.isPlayerTurn &&
                    engine.currentPhase == DuelPhase.BATTLE_PHASE &&
                    selectedPlayerMonsterSlot != null &&
                    engine.opponent.activeMonstersCount() == 0,
                onDirectAttack = {
                    if (selectedPlayerMonsterSlot != null) {
                        playSfx("attack")
                        engine.declareAttack(selectedPlayerMonsterSlot!!, null)
                        selectedPlayerMonsterSlot = null
                        refresh()
                    }
                }
            )

            // ==============================================================
            // 3. BOTTOM SECTION: PLAYER PLAYFIELD, CAROUSEL & CONTROLS
            // ==============================================================
            PlayerSection(
                player = engine.player,
                selectedCard = selectedCard,
                selectedPlayerMonsterSlot = selectedPlayerMonsterSlot,
                currentPhase = engine.currentPhase,
                isPlayerTurn = engine.isPlayerTurn,
                onMonsterZoneClick = { slotIdx ->
                    if (selectedCard != null && selectedCardHandIndex != null) {
                        // Summon or set into this zone
                        if (selectedCard.isMonster) {
                            playSfx("summon")
                            engine.summonMonster(
                                isPlayer = true,
                                handIndex = selectedCardHandIndex!!,
                                slotIndex = slotIdx,
                                setDefense = false
                            )
                            selectedCardHandIndex = null
                            refresh()
                        }
                    } else {
                        // Select this monster for battle or position toggle
                        val current = engine.player.monsterZones[slotIdx]
                        if (current != null) {
                            if (engine.currentPhase == DuelPhase.BATTLE_PHASE) {
                                selectedPlayerMonsterSlot = if (selectedPlayerMonsterSlot == slotIdx) null else slotIdx
                            } else {
                                engine.changeBattlePosition(isPlayer = true, slotIndex = slotIdx)
                            }
                            refresh()
                        }
                    }
                },
                onSpellZoneClick = { slotIdx ->
                    if (selectedCard != null && selectedCardHandIndex != null && (selectedCard.isSpell || selectedCard.isTrap)) {
                        playSfx("summon")
                        engine.setSpellTrap(isPlayer = true, handIndex = selectedCardHandIndex!!, slotIndex = slotIdx)
                        selectedCardHandIndex = null
                        refresh()
                    }
                },
                onInspectCard = { inspectingCard = it }
            )

            // Hand Carousel & Action Bar
            HandCarouselAndControls(
                hand = engine.player.hand,
                selectedCardIndex = selectedCardHandIndex,
                selectedPlayerMonster = selectedPlayerMonsterSlot?.let { engine.player.monsterZones[it]?.card },
                currentPhase = engine.currentPhase,
                isPlayerTurn = engine.isPlayerTurn,
                hasSummoned = engine.hasNormalSummonedThisTurn,
                onSelectHandCard = { idx ->
                    selectedCardHandIndex = if (selectedCardHandIndex == idx) null else idx
                    selectedPlayerMonsterSlot = null
                },
                onSummonAttack = {
                    selectedCardHandIndex?.let { idx ->
                        playSfx("summon")
                        engine.summonMonster(isPlayer = true, handIndex = idx, slotIndex = engine.player.firstEmptyMonsterSlot(), setDefense = false)
                        selectedCardHandIndex = null
                        refresh()
                    }
                },
                onSetDefense = {
                    selectedCardHandIndex?.let { idx ->
                        playSfx("summon")
                        engine.summonMonster(isPlayer = true, handIndex = idx, slotIndex = engine.player.firstEmptyMonsterSlot(), setDefense = true)
                        selectedCardHandIndex = null
                        refresh()
                    }
                },
                onActivateSpell = {
                    selectedCardHandIndex?.let { idx ->
                        playSfx("summon")
                        engine.activateSpell(isPlayer = true, handIndex = idx)
                        selectedCardHandIndex = null
                        refresh()
                    }
                },
                onSetSpellTrap = {
                    selectedCardHandIndex?.let { idx ->
                        playSfx("summon")
                        engine.setSpellTrap(isPlayer = true, handIndex = idx, slotIndex = engine.player.firstEmptySpellSlot())
                        selectedCardHandIndex = null
                        refresh()
                    }
                },
                onInspectSelectedCard = {
                    selectedCard?.let { inspectingCard = it }
                },
                onAdvancePhase = {
                    playSfx("draw")
                    engine.nextPhase()
                    selectedCardHandIndex = null
                    selectedPlayerMonsterSlot = null
                    refresh()
                },
                onEndTurn = {
                    playSfx("draw")
                    engine.endTurn()
                    selectedCardHandIndex = null
                    selectedPlayerMonsterSlot = null
                    refresh()
                }
            )
        }

        // Card Detail Modal
        inspectingCard?.let { card ->
            CardDetailDialog(card = card, onDismiss = { inspectingCard = null })
        }

        // Winner Overlay
        engine.winner?.let { winnerName ->
            WinnerOverlay(
                winnerName = winnerName,
                onRestart = {
                    engine.startDuel()
                    selectedCardHandIndex = null
                    selectedPlayerMonsterSlot = null
                    refresh()
                },
                onExit = onNavigateBackToRom
            )
        }
    }
}

// =========================================================================
// TOP SECTION: OPPONENT PLAYFIELD
// =========================================================================
@Composable
private fun OpponentSection(
    opponent: Duelist,
    selectedPlayerMonsterSlot: Int?,
    currentPhase: DuelPhase,
    onOpponentMonsterClick: (Int) -> Unit,
    onInspectCard: (Card) -> Unit
) {
    Surface(
        color = Color(0xFF141724),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF262C42), RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            // Opponent Header & Life Points
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDC2626)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("K", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = opponent.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Opponent LP Counter
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "LP",
                        color = MillenniumGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${opponent.lifePoints}",
                        color = if (opponent.lifePoints > 2000) LifePointGreen else AttackRed,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // LP Bar
            val lpProgress = (opponent.lifePoints / 8000f).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { lpProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (opponent.lifePoints > 2000) LifePointGreen else AttackRed,
                trackColor = Color(0xFF2D3248)
            )

            // Opponent Hand Cards Row (Facedown Card Backs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val cardBack = BitmapUtils.spriteToImageBitmap(RomAssetCache.getCardBack())
                repeat(opponent.hand.size) {
                    Image(
                        bitmap = cardBack,
                        contentDescription = "Opponent Hand Card",
                        modifier = Modifier
                            .width(18.dp)
                            .height(26.dp)
                            .padding(horizontal = 1.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .border(0.5.dp, Color(0xFF475569), RoundedCornerShape(2.dp))
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "(${opponent.hand.size})",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Opponent Spell/Trap Zones (Row 1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (slot in 4 downTo 0) {
                    val stCard = opponent.spellTrapZones[slot]
                    ZoneSlot(
                        duelCard = stCard,
                        isSpellTrap = true,
                        isOpponent = true,
                        highlighted = false,
                        onClick = {}
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Opponent Monster Zones (Row 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (slot in 4 downTo 0) {
                    val mCard = opponent.monsterZones[slot]
                    val isTargetable = selectedPlayerMonsterSlot != null &&
                        currentPhase == DuelPhase.BATTLE_PHASE &&
                        mCard != null

                    ZoneSlot(
                        duelCard = mCard,
                        isSpellTrap = false,
                        isOpponent = true,
                        highlighted = isTargetable,
                        onClick = {
                            if (mCard != null) {
                                if (isTargetable) onOpponentMonsterClick(slot)
                                else onInspectCard(mCard.card)
                            }
                        }
                    )
                }
            }
        }
    }
}

// =========================================================================
// MIDDLE SECTION: DUEL ANIMATION STAGE & COMBAT TICKER
// =========================================================================
@Composable
private fun MiddleAnimationStage(
    currentPhase: DuelPhase,
    turnNumber: Int,
    isPlayerTurn: Boolean,
    clashState: ClashState?,
    combatLogs: List<String>,
    selectedAttacker: Card?,
    canDirectAttack: Boolean,
    onDirectAttack: () -> Unit
) {
    Surface(
        color = Color(0xFF0F131F),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E2638), RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Phase Ribbon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "T$turnNumber ${if (isPlayerTurn) "YOU" else "OPP"}",
                    color = if (isPlayerTurn) MillenniumGold else Color(0xFFF87171),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    DuelPhase.values().forEach { phase ->
                        val isActive = phase == currentPhase
                        Surface(
                            color = if (isActive) MillenniumGold else Color(0xFF1A202E),
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = phase.shortName,
                                color = if (isActive) Color.Black else Color(0xFF64748B),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Clash / Battle Animation Viewport
            if (clashState != null) {
                Surface(
                    color = Color(0xFF1A1C29),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MillenniumGold, RoundedCornerShape(6.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = clashState.resultSummary,
                            color = MillenniumGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Attacker Preview
                            MiniCardPreview(clashState.attackerCard)
                            Text("⚔", fontSize = 16.sp, color = AttackRed)
                            // Defender Preview or Direct Attack Banner
                            if (clashState.defenderCard != null) {
                                MiniCardPreview(clashState.defenderCard)
                            } else {
                                Text(
                                    "DIRECT\n-${clashState.damageDealt} LP",
                                    color = AttackRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else if (canDirectAttack) {
                Button(
                    onClick = onDirectAttack,
                    colors = ButtonDefaults.buttonColors(containerColor = AttackRed),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                ) {
                    Text(
                        "DECLARE DIRECT ATTACK!",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            } else if (selectedAttacker != null) {
                Surface(
                    color = Color(0xFF271515),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AttackRed, RoundedCornerShape(6.dp))
                        .padding(4.dp)
                ) {
                    Text(
                        text = "⚔ ${selectedAttacker.name} (ATK ${selectedAttacker.atk}) ready to strike! Select enemy monster.",
                        color = Color(0xFFFCA5A5),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Recent Combat Log Ticker
                val latest = combatLogs.firstOrNull() ?: "Ready to duel."
                Text(
                    text = latest,
                    color = Color(0xFF86EFAC),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// =========================================================================
// BOTTOM SECTION: PLAYER PLAYFIELD
// =========================================================================
@Composable
private fun PlayerSection(
    player: Duelist,
    selectedCard: Card?,
    selectedPlayerMonsterSlot: Int?,
    currentPhase: DuelPhase,
    isPlayerTurn: Boolean,
    onMonsterZoneClick: (Int) -> Unit,
    onSpellZoneClick: (Int) -> Unit,
    onInspectCard: (Card) -> Unit
) {
    Surface(
        color = Color(0xFF141724),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF262C42), RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            // Player Monster Zones (Row 1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (slot in 0 until 5) {
                    val mCard = player.monsterZones[slot]
                    val isAttackerSelected = selectedPlayerMonsterSlot == slot
                    val isEligibleSummonSlot = selectedCard?.isMonster == true && mCard == null

                    ZoneSlot(
                        duelCard = mCard,
                        isSpellTrap = false,
                        isOpponent = false,
                        highlighted = isAttackerSelected || isEligibleSummonSlot,
                        onClick = { onMonsterZoneClick(slot) },
                        onLongClick = { mCard?.card?.let { onInspectCard(it) } }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Player Spell/Trap Zones (Row 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (slot in 0 until 5) {
                    val stCard = player.spellTrapZones[slot]
                    val isEligibleSlot = (selectedCard?.isSpell == true || selectedCard?.isTrap == true) && stCard == null

                    ZoneSlot(
                        duelCard = stCard,
                        isSpellTrap = true,
                        isOpponent = false,
                        highlighted = isEligibleSlot,
                        onClick = { onSpellZoneClick(slot) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Player LP Bar
            val lpProgress = (player.lifePoints / 8000f).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { lpProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (player.lifePoints > 2000) LifePointGreen else AttackRed,
                trackColor = Color(0xFF2D3248)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Player Header & LP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Y", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = player.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    if (isPlayerTurn) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(color = Color(0xFF1E3A8A), shape = RoundedCornerShape(3.dp)) {
                            Text(
                                text = currentPhase.shortName,
                                color = Color(0xFF93C5FD),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Deck: ${player.deck.size}",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }

                // Player LP Counter
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "LP",
                        color = MillenniumGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${player.lifePoints}",
                        color = if (player.lifePoints > 2000) LifePointGreen else AttackRed,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// =========================================================================
// HAND CAROUSEL & ACTION CONTROLS (PORTRAIT BOTTOM)
// =========================================================================
@Composable
private fun HandCarouselAndControls(
    hand: List<DuelCard>,
    selectedCardIndex: Int?,
    selectedPlayerMonster: Card?,
    currentPhase: DuelPhase,
    isPlayerTurn: Boolean,
    hasSummoned: Boolean,
    onSelectHandCard: (Int) -> Unit,
    onSummonAttack: () -> Unit,
    onSetDefense: () -> Unit,
    onActivateSpell: () -> Unit,
    onSetSpellTrap: () -> Unit,
    onInspectSelectedCard: () -> Unit,
    onAdvancePhase: () -> Unit,
    onEndTurn: () -> Unit
) {
    val selectedCard = selectedCardIndex?.let { hand.getOrNull(it)?.card }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (selectedPlayerMonster != null && selectedCard == null) {
            Surface(
                color = Color(0xFF1E2435),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
                    .border(1.dp, AttackRed, RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = "⚔ Selected Attacker: ${selectedPlayerMonster.name} (ATK ${selectedPlayerMonster.atk})",
                    color = Color(0xFFFCA5A5),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        // Context Action Bar (appears above hand carousel when a card is selected)
        AnimatedVisibility(
            visible = selectedCard != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            selectedCard?.let { card ->
                Surface(
                    color = Color(0xFF1E2435),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                        .border(1.dp, MillenniumGold, RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (card.isMonster) {
                            val canSummonNow = !hasSummoned && (currentPhase == DuelPhase.MAIN_PHASE_1 || currentPhase == DuelPhase.MAIN_PHASE_2)
                            Button(
                                onClick = onSummonAttack,
                                enabled = canSummonNow,
                                colors = ButtonDefaults.buttonColors(containerColor = AttackRed),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(if (hasSummoned) "Summoned" else "Summon", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onSetDefense,
                                enabled = canSummonNow,
                                colors = ButtonDefaults.buttonColors(containerColor = DefenseBlue),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Set DEF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (card.isSpell) {
                            Button(
                                onClick = onActivateSpell,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Activate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onSetSpellTrap,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Set", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (card.isTrap) {
                            Button(
                                onClick = onSetSpellTrap,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBE185D)),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Set Trap", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        IconButton(
                            onClick = onInspectSelectedCard,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Lore", tint = MillenniumGold)
                        }
                    }
                }
            }
        }

        // Horizontal Hand Carousel
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(105.dp)
                .background(Color(0xFF0D101A), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF1E2333), RoundedCornerShape(8.dp))
                .padding(vertical = 4.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(hand) { index, duelCard ->
                val isSelected = selectedCardIndex == index
                HandCardItem(
                    card = duelCard.card,
                    isSelected = isSelected,
                    onClick = { onSelectHandCard(index) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Phase Controls Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onAdvancePhase,
                enabled = isPlayerTurn && currentPhase != DuelPhase.END_PHASE,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
            ) {
                Text(
                    text = when (currentPhase) {
                        DuelPhase.MAIN_PHASE_1 -> "Enter Battle Phase >"
                        DuelPhase.BATTLE_PHASE -> "Main Phase 2 >"
                        DuelPhase.MAIN_PHASE_2 -> "End Phase >"
                        else -> "Next Phase >"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onEndTurn,
                enabled = isPlayerTurn,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(0.7f)
                    .height(38.dp)
            ) {
                Text(
                    text = "End Turn",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// =========================================================================
// ZONE COMPONENT & HAND CARD ITEM
// =========================================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ZoneSlot(
    duelCard: DuelCard?,
    isSpellTrap: Boolean,
    isOpponent: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val borderColor = when {
        highlighted -> MillenniumGold
        duelCard != null -> Color(0xFF475569)
        isSpellTrap -> Color(0xFF1E3A8A)
        else -> ZoneBorderGreen
    }

    val backgroundColor = when {
        duelCard != null -> Color(0xFF131722)
        isSpellTrap -> Color(0xFF0C101A)
        else -> Color(0xFF0F1A15)
    }

    Box(
        modifier = Modifier
            .width(62.dp)
            .height(58.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .border(
                width = if (highlighted) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(4.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (duelCard != null) {
            val card = duelCard.card
            if (duelCard.position.isFaceDown && isOpponent) {
                // Facedown Opponent Card Back
                val cardBackBitmap = BitmapUtils.spriteToImageBitmap(RomAssetCache.getCardBack())
                Box(
                    modifier = Modifier.fillMaxSize().padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = cardBackBitmap,
                        contentDescription = "Facedown Card",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(3.dp))
                            .then(if (duelCard.position.isDefense) Modifier.rotate(90f) else Modifier)
                    )
                    if (duelCard.position.isDefense) {
                        Surface(
                            color = Color(0xCC000000),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            Text(
                                text = "DEF",
                                color = DefenseBlue,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            } else if (duelCard.position.isDefense) {
                // Defense Mode (Horizontal/Rotated Faceup or Player Facedown)
                val cardBitmap = if (duelCard.position.isFaceDown) {
                    BitmapUtils.spriteToImageBitmap(RomAssetCache.getCardBack())
                } else {
                    BitmapUtils.spriteToImageBitmap(RomAssetCache.getCardFullTexture(card.id))
                }
                Box(
                    modifier = Modifier.fillMaxSize().padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = cardBitmap,
                        contentDescription = card.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(3.dp))
                            .rotate(90f)
                    )
                    Surface(
                        color = Color(0xDD0F172A),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Text(
                            text = if (duelCard.position.isFaceDown) "SET DEF" else "DEF ${card.def}",
                            color = DefenseBlue,
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            } else {
                // Attack Mode / Faceup Spell or Trap
                val cardBitmap = BitmapUtils.spriteToImageBitmap(RomAssetCache.getCardFullTexture(card.id))
                Box(
                    modifier = Modifier.fillMaxSize().padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = cardBitmap,
                        contentDescription = card.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Surface(
                        color = Color(0xDD0F172A),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Text(
                            text = if (card.isMonster) "ATK ${card.atk}" else card.cardType.displayName.uppercase(),
                            color = if (card.isMonster) AttackRed else if (card.isSpell) Color(0xFF2DD4BF) else Color(0xFFF472B6),
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isSpellTrap) "S/T" else "M",
                    color = Color(0xFF475569),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun HandCardItem(
    card: Card,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val fullCardBitmap = BitmapUtils.spriteToImageBitmap(RomAssetCache.getCardFullTexture(card.id))

    Surface(
        color = Color(0xFF141724),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .width(66.dp)
            .fillMaxHeight()
            .offset(y = if (isSelected) (-8).dp else 0.dp)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) MillenniumGold else Color(0xFF2B3245),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = fullCardBitmap,
                contentDescription = card.name,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            // Sleek ATK/DEF score badge at bottom of hand card
            Surface(
                color = Color(0xEE0B0E17),
                shape = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 2.dp, vertical = 2.dp)
            ) {
                if (card.isMonster) {
                    Text(
                        text = "${card.atk}/${card.def}",
                        color = MillenniumGold,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = card.cardType.displayName,
                        color = Color(card.attribute.colorHex),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniCardPreview(card: Card) {
    val fullCardBitmap = BitmapUtils.spriteToImageBitmap(RomAssetCache.getCardFullTexture(card.id))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            bitmap = fullCardBitmap,
            contentDescription = card.name,
            modifier = Modifier
                .width(26.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(3.dp))
                .border(1.dp, MillenniumGold, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(card.name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            if (card.isMonster) {
                Text("ATK ${card.atk} / DEF ${card.def}", color = AttackRed, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            } else {
                Text(card.cardType.displayName, color = Color(card.attribute.colorHex), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WinnerOverlay(
    winnerName: String,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Surface(
        color = Color(0xDD000000),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (winnerName == "Yugi") "VICTORY!" else "DEFEAT",
                color = if (winnerName == "Yugi") MillenniumGold else AttackRed,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Winner: $winnerName",
                color = Color.White,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = MillenniumGold),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text("Rematch", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text("Return to Importer", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
