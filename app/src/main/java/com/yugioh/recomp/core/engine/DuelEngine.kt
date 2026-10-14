package com.yugioh.recomp.core.engine

import com.yugioh.recomp.core.rom.CardDataExtractor

class DuelEngine {

    val player = Duelist("Yugi", isHuman = true)
    val opponent = Duelist("Seto Kaiba", isHuman = false)

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

    var clashState: ClashState? = null
        private set

    private val _combatLogs = mutableListOf<String>()
    val combatLogs: List<String> get() = _combatLogs

    fun log(message: String) {
        _combatLogs.add(0, message)
        if (_combatLogs.size > 50) {
            _combatLogs.removeAt(_combatLogs.lastIndex)
        }
    }

    /**
     * Initializes a fresh duel match with 8000 LP and 5-card opening hands.
     */
    fun startDuel() {
        player.lifePoints = 8000
        player.deck.clear()
        player.hand.clear()
        player.graveyard.clear()
        for (i in 0 until 5) {
            player.monsterZones[i] = null
            player.spellTrapZones[i] = null
        }

        opponent.lifePoints = 8000
        opponent.deck.clear()
        opponent.hand.clear()
        opponent.graveyard.clear()
        for (i in 0 until 5) {
            opponent.monsterZones[i] = null
            opponent.spellTrapZones[i] = null
        }

        val (pDeck, oDeck) = CardDataExtractor.buildStarterDecks()
        player.deck.addAll(pDeck)
        opponent.deck.addAll(oDeck)

        // Draw 5 opening cards
        for (i in 0 until 5) {
            drawCard(isPlayer = true)
            drawCard(isPlayer = false)
        }

        turnNumber = 1
        isPlayerTurn = true
        hasNormalSummonedThisTurn = false
        winner = null
        clashState = null
        _combatLogs.clear()

        log("=== DUEL STARTED! ===")
        log("Initial Life Points: 8000 | 8000")
        log("Turn 1 - ${player.name}'s Turn (Main Phase 1)")

        currentPhase = DuelPhase.MAIN_PHASE_1
    }

    /**
     * Draws one card from the deck into the hand.
     */
    fun drawCard(isPlayer: Boolean): Card? {
        val duelist = if (isPlayer) player else opponent
        if (duelist.deck.isEmpty()) {
            val opponentName = if (isPlayer) opponent.name else player.name
            winner = opponentName
            log("${duelist.name} cannot draw from an empty Deck! $opponentName wins!")
            return null
        }
        val card = duelist.deck.removeAt(0)
        duelist.hand.add(DuelCard(card = card))
        log("${duelist.name} drew a card.")
        return card
    }

    /**
     * Checks if a card can be summoned / set.
     */
    fun canSummon(isPlayer: Boolean, card: Card): Pair<Boolean, String> {
        val duelist = if (isPlayer) player else opponent
        if (winner != null) return Pair(false, "Duel is already finished")
        if (isPlayer != isPlayerTurn) return Pair(false, "Not your turn")
        if (currentPhase != DuelPhase.MAIN_PHASE_1 && currentPhase != DuelPhase.MAIN_PHASE_2) {
            return Pair(false, "Can only summon during Main Phase")
        }
        if (hasNormalSummonedThisTurn) {
            return Pair(false, "Already performed a Normal Summon / Set this turn")
        }
        if (duelist.firstEmptyMonsterSlot() == -1 && card.tributeCountRequired == 0) {
            return Pair(false, "No open monster zones")
        }
        if (duelist.activeMonstersCount() < card.tributeCountRequired) {
            return Pair(false, "Requires ${card.tributeCountRequired} tribute(s) on field")
        }
        return Pair(true, "OK")
    }

