package com.yugioh.recomp.core.engine

import com.yugioh.recomp.core.rom.CardDataExtractor
import com.yugioh.recomp.core.rom.RomAssetCache
import com.yugioh.recomp.core.rom.RomPlatform

class DuelEngine {

    val isGbaMode: Boolean
        get() = RomAssetCache.activeTarget?.platform == RomPlatform.GBA

    val player = Duelist("Yugi", isHuman = true)
    var opponent = Duelist(
        if (RomAssetCache.activeTarget?.platform == RomPlatform.GBA) GbaCampaignManager.selectedOpponentName else DdsCampaignManager.selectedOpponentName,
        isHuman = false
    )
        private set

    var isPlayerTurn: Boolean = true
        private set

    var currentPhase: DuelPhase = DuelPhase.DRAW_PHASE
        private set

    var turnNumber: Int = 1
        private set

    var hasNormalSummonedThisTurn: Boolean = false
        private set

    var winner: String? = null
        private set

    var activeFieldSpell: Card? = null

    var clashState: ClashState? = null

    var lastTriggeredTrap: Pair<String, Int>? = null

    var lastActivatedSpell: Pair<String, String>? = null

    val activeChain: MutableList<ChainLink> = mutableListOf()
    var lastChainSummary: String? = null

    var isMatchDuel: Boolean = false
        private set
    var playerMatchWins: Int = 0
        private set
    var opponentMatchWins: Int = 0
        private set
    var matchGameNumber: Int = 1
        private set
    var isMatchFinished: Boolean = false
        private set
    var matchWinner: Duelist? = null
        private set

    private val tempControlledMonsters = mutableListOf<TempControlRecord>()

    data class TempControlRecord(
        val monster: DuelCard,
        val originalOwner: Duelist,
        val currentHolder: Duelist
    )

    var lastRewardCard: Card? = null
        private set

    var isExodiaWin: Boolean = false
        private set

    fun isToonWorldActive(duelist: Duelist): Boolean =
        duelist.spellTrapZones.filterNotNull().any { !it.position.isFaceDown && it.card.name == "Toon World" }

    fun isChainEnergyActive(): Boolean =
        player.spellTrapZones.filterNotNull().any { !it.position.isFaceDown && it.card.name == "Chain Energy" } ||
        opponent.spellTrapZones.filterNotNull().any { !it.position.isFaceDown && it.card.name == "Chain Energy" }

    fun isTollActive(): Boolean =
        player.spellTrapZones.filterNotNull().any { !it.position.isFaceDown && it.card.name == "Toll" } ||
        opponent.spellTrapZones.filterNotNull().any { !it.position.isFaceDown && it.card.name == "Toll" }

    fun applyChainEnergyTax(duelist: Duelist, actionName: String) {
        if (isChainEnergyActive() && duelist.lifePoints > 0) {
            val tax = 500.coerceAtMost(duelist.lifePoints)
            duelist.lifePoints -= tax
            log("⚡ CHAIN ENERGY: ${duelist.name} paid $tax LP to $actionName! (${duelist.lifePoints} LP remaining)")
            checkWinConditions()
        }
    }

    fun decrementSwordsOfRevealingLight(endingDuelist: Duelist) {
        if (endingDuelist.swordsTurnsRemaining > 0) {
            endingDuelist.swordsTurnsRemaining--
            if (endingDuelist.swordsTurnsRemaining == 0) {
                val opponentOfEnding = if (endingDuelist === player) opponent else player
                for (i in 0 until 5) {
                    val st = opponentOfEnding.spellTrapZones[i]
                    if (st != null && st.card.name == "Swords of Revealing Light") {
                        opponentOfEnding.spellTrapZones[i] = null
                        opponentOfEnding.graveyard.add(st.card)
                        log("⚔️ Swords of Revealing Light expired after 3 turns and shattered!")
                        break
                    }
                }
            } else {
                log("⚔️ Swords of Revealing Light: ${endingDuelist.swordsTurnsRemaining} turn(s) remaining for ${endingDuelist.name}.")
            }
        }
    }

    fun checkKuribohHandTrap(victim: Duelist, incomingDamage: Int): Int {
        if (incomingDamage <= 0) return 0
        val kuribohIdx = victim.hand.indexOfFirst { it.card.name == "Kuriboh" }
        if (kuribohIdx != -1) {
            val kuriboh = victim.hand.removeAt(kuribohIdx)
            victim.graveyard.add(kuriboh.card)
            val link = ChainLink(
                linkIndex = 1,
                card = kuriboh.card,
                isPlayer = (victim === player),
                isNegated = false,
                description = "Discarded from hand during Damage Step to reduce battle damage to 0"
            )
            activeChain.clear()
            activeChain.add(link)
            lastChainSummary = "🌰 HAND-TRAP: ${victim.name} discarded [Kuriboh]! Battle damage ($incomingDamage) reduced to 0!"
            log(lastChainSummary!!)
            return 0
        }
        return incomingDamage
    }

    fun checkSolemnSummonNegation(summonOwner: Duelist, oppDuelist: Duelist, summonedCard: Card): Boolean {
        if (isJinzoActive()) return false
        val solemnSlot = (0 until 5).firstOrNull { slot ->
            val t = oppDuelist.spellTrapZones[slot]
            t != null && t.position.isFaceDown && t.card.name == "Solemn Judgment"
        }
        if (solemnSlot != null && oppDuelist.lifePoints > 1) {
            val cost = oppDuelist.lifePoints / 2
            oppDuelist.lifePoints -= cost
            val trapCard = oppDuelist.spellTrapZones[solemnSlot]!!.card
            oppDuelist.spellTrapZones[solemnSlot] = null
            oppDuelist.graveyard.add(trapCard)
            summonOwner.graveyard.add(summonedCard)
            activeChain.clear()
            activeChain.add(ChainLink(1, summonedCard, isPlayer = (summonOwner === player), isNegated = true, "Summon Attempt"))
            activeChain.add(ChainLink(2, trapCard, isPlayer = (oppDuelist === player), isNegated = false, "Counter-Trap Negation"))
            lastChainSummary = "⛓️ CHAIN: [Chain Link 2: Solemn Judgment] negated summon of [Chain Link 1: ${summonedCard.name}]!"
            log("⚡ CHAIN LINK 1: ${summonOwner.name} attempted to summon [${summonedCard.name}]!")
            log("⚡ CHAIN LINK 2: ${oppDuelist.name} chains Counter-Trap: [Solemn Judgment] (Paid $cost LP: ${oppDuelist.lifePoints} LP remaining)!")
            log("❌ CHAIN RESOLUTION: [Solemn Judgment] negated the summon and sent [${summonedCard.name}] to the Graveyard!")
            return true
        }
        return false
    }

    fun dismissClash() {
        clashState = null
    }

    /**
     * Whether Dark Duel Stories (GBC) elemental alignment dominance rules apply.
     * In DDS GBC mode, this is active by default. In GBA TCG mode, this is disabled.
     */
    var enableDdsAlignmentRule: Boolean = true

    private val _combatLogs = mutableListOf<String>()
    val combatLogs: List<String> get() = _combatLogs

    fun log(message: String) {
        _combatLogs.add(0, message)
        if (_combatLogs.size > 50) {
            _combatLogs.removeAt(_combatLogs.lastIndex)
        }
    }

    /**
     * Initializes a fresh duel match with 8000 LP and 5-card opening hands
     * using authentic platform-specific decks (GBA TCG Expert vs DDS GBC).
     */
    fun startDuel(
        opponentName: String = if (isGbaMode) GbaCampaignManager.selectedOpponentName else DdsCampaignManager.selectedOpponentName,
        startingLp: Int = 8000,
        customOpponentDeck: List<Card>? = null
    ) {
        opponent = Duelist(opponentName, isHuman = false)

        player.lifePoints = startingLp
        player.deck.clear()
        player.hand.clear()
        player.graveyard.clear()
        player.banishedZone.clear()
        player.sideDeck.clear()
        player.sideDeck.addAll(GbaCampaignManager.activeSideDeck.ifEmpty { GbaCampaignManager.getStarterSideDeck() })
        player.extraDeck.clear()
        player.extraDeck.addAll(FusionRitualRegistry.getDefaultExtraDeck())
        player.swordsTurnsRemaining = 0
        for (i in 0 until 5) {
            player.monsterZones[i] = null
            player.spellTrapZones[i] = null
        }

        opponent.lifePoints = startingLp
        opponent.deck.clear()
        opponent.hand.clear()
        opponent.graveyard.clear()
        opponent.banishedZone.clear()
        opponent.sideDeck.clear()
        opponent.sideDeck.addAll(GbaCampaignManager.getStarterSideDeck())
        opponent.extraDeck.clear()
        opponent.extraDeck.addAll(FusionRitualRegistry.getDefaultExtraDeck())
        opponent.swordsTurnsRemaining = 0
        for (i in 0 until 5) {
            opponent.monsterZones[i] = null
            opponent.spellTrapZones[i] = null
        }

        val (pDeck, oDeck) = if (customOpponentDeck != null) {
            val playerDeck = if (GbaCampaignManager.activeDeck.size in 40..60) {
                GbaCampaignManager.activeDeck.toList()
            } else {
                GbaCampaignManager.getGbaStarterDeck()
            }
            Pair(playerDeck.shuffled(), customOpponentDeck.shuffled())
        } else if (isGbaMode) {
            val playerDeck = if (GbaCampaignManager.activeDeck.size in 40..60) {
                GbaCampaignManager.activeDeck.toList()
            } else {
                GbaCampaignManager.getGbaStarterDeck()
            }
            val oppDeck = GbaCampaignManager.buildGbaOpponentDeck(opponentName)
            Pair(playerDeck.shuffled(), oppDeck.shuffled())
        } else {
            CardDataExtractor.buildStarterDecks()
        }
        player.deck.addAll(pDeck)
        opponent.deck.addAll(oDeck)

        // Draw 5 opening cards
        for (i in 0 until 5) {
            drawCard(isPlayer = true, ignoreHandLimit = true)
            drawCard(isPlayer = false, ignoreHandLimit = true)
        }

        turnNumber = 1
        isPlayerTurn = true
        hasNormalSummonedThisTurn = false
        activeFieldSpell = null
        winner = null
        isExodiaWin = false
        clashState = null
        lastTriggeredTrap = null
        lastRewardCard = null
        _combatLogs.clear()

        val rulesLabel = if (isGbaMode) "GBA TCG Expert Rules" else "DDS GBC Rules"
        log("=== DUEL STARTED vs $opponentName! ===")
        log("Initial Life Points: $startingLp | $startingLp ($rulesLabel)")
        log("Turn 1 - ${player.name}'s Turn (Main Phase 1)")

        currentPhase = DuelPhase.MAIN_PHASE_1
    }

    fun startMatchDuel(
        opponentName: String = if (isGbaMode) GbaCampaignManager.selectedOpponentName else DdsCampaignManager.selectedOpponentName,
        startingLp: Int = 8000,
        customOpponentDeck: List<Card>? = null
    ) {
        isMatchDuel = true
        playerMatchWins = 0
        opponentMatchWins = 0
        matchGameNumber = 1
        isMatchFinished = false
        matchWinner = null
        startDuel(opponentName, startingLp, customOpponentDeck)
        log("🏆 BEST-OF-3 TOURNAMENT MATCH STARTED! Game 1 / 3 against ${opponent.name}.")
    }

    fun advanceMatchGame(): Boolean {
        if (!isMatchDuel || isMatchFinished || winner == null) return false
        matchGameNumber++
        winner = null
        isExodiaWin = false
        turnNumber = 1
        isPlayerTurn = true
        currentPhase = DuelPhase.MAIN_PHASE_1
        hasNormalSummonedThisTurn = false
        activeFieldSpell = null
        clashState = null
        lastTriggeredTrap = null
        lastActivatedSpell = null
        activeChain.clear()
        lastChainSummary = null

        player.lifePoints = 8000
        player.deck.clear()
        player.hand.clear()
        player.graveyard.clear()
        player.banishedZone.clear()
        player.swordsTurnsRemaining = 0
        player.isWabokuActive = false
        player.temporaryAtkBuffs.clear()
        for (i in 0 until 5) {
            player.monsterZones[i] = null
            player.spellTrapZones[i] = null
        }

        opponent.lifePoints = 8000
        opponent.deck.clear()
        opponent.hand.clear()
        opponent.graveyard.clear()
        opponent.banishedZone.clear()
        opponent.swordsTurnsRemaining = 0
        opponent.isWabokuActive = false
        opponent.temporaryAtkBuffs.clear()
        for (i in 0 until 5) {
            opponent.monsterZones[i] = null
            opponent.spellTrapZones[i] = null
        }

        val pDeck = if (GbaCampaignManager.activeDeck.size in 40..60) {
            GbaCampaignManager.activeDeck.toList()
        } else {
            GbaCampaignManager.getGbaStarterDeck()
        }
        val oDeck = GbaCampaignManager.buildGbaOpponentDeck(opponent.name)
        player.deck.addAll(pDeck.shuffled())
        opponent.deck.addAll(oDeck.shuffled())

        for (i in 0 until 5) {
            drawCard(isPlayer = true, ignoreHandLimit = true)
            drawCard(isPlayer = false, ignoreHandLimit = true)
        }
        log("=== MATCH GAME $matchGameNumber BEGINS! (Score: ${player.name} $playerMatchWins - ${opponent.name} $opponentMatchWins) ===")
        return true
    }

    fun swapSideDeckCard(isPlayer: Boolean, mainDeckCardIndex: Int, sideDeckCardIndex: Int): Boolean {
        val duelist = if (isPlayer) player else opponent
        if (mainDeckCardIndex !in duelist.deck.indices || sideDeckCardIndex !in duelist.sideDeck.indices) {
            return false
        }
        val mainCard = duelist.deck[mainDeckCardIndex]
        val sideCard = duelist.sideDeck[sideDeckCardIndex]

        duelist.deck[mainDeckCardIndex] = sideCard
        duelist.sideDeck[sideDeckCardIndex] = mainCard
        if (isPlayer) {
            val gbaIdx = GbaCampaignManager.activeDeck.indexOfFirst { it.name == mainCard.name }
            if (gbaIdx != -1) {
                GbaCampaignManager.activeDeck[gbaIdx] = sideCard
            }
        }
        log("🔄 SIDE DECK: ${duelist.name} swapped [${mainCard.name}] for [${sideCard.name}] from Side Deck.")
        return true
    }

    /**
     * Draws one card from the deck into the hand.
     * In GBC DDS, hand cannot exceed 5 cards; in GBA TCG rules, standard draw applies.
     */
    fun drawCard(isPlayer: Boolean, ignoreHandLimit: Boolean = false): Card? {
        val duelist = if (isPlayer) player else opponent
        if (!isGbaMode && !ignoreHandLimit && duelist.hand.size >= 5) {
            log("${duelist.name}'s hand is at maximum capacity (5 cards). No card drawn.")
            return null
        }
        if (duelist.deck.isEmpty()) {
            val opponentName = if (isPlayer) opponent.name else player.name
            winner = opponentName
            log("${duelist.name} cannot draw from an empty Deck! $opponentName wins!")
            return null
        }
        val card = duelist.deck.removeAt(0)
        duelist.hand.add(DuelCard(card = card))
        log("${duelist.name} drew [${card.name}].")
        checkWinConditions()
        return card
    }

    /**
     * Checks if a card can be summoned / set.
     * Enforces GBA TCG Tribute rules (Level 5-6: 1 tribute, Level 7+: 2 tributes)
     * or GBC DDS free-summon rules.
     */
    fun canSummon(isPlayer: Boolean, card: Card): Pair<Boolean, String> {
        if (winner != null) return Pair(false, "Duel is already finished")
        if (isPlayer != isPlayerTurn) return Pair(false, "Not your turn")
        if (currentPhase != DuelPhase.MAIN_PHASE_1 && currentPhase != DuelPhase.MAIN_PHASE_2) {
            return Pair(false, "Can only summon during Main Phase")
        }
        if (hasNormalSummonedThisTurn) {
            return Pair(false, "Already performed a Normal Summon / Set this turn")
        }

        val duelist = if (isPlayer) player else opponent
        if (ToonAnimeArchetype.isToonMonster(card) && !isToonWorldActive(duelist)) {
            return Pair(false, "Requires Toon World face-up on the field to summon Toon monsters")
        }
        if (isGbaMode) {
            val tributesNeeded = when {
                card.level >= 7 -> 2
                card.level in 5..6 -> 1
                else -> 0
            }
            val activeMonsters = (0 until 5).count {
                val m = duelist.monsterZones[it]
                m != null && m.canBeTributed && !m.isToken
            }
            if (activeMonsters < tributesNeeded) {
                return Pair(false, "Requires $tributesNeeded Tribute(s) on field ($activeMonsters available)")
            }
            if (tributesNeeded == 0 && duelist.firstEmptyMonsterSlot() == -1) {
                return Pair(false, "Monster Zone is full")
            }
        }
        return Pair(true, "OK")
    }

