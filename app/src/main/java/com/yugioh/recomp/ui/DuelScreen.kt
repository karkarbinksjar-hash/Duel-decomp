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
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugioh.recomp.core.audio.ChiptuneSynthesizer
import com.yugioh.recomp.core.engine.*
import com.yugioh.recomp.core.rom.RomAssetCache
import com.yugioh.recomp.core.rom.RomPlatform
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

fun getDuelistThemeColor(name: String): Pair<Color, Color> {
    return when {
        name.contains("Kaiba", ignoreCase = true) -> Pair(Color(0xFF1D4ED8), Color(0xFF38BDF8))
        name.contains("Joey", ignoreCase = true) -> Pair(Color(0xFFD97706), Color(0xFFFBBF24))
        name.contains("Yugi", ignoreCase = true) -> Pair(Color(0xFF7C3AED), Color(0xFFEC4899))
        name.contains("Mai", ignoreCase = true) -> Pair(Color(0xFF047857), Color(0xFF34D399))
        name.contains("Keith", ignoreCase = true) -> Pair(Color(0xFF374151), Color(0xFF9CA3AF))
        name.contains("Pegasus", ignoreCase = true) -> Pair(Color(0xFFD4AF37), Color(0xFFA855F7))
        name.contains("Marik", ignoreCase = true) -> Pair(Color(0xFF92400E), Color(0xFF7C2D12))
        name.contains("Bakura", ignoreCase = true) -> Pair(Color(0xFF0F766E), Color(0xFF14B8A6))
        name.contains("Ishizu", ignoreCase = true) -> Pair(Color(0xFFCA8A04), Color(0xFFFEF08A))
        name.contains("Weevil", ignoreCase = true) -> Pair(Color(0xFF15803D), Color(0xFF86EFAC))
        name.contains("Rex", ignoreCase = true) -> Pair(Color(0xFFB45309), Color(0xFFFDE047))
        name.contains("Mako", ignoreCase = true) -> Pair(Color(0xFF0284C7), Color(0xFF38BDF8))
        else -> Pair(Color(0xFFDC2626), Color(0xFFF87171))
    }
}