    /**
     * Summons a monster to the specified zone slot.
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

        // Validate tributes
        if (tributeSlots.size != card.tributeCountRequired) {
            // Auto-select tributes if not explicitly supplied
            val available = (0 until 5).filter { duelist.monsterZones[it] != null }
            if (available.size < card.tributeCountRequired) return false
            val autoTributes = available.take(card.tributeCountRequired)
            for (tSlot in autoTributes) {
                val tMonster = duelist.monsterZones[tSlot]
                if (tMonster != null) {
                    duelist.graveyard.add(tMonster.card)
                    duelist.monsterZones[tSlot] = null
                    log("${duelist.name} tributed ${tMonster.card.name}!")
                }
            }
        } else {
            for (tSlot in tributeSlots) {
                val tMonster = duelist.monsterZones[tSlot] ?: return false
                duelist.graveyard.add(tMonster.card)
                duelist.monsterZones[tSlot] = null
                log("${duelist.name} tributed ${tMonster.card.name}!")
            }
        }

        // Find target slot
        val targetSlot = if (slotIndex in 0..4 && duelist.monsterZones[slotIndex] == null) {
            slotIndex
        } else {
            duelist.firstEmptyMonsterSlot()
        }

        if (targetSlot == -1) return false

        duelist.hand.removeAt(handIndex)
        duelCard.position = if (setDefense) CardPosition.FACE_DOWN_DEFENSE else CardPosition.FACE_UP_ATTACK
        duelCard.hasAttackedThisTurn = false
        duelCard.canChangePosition = false
        duelCard.turnsOnField = 0
        duelist.monsterZones[targetSlot] = duelCard
        hasNormalSummonedThisTurn = true

        if (setDefense) {
            log("${duelist.name} Set a monster in face-down defense position.")
        } else {
            log("${duelist.name} Normal Summoned ${card.name} (${card.atk}/${card.def}) in Attack Position!")
        }
        return true
    }

    /**
     * Activates a Spell card from hand.
     */
    fun activateSpell(isPlayer: Boolean, handIndex: Int): Boolean {
        val duelist = if (isPlayer) player else opponent
        val other = if (isPlayer) opponent else player
        if (handIndex !in duelist.hand.indices) return false
        val duelCard = duelist.hand[handIndex]
        val card = duelCard.card
        if (!card.isSpell) return false

        if (currentPhase != DuelPhase.MAIN_PHASE_1 && currentPhase != DuelPhase.MAIN_PHASE_2) {
            log("Can only activate Spells during Main Phase")
            return false
        }

        duelist.hand.removeAt(handIndex)
        duelist.graveyard.add(card)
        log("${duelist.name} activated Spell: [${card.name}]!")

        when (card.name) {
            "Pot of Greed" -> {
                drawCard(isPlayer)
                drawCard(isPlayer)
                log("Pot of Greed: ${duelist.name} drew 2 cards!")
            }
            "Raigeki" -> {
                var count = 0
                for (i in 0 until 5) {
                    val m = other.monsterZones[i]
                    if (m != null) {
                        other.graveyard.add(m.card)
                        other.monsterZones[i] = null
                        count++
                    }
                }
                log("Raigeki: Destroyed $count monster(s) on ${other.name}'s field!")
            }
            "Dark Hole" -> {
                for (i in 0 until 5) {
                    player.monsterZones[i]?.let { player.graveyard.add(it.card); player.monsterZones[i] = null }
                    opponent.monsterZones[i]?.let { opponent.graveyard.add(it.card); opponent.monsterZones[i] = null }
                }
                log("Dark Hole: All monsters on the field were destroyed!")
            }
            "Dian Keto the Cure Maiden" -> {
                duelist.lifePoints += 1000
                log("Dian Keto: ${duelist.name} gained +1000 LP (Now: ${duelist.lifePoints})")
            }
            "Sparks" -> {
                other.lifePoints = (other.lifePoints - 500).coerceAtLeast(0)
                log("Sparks: Inflicted 500 direct damage to ${other.name} (Remaining: ${other.lifePoints})")
                checkWinConditions()
            }
            else -> {
                log("Spell resolved.")
            }
        }
        return true
    }

    /**
     * Sets a Spell or Trap facedown in a Spell/Trap Zone.
     */
    fun setSpellTrap(isPlayer: Boolean, handIndex: Int, slotIndex: Int): Boolean {
        val duelist = if (isPlayer) player else opponent
        if (handIndex !in duelist.hand.indices) return false
        val duelCard = duelist.hand[handIndex]
        val card = duelCard.card
        if (!card.isSpell && !card.isTrap) return false

        val targetSlot = if (slotIndex in 0..4 && duelist.spellTrapZones[slotIndex] == null) {
            slotIndex
        } else {
            duelist.firstEmptySpellSlot()
        }

        if (targetSlot == -1) {
            log("No open Spell & Trap zones.")
            return false
        }

        duelist.hand.removeAt(handIndex)
        duelCard.position = CardPosition.SET_SPELL_TRAP
        duelist.spellTrapZones[targetSlot] = duelCard
        log("${duelist.name} Set 1 card facedown in Spell/Trap Zone.")
        return true
    }

