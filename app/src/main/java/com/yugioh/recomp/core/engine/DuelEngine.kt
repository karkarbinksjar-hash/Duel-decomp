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

    var lastRewardCard: Card? = null
        private set

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
    fun startDuel(opponentName: String = if (isGbaMode) GbaCampaignManager.selectedOpponentName else DdsCampaignManager.selectedOpponentName) {
        opponent = Duelist(opponentName, isHuman = false)

        player.lifePoints = 8000
        player.deck.clear()
        player.hand.clear()
        player.graveyard.clear()
        player.swordsTurnsRemaining = 0
        for (i in 0 until 5) {
            player.monsterZones[i] = null
            player.spellTrapZones[i] = null
        }

        opponent.lifePoints = 8000
        opponent.deck.clear()
        opponent.hand.clear()
        opponent.graveyard.clear()
        opponent.swordsTurnsRemaining = 0
        for (i in 0 until 5) {
            opponent.monsterZones[i] = null
            opponent.spellTrapZones[i] = null
        }

        val (pDeck, oDeck) = if (isGbaMode) {
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
        clashState = null
        lastRewardCard = null
        _combatLogs.clear()

        val rulesLabel = if (isGbaMode) "GBA TCG Expert Rules" else "DDS GBC Rules"
        log("=== DUEL STARTED vs $opponentName! ===")
        log("Initial Life Points: 8000 | 8000 ($rulesLabel)")
        log("Turn 1 - ${player.name}'s Turn (Main Phase 1)")

        currentPhase = DuelPhase.MAIN_PHASE_1
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
        if (isGbaMode) {
            val tributesNeeded = when {
                card.level >= 7 -> 2
                card.level in 5..6 -> 1
                else -> 0
            }
            val activeMonsters = duelist.activeMonstersCount()
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

            duelist.hand.removeAt(handIndex)
            val pos = if (setDefense) CardPosition.FACE_DOWN_DEFENSE else CardPosition.FACE_UP_ATTACK
            duelist.monsterZones[targetSlot] = DuelCard(card = card, position = pos, canChangePosition = false)
            hasNormalSummonedThisTurn = true

            val actionDesc = if (tributesNeeded > 0) "Tribute Summoned" else if (setDefense) "Set" else "Normal Summoned"
            log("${duelist.name} $actionDesc [${card.name}] (ATK ${card.atk} / DEF ${card.def}) to Zone ${targetSlot + 1}.")

            val opponentDuelist = if (isPlayer) opponent else player
            checkTrapHole(opponentDuelist, duelist, targetSlot)

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

        duelist.hand.removeAt(handIndex)
        val pos = if (setDefense) CardPosition.FACE_DOWN_DEFENSE else CardPosition.FACE_UP_ATTACK
        duelist.monsterZones[targetSlot] = DuelCard(card = card, position = pos, canChangePosition = false)
        hasNormalSummonedThisTurn = true
        log("${duelist.name} placed [${card.name}] in Zone ${targetSlot + 1}.")

        val opponentDuelist = if (isPlayer) opponent else player
        checkTrapHole(opponentDuelist, duelist, targetSlot)

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

    fun getEffectiveAtk(card: Card): Int =
        (card.atk + getFieldSpellModifier(activeFieldSpell, card)).coerceAtLeast(0)

    fun getEffectiveDef(card: Card): Int =
        (card.def + getFieldSpellModifier(activeFieldSpell, card)).coerceAtLeast(0)

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
            summoningDuelist.monsterZones[summonedSlot] = null
            summoningDuelist.graveyard.add(summonedMonster.card)
            log("TRAP TRIGGER: ${trapOwner.name} activated [Trap Hole]! [${summonedMonster.card.name}] was destroyed!")
        }
    }

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
                log("⚡ CHAIN LINK 1: ${duelist.name} activates Spell: [${card.name}]")
                log("⚡ CHAIN LINK 2: ${opp.name} chains Counter-Trap: [Magic Jammer] (Discarded [${discard.card.name}])!")
                log("❌ CHAIN RESOLUTION: [Magic Jammer] negates and destroys [${card.name}]!")
                return true
            } else if (counterTrap.name == "Solemn Judgment" && opp.lifePoints > 1) {
                opp.lifePoints /= 2
                opp.spellTrapZones[counterSlot] = null
                opp.graveyard.add(counterTrap)
                duelist.hand.removeAt(handIndex)
                duelist.graveyard.add(card)
                log("⚡ CHAIN LINK 1: ${duelist.name} activates Spell: [${card.name}]")
                log("⚡ CHAIN LINK 2: ${opp.name} chains Counter-Trap: [Solemn Judgment] (Paid half LP: ${opp.lifePoints})!")
                log("❌ CHAIN RESOLUTION: [Solemn Judgment] negates and destroys [${card.name}]!")
                return true
            }
        }

        val emptySlot = duelist.firstEmptySpellSlot()
        val tempSlot = if (emptySlot != -1) emptySlot else 0

        duelist.hand.removeAt(handIndex)
        duelist.spellTrapZones[tempSlot] = DuelCard(card = card, position = CardPosition.FACE_UP_ATTACK)
        log("${duelist.name} activated Spell: [${card.name}]!")

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
                        log("${duelist.name} Special Summoned [${revived.name}] from Graveyard via Monster Reborn!")
                    }
                }
            }
            "Change of Heart", "Snatch Steal" -> {
                val opp = if (isPlayer) opponent else player
                val oppSlot = (0 until 5).firstOrNull { opp.monsterZones[it] != null }
                val mySlot = duelist.firstEmptyMonsterSlot()
                if (oppSlot != null && mySlot != -1) {
                    val stolen = opp.monsterZones[oppSlot]!!
                    opp.monsterZones[oppSlot] = null
                    duelist.monsterZones[mySlot] = stolen
                    log("${duelist.name} took control of [${stolen.card.name}] via ${card.name}!")
                }
            }
            "Swords of Revealing Light" -> {
                val opp = if (isPlayer) opponent else player
                opp.swordsTurnsRemaining = 3
                log("Swords of Revealing Light prevents ${opp.name} from attacking for 3 turns!")
            }
            "Raigeki" -> {
                val opp = if (isPlayer) opponent else player
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
                val opp = if (isPlayer) opponent else player
                var count = 0
                for (i in 0 until 5) {
                    val st = opp.spellTrapZones[i]
                    if (st != null) {
                        opp.graveyard.add(st.card)
                        opp.spellTrapZones[i] = null
                        count++
                    }
                }
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
                log("Heavy Storm swept away $count Spell/Trap(s) on the field!")
            }
            "Dian Keto the Cure Maiden" -> {
                duelist.lifePoints += 1000
                log("${duelist.name} gained 1000 LP (Total: ${duelist.lifePoints})")
            }
            "Sparks" -> {
                val opp = if (isPlayer) opponent else player
                opp.lifePoints = (opp.lifePoints - 200).coerceAtLeast(0)
                log("Sparks dealt 200 damage to ${opp.name}! (LP: ${opp.lifePoints})")
                checkWinConditions()
            }
            "Hinotama" -> {
                val opp = if (isPlayer) opponent else player
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
        }

        duelist.spellTrapZones[tempSlot] = null
        if (card.name !in listOf("Yami", "Forest", "Sogen", "Mountain", "Umi", "Wasteland")) {
            duelist.graveyard.add(card)
        }
        return true
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
                    m.canChangePosition = false
                    log("🕸️ [Spellbinding Circle] binds [${m.card.name}]! Cannot attack or change position.")
                }
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
        val monster = duelist.monsterZones.getOrNull(slotIndex) ?: return false
        return monster.canDeclareAttack
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
                        log("⚡ CHAIN LINK 1: ${defenderDuelist.name} triggered Trap: [Magic Cylinder]!")
                        log("⚡ CHAIN LINK 2: ${attackerDuelist.name} chains Counter-Trap: [Seven Tools of the Bandit] (Paid 1000 LP)!")
                        log("❌ CHAIN RESOLUTION: [Seven Tools of the Bandit] negates and destroys [Magic Cylinder]!")
                    } else {
                        val damage = attackerMonster.card.atk
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
                    val damage = attackerMonster.card.atk
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
                        log("⚡ CHAIN LINK 1: ${defenderDuelist.name} triggered Trap: [Mirror Force]!")
                        log("⚡ CHAIN LINK 2: ${attackerDuelist.name} chains Counter-Trap: [Seven Tools of the Bandit] (Paid 1000 LP)!")
                        log("❌ CHAIN RESOLUTION: [Seven Tools of the Bandit] negates and destroys [Mirror Force]!")
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
            if (defenderDuelist.activeMonstersCount() > 0) {
                log("Cannot attack directly while opponent controls monsters!")
                return false
            }

            attackerMonster.markAttacked()
            val damage = getEffectiveAtk(attackerMonster.card)
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
                resultSummary = "${attackerMonster.card.name} attacks directly! (-$damage LP)"
            )
            log("DIRECT ATTACK: ${attackerMonster.card.name} attacked directly! $damage damage to ${defenderDuelist.name}.")
            checkWinConditions()
            return true
        }

        // Monster vs Monster battle
        if (targetSlot !in 0..4) return false
        val targetMonster = defenderDuelist.monsterZones[targetSlot] ?: return false

        attackerMonster.markAttacked()
        val aAtk = getEffectiveAtk(attackerMonster.card)
        val dCard = targetMonster.card

        // Yu-Gi-Oh! Official Rule: When a face-down monster is attacked, it is flipped face-up in the Damage Step before damage calculation
        val wasFaceDown = targetMonster.position == CardPosition.FACE_DOWN_DEFENSE
        if (wasFaceDown) {
            targetMonster.position = CardPosition.FACE_UP_DEFENSE
            log("${defenderDuelist.name}'s face-down monster was flipped face-up: [${dCard.name}] (${getEffectiveDef(dCard)} DEF)!")
        }

        // Check Dark Duel Stories Elemental Alignment Dominance (GBC DDS Only)
        if (!isGbaMode && enableDdsAlignmentRule) {
            val aAttr = attackerMonster.card.attribute
            val dAttr = dCard.attribute
            if (aAttr.hasAlignmentAdvantageOver(dAttr)) {
                defenderDuelist.graveyard.add(dCard)
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
                attackerDuelist.graveyard.add(attackerMonster.card)
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
            val dAtk = getEffectiveAtk(dCard)
            if (aAtk > dAtk) {
                val diff = aAtk - dAtk
                defenderDuelist.lifePoints = (defenderDuelist.lifePoints - diff).coerceAtLeast(0)
                defenderDuelist.graveyard.add(dCard)
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
                val diff = dAtk - aAtk
                attackerDuelist.lifePoints = (attackerDuelist.lifePoints - diff).coerceAtLeast(0)
                attackerDuelist.graveyard.add(attackerMonster.card)
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
                attackerDuelist.graveyard.add(attackerMonster.card)
                attackerDuelist.monsterZones[attackerSlot] = null
                defenderDuelist.graveyard.add(dCard)
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
            val dDef = getEffectiveDef(dCard)
            val flipPrefix = if (wasFaceDown) "Flipped face-up! " else ""
            if (aAtk > dDef) {
                defenderDuelist.graveyard.add(dCard)
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
                val diff = dDef - aAtk
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
            DuelPhase.DRAW_PHASE -> currentPhase = DuelPhase.STANDBY_PHASE
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
        currentDuelist.resetTurnActions()

        isPlayerTurn = !isPlayerTurn
        hasNormalSummonedThisTurn = false
        clashState = null

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
            if (c.name in listOf("Pot of Greed", "Graceful Charity", "Raigeki", "Dark Hole", "Monster Reborn", "Snatch Steal", "Change of Heart", "Swords of Revealing Light", "Harpie's Feather Duster", "Heavy Storm", "Dian Keto the Cure Maiden", "Sparks", "Hinotama", "Mooyan Curry")) {
                steps.add(AiTurnStep.ActivateSpell(c, "${opponent.name} activates Spell: [${c.name}]!"))
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
                steps.add(
                    AiTurnStep.SummonMonster(
                        boss.card,
                        targetSlot,
                        isDefense = false,
                        "⚡ TRIBUTE SUMMON! ${opponent.name} sacrifices 2 monsters to summon [${boss.card.name}]! (ATK ${boss.card.atk})"
                    )
                )
            } else if (level5or6.isNotEmpty() && activeMonsters.isNotEmpty()) {
                val boss = level5or6.maxByOrNull { it.card.atk }!!
                val targetSlot = activeMonsters.first()
                virtualAiMonsters[targetSlot] = DuelCard(card = boss.card, position = CardPosition.FACE_UP_ATTACK)
                steps.add(
                    AiTurnStep.SummonMonster(
                        boss.card,
                        targetSlot,
                        isDefense = false,
                        "⚡ TRIBUTE SUMMON! ${opponent.name} sacrifices a monster to summon [${boss.card.name}]! (ATK ${boss.card.atk})"
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
    }
}