    /**
     * Summons or sets a monster to the specified zone slot.
     * In GBA mode, consumes required tributes and triggers trap checks.
     * In GBC mode, supports Dark Duel Stories on-the-field fusions and monster replacement.
     */
    fun summonMonster(
        isPlayer: Boolean,
        handIndex: Int,
        slotIndex: Int,
        tributeSlots: List<Int> = emptyList(),
        setDefense: Boolean = false
    ): Boolean {
        val duelist = if (isPlayer) player else opponent
        if (handIndex !in duelist.hand.indices) return false
        val duelCard = duelist.hand[handIndex]
        val card = duelCard.card
        if (!card.isMonster) return false

        val (can, reason) = canSummon(isPlayer, card)
        if (!can) {
            log("Cannot summon: $reason")
            return false
        }

        if (isGbaMode) {
            val tributesNeeded = when {
                card.level >= 7 -> 2
                card.level in 5..6 -> 1
                else -> 0
            }

            if (tributesNeeded > 0) {
                val actualTributes = mutableListOf<Int>()
                if (tributeSlots.size == tributesNeeded && tributeSlots.all { duelist.monsterZones.getOrNull(it) != null }) {
                    actualTributes.addAll(tributeSlots)
                } else {
                    // Smart auto-tribute: select weakest monsters on field
                    val available = (0 until 5).filter { duelist.monsterZones[it] != null }
                        .sortedBy { duelist.monsterZones[it]?.card?.atk ?: 0 }
                    if (available.size < tributesNeeded) return false
                    actualTributes.addAll(available.take(tributesNeeded))
                }

                for (tSlot in actualTributes) {
                    val tMon = duelist.monsterZones[tSlot]!!.card
                    duelist.graveyard.add(tMon)
                    duelist.monsterZones[tSlot] = null
                    log("${duelist.name} Tributed [${tMon.name}] from Zone ${tSlot + 1}!")
                }
            }

            val targetSlot = if (slotIndex in 0..4 && duelist.monsterZones[slotIndex] == null) {
                slotIndex
            } else {
                duelist.firstEmptyMonsterSlot()
            }
            if (targetSlot == -1) return false

            val opponentDuelist = if (isPlayer) opponent else player
            if (!setDefense && checkSolemnSummonNegation(duelist, opponentDuelist, card)) {
                duelist.hand.removeAt(handIndex)
                hasNormalSummonedThisTurn = true
                return true
            }

            duelist.hand.removeAt(handIndex)
            val pos = if (setDefense) CardPosition.FACE_DOWN_DEFENSE else CardPosition.FACE_UP_ATTACK
            duelist.monsterZones[targetSlot] = DuelCard(card = card, position = pos, canChangePosition = false)
            hasNormalSummonedThisTurn = true

            val actionDesc = if (tributesNeeded > 0) "Tribute Summoned" else if (setDefense) "Set" else "Normal Summoned"
            log("${duelist.name} $actionDesc [${card.name}] (ATK ${card.atk} / DEF ${card.def}) to Zone ${targetSlot + 1}.")

            applyChainEnergyTax(duelist, if (setDefense) "Set monster" else "Summon [${card.name}]")
            checkTrapHole(opponentDuelist, duelist, targetSlot)
            checkTorrentialTribute(opponentDuelist, duelist.monsterZones[targetSlot])

            return true
        }

        // GBC Dark Duel Stories Logic (Fusion & Replacement)
        val targetSlot = if (slotIndex in 0..4) slotIndex else duelist.firstEmptyMonsterSlot()
        if (targetSlot == -1) return false

        val existing = duelist.monsterZones[targetSlot]
        if (existing != null) {
            val fusion = DdsCampaignManager.checkFusion(existing.card, card)
            if (fusion != null) {
                log("⚡ GBC FUSION! [${existing.card.name}] + [${card.name}] = [${fusion.name}]! (ATK ${fusion.atk} / DEF ${fusion.def})")
                duelist.hand.removeAt(handIndex)
                duelist.graveyard.add(existing.card)
                duelist.graveyard.add(card)
                duelist.monsterZones[targetSlot] = DuelCard(
                    card = fusion,
                    position = if (setDefense) CardPosition.FACE_UP_DEFENSE else CardPosition.FACE_UP_ATTACK,
                    canChangePosition = false
                )
                hasNormalSummonedThisTurn = true
                return true
            } else {
                log("[${card.name}] replaces [${existing.card.name}] in Zone ${targetSlot + 1}.")
                duelist.graveyard.add(existing.card)
            }
        }

        val opponentDuelist = if (isPlayer) opponent else player
        if (!setDefense && checkSolemnSummonNegation(duelist, opponentDuelist, card)) {
            duelist.hand.removeAt(handIndex)
            hasNormalSummonedThisTurn = true
            return true
        }

        duelist.hand.removeAt(handIndex)
        val pos = if (setDefense) CardPosition.FACE_DOWN_DEFENSE else CardPosition.FACE_UP_ATTACK
        duelist.monsterZones[targetSlot] = DuelCard(card = card, position = pos, canChangePosition = false)
        hasNormalSummonedThisTurn = true
        log("${duelist.name} placed [${card.name}] in Zone ${targetSlot + 1}.")

        applyChainEnergyTax(duelist, if (setDefense) "Set monster" else "Summon [${card.name}]")
        checkTrapHole(opponentDuelist, duelist, targetSlot)
        checkTorrentialTribute(opponentDuelist, duelist.monsterZones[targetSlot])

        return true
    }

    /**
     * Toggles a monster's battle position between Attack and Defense position.
     */
    fun changeBattlePosition(isPlayer: Boolean, slotIndex: Int): Boolean {
        val duelist = if (isPlayer) player else opponent
        if (isPlayer != isPlayerTurn || winner != null) return false
        if (currentPhase != DuelPhase.MAIN_PHASE_1 && currentPhase != DuelPhase.MAIN_PHASE_2) return false
        if (slotIndex !in 0..4) return false

        val duelCard = duelist.monsterZones[slotIndex] ?: return false
        if (!duelCard.canChangePosition) {
            log("Cannot change position of ${duelCard.card.name} again this turn")
            return false
        }

        val oldPos = duelCard.position
        duelCard.position = when (duelCard.position) {
            CardPosition.FACE_UP_ATTACK -> CardPosition.FACE_UP_DEFENSE
            CardPosition.FACE_UP_DEFENSE -> CardPosition.FACE_UP_ATTACK
            CardPosition.FACE_DOWN_DEFENSE -> CardPosition.FACE_UP_ATTACK
            else -> CardPosition.FACE_UP_ATTACK
        }
        duelCard.canChangePosition = false
        val action = if (oldPos == CardPosition.FACE_DOWN_DEFENSE) "Flip Summoned" else "changed"
        log("${duelist.name} $action [${duelCard.card.name}] to ${duelCard.position.displayName}.")

        // Flip Summon Triggers (Trap Hole & Flip Effects)
        if (oldPos == CardPosition.FACE_DOWN_DEFENSE) {
            val opponentDuelist = if (isPlayer) opponent else player
            checkTrapHole(opponentDuelist, duelist, slotIndex)
            triggerFlipEffect(duelist, opponentDuelist, duelCard.card, slotIndex, null)
        }
        return true
    }

    fun getFieldSpellModifier(field: Card?, monster: Card): Int {
        if (field == null || !monster.isMonster) return 0
        return when (field.name) {
            "Yami" -> when (monster.monsterType) {
                MonsterType.SPELLCASTER, MonsterType.FIEND -> 200
                MonsterType.FAIRY -> -200
                else -> 0
            }
            "Forest" -> when (monster.monsterType) {
                MonsterType.INSECT, MonsterType.BEAST, MonsterType.PLANT, MonsterType.BEAST_WARRIOR -> 200
                else -> 0
            }
            "Sogen" -> when (monster.monsterType) {
                MonsterType.WARRIOR, MonsterType.BEAST_WARRIOR -> 200
                else -> 0
            }
            "Mountain" -> when (monster.monsterType) {
                MonsterType.DRAGON, MonsterType.WINGED_BEAST, MonsterType.THUNDER -> 200
                else -> 0
            }
            "Umi" -> when (monster.monsterType) {
                MonsterType.FISH, MonsterType.SEA_SERPENT, MonsterType.THUNDER, MonsterType.AQUA -> 200
                MonsterType.MACHINE, MonsterType.PYRO -> -200
                else -> 0
            }
            "Wasteland" -> when (monster.monsterType) {
                MonsterType.DINOSAUR, MonsterType.ZOMBIE, MonsterType.ROCK -> 200
                else -> 0
            }
            else -> 0
        }
    }

    fun getEffectiveAtk(card: Card, duelCard: DuelCard? = null): Int {
        var atk = card.atk + getFieldSpellModifier(activeFieldSpell, card)
        if (duelCard != null) {
            for (equip in duelCard.equippedCards) {
                atk += when (equip.name) {
                    "Axe of Despair" -> 1000
                    "Malevolent Nuzzler" -> 700
                    "Black Pendant" -> 500
                    "Horn of the Unicorn" -> 700
                    "Megamorph" -> {
                        val playerControls = player.monsterZones.any { it === duelCard }
                        val isLower = if (playerControls) player.lifePoints < opponent.lifePoints else opponent.lifePoints < player.lifePoints
                        if (isLower) card.atk else -(card.atk / 2)
                    }
                    else -> 0
                }
            }
        }
        return atk.coerceAtLeast(0)
    }

    fun getEffectiveDef(card: Card, duelCard: DuelCard? = null): Int {
        var def = card.def + getFieldSpellModifier(activeFieldSpell, card)
        if (duelCard != null) {
            for (equip in duelCard.equippedCards) {
                def += when (equip.name) {
                    "Horn of the Unicorn" -> 700
                    else -> 0
                }
            }
        }
        return def.coerceAtLeast(0)
    }

    fun triggerFlipEffect(
        flipOwner: Duelist,
        oppDuelist: Duelist,
        flippedCard: Card,
        flippedSlot: Int,
        attackingSlot: Int? = null
    ) {
        when (flippedCard.name) {
            "Man-Eater Bug" -> {
                if (attackingSlot != null && oppDuelist.monsterZones[attackingSlot] != null) {
                    val attacker = oppDuelist.monsterZones[attackingSlot]!!
                    oppDuelist.monsterZones[attackingSlot] = null
                    oppDuelist.graveyard.add(attacker.card)
                    log("⚡ FLIP EFFECT: [Man-Eater Bug] destroyed the attacking monster [${attacker.card.name}]!")
                } else {
                    val target = (0 until 5).firstOrNull { oppDuelist.monsterZones[it] != null }
                    if (target != null) {
                        val victim = oppDuelist.monsterZones[target]!!
                        oppDuelist.monsterZones[target] = null
                        oppDuelist.graveyard.add(victim.card)
                        log("⚡ FLIP EFFECT: [Man-Eater Bug] destroyed [${victim.card.name}] in Zone ${target + 1}!")
                    }
                }
            }
            "Cyber Jar" -> {
                log("⚡ FLIP EFFECT: [Cyber Jar] triggered! All monsters on the field are destroyed!")
                var count = 0
                for (i in 0 until 5) {
                    player.monsterZones[i]?.let {
                        player.graveyard.add(it.card)
                        player.monsterZones[i] = null
                        count++
                    }
                    opponent.monsterZones[i]?.let {
                        opponent.graveyard.add(it.card)
                        opponent.monsterZones[i] = null
                        count++
                    }
                }
                log("Cyber Jar destroyed $count monster(s) on the field!")
                repeat(2) {
                    if (player.deck.isNotEmpty()) drawCard(isPlayer = true, ignoreHandLimit = true)
                    if (opponent.deck.isNotEmpty()) drawCard(isPlayer = false, ignoreHandLimit = true)
                }
            }
            "Morphing Jar" -> {
                log("⚡ FLIP EFFECT: [Morphing Jar]! Both players discard hands and draw 5 cards!")
                player.graveyard.addAll(player.hand.map { it.card })
                player.hand.clear()
                opponent.graveyard.addAll(opponent.hand.map { it.card })
                opponent.hand.clear()
                for (i in 0 until 5) {
                    if (player.deck.isNotEmpty()) drawCard(isPlayer = true, ignoreHandLimit = true)
                    if (opponent.deck.isNotEmpty()) drawCard(isPlayer = false, ignoreHandLimit = true)
                }
            }
            "Hane-Hane" -> {
                if (attackingSlot != null && oppDuelist.monsterZones[attackingSlot] != null) {
                    val attacker = oppDuelist.monsterZones[attackingSlot]!!
                    oppDuelist.monsterZones[attackingSlot] = null
                    oppDuelist.hand.add(DuelCard(card = attacker.card))
                    log("⚡ FLIP EFFECT: [Hane-Hane] returned attacking [${attacker.card.name}] to ${oppDuelist.name}'s hand!")
                } else {
                    val target = (0 until 5).firstOrNull { oppDuelist.monsterZones[it] != null }
                    if (target != null) {
                        val returned = oppDuelist.monsterZones[target]!!
                        oppDuelist.monsterZones[target] = null
                        oppDuelist.hand.add(DuelCard(card = returned.card))
                        log("⚡ FLIP EFFECT: [Hane-Hane] returned [${returned.card.name}] to ${oppDuelist.name}'s hand!")
                    }
                }
            }
            "Penguin Soldier" -> {
                log("⚡ FLIP EFFECT: [Penguin Soldier] activated!")
                var returnedCount = 0
                for (i in 0 until 5) {
                    val oppMon = oppDuelist.monsterZones[i]
                    if (oppMon != null && returnedCount < 2) {
                        oppDuelist.monsterZones[i] = null
                        oppDuelist.hand.add(DuelCard(card = oppMon.card))
                        returnedCount++
                        log("Penguin Soldier returned [${oppMon.card.name}] to ${oppDuelist.name}'s hand!")
                    }
                }
            }
            "Needle Worm" -> {
                val count = minOf(5, oppDuelist.deck.size)
                log("⚡ FLIP EFFECT: [Needle Worm]! ${oppDuelist.name} discards top 5 cards from Deck!")
                for (i in 0 until count) {
                    oppDuelist.graveyard.add(oppDuelist.deck.removeAt(0))
                }
            }
            "Magician of Faith" -> {
                val spellInGrave = flipOwner.graveyard.firstOrNull { it.isSpell }
                if (spellInGrave != null) {
                    flipOwner.graveyard.remove(spellInGrave)
                    flipOwner.hand.add(DuelCard(card = spellInGrave))
                    log("⚡ FLIP EFFECT: [Magician of Faith] returned [${spellInGrave.name}] from Graveyard to ${flipOwner.name}'s hand!")
                }
            }
            "Mask of Darkness" -> {
                val trapInGrave = flipOwner.graveyard.firstOrNull { it.isTrap }
                if (trapInGrave != null) {
                    flipOwner.graveyard.remove(trapInGrave)
                    flipOwner.hand.add(DuelCard(card = trapInGrave))
                    log("⚡ FLIP EFFECT: [Mask of Darkness] returned [${trapInGrave.name}] from Graveyard to ${flipOwner.name}'s hand!")
                }
            }
            "Trap Master" -> {
                val targetTrap = (0 until 5).firstOrNull { oppDuelist.spellTrapZones[it] != null }
                if (targetTrap != null) {
                    val destroyedTrap = oppDuelist.spellTrapZones[targetTrap]!!
                    oppDuelist.spellTrapZones[targetTrap] = null
                    oppDuelist.graveyard.add(destroyedTrap.card)
                    log("⚡ FLIP EFFECT: [Trap Master] destroyed [${destroyedTrap.card.name}] in Zone ${targetTrap + 1}!")
                }
            }
        }
    }