@Composable
fun DuelScreen(
    onNavigateBackToRom: () -> Unit
) {
    val engine = remember {
        DuelEngine().apply {
            val opp = if (RomAssetCache.activeTarget?.platform == RomPlatform.GBA) {
                GbaCampaignManager.selectedOpponentName
            } else {
                DdsCampaignManager.selectedOpponentName
            }
            startDuel(opp)
        }
    }

    val isGba = RomAssetCache.activeTarget?.platform == RomPlatform.GBA
    var isMenuOpen by remember { mutableStateOf(false) }
    var isSfxEnabled by remember { mutableStateOf(true) }
    var duelSpeed by remember { mutableStateOf(DuelSpeed.NORMAL) }

    BackHandler(enabled = true) {
        isMenuOpen = !isMenuOpen
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
    var lungingMonsterSlot by remember { mutableStateOf<Pair<Boolean, Int>?>(null) }
    var screenShakeOffsetX by remember { mutableStateOf(0f) }
    var screenShakeOffsetY by remember { mutableStateOf(0f) }

    // Preload full-resolution authentic artwork for all cards in the duel on launch
    LaunchedEffect(engine) {
        val cardsToPreload = mutableListOf<Card>()
        cardsToPreload.addAll(engine.player.deck)
        cardsToPreload.addAll(engine.player.hand.map { it.card })
        cardsToPreload.addAll(engine.opponent.deck)
        cardsToPreload.addAll(engine.opponent.hand.map { it.card })
        com.yugioh.recomp.core.rom.CardArtworkLoader.preloadCards(cardsToPreload)
    }

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
        if (!isSfxEnabled) return
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
                m != null && m.position == CardPosition.FACE_UP_ATTACK && m.canDeclareAttack
            }
            selectedPlayerMonsterSlot = readySlot
        } else if (selectedPlayerMonsterSlot != null) {
            val m = engine.player.monsterZones.getOrNull(selectedPlayerMonsterSlot!!)
            if (m == null || m.position != CardPosition.FACE_UP_ATTACK || !m.canDeclareAttack) {
                selectedPlayerMonsterSlot = null
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

            val speedMul = duelSpeed.multiplier
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
                        delay((1500 * speedMul).toLong())
                    }
                    is AiTurnStep.Draw -> {
                        aiStepType = "DRAW"
                        aiFeaturedCard = null
                        aiAnnouncementText = step.announcement
                        playSfx("draw")
                        engine.executeAiStep(step)
                        refresh()
                        delay((1700 * speedMul).toLong())
                    }
                    is AiTurnStep.ActivateSpell -> {
                        aiStepType = "SPELL"
                        aiFeaturedCard = step.card
                        aiAnnouncementText = step.announcement
                        playSfx("summon")
                        engine.executeAiStep(step)
                        refresh()
                        delay((2400 * speedMul).toLong())
                    }
                    is AiTurnStep.SetTrap -> {
                        aiStepType = "SET"
                        aiFeaturedCard = null
                        aiAnnouncementText = "${engine.opponent.name} set a card face-down in Spell & Trap Zone."
                        playSfx("summon")
                        engine.executeAiStep(step)
                        refresh()
                        delay((1800 * speedMul).toLong())
                    }
                    is AiTurnStep.SummonMonster -> {
                        aiStepType = if (step.isDefense) "SET" else "SUMMON"
                        aiFeaturedCard = if (step.isDefense) null else step.card
                        aiAnnouncementText = if (step.isDefense) "${engine.opponent.name} set a monster face-down in Defense." else step.announcement
                        playSfx("summon")
                        animatingSummonSlot = Pair(false, step.slotIndex)
                        engine.executeAiStep(step)
                        refresh()
                        delay((2200 * speedMul).toLong())
                        animatingSummonSlot = null
                    }
                    is AiTurnStep.DeclareAttack -> {
                        aiStepType = "ATTACK"
                        aiFeaturedCard = step.attackerCard
                        aiAnnouncementText = step.announcement
                        lungingMonsterSlot = Pair(false, step.attackerSlot)
                        activeAttackRay = AttackRayState(
                            fromPlayer = false,
                            fromSlot = step.attackerSlot,
                            toSlot = step.defenderSlot,
                            isDirect = step.isDirect
                        )
                        playSfx(if (step.isDirect) "direct" else "attack")
                        delay((400 * speedMul).toLong())
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
                        delay((700 * speedMul).toLong())
                        lungingMonsterSlot = null
                        delay((700 * speedMul).toLong())
                        cardDestructionSlot = null
                        activeAttackRay = null
                        delay((1000 * speedMul).toLong())
                        playerFloatingDamage = null
                    }
                    is AiTurnStep.EndTurn -> {
                        aiStepType = "END"
                        aiFeaturedCard = null
                        aiAnnouncementText = step.announcement
                        engine.executeAiStep(step)
                        refresh()
                        delay((1400 * speedMul).toLong())
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
        ?: selectedOpponentSlot?.let {
            val m = engine.opponent.monsterZones[it]
            if (m != null && !m.position.isFaceDown) m.card else null
        }
        ?: engine.player.hand.firstOrNull()?.card
        ?: engine.player.monsterZones.firstOrNull { it != null }?.card

    val selectedAttackerMonster = selectedPlayerMonsterSlot?.let { engine.player.monsterZones.getOrNull(it) }
    val selectedAttacker = selectedAttackerMonster?.card
    val canDirectAttack = !isAiExecuting &&
        engine.isPlayerTurn &&
        engine.currentPhase == DuelPhase.BATTLE_PHASE &&
        selectedPlayerMonsterSlot != null &&
        selectedAttackerMonster != null &&
        selectedAttackerMonster.position == CardPosition.FACE_UP_ATTACK &&
        selectedAttackerMonster.canDeclareAttack &&
        engine.opponent.activeMonstersCount() == 0

    fun executePlayerAttack(targetSlot: Int?) {
        if (isAiExecuting || !engine.isPlayerTurn || engine.currentPhase != DuelPhase.BATTLE_PHASE) return
        val playerSlot = selectedPlayerMonsterSlot ?: return
        val attacker = engine.player.monsterZones.getOrNull(playerSlot) ?: return

        if (attacker.position != CardPosition.FACE_UP_ATTACK) {
            engine.log("Monsters in Defense position cannot declare attacks.")
            return
        }
        if (!attacker.canDeclareAttack) {
            engine.log("${attacker.card.name} has already declared its attack(s) this turn.")
            return
        }
        if (targetSlot == null && engine.opponent.activeMonstersCount() > 0) {
            engine.log("Cannot attack directly while opponent controls monsters!")
            return
        }

        val isDirect = (targetSlot == null)
        val prevOppLp = engine.opponent.lifePoints
        val prevOppMon = targetSlot?.let { engine.opponent.monsterZones.getOrNull(it) }
        val prevPlayerMon = engine.player.monsterZones.getOrNull(playerSlot)

        // Execute engine attack FIRST before any animation is triggered
        val success = engine.declareAttack(playerSlot, targetSlot)
        if (!success) {
            refresh()
            return
        }

        // Only animate when attack successfully executed!
        selectedPlayerMonsterSlot = null
        lungingMonsterSlot = Pair(true, playerSlot)
        playSfx(if (isDirect) "direct" else "attack")
        activeAttackRay = AttackRayState(
            fromPlayer = true,
            fromSlot = playerSlot,
            toSlot = targetSlot,
            isDirect = isDirect
        )

        val damageDealt = (prevOppLp - engine.opponent.lifePoints).coerceAtLeast(0)
        if (damageDealt > 0) {
            opponentFloatingDamage = damageDealt
        }

        refresh()

        coroutineScope.launch {
            delay(250)
            triggerScreenShake(if (isDirect) 18f else 14f)

            if (targetSlot != null && prevOppMon != null && engine.opponent.monsterZones.getOrNull(targetSlot) == null) {
                cardDestructionSlot = Pair(false, targetSlot)
                playSfx("destroy")
            }
            if (prevPlayerMon != null && engine.player.monsterZones.getOrNull(playerSlot) == null) {
                cardDestructionSlot = Pair(true, playerSlot)
                playSfx("destroy")
            }

            delay(350)
            lungingMonsterSlot = null
            delay(350)
            cardDestructionSlot = null
            activeAttackRay = null
            autoTargetNextAttacker()
            delay(600)
            opponentFloatingDamage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(x = screenShakeOffsetX.dp, y = screenShakeOffsetY.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF070B16),
                        Color(0xFF0C1222),
                        Color(0xFF070B16)
                    )
                )
            )
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
            DuelTopBar(
                isGba = isGba,
                opponentName = engine.opponent.name,
                turnNumber = engine.turnNumber,
                currentPhase = engine.currentPhase,
                onOpenMenu = { isMenuOpen = true }
            )

            OpponentSection(
                opponent = engine.opponent,
                selectedPlayerMonsterSlot = selectedPlayerMonsterSlot,
                currentPhase = engine.currentPhase,
                canDirectAttack = canDirectAttack,
                animatingSummonSlot = animatingSummonSlot,
                cardDestructionSlot = cardDestructionSlot,
                lungingSlot = lungingMonsterSlot,
                floatingDamage = opponentFloatingDamage,
                onDamageTick = { playSfx("damage") },
                onOpponentMonsterClick = { slotIdx -> executePlayerAttack(slotIdx) },
                onDirectAttack = { executePlayerAttack(null) },
                onInspectCard = { card: Card ->
                    selectedFieldCard = card
                    selectedCardHandIndex = null
                    inspectingCard = card
                },
                onSelectOpponentSlot = { slot: Int ->
                    selectedOpponentSlot = slot
                    val m = engine.opponent.monsterZones[slot]
                    if (m != null && !m.position.isFaceDown) {
                        selectedFieldCard = m.card
                        selectedCardHandIndex = null
                    } else {
                        selectedFieldCard = null
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
                activeFieldSpell = engine.activeFieldSpell,
                onDismissClash = {
                    engine.dismissClash()
                    refresh()
                    autoTargetNextAttacker()
                },
                onDirectAttack = { executePlayerAttack(null) }
            )

            PlayerSection(
                player = engine.player,
                isGba = isGba,
                selectedCard = selectedCard,
                selectedPlayerMonsterSlot = selectedPlayerMonsterSlot,
                currentPhase = engine.currentPhase,
                isPlayerTurn = engine.isPlayerTurn,
                animatingSummonSlot = animatingSummonSlot,
                cardDestructionSlot = cardDestructionSlot,
                lungingSlot = lungingMonsterSlot,
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
                                    if (!current.canDeclareAttack) {
                                        selectedPlayerMonsterSlot = null
                                        engine.log("${current.card.name} has already declared its attack(s) this turn.")
                                    } else if (current.position != CardPosition.FACE_UP_ATTACK) {
                                        selectedPlayerMonsterSlot = null
                                        engine.log("Monsters in Defense position cannot declare attacks.")
                                    } else {
                                        selectedPlayerMonsterSlot = if (selectedPlayerMonsterSlot == slotIdx) null else slotIdx
                                    }
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
                    } else if (!isAiExecuting && engine.player.spellTrapZones[slotIdx] != null) {
                        val st = engine.player.spellTrapZones[slotIdx]!!
                        if (st.card.isTrap) {
                            playSfx("spell")
                            engine.activateSetTrap(isPlayer = true, slotIndex = slotIdx)
                            refresh()
                        }
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
                    effectiveAtk = activeHudCard?.let { engine.getEffectiveAtk(it) } ?: 0,
                    effectiveDef = activeHudCard?.let { engine.getEffectiveDef(it) } ?: 0,
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
                    onDirectAttack = { executePlayerAttack(null) }
                )
            }
        }

        if (isShaderEnabled) {
            GbcShaderOverlay(alpha = 0.18f)
        }

        if (isMenuOpen) {
            InGameMenuDialog(
                opponentName = engine.opponent.name,
                turnNumber = engine.turnNumber,
                currentPhase = engine.currentPhase,
                playerLp = engine.player.lifePoints,
                opponentLp = engine.opponent.lifePoints,
                playerDeckCount = engine.player.deck.size,
                opponentDeckCount = engine.opponent.deck.size,
                playerGyCount = engine.player.graveyard.size,
                opponentGyCount = engine.opponent.graveyard.size,
                combatLogs = engine.combatLogs,
                isGba = isGba,
                isSfxEnabled = isSfxEnabled,
                onToggleSfx = { isSfxEnabled = !isSfxEnabled },
                isShaderEnabled = isShaderEnabled,
                onToggleShader = {
                    isShaderEnabled = !isShaderEnabled
                    DdsCampaignManager.isGbcLcdShaderEnabled = isShaderEnabled
                },
                currentSpeed = duelSpeed,
                onSelectSpeed = { duelSpeed = it },
                onResume = { isMenuOpen = false },
                onRestart = {
                    val oppName = if (isGba) GbaCampaignManager.selectedOpponentName else DdsCampaignManager.selectedOpponentName
                    engine.startDuel(oppName)
                    selectedCardHandIndex = null
                    selectedPlayerMonsterSlot = null
                    selectedOpponentSlot = null
                    selectedFieldCard = null
                    isMenuOpen = false
                    refresh()
                },
                onSurrender = {
                    engine.surrender(isPlayer = true)
                    isMenuOpen = false
                    onNavigateBackToRom()
                },
                onOpenPlayerGy = {
                    viewingGraveyardTitle = "${engine.player.name}'s Graveyard"
                    viewingGraveyardCards = engine.player.graveyard.toList()
                },
                onOpenOpponentGy = {
                    viewingGraveyardTitle = "${engine.opponent.name}'s Graveyard"
                    viewingGraveyardCards = engine.opponent.graveyard.toList()
                },
                onExitToHub = {
                    isMenuOpen = false
                    onNavigateBackToRom()
                },
                onDismiss = { isMenuOpen = false }
            )
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
                isGba = isGba,
                opponentWins = GbaCampaignManager.opponentWins[engine.opponent.name] ?: 0,
                deckCapacity = DdsCampaignManager.deckCapacity,
                duelistLevel = DdsCampaignManager.duelistLevel,
                onRestart = {
                    val oppName = if (isGba) GbaCampaignManager.selectedOpponentName else DdsCampaignManager.selectedOpponentName
                    engine.startDuel(oppName)
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
// TOP BATTLE CITY / DDS STATUS & IN-GAME MENU BAR
// =========================================================================
@Composable
private fun DuelTopBar(
    isGba: Boolean,
    opponentName: String,
    turnNumber: Int,
    currentPhase: DuelPhase,
    onOpenMenu: () -> Unit
) {
    Surface(
        color = Color(0xFF0F1424),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 3.dp)
            .border(1.dp, Color(0xFF26324D), RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            Brush.radialGradient(listOf(MillenniumGold, Color(0xFFB45309))),
                            CircleShape
                        )
                        .border(1.dp, Color(0xFFFEF08A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👁", fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = if (isGba) "BATTLE CITY • EDS" else "DARK DUEL STORIES",
                        color = MillenniumGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "VS $opponentName • T$turnNumber • ${currentPhase.shortName}",
                        color = Color(0xFF94A3B8),
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // GBA Arcade In-Game Menu Button
            Surface(
                color = Color(0xFF1E2842),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .clickable { onOpenMenu() }
                    .border(1.dp, MillenniumGold, RoundedCornerShape(6.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("☰", color = MillenniumGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "MENU",
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
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
    lungingSlot: Pair<Boolean, Int>? = null,
    floatingDamage: Int?,
    onDamageTick: () -> Unit,
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
                val themeColors = getDuelistThemeColor(opponent.name)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(themeColors.first, themeColors.second)))
                            .border(1.5.dp, Color(0xFFFEF08A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(opponent.name.first().toString(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = opponent.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
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

            // Smooth Animated LP Color Gauge Bar
            val lpProgress = (opponent.lifePoints / 8000f).coerceIn(0f, 1f)
            val animatedProgress by animateFloatAsState(
                targetValue = lpProgress,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                label = "oppLpProgress"
            )
            val gaugeColor = when {
                opponent.lifePoints > 3000 -> Color(0xFF10B981) // Emerald
                opponent.lifePoints > 1500 -> Color(0xFFF59E0B) // Amber
                else -> Color(0xFFEF4444) // Red
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = gaugeColor,
                trackColor = Color(0xFF1E2436)
            )

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

                    val isOpponentLunging = lungingSlot != null &&
                        !lungingSlot.first &&
                        lungingSlot.second == slot

                    ZoneSlot(
                        duelCard = mCard,
                        isSpellTrap = false,
                        isOpponent = true,
                        highlighted = isTargetable,
                        isLunging = isOpponentLunging,
                        isSummonAnimating = isSummonAnim,
                        isDestructionAnimating = isDestructionAnim,
                        onClick = {
                            if (mCard != null) {
                                if (!mCard.position.isFaceDown) {
                                    onSelectOpponentSlot(slot)
                                    if (isTargetable) onOpponentMonsterClick(slot)
                                    else onInspectCard(mCard.card)
                                } else {
                                    if (isTargetable) onOpponentMonsterClick(slot)
                                }
                            } else if (canDirectAttack) {
                                onDirectAttack()
                            }
                        },
                        onLongClick = {
                            if (mCard != null && !mCard.position.isFaceDown) {
                                onInspectCard(mCard.card)
                            }
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
    activeFieldSpell: Card? = null,
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
                    DuelPhase.entries.forEach { phase ->
                        val isActive = phase == currentPhase
                        val phaseColor = when (phase) {
                            DuelPhase.DRAW_PHASE, DuelPhase.STANDBY_PHASE -> Color(0xFF38BDF8)
                            DuelPhase.MAIN_PHASE_1, DuelPhase.MAIN_PHASE_2 -> Color(0xFFFBBF24)
                            DuelPhase.BATTLE_PHASE -> Color(0xFFEF4444)
                            DuelPhase.END_PHASE -> Color(0xFFA855F7)
                        }
                        Surface(
                            color = if (isActive) phaseColor else Color(0xFF161E2E),
                            shape = RoundedCornerShape(4.dp),
                            modifier = if (isActive) {
                                Modifier
                                    .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                                    .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                            } else Modifier
                        ) {
                            Text(
                                text = phase.shortName,
                                color = if (isActive) Color.Black else Color(0xFF64748B),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            if (activeFieldSpell != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Surface(
                    color = Color(0xFF1E1B4B),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.border(1.dp, Color(0xFF818CF8), RoundedCornerShape(4.dp))
                ) {
                    Text(
                        text = "🌌 FIELD: ${activeFieldSpell.name.uppercase()}",
                        color = Color(0xFFA5B4FC),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                    )
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
                                "⚔ DIRECT ATTACK! TAP TO STRIKE",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.5.sp
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
    isGba: Boolean,
    selectedCard: Card?,
    selectedPlayerMonsterSlot: Int?,
    currentPhase: DuelPhase,
    isPlayerTurn: Boolean,
    animatingSummonSlot: Pair<Boolean, Int>?,
    cardDestructionSlot: Pair<Boolean, Int>?,
    lungingSlot: Pair<Boolean, Int>? = null,
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
                    val canAttackNow = isPlayerTurn && currentPhase == DuelPhase.BATTLE_PHASE && mCard != null && mCard.canDeclareAttack
                    val hasAlreadyAttacked = isPlayerTurn && currentPhase == DuelPhase.BATTLE_PHASE && mCard != null && !mCard.canDeclareAttack

                    val isSummonAnim = animatingSummonSlot != null &&
                        animatingSummonSlot.first &&
                        animatingSummonSlot.second == slot

                    val isDestructionAnim = cardDestructionSlot != null &&
                        cardDestructionSlot.first &&
                        cardDestructionSlot.second == slot

                    val isPlayerLunging = lungingSlot != null &&
                        lungingSlot.first &&
                        lungingSlot.second == slot

                    ZoneSlot(
                        duelCard = mCard,
                        isSpellTrap = false,
                        isOpponent = false,
                        highlighted = isAttackerSelected || isEligibleSummonSlot,
                        isAttackingSource = isAttackerSelected,
                        canAttackNow = canAttackNow && !isAttackerSelected,
                        hasAlreadyAttacked = hasAlreadyAttacked,
                        isLunging = isPlayerLunging,
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

            // Smooth Animated LP Color Gauge Bar
            val lpProgress = (player.lifePoints / 8000f).coerceIn(0f, 1f)
            val animatedProgress by animateFloatAsState(
                targetValue = lpProgress,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                label = "playerLpProgress"
            )
            val gaugeColor = when {
                player.lifePoints > 3000 -> Color(0xFF10B981) // Emerald
                player.lifePoints > 1500 -> Color(0xFFF59E0B) // Amber
                else -> Color(0xFFEF4444) // Red
            }
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = gaugeColor,
                trackColor = Color(0xFF1E2436)
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Player Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF38BDF8))))
                            .border(1.5.dp, Color(0xFFFEF08A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Y", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = player.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    if (isPlayerTurn) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            color = Color(0xFF1E3A8A),
                            shape = RoundedCornerShape(3.dp),
                            modifier = Modifier.border(0.5.dp, Color(0xFF60A5FA), RoundedCornerShape(3.dp))
                        ) {
                            Text(
                                text = currentPhase.shortName,
                                color = Color(0xFF93C5FD),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isGba) "🎴 Deck: ${player.deck.size}" else "DC: ${DdsCampaignManager.totalDeckCost()}/${DdsCampaignManager.deckCapacity}",
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
    effectiveAtk: Int = card?.atk ?: 0,
    effectiveDef: Int = card?.def ?: 0,
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
            val cardBitmap = com.yugioh.recomp.core.rom.rememberCardDisplayBitmap(card, isFullCard = true)

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
                            val isAtkBoosted = effectiveAtk > card.atk
                            val isAtkReduced = effectiveAtk < card.atk
                            val atkColor = if (isAtkBoosted) Color(0xFF4ADE80) else if (isAtkReduced) Color(0xFFF87171) else AttackRed
                            val atkText = if (isAtkBoosted) "ATK $effectiveAtk (+${effectiveAtk - card.atk})" else if (isAtkReduced) "ATK $effectiveAtk (-${card.atk - effectiveAtk})" else "ATK ${card.atk}"
                            Text(
                                text = atkText,
                                color = atkColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            val isDefBoosted = effectiveDef > card.def
                            val isDefReduced = effectiveDef < card.def
                            val defColor = if (isDefBoosted) Color(0xFF4ADE80) else if (isDefReduced) Color(0xFFF87171) else DefenseBlue
                            val defText = if (isDefBoosted) "DEF $effectiveDef (+${effectiveDef - card.def})" else if (isDefReduced) "DEF $effectiveDef (-${card.def - effectiveDef})" else "DEF ${card.def}"
                            Text(
                                text = defText,
                                color = defColor,
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
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (isTribute) MillenniumGold else Color(0xFFFCA5A5)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(if (hasSummoned) "Summoned" else "⚔ $tributeLabel", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                            Button(
                                onClick = onSetDefense,
                                enabled = canSummonNow,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isTribute) Color(0xFF78350F) else DefenseBlue
                                ),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFF60A5FA)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(if (isTribute) "🛡 Trib Set" else "🛡 Set DEF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (card.isSpell) {
                            Button(
                                onClick = onActivateSpell,
                                enabled = isPlayerTurn,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFF5EEAD4)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("✨ Activate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onSetSpellTrap,
                                enabled = isPlayerTurn,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFF64748B)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("🛡 Set", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (card.isTrap) {
                            Button(
                                onClick = onSetSpellTrap,
                                enabled = isPlayerTurn,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBE185D)),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFFF472B6)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("🛡 Set Trap", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
            val nextPhaseName = when (currentPhase) {
                DuelPhase.DRAW_PHASE -> "Standby ▶"
                DuelPhase.STANDBY_PHASE -> "Main Phase 1 ▶"
                DuelPhase.MAIN_PHASE_1 -> "⚔ Battle Phase ▶"
                DuelPhase.BATTLE_PHASE -> "Main Phase 2 ▶"
                DuelPhase.MAIN_PHASE_2 -> "End Phase ▶"
                DuelPhase.END_PHASE -> "End Turn ▶"
            }
            val phaseButtonColor = when (currentPhase) {
                DuelPhase.MAIN_PHASE_1 -> Color(0xFFB91C1C) // Crimson leading to Battle
                DuelPhase.BATTLE_PHASE -> Color(0xFF1D4ED8) // Sapphire leading to M2
                else -> Color(0xFF2563EB)
            }
            val phaseBorderColor = when (currentPhase) {
                DuelPhase.MAIN_PHASE_1 -> Color(0xFFFCA5A5)
                DuelPhase.BATTLE_PHASE -> Color(0xFF93C5FD)
                else -> Color(0xFF60A5FA)
            }

            Button(
                onClick = onAdvancePhase,
                enabled = isPlayerTurn && currentPhase != DuelPhase.END_PHASE,
                colors = ButtonDefaults.buttonColors(
                    containerColor = phaseButtonColor,
                    disabledContainerColor = Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (isPlayerTurn && currentPhase != DuelPhase.END_PHASE) phaseBorderColor else Color(0xFF334155)),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
            ) {
                Text(
                    text = nextPhaseName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isPlayerTurn && currentPhase != DuelPhase.END_PHASE) Color.White else Color(0xFF64748B)
                )
            }

            Button(
                onClick = onEndTurn,
                enabled = isPlayerTurn,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF334155),
                    disabledContainerColor = Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (isPlayerTurn) Color(0xFF64748B) else Color(0xFF334155)),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⌛", fontSize = 11.sp, color = if (isPlayerTurn) Color.White else Color(0xFF64748B))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "End Turn",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPlayerTurn) Color.White else Color(0xFF64748B)
                    )
                }
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
    canAttackNow: Boolean = false,
    hasAlreadyAttacked: Boolean = false,
    isLunging: Boolean = false,
    isSummonAnimating: Boolean = false,
    isDestructionAnimating: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "zonePulse")
    val attackPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "atkPulse"
    )

    val borderColor = when {
        isLunging || isAttackingSource -> Color(0xFF00F2FE)
        canAttackNow -> Color(0xFFF59E0B).copy(alpha = attackPulseAlpha)
        highlighted -> MillenniumGold
        hasAlreadyAttacked -> Color(0xFF334155)
        duelCard != null -> Color(0xFF475569)
        isSpellTrap -> Color(0xFF1E3A8A)
        else -> ZoneBorderGreen
    }

    val backgroundColor = when {
        hasAlreadyAttacked -> Color(0xFF0C0F17)
        duelCard != null -> Color(0xFF131722)
        isSpellTrap -> Color(0xFF0C101A)
        else -> Color(0xFF0F1A15)
    }

    val finalAlpha = if (hasAlreadyAttacked) 0.55f else 1.0f

    // Physical Card Lunge Animation towards the opponent/player
    val lungeTargetY = if (isLunging) (if (isOpponent) 32f else -32f) else 0f
    val lungeOffsetY by animateFloatAsState(
        targetValue = lungeTargetY,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "lungeY"
    )
    val lungeScale by animateFloatAsState(
        targetValue = if (isLunging) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "lungeScale"
    )

    // Smooth Summon Bounce
    val animatedSummonScale by animateFloatAsState(
        targetValue = if (isSummonAnimating) 1.22f else 1.0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow),
        label = "summonScale"
    )

    // Card Destruction Spin & Dissolve
    val destructionScale by animateFloatAsState(
        targetValue = if (isDestructionAnimating) 0.3f else 1.0f,
        animationSpec = tween(500, easing = FastOutLinearInEasing),
        label = "destrScale"
    )
    val destructionAlpha by animateFloatAsState(
        targetValue = if (isDestructionAnimating) 0.0f else 1.0f,
        animationSpec = tween(500),
        label = "destrAlpha"
    )
    val destructionRotation by animateFloatAsState(
        targetValue = if (isDestructionAnimating) 35f else 0f,
        animationSpec = tween(500),
        label = "destrRot"
    )

    Box(
        modifier = Modifier
            .width(62.dp)
            .height(58.dp)
            .graphicsLayer {
                translationY = lungeOffsetY * density
                scaleX = animatedSummonScale * lungeScale * destructionScale
                scaleY = animatedSummonScale * lungeScale * destructionScale
                rotationZ = destructionRotation
                alpha = destructionAlpha * finalAlpha
            }
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .border(
                width = if (highlighted || isAttackingSource || isLunging || canAttackNow) 2.dp else 1.dp,
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
                        filterQuality = FilterQuality.Medium,
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
                    com.yugioh.recomp.core.rom.rememberCardDisplayBitmap(card, isFullCard = true)
                }
                Box(
                    modifier = Modifier.fillMaxSize().padding(1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = cardBitmap,
                        contentDescription = card.name,
                        filterQuality = FilterQuality.Medium,
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
                val cardBitmap = com.yugioh.recomp.core.rom.rememberCardDisplayBitmap(card, isFullCard = true)
                Box(
                    modifier = Modifier.fillMaxSize().padding(1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = cardBitmap,
                        contentDescription = card.name,
                        filterQuality = FilterQuality.Medium,
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

                    if (hasAlreadyAttacked) {
                        Surface(
                            color = Color(0xEE0F172A),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Text(
                                text = "✓ ATTACKED",
                                color = Color(0xFF94A3B8),
                                fontSize = 6.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    } else if (isAttackingSource) {
                        Surface(
                            color = Color(0xEE0E7490),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Text(
                                text = "⚔ ATTACKING",
                                color = Color(0xFFE0F2FE),
                                fontSize = 6.5.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    } else if (canAttackNow) {
                        Surface(
                            color = Color(0xEE78350F),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Text(
                                text = "⚔ READY",
                                color = Color(0xFFFEF08A),
                                fontSize = 6.5.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(2.dp)
            ) {
                Text(
                    text = if (isSpellTrap) "🛡" else "⚔",
                    color = if (isSpellTrap) Color(0xFF38BDF8) else Color(0xFF64748B),
                    fontSize = 12.sp
                )
                Text(
                    text = if (isSpellTrap) "SPELL/TRAP" else "MONSTER",
                    color = Color(0xFF475569),
                    fontSize = 6.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Golden Summoning Portal or Spell/Trap Activation Glow
        if (isSummonAnimating) {
            if (isSpellTrap) {
                SpellTrapActivationGlow(
                    isTrap = duelCard?.card?.isTrap == true,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                GoldenSummonPortal(modifier = Modifier.fillMaxSize())
            }
        }

        // Explosive Card Destruction Burst overlay animation
        if (isDestructionAnimating) {
            CardDestructionBurst(modifier = Modifier.fillMaxSize())
        }
    }
}

// =========================================================================
// SPELL / TRAP ACTIVATION RUNIC GLOW
// =========================================================================
@Composable
private fun SpellTrapActivationGlow(
    isTrap: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "stGlow")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "stRot"
    )
    val ringPulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "stRing"
    )

    val coreColor = if (isTrap) Color(0xFFF43F5E) else Color(0xFF14B8A6)
    val outerGlow = if (isTrap) Color(0xFFBE185D) else Color(0xFF0D9488)

    Box(
        modifier = modifier.clip(RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = (min(size.width, size.height) / 2f) * ringPulse

            // Runic Hexagram Ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(coreColor.copy(alpha = 0.7f), outerGlow.copy(alpha = 0.3f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(cx, cy),
                    radius = r * 1.5f
                ),
                radius = r * 1.5f,
                center = androidx.compose.ui.geometry.Offset(cx, cy)
            )

            drawCircle(
                color = coreColor,
                radius = r * 0.9f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )

            // Magic Seal Cross Rays
            val rays = 6
            for (i in 0 until rays) {
                val rad = (i * (Math.PI / 3.0) + Math.toRadians(rotation.toDouble()))
                val rx = (cx + cos(rad) * r).toFloat()
                val ry = (cy + sin(rad) * r).toFloat()
                drawLine(
                    color = Color.White.copy(alpha = 0.8f),
                    start = androidx.compose.ui.geometry.Offset(cx, cy),
                    end = androidx.compose.ui.geometry.Offset(rx, ry),
                    strokeWidth = 1.5.dp.toPx()
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
    val fullCardBitmap = com.yugioh.recomp.core.rom.rememberCardDisplayBitmap(card, isFullCard = true)

    val animatedOffsetY by animateDpAsState(
        targetValue = if (isSelected) (-16).dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
        label = "handOffsetY"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
        label = "handScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "HandGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderGlow"
    )

    Surface(
        color = Color(0xFF141724),
        shape = RoundedCornerShape(6.dp),
        shadowElevation = if (isSelected) 8.dp else 2.dp,
        modifier = Modifier
            .width(68.dp)
            .fillMaxHeight()
            .offset(y = animatedOffsetY)
            .graphicsLayer(scaleX = animatedScale, scaleY = animatedScale)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) MillenniumGold.copy(alpha = glowAlpha) else Color(0xFF2B3245),
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
                filterQuality = FilterQuality.Medium,
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
            } else if (stepType == "SET") {
                Spacer(modifier = Modifier.height(4.dp))
                val cardBackBitmap = BitmapUtils.spriteToImageBitmap(RomAssetCache.getCardBack())
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        bitmap = cardBackBitmap,
                        contentDescription = "Face-Down Card",
                        modifier = Modifier
                            .width(26.dp)
                            .height(38.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .border(1.dp, Color(0xFF64748B), RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Face-Down Card",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "[Hidden on Field]",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
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
// GOLDEN MILLENNIUM SUMMONING PORTAL & LIGHT PILLAR
// =========================================================================
@Composable
private fun GoldenSummonPortal(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "summonRune")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "rot"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(320, easing = FastOutSlowInEasing), RepeatMode.Reverse),
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

            // 1. Soaring Vertical Golden Light Beam Pillar
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0x99FEF08A),
                        Color(0xDDFFD700),
                        Color(0x99F59E0B),
                        Color.Transparent
                    )
                ),
                topLeft = androidx.compose.ui.geometry.Offset(0f, -size.height * 0.4f),
                size = androidx.compose.ui.geometry.Size(size.width, size.height * 1.8f)
            )

            // 2. Glowing Golden Portal Radial Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(cx, cy),
                    radius = r * 1.4f
                ),
                radius = r * 1.4f,
                center = androidx.compose.ui.geometry.Offset(cx, cy)
            )

            // 3. Inner Millennium Ring
            drawCircle(
                color = Color(0xFFFFD700),
                radius = r * 0.85f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )

            // 4. Rotating Millennium Star Rays
            val numRays = 8
            for (i in 0 until numRays) {
                val rad = (i * (Math.PI / 4.0) + Math.toRadians(rotation.toDouble()))
                val rx = (cx + cos(rad) * r * 0.95f).toFloat()
                val ry = (cy + sin(rad) * r * 0.95f).toFloat()
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
    val fullCardBitmap = com.yugioh.recomp.core.rom.rememberCardDisplayBitmap(card, isFullCard = true)

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
    isGba: Boolean,
    opponentWins: Int,
    deckCapacity: Int,
    duelistLevel: Int,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    val isPlayerWin = winnerName == "Yugi"

    Surface(
        color = Color(0xF2090D18),
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
                text = if (isPlayerWin) (if (isGba) "BATTLE CITY VICTORY!" else "VICTORY!") else "DUEL DEFEAT",
                color = if (isPlayerWin) MillenniumGold else AttackRed,
                fontSize = if (isGba && isPlayerWin) 26.sp else 30.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isPlayerWin) "You defeated $opponentName!" else "$opponentName was victorious.",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = Color(0xFF131A2B),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .border(1.dp, MillenniumGold, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isGba) {
                        Text(
                            text = if (isPlayerWin) "Battle City Tournament Win Recorded!" else "Match Completed",
                            color = if (isPlayerWin) LifePointGreen else Color(0xFF94A3B8),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Record vs $opponentName: $opponentWins Wins",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
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
                    }

                    if (isPlayerWin && rewardCard != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "🎁 CARD REWARD WON FROM $opponentName:",
                            color = MillenniumGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        MiniCardPreview(rewardCard)
                        Text(
                            text = "Added to your Trunk Inventory",
                            color = Color(0xFF86EFAC),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
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
                modifier = Modifier.fillMaxWidth(0.7f).height(44.dp)
            ) {
                Text("Rematch Duel", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(0.7f).height(44.dp)
            ) {
                Text(if (isGba) "Return to Campaign Hub" else "Return to Map", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
