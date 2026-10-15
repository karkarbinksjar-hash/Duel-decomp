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
    fun testDdsHighLevelNormalSummon() {
        // High level monster (Level 7) does not require tributes in DDS rules
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

        // Can summon without tributes in DDS rules
        val (canSummon, reason) = engine.canSummon(isPlayer = true, card = highLevelMonster)
        assertTrue("High level monster can summon directly without tributes: $reason", canSummon)

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
    fun testGbaRomValidation() {
        val synthGba = com.yugioh.recomp.core.rom.RomValidator.createSyntheticGbaRom()
        assertTrue("Synthetic GBA ROM must be >= 4MB", synthGba.size >= 0x400000)

        val validation = com.yugioh.recomp.core.rom.RomValidator.validate(synthGba)
        assertTrue("GBA ROM should validate successfully", validation.isValid)
        assertTrue(validation is com.yugioh.recomp.core.rom.RomValidationResult.Valid)
        val valid = validation as com.yugioh.recomp.core.rom.RomValidationResult.Valid
        assertEquals(com.yugioh.recomp.core.rom.RomPlatform.GBA, valid.target.platform)
        assertEquals("YUGIOH EDS", valid.headerTitle)
    }

    @Test
    fun testGbaHeaderParsing() {
        val synthGba = com.yugioh.recomp.core.rom.RomValidator.createSyntheticGbaRom()
        val header = com.yugioh.recomp.core.rom.GbaRomParser.parseHeader(synthGba)
        assertEquals("YUGIOH EDS", header.title)
        assertEquals("AYCE", header.gameCode)
        assertEquals("01", header.makerCode)
    }

    @Test
    fun testGba4bppTileDecoding() {
        val synthGba = com.yugioh.recomp.core.rom.RomValidator.createSyntheticGbaRom()
        val palette = com.yugioh.recomp.core.rom.GbaRomParser.getGba16ColorPaletteForAttribute(CardAttribute.DARK)
        assertEquals(16, palette.size)

        val tile = com.yugioh.recomp.core.rom.GbaTileDecoder.decodeTile4bpp(synthGba, 0x10000, palette)
        assertEquals(64, tile.size) // 8x8 pixels

        val grid = com.yugioh.recomp.core.rom.GbaTileDecoder.decodeTileGrid4bpp(synthGba, 0x10000, 4, 4, palette)
        assertEquals(32, grid.width)
        assertEquals(32, grid.height)
        assertEquals(32 * 32, grid.pixels.size)
    }

    @Test
    fun testGbaCardExtractionAndAssetCache() {
        val synthGba = com.yugioh.recomp.core.rom.RomValidator.createSyntheticGbaRom()
        val cards = com.yugioh.recomp.core.rom.GbaRomParser.extractCardsFromRom(synthGba)
        assertTrue("GBA expansion must include Jinzo", cards.any { it.name == "Jinzo" && it.atk == 2400 })
        assertTrue("GBA expansion must include Buster Blader", cards.any { it.name == "Buster Blader" && it.atk == 2600 })
        assertTrue("GBA expansion must include Dark Magician Girl", cards.any { it.name == "Dark Magician Girl" })

        com.yugioh.recomp.core.rom.RomAssetCache.populateFromRom(
            com.yugioh.recomp.core.rom.RomTarget.ETERNAL_DUELIST_SOUL_USA,
            synthGba
        )
        assertTrue(com.yugioh.recomp.core.rom.RomAssetCache.isAssetPipelineReady)
        assertEquals(com.yugioh.recomp.core.rom.RomPlatform.GBA, com.yugioh.recomp.core.rom.RomAssetCache.activeTarget?.platform)
        val jinzoTexture = com.yugioh.recomp.core.rom.RomAssetCache.getCardFullTexture(801)
        assertNotNull(jinzoTexture)
        assertEquals(96, jinzoTexture.width)
        assertEquals(140, jinzoTexture.height)
    }
}