    /**
     * Changes monster battle position between Attack and Defense.
     */
    fun changeBattlePosition(isPlayer: Boolean, slotIndex: Int): Boolean {
        val duelist = if (isPlayer) player else opponent
        if (slotIndex !in 0..4) return false
        val monster = duelist.monsterZones[slotIndex] ?: return false
        if (!monster.canChangePosition || monster.turnsOnField == 0) {
            log("Cannot change position this turn")
            return false
        }

        monster.position = if (monster.position == CardPosition.FACE_UP_ATTACK) {
            CardPosition.FACE_UP_DEFENSE
        } else {
            CardPosition.FACE_UP_ATTACK
        }
        monster.canChangePosition = false
        log("${duelist.name} changed ${monster.card.name} to ${monster.position.name}.")
        return true
    }

    /**
     * Declares an attack from attackerSlot against targetSlot (or direct attack if null).
     */
    fun declareAttack(attackerSlot: Int, targetSlot: Int?): Boolean {
        if (winner != null) return false
        if (currentPhase != DuelPhase.BATTLE_PHASE) {
            log("Attacks can only be declared during the Battle Phase")
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
        if (attackerMonster.hasAttackedThisTurn) {
            log("${attackerMonster.card.name} has already declared an attack this turn")
            return false
        }

        // Direct attack check
        if (targetSlot == null) {
            if (defenderDuelist.activeMonstersCount() > 0) {
                log("Cannot attack directly while opponent controls monsters!")
                return false
            }

            attackerMonster.hasAttackedThisTurn = true
            val damage = attackerMonster.card.atk
            defenderDuelist.lifePoints = (defenderDuelist.lifePoints - damage).coerceAtLeast(0)

            clashState = ClashState(
                attackerCard = attackerMonster.card,
                defenderCard = null,
                isDirectAttack = true,
                damageDealt = damage,
                damageToPlayer = !isPlayerTurn,
                resultSummary = "DIRECT ATTACK! -${damage} LP to ${defenderDuelist.name}"
            )
            log("DIRECT ATTACK! ${attackerMonster.card.name} attacked ${defenderDuelist.name} for $damage damage!")
            checkWinConditions()
            return true
        }

        // Monster vs Monster attack
        if (targetSlot !in 0..4) return false
        val targetMonster = defenderDuelist.monsterZones[targetSlot] ?: return false

        attackerMonster.hasAttackedThisTurn = true

        val aAtk = attackerMonster.card.atk
        val dCard = targetMonster.card

        if (targetMonster.position == CardPosition.FACE_UP_ATTACK) {
            val dAtk = dCard.atk
            if (aAtk > dAtk) {
                val diff = aAtk - dAtk
                defenderDuelist.lifePoints = (defenderDuelist.lifePoints - diff).coerceAtLeast(0)
                defenderDuelist.graveyard.add(dCard)
                defenderDuelist.monsterZones[targetSlot] = null

                clashState = ClashState(
                    attackerCard = attackerMonster.card,
                    defenderCard = dCard,
                    isDirectAttack = false,
                    damageDealt = diff,
                    damageToPlayer = !isPlayerTurn,
                    resultSummary = "${attackerMonster.card.name} destroys ${dCard.name}! (-$diff LP to ${defenderDuelist.name})"
                )
                log("${attackerMonster.card.name} ($aAtk) destroyed ${dCard.name} ($dAtk)! $diff LP damage to ${defenderDuelist.name}.")
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
                    resultSummary = "${dCard.name} counters! ${attackerMonster.card.name} destroyed! (-$diff LP)"
                )
                log("${attackerMonster.card.name} was defeated by ${dCard.name}! $diff damage to ${attackerDuelist.name}.")
            } else {
                // Tie: both destroyed
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
                    resultSummary = "DOUBLE KNOCKOUT! Both monsters destroyed!"
                )
                log("Both ${attackerMonster.card.name} and ${dCard.name} were destroyed!")
            }
        } else {
            // Target is in DEFENSE position
            val dDef = dCard.def
            if (aAtk > dDef) {
                defenderDuelist.graveyard.add(dCard)
                defenderDuelist.monsterZones[targetSlot] = null

                clashState = ClashState(
                    attackerCard = attackerMonster.card,
                    defenderCard = dCard,
                    isDirectAttack = false,
                    damageDealt = 0,
                    damageToPlayer = false,
                    resultSummary = "${attackerMonster.card.name} destroyed defense monster ${dCard.name}!"
                )
                log("${attackerMonster.card.name} ($aAtk) pierced ${dCard.name}'s Defense ($dDef)!")
            } else if (aAtk < dDef) {
                val diff = dDef - aAtk
                attackerDuelist.lifePoints = (attackerDuelist.lifePoints - diff).coerceAtLeast(0)

                clashState = ClashState(
                    attackerCard = attackerMonster.card,
                    defenderCard = dCard,
                    isDirectAttack = false,
                    damageDealt = diff,
                    damageToPlayer = isPlayerTurn,
                    resultSummary = "${dCard.name}'s Defense deflected the attack! (-$diff LP)"
                )
                log("${attackerMonster.card.name} failed to breach ${dCard.name}'s DEF! $diff damage to ${attackerDuelist.name}.")
            } else {
                clashState = ClashState(
                    attackerCard = attackerMonster.card,
                    defenderCard = dCard,
                    isDirectAttack = false,
                    damageDealt = 0,
                    damageToPlayer = false,
                    resultSummary = "Stalemate: Neither monster destroyed."
                )
                log("Attack blocked by defense; neither monster destroyed.")
            }
        }

