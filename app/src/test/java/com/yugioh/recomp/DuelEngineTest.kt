package com.yugioh.recomp

import com.yugioh.recomp.core.engine.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DuelEngineTest {

    private lateinit var engine: DuelEngine

    @Before
    fun setUp() {
        engine = DuelEngine()
        engine.startDuel()
    }

    @Test
    fun testDuelInitialization() {
        assertEquals(8000, engine.player.lifePoints)
        assertEquals(8000, engine.opponent.lifePoints)
        assertEquals(5, engine.player.hand.size)
        assertEquals(5, engine.opponent.hand.size)
        assertEquals(1, engine.turnNumber)
        assertTrue(engine.isPlayerTurn)
        assertNull(engine.winner)
    }

    @Test
    fun testNormalSummonMonster() {
        // Place a Level 4 monster into player's hand
        val level4Monster = Card(
            id = 24,
            name = "Celtic Guardian",
            atk = 1400,
            def = 1200,
            level = 4,
            attribute = CardAttribute.EARTH,
            cardType = CardType.MONSTER,
            monsterType = MonsterType.WARRIOR
        )
        engine.player.hand.add(0, DuelCard(card = level4Monster))

        val success = engine.summonMonster(
            isPlayer = true,
            handIndex = 0,
            slotIndex = 0,
            setDefense = false
        )

        assertTrue(success)
        assertNotNull(engine.player.monsterZones[0])
        assertEquals("Celtic Guardian", engine.player.monsterZones[0]?.card?.name)
        assertEquals(CardPosition.FACE_UP_ATTACK, engine.player.monsterZones[0]?.position)
        assertTrue(engine.hasNormalSummonedThisTurn)
    }

    @Test
    fun testDdsNoTributeSummonRule() {
        // In Dark Duel Stories (GBC), high level monsters (e.g. Level 7 Dark Magician)
        // do not require tributes to normal summon directly.
        val highLevelMonster = Card(
            id = 14,
            name = "Dark Magician",
            atk = 2500,
            def = 2100,
            level = 7,
            attribute = CardAttribute.DARK,
            cardType = CardType.MONSTER,
            monsterType = MonsterType.SPELLCASTER
        )
        engine.player.hand.add(0, DuelCard(card = highLevelMonster))

        // DDS rule: can summon without tributes
        val (canSummon, reason) = engine.canSummon(isPlayer = true, card = highLevelMonster)
        assertTrue("DDS rules allow direct summon without tribute requirement: $reason", canSummon)

        val success = engine.summonMonster(
            isPlayer = true,
            handIndex = 0,
            slotIndex = 0,
            setDefense = false
        )
        assertTrue(success)
        assertEquals("Dark Magician", engine.player.monsterZones[0]?.card?.name)
    }

    @Test
    fun testDirectAttackAndWinCondition() {
        // Player has Blue-Eyes White Dragon in attack mode
        val bewd = Card(id = 1, name = "Blue-Eyes White Dragon", atk = 3000, def = 2500, level = 8, attribute = CardAttribute.LIGHT, cardType = CardType.MONSTER)
        engine.player.monsterZones[0] = DuelCard(card = bewd, position = CardPosition.FACE_UP_ATTACK)

        // Ensure opponent has no monsters
        for (i in 0 until 5) engine.opponent.monsterZones[i] = null

        // Advance to Battle Phase
        engine.nextPhase() // DP/Standby -> Battle
        if (engine.currentPhase != DuelPhase.BATTLE_PHASE) {
            engine.nextPhase()
        }

        // Opponent LP starts at 8000
        engine.declareAttack(attackerSlot = 0, targetSlot = null)

        assertEquals(5000, engine.opponent.lifePoints)
        assertNotNull(engine.clashState)
        assertTrue(engine.clashState!!.isDirectAttack)
        assertEquals(3000, engine.clashState!!.damageDealt)

        // Reduce opponent LP to 0 to verify win condition
        engine.opponent.lifePoints = 1000
        val bewd2 = DuelCard(card = bewd, position = CardPosition.FACE_UP_ATTACK)
        engine.player.monsterZones[1] = bewd2

        engine.declareAttack(attackerSlot = 1, targetSlot = null)
        assertEquals(0, engine.opponent.lifePoints)
        assertEquals("Yugi", engine.winner)
    }

    @Test
    fun testSpellActivations() {
        // Set player hand to 2 cards so Pot of Greed (removes 1, draws 2) respects DDS 5-card hand limit
        engine.player.hand.clear()
        engine.player.hand.add(DuelCard(card = Card(id = 2, name = "Monster 1", atk = 500, def = 500, level = 2, attribute = CardAttribute.EARTH, cardType = CardType.MONSTER)))
        engine.player.hand.add(DuelCard(card = Card(id = 3, name = "Monster 2", atk = 600, def = 600, level = 2, attribute = CardAttribute.EARTH, cardType = CardType.MONSTER)))

        // Pot of Greed
        val potOfGreed = Card(id = 150, name = "Pot of Greed", atk = 0, def = 0, level = 0, attribute = CardAttribute.SPELL, cardType = CardType.SPELL)
        engine.player.hand.add(0, DuelCard(card = potOfGreed))
        val handCountBefore = engine.player.hand.size // 3

        engine.activateSpell(isPlayer = true, handIndex = 0)
        // Hand should have -1 for Pot of Greed + 2 drawn = handCountBefore + 1 (4 cards <= 5 hand limit)
        assertEquals(handCountBefore + 1, engine.player.hand.size)

        // Dian Keto the Cure Maiden
        val dianKeto = Card(id = 153, name = "Dian Keto the Cure Maiden", atk = 0, def = 0, level = 0, attribute = CardAttribute.SPELL, cardType = CardType.SPELL)
        engine.player.hand.add(0, DuelCard(card = dianKeto))
        val lpBefore = engine.player.lifePoints
        engine.activateSpell(isPlayer = true, handIndex = 0)
        assertEquals(lpBefore + 1000, engine.player.lifePoints)
    }

    @Test
    fun testDdsAlignmentDominance() {
        engine.enableDdsAlignmentRule = true

        // Advance to Battle Phase
        while (engine.currentPhase != DuelPhase.BATTLE_PHASE) {
            engine.nextPhase()
        }

        // Setup: Dark attribute attacker (e.g. 500 ATK) vs Light attribute defender (e.g. 3000 ATK Blue-Eyes)
        val darkAttacker = Card(id = 10, name = "Kuriboh", atk = 300, def = 200, level = 1, attribute = CardAttribute.DARK, cardType = CardType.MONSTER)
        val lightDefender = Card(id = 1, name = "Blue-Eyes White Dragon", atk = 3000, def = 2500, level = 8, attribute = CardAttribute.LIGHT, cardType = CardType.MONSTER)

        engine.player.monsterZones[0] = DuelCard(card = darkAttacker, position = CardPosition.FACE_UP_ATTACK)
        engine.opponent.monsterZones[0] = DuelCard(card = lightDefender, position = CardPosition.FACE_UP_ATTACK)

        // Dark has alignment advantage over Light in Dark Duel Stories rules
        assertTrue(CardAttribute.DARK.hasAlignmentAdvantageOver(CardAttribute.LIGHT))
        assertFalse(CardAttribute.LIGHT.hasAlignmentAdvantageOver(CardAttribute.DARK))

        val attacked = engine.declareAttack(attackerSlot = 0, targetSlot = 0)
        assertTrue(attacked)
        // Opponent's Blue-Eyes must be destroyed despite having 3000 ATK vs 300 ATK!
        assertNull(engine.opponent.monsterZones[0])
        assertNotNull(engine.player.monsterZones[0])
        assertEquals(1, engine.opponent.graveyard.size)
    }

    @Test
    fun testCardPasscodeRegistry() {
        assertTrue(CardPasscodeRegistry.isValidPasscode("89631139"))
        assertEquals("Blue-Eyes White Dragon", CardPasscodeRegistry.findCardNameByPasscode("89631139"))
        assertEquals("Dark Magician", CardPasscodeRegistry.findCardNameByPasscode("46986414"))
        assertEquals("Pot of Greed", CardPasscodeRegistry.findCardNameByPasscode("55144522"))
        assertFalse(CardPasscodeRegistry.isValidPasscode("00000000"))
        assertNull(CardPasscodeRegistry.findCardNameByPasscode("00000000"))
    }

    @Test
    fun testTrapHoleTrigger() {
        // Opponent has a set Trap Hole
        val trapHole = Card(id = 201, name = "Trap Hole", atk = 0, def = 0, level = 0, attribute = CardAttribute.TRAP, cardType = CardType.TRAP)
        engine.opponent.spellTrapZones[0] = DuelCard(card = trapHole, position = CardPosition.SET_SPELL_TRAP)

        // Player normal summons a monster with ATK >= 1000 in attack position
        val celticGuardian = Card(id = 24, name = "Celtic Guardian", atk = 1400, def = 1200, level = 4, attribute = CardAttribute.EARTH, cardType = CardType.MONSTER)
        engine.player.hand.add(0, DuelCard(card = celticGuardian))

        val summoned = engine.summonMonster(isPlayer = true, handIndex = 0, slotIndex = 0, setDefense = false)
        assertTrue(summoned)

        // Trap Hole should trigger, sending Celtic Guardian to player's GY and Trap Hole to opponent's GY
        assertNull(engine.player.monsterZones[0])
        assertNull(engine.opponent.spellTrapZones[0])
        assertEquals(1, engine.player.graveyard.size)
        assertEquals("Celtic Guardian", engine.player.graveyard[0].name)
        assertEquals(1, engine.opponent.graveyard.size)
        assertEquals("Trap Hole", engine.opponent.graveyard[0].name)
    }

    @Test
    fun testMirrorForceTrigger() {
        // Opponent has a set Mirror Force
        val mirrorForce = Card(id = 200, name = "Mirror Force", atk = 0, def = 0, level = 0, attribute = CardAttribute.TRAP, cardType = CardType.TRAP)
        engine.opponent.spellTrapZones[0] = DuelCard(card = mirrorForce, position = CardPosition.SET_SPELL_TRAP)

        // Player controls 2 attack position monsters
        val mon1 = Card(id = 24, name = "Celtic Guardian", atk = 1400, def = 1200, level = 4, attribute = CardAttribute.EARTH, cardType = CardType.MONSTER)
        val mon2 = Card(id = 14, name = "Dark Magician", atk = 2500, def = 2100, level = 7, attribute = CardAttribute.DARK, cardType = CardType.MONSTER)
        engine.player.monsterZones[0] = DuelCard(card = mon1, position = CardPosition.FACE_UP_ATTACK)
        engine.player.monsterZones[1] = DuelCard(card = mon2, position = CardPosition.FACE_UP_ATTACK)

        // Advance to Battle Phase
        while (engine.currentPhase != DuelPhase.BATTLE_PHASE) {
            engine.nextPhase()
        }

        // Declare attack with monster 0
        val attacked = engine.declareAttack(attackerSlot = 0, targetSlot = null)
        assertTrue(attacked)

        // Mirror Force should trigger and destroy all attack position monsters on player's field!
        assertNull(engine.player.monsterZones[0])
        assertNull(engine.player.monsterZones[1])
        assertNull(engine.opponent.spellTrapZones[0])
        assertEquals(2, engine.player.graveyard.size)
        assertEquals(1, engine.opponent.graveyard.size)
    }

    @Test
    fun testDdsStarterDeckAndCapacity() {
        val starterDeck = DdsCampaignManager.getCanonicalStarterDeck()
        assertEquals("Canonical DDS Starter Deck must contain exactly 40 cards", 40, starterDeck.size)
        assertTrue("Starter deck cost sum must be within 400 DC", starterDeck.sumOf { it.deckCost } <= 400)
        for (card in starterDeck) {
            assertTrue("Every starter card cost must be <= starting level 15: ${card.name} (${card.deckCost})", card.deckCost <= 15)
        }
    }

    @Test
    fun testDdsDuelistLevelCalculation() {
        DdsCampaignManager.deckCapacity = 400
        assertEquals(15, DdsCampaignManager.duelistLevel)

        DdsCampaignManager.deckCapacity = 430
        assertEquals(25, DdsCampaignManager.duelistLevel)

        DdsCampaignManager.deckCapacity = 1120
        assertEquals(255, DdsCampaignManager.duelistLevel)
    }

    @Test
    fun testDdsOnTheFieldFusion() {
        val dragon = Card(id = 122, name = "Yamatano Dragon Scroll", atk = 900, def = 300, level = 2, attribute = CardAttribute.WIND, cardType = CardType.MONSTER, monsterType = MonsterType.DRAGON)
        val thunder = Card(id = 211, name = "Kaminarikozou", atk = 700, def = 600, level = 2, attribute = CardAttribute.THUNDER, cardType = CardType.MONSTER, monsterType = MonsterType.THUNDER)

        val fused = DdsCampaignManager.checkFusion(dragon, thunder)
        assertNotNull("Dragon + Thunder must fuse", fused)
        assertEquals("Twin-Headed Thunder Dragon", fused?.name)
        assertEquals(2800, fused?.atk)
    }

    @Test
    fun testDdsHandLimitRule() {
        // duelist starts with 5 cards in hand
        assertEquals(5, engine.player.hand.size)
        val drawn = engine.drawCard(isPlayer = true)
        assertNull("Cannot draw when hand already contains 5 cards in DDS rules", drawn)
        assertEquals(5, engine.player.hand.size)
    }

    @Test
    fun testAiTurnStepPlanningAndExecution() {
        engine.passTurn(autoRunAi = false)
        assertFalse(engine.isPlayerTurn)

        val steps = engine.planAiTurnSteps()
        assertTrue("AI should plan steps", steps.isNotEmpty())

        for (step in steps) {
            val executed = engine.executeAiStep(step)
            assertTrue("Step execution should succeed or be valid", executed || engine.winner != null)
        }

        assertTrue("Turn must return to player", engine.isPlayerTurn)
    }

    @Test
    fun testTristanSummonsMonsterDuringAiTurn() {
        engine.passTurn(autoRunAi = false)
        assertFalse(engine.isPlayerTurn)
        assertEquals(0, engine.opponent.activeMonstersCount())

        val steps = engine.planAiTurnSteps()
        assertTrue("AI must plan at least one SummonMonster step", steps.any { it is AiTurnStep.SummonMonster })

        val summonStep = steps.first { it is AiTurnStep.SummonMonster }
        val executed = engine.executeAiStep(summonStep)
        assertTrue("Summon execution must succeed", executed)
        assertEquals(1, engine.opponent.activeMonstersCount())
    }
}