    fun isJinzoActive(): Boolean {
        return player.hasFaceUpMonster("Jinzo") || opponent.hasFaceUpMonster("Jinzo")
    }

    fun isImperialOrderActive(): Boolean {
        return player.spellTrapZones.filterNotNull().any { !it.position.isFaceDown && it.card.name == "Imperial Order" } ||
            opponent.spellTrapZones.filterNotNull().any { !it.position.isFaceDown && it.card.name == "Imperial Order" }
    }

    fun isFlipEffectMonster(card: Card): Boolean =
        card.name in listOf(
            "Man-Eater Bug", "Cyber Jar", "Morphing Jar", "Hane-Hane",
            "Penguin Soldier", "Needle Worm", "Magician of Faith",
            "Mask of Darkness", "Trap Master", "Skelengel"
        ) || card.description.contains("FLIP:", ignoreCase = true)

    fun handleStandbyPhaseUpkeep() {
        val currentDuelist = if (isPlayerTurn) player else opponent
        // Imperial Order 700 LP maintenance upkeep
        for (i in 0 until 5) {
            val st = currentDuelist.spellTrapZones[i]
            if (st != null && !st.position.isFaceDown && st.card.name == "Imperial Order") {
                if (currentDuelist.lifePoints > 700) {
                    currentDuelist.lifePoints -= 700
                    log("⚡ IMPERIAL ORDER: ${currentDuelist.name} paid 700 LP maintenance cost! (${currentDuelist.lifePoints} LP remaining)")
                    checkWinConditions()
                } else {
                    currentDuelist.spellTrapZones[i] = null
                    currentDuelist.graveyard.add(st.card)
                    log("⚡ IMPERIAL ORDER: ${currentDuelist.name} could not pay 700 LP maintenance cost! Imperial Order was destroyed!")
                }
            }
        }
    }

    fun triggerGraveyardSearcher(card: Card, duelist: Duelist) {
        if (card.name == "Sangan") {
            val target = duelist.deck.firstOrNull { it.isMonster && it.atk <= 1500 }
            if (target != null) {
                duelist.deck.remove(target)
                duelist.hand.add(DuelCard(card = target))
                log("⚡ SEARCH EFFECT: [Sangan] sent to GY! Added [${target.name}] (ATK ${target.atk} <= 1500) from Deck to Hand!")
                checkWinConditions()
            }
        } else if (card.name == "Witch of the Black Forest") {
            val target = duelist.deck.firstOrNull { it.isMonster && it.def <= 1500 }
            if (target != null) {
                duelist.deck.remove(target)
                duelist.hand.add(DuelCard(card = target))
                log("⚡ SEARCH EFFECT: [Witch of the Black Forest] sent to GY! Added [${target.name}] (DEF ${target.def} <= 1500) from Deck to Hand!")
                checkWinConditions()
            }
        }
    }

    fun checkToonWorldDestruction(duelist: Duelist) {
        if (!isToonWorldActive(duelist)) {
            val opp = if (duelist === player) opponent else player
            for (i in 0 until 5) {
                val m = duelist.monsterZones[i]
                if (m != null && !m.position.isFaceDown && ToonAnimeArchetype.isToonMonster(m.card)) {
                    cleanupMonsterEquips(m, duelist, opp)
                    duelist.graveyard.add(m.card)
                    duelist.monsterZones[i] = null
                    triggerGraveyardSearcher(m.card, duelist)
                    log("📖 TOON COLLAPSE: [${m.card.name}] cannot survive without Toon World and was destroyed!")
                }
            }
        }
    }

    private fun checkTrapHole(trapOwner: Duelist, summoningDuelist: Duelist, summonedSlot: Int) {
        val summonedMonster = summoningDuelist.monsterZones[summonedSlot] ?: return
        if (summonedMonster.position.isFaceDown || summonedMonster.card.atk < 1000) return

        if (isJinzoActive()) {
            log("⚡ Jinzo's continuous effect prevents Trap Card activation!")
            return
        }

        val trapSlot = (0 until 5).firstOrNull { slot ->
            val t = trapOwner.spellTrapZones[slot]
            t != null && t.position.isFaceDown && t.card.name == "Trap Hole"
        }
        if (trapSlot != null) {
            val trapCard = trapOwner.spellTrapZones[trapSlot]!!.card
            trapOwner.spellTrapZones[trapSlot] = null
            trapOwner.graveyard.add(trapCard)

            // Summoning duelist counter-trap check: Seven Tools or Solemn Judgment
            val counterSlot = (0 until 5).firstOrNull { slot ->
                val t = summoningDuelist.spellTrapZones[slot]
                t != null && t.position.isFaceDown && (t.card.name == "Seven Tools of the Bandit" || t.card.name == "Solemn Judgment")
            }
            if (counterSlot != null) {
                val counterTrap = summoningDuelist.spellTrapZones[counterSlot]!!.card
                if (counterTrap.name == "Seven Tools of the Bandit" && summoningDuelist.lifePoints > 1000) {
                    summoningDuelist.lifePoints -= 1000
                    summoningDuelist.spellTrapZones[counterSlot] = null
                    summoningDuelist.graveyard.add(counterTrap)
                    activeChain.clear()
                    activeChain.add(ChainLink(1, trapCard, isPlayer = (trapOwner === player), isNegated = true, "Trap Hole Trigger"))
                    activeChain.add(ChainLink(2, counterTrap, isPlayer = (summoningDuelist === player), isNegated = false, "Counter-Trap Negation"))
                    lastChainSummary = "⛓️ CHAIN: [Chain Link 2: Seven Tools of the Bandit] negated [Chain Link 1: Trap Hole]!"
                    log("⚡ CHAIN LINK 1: ${trapOwner.name} triggered Trap: [Trap Hole]!")
                    log("⚡ CHAIN LINK 2: ${summoningDuelist.name} chains Counter-Trap: [Seven Tools of the Bandit] (Paid 1000 LP)!")
                    log("❌ CHAIN RESOLUTION: [Seven Tools of the Bandit] negates and destroys [Trap Hole]!")
                    return
                } else if (counterTrap.name == "Solemn Judgment" && summoningDuelist.lifePoints > 1) {
                    val cost = summoningDuelist.lifePoints / 2
                    summoningDuelist.lifePoints -= cost
                    summoningDuelist.spellTrapZones[counterSlot] = null
                    summoningDuelist.graveyard.add(counterTrap)
                    activeChain.clear()
                    activeChain.add(ChainLink(1, trapCard, isPlayer = (trapOwner === player), isNegated = true, "Trap Hole Trigger"))
                    activeChain.add(ChainLink(2, counterTrap, isPlayer = (summoningDuelist === player), isNegated = false, "Counter-Trap Negation"))
                    lastChainSummary = "⛓️ CHAIN: [Chain Link 2: Solemn Judgment] negated [Chain Link 1: Trap Hole]!"
                    log("⚡ CHAIN LINK 1: ${trapOwner.name} triggered Trap: [Trap Hole]!")
                    log("⚡ CHAIN LINK 2: ${summoningDuelist.name} chains Counter-Trap: [Solemn Judgment] (Paid $cost LP)!")
                    log("❌ CHAIN RESOLUTION: [Solemn Judgment] negates and destroys [Trap Hole]!")
                    return
                }
            }

            summoningDuelist.monsterZones[summonedSlot] = null
            summoningDuelist.graveyard.add(summonedMonster.card)
            triggerGraveyardSearcher(summonedMonster.card, summoningDuelist)
            lastTriggeredTrap = Pair("Trap Hole", summonedSlot)
            log("TRAP TRIGGER: ${trapOwner.name} activated [Trap Hole]! [${summonedMonster.card.name}] was destroyed!")
        }
    }

    private fun checkTorrentialTribute(trapOwner: Duelist, summonedMonster: DuelCard?) {
        if (summonedMonster == null || isJinzoActive()) return
        val trapSlot = (0 until 5).firstOrNull { slot ->
            val t = trapOwner.spellTrapZones[slot]
            t != null && t.position.isFaceDown && t.card.name == "Torrential Tribute"
        }
        if (trapSlot != null) {
            val trapCard = trapOwner.spellTrapZones[trapSlot]!!.card
            trapOwner.spellTrapZones[trapSlot] = null
            trapOwner.graveyard.add(trapCard)

            val summoningDuelist = if (trapOwner === player) opponent else player

            // Summoning duelist counter-trap check: Seven Tools or Solemn Judgment
            val counterSlot = (0 until 5).firstOrNull { slot ->
                val t = summoningDuelist.spellTrapZones[slot]
                t != null && t.position.isFaceDown && (t.card.name == "Seven Tools of the Bandit" || t.card.name == "Solemn Judgment")
            }
            if (counterSlot != null) {
                val counterTrap = summoningDuelist.spellTrapZones[counterSlot]!!.card
                if (counterTrap.name == "Seven Tools of the Bandit" && summoningDuelist.lifePoints > 1000) {
                    summoningDuelist.lifePoints -= 1000
                    summoningDuelist.spellTrapZones[counterSlot] = null
                    summoningDuelist.graveyard.add(counterTrap)
                    activeChain.clear()
                    activeChain.add(ChainLink(1, trapCard, isPlayer = (trapOwner === player), isNegated = true, "Torrential Tribute Trigger"))
                    activeChain.add(ChainLink(2, counterTrap, isPlayer = (summoningDuelist === player), isNegated = false, "Counter-Trap Negation"))
                    lastChainSummary = "⛓️ CHAIN: [Chain Link 2: Seven Tools of the Bandit] negated [Chain Link 1: Torrential Tribute]!"
                    log("⚡ CHAIN LINK 1: ${trapOwner.name} triggered Trap: [Torrential Tribute]!")
                    log("⚡ CHAIN LINK 2: ${summoningDuelist.name} chains Counter-Trap: [Seven Tools of the Bandit] (Paid 1000 LP)!")
                    log("❌ CHAIN RESOLUTION: [Seven Tools of the Bandit] negates and destroys [Torrential Tribute]!")
                    return
                } else if (counterTrap.name == "Solemn Judgment" && summoningDuelist.lifePoints > 1) {
                    val cost = summoningDuelist.lifePoints / 2
                    summoningDuelist.lifePoints -= cost
                    summoningDuelist.spellTrapZones[counterSlot] = null
                    summoningDuelist.graveyard.add(counterTrap)
                    activeChain.clear()
                    activeChain.add(ChainLink(1, trapCard, isPlayer = (trapOwner === player), isNegated = true, "Torrential Tribute Trigger"))
                    activeChain.add(ChainLink(2, counterTrap, isPlayer = (summoningDuelist === player), isNegated = false, "Counter-Trap Negation"))
                    lastChainSummary = "⛓️ CHAIN: [Chain Link 2: Solemn Judgment] negated [Chain Link 1: Torrential Tribute]!"
                    log("⚡ CHAIN LINK 1: ${trapOwner.name} triggered Trap: [Torrential Tribute]!")
                    log("⚡ CHAIN LINK 2: ${summoningDuelist.name} chains Counter-Trap: [Solemn Judgment] (Paid $cost LP)!")
                    log("❌ CHAIN RESOLUTION: [Solemn Judgment] negates and destroys [Torrential Tribute]!")
                    return
                }
            }

            var count = 0
            for (i in 0 until 5) {
                player.monsterZones[i]?.let {
                    cleanupMonsterEquips(it, player, opponent)
                    if (!it.isToken) {
                        player.graveyard.add(it.card)
                        triggerGraveyardSearcher(it.card, player)
                    }
                    player.monsterZones[i] = null
                    count++
                }
                opponent.monsterZones[i]?.let {
                    cleanupMonsterEquips(it, opponent, player)
                    if (!it.isToken) {
                        opponent.graveyard.add(it.card)
                        triggerGraveyardSearcher(it.card, opponent)
                    }
                    opponent.monsterZones[i] = null
                    count++
                }
            }
            lastTriggeredTrap = Pair("Torrential Tribute", -1)
            log("🌊 TRAP TRIGGER: ${trapOwner.name} activated [Torrential Tribute]! A massive tidal wave destroyed all $count monster(s) on the field!")
            checkWinConditions()
        }
    }

    fun isContinuousOrEquipSpell(card: Card): Boolean =
        card.name in listOf("Toon World", "Premature Burial", "Chain Energy", "Toll", "Swords of Revealing Light") || card.isEquipSpell

    /**
     * Activates a Spell card from the hand.
     */
    fun activateSpell(isPlayer: Boolean, handIndex: Int): Boolean {
        val duelist = if (isPlayer) player else opponent
        if (isPlayer != isPlayerTurn || winner != null) return false
        if (handIndex !in duelist.hand.indices) return false

        val duelCard = duelist.hand[handIndex]
        val card = duelCard.card
        if (!card.isSpell) return false

        if (card.name != "Chain Energy") {
            applyChainEnergyTax(duelist, "Activate [${card.name}]")
        }

        // Imperial Order check: negates all Spell cards
        val isImperialOrderActive = player.spellTrapZones.filterNotNull().any { !it.position.isFaceDown && it.card.name == "Imperial Order" } ||
            opponent.spellTrapZones.filterNotNull().any { !it.position.isFaceDown && it.card.name == "Imperial Order" }
        if (isImperialOrderActive) {
            log("⚡ Imperial Order negates the activation of [${card.name}]!")
            duelist.hand.removeAt(handIndex)
            duelist.graveyard.add(card)
            return false
        }

        // Counter-Trap check: Magic Jammer or Solemn Judgment
        val opp = if (isPlayer) opponent else player
        val counterSlot = (0 until 5).firstOrNull { slot ->
            val t = opp.spellTrapZones[slot]
            t != null && t.position.isFaceDown && (t.card.name == "Magic Jammer" || t.card.name == "Solemn Judgment")
        }

        if (counterSlot != null && !isJinzoActive()) {
            val counterTrap = opp.spellTrapZones[counterSlot]!!.card
            if (counterTrap.name == "Magic Jammer" && opp.hand.isNotEmpty()) {
                val discard = opp.hand.removeAt(0)
                opp.graveyard.add(discard.card)
                opp.spellTrapZones[counterSlot] = null
                opp.graveyard.add(counterTrap)
                duelist.hand.removeAt(handIndex)
                duelist.graveyard.add(card)
                activeChain.clear()
                activeChain.add(ChainLink(1, card, isPlayer = (duelist === player), isNegated = true, "Spell Activation"))
                activeChain.add(ChainLink(2, counterTrap, isPlayer = (opp === player), isNegated = false, "Counter-Trap Negation"))
                lastChainSummary = "⛓️ CHAIN: [Chain Link 2: Magic Jammer] negated [Chain Link 1: ${card.name}]!"
                log("⚡ CHAIN LINK 1: ${duelist.name} activates Spell: [${card.name}]")
                log("⚡ CHAIN LINK 2: ${opp.name} chains Counter-Trap: [Magic Jammer] (Discarded [${discard.card.name}])!")
                log("❌ CHAIN RESOLUTION: [Magic Jammer] negates and destroys [${card.name}]!")
                return true
            } else if (counterTrap.name == "Solemn Judgment" && opp.lifePoints > 1) {
                val cost = opp.lifePoints / 2
                opp.lifePoints -= cost
                opp.spellTrapZones[counterSlot] = null
                opp.graveyard.add(counterTrap)
                duelist.hand.removeAt(handIndex)
                duelist.graveyard.add(card)
                activeChain.clear()
                activeChain.add(ChainLink(1, card, isPlayer = (duelist === player), isNegated = true, "Spell Activation"))
                activeChain.add(ChainLink(2, counterTrap, isPlayer = (opp === player), isNegated = false, "Counter-Trap Negation"))
                lastChainSummary = "⛓️ CHAIN: [Chain Link 2: Solemn Judgment] negated [Chain Link 1: ${card.name}]!"
                log("⚡ CHAIN LINK 1: ${duelist.name} activates Spell: [${card.name}]")
                log("⚡ CHAIN LINK 2: ${opp.name} chains Counter-Trap: [Solemn Judgment] (Paid $cost LP: ${opp.lifePoints} LP remaining)!")
                log("❌ CHAIN RESOLUTION: [Solemn Judgment] negates and destroys [${card.name}]!")
                return true
            }
        }

        val emptySlot = duelist.firstEmptySpellSlot()
        val tempSlot = if (emptySlot != -1) emptySlot else 0

        duelist.hand.removeAt(handIndex)
        duelist.spellTrapZones[tempSlot] = DuelCard(card = card, position = CardPosition.FACE_UP_ATTACK)
        lastActivatedSpell = Pair(card.name, duelist.name)
        log("${duelist.name} activated Spell: [${card.name}]!")

        resolveSpellEffect(duelist, opp, card, isPlayer, tempSlot)

        val isContinuousOrEquip = isContinuousOrEquipSpell(card)
        if (isContinuousOrEquip) {
            duelist.spellTrapZones[tempSlot] = DuelCard(card = card, position = CardPosition.FACE_UP_SPELL_TRAP)
        } else {
            duelist.spellTrapZones[tempSlot] = null
            if (card.name !in listOf("Yami", "Forest", "Sogen", "Mountain", "Umi", "Wasteland")) {
                duelist.graveyard.add(card)
            }
        }
        return true
    }

