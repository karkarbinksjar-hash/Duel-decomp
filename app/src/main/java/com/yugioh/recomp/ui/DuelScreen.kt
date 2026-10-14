package com.yugioh.recomp.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class AttackRayState(
    val fromPlayer: Boolean,
    val fromSlot: Int,
    val toSlot: Int?, // null = direct attack
    val isDirect: Boolean
)

@Composable
fun DuelScreen(
    onNavigateBackToRom: () -> Unit
) {
    val engine = remember {
        DuelEngine().apply {
            startDuel()
        }
    }

    val coroutineScope = rememberCoroutineScope()
    var updateCounter by remember { mutableStateOf(0) }
    fun refresh() { updateCounter++ }

    var selectedCardHandIndex by remember { mutableStateOf<Int?>(null) }
    var selectedPlayerMonsterSlot by remember { mutableStateOf<Int?>(null) }
    var selectedOpponentSlot by remember { mutableStateOf<Int?>(null) }
    var selectedFieldCard by remember { mutableStateOf<Card?>(null) }
    var inspectingCard by remember { mutableStateOf<Card?>(null) }
    var viewingGraveyardTitle by remember { mutableStateOf<String?>(null) }
    var viewingGraveyardCards by remember { mutableStateOf<List<Card>?>(null) }

    // Animations & Sequencer States
    var isAiExecuting by remember { mutableStateOf(false) }
    var aiAnnouncementText by remember { mutableStateOf<String?>(null) }
    var aiFeaturedCard by remember { mutableStateOf<Card?>(null) }
    var aiStepType by remember { mutableStateOf<String?>(null) }
    var phaseBannerText by remember { mutableStateOf<String?>(null) }
    var playerFloatingDamage by remember { mutableStateOf<Int?>(null) }
    var opponentFloatingDamage by remember { mutableStateOf<Int?>(null) }
    var animatingSummonSlot by remember { mutableStateOf<Pair<Boolean, Int>?>(null) }
    var activeAttackRay by remember { mutableStateOf<AttackRayState?>(null) }

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

    // Helper to flash phase banner
    fun announcePhase(phaseName: String) {
        coroutineScope.launch {
            phaseBannerText = phaseName
            delay(800)
            phaseBannerText = null
        }
    }

    // Auto-select next ready attacker if player wiped enemy field
    fun autoTargetNextAttacker() {
        if (engine.isPlayerTurn && engine.currentPhase == DuelPhase.BATTLE_PHASE && engine.opponent.activeMonstersCount() == 0) {
            val readySlot = engine.player.monsterZones.indices.firstOrNull { idx ->
                val m = engine.player.monsterZones[idx]
                m != null && m.position == CardPosition.FACE_UP_ATTACK && !m.hasAttackedThisTurn
            }
            if (readySlot != null) {
                selectedPlayerMonsterSlot = readySlot
            }
        }
    }

    // Coroutine-driven step-by-step AI Turn Sequencer
    fun executeAiTurnSequence() {
        coroutineScope.launch {
            isAiExecuting = true
            selectedCardHandIndex = null
            selectedPlayerMonsterSlot = null
            selectedOpponentSlot = null
            selectedFieldCard = null
            engine.dismissClash()
            refresh()

            val steps = engine.planAiTurnSteps()
            for (step in steps) {
                if (engine.winner != null) break

                when (step) {
                    is AiTurnStep.PhaseTransition -> {
                        aiStepType = "PHASE"
                        aiFeaturedCard = null
                        aiAnnouncementText = step.announcement
                        engine.executeAiStep(step)
                        refresh()
                        delay(750)
                    }
                    is AiTurnStep.Draw -> {
                        aiStepType = "DRAW"
                        aiFeaturedCard = step.card
                        aiAnnouncementText = step.announcement
                        playSfx("draw")
                        engine.executeAiStep(step)
                        refresh()
                        delay(850)
                    }
                    is AiTurnStep.ActivateSpell -> {
                        aiStepType = "SPELL"
                        aiFeaturedCard = step.card
                        aiAnnouncementText = step.announcement
                        playSfx("summon")
                        engine.executeAiStep(step)
                        refresh()
                        delay(1200)
                    }
                    is AiTurnStep.SetTrap -> {
                        aiStepType = "TRAP"
                        aiFeaturedCard = step.card
                        aiAnnouncementText = step.announcement
                        playSfx("summon")
                        engine.executeAiStep(step)
                        refresh()
                        delay(900)
                    }
                    is AiTurnStep.SummonMonster -> {
                        aiStepType = "SUMMON"
                        aiFeaturedCard = step.card
                        aiAnnouncementText = step.announcement
                        playSfx("summon")
                        animatingSummonSlot = Pair(false, step.slotIndex)
                        engine.executeAiStep(step)
                        refresh()
                        delay(1200)
                        animatingSummonSlot = null
                    }
                    is AiTurnStep.DeclareAttack -> {
                        aiStepType = "ATTACK"
                        aiFeaturedCard = step.attackerCard
                        aiAnnouncementText = step.announcement
                        activeAttackRay = AttackRayState(
                            fromPlayer = false,
                            fromSlot = step.attackerSlot,
                            toSlot = step.defenderSlot,
                            isDirect = step.isDirect
                        )
                        playSfx("attack")
                        delay(500)
                        engine.executeAiStep(step)
                        refresh()
                        if (step.predictedDamage > 0) {
                            playerFloatingDamage = step.predictedDamage
                            playSfx("damage")
                        }
                        delay(1200)
                        activeAttackRay = null
                        playerFloatingDamage = null
                    }
                    is AiTurnStep.EndTurn -> {
                        aiStepType = "END"
                        aiFeaturedCard = null
                        aiAnnouncementText = step.announcement
                        engine.executeAiStep(step)
                        refresh()
                        delay(800)
                    }
                }
            }

            aiAnnouncementText = null
            aiFeaturedCard = null
            aiStepType = null
            isAiExecuting = false
            announcePhase("YOUR TURN! - DRAW PHASE")
            refresh()
        }
    }

    val selectedCard = selectedCardHandIndex?.let { idx ->
        if (idx in engine.player.hand.indices) engine.player.hand[idx].card else null
    }

    // Active card for Persistent Selected-Card HUD
    val activeHudCard: Card? = selectedFieldCard
        ?: selectedCard
        ?: selectedPlayerMonsterSlot?.let { engine.player.monsterZones[it]?.card }
        ?: selectedOpponentSlot?.let { engine.opponent.monsterZones[it]?.card }
        ?: engine.player.hand.firstOrNull()?.card
        ?: engine.player.monsterZones.firstOrNull { it != null }?.card

    val selectedAttacker = selectedPlayerMonsterSlot?.let { engine.player.monsterZones[it]?.card }
    val canDirectAttack = !isAiExecuting &&
        engine.isPlayerTurn &&
        engine.currentPhase == DuelPhase.BATTLE_PHASE &&
        selectedPlayerMonsterSlot != null &&
        engine.opponent.activeMonstersCount() == 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkFieldBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) {
                // Tapping anywhere dismisses clash popup
                if (engine.clashState != null) {
                    engine.dismissClash()
                    refresh()
                    autoTargetNextAttacker()
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ==============================================================
            // 1. TOP SECTION: OPPONENT PLAYFIELD
            // ==============================================================
            OpponentSection(
                opponent = engine.opponent,
                selectedPlayerMonsterSlot = selectedPlayerMonsterSlot,
                currentPhase = engine.currentPhase,
                canDirectAttack = canDirectAttack,
                animatingSummonSlot = animatingSummonSlot,
                floatingDamage = opponentFloatingDamage,
                onOpponentMonsterClick = { slotIdx ->
                    if (!isAiExecuting && engine.isPlayerTurn && engine.currentPhase == DuelPhase.BATTLE_PHASE && selectedPlayerMonsterSlot != null) {
                        activeAttackRay = AttackRayState(
                            fromPlayer = true,
                            fromSlot = selectedPlayerMonsterSlot!!,
                            toSlot = slotIdx,
                            isDirect = false
                        )
                        playSfx("attack")
                        val prevOppLp = engine.opponent.lifePoints
                        engine.declareAttack(selectedPlayerMonsterSlot!!, slotIdx)
                        selectedPlayerMonsterSlot = null
                        val dmg = (prevOppLp - engine.opponent.lifePoints).coerceAtLeast(0)
                        if (dmg > 0) {
                            opponentFloatingDamage = dmg
                            playSfx("damage")
                        }
                        refresh()
                        coroutineScope.launch {
                            delay(1000)
                            activeAttackRay = null
                            autoTargetNextAttacker()
                            delay(800)
                            opponentFloatingDamage = null
                        }
                    }
                },
                onDirectAttack = {
                    if (canDirectAttack && selectedPlayerMonsterSlot != null) {
                        val dmg = engine.player.monsterZones[selectedPlayerMonsterSlot!!]?.card?.atk ?: 0
                        playSfx("attack")
                        activeAttackRay = AttackRayState(
                            fromPlayer = true,
                            fromSlot = selectedPlayerMonsterSlot!!,
                            toSlot = null,
                            isDirect = true
                        )
                        engine.declareAttack(selectedPlayerMonsterSlot!!, null)
                        selectedPlayerMonsterSlot = null
                        if (dmg > 0) {
                            opponentFloatingDamage = dmg
                            playSfx("damage")
                        }
                        refresh()
                        coroutineScope.launch {
                            delay(1000)
                            activeAttackRay = null
                            autoTargetNextAttacker()
                            delay(800)
                            opponentFloatingDamage = null
                        }
                    }
                },
                onInspectCard = {
                    selectedFieldCard = it
                    selectedCardHandIndex = null
                    inspectingCard = it
                },
                onSelectOpponentSlot = { slot ->
                    selectedOpponentSlot = slot
                    engine.opponent.monsterZones[slot]?.card?.let {
                        selectedFieldCard = it
                        selectedCardHandIndex = null
                    }
                },
                onOpenGraveyard = {
                    viewingGraveyardTitle = "${engine.opponent.name}'s Graveyard"
                    viewingGraveyardCards = engine.opponent.graveyard.toList()
                }
            )

            // ==============================================================
            // 2. MIDDLE SECTION: DUEL STAGE, ANIMATIONS & ANNOUNCER
            // ==============================================================
            MiddleAnimationStage(
                currentPhase = engine.currentPhase,
                turnNumber = engine.turnNumber,
                isPlayerTurn = engine.isPlayerTurn,
                isAiExecuting = isAiExecuting,
                aiStepType = aiStepType,
                aiAnnouncementText = aiAnnouncementText,
                aiFeaturedCard = aiFeaturedCard,
                phaseBannerText = phaseBannerText,
                activeAttackRay = activeAttackRay,
                clashState = engine.clashState,
                combatLogs = engine.combatLogs,
                selectedAttacker = selectedAttacker,
                canDirectAttack = canDirectAttack,
                onDismissClash = {
                    engine.dismissClash()
                    refresh()
                    autoTargetNextAttacker()
                },
                onDirectAttack = {
                    if (canDirectAttack && selectedPlayerMonsterSlot != null) {
                        val dmg = engine.player.monsterZones[selectedPlayerMonsterSlot!!]?.card?.atk ?: 0
                        playSfx("attack")
                        activeAttackRay = AttackRayState(
                            fromPlayer = true,
                            fromSlot = selectedPlayerMonsterSlot!!,
                            toSlot = null,
                            isDirect = true
                        )
                        engine.declareAttack(selectedPlayerMonsterSlot!!, null)
                        selectedPlayerMonsterSlot = null
                        if (dmg > 0) {
                            opponentFloatingDamage = dmg
                            playSfx("damage")
                        }
                        refresh()
                        coroutineScope.launch {
                            delay(1000)
                            activeAttackRay = null
                            autoTargetNextAttacker()
                            delay(800)
                            opponentFloatingDamage = null
                        }
                    }
                }
            )

            // ==============================================================
            // 3. BOTTOM SECTION: PLAYER PLAYFIELD
            // ==============================================================
            PlayerSection(
                player = engine.player,
                selectedCard = selectedCard,
                selectedPlayerMonsterSlot = selectedPlayerMonsterSlot,
                currentPhase = engine.currentPhase,
                isPlayerTurn = engine.isPlayerTurn,
                animatingSummonSlot = animatingSummonSlot,
                floatingDamage = playerFloatingDamage,
                onMonsterZoneClick = { slotIdx ->
                    if (!isAiExecuting) {
                        if (selectedCard != null && selectedCardHandIndex != null) {
                            if (selectedCard.isMonster) {
                                playSfx("summon")
                                animatingSummonSlot = Pair(true, slotIdx)
                                engine.summonMonster(
                                    isPlayer = true,
                                    handIndex = selectedCardHandIndex!!,
                                    slotIndex = slotIdx,
                                    setDefense = false
                                )
                                selectedCardHandIndex = null
                                refresh()
                                coroutineScope.launch {
                                    delay(900)
                                    animatingSummonSlot = null
                                }
                            }
                        } else {
                            val current = engine.player.monsterZones[slotIdx]
                            if (current != null) {
                                selectedFieldCard = current.card
                                if (engine.currentPhase == DuelPhase.BATTLE_PHASE && engine.isPlayerTurn) {
                                    selectedPlayerMonsterSlot = if (selectedPlayerMonsterSlot == slotIdx) null else slotIdx
                                } else {
                                    engine.changeBattlePosition(isPlayer = true, slotIndex = slotIdx)
                                }
                                refresh()
                            }
                        }
                    }
                },
                onSpellZoneClick = { slotIdx ->
                    if (!isAiExecuting && selectedCard != null && selectedCardHandIndex != null && (selectedCard.isSpell || selectedCard.isTrap)) {
                        playSfx("summon")
                        engine.setSpellTrap(isPlayer = true, handIndex = selectedCardHandIndex!!, slotIndex = slotIdx)
                        selectedCardHandIndex = null
                        refresh()
                    }
                },
                onInspectCard = {
                    selectedFieldCard = it
                    selectedCardHandIndex = null
                    inspectingCard = it
                },
                onOpenGraveyard = {
                    viewingGraveyardTitle = "${engine.player.name}'s Graveyard"
                    viewingGraveyardCards = engine.player.graveyard.toList()
                }
            )

            // ==============================================================
            // 4. PERSISTENT SELECTED-CARD HUD & CONTROLS
            // ==============================================================
            Column(modifier = Modifier.fillMaxWidth()) {
                // Persistent HUD Bar (Always visible showing selected or active card)
                SelectedCardHudBar(
                    card = activeHudCard,
                    onInspect = { activeHudCard?.let { inspectingCard = it } }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Hand Carousel and Action Buttons
                HandCarouselAndControls(
                    hand = engine.player.hand,
                    selectedCardIndex = selectedCardHandIndex,
                    selectedPlayerMonster = selectedAttacker,
                    currentPhase = engine.currentPhase,
                    isPlayerTurn = engine.isPlayerTurn && !isAiExecuting,
                    hasSummoned = engine.hasNormalSummonedThisTurn,
                    onSelectHandCard = { idx ->
                        if (!isAiExecuting) {
                            selectedCardHandIndex = if (selectedCardHandIndex == idx) null else idx
                            selectedFieldCard = selectedCardHandIndex?.let { engine.player.hand.getOrNull(it)?.card }
                            selectedPlayerMonsterSlot = null
                            selectedOpponentSlot = null
                        }
                    },
                    onSummonAttack = {
                        selectedCardHandIndex?.let { idx ->
                            val emptySlot = engine.player.firstEmptyMonsterSlot()
                            playSfx("summon")
                            if (emptySlot != -1) animatingSummonSlot = Pair(true, emptySlot)
                            engine.summonMonster(isPlayer = true, handIndex = idx, slotIndex = emptySlot, setDefense = false)
                            selectedCardHandIndex = null
                            refresh()
                            coroutineScope.launch {
                                delay(900)
                                animatingSummonSlot = null
                            }
                        }
                    },
                    onSetDefense = {
                        selectedCardHandIndex?.let { idx ->
                            val emptySlot = engine.player.firstEmptyMonsterSlot()
                            playSfx("summon")
                            if (emptySlot != -1) animatingSummonSlot = Pair(true, emptySlot)
                            engine.summonMonster(isPlayer = true, handIndex = idx, slotIndex = emptySlot, setDefense = true)
                            selectedCardHandIndex = null
                            refresh()
                            coroutineScope.launch {
                                delay(900)
                                animatingSummonSlot = null
                            }
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
                    onAdvancePhase = {
                        if (!isAiExecuting) {
                            playSfx("draw")
                            engine.nextPhase()
                            announcePhase(engine.currentPhase.displayName.uppercase())
                            selectedCardHandIndex = null
                            selectedPlayerMonsterSlot = null
                            autoTargetNextAttacker()
                            refresh()
                        }
                    },
                    onEndTurn = {
                        if (!isAiExecuting && engine.isPlayerTurn && engine.winner == null) {
                            playSfx("draw")
                            engine.passTurn(autoRunAi = false)
                            executeAiTurnSequence()
                        }
                    }
                )
            }
        }

        // Card Detail Modal
        inspectingCard?.let { card ->
            CardDetailDialog(card = card, onDismiss = { inspectingCard = null })
        }

        // Graveyard Modal
        viewingGraveyardCards?.let { gyCards ->
            GraveyardDialog(
                title = viewingGraveyardTitle ?: "Graveyard",
                cards = gyCards,
                onCardClick = { card -> inspectingCard = card },
                onDismiss = { viewingGraveyardCards = null }
            )
        }

        // Winner Overlay
        engine.winner?.let { winnerName ->
            WinnerOverlay(
                winnerName = winnerName,
                onRestart = {
                    engine.startDuel()
                    selectedCardHandIndex = null
                    selectedPlayerMonsterSlot = null
                    selectedOpponentSlot = null
                    selectedFieldCard = null
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
    canDirectAttack: Boolean,
    animatingSummonSlot: Pair<Boolean, Int>?,
    floatingDamage: Int?,
    onOpponentMonsterClick: (Int) -> Unit,
    onDirectAttack: () -> Unit,
    onInspectCard: (Card) -> Unit,
    onSelectOpponentSlot: (Int) -> Unit,
    onOpenGraveyard: () -> Unit
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Deck: ${opponent.deck.size}",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }

                // Opponent GY & LP Counter + Floating Damage
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFF1E2230),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .clickable { onOpenGraveyard() }
                            .border(0.5.dp, Color(0xFF475569), RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = "GY: ${opponent.graveyard.size}",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LP",
                        color = MillenniumGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${opponent.lifePoints}",
                            color = if (opponent.lifePoints > 2000) LifePointGreen else AttackRed,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        FloatingDamageText(damage = floatingDamage)
                    }
                }
            }

            // High-Visibility Direct Attack Prompt over Opponent Area
            if (canDirectAttack) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color(0x33DC2626),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, AttackRed, RoundedCornerShape(6.dp))
                        .clickable { onDirectAttack() }
                ) {
                    Text(
                        text = "🎯 OPPONENT UNGUARDED! TAP HERE TO ATTACK DIRECTLY! ⚔",
                        color = Color(0xFFFCA5A5),
                        fontWeight = FontWeight.Black,
                        fontSize = 10.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

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
                        onClick = {
                            if (stCard != null && !stCard.position.isFaceDown) onInspectCard(stCard.card)
                        },
                        onLongClick = {}
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

                    val isSummonAnim = animatingSummonSlot != null &&
                        !animatingSummonSlot.first &&
                        animatingSummonSlot.second == slot

                    ZoneSlot(
                        duelCard = mCard,
                        isSpellTrap = false,
                        isOpponent = true,
                        highlighted = isTargetable,
                        isSummonAnimating = isSummonAnim,
                        onClick = {
                            if (mCard != null) {
                                onSelectOpponentSlot(slot)
                                if (isTargetable) onOpponentMonsterClick(slot)
                                else onInspectCard(mCard.card)
                            } else if (canDirectAttack) {
                                onDirectAttack()
                            }
                        },
                        onLongClick = {
                            mCard?.card?.let { onInspectCard(it) }
                        }
                    )
                }
            }
        }
    }
}