        checkWinConditions()
        return true
    }

    /**
     * Moves to the next duel phase in sequence.
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
    fun endTurn() {
        val currentDuelist = if (isPlayerTurn) player else opponent
        currentDuelist.resetTurnActions()

        isPlayerTurn = !isPlayerTurn
        hasNormalSummonedThisTurn = false
        clashState = null

        val nextDuelist = if (isPlayerTurn) player else opponent
        if (isPlayerTurn) {
            turnNumber++
        }

        log("--- Turn $turnNumber: ${nextDuelist.name}'s Turn ---")
        currentPhase = DuelPhase.DRAW_PHASE
        drawCard(isPlayerTurn)
        currentPhase = DuelPhase.MAIN_PHASE_1

        if (!isPlayerTurn && winner == null) {
            runAiTurn()
        }
    }

    /**
     * Executes AI opponent turn with competitive heuristics.
     */
    fun runAiTurn() {
        if (isPlayerTurn || winner != null) return

        // 1. Check Spells in hand
        val spells = opponent.hand.mapIndexed { idx, dc -> Pair(idx, dc) }
            .filter { it.second.card.isSpell }
        for ((_, spell) in spells) {
            if (spell.card.name == "Pot of Greed" || spell.card.name == "Raigeki" || spell.card.name == "Dian Keto the Cure Maiden" || spell.card.name == "Sparks") {
                activateSpell(isPlayer = false, handIndex = opponent.hand.indexOf(spell))
                break
            }
        }

        // 2. Summon best available monster
        val monsterOptions = opponent.hand.mapIndexed { idx, dc -> Pair(idx, dc) }
            .filter { it.second.card.isMonster }
            .sortedByDescending { it.second.card.atk }

        for ((_, mCard) in monsterOptions) {
            val hIdx = opponent.hand.indexOf(mCard)
            if (hIdx == -1) continue
            val (can, _) = canSummon(isPlayer = false, mCard.card)
            if (can) {
                summonMonster(isPlayer = false, handIndex = hIdx, slotIndex = opponent.firstEmptyMonsterSlot())
                break
            }
        }

        // 3. Move to Battle Phase
        currentPhase = DuelPhase.BATTLE_PHASE

        for (slot in 0 until 5) {
            val aiMonster = opponent.monsterZones[slot] ?: continue
            if (aiMonster.position != CardPosition.FACE_UP_ATTACK || aiMonster.hasAttackedThisTurn) continue

            if (player.activeMonstersCount() == 0) {
                // Direct attack!
                declareAttack(slot, null)
                if (winner != null) return
            } else {
                // Find a player monster we can defeat
                var bestTarget: Int? = null
                var maxDmg = -1

                for (pSlot in 0 until 5) {
                    val pMon = player.monsterZones[pSlot] ?: continue
                    val targetStat = if (pMon.position == CardPosition.FACE_UP_ATTACK) pMon.card.atk else pMon.card.def
                    if (aiMonster.card.atk > targetStat) {
                        val dmg = aiMonster.card.atk - targetStat
                        if (dmg > maxDmg) {
                            maxDmg = dmg
                            bestTarget = pSlot
                        }
                    }
                }

                if (bestTarget != null) {
                    declareAttack(slot, bestTarget)
                    if (winner != null) return
                }
            }
        }

        // 4. End Turn
        currentPhase = DuelPhase.END_PHASE
        endTurn()
    }

    private fun checkWinConditions() {
        if (player.lifePoints <= 0 && opponent.lifePoints <= 0) {
            winner = "DRAW"
            log("DUEL RESULT: DRAW!")
        } else if (player.lifePoints <= 0) {
            winner = opponent.name
            log("DUEL OVER: ${opponent.name} WINS!")
        } else if (opponent.lifePoints <= 0) {
            winner = player.name
            log("DUEL OVER: ${player.name} WINS!")
        }
    }
}