    /**
     * Activates a set Spell card from the Spell/Trap zone (flipping it face-up).
     */
    fun activateSetSpell(isPlayer: Boolean, slotIndex: Int): Boolean {
        val duelist = if (isPlayer) player else opponent
        val other = if (isPlayer) opponent else player
        if (winner != null || slotIndex !in 0..4) return false
        val duelCard = duelist.spellTrapZones[slotIndex] ?: return false
        val card = duelCard.card
        if (!card.isSpell) return false

        if (currentPhase != DuelPhase.MAIN_PHASE_1 && currentPhase != DuelPhase.MAIN_PHASE_2) {
            log("Can only activate Spells during Main Phase")
            return false
        }

        // Imperial Order check
        if (isImperialOrderActive()) {
            log("⚡ Imperial Order negates the activation of [${card.name}]!")
            duelist.spellTrapZones[slotIndex] = null
            duelist.graveyard.add(card)
            return false
        }

        // Counter-Trap check
        val counterSlot = (0 until 5).firstOrNull { slot ->
            val t = other.spellTrapZones[slot]
            t != null && t.position.isFaceDown && (t.card.name == "Magic Jammer" || t.card.name == "Solemn Judgment")
        }
        if (counterSlot != null && !isJinzoActive()) {
            val counterTrap = other.spellTrapZones[counterSlot]!!.card
            if (counterTrap.name == "Magic Jammer" && other.hand.isNotEmpty()) {
                val discard = other.hand.removeAt(0)
                other.graveyard.add(discard.card)
                other.spellTrapZones[counterSlot] = null
                other.graveyard.add(counterTrap)
                duelist.spellTrapZones[slotIndex] = null
                duelist.graveyard.add(card)
                activeChain.clear()
                activeChain.add(ChainLink(1, card, isPlayer = (duelist === player), isNegated = true, "Set Spell Activation"))
                activeChain.add(ChainLink(2, counterTrap, isPlayer = (other === player), isNegated = false, "Counter-Trap Negation"))
                lastChainSummary = "⛓️ CHAIN: [Chain Link 2: Magic Jammer] negated [Chain Link 1: ${card.name}]!"
                log("⚡ CHAIN LINK 1: ${duelist.name} activates Set Spell: [${card.name}]")
                log("⚡ CHAIN LINK 2: ${other.name} chains Counter-Trap: [Magic Jammer]!")
                log("❌ CHAIN RESOLUTION: [Magic Jammer] negates and destroys [${card.name}]!")
                return true
            } else if (counterTrap.name == "Solemn Judgment" && other.lifePoints > 1) {
                val cost = other.lifePoints / 2
                other.lifePoints -= cost
                other.spellTrapZones[counterSlot] = null
                other.graveyard.add(counterTrap)
                duelist.spellTrapZones[slotIndex] = null
                duelist.graveyard.add(card)
                activeChain.clear()
                activeChain.add(ChainLink(1, card, isPlayer = (duelist === player), isNegated = true, "Set Spell Activation"))
                activeChain.add(ChainLink(2, counterTrap, isPlayer = (other === player), isNegated = false, "Counter-Trap Negation"))
                lastChainSummary = "⛓️ CHAIN: [Chain Link 2: Solemn Judgment] negated [Chain Link 1: ${card.name}]!"
                log("⚡ CHAIN LINK 1: ${duelist.name} activates Set Spell: [${card.name}]")
                log("⚡ CHAIN LINK 2: ${other.name} chains Counter-Trap: [Solemn Judgment] (Paid $cost LP)!")
                log("❌ CHAIN RESOLUTION: [Solemn Judgment] negates and destroys [${card.name}]!")
                return true
            }
        }

        duelCard.position = CardPosition.FACE_UP_SPELL_TRAP
        lastActivatedSpell = Pair(card.name, duelist.name)
        log("${duelist.name} flipped face-up & activated Set Spell: [${card.name}]!")

        resolveSpellEffect(duelist, other, card, isPlayer, slotIndex)

        val isContinuousOrEquip = isContinuousOrEquipSpell(card)
        if (!isContinuousOrEquip && card.name !in listOf("Yami", "Forest", "Sogen", "Mountain", "Umi", "Wasteland")) {
            duelist.spellTrapZones[slotIndex] = null
            duelist.graveyard.add(card)
        }
        return true
    }

    private fun resolveSpellEffect(
        duelist: Duelist,
        opp: Duelist,
        card: Card,
        isPlayer: Boolean,
        zoneSlot: Int
    ) {
        when (card.name) {
            "Pot of Greed" -> {
                drawCard(isPlayer, ignoreHandLimit = true)
                drawCard(isPlayer, ignoreHandLimit = true)
            }
            "Graceful Charity" -> {
                drawCard(isPlayer, ignoreHandLimit = true)
                drawCard(isPlayer, ignoreHandLimit = true)
                drawCard(isPlayer, ignoreHandLimit = true)
                repeat(2) {
                    if (duelist.hand.isNotEmpty()) {
                        val discard = duelist.hand.minByOrNull { it.card.atk } ?: duelist.hand.first()
                        duelist.hand.remove(discard)
                        duelist.graveyard.add(discard.card)
                        log("${duelist.name} discarded [${discard.card.name}] for Graceful Charity.")
                    }
                }
            }
            "Monster Reborn" -> {
                val targets = (player.graveyard + opponent.graveyard).filter { it.isMonster }
                val revived = targets.maxByOrNull { it.atk }
                if (revived != null) {
                    player.graveyard.remove(revived)
                    opponent.graveyard.remove(revived)
                    val s = duelist.firstEmptyMonsterSlot()
                    if (s != -1) {
                        duelist.monsterZones[s] = DuelCard(card = revived, position = CardPosition.FACE_UP_ATTACK)
                        log("✨ MONSTER REBORN: ${duelist.name} Special Summoned [${revived.name}] (ATK ${revived.atk}) from the Graveyard!")
                    }
                }
            }
            "Change of Heart", "Snatch Steal" -> {
                val oppSlot = (0 until 5)
                    .filter { opp.monsterZones[it] != null }
                    .maxByOrNull { opp.monsterZones[it]!!.card.atk }
                val mySlot = duelist.firstEmptyMonsterSlot()
                if (oppSlot != null && mySlot != -1) {
                    val stolen = opp.monsterZones[oppSlot]!!
                    opp.monsterZones[oppSlot] = null
                    duelist.monsterZones[mySlot] = stolen
                    if (card.name == "Change of Heart") {
                        tempControlledMonsters.add(TempControlRecord(stolen, originalOwner = opp, currentHolder = duelist))
                    }
                    log("💖 CHANGE OF HEART: ${duelist.name} took control of [${stolen.card.name}] (ATK ${stolen.card.atk}) until end of turn!")
                }
            }
            "Swords of Revealing Light" -> {
                opp.swordsTurnsRemaining = 3
                var revealedCount = 0
                for (i in 0 until 5) {
                    val m = opp.monsterZones[i]
                    if (m != null && m.position == CardPosition.FACE_DOWN_DEFENSE) {
                        m.position = CardPosition.FACE_UP_DEFENSE
                        revealedCount++
                    }
                }
                log("⚔️ SWORDS OF REVEALING LIGHT: ${duelist.name} illuminated the field! Revealed $revealedCount face-down monster(s). ${opp.name} cannot attack for 3 turns.")
            }
            "Raigeki" -> {
                var count = 0
                for (i in 0 until 5) {
                    val m = opp.monsterZones[i]
                    if (m != null) {
                        opp.graveyard.add(m.card)
                        opp.monsterZones[i] = null
                        count++
                    }
                }
                log("Raigeki destroyed $count monster(s) on ${opp.name}'s field!")
            }
            "Dark Hole" -> {
                var count = 0
                for (i in 0 until 5) {
                    player.monsterZones[i]?.let {
                        player.graveyard.add(it.card)
                        player.monsterZones[i] = null
                        count++
                    }
                    opponent.monsterZones[i]?.let {
                        opponent.graveyard.add(it.card)
                        opponent.monsterZones[i] = null
                        count++
                    }
                }
                log("Dark Hole destroyed $count monster(s) on the field!")
            }
            "Harpie's Feather Duster" -> {
                var count = 0
                for (i in 0 until 5) {
                    val st = opp.spellTrapZones[i]
                    if (st != null) {
                        opp.graveyard.add(st.card)
                        opp.spellTrapZones[i] = null
                        count++
                    }
                }
                checkToonWorldDestruction(opp)
                log("Harpie's Feather Duster destroyed $count Spell/Trap(s) on ${opp.name}'s field!")
            }
            "Heavy Storm" -> {
                var count = 0
                for (i in 0 until 5) {
                    player.spellTrapZones[i]?.let {
                        if (it.card.id != card.id) {
                            player.graveyard.add(it.card)
                            player.spellTrapZones[i] = null
                            count++
                        }
                    }
                    opponent.spellTrapZones[i]?.let {
                        opponent.graveyard.add(it.card)
                        opponent.spellTrapZones[i] = null
                        count++
                    }
                }
                checkToonWorldDestruction(player)
                checkToonWorldDestruction(opponent)
                log("Heavy Storm swept away $count Spell/Trap(s) on the field!")
            }
            "Dian Keto the Cure Maiden" -> {
                duelist.lifePoints += 1000
                log("${duelist.name} gained 1000 LP (Total: ${duelist.lifePoints})")
            }
            "Sparks" -> {
                opp.lifePoints = (opp.lifePoints - 200).coerceAtLeast(0)
                log("Sparks dealt 200 damage to ${opp.name}! (LP: ${opp.lifePoints})")
                checkWinConditions()
            }
            "Hinotama" -> {
                opp.lifePoints = (opp.lifePoints - 500).coerceAtLeast(0)
                log("Hinotama dealt 500 damage to ${opp.name}! (LP: ${opp.lifePoints})")
                checkWinConditions()
            }
            "Mooyan Curry" -> {
                duelist.lifePoints += 200
                log("${duelist.name} gained 200 LP from Mooyan Curry! (Total: ${duelist.lifePoints})")
            }
            "Yami", "Forest", "Sogen", "Mountain", "Umi", "Wasteland" -> {
                val prevField = activeFieldSpell
                if (prevField != null) {
                    duelist.graveyard.add(prevField)
                }
                activeFieldSpell = card
                log("🌌 FIELD SPELL: [${card.name}] is now active across the arena! Modifying monster stats.")
            }
            "Polymerization" -> {
                val available = (duelist.hand.map { it.card } + duelist.monsterZones.filterNotNull().map { it.card })
                val possible = FusionRitualRegistry.findPossibleFusions(duelist.extraDeck, available)
                if (possible.isNotEmpty()) {
                    val (recipe, materials) = possible.first()
                    for (mat in materials) {
                        val handIdx = duelist.hand.indexOfFirst { it.card.name.equals(mat.name, ignoreCase = true) }
                        if (handIdx != -1) {
                            duelist.hand.removeAt(handIdx)
                            duelist.graveyard.add(mat)
                        } else {
                            val fIdx = (0 until 5).firstOrNull { duelist.monsterZones[it]?.card?.name.equals(mat.name, ignoreCase = true) }
                            if (fIdx != null) {
                                duelist.monsterZones[fIdx] = null
                                duelist.graveyard.add(mat)
                            }
                        }
                    }
                    duelist.extraDeck.remove(recipe.fusionMonster)
                    val emptySlot = duelist.firstEmptyMonsterSlot()
                    if (emptySlot != -1) {
                        duelist.monsterZones[emptySlot] = DuelCard(card = recipe.fusionMonster, position = CardPosition.FACE_UP_ATTACK)
                        log("🌀 FUSION SUMMON! ${duelist.name} fused ${materials.joinToString(" + ") { it.name }} to summon [${recipe.fusionMonster.name}]! (ATK ${recipe.fusionMonster.atk} / DEF ${recipe.fusionMonster.def})")
                    }
                } else {
                    log("Polymerization resolved: No valid Fusion Materials found in Hand or on Field.")
                }
            }
            "Black Luster Ritual", "Black Magic Ritual", "Black Illusion Ritual" -> {
                val ritualResult = FusionRitualRegistry.checkRitualRequirements(
                    ritualSpellName = card.name,
                    handCards = duelist.hand.map { it.card },
                    fieldMonsters = duelist.monsterZones.filterNotNull().map { it.card }
                )
                if (ritualResult != null) {
                    val (ritualMonster, tributes) = ritualResult
                    val rHandIdx = duelist.hand.indexOfFirst { it.card.name.equals(ritualMonster.name, ignoreCase = true) }
                    if (rHandIdx != -1) {
                        duelist.hand.removeAt(rHandIdx)
                    }
                    for (trib in tributes) {
                        val hIdx = duelist.hand.indexOfFirst { it.card.name.equals(trib.name, ignoreCase = true) }
                        if (hIdx != -1) {
                            duelist.hand.removeAt(hIdx)
                            duelist.graveyard.add(trib)
                        } else {
                            val fIdx = (0 until 5).firstOrNull { duelist.monsterZones[it]?.card?.name.equals(trib.name, ignoreCase = true) }
                            if (fIdx != null) {
                                duelist.monsterZones[fIdx] = null
                                duelist.graveyard.add(trib)
                            }
                        }
                    }
                    val slot = duelist.firstEmptyMonsterSlot()
                    if (slot != -1) {
                        duelist.monsterZones[slot] = DuelCard(card = ritualMonster, position = CardPosition.FACE_UP_ATTACK)
                        log("✨ RITUAL SUMMON! ${duelist.name} sacrificed ${tributes.size} monster(s) to awaken [${ritualMonster.name}]! (ATK ${ritualMonster.atk} / DEF ${ritualMonster.def})")
                    }
                } else {
                    log("${card.name} resolved: Missing Ritual Monster in hand or insufficient tribute levels.")
                }
            }
            "Axe of Despair", "Malevolent Nuzzler", "Black Pendant", "Megamorph", "Horn of the Unicorn" -> {
                val targetSlot = (0 until 5).firstOrNull { duelist.monsterZones[it] != null }
                if (targetSlot != null) {
                    val targetMonster = duelist.monsterZones[targetSlot]!!
                    targetMonster.equippedCards.add(card)
                    if (zoneSlot in 0..4) {
                        duelist.spellTrapZones[zoneSlot]?.equippedTargetSlot = targetSlot
                    }
                    val buff = when (card.name) {
                        "Axe of Despair" -> "+1000 ATK"
                        "Malevolent Nuzzler" -> "+700 ATK"
                        "Black Pendant" -> "+500 ATK"
                        "Horn of the Unicorn" -> "+700 ATK/DEF"
                        "Megamorph" -> "ATK Doubled / Halved"
                        else -> "Equipped"
                    }
                    log("⚔️ EQUIP: [${card.name}] equipped to [${targetMonster.card.name}] ($buff)!")
                } else {
                    log("[${card.name}] has no valid target monster on the field.")
                }
            }
            "Toon World" -> {
                if (duelist.lifePoints > 1000) {
                    duelist.lifePoints -= 1000
                    log("📖 TOON WORLD: ${duelist.name} paid 1000 LP! The fantastical pop-up book opens upon the arena!")
                } else {
                    log("Toon World: Insufficient LP to pay 1000 LP cost.")
                }
            }
            "Premature Burial" -> {
                if (duelist.lifePoints > 800) {
                    val revivable = duelist.graveyard.filter { it.isMonster }.maxByOrNull { it.atk }
                    val emptySlot = duelist.firstEmptyMonsterSlot()
                    if (revivable != null && emptySlot != -1) {
                        duelist.lifePoints -= 800
                        duelist.graveyard.remove(revivable)
                        val dMonster = DuelCard(card = revivable, position = CardPosition.FACE_UP_ATTACK)
                        dMonster.equippedCards.add(card)
                        duelist.monsterZones[emptySlot] = dMonster
                        if (zoneSlot in 0..4) {
                            duelist.spellTrapZones[zoneSlot]?.equippedTargetSlot = emptySlot
                        }
                        log("✝️ PREMATURE BURIAL: ${duelist.name} paid 800 LP! Revived [${revivable.name}] (ATK ${revivable.atk})!")
                    } else {
                        log("Premature Burial: No monsters in Graveyard or Monster Zone full.")
                    }
                } else {
                    log("Premature Burial: Insufficient LP to pay 800 LP cost.")
                }
            }
            "Time Wizard" -> {
                val heads = (0..1).random() == 0
                if (heads) {
                    var count = 0
                    for (i in 0 until 5) {
                        opp.monsterZones[i]?.let {
                            cleanupMonsterEquips(it, opp, duelist)
                            opp.graveyard.add(it.card)
                            triggerGraveyardSearcher(it.card, opp)
                            opp.monsterZones[i] = null
                            count++
                        }
                    }
                    log("⏱️ TIME ROULETTE (HEADS): Time Wizard spun the magic roulette wheel! Destroyed $count opposing monster(s)!")
                } else {
                    var selfDmg = 0
                    for (i in 0 until 5) {
                        duelist.monsterZones[i]?.let {
                            cleanupMonsterEquips(it, duelist, opp)
                            duelist.graveyard.add(it.card)
                            triggerGraveyardSearcher(it.card, duelist)
                            selfDmg += it.card.atk / 2
                            duelist.monsterZones[i] = null
                        }
                    }
                    duelist.lifePoints = (duelist.lifePoints - selfDmg).coerceAtLeast(0)
                    log("⏱️ TIME ROULETTE (TAILS): Time Wizard backfired! Destroyed all your monsters and inflicted $selfDmg LP damage!")
                    checkWinConditions()
                }
            }
            "Cup of Ace" -> {
                val heads = (0..1).random() == 0
                if (heads) {
                    drawCard(isPlayer, ignoreHandLimit = true)
                    drawCard(isPlayer, ignoreHandLimit = true)
                    log("🏆 CUP OF ACE (HEADS): Lucky flip! ${duelist.name} drew 2 cards!")
                } else {
                    drawCard(!isPlayer, ignoreHandLimit = true)
                    drawCard(!isPlayer, ignoreHandLimit = true)
                    log("🏆 CUP OF ACE (TAILS): Unlucky flip! ${opp.name} drew 2 cards!")
                }
            }
            "Scapegoat" -> {
                var spawned = 0
                for (s in 0 until 5) {
                    if (duelist.monsterZones[s] == null) {
                        duelist.monsterZones[s] = DuelCard(
                            card = ToonAnimeArchetype.SHEEP_TOKEN,
                            position = CardPosition.FACE_UP_DEFENSE,
                            isToken = true,
                            canBeTributed = false,
                            canChangePosition = false
                        )
                        spawned++
                    }
                }
                log("🐑 SCAPEGOAT: ${duelist.name} summoned $spawned Sheep Token(s) (0 ATK / 0 DEF) in Defense Position! (Tokens cannot be tributed for a Tribute Summon)")
            }
            "Chain Energy" -> {
                log("⚡ CHAIN ENERGY: A web of electric chains envelops the arena! Both players must pay 500 LP for each card summoned, set, or activated from hand.")
            }
            "Toll" -> {
                log("💰 TOLL: A golden toll bridge rises! Either player must pay 500 LP to declare an attack with any monster.")
            }
            "Delinquent Duo" -> {
                if (duelist.lifePoints > 1000) {
                    duelist.lifePoints -= 1000
                    log("🃏 DELINQUENT DUO: ${duelist.name} paid 1000 LP! (${duelist.lifePoints} LP remaining)")
                }
                var discardedCount = 0
                if (opp.hand.isNotEmpty()) {
                    val rIdx = opp.hand.indices.random()
                    val c1 = opp.hand.removeAt(rIdx)
                    opp.graveyard.add(c1.card)
                    discardedCount++
                    log("Delinquent Duo: ${opp.name} randomly discarded [${c1.card.name}]!")
                }
                if (opp.hand.isNotEmpty()) {
                    val c2 = opp.hand.minByOrNull { it.card.atk } ?: opp.hand.first()
                    opp.hand.remove(c2)
                    opp.graveyard.add(c2.card)
                    discardedCount++
                    log("Delinquent Duo: ${opp.name} discarded chosen card [${c2.card.name}]!")
                }
                log("Delinquent Duo forced ${opp.name} to discard $discardedCount card(s).")
            }
            "The Forceful Sentry" -> {
                if (opp.hand.isNotEmpty()) {
                    val chosen = opp.hand.maxByOrNull { if (it.card.isMonster) it.card.atk else 2500 } ?: opp.hand.first()
                    opp.hand.remove(chosen)
                    opp.deck.add(chosen.card)
                    opp.deck.shuffle()
                    log("👁️ THE FORCEFUL SENTRY: ${duelist.name} inspected ${opp.name}'s hand and returned [${chosen.card.name}] to their Deck (Deck shuffled)!")
                } else {
                    log("The Forceful Sentry: ${opp.name}'s hand is empty.")
                }
            }
            "Confiscation" -> {
                if (duelist.lifePoints > 1000) {
                    duelist.lifePoints -= 1000
                    log("👁️ CONFISCATION: ${duelist.name} paid 1000 LP! (${duelist.lifePoints} LP remaining)")
                }
                if (opp.hand.isNotEmpty()) {
                    val chosen = opp.hand.maxByOrNull { if (it.card.isMonster) it.card.atk else 2500 } ?: opp.hand.first()
                    opp.hand.remove(chosen)
                    opp.graveyard.add(chosen.card)
                    log("Confiscation: ${duelist.name} selected and discarded [${chosen.card.name}] from ${opp.name}'s hand to the Graveyard!")
                } else {
                    log("Confiscation: ${opp.name}'s hand is empty.")
                }
            }
            "Card Destruction" -> {
                val pCount = player.hand.size
                player.graveyard.addAll(player.hand.map { it.card })
                player.hand.clear()

                val oCount = opponent.hand.size
                opponent.graveyard.addAll(opponent.hand.map { it.card })
                opponent.hand.clear()

                repeat(pCount) {
                    if (player.deck.isNotEmpty()) drawCard(isPlayer = true, ignoreHandLimit = true)
                }
                repeat(oCount) {
                    if (opponent.deck.isNotEmpty()) drawCard(isPlayer = false, ignoreHandLimit = true)
                }
                log("💥 CARD DESTRUCTION: Both duelists discarded their hands! ${player.name} drew $pCount card(s), ${opponent.name} drew $oCount card(s).")
                checkWinConditions()
            }
            "Nobleman of Crossout" -> {
                val oppTarget = (0 until 5).firstOrNull { opp.monsterZones[it]?.position == CardPosition.FACE_DOWN_DEFENSE }
                val (targetZone, targetOwner, targetSlot) = if (oppTarget != null) {
                    Triple(opp.monsterZones, opp, oppTarget)
                } else {
                    val pTarget = (0 until 5).firstOrNull { duelist.monsterZones[it]?.position == CardPosition.FACE_DOWN_DEFENSE }
                    Triple(duelist.monsterZones, duelist, pTarget)
                }

                if (targetSlot != null) {
                    val targetDuelCard = targetZone[targetSlot]!!
                    val victimCard = targetDuelCard.card
                    targetZone[targetSlot] = null
                    targetOwner.banishedZone.add(victimCard)
                    log("⚔️ NOBLEMAN OF CROSSOUT: Destroyed and banished face-down monster [${victimCard.name}]!")

                    if (isFlipEffectMonster(victimCard)) {
                        val pCopies = player.deck.filter { it.name.equals(victimCard.name, ignoreCase = true) }
                        player.deck.removeAll(pCopies)
                        player.banishedZone.addAll(pCopies)

                        val oCopies = opponent.deck.filter { it.name.equals(victimCard.name, ignoreCase = true) }
                        opponent.deck.removeAll(oCopies)
                        opponent.banishedZone.addAll(oCopies)

                        log("🌀 CROSSOUT EXILE: [${victimCard.name}] is a Flip Effect monster! Banished ${pCopies.size} copy/copies from ${player.name}'s Deck and ${oCopies.size} from ${opponent.name}'s Deck!")
                    }
                } else {
                    log("Nobleman of Crossout resolved: No face-down monsters on the field to banish.")
                }
            }
            "Mystical Space Typhoon" -> {
                val oppTarget = (0 until 5).firstOrNull { opp.spellTrapZones[it] != null }
                if (oppTarget != null) {
                    val destroyed = opp.spellTrapZones[oppTarget]!!
                    opp.spellTrapZones[oppTarget] = null
                    opp.graveyard.add(destroyed.card)
                    if (destroyed.card.name == "Toon World") {
                        checkToonWorldDestruction(opp)
                    }
                    log("🌪️ MYSTICAL SPACE TYPHOON: Destroyed [${destroyed.card.name}] in Zone ${oppTarget + 1} on ${opp.name}'s field!")
                } else {
                    val myTarget = (0 until 5).firstOrNull { it != zoneSlot && duelist.spellTrapZones[it] != null }
                    if (myTarget != null) {
                        val destroyed = duelist.spellTrapZones[myTarget]!!
                        duelist.spellTrapZones[myTarget] = null
                        duelist.graveyard.add(destroyed.card)
                        if (destroyed.card.name == "Toon World") {
                            checkToonWorldDestruction(duelist)
                        }
                        log("🌪️ MYSTICAL SPACE TYPHOON: Destroyed [${destroyed.card.name}] in Zone ${myTarget + 1}!")
                    } else {
                        log("Mystical Space Typhoon resolved: No other Spell/Trap cards on the field to destroy.")
                    }
                }
            }
        }
    }