// =========================================================================
// MIDDLE SECTION: DUEL ANIMATION STAGE & ANNOUNCER
// =========================================================================
@Composable
private fun MiddleAnimationStage(
    currentPhase: DuelPhase,
    turnNumber: Int,
    isPlayerTurn: Boolean,
    isAiExecuting: Boolean,
    aiStepType: String?,
    aiAnnouncementText: String?,
    aiFeaturedCard: Card?,
    phaseBannerText: String?,
    activeAttackRay: AttackRayState?,
    clashState: ClashState?,
    combatLogs: List<String>,
    selectedAttacker: Card?,
    canDirectAttack: Boolean,
    onDismissClash: () -> Unit,
    onDirectAttack: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(550, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseScale"
    )

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
                    text = "T$turnNumber ${if (isPlayerTurn) "PLAYER" else "KAIBA"}",
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

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Active attack energy ray animation overlay
                activeAttackRay?.let { ray ->
                    AttackRayOverlay(ray = ray)
                }

                // AI Action Announcement Overlay
                if (isAiExecuting && aiAnnouncementText != null) {
                    AiActionAnnouncementOverlay(
                        stepType = aiStepType,
                        announcement = aiAnnouncementText,
                        featuredCard = aiFeaturedCard
                    )
                } else if (clashState != null) {
                    // Clash / Battle Animation Viewport
                    Surface(
                        color = Color(0xFF1A1C29),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MillenniumGold, RoundedCornerShape(6.dp))
                            .clickable { onDismissClash() }
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
                                MiniCardPreview(clashState.attackerCard)
                                Text("⚔", fontSize = 16.sp, color = AttackRed)
                                if (clashState.defenderCard != null) {
                                    MiniCardPreview(clashState.defenderCard)
                                } else {
                                    Text(
                                        "DIRECT HIT\n-${clashState.damageDealt} LP",
                                        color = AttackRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            Text(
                                "(Tap to dismiss)",
                                color = Color(0xFF94A3B8),
                                fontSize = 8.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                } else if (canDirectAttack) {
                    // High-Visibility Direct Attack Button
                    Button(
                        onClick = onDirectAttack,
                        colors = ButtonDefaults.buttonColors(containerColor = AttackRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                            .border(2.dp, MillenniumGold, RoundedCornerShape(8.dp))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "⚔ DECLARE DIRECT ATTACK!",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                            selectedAttacker?.let {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "(-${it.atk} LP)",
                                    color = MillenniumGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
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
                            text = "⚔ ${selectedAttacker.name} (ATK ${selectedAttacker.atk}) ready! Select target monster.",
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

                // Phase Transition Flash Banner
                phaseBannerText?.let { pName ->
                    PhaseTransitionBanner(phaseName = pName)
                }
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
    animatingSummonSlot: Pair<Boolean, Int>?,
    floatingDamage: Int?,
    onMonsterZoneClick: (Int) -> Unit,
    onSpellZoneClick: (Int) -> Unit,
    onInspectCard: (Card) -> Unit,
    onOpenGraveyard: () -> Unit
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
                    val isSummonAnim = animatingSummonSlot != null &&
                        animatingSummonSlot.first &&
                        animatingSummonSlot.second == slot

                    ZoneSlot(
                        duelCard = mCard,
                        isSpellTrap = false,
                        isOpponent = false,
                        highlighted = isAttackerSelected || isEligibleSummonSlot,
                        isAttackingSource = isAttackerSelected,
                        isSummonAnimating = isSummonAnim,
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
                    val isEligibleSetSlot = (selectedCard?.isSpell == true || selectedCard?.isTrap == true) && stCard == null

                    ZoneSlot(
                        duelCard = stCard,
                        isSpellTrap = true,
                        isOpponent = false,
                        highlighted = isEligibleSetSlot,
                        onClick = { onSpellZoneClick(slot) },
                        onLongClick = { stCard?.card?.let { onInspectCard(it) } }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Player Header & Life Points
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

                // Player GY & LP Counter + Floating Damage
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFF1E2230),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .clickable { onOpenGraveyard() }
                            .border(0.5.dp, Color(0xFF475569), RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = "GY: ${player.graveyard.size}",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LP",
                        color = MillenniumGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${player.lifePoints}",
                            color = if (player.lifePoints > 2000) LifePointGreen else AttackRed,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        FloatingDamageText(damage = floatingDamage)
                    }
                }
            }
        }
    }
}

// =========================================================================
// PERSISTENT SELECTED-CARD HUD BAR
// =========================================================================
@Composable
private fun SelectedCardHudBar(
    card: Card?,
    onInspect: () -> Unit
) {
    Surface(
        color = Color(0xFF121624),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MillenniumGold, RoundedCornerShape(8.dp))
            .clickable { onInspect() }
    ) {
        if (card != null) {
            val cardBitmap = BitmapUtils.spriteToImageBitmap(RomAssetCache.getCardFullTexture(card.id))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Card Thumbnail
                Image(
                    bitmap = cardBitmap,
                    contentDescription = card.name,
                    modifier = Modifier
                        .width(34.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(3.dp))
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Card Info Center Column
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = card.name,
                            color = MillenniumGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // Attribute Gem Tag
                        Surface(
                            color = Color(card.attribute.colorHex),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text(
                                text = card.attribute.name,
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    if (card.isMonster) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "★ ${card.level}",
                                color = Color(0xFFFBBF24),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "[${card.monsterType.name}]",
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp
                            )
                            Text(
                                text = "ATK ${card.atk}",
                                color = AttackRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "DEF ${card.def}",
                                color = DefenseBlue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        Text(
                            text = "[${card.cardType.displayName.uppercase()}]",
                            color = if (card.isSpell) Color(0xFF2DD4BF) else Color(0xFFF472B6),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Lore quick-view icon
                IconButton(
                    onClick = onInspect,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Inspect", tint = MillenniumGold)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Select any card to view detailed stats and lore",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

// =========================================================================
// HAND CAROUSEL & CONTROLS
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
                    text = "⚔ Attacking Monster: ${selectedPlayerMonster.name} (ATK ${selectedPlayerMonster.atk})",
                    color = Color(0xFFFCA5A5),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        // Context Action Buttons (appears when hand card is selected)
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
                            val canSummonNow = isPlayerTurn && !hasSummoned && (currentPhase == DuelPhase.MAIN_PHASE_1 || currentPhase == DuelPhase.MAIN_PHASE_2)
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
                                enabled = isPlayerTurn,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Activate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onSetSpellTrap,
                                enabled = isPlayerTurn,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Set", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (card.isTrap) {
                            Button(
                                onClick = onSetSpellTrap,
                                enabled = isPlayerTurn,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBE185D)),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Set Trap", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
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
                Text("Next Phase", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onEndTurn,
                enabled = isPlayerTurn,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
            ) {
                Text("End Turn", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// =========================================================================
// FIELD ZONE SLOT WITH VISIBLE NAME & STAT LABELS
// =========================================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ZoneSlot(
    duelCard: DuelCard?,
    isSpellTrap: Boolean,
    isOpponent: Boolean,
    highlighted: Boolean,
    isAttackingSource: Boolean = false,
    isSummonAnimating: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val borderColor = when {
        isAttackingSource -> AttackRed
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

    val scale = if (isSummonAnimating) 1.12f else 1.0f

    Box(
        modifier = Modifier
            .width(62.dp)
            .height(58.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .border(
                width = if (highlighted || isAttackingSource) 2.dp else 1.dp,
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
                    // Bottom indicator for facedown
                    Surface(
                        color = Color(0xCC000000),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Text(
                            text = if (duelCard.position.isDefense) "SET DEF" else "SET",
                            color = DefenseBlue,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            } else if (duelCard.position.isDefense) {
                // Defense Mode (Rotated Faceup or Player Facedown)
                val cardBitmap = if (duelCard.position.isFaceDown) {
                    BitmapUtils.spriteToImageBitmap(RomAssetCache.getCardBack())
                } else {
                    BitmapUtils.spriteToImageBitmap(RomAssetCache.getCardFullTexture(card.id))
                }
                Box(
                    modifier = Modifier.fillMaxSize().padding(1.dp),
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
                    // Visible Card Name at Top
                    Surface(
                        color = Color(0xEE0B0E17),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        Text(
                            text = if (duelCard.position.isFaceDown) "SET" else card.name,
                            color = Color.White,
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                    // Visible DEF score at Bottom
                    Surface(
                        color = Color(0xEE0F172A),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Text(
                            text = if (duelCard.position.isFaceDown) "DEF" else "DEF ${card.def}",
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
                    modifier = Modifier.fillMaxSize().padding(1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = cardBitmap,
                        contentDescription = card.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(3.dp))
                    )
                    // Visible Card Name at Top
                    Surface(
                        color = Color(0xEE0B0E17),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        Text(
                            text = card.name,
                            color = Color.White,
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                    // Visible ATK score at Bottom
                    Surface(
                        color = Color(0xEE0F172A),
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

// =========================================================================
// HAND CARD ITEM WITH VISIBLE TOP NAME & BOTTOM STATS
// =========================================================================
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
            .width(68.dp)
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

            // Top Name Badge (Always identifies card)
            Surface(
                color = Color(0xEE0B0E17),
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 2.dp, vertical = 2.dp)
            ) {
                Text(
                    text = card.name,
                    color = Color.White,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bottom ATK/DEF or Spell/Trap Badge
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

// =========================================================================
// AI STEP ANNOUNCEMENT OVERLAY
// =========================================================================
@Composable
private fun AiActionAnnouncementOverlay(
    stepType: String?,
    announcement: String?,
    featuredCard: Card?
) {
    Surface(
        color = Color(0xF7111827),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, if (stepType == "ATTACK") AttackRed else MillenniumGold, RoundedCornerShape(10.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            Surface(
                color = if (stepType == "ATTACK") AttackRed else Color(0xFF1D4ED8),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "KAIBA: ${stepType ?: "ACTION"}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = announcement ?: "",
                color = MillenniumGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (featuredCard != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    MiniCardPreview(featuredCard)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = featuredCard.name,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (featuredCard.isMonster) {
                            Text(
                                text = "ATK ${featuredCard.atk} / DEF ${featuredCard.def}",
                                color = AttackRed,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold
                            )
                        } else {
                            Text(
                                text = featuredCard.cardType.displayName,
                                color = Color(featuredCard.attribute.colorHex),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// ATTACK TRAVEL RAY & IMPACT ANIMATION OVERLAY
// =========================================================================
@Composable
private fun AttackRayOverlay(
    ray: AttackRayState
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ray")
    val rayAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(250), RepeatMode.Reverse),
        label = "rayAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val startY = if (ray.fromPlayer) size.height else 0f
            val endY = if (ray.fromPlayer) 0f else size.height
            drawLine(
                brush = Brush.linearGradient(listOf(AttackRed, MillenniumGold, Color.White)),
                start = androidx.compose.ui.geometry.Offset(size.width * 0.5f, startY),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.5f, endY),
                strokeWidth = 5.dp.toPx() * rayAlpha
            )
        }
        Text("⚡ 💥 ⚔", fontSize = 24.sp)
    }
}

// =========================================================================
// FLOATING LP DAMAGE TEXT INDICATOR
// =========================================================================
@Composable
private fun FloatingDamageText(
    damage: Int?
) {
    AnimatedVisibility(
        visible = damage != null && damage > 0,
        enter = fadeIn(tween(150)) + slideInVertically(initialOffsetY = { 16 }),
        exit = fadeOut(tween(350)) + slideOutVertically(targetOffsetY = { -24 })
    ) {
        Surface(
            color = Color(0xEE7F1D1D),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.border(1.dp, AttackRed, RoundedCornerShape(4.dp))
        ) {
            Text(
                text = "-${damage} LP",
                color = Color(0xFFFEF2F2),
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}

// =========================================================================
// PHASE TRANSITION FLASH BANNER
// =========================================================================
@Composable
private fun PhaseTransitionBanner(
    phaseName: String
) {
    Surface(
        color = Color(0xEE0F172A),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .border(1.5.dp, MillenniumGold, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = phaseName,
            color = MillenniumGold,
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
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
