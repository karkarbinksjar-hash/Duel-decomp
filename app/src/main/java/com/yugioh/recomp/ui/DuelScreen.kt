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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
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
import kotlin.math.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class AttackRayState(
    val fromPlayer: Boolean,
    val fromSlot: Int,
    val toSlot: Int?,
    val isDirect: Boolean
)

@Composable
fun DuelScreen(
    onNavigateBackToRom: () -> Unit
) {
    val engine = remember {
        DuelEngine().apply {
            startDuel(DdsCampaignManager.selectedOpponentName)
        }
    }

    val coroutineScope = rememberCoroutineScope()
    var updateCounter by remember { mutableStateOf(0) }
    fun refresh() { updateCounter++ }

    var isShaderEnabled by remember { mutableStateOf(DdsCampaignManager.isGbcLcdShaderEnabled) }
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
    var cardDestructionSlot by remember { mutableStateOf<Pair<Boolean, Int>?>(null) }
    var activeAttackRay by remember { mutableStateOf<AttackRayState?>(null) }
    var screenShakeOffsetX by remember { mutableStateOf(0f) }
    var screenShakeOffsetY by remember { mutableStateOf(0f) }

    fun triggerScreenShake(intensity: Float = 12f) {
        coroutineScope.launch {
            val offsets = listOf(
                Pair(intensity, -intensity * 0.7f),
                Pair(-intensity, intensity * 0.8f),
                Pair(intensity * 0.6f, -intensity * 0.5f),
                Pair(-intensity * 0.4f, intensity * 0.3f),
                Pair(intensity * 0.2f, -intensity * 0.1f),
                Pair(0f, 0f)
            )
            for (offset in offsets) {
                screenShakeOffsetX = offset.first
                screenShakeOffsetY = offset.second
                delay(35)
            }
        }
    }

    fun playSfx(type: String) {
        val sfx = when (type) {
            "draw" -> ChiptuneSynthesizer.createCardDrawSfx()
            "summon" -> ChiptuneSynthesizer.createSummonSfx()
            "attack" -> ChiptuneSynthesizer.createAttackClashSfx()
            "damage" -> ChiptuneSynthesizer.createDamageTickSfx()
            "destroy" -> ChiptuneSynthesizer.createCardDestructionSfx()
            "direct" -> ChiptuneSynthesizer.createDirectAttackSfx()
            "victory" -> ChiptuneSynthesizer.createVictoryFanfare()
            else -> ChiptuneSynthesizer.createCardDrawSfx()
        }
        ChiptuneSynthesizer.playSfx(sfx)
    }

    fun announcePhase(phaseName: String) {
        coroutineScope.launch {
            phaseBannerText = phaseName
            delay(800)
            phaseBannerText = null
        }
    }

    fun autoTargetNextAttacker() {
        if (engine.isPlayerTurn && engine.currentPhase == DuelPhase.BATTLE_PHASE && engine.opponent.activeMonstersCount() == 0) {
            val readySlot = engine.player.monsterZones.indices.firstOrNull { idx: Int ->
                val m = engine.player.monsterZones[idx]
                m != null && m.position == CardPosition.FACE_UP_ATTACK && !m.hasAttackedThisTurn
            }
            if (readySlot != null) {
                selectedPlayerMonsterSlot = readySlot
            }
        }
    }

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
                        delay(1100)
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
                        playSfx(if (step.isDirect) "direct" else "attack")
                        delay(400)
                        triggerScreenShake(if (step.isDirect) 16f else 12f)
                        val defenderSlot = step.defenderSlot
                        val prevDefender = defenderSlot?.let { engine.player.monsterZones[it] }
                        val prevAttacker = engine.opponent.monsterZones[step.attackerSlot]

                        engine.executeAiStep(step)
                        refresh()

                        if (defenderSlot != null && prevDefender != null && engine.player.monsterZones[defenderSlot] == null) {
                            cardDestructionSlot = Pair(true, defenderSlot)
                            playSfx("destroy")
                        }
                        if (prevAttacker != null && engine.opponent.monsterZones[step.attackerSlot] == null) {
                            cardDestructionSlot = Pair(false, step.attackerSlot)
                            playSfx("destroy")
                        }

                        if (step.predictedDamage > 0) {
                            playerFloatingDamage = step.predictedDamage
                        }
                        delay(900)
                        cardDestructionSlot = null
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
            .offset(x = screenShakeOffsetX.dp, y = screenShakeOffsetY.dp)
            .background(DarkFieldBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) {
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
            OpponentSection(
                opponent = engine.opponent,
                selectedPlayerMonsterSlot = selectedPlayerMonsterSlot,
                currentPhase = engine.currentPhase,
                canDirectAttack = canDirectAttack,
                animatingSummonSlot = animatingSummonSlot,
                cardDestructionSlot = cardDestructionSlot,
                floatingDamage = opponentFloatingDamage,
                isShaderEnabled = isShaderEnabled,
                onDamageTick = { playSfx("damage") },
                onToggleShader = {
                    isShaderEnabled = !isShaderEnabled
                    DdsCampaignManager.isGbcLcdShaderEnabled = isShaderEnabled
                },
                onOpponentMonsterClick = { slotIdx ->
                    if (!isAiExecuting && engine.isPlayerTurn && engine.currentPhase == DuelPhase.BATTLE_PHASE && selectedPlayerMonsterSlot != null) {
                        val playerSlot = selectedPlayerMonsterSlot!!
                        activeAttackRay = AttackRayState(
                            fromPlayer = true,
                            fromSlot = playerSlot,
                            toSlot = slotIdx,
                            isDirect = false
                        )
                        playSfx("attack")
                        val prevOppLp = engine.opponent.lifePoints
                        val prevOppMon = engine.opponent.monsterZones[slotIdx]
                        val prevPlayerMon = engine.player.monsterZones[playerSlot]
                        selectedPlayerMonsterSlot = null

                        coroutineScope.launch {
                            delay(350)
                            triggerScreenShake(14f)
                            engine.declareAttack(playerSlot, slotIdx)
                            refresh()

                            if (prevOppMon != null && engine.opponent.monsterZones[slotIdx] == null) {
                                cardDestructionSlot = Pair(false, slotIdx)
                                playSfx("destroy")
                            }
                            if (prevPlayerMon != null && engine.player.monsterZones[playerSlot] == null) {
                                cardDestructionSlot = Pair(true, playerSlot)
                                playSfx("destroy")
                            }

                            val dmg = (prevOppLp - engine.opponent.lifePoints).coerceAtLeast(0)
                            if (dmg > 0) {
                                opponentFloatingDamage = dmg
                            }

                            delay(700)
                            cardDestructionSlot = null
                            activeAttackRay = null
                            autoTargetNextAttacker()
                            delay(600)
                            opponentFloatingDamage = null
                        }
                    }
                },
                onDirectAttack = {
                    if (canDirectAttack && selectedPlayerMonsterSlot != null) {
                        val playerSlot = selectedPlayerMonsterSlot!!
                        val dmg = engine.player.monsterZones[playerSlot]?.card?.atk ?: 0
                        playSfx("direct")
                        activeAttackRay = AttackRayState(
                            fromPlayer = true,
                            fromSlot = playerSlot,
                            toSlot = null,
                            isDirect = true
                        )
                        selectedPlayerMonsterSlot = null
                        coroutineScope.launch {
                            delay(400)
                            triggerScreenShake(18f)
                            engine.declareAttack(playerSlot, null)
                            if (dmg > 0) {
                                opponentFloatingDamage = dmg
                            }
                            refresh()
                            delay(750)
                            activeAttackRay = null
                            autoTargetNextAttacker()
                            delay(600)
                            opponentFloatingDamage = null
                        }
                    }
                },
                onInspectCard = { card: Card ->
                    selectedFieldCard = card
                    selectedCardHandIndex = null
                    inspectingCard = card
                },
                onSelectOpponentSlot = { slot: Int ->
                    selectedOpponentSlot = slot
                    engine.opponent.monsterZones[slot]?.card?.let { card ->
                        selectedFieldCard = card
                        selectedCardHandIndex = null
                    }
                },
                onOpenGraveyard = {
                    viewingGraveyardTitle = "${engine.opponent.name}'s Graveyard"
                    viewingGraveyardCards = engine.opponent.graveyard.toList()
                }
            )

            MiddleAnimationStage(
                currentPhase = engine.currentPhase,
                turnNumber = engine.turnNumber,
                isPlayerTurn = engine.isPlayerTurn,
                opponentName = engine.opponent.name,
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

            PlayerSection(
                player = engine.player,
                selectedCard = selectedCard,
                selectedPlayerMonsterSlot = selectedPlayerMonsterSlot,
                currentPhase = engine.currentPhase,
                isPlayerTurn = engine.isPlayerTurn,
                animatingSummonSlot = animatingSummonSlot,
                cardDestructionSlot = cardDestructionSlot,
                floatingDamage = playerFloatingDamage,
                onDamageTick = { playSfx("damage") },
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
                onInspectCard = { card: Card ->
                    selectedFieldCard = card
                    selectedCardHandIndex = null
                    inspectingCard = card
                },
                onOpenGraveyard = {
                    viewingGraveyardTitle = "${engine.player.name}'s Graveyard"
                    viewingGraveyardCards = engine.player.graveyard.toList()
                }
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                SelectedCardHudBar(
                    card = activeHudCard,
                    onInspect = { activeHudCard?.let { inspectingCard = it } }
                )

                Spacer(modifier = Modifier.height(4.dp))

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
                    },
                    canDirectAttack = canDirectAttack,
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
            }
        }

        if (isShaderEnabled) {
            GbcShaderOverlay(alpha = 0.18f)
        }

        inspectingCard?.let { card ->
            CardDetailDialog(card = card, onDismiss = { inspectingCard = null })
        }

        viewingGraveyardCards?.let { gyCards ->
            GraveyardDialog(
                title = viewingGraveyardTitle ?: "Graveyard",
                cards = gyCards,
                onCardClick = { card -> inspectingCard = card },
                onDismiss = { viewingGraveyardCards = null }
            )
        }

        engine.winner?.let { winnerName ->
            WinnerOverlay(
                winnerName = winnerName,
                opponentName = engine.opponent.name,
                rewardCard = engine.lastRewardCard,
                deckCapacity = DdsCampaignManager.deckCapacity,
                duelistLevel = DdsCampaignManager.duelistLevel,
                onRestart = {
                    engine.startDuel(DdsCampaignManager.selectedOpponentName)
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
    cardDestructionSlot: Pair<Boolean, Int>?,
    floatingDamage: Int?,
    isShaderEnabled: Boolean,
    onDamageTick: () -> Unit,
    onToggleShader: () -> Unit,
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
                        Text(opponent.name.first().toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = opponent.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${opponent.deck.size})",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                    if (RomAssetCache.activeTarget?.platform == com.yugioh.recomp.core.rom.RomPlatform.GBA) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFF4338CA),
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = "GBA",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isShaderEnabled) Color(0xFF047857) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .clickable { onToggleShader() }
                            .border(0.5.dp, Color(0xFF475569), RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = if (isShaderEnabled) "📺 GBC" else "📺 OFF",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
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
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LP",
                        color = MillenniumGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(contentAlignment = Alignment.Center) {
                        RollingLifePointsCounter(
                            targetLp = opponent.lifePoints,
                            isOpponent = true,
                            onDamageTick = onDamageTick
                        )
                        FloatingDamageText(damage = floatingDamage)
                    }
                }
            }

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
                        text = "🎯 OPPONENT UNGUARDED! TAP TO ATTACK DIRECTLY! ⚔",
                        color = Color(0xFFFCA5A5),
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Opponent Spell/Trap Zones
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

            // Opponent Monster Zones
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

                    val isDestructionAnim = cardDestructionSlot != null &&
                        !cardDestructionSlot.first &&
                        cardDestructionSlot.second == slot

                    ZoneSlot(
                        duelCard = mCard,
                        isSpellTrap = false,
                        isOpponent = true,
                        highlighted = isTargetable,
                        isSummonAnimating = isSummonAnim,
                        isDestructionAnimating = isDestructionAnim,
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
    opponentName: String,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "T$turnNumber ${if (isPlayerTurn) "YUGI" else opponentName.uppercase().take(8)}",
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
                activeAttackRay?.let { ray ->
                    AttackRayOverlay(ray = ray)
                }

                if (isAiExecuting && aiAnnouncementText != null) {
                    AiActionAnnouncementOverlay(
                        stepType = aiStepType,
                        announcement = aiAnnouncementText,
                        featuredCard = aiFeaturedCard,
                        opponentName = opponentName
                    )
                } else if (clashState != null) {
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
    cardDestructionSlot: Pair<Boolean, Int>?,
    floatingDamage: Int?,
    onDamageTick: () -> Unit,
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
            // Player Monster Zones
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

                    val isDestructionAnim = cardDestructionSlot != null &&
                        cardDestructionSlot.first &&
                        cardDestructionSlot.second == slot

                    ZoneSlot(
                        duelCard = mCard,
                        isSpellTrap = false,
                        isOpponent = false,
                        highlighted = isAttackerSelected || isEligibleSummonSlot,
                        isAttackingSource = isAttackerSelected,
                        isSummonAnimating = isSummonAnim,
                        isDestructionAnimating = isDestructionAnim,
                        onClick = { onMonsterZoneClick(slot) },
                        onLongClick = { mCard?.card?.let { onInspectCard(it) } }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Player Spell/Trap Zones
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

            // Player Header
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
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DC: ${DdsCampaignManager.totalDeckCost()}/${DdsCampaignManager.deckCapacity}",
                        color = MillenniumGold,
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

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
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LP",
                        color = MillenniumGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(contentAlignment = Alignment.Center) {
                        RollingLifePointsCounter(
                            targetLp = player.lifePoints,
                            isOpponent = false,
                            onDamageTick = onDamageTick
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

                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text(
                                text = "Cost: ${card.deckCost}",
                                color = Color(0xFFFDE047),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
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
    onEndTurn: () -> Unit,
    canDirectAttack: Boolean = false,
    onDirectAttack: () -> Unit = {}
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚔ Attacking: ${selectedPlayerMonster.name} (ATK ${selectedPlayerMonster.atk})",
                        color = Color(0xFFFCA5A5),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (canDirectAttack) {
                        Button(
                            onClick = onDirectAttack,
                            colors = ButtonDefaults.buttonColors(containerColor = AttackRed),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("DIRECT ATTACK", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

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
                            val isTribute = card.level >= 5
                            val tributeLabel = if (card.level >= 7) "Tribute (2)" else if (card.level in 5..6) "Tribute (1)" else "Summon"
                            Button(
                                onClick = onSummonAttack,
                                enabled = canSummonNow,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isTribute) Color(0xFFD97706) else AttackRed
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(if (hasSummoned) "Summoned" else tributeLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onSetDefense,
                                enabled = canSummonNow,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isTribute) Color(0xFF92400E) else DefenseBlue
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(if (isTribute) "Trib Set" else "Set DEF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    isDestructionAnimating: Boolean = false,
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

    val scale = if (isSummonAnimating) 1.15f else 1.0f

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
                val cardBackBitmap = BitmapUtils.spriteToImageBitmap(RomAssetCache.getCardBack())
                Box(
                    modifier = Modifier.fillMaxSize().padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = cardBackBitmap,
                        contentDescription = "Facedown Card",
                        filterQuality = FilterQuality.None,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(3.dp))
                            .then(if (duelCard.position.isDefense) Modifier.rotate(90f) else Modifier)
                    )
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
                        filterQuality = FilterQuality.None,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(3.dp))
                            .rotate(90f)
                    )
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
                val cardBitmap = BitmapUtils.spriteToImageBitmap(RomAssetCache.getCardFullTexture(card.id))
                Box(
                    modifier = Modifier.fillMaxSize().padding(1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = cardBitmap,
                        contentDescription = card.name,
                        filterQuality = FilterQuality.None,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(3.dp))
                    )
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

        // Golden Summoning Portal overlay animation
        if (isSummonAnimating) {
            GoldenSummonPortal(modifier = Modifier.fillMaxSize())
        }

        // Explosive Card Destruction Burst overlay animation
        if (isDestructionAnimating) {
            CardDestructionBurst(modifier = Modifier.fillMaxSize())
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
                filterQuality = FilterQuality.None,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            // Top Name Badge
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
    featuredCard: Card?,
    opponentName: String
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
                    text = "${opponentName.uppercase()}: ${stepType ?: "ACTION"}",
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
    val infiniteTransition = rememberInfiniteTransition(label = "rayAnim")
    val beamPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(180), RepeatMode.Reverse),
        label = "beamPulse"
    )
    val shockwaveScale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(350), RepeatMode.Reverse),
        label = "shockwave"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val startY = if (ray.fromPlayer) size.height else 0f
            val endY = if (ray.fromPlayer) 0f else size.height
            val midX = size.width * 0.5f

            // Radiant Outer Energy Beam
            drawLine(
                color = Color(0x66FFD700),
                start = androidx.compose.ui.geometry.Offset(midX, startY),
                end = androidx.compose.ui.geometry.Offset(midX, endY),
                strokeWidth = 18.dp.toPx() * beamPulse
            )

            // Lightning Core Beam
            drawLine(
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFFEF4444),
                        Color(0xFFFFD700),
                        Color(0xFFFFFFFF),
                        Color(0xFFFFD700),
                        Color(0xFFEF4444)
                    )
                ),
                start = androidx.compose.ui.geometry.Offset(midX, startY),
                end = androidx.compose.ui.geometry.Offset(midX, endY),
                strokeWidth = 8.dp.toPx() * beamPulse
            )

            // Impact Shockwave at center clash point
            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = 0.8f),
                radius = 16.dp.toPx() * shockwaveScale,
                center = androidx.compose.ui.geometry.Offset(midX, size.height * 0.5f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )
        }

        // Animated Clashing Blades Emblem
        Surface(
            color = Color(0xEE1E0B0B),
            shape = CircleShape,
            modifier = Modifier
                .size(36.dp)
                .border(2.dp, AttackRed, CircleShape)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "⚔",
                    color = Color(0xFFFEF08A),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

// =========================================================================
// GOLDEN MILLENNIUM SUMMONING PORTAL
// =========================================================================
@Composable
private fun GoldenSummonPortal(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "summonRune")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "rot"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(
        modifier = modifier.clip(RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = (min(size.width, size.height) / 2f) * pulseScale

            // Glowing Golden Portal Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(cx, cy),
                    radius = r * 1.4f
                ),
                radius = r * 1.4f,
                center = androidx.compose.ui.geometry.Offset(cx, cy)
            )

            // Inner Millennium Circle
            drawCircle(
                color = Color(0xFFFFD700),
                radius = r * 0.85f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )

            // Millennium Star Rays
            val numRays = 8
            for (i in 0 until numRays) {
                val rad = (i * (Math.PI / 4.0) + Math.toRadians(rotation.toDouble()))
                val rx = (cx + cos(rad) * r * 0.9f).toFloat()
                val ry = (cy + sin(rad) * r * 0.9f).toFloat()
                drawLine(
                    color = Color(0xFFFFFBEB),
                    start = androidx.compose.ui.geometry.Offset(cx, cy),
                    end = androidx.compose.ui.geometry.Offset(rx, ry),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

// =========================================================================
// CARD DESTRUCTION EXPLOSIVE BURST
// =========================================================================
@Composable
private fun CardDestructionBurst(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "burst")
    val burstScale by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "burstScale"
    )
    val burstAlpha by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "burstAlpha"
    )

    Box(
        modifier = modifier.clip(RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = (min(size.width, size.height) / 2f) * burstScale

            // Expanding Fiery Shockwave
            drawCircle(
                color = Color(0xFFEF4444).copy(alpha = burstAlpha),
                radius = r,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                color = Color(0xFFFDE047).copy(alpha = burstAlpha),
                radius = r * 0.6f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )

            // Exploding Card Shards / Debris
            val numShards = 12
            for (i in 0 until numShards) {
                val ang = i * (Math.PI * 2 / numShards)
                val sx = (cx + cos(ang) * r * 0.85f).toFloat()
                val sy = (cy + sin(ang) * r * 0.85f).toFloat()
                drawCircle(
                    color = Color(0xFFF97316).copy(alpha = burstAlpha),
                    radius = 3.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(sx, sy)
                )
            }
        }
    }
}

// =========================================================================
// ANIMATED ROLLING LIFE POINTS COUNTER
// =========================================================================
@Composable
private fun RollingLifePointsCounter(
    targetLp: Int,
    isOpponent: Boolean,
    onDamageTick: () -> Unit = {}
) {
    var displayLp by remember { mutableStateOf(targetLp) }
    var isDecrementing by remember { mutableStateOf(false) }

    LaunchedEffect(targetLp) {
        if (targetLp != displayLp) {
            val startLp = displayLp
            val diff = targetLp - startLp
            if (diff < 0) {
                isDecrementing = true
                val steps = min(20, max(5, abs(diff) / 50))
                val stepDelay = 650L / steps
                for (i in 1..steps) {
                    val current = startLp + (diff * i / steps)
                    displayLp = current
                    onDamageTick()
                    delay(stepDelay)
                }
                displayLp = targetLp
                isDecrementing = false
            } else {
                displayLp = targetLp
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "lpPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isDecrementing) 1.22f else 1.0f,
        animationSpec = infiniteRepeatable(tween(120), RepeatMode.Reverse),
        label = "pulse"
    )

    Text(
        text = "$displayLp",
        color = when {
            isDecrementing -> Color(0xFFEF4444) // Bright red flash while taking damage
            displayLp > 2000 -> LifePointGreen
            else -> AttackRed
        },
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        fontSize = if (isOpponent) 15.sp else 16.sp,
        modifier = Modifier.scale(if (isDecrementing) pulseScale else 1.0f)
    )
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
    opponentName: String,
    rewardCard: Card?,
    deckCapacity: Int,
    duelistLevel: Int,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    val isPlayerWin = winnerName == "Yugi"

    Surface(
        color = Color(0xEE090D16),
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
                text = if (isPlayerWin) "VICTORY!" else "DEFEAT",
                color = if (isPlayerWin) MillenniumGold else AttackRed,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isPlayerWin) "You defeated $opponentName!" else "$opponentName was victorious.",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = Color(0xFF131722),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .border(1.dp, MillenniumGold, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isPlayerWin) "+3 Deck Capacity Earned!" else "+2 Deck Capacity Earned",
                        color = if (isPlayerWin) LifePointGreen else Color(0xFF94A3B8),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Total Deck Capacity: $deckCapacity DC (Level $duelistLevel)",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    if (isPlayerWin && rewardCard != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "🎁 CARD WON FROM $opponentName:",
                            color = MillenniumGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        MiniCardPreview(rewardCard)
                        Text(
                            text = "(Added to your Trunk Chest)",
                            color = Color(0xFF86EFAC),
                            fontSize = 9.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

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
                Text("Return to Stage 1 Map", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