    /**
     * Sets a Spell or Trap card into the Spell/Trap zone.
     */
    fun setSpellTrap(isPlayer: Boolean, handIndex: Int, slotIndex: Int): Boolean {
        val duelist = if (isPlayer) player else opponent
        if (isPlayer != isPlayerTurn || winner != null) return false
        if (handIndex !in duelist.hand.indices) return false

        val duelCard = duelist.hand[handIndex]
        val card = duelCard.card
        if (!card.isSpell && !card.isTrap) return false

        val targetSlot = if (slotIndex in 0..4 && duelist.spellTrapZones[slotIndex] == null) {
            slotIndex
        } else {
            duelist.firstEmptySpellSlot()
        }
        if (targetSlot == -1) return false

        duelist.hand.removeAt(handIndex)
        duelCard.position = CardPosition.SET_SPELL_TRAP
        duelist.spellTrapZones[targetSlot] = duelCard
        log("${duelist.name} Set 1 card in Spell/Trap Zone.")
        applyChainEnergyTax(duelist, "Set Spell/Trap")
        return true
    }

    /**
     * Activates a set Trap card from the Spell/Trap zone.
     */
    fun activateSetTrap(isPlayer: Boolean, slotIndex: Int): Boolean {
        val duelist = if (isPlayer) player else opponent
        val opp = if (isPlayer) opponent else player
        if (winner != null || slotIndex !in 0..4) return false

        val duelCard = duelist.spellTrapZones[slotIndex] ?: return false
        if (!duelCard.card.isTrap) return false

        val trapCard = duelCard.card
        if (isJinzoActive()) {
            log("⚡ Jinzo's continuous effect negates the activation of [${trapCard.name}]!")
            return false
        }

        duelist.spellTrapZones[slotIndex] = null
        duelist.graveyard.add(trapCard)
        log("${duelist.name} activated Trap: [${trapCard.name}]!")

        // Counter-Trap check against Trap activation: Seven Tools of the Bandit or Solemn Judgment
        val counterSlot = (0 until 5).firstOrNull { slot ->
            val t = opp.spellTrapZones[slot]
            t != null && t.position.isFaceDown && (t.card.name == "Seven Tools of the Bandit" || t.card.name == "Solemn Judgment")
        }

        if (counterSlot != null && !isJinzoActive()) {
            val counterTrap = opp.spellTrapZones[counterSlot]!!.card
            if (counterTrap.name == "Seven Tools of the Bandit" && opp.lifePoints > 1000) {
                opp.lifePoints -= 1000
                opp.spellTrapZones[counterSlot] = null
                opp.graveyard.add(counterTrap)
                activeChain.clear()
                activeChain.add(ChainLink(1, trapCard, isPlayer = (duelist === player), isNegated = true, "Trap Activation"))
                activeChain.add(ChainLink(2, counterTrap, isPlayer = (opp === player), isNegated = false, "Counter-Trap Negation"))
                lastChainSummary = "⛓️ CHAIN: [Chain Link 2: Seven Tools of the Bandit] negated [Chain Link 1: ${trapCard.name}]!"
                log("⚡ CHAIN LINK 1: ${duelist.name} activates Trap: [${trapCard.name}]")
                log("⚡ CHAIN LINK 2: ${opp.name} chains Counter-Trap: [Seven Tools of the Bandit] (Paid 1000 LP)!")
                log("❌ CHAIN RESOLUTION: [Seven Tools of the Bandit] negates and destroys [${trapCard.name}]!")
                return true
            } else if (counterTrap.name == "Solemn Judgment" && opp.lifePoints > 1) {
                val cost = opp.lifePoints / 2
                opp.lifePoints -= cost
                opp.spellTrapZones[counterSlot] = null
                opp.graveyard.add(counterTrap)
                activeChain.clear()
                activeChain.add(ChainLink(1, trapCard, isPlayer = (duelist === player), isNegated = true, "Trap Activation"))
                activeChain.add(ChainLink(2, counterTrap, isPlayer = (opp === player), isNegated = false, "Counter-Trap Negation"))
                lastChainSummary = "⛓️ CHAIN: [Chain Link 2: Solemn Judgment] negated [Chain Link 1: ${trapCard.name}]!"
                log("⚡ CHAIN LINK 1: ${duelist.name} activates Trap: [${trapCard.name}]")
                log("⚡ CHAIN LINK 2: ${opp.name} chains Counter-Trap: [Solemn Judgment] (Paid $cost LP: ${opp.lifePoints} LP remaining)!")
                log("❌ CHAIN RESOLUTION: [Solemn Judgment] negates and destroys [${trapCard.name}]!")
                return true
            }
        }

        when (trapCard.name) {
            "Waboku" -> {
                duelist.isWabokuActive = true
                log("🛡️ [Waboku] active: ${duelist.name} will take no battle damage this turn!")
            }
            "Just Desserts" -> {
                val count = opp.activeMonstersCount()
                val damage = count * 500
                opp.lifePoints = (opp.lifePoints - damage).coerceAtLeast(0)
                log("💥 [Just Desserts] deals $damage damage to ${opp.name} ($count monsters x 500)!")
                checkWinConditions()
            }
            "Reinforcements" -> {
                val targetMonSlot = (0 until 5).firstOrNull { duelist.monsterZones[it] != null }
                if (targetMonSlot != null) {
                    val m = duelist.monsterZones[targetMonSlot]!!
                    duelist.temporaryAtkBuffs[targetMonSlot] = (duelist.temporaryAtkBuffs[targetMonSlot] ?: 0) + 500
                    log("⚡ [Reinforcements] gives [${m.card.name}] +500 ATK this turn!")
                }
            }
            "Spellbinding Circle" -> {
                val oppTarget = (0 until 5).firstOrNull { opp.monsterZones[it] != null }
                if (oppTarget != null) {
                    val m = opp.monsterZones[oppTarget]!!
                    m.hasAttackedThisTurn = true
                    m.attacksDeclaredThisTurn = m.maxAttacksPerTurn
                    m.canChangePosition = false
                    log("🕸️ [Spellbinding Circle] binds [${m.card.name}]! Cannot attack or change position.")
                }
            }
            "Call of the Haunted" -> {
                val revivable = duelist.graveyard.filter { it.isMonster }.maxByOrNull { it.atk }
                val emptySlot = duelist.firstEmptyMonsterSlot()
                if (revivable != null && emptySlot != -1) {
                    duelist.graveyard.remove(revivable)
                    val dMonster = DuelCard(card = revivable, position = CardPosition.FACE_UP_ATTACK)
                    duelist.monsterZones[emptySlot] = dMonster
                    duelist.spellTrapZones[slotIndex] = DuelCard(card = trapCard, position = CardPosition.FACE_UP_SPELL_TRAP, equippedTargetSlot = emptySlot)
                    duelist.graveyard.remove(trapCard)
                    log("⚰️ CALL OF THE HAUNTED: ${duelist.name} summoned [${revivable.name}] from the Graveyard in Attack Position!")
                } else {
                    log("Call of the Haunted: No monsters in Graveyard or Monster Zone is full.")
                }
            }
            "Ring of Destruction" -> {
                val allFaceUpMonsters = mutableListOf<Pair<DuelCard, Duelist>>()
                for (i in 0 until 5) {
                    player.monsterZones[i]?.let {
                        if (!it.position.isFaceDown) allFaceUpMonsters.add(Pair(it, player))
                    }
                    opponent.monsterZones[i]?.let {
                        if (!it.position.isFaceDown) allFaceUpMonsters.add(Pair(it, opponent))
                    }
                }
                val target = allFaceUpMonsters.maxByOrNull { it.first.card.atk }
                if (target != null) {
                    val (dCard, targetOwner) = target
                    val burnDamage = dCard.card.atk
                    val oppTargetOwner = if (targetOwner === player) opponent else player
                    cleanupMonsterEquips(dCard, targetOwner, oppTargetOwner)
                    val sIdx = targetOwner.monsterZones.indexOfFirst { it === dCard }
                    if (sIdx != -1) {
                        targetOwner.monsterZones[sIdx] = null
                    }
                    if (!dCard.isToken) {
                        targetOwner.graveyard.add(dCard.card)
                        triggerGraveyardSearcher(dCard.card, targetOwner)
                    }

                    player.lifePoints = (player.lifePoints - burnDamage).coerceAtLeast(0)
                    opponent.lifePoints = (opponent.lifePoints - burnDamage).coerceAtLeast(0)

                    log("💥 RING OF DESTRUCTION: Detonated [${dCard.card.name}]! Both players take $burnDamage effect damage! (Player: ${player.lifePoints} LP, Opponent: ${opponent.lifePoints} LP)")
                    checkWinConditions()
                } else {
                    log("Ring of Destruction: No face-up monsters on the field to destroy.")
                }
            }
            "Torrential Tribute" -> {
                var count = 0
                for (i in 0 until 5) {
                    player.monsterZones[i]?.let {
                        cleanupMonsterEquips(it, player, opponent)
                        if (!it.isToken) {
                            player.graveyard.add(it.card)
                            triggerGraveyardSearcher(it.card, player)
                        }
                        player.monsterZones[i] = null
                        count++
                    }
                    opponent.monsterZones[i]?.let {
                        cleanupMonsterEquips(it, opponent, player)
                        if (!it.isToken) {
                            opponent.graveyard.add(it.card)
                            triggerGraveyardSearcher(it.card, opponent)
                        }
                        opponent.monsterZones[i] = null
                        count++
                    }
                }
                log("🌊 TORRENTIAL TRIBUTE: A tidal wave swept away and destroyed all $count monster(s) on the field!")
                checkWinConditions()
            }
        }
        return true
    }

