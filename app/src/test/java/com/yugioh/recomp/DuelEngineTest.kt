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
    fun testTributeSummonRequirements() {
        // High level monster (Level 7) requires 2 tributes
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

        // Cannot summon without tributes
        val (canSummonNoTributes, _) = engine.canSummon(isPlayer = true, card = highLevelMonster)
        assertFalse(canSummonNoTributes)

        // Put 2 tributes on the field
        val tribute1 = Card(id = 2, name = "Elf 1", atk = 800, def = 2000, level = 4, attribute = CardAttribute.LIGHT, cardType = CardType.MONSTER)
        val tribute2 = Card(id = 3, name = "Giant 2", atk = 1200, def = 1000, level = 4, attribute = CardAttribute.EARTH, cardType = CardType.MONSTER)
        engine.player.monsterZones[1] = DuelCard(card = tribute1)
        engine.player.monsterZones[2] = DuelCard(card = tribute2)

        val (canSummonWithTributes, _) = engine.canSummon(isPlayer = true, card = highLevelMonster)
        assertTrue(canSummonWithTributes)

        val success = engine.summonMonster(
            isPlayer = true,
            handIndex = 0,
            slotIndex = 0,
            tributeSlots = listOf(1, 2),
            setDefense = false
        )
        assertTrue(success)
        assertEquals("Dark Magician", engine.player.monsterZones[0]?.card?.name)
        assertNull(engine.player.monsterZones[1])
        assertNull(engine.player.monsterZones[2])
        assertEquals(2, engine.player.graveyard.size)
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
        // Pot of Greed
        val potOfGreed = Card(id = 150, name = "Pot of Greed", atk = 0, def = 0, level = 0, attribute = CardAttribute.SPELL, cardType = CardType.SPELL)
        engine.player.hand.add(0, DuelCard(card = potOfGreed))
        val handCountBefore = engine.player.hand.size

        engine.activateSpell(isPlayer = true, handIndex = 0)
        // Hand should have -1 for Pot of Greed + 2 drawn = handCountBefore + 1
        assertEquals(handCountBefore + 1, engine.player.hand.size)

        // Dian Keto the Cure Maiden
        val dianKeto = Card(id = 153, name = "Dian Keto the Cure Maiden", atk = 0, def = 0, level = 0, attribute = CardAttribute.SPELL, cardType = CardType.SPELL)
        engine.player.hand.add(0, DuelCard(card = dianKeto))
        val lpBefore = engine.player.lifePoints
        engine.activateSpell(isPlayer = true, handIndex = 0)
        assertEquals(lpBefore + 1000, engine.player.lifePoints)
    }
}