    /**
     * Checks if a monster at the specified slot is eligible to declare an attack this turn.
     */
    fun canMonsterAttack(isPlayer: Boolean, slotIndex: Int): Boolean {
        if (winner != null || currentPhase != DuelPhase.BATTLE_PHASE) return false
        val duelist = if (isPlayer) player else opponent
        if (duelist.swordsTurnsRemaining > 0) return false
        if (isTollActive() && duelist.lifePoints < 500) return false
        val monster = duelist.monsterZones.getOrNull(slotIndex) ?: return false
        return monster.canDeclareAttack
    }

    private fun cleanupMonsterEquips(monster: DuelCard, owner: Duelist, opponentDuelist: Duelist) {
        if (monster.equippedCards.isEmpty()) return
        for (equip in monster.equippedCards) {
            owner.graveyard.add(equip)
            if (equip.name == "Black Pendant") {
                opponentDuelist.lifePoints = (opponentDuelist.lifePoints - 500).coerceAtLeast(0)
                log("💥 [Black Pendant] sent to Graveyard! Inflicted 500 damage to ${opponentDuelist.name}!")
            }
        }
        for (i in 0 until 5) {
            val st = owner.spellTrapZones[i]
            if (st != null && st.equippedTargetSlot != null && monster.equippedCards.any { it.id == st.card.id }) {
                owner.spellTrapZones[i] = null
            }
        }
        monster.equippedCards.clear()
    }

    /**
     * Declares an attack from an attacking monster zone towards a target monster zone or direct attack.
     * Enforces GBA TCG battle rules or Dark Duel Stories elemental alignment dominance rules.
     */
    fun declareAttack(attackerSlot: Int, targetSlot: Int?): Boolean {
        if (winner != null) return false
        if (currentPhase != DuelPhase.BATTLE_PHASE) {
            log("Attacks can only be declared during Battle Phase")
            return false
        }

        val attackerDuelist = if (isPlayerTurn) player else opponent
        val defenderDuelist = if (isPlayerTurn) opponent else player

        if (attackerSlot !in 0..4) return false
        val attackerMonster = attackerDuelist.monsterZones[attackerSlot] ?: return false
        if (attackerMonster.position != CardPosition.FACE_UP_ATTACK) {
            log("Monsters in Defense position cannot declare attacks")
            return false
        }
        if (!attackerMonster.canDeclareAttack) {
            log("${attackerMonster.card.name} has already declared its attack(s) this turn.")
            return false
        }
        if (attackerDuelist.swordsTurnsRemaining > 0) {
            log("${attackerDuelist.name} cannot attack due to Swords of Revealing Light!")
            return false
        }
        if (isTollActive()) {
            if (attackerDuelist.lifePoints < 500) {
                log("${attackerDuelist.name} cannot pay 500 LP for Toll to declare an attack!")
                return false
            }
            attackerDuelist.lifePoints -= 500
            log("💰 TOLL: ${attackerDuelist.name} paid 500 LP to declare an attack! (${attackerDuelist.lifePoints} LP remaining)")
            checkWinConditions()
            if (winner != null) return true
        }

        // Waboku protection check
        if (defenderDuelist.isWabokuActive) {
            attackerMonster.markAttacked()
            val targetMonster = targetSlot?.let { defenderDuelist.monsterZones.getOrNull(it) }
            clashState = ClashState(
                attackerCard = attackerMonster.card,
                defenderCard = targetMonster?.card,
                isDirectAttack = (targetSlot == null),
                damageDealt = 0,
                damageToPlayer = false,
                resultSummary = "WABOKU: ${defenderDuelist.name}'s Waboku prevents all battle damage!"
            )
            log("🛡️ WABOKU SHIELD: ${defenderDuelist.name}'s Waboku prevents battle damage and monster destruction!")
            return true
        }

        // Trap Trigger on Attack Declaration (Negated by Jinzo!)
        if (!isJinzoActive()) {
            val trapCounterSlot = (0 until 5).firstOrNull { slot ->
                val t = attackerDuelist.spellTrapZones[slot]
                t != null && t.position.isFaceDown && (t.card.name == "Seven Tools of the Bandit" || t.card.name == "Solemn Judgment")
            }

            // Magic Cylinder
            val magicCylinderSlot = (0 until 5).firstOrNull { slot ->
                val trap = defenderDuelist.spellTrapZones[slot]
                trap != null && trap.position.isFaceDown && trap.card.name == "Magic Cylinder"
            }
            if (magicCylinderSlot != null) {
                val trapCard = defenderDuelist.spellTrapZones[magicCylinderSlot]!!.card
                defenderDuelist.spellTrapZones[magicCylinderSlot] = null
                defenderDuelist.graveyard.add(trapCard)

                if (trapCounterSlot != null) {
                    val counter = attackerDuelist.spellTrapZones[trapCounterSlot]!!.card
                    if (counter.name == "Seven Tools of the Bandit" && attackerDuelist.lifePoints > 1000) {
                        attackerDuelist.lifePoints -= 1000
                        attackerDuelist.spellTrapZones[trapCounterSlot] = null
                        attackerDuelist.graveyard.add(counter)
                        activeChain.clear()
                        activeChain.add(ChainLink(1, trapCard, isPlayer = (defenderDuelist === player), isNegated = true, "Magic Cylinder Trigger"))
                        activeChain.add(ChainLink(2, counter, isPlayer = (attackerDuelist === player), isNegated = false, "Counter-Trap Negation"))
                        lastChainSummary = "⛓️ CHAIN: [Chain Link 2: Seven Tools of the Bandit] negated [Chain Link 1: Magic Cylinder]!"
                        log("⚡ CHAIN LINK 1: ${defenderDuelist.name} triggered Trap: [Magic Cylinder]!")
                        log("⚡ CHAIN LINK 2: ${attackerDuelist.name} chains Counter-Trap: [Seven Tools of the Bandit] (Paid 1000 LP)!")
                        log("❌ CHAIN RESOLUTION: [Seven Tools of the Bandit] negates and destroys [Magic Cylinder]!")
                    } else if (counter.name == "Solemn Judgment" && attackerDuelist.lifePoints > 1) {
                        val cost = attackerDuelist.lifePoints / 2
                        attackerDuelist.lifePoints -= cost
                        attackerDuelist.spellTrapZones[trapCounterSlot] = null
                        attackerDuelist.graveyard.add(counter)
                        activeChain.clear()
                        activeChain.add(ChainLink(1, trapCard, isPlayer = (defenderDuelist === player), isNegated = true, "Magic Cylinder Trigger"))
                        activeChain.add(ChainLink(2, counter, isPlayer = (attackerDuelist === player), isNegated = false, "Counter-Trap Negation"))
                        lastChainSummary = "⛓️ CHAIN: [Chain Link 2: Solemn Judgment] negated [Chain Link 1: Magic Cylinder]!"
                        log("⚡ CHAIN LINK 1: ${defenderDuelist.name} triggered Trap: [Magic Cylinder]!")
                        log("⚡ CHAIN LINK 2: ${attackerDuelist.name} chains Counter-Trap: [Solemn Judgment] (Paid $cost LP)!")
                        log("❌ CHAIN RESOLUTION: [Solemn Judgment] negates and destroys [Magic Cylinder]!")
                    } else {
                        val rawDamage = attackerMonster.card.atk
                        val damage = checkKuribohHandTrap(attackerDuelist, rawDamage)
                        attackerDuelist.lifePoints = (attackerDuelist.lifePoints - damage).coerceAtLeast(0)
                        log("${defenderDuelist.name} triggered Trap: [Magic Cylinder]! Attack negated and $damage damage reflected to ${attackerDuelist.name}!")
                        clashState = ClashState(
                            attackerCard = attackerMonster.card,
                            defenderCard = trapCard,
                            isDirectAttack = false,
                            damageDealt = damage,
                            damageToPlayer = isPlayerTurn,
                            resultSummary = "TRAP: Magic Cylinder reflected $damage damage!"
                        )
                        attackerMonster.markAttacked()
                        checkWinConditions()
                        return true
                    }
                } else {
                    val rawDamage = attackerMonster.card.atk
                    val damage = checkKuribohHandTrap(attackerDuelist, rawDamage)
                    attackerDuelist.lifePoints = (attackerDuelist.lifePoints - damage).coerceAtLeast(0)
                    log("${defenderDuelist.name} triggered Trap: [Magic Cylinder]! Attack negated and $damage damage reflected to ${attackerDuelist.name}!")
                    clashState = ClashState(
                        attackerCard = attackerMonster.card,
                        defenderCard = trapCard,
                        isDirectAttack = false,
                        damageDealt = damage,
                        damageToPlayer = isPlayerTurn,
                        resultSummary = "TRAP: Magic Cylinder reflected $damage damage!"
                    )
                    attackerMonster.markAttacked()
                    checkWinConditions()
                    return true
                }
            }

            // Mirror Force
            val mirrorForceSlot = (0 until 5).firstOrNull { slot ->
                val trap = defenderDuelist.spellTrapZones[slot]
                trap != null && trap.position.isFaceDown && trap.card.name == "Mirror Force"
            }
            if (mirrorForceSlot != null) {
                val trapCard = defenderDuelist.spellTrapZones[mirrorForceSlot]!!.card
                defenderDuelist.spellTrapZones[mirrorForceSlot] = null
                defenderDuelist.graveyard.add(trapCard)

                if (trapCounterSlot != null) {
                    val counter = attackerDuelist.spellTrapZones[trapCounterSlot]!!.card
                    if (counter.name == "Seven Tools of the Bandit" && attackerDuelist.lifePoints > 1000) {
                        attackerDuelist.lifePoints -= 1000
                        attackerDuelist.spellTrapZones[trapCounterSlot] = null
                        attackerDuelist.graveyard.add(counter)
                        activeChain.clear()
                        activeChain.add(ChainLink(1, trapCard, isPlayer = (defenderDuelist === player), isNegated = true, "Mirror Force Trigger"))
                        activeChain.add(ChainLink(2, counter, isPlayer = (attackerDuelist === player), isNegated = false, "Counter-Trap Negation"))
                        lastChainSummary = "⛓️ CHAIN: [Chain Link 2: Seven Tools of the Bandit] negated [Chain Link 1: Mirror Force]!"
                        log("⚡ CHAIN LINK 1: ${defenderDuelist.name} triggered Trap: [Mirror Force]!")
                        log("⚡ CHAIN LINK 2: ${attackerDuelist.name} chains Counter-Trap: [Seven Tools of the Bandit] (Paid 1000 LP)!")
                        log("❌ CHAIN RESOLUTION: [Seven Tools of the Bandit] negates and destroys [Mirror Force]!")
                    } else if (counter.name == "Solemn Judgment" && attackerDuelist.lifePoints > 1) {
                        val cost = attackerDuelist.lifePoints / 2
                        attackerDuelist.lifePoints -= cost
                        attackerDuelist.spellTrapZones[trapCounterSlot] = null
                        attackerDuelist.graveyard.add(counter)
                        activeChain.clear()
                        activeChain.add(ChainLink(1, trapCard, isPlayer = (defenderDuelist === player), isNegated = true, "Mirror Force Trigger"))
                        activeChain.add(ChainLink(2, counter, isPlayer = (attackerDuelist === player), isNegated = false, "Counter-Trap Negation"))
                        lastChainSummary = "⛓️ CHAIN: [Chain Link 2: Solemn Judgment] negated [Chain Link 1: Mirror Force]!"
                        log("⚡ CHAIN LINK 1: ${defenderDuelist.name} triggered Trap: [Mirror Force]!")
                        log("⚡ CHAIN LINK 2: ${attackerDuelist.name} chains Counter-Trap: [Solemn Judgment] (Paid $cost LP)!")
                        log("❌ CHAIN RESOLUTION: [Solemn Judgment] negates and destroys [Mirror Force]!")
                    } else {
                        log("${defenderDuelist.name} triggered Trap: [Mirror Force]!")
                        var destroyedCount = 0
                        for (i in 0 until 5) {
                            val m = attackerDuelist.monsterZones[i]
                            if (m != null && m.position == CardPosition.FACE_UP_ATTACK) {
                                attackerDuelist.graveyard.add(m.card)
                                attackerDuelist.monsterZones[i] = null
                                destroyedCount++
                            }
                        }
                        clashState = ClashState(
                            attackerCard = attackerMonster.card,
                            defenderCard = trapCard,
                            isDirectAttack = false,
                            damageDealt = 0,
                            damageToPlayer = false,
                            resultSummary = "TRAP: Mirror Force destroyed $destroyedCount monster(s)!"
                        )
                        log("Mirror Force destroyed $destroyedCount attacking monster(s) on ${attackerDuelist.name}'s field!")
                        checkWinConditions()
                        return true
                    }
                } else {
                    log("${defenderDuelist.name} triggered Trap: [Mirror Force]!")
                    var destroyedCount = 0
                    for (i in 0 until 5) {
                        val m = attackerDuelist.monsterZones[i]
                        if (m != null && m.position == CardPosition.FACE_UP_ATTACK) {
                            attackerDuelist.graveyard.add(m.card)
                            attackerDuelist.monsterZones[i] = null
                            destroyedCount++
                        }
                    }
                    clashState = ClashState(
                        attackerCard = attackerMonster.card,
                        defenderCard = trapCard,
                        isDirectAttack = false,
                        damageDealt = 0,
                        damageToPlayer = false,
                        resultSummary = "TRAP: Mirror Force destroyed $destroyedCount monster(s)!"
                    )
                    log("Mirror Force destroyed $destroyedCount attacking monster(s) on ${attackerDuelist.name}'s field!")
                    checkWinConditions()
                    return true
                }
            }
        } else {
            log("⚡ Jinzo's presence negates all Trap cards on the field!")
        }

        // Direct attack check
        if (targetSlot == null) {
            val isToonAttacker = ToonAnimeArchetype.isToonMonster(attackerMonster.card)
            val oppHasToon = defenderDuelist.monsterZones.filterNotNull().any { !it.position.isFaceDown && ToonAnimeArchetype.isToonMonster(it.card) }
            val canBypassMonsters = isToonAttacker && !oppHasToon

            if (defenderDuelist.activeMonstersCount() > 0 && !canBypassMonsters) {
                log("Cannot attack directly while opponent controls monsters!")
                return false
            }

            if (isToonAttacker && attackerMonster.turnsOnField == 0) {
                log("Toon monsters cannot attack the turn they are summoned!")
                return false
            }

            attackerMonster.markAttacked()
            val rawDamage = getEffectiveAtk(attackerMonster.card, attackerMonster)
            val damage = checkKuribohHandTrap(defenderDuelist, rawDamage)
            defenderDuelist.lifePoints = (defenderDuelist.lifePoints - damage).coerceAtLeast(0)

            if (attackerMonster.card.name == "White Magical Hat" && damage > 0 && defenderDuelist.hand.isNotEmpty()) {
                val discarded = defenderDuelist.hand.removeAt(0)
                defenderDuelist.graveyard.add(discarded.card)
                log("⚡ EFFECT: [White Magical Hat] discarded [${discarded.card.name}] from ${defenderDuelist.name}'s hand!")
            }

            clashState = ClashState(
                attackerCard = attackerMonster.card,
                defenderCard = null,
                isDirectAttack = true,
                damageDealt = damage,
                damageToPlayer = !isPlayerTurn,
                resultSummary = if (damage > 0) "${attackerMonster.card.name} attacks directly! (-$damage LP)" else "${attackerMonster.card.name} direct attack negated by Kuriboh!"
            )
            log("DIRECT ATTACK: ${attackerMonster.card.name} attacked directly! $damage damage to ${defenderDuelist.name}.")
            checkWinConditions()
            return true
        }

        // Monster vs Monster battle
        if (targetSlot !in 0..4) return false
        val targetMonster = defenderDuelist.monsterZones[targetSlot] ?: return false

        attackerMonster.markAttacked()
        val aAtk = getEffectiveAtk(attackerMonster.card, attackerMonster)
        val dCard = targetMonster.card

        // Yu-Gi-Oh! Official Rule: When a face-down monster is attacked, it is flipped face-up in the Damage Step before damage calculation
        val wasFaceDown = targetMonster.position == CardPosition.FACE_DOWN_DEFENSE
        if (wasFaceDown) {
            targetMonster.position = CardPosition.FACE_UP_DEFENSE
            log("${defenderDuelist.name}'s face-down monster was flipped face-up: [${dCard.name}] (${getEffectiveDef(dCard, targetMonster)} DEF)!")
        }

        // Check Dark Duel Stories Elemental Alignment Dominance (GBC DDS Only)
        if (!isGbaMode && enableDdsAlignmentRule) {
            val aAttr = attackerMonster.card.attribute
            val dAttr = dCard.attribute
            if (aAttr.hasAlignmentAdvantageOver(dAttr)) {
                cleanupMonsterEquips(targetMonster, defenderDuelist, attackerDuelist)
                defenderDuelist.graveyard.add(dCard)
                triggerGraveyardSearcher(dCard, defenderDuelist)
                defenderDuelist.monsterZones[targetSlot] = null
                clashState = ClashState(
                    attackerCard = attackerMonster.card,
                    defenderCard = dCard,
                    isDirectAttack = false,
                    damageDealt = 0,
                    damageToPlayer = false,
                    resultSummary = "ALIGNMENT DOMINANCE! ${aAttr.displayName} overpowers ${dAttr.displayName}!"
                )
                log("ALIGNMENT ADVANTAGE: ${attackerMonster.card.name} ($aAttr) instantly destroyed ${dCard.name} ($dAttr)!")
                checkWinConditions()
                return true
            } else if (dAttr.hasAlignmentAdvantageOver(aAttr)) {
                cleanupMonsterEquips(attackerMonster, attackerDuelist, defenderDuelist)
                attackerDuelist.graveyard.add(attackerMonster.card)
                triggerGraveyardSearcher(attackerMonster.card, attackerDuelist)
                attackerDuelist.monsterZones[attackerSlot] = null
                clashState = ClashState(
                    attackerCard = attackerMonster.card,
                    defenderCard = dCard,
                    isDirectAttack = false,
                    damageDealt = 0,
                    damageToPlayer = false,
                    resultSummary = "ALIGNMENT DEFEAT! ${dAttr.displayName} repels ${aAttr.displayName}!"
                )
                log("ALIGNMENT DEFEAT: ${attackerMonster.card.name} was destroyed by ${dCard.name} ($dAttr)!")
                checkWinConditions()
                return true
            }
        }

        // Standard Battle Calculation (Official GBA TCG Expert Rules)
        if (targetMonster.position == CardPosition.FACE_UP_ATTACK) {
            val dAtk = getEffectiveAtk(dCard, targetMonster)
            if (aAtk > dAtk) {
                val rawDiff = aAtk - dAtk
                val diff = checkKuribohHandTrap(defenderDuelist, rawDiff)
                defenderDuelist.lifePoints = (defenderDuelist.lifePoints - diff).coerceAtLeast(0)
                cleanupMonsterEquips(targetMonster, defenderDuelist, attackerDuelist)
                defenderDuelist.graveyard.add(dCard)
                triggerGraveyardSearcher(dCard, defenderDuelist)
                defenderDuelist.monsterZones[targetSlot] = null

                if (attackerMonster.card.name == "White Magical Hat" && diff > 0 && defenderDuelist.hand.isNotEmpty()) {
                    val discarded = defenderDuelist.hand.removeAt(0)
                    defenderDuelist.graveyard.add(discarded.card)
                    log("⚡ EFFECT: [White Magical Hat] discarded [${discarded.card.name}] from ${defenderDuelist.name}'s hand!")
                }

                clashState = ClashState(
                    attackerCard = attackerMonster.card,
                    defenderCard = dCard,
                    isDirectAttack = false,
                    damageDealt = diff,
                    damageToPlayer = !isPlayerTurn,
                    resultSummary = "${attackerMonster.card.name} destroys ${dCard.name}! (-$diff LP to ${defenderDuelist.name})"
                )
                log("${attackerMonster.card.name} ($aAtk ATK) destroyed ${dCard.name} ($dAtk ATK)! $diff damage to ${defenderDuelist.name}.")
            } else if (aAtk < dAtk) {
                val rawDiff = dAtk - aAtk
                val diff = checkKuribohHandTrap(attackerDuelist, rawDiff)
                attackerDuelist.lifePoints = (attackerDuelist.lifePoints - diff).coerceAtLeast(0)
                cleanupMonsterEquips(attackerMonster, attackerDuelist, defenderDuelist)
                attackerDuelist.graveyard.add(attackerMonster.card)
                triggerGraveyardSearcher(attackerMonster.card, attackerDuelist)
                attackerDuelist.monsterZones[attackerSlot] = null

                clashState = ClashState(
                    attackerCard = attackerMonster.card,
                    defenderCard = dCard,
                    isDirectAttack = false,
                    damageDealt = diff,
                    damageToPlayer = isPlayerTurn,
                    resultSummary = "${dCard.name} destroys ${attackerMonster.card.name}! (-$diff LP to ${attackerDuelist.name})"
                )
                log("${attackerMonster.card.name} ($aAtk ATK) was destroyed by ${dCard.name} ($dAtk ATK)! $diff damage to ${attackerDuelist.name}.")
            } else {
                cleanupMonsterEquips(attackerMonster, attackerDuelist, defenderDuelist)
                attackerDuelist.graveyard.add(attackerMonster.card)
                triggerGraveyardSearcher(attackerMonster.card, attackerDuelist)
                attackerDuelist.monsterZones[attackerSlot] = null
                cleanupMonsterEquips(targetMonster, defenderDuelist, attackerDuelist)
                defenderDuelist.graveyard.add(dCard)
                triggerGraveyardSearcher(dCard, defenderDuelist)
                defenderDuelist.monsterZones[targetSlot] = null

                clashState = ClashState(
                    attackerCard = attackerMonster.card,
                    defenderCard = dCard,
                    isDirectAttack = false,
                    damageDealt = 0,
                    damageToPlayer = false,
                    resultSummary = "MUTUAL DESTRUCTION! Both ${attackerMonster.card.name} and ${dCard.name} destroyed!"
                )
                log("MUTUAL DESTRUCTION: Both monsters were destroyed in battle.")
            }
        } else {
            // Defender in Defense Position (flipped to FACE_UP_DEFENSE if wasFaceDown)
            val dDef = getEffectiveDef(dCard, targetMonster)
            val flipPrefix = if (wasFaceDown) "Flipped face-up! " else ""
            if (aAtk > dDef) {
                cleanupMonsterEquips(targetMonster, defenderDuelist, attackerDuelist)
                if (!targetMonster.isToken) {
                    defenderDuelist.graveyard.add(dCard)
                    triggerGraveyardSearcher(dCard, defenderDuelist)
                }
                defenderDuelist.monsterZones[targetSlot] = null

                clashState = ClashState(
                    attackerCard = attackerMonster.card,
                    defenderCard = dCard,
                    isDirectAttack = false,
                    damageDealt = 0,
                    damageToPlayer = false,
                    resultSummary = "${flipPrefix}${attackerMonster.card.name} destroys defense monster ${dCard.name}!"
                )
                log("${attackerMonster.card.name} ($aAtk ATK) destroyed ${dCard.name} ($dDef DEF). No LP damage.")
            } else if (aAtk < dDef) {
                val rawDiff = dDef - aAtk
                val diff = checkKuribohHandTrap(attackerDuelist, rawDiff)
                attackerDuelist.lifePoints = (attackerDuelist.lifePoints - diff).coerceAtLeast(0)

                // Defender survives in FACE_UP_DEFENSE
                clashState = ClashState(
                    attackerCard = attackerMonster.card,
                    defenderCard = dCard,
                    isDirectAttack = false,
                    damageDealt = diff,
                    damageToPlayer = isPlayerTurn,
                    resultSummary = "${flipPrefix}${dCard.name} holds defense! (-$diff LP to ${attackerDuelist.name})"
                )
                log("${dCard.name}'s defense ($dDef) repelled ${attackerMonster.card.name}! $diff damage to ${attackerDuelist.name}.")
            } else {
                // Defender survives in FACE_UP_DEFENSE
                clashState = ClashState(
                    attackerCard = attackerMonster.card,
                    defenderCard = dCard,
                    isDirectAttack = false,
                    damageDealt = 0,
                    damageToPlayer = false,
                    resultSummary = "${flipPrefix}Defense held! No cards destroyed."
                )
                log("Attack equaled defense ($dDef). No damage dealt.")
            }
        }

        // Trigger Flip Effects if the attacked monster was flipped face-up
        if (wasFaceDown) {
            triggerFlipEffect(defenderDuelist, attackerDuelist, dCard, targetSlot, attackerSlot)
        }

        checkWinConditions()
        return true
    }

    /**
     * Progresses through the turn phases.
     */
    fun nextPhase() {
        if (winner != null) return
        when (currentPhase) {
            DuelPhase.DRAW_PHASE -> {
                currentPhase = DuelPhase.STANDBY_PHASE
                handleStandbyPhaseUpkeep()
            }
            DuelPhase.STANDBY_PHASE -> currentPhase = DuelPhase.MAIN_PHASE_1
            DuelPhase.MAIN_PHASE_1 -> currentPhase = DuelPhase.BATTLE_PHASE
            DuelPhase.BATTLE_PHASE -> currentPhase = DuelPhase.MAIN_PHASE_2
            DuelPhase.MAIN_PHASE_2 -> currentPhase = DuelPhase.END_PHASE
            DuelPhase.END_PHASE -> endTurn()
        }
        log("Phase: ${currentPhase.displayName}")
    }

    /**
     * Ends the current turn and passes initiative to the opponent or player.
     */
    fun passTurn(autoRunAi: Boolean = true) {
        val currentDuelist = if (isPlayerTurn) player else opponent
        decrementSwordsOfRevealingLight(currentDuelist)
        currentDuelist.resetTurnActions()

        // Return temporary controlled monsters (Change of Heart)
        val returning = tempControlledMonsters.toList()
        for (record in returning) {
            val (m, orig, holder) = record
            val currentSlot = (0 until 5).firstOrNull { holder.monsterZones[it] === m }
            if (currentSlot != null) {
                holder.monsterZones[currentSlot] = null
                val returnSlot = orig.firstEmptyMonsterSlot()
                if (returnSlot != -1) {
                    orig.monsterZones[returnSlot] = m
                    log("Change of Heart expired: [${m.card.name}] returned to ${orig.name}'s control.")
                } else {
                    orig.graveyard.add(m.card)
                    log("Change of Heart expired: [${m.card.name}] sent to ${orig.name}'s Graveyard (no open zones).")
                }
            }
        }
        tempControlledMonsters.clear()
        lastActivatedSpell = null

        isPlayerTurn = !isPlayerTurn
        hasNormalSummonedThisTurn = false
        clashState = null
        lastTriggeredTrap = null

        val nextDuelist = if (isPlayerTurn) player else opponent
        nextDuelist.resetTurnActions()
        if (isPlayerTurn) {
            turnNumber++
        }

        log("--- Turn $turnNumber: ${nextDuelist.name}'s Turn ---")
        currentPhase = DuelPhase.DRAW_PHASE

        if (autoRunAi) {
            if (isGbaMode) {
                drawCard(isPlayerTurn, ignoreHandLimit = true)
            } else {
                val needed = (5 - nextDuelist.hand.size).coerceAtLeast(0)
                for (i in 0 until needed) {
                    drawCard(isPlayerTurn)
                }
            }
            currentPhase = DuelPhase.MAIN_PHASE_1
            if (!isPlayerTurn && winner == null) {
                runAiTurn()
            }
        }
    }

    fun endTurn() {
        passTurn(autoRunAi = true)
    }

    /**
     * Plans the strategic step-by-step sequence of actions for the AI opponent.
     */
    fun planAiTurnSteps(): List<AiTurnStep> {
        val steps = mutableListOf<AiTurnStep>()
        if (isPlayerTurn || winner != null) return steps

        // 1. Draw Phase
        steps.add(AiTurnStep.PhaseTransition(DuelPhase.DRAW_PHASE, "${opponent.name}'s Turn - Draw Phase"))
        if (isGbaMode) {
            val nextCard = opponent.deck.firstOrNull()
            if (nextCard != null) {
                steps.add(AiTurnStep.Draw(nextCard, "${opponent.name} draws a card for turn."))
            }
        } else {
            val drawsNeeded = (5 - opponent.hand.size).coerceAtLeast(0)
            for (d in 0 until drawsNeeded) {
                val nextCard = opponent.deck.getOrNull(d)
                if (nextCard != null) {
                    steps.add(AiTurnStep.Draw(nextCard, "${opponent.name} draws a card."))
                }
            }
        }

        // 2. Main Phase 1
        steps.add(AiTurnStep.PhaseTransition(DuelPhase.MAIN_PHASE_1, "${opponent.name} enters Main Phase 1"))

        // Spells
        val playableSpells = opponent.hand.filter { it.card.isSpell }
        for (dc in playableSpells) {
            val c = dc.card
            if (AiDuelTactics.shouldActivateSpell(opponent.name, c, opponent.monsterZones.toList(), player.monsterZones.toList(), opponent.lifePoints, player.lifePoints)) {
                val taunt = AiDuelTactics.getProfile(opponent.name).spellTauntQuote
                steps.add(AiTurnStep.ActivateSpell(c, "${opponent.name}: \"$taunt\" [${c.name}]!"))
                break
            }
        }

        // Traps
        val emptySpellSlot = opponent.firstEmptySpellSlot()
        if (emptySpellSlot != -1) {
            val trap = opponent.hand.firstOrNull { it.card.isTrap }
            if (trap != null) {
                steps.add(AiTurnStep.SetTrap(trap.card, emptySpellSlot, "${opponent.name} sets 1 card facedown in Spell/Trap Zone."))
            }
        }

        // Monsters to Summon
        val virtualAiMonsters = Array<DuelCard?>(5) { opponent.monsterZones[it]?.copy() }
        val virtualPlayerMonsters = Array<DuelCard?>(5) { player.monsterZones[it]?.copy() }

        // At the start of turn, all AI monsters can attack
        for (m in virtualAiMonsters) {
            m?.hasAttackedThisTurn = false
        }

        if (isGbaMode) {
            val monstersInHand = opponent.hand.filter { it.card.isMonster }
            val level7Plus = monstersInHand.filter { it.card.level >= 7 }
            val level5or6 = monstersInHand.filter { it.card.level in 5..6 }
            val level1to4 = monstersInHand.filter { it.card.level in 1..4 }
            val activeMonsters = (0 until 5).filter { virtualAiMonsters[it] != null }

            if (level7Plus.isNotEmpty() && activeMonsters.size >= 2) {
                val boss = level7Plus.maxByOrNull { it.card.atk }!!
                val targetSlot = activeMonsters.first()
                val tribute2 = activeMonsters[1]
                virtualAiMonsters[activeMonsters[0]] = null
                virtualAiMonsters[tribute2] = null
                virtualAiMonsters[targetSlot] = DuelCard(card = boss.card, position = CardPosition.FACE_UP_ATTACK)
                val bossQuote = AiDuelTactics.getBossSummonDialogue(opponent.name, boss.card.name)
                val summonMsg = if (bossQuote != null) {
                    "⚡ ${opponent.name}: \"$bossQuote\""
                } else {
                    "⚡ TRIBUTE SUMMON! ${opponent.name} sacrifices 2 monsters to summon [${boss.card.name}]! (ATK ${boss.card.atk})"
                }
                steps.add(
                    AiTurnStep.SummonMonster(
                        boss.card,
                        targetSlot,
                        isDefense = false,
                        summonMsg
                    )
                )
            } else if (level5or6.isNotEmpty() && activeMonsters.isNotEmpty()) {
                val boss = level5or6.maxByOrNull { it.card.atk }!!
                val targetSlot = activeMonsters.first()
                virtualAiMonsters[targetSlot] = DuelCard(card = boss.card, position = CardPosition.FACE_UP_ATTACK)
                val bossQuote = AiDuelTactics.getBossSummonDialogue(opponent.name, boss.card.name)
                val summonMsg = if (bossQuote != null) {
                    "⚡ ${opponent.name}: \"$bossQuote\""
                } else {
                    "⚡ TRIBUTE SUMMON! ${opponent.name} sacrifices a monster to summon [${boss.card.name}]! (ATK ${boss.card.atk})"
                }
                steps.add(
                    AiTurnStep.SummonMonster(
                        boss.card,
                        targetSlot,
                        isDefense = false,
                        summonMsg
                    )
                )
            } else if (level1to4.isNotEmpty()) {
                val best = level1to4.maxByOrNull { it.card.atk }!!
                val emptySlot = virtualAiMonsters.indexOfFirst { it == null }
                if (emptySlot != -1) {
                    virtualAiMonsters[emptySlot] = DuelCard(card = best.card, position = CardPosition.FACE_UP_ATTACK)
                    steps.add(
                        AiTurnStep.SummonMonster(
                            best.card,
                            emptySlot,
                            isDefense = false,
                            "${opponent.name} Normal Summons [${best.card.name}] (ATK ${best.card.atk})!"
                        )
                    )
                }
            }
        } else {
            val summonableMonster = opponent.hand
                .filter { it.card.isMonster }
                .maxByOrNull { it.card.atk }

            val emptyMonsterSlot = virtualAiMonsters.indexOfFirst { it == null }
            if (summonableMonster != null && emptyMonsterSlot != -1) {
                val c = summonableMonster.card
                virtualAiMonsters[emptyMonsterSlot] = DuelCard(card = c, position = CardPosition.FACE_UP_ATTACK)
                steps.add(
                    AiTurnStep.SummonMonster(
                        c,
                        emptyMonsterSlot,
                        isDefense = false,
                        "${opponent.name} Normal Summons [${c.name}] (ATK ${c.atk})!"
                    )
                )
            }
        }

        // 3. Battle Phase
        steps.add(AiTurnStep.PhaseTransition(DuelPhase.BATTLE_PHASE, "${opponent.name} enters Battle Phase!"))

        // Attacks
        for (slot in 0 until 5) {
            val aiMonster = virtualAiMonsters[slot] ?: continue
            if (!aiMonster.canDeclareAttack) continue

            val activePlayerIndices = (0 until 5).filter { virtualPlayerMonsters[it] != null }
            if (activePlayerIndices.isEmpty()) {
                // Direct Attack!
                steps.add(
                    AiTurnStep.DeclareAttack(
                        attackerSlot = slot,
                        attackerCard = aiMonster.card,
                        defenderSlot = null,
                        defenderCard = null,
                        predictedDamage = aiMonster.card.atk,
                        isDirect = true,
                        announcement = "⚔ DIRECT ATTACK! ${aiMonster.card.name} strikes directly for ${aiMonster.card.atk} damage!"
                    )
                )
                aiMonster.markAttacked()
            } else {
                // Monster attack: target monster with lower ATK, lower DEF, equal ATK, or face-down defense
                var bestTarget: Int? = null
                var bestScore = -99999

                for (pSlot in activePlayerIndices) {
                    val pMon = virtualPlayerMonsters[pSlot]!!
                    val score = when {
                        // 1. Face-up Attack position with lower ATK: High priority target!
                        pMon.position == CardPosition.FACE_UP_ATTACK && aiMonster.card.atk > pMon.card.atk -> {
                            10000 + (aiMonster.card.atk - pMon.card.atk)
                        }
                        // 2. Face-up Defense position with lower DEF: AI knows it can destroy it!
                        pMon.position == CardPosition.FACE_UP_DEFENSE && aiMonster.card.atk > pMon.card.def -> {
                            5000 + (aiMonster.card.atk - pMon.card.def)
                        }
                        // 3. Unrevealed Face-Down Defense:
                        // Only probe if AI has strong ATK (>= 1400) and enough LP (> 1200) to safely absorb possible reflection damage.
                        // Never attack face-down if AI is at critical LP or has weak monster.
                        pMon.position == CardPosition.FACE_DOWN_DEFENSE && aiMonster.card.atk >= 1400 && opponent.lifePoints > 1200 -> {
                            if (opponent.lifePoints > 2500) 3000 else 1500
                        }
                        // 4. Face-up Attack with equal ATK (trade): only if strong monster (>= 1500)
                        pMon.position == CardPosition.FACE_UP_ATTACK && aiMonster.card.atk == pMon.card.atk && aiMonster.card.atk >= 1500 -> {
                            2000
                        }
                        // Face-up Defense with DEF >= ATK: NEVER attack (score -1, avoid self-damage wall suicide!)
                        else -> -1
                    }

                    // In DDS mode, avoid attacking monsters with elemental advantage
                    val alignmentPenalty = if (!isGbaMode && enableDdsAlignmentRule && !pMon.position.isFaceDown) {
                        if (pMon.card.attribute.hasAlignmentAdvantageOver(aiMonster.card.attribute)) -99999 else 0
                    } else 0

                    val finalScore = if (score > 0) score + alignmentPenalty else -1

                    if (finalScore > bestScore && finalScore > 0) {
                        bestScore = finalScore
                        bestTarget = pSlot
                    }
                }

                if (bestTarget != null) {
                    val targetMon = virtualPlayerMonsters[bestTarget]!!
                    val defenderCard = targetMon.card
                    val isTargetFaceDown = targetMon.position == CardPosition.FACE_DOWN_DEFENSE
                    val predictedDmg = if (targetMon.position == CardPosition.FACE_UP_ATTACK) {
                        (aiMonster.card.atk - defenderCard.atk).coerceAtLeast(0)
                    } else 0

                    val attackDesc = if (isTargetFaceDown) {
                        "⚔ [${aiMonster.card.name}] (ATK ${aiMonster.card.atk}) attacks your face-down monster!"
                    } else {
                        "⚔ [${aiMonster.card.name}] (ATK ${aiMonster.card.atk}) attacks your [${defenderCard.name}]!"
                    }

                    steps.add(
                        AiTurnStep.DeclareAttack(
                            attackerSlot = slot,
                            attackerCard = aiMonster.card,
                            defenderSlot = bestTarget,
                            defenderCard = defenderCard,
                            predictedDamage = predictedDmg,
                            isDirect = false,
                            announcement = attackDesc
                        )
                    )
                    aiMonster.markAttacked()

                    val willDestroy = if (targetMon.position == CardPosition.FACE_UP_ATTACK) {
                        aiMonster.card.atk >= defenderCard.atk
                    } else {
                        aiMonster.card.atk > defenderCard.def
                    }
                    if (willDestroy) {
                        virtualPlayerMonsters[bestTarget] = null
                    } else {
                        // The monster survives and is revealed in FACE_UP_DEFENSE!
                        // Other AI monsters on this same turn will see its actual DEF and won't suicide into it!
                        virtualPlayerMonsters[bestTarget] = targetMon.copy(position = CardPosition.FACE_UP_DEFENSE)
                    }
                }
            }
        }

        // 4. End Turn
        steps.add(AiTurnStep.EndTurn("${opponent.name} ends turn. It is now your turn!"))
        return steps
    }

    /**
     * Executes a single planned AI step on the state machine.
     */
    fun executeAiStep(step: AiTurnStep): Boolean {
        if (winner != null) return false

        return when (step) {
            is AiTurnStep.PhaseTransition -> {
                currentPhase = step.phase
                log("Phase: ${currentPhase.displayName}")
                true
            }
            is AiTurnStep.Draw -> {
                drawCard(isPlayer = false, ignoreHandLimit = isGbaMode)
                true
            }
            is AiTurnStep.ActivateSpell -> {
                val idx = opponent.hand.indexOfFirst { it.card.id == step.card.id || it.card.name == step.card.name }
                if (idx != -1) {
                    activateSpell(isPlayer = false, handIndex = idx)
                } else false
            }
            is AiTurnStep.SetTrap -> {
                val idx = opponent.hand.indexOfFirst { it.card.id == step.card.id || it.card.name == step.card.name }
                if (idx != -1) {
                    setSpellTrap(isPlayer = false, handIndex = idx, slotIndex = step.slotIndex)
                } else false
            }
            is AiTurnStep.SummonMonster -> {
                currentPhase = DuelPhase.MAIN_PHASE_1
                val idx = opponent.hand.indexOfFirst { it.card.id == step.card.id || it.card.name == step.card.name }
                if (idx != -1) {
                    val targetSlot = if (opponent.monsterZones[step.slotIndex] == null) step.slotIndex else opponent.firstEmptyMonsterSlot()
                    summonMonster(isPlayer = false, handIndex = idx, slotIndex = targetSlot, setDefense = step.isDefense)
                } else false
            }
            is AiTurnStep.DeclareAttack -> {
                currentPhase = DuelPhase.BATTLE_PHASE
                declareAttack(attackerSlot = step.attackerSlot, targetSlot = step.defenderSlot)
            }
            is AiTurnStep.EndTurn -> {
                decrementSwordsOfRevealingLight(opponent)
                opponent.resetTurnActions()
                isPlayerTurn = true
                hasNormalSummonedThisTurn = false
                clashState = null
                turnNumber++
                log("--- Turn $turnNumber: ${player.name}'s Turn ---")
                currentPhase = DuelPhase.DRAW_PHASE
                if (isGbaMode) {
                    drawCard(isPlayer = true, ignoreHandLimit = true)
                } else {
                    while (player.hand.size < 5 && player.deck.isNotEmpty()) {
                        drawCard(isPlayer = true)
                    }
                }
                currentPhase = DuelPhase.MAIN_PHASE_1
                true
            }
        }
    }

    /**
     * Executes AI opponent turn with competitive heuristics (headless mode).
     */
    fun runAiTurn() {
        if (isPlayerTurn || winner != null) return
        val steps = planAiTurnSteps()
        for (step in steps) {
            executeAiStep(step)
            if (winner != null) break
        }
    }

    fun surrender(isPlayer: Boolean = true) {
        if (winner != null) return
        val concedingDuelist = if (isPlayer) player else opponent
        concedingDuelist.lifePoints = 0
        log("${concedingDuelist.name} surrendered the duel.")
        checkWinConditions()
    }

    private fun checkWinConditions() {
        if (ToonAnimeArchetype.hasAllExodiaPieces(player.hand)) {
            winner = player.name
            isExodiaWin = true
            log("🌟 EXODIA, OBLITERATE! All 5 Forbidden pieces gathered in ${player.name}'s hand! Instant Victory!")
            val drop = if (isGbaMode) {
                GbaCampaignManager.recordDuelResult(opponent.name, won = true)
            } else {
                DdsCampaignManager.recordDuelResult(opponent.name, won = true)
            }
            lastRewardCard = drop
            return
        } else if (ToonAnimeArchetype.hasAllExodiaPieces(opponent.hand)) {
            winner = opponent.name
            isExodiaWin = true
            log("🌟 EXODIA, OBLITERATE! All 5 Forbidden pieces gathered in ${opponent.name}'s hand! Instant Defeat!")
            if (isGbaMode) {
                GbaCampaignManager.recordDuelResult(opponent.name, won = false)
            } else {
                DdsCampaignManager.recordDuelResult(opponent.name, won = false)
            }
            return
        }

        if (player.lifePoints <= 0 && opponent.lifePoints <= 0) {
            winner = "DRAW"
            log("DUEL RESULT: DRAW!")
        } else if (player.lifePoints <= 0) {
            winner = opponent.name
            if (isGbaMode) {
                GbaCampaignManager.recordDuelResult(opponent.name, won = false)
            } else {
                DdsCampaignManager.recordDuelResult(opponent.name, won = false)
            }
            log("DUEL OVER: ${opponent.name} WINS!")
        } else if (opponent.lifePoints <= 0) {
            winner = player.name
            val drop = if (isGbaMode) {
                GbaCampaignManager.recordDuelResult(opponent.name, won = true)
            } else {
                DdsCampaignManager.recordDuelResult(opponent.name, won = true)
            }
            lastRewardCard = drop
            log("DUEL OVER: ${player.name} WINS! Defeated ${opponent.name}!")
            if (drop != null) {
                log("🎁 WON CARD REWARD: [${drop.name}] added to Trunk!")
            }
        }

        if (isMatchDuel && winner != null) {
            if (winner == player.name) {
                playerMatchWins++
            } else if (winner == opponent.name) {
                opponentMatchWins++
            }
            if (playerMatchWins >= 2) {
                isMatchFinished = true
                matchWinner = player
                log("🏆 MATCH VICTORY! ${player.name} won the Best-of-3 Match ($playerMatchWins - $opponentMatchWins)!")
            } else if (opponentMatchWins >= 2) {
                isMatchFinished = true
                matchWinner = opponent
                log("💀 MATCH DEFEAT! ${opponent.name} won the Best-of-3 Match ($opponentMatchWins - $playerMatchWins)!")
            } else {
                log("⚔️ MATCH UPDATE: Game $matchGameNumber finished! (Score: ${player.name} $playerMatchWins - ${opponent.name} $opponentMatchWins). Side decking enabled before Game ${matchGameNumber + 1}.")
            }
        }
    }
}
