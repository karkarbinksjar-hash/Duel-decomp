package com.yugioh.recomp

import com.yugioh.recomp.core.engine.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DuelEngineTest {

    private lateinit var engine: DuelEngine

    @Before
    fun setUp() {
        com.yugioh.recomp.core.rom.RomAssetCache.reset()
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
        assertEquals(144, jinzoTexture.width)
        assertEquals(208, jinzoTexture.height)
    }

    @Test
    fun testGbaTributeSummonRequirements() {
        val synthGba = com.yugioh.recomp.core.rom.RomValidator.createSyntheticGbaRom()
        com.yugioh.recomp.core.rom.RomAssetCache.populateFromRom(
            com.yugioh.recomp.core.rom.RomTarget.ETERNAL_DUELIST_SOUL_USA,
            synthGba
        )
        val gbaEngine = DuelEngine()
        gbaEngine.startDuel("Joey Wheeler")
        assertTrue(gbaEngine.isGbaMode)

        val dm = GbaCardCatalog.BUSTER_BLADER // Level 7: requires 2 tributes
        gbaEngine.player.hand.add(0, DuelCard(card = dm))

        // No monsters on field -> Cannot summon
        val (canSummonNoTrib, reason) = gbaEngine.canSummon(isPlayer = true, card = dm)
        assertFalse(canSummonNoTrib)
        assertTrue(reason.contains("Requires 2 Tribute"))

        // Put 1 monster on field -> Still cannot summon
        val tribute1 = GbaCardCatalog.GEMINI_ELF
        gbaEngine.player.monsterZones[0] = DuelCard(card = tribute1)
        val (canSummon1Trib, _) = gbaEngine.canSummon(isPlayer = true, card = dm)
        assertFalse(canSummon1Trib)

        // Put 2nd monster on field -> Can tribute summon!
        val tribute2 = GbaCardCatalog.VORSE_RAIDER
        gbaEngine.player.monsterZones[1] = DuelCard(card = tribute2)
        val (canSummon2Trib, _) = gbaEngine.canSummon(isPlayer = true, card = dm)
        assertTrue(canSummon2Trib)

        val success = gbaEngine.summonMonster(
            isPlayer = true,
            handIndex = 0,
            slotIndex = 0,
            tributeSlots = listOf(0, 1)
        )
        assertTrue(success)
        assertEquals("Buster Blader", gbaEngine.player.monsterZones[0]?.card?.name)
        assertNull(gbaEngine.player.monsterZones[1])
        assertEquals(2, gbaEngine.player.graveyard.size)
    }

    @Test
    fun testGbaJinzoTrapNegation() {
        val synthGba = com.yugioh.recomp.core.rom.RomValidator.createSyntheticGbaRom()
        com.yugioh.recomp.core.rom.RomAssetCache.populateFromRom(
            com.yugioh.recomp.core.rom.RomTarget.ETERNAL_DUELIST_SOUL_USA,
            synthGba
        )
        val gbaEngine = DuelEngine()
        gbaEngine.startDuel("Joey Wheeler")

        // Player controls face-up Jinzo
        val jinzo = GbaCardCatalog.JINZO
        gbaEngine.player.monsterZones[0] = DuelCard(card = jinzo, position = CardPosition.FACE_UP_ATTACK)

        // Opponent has set Mirror Force
        val mirrorForce = DdsCardCatalog.MIRROR_FORCE
        gbaEngine.opponent.spellTrapZones[0] = DuelCard(card = mirrorForce, position = CardPosition.SET_SPELL_TRAP)

        // Advance to Battle Phase
        while (gbaEngine.currentPhase != DuelPhase.BATTLE_PHASE) {
            gbaEngine.nextPhase()
        }

        val attacked = gbaEngine.declareAttack(attackerSlot = 0, targetSlot = null)
        assertTrue(attacked)

        // Jinzo negates Mirror Force! Jinzo must NOT be destroyed!
        assertNotNull(gbaEngine.player.monsterZones[0])
        assertEquals("Jinzo", gbaEngine.player.monsterZones[0]?.card?.name)
        assertEquals(5600, gbaEngine.opponent.lifePoints) // 8000 - 2400 direct attack
    }

    @Test
    fun testGbaMagicCylinderReflection() {
        val synthGba = com.yugioh.recomp.core.rom.RomValidator.createSyntheticGbaRom()
        com.yugioh.recomp.core.rom.RomAssetCache.populateFromRom(
            com.yugioh.recomp.core.rom.RomTarget.ETERNAL_DUELIST_SOUL_USA,
            synthGba
        )
        val gbaEngine = DuelEngine()
        gbaEngine.startDuel("Joey Wheeler")

        // Player controls Gemini Elf (1900 ATK)
        val gemini = GbaCardCatalog.GEMINI_ELF
        gbaEngine.player.monsterZones[0] = DuelCard(card = gemini, position = CardPosition.FACE_UP_ATTACK)

        // Opponent has set Magic Cylinder
        val cylinder = GbaCardCatalog.MAGIC_CYLINDER
        gbaEngine.opponent.spellTrapZones[0] = DuelCard(card = cylinder, position = CardPosition.SET_SPELL_TRAP)

        while (gbaEngine.currentPhase != DuelPhase.BATTLE_PHASE) {
            gbaEngine.nextPhase()
        }

        val attacked = gbaEngine.declareAttack(attackerSlot = 0, targetSlot = null)
        assertTrue(attacked)

        // Magic Cylinder negates attack and deals 1900 damage to player!
        assertEquals(8000 - 1900, gbaEngine.player.lifePoints)
        assertEquals(8000, gbaEngine.opponent.lifePoints)
    }

    @Test
    fun testGbaCampaignManagerStarterDeck() {
        GbaCampaignManager.resetToNewGame()
        assertEquals(40, GbaCampaignManager.activeDeck.size)
        // Authentic EDS Black Starter Deck features Armored Lizard!
        assertTrue(GbaCampaignManager.activeDeck.any { it.name == "Armored Lizard" })
        assertTrue(GbaCampaignManager.activeDeck.any { it.name == "Summoned Skull" })
        assertTrue(GbaCampaignManager.activeDeck.any { it.name == "Sangan" })
        assertTrue(GbaCampaignManager.activeDeck.any { it.name == "Raigeki" })
        assertTrue(GbaCampaignManager.opponents.any { it.name == "Joey Wheeler" })
        assertTrue(GbaCampaignManager.opponents.any { it.name == "Seto Kaiba" })
    }

    @Test
    fun testEdsStartingDecksSelection() {
        GbaCampaignManager.selectStarterDeck(StarterDeckType.BLACK)
        assertEquals(40, GbaCampaignManager.activeDeck.size)
        assertTrue(GbaCampaignManager.activeDeck.any { it.name == "Armored Lizard" })
        assertEquals("15480588", GbaCampaignManager.activeDeck.first { it.name == "Armored Lizard" }.password)

        GbaCampaignManager.selectStarterDeck(StarterDeckType.RED)
        assertEquals(40, GbaCampaignManager.activeDeck.size)
        assertTrue(GbaCampaignManager.activeDeck.any { it.name == "Axe Raider" })
        assertTrue(GbaCampaignManager.activeDeck.any { it.name == "7 Colored Fish" })

        GbaCampaignManager.selectStarterDeck(StarterDeckType.GREEN)
        assertEquals(40, GbaCampaignManager.activeDeck.size)
        assertTrue(GbaCampaignManager.activeDeck.any { it.name == "Judge Man" })
        assertTrue(GbaCampaignManager.activeDeck.any { it.name == "Armored Lizard" })
    }

    @Test
    fun testEdsFiveTierOpponentProgression() {
        GbaCampaignManager.resetToNewGame()
        // Tier 1 must be unlocked by default
        assertTrue(GbaCampaignManager.isTierUnlocked(1))
        assertFalse(GbaCampaignManager.isTierUnlocked(2))

        // Check opponents across all 5 tiers
        val tier1 = GbaCampaignManager.opponents.filter { it.tier == 1 }
        val tier2 = GbaCampaignManager.opponents.filter { it.tier == 2 }
        val tier3 = GbaCampaignManager.opponents.filter { it.tier == 3 }
        val tier4 = GbaCampaignManager.opponents.filter { it.tier == 4 }
        val tier5 = GbaCampaignManager.opponents.filter { it.tier == 5 }

        assertEquals(5, tier1.size)
        assertEquals(5, tier2.size)
        assertEquals(5, tier3.size)
        assertEquals(5, tier4.size)
        assertEquals(4, tier5.size)

        // Clear Tier 1 (2 wins each)
        tier1.forEach { GbaCampaignManager.opponentWins[it.name] = 2 }
        assertTrue(GbaCampaignManager.isTierUnlocked(2))
    }

    @Test
    fun testCardArtworkLoaderPasscodeResolution() {
        val lizard = GbaCardCatalog.ARMORED_LIZARD
        val passcode = com.yugioh.recomp.core.rom.CardArtworkLoader.getPasscode(lizard)
        assertEquals("15480588", passcode)

        val skull = GbaCardCatalog.SUMMONED_SKULL_GBA
        val skullPasscode = com.yugioh.recomp.core.rom.CardArtworkLoader.getPasscode(skull)
        assertEquals("70781052", skullPasscode)

        val petit = GbaCardCatalog.PETIT_DRAGON
        val petitPasscode = com.yugioh.recomp.core.rom.CardArtworkLoader.getPasscode(petit)
        assertEquals("75356564", petitPasscode)

        val coh = GbaCardCatalog.CALL_OF_THE_HAUNTED
        val cohPasscode = com.yugioh.recomp.core.rom.CardArtworkLoader.getPasscode(coh)
        assertEquals("97077563", cohPasscode)

        val battleOx = GbaCardCatalog.BATTLE_OX_GBA
        val battleOxPasscode = com.yugioh.recomp.core.rom.CardArtworkLoader.getPasscode(battleOx)
        assertNotNull(battleOxPasscode)
        assertTrue(battleOxPasscode!!.endsWith("5053103"))
    }

    @Test
    fun testAiAttacksPlayerMonsterWithLowerAtk() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        // Player controls Celtic Guardian (1400 ATK) in Attack position
        val celtic = Card(
            id = 24,
            name = "Celtic Guardian",
            atk = 1400,
            def = 1200,
            level = 4,
            attribute = CardAttribute.EARTH,
            cardType = CardType.MONSTER,
            monsterType = MonsterType.WARRIOR
        )
        testEngine.player.monsterZones[0] = DuelCard(card = celtic, position = CardPosition.FACE_UP_ATTACK)

        // Clear opponent hand to isolate battle calculation from random burn spells
        testEngine.opponent.hand.clear()

        // Opponent controls Armored Lizard (1500 ATK) in Attack position
        val lizard = GbaCardCatalog.ARMORED_LIZARD
        testEngine.opponent.monsterZones[0] = DuelCard(card = lizard, position = CardPosition.FACE_UP_ATTACK)

        // Pass turn to opponent
        testEngine.passTurn(autoRunAi = false)
        assertFalse(testEngine.isPlayerTurn)

        // Plan AI turn
        val steps = testEngine.planAiTurnSteps()

        // Verify that an attack step was planned targeting Celtic Guardian!
        val attackStep = steps.filterIsInstance<AiTurnStep.DeclareAttack>().firstOrNull()
        assertNotNull("AI must plan an attack step when it controls a monster with higher ATK!", attackStep)
        assertEquals(0, attackStep!!.attackerSlot)
        assertEquals(0, attackStep.defenderSlot)
        assertEquals("Armored Lizard", attackStep.attackerCard.name)

        // Execute all steps
        for (step in steps) {
            testEngine.executeAiStep(step)
        }

        // Celtic Guardian must be destroyed and sent to Graveyard!
        assertNull("Player monster must be destroyed!", testEngine.player.monsterZones[0])
        assertTrue("Celtic Guardian must be in player graveyard!", testEngine.player.graveyard.any { it.name == "Celtic Guardian" })

        // Player must take 100 battle damage (1500 - 1400 = 100)
        assertEquals(7900, testEngine.player.lifePoints)
    }

    @Test
    fun testAiAttacksWithNewlySummonedMonster() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        // Opponent field is empty, but has Armored Lizard (1500 ATK) in hand
        testEngine.opponent.monsterZones[0] = null
        testEngine.opponent.hand.clear()
        testEngine.opponent.hand.add(DuelCard(card = GbaCardCatalog.ARMORED_LIZARD))

        // Player controls weak monster (1000 ATK)
        val weakMonster = Card(
            id = 99,
            name = "Kuriboh",
            atk = 300,
            def = 200,
            level = 1,
            attribute = CardAttribute.DARK,
            cardType = CardType.MONSTER,
            monsterType = MonsterType.FIEND
        )
        testEngine.player.monsterZones[0] = DuelCard(card = weakMonster, position = CardPosition.FACE_UP_ATTACK)

        // Pass turn to opponent
        testEngine.passTurn(autoRunAi = false)
        assertFalse(testEngine.isPlayerTurn)

        // Plan AI steps
        val steps = testEngine.planAiTurnSteps()

        // Must include summon AND attack
        assertTrue("AI must plan summon", steps.any { it is AiTurnStep.SummonMonster })
        val attack = steps.filterIsInstance<AiTurnStep.DeclareAttack>().firstOrNull()
        assertNotNull("AI must plan attack with newly summoned monster!", attack)

        // Execute steps
        for (step in steps) {
            testEngine.executeAiStep(step)
        }

        // Verify player monster was destroyed and damage was dealt
        assertNull(testEngine.player.monsterZones[0])
        assertEquals(8000 - (1500 - 300), testEngine.player.lifePoints)
    }

    @Test
    fun testAiDirectAttackWhenPlayerFieldEmpty() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        // Player has 0 monsters
        for (i in 0 until 5) testEngine.player.monsterZones[i] = null

        // Clear opponent hand to isolate battle calculation from random burn spells
        testEngine.opponent.hand.clear()

        // Opponent controls Armored Lizard (1500 ATK)
        testEngine.opponent.monsterZones[0] = DuelCard(card = GbaCardCatalog.ARMORED_LIZARD, position = CardPosition.FACE_UP_ATTACK)

        testEngine.passTurn(autoRunAi = false)
        assertFalse(testEngine.isPlayerTurn)

        val steps = testEngine.planAiTurnSteps()
        val directAttack = steps.filterIsInstance<AiTurnStep.DeclareAttack>().firstOrNull()
        assertNotNull("AI must plan direct attack when player field has 0 monsters!", directAttack)
        assertTrue(directAttack!!.isDirect)

        for (step in steps) {
            testEngine.executeAiStep(step)
        }

        assertEquals(8000 - 1500, testEngine.player.lifePoints)
    }

    @Test
    fun testAttackedFaceDownMonsterFlipsToFaceUpDefenseAndSurvives() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        // Player sets Giant Soldier of Stone (1300 ATK / 2000 DEF) in Face-Down Defense
        val stoneGiant = Card(
            id = 55,
            name = "Giant Soldier of Stone",
            atk = 1300,
            def = 2000,
            level = 3,
            attribute = CardAttribute.EARTH,
            cardType = CardType.MONSTER,
            monsterType = MonsterType.ROCK
        )
        testEngine.player.monsterZones[0] = DuelCard(card = stoneGiant, position = CardPosition.FACE_DOWN_DEFENSE)

        // Opponent controls Armored Lizard (1500 ATK) in Attack position
        testEngine.opponent.monsterZones[0] = DuelCard(card = GbaCardCatalog.ARMORED_LIZARD, position = CardPosition.FACE_UP_ATTACK)

        // Pass turn to opponent and enter Battle Phase
        testEngine.passTurn(autoRunAi = false)
        while (testEngine.currentPhase != DuelPhase.BATTLE_PHASE) {
            testEngine.nextPhase()
        }

        // Opponent attacks player's face-down monster
        val attackSuccess = testEngine.declareAttack(attackerSlot = 0, targetSlot = 0)
        assertTrue("Attack declaration must succeed", attackSuccess)

        // Monster must be flipped to FACE_UP_DEFENSE and survive!
        val survivingMonster = testEngine.player.monsterZones[0]
        assertNotNull("Giant Soldier of Stone must survive because DEF (2000) > ATK (1500)!", survivingMonster)
        assertEquals("Must be flipped to Face-Up Defense Position", CardPosition.FACE_UP_DEFENSE, survivingMonster!!.position)
        assertEquals(2000, survivingMonster.card.def)

        // Opponent must have taken 500 reflective battle damage (2000 DEF - 1500 ATK = 500)
        assertEquals(7500, testEngine.opponent.lifePoints)

        // Clash summary must indicate flipped face-up and defense held
        assertNotNull(testEngine.clashState)
        assertTrue("Summary should report flipped face-up", testEngine.clashState!!.resultSummary.contains("Flipped face-up"))
        assertTrue("Summary should report defense held", testEngine.clashState!!.resultSummary.contains("holds defense"))
    }

    @Test
    fun testAiDoesNotRepeatedlySuicideIntoFaceUpDefenseWall() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        // Player controls Giant Soldier of Stone (2000 DEF) already face-up in defense
        val stoneGiant = Card(
            id = 55,
            name = "Giant Soldier of Stone",
            atk = 1300,
            def = 2000,
            level = 3,
            attribute = CardAttribute.EARTH,
            cardType = CardType.MONSTER,
            monsterType = MonsterType.ROCK
        )
        testEngine.player.monsterZones[0] = DuelCard(card = stoneGiant, position = CardPosition.FACE_UP_DEFENSE)

        // Opponent has empty hand and controls Celtic Guardian (1400 ATK) and Armored Lizard (1500 ATK)
        testEngine.opponent.hand.clear()
        testEngine.opponent.monsterZones[0] = DuelCard(card = GbaCardCatalog.ARMORED_LIZARD, position = CardPosition.FACE_UP_ATTACK)
        testEngine.opponent.monsterZones[1] = DuelCard(
            card = Card(id = 24, name = "Celtic Guardian", atk = 1400, def = 1200, level = 4, attribute = CardAttribute.EARTH, cardType = CardType.MONSTER, monsterType = MonsterType.WARRIOR),
            position = CardPosition.FACE_UP_ATTACK
        )

        testEngine.passTurn(autoRunAi = false)
        assertFalse(testEngine.isPlayerTurn)

        // Plan AI turn
        val steps = testEngine.planAiTurnSteps()

        // AI must NOT plan any attack against the 2000 DEF face-up wall!
        val attackSteps = steps.filterIsInstance<AiTurnStep.DeclareAttack>()
        assertTrue("AI must not suicide attack into a known face-up defense wall with higher DEF!", attackSteps.isEmpty())
    }

    @Test
    fun testAiDoesNotRepeatedlyAttackRevealedWallOnSameTurn() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        // Player has 1 face-down monster with 2000 DEF
        val stoneGiant = Card(
            id = 55,
            name = "Giant Soldier of Stone",
            atk = 1300,
            def = 2000,
            level = 3,
            attribute = CardAttribute.EARTH,
            cardType = CardType.MONSTER,
            monsterType = MonsterType.ROCK
        )
        testEngine.player.monsterZones[0] = DuelCard(card = stoneGiant, position = CardPosition.FACE_DOWN_DEFENSE)

        // Opponent has two monsters: Monster 1 (1600 ATK) and Monster 2 (1500 ATK)
        testEngine.opponent.hand.clear()
        testEngine.opponent.monsterZones[0] = DuelCard(
            card = Card(id = 301, name = "Cyber-Tech Alligator", atk = 1600, def = 1400, level = 4, attribute = CardAttribute.WIND, cardType = CardType.MONSTER, monsterType = MonsterType.MACHINE),
            position = CardPosition.FACE_UP_ATTACK
        )
        testEngine.opponent.monsterZones[1] = DuelCard(card = GbaCardCatalog.ARMORED_LIZARD, position = CardPosition.FACE_UP_ATTACK)

        testEngine.passTurn(autoRunAi = false)
        val steps = testEngine.planAiTurnSteps()

        // Monster 1 probes the face-down card, but once revealed, Monster 2 must NOT attack the 2000 DEF wall!
        val attackSteps = steps.filterIsInstance<AiTurnStep.DeclareAttack>()
        assertEquals("Only one probe attack should occur on the face-down card; second monster must not suicide into revealed wall!", 1, attackSteps.size)
    }

    @Test
    fun testFlipEffectManEaterBugDestroysAttackerWhenFlipped() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        // Player sets Man-Eater Bug (450 ATK / 600 DEF) face-down
        testEngine.player.monsterZones[0] = DuelCard(card = GbaCardCatalog.MAN_EATER_BUG, position = CardPosition.FACE_DOWN_DEFENSE)

        // Opponent controls Blue-Eyes White Dragon (3000 ATK)
        testEngine.opponent.monsterZones[0] = DuelCard(
            card = Card(id = 1, name = "Blue-Eyes White Dragon", atk = 3000, def = 2500, level = 8, attribute = CardAttribute.LIGHT, cardType = CardType.MONSTER, monsterType = MonsterType.DRAGON),
            position = CardPosition.FACE_UP_ATTACK
        )

        testEngine.passTurn(autoRunAi = false)
        while (testEngine.currentPhase != DuelPhase.BATTLE_PHASE) {
            testEngine.nextPhase()
        }

        // Opponent attacks Man-Eater Bug
        testEngine.declareAttack(attackerSlot = 0, targetSlot = 0)

        // Man-Eater Bug is destroyed by battle (600 < 3000)
        assertNull("Man-Eater Bug destroyed by battle", testEngine.player.monsterZones[0])
        assertTrue("Man-Eater Bug in player graveyard", testEngine.player.graveyard.any { it.name == "Man-Eater Bug" })

        // FLIP EFFECT: Blue-Eyes White Dragon must also be destroyed and in opponent graveyard!
        assertNull("Attacker must be destroyed by Man-Eater Bug's flip effect!", testEngine.opponent.monsterZones[0])
        assertTrue("Blue-Eyes must be in opponent graveyard", testEngine.opponent.graveyard.any { it.name == "Blue-Eyes White Dragon" })
    }

    @Test
    fun testTristanTaylorRookieDeckHasNoVorseRaiderAndFairStats() {
        val tristanDeck = GbaCampaignManager.buildOpponentDeck("Tristan Taylor")
        assertEquals("Tristan's deck must contain exactly 40 cards", 40, tristanDeck.size)
        assertFalse("Tristan must NEVER have Vorse Raider in his deck!", tristanDeck.any { it.name == "Vorse Raider" })
        assertFalse("Tristan must NEVER have Gemini Elf in his deck!", tristanDeck.any { it.name == "Gemini Elf" })
        assertFalse("Tristan must NEVER have La Jinn in his deck!", tristanDeck.any { it.name.startsWith("La Jinn") })

        val highAtkMonsters = tristanDeck.filter { it.isMonster && it.atk > 1700 }
        assertTrue("Tristan should not have any monster with ATK > 1700", highAtkMonsters.isEmpty())

        val battleOxCount = tristanDeck.count { it.name == "Battle Ox" }
        assertEquals("Tristan should have at most 1 signature Battle Ox", 1, battleOxCount)
    }

    @Test
    fun testAttackedMonsterCannotAttackAgainInSameTurn() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Tristan Taylor")

        // Put a monster on player field
        val monsterCard = Card(id = 10, name = "Armored Lizard", atk = 1500, def = 1200, level = 4, attribute = CardAttribute.EARTH, cardType = CardType.MONSTER, monsterType = MonsterType.REPTILE)
        testEngine.player.monsterZones[0] = DuelCard(card = monsterCard, position = CardPosition.FACE_UP_ATTACK)

        // Clear opponent field
        for (i in 0 until 5) testEngine.opponent.monsterZones[i] = null

        // Advance to Battle Phase
        while (testEngine.currentPhase != DuelPhase.BATTLE_PHASE) {
            testEngine.nextPhase()
        }

        assertTrue("Armored Lizard should be eligible to attack", testEngine.canMonsterAttack(isPlayer = true, slotIndex = 0))

        // First direct attack: should succeed
        val attack1Success = testEngine.declareAttack(attackerSlot = 0, targetSlot = null)
        assertTrue("First direct attack should succeed", attack1Success)
        assertEquals("Opponent should take 1500 damage (8000 -> 6500)", 6500, testEngine.opponent.lifePoints)

        // Monster should now be marked as attacked
        assertFalse("Monster should no longer be eligible to attack", testEngine.canMonsterAttack(isPlayer = true, slotIndex = 0))
        assertTrue("hasAttackedThisTurn must be true", testEngine.player.monsterZones[0]!!.hasAttackedThisTurn)
        assertFalse("canDeclareAttack must be false", testEngine.player.monsterZones[0]!!.canDeclareAttack)

        // Second direct attack attempt by same monster: MUST BE REJECTED!
        val attack2Success = testEngine.declareAttack(attackerSlot = 0, targetSlot = null)
        assertFalse("Second attack by already-attacked monster must be rejected!", attack2Success)
        assertEquals("Opponent LP must remain 6500 without reduction", 6500, testEngine.opponent.lifePoints)
    }

    @Test
    fun testMultiAttackMonsterHayabusaKnightCanAttackTwice() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Tristan Taylor")

        val hayabusa = Card(id = 88, name = "Hayabusa Knight", atk = 1000, def = 700, level = 3, attribute = CardAttribute.EARTH, cardType = CardType.MONSTER, monsterType = MonsterType.WARRIOR)
        testEngine.player.monsterZones[0] = DuelCard(card = hayabusa, position = CardPosition.FACE_UP_ATTACK)

        for (i in 0 until 5) testEngine.opponent.monsterZones[i] = null

        while (testEngine.currentPhase != DuelPhase.BATTLE_PHASE) {
            testEngine.nextPhase()
        }

        assertTrue("Hayabusa can attack first time", testEngine.canMonsterAttack(isPlayer = true, slotIndex = 0))
        assertTrue("First attack succeeds", testEngine.declareAttack(0, null))
        assertEquals(7000, testEngine.opponent.lifePoints)

        // Hayabusa has attacked once, but has maxAttacksPerTurn = 2
        assertTrue("Hayabusa can attack second time", testEngine.canMonsterAttack(isPlayer = true, slotIndex = 0))
        assertTrue("Second attack succeeds", testEngine.declareAttack(0, null))
        assertEquals(6000, testEngine.opponent.lifePoints)

        // Third attack should be rejected
        assertFalse("Hayabusa cannot attack third time", testEngine.canMonsterAttack(isPlayer = true, slotIndex = 0))
        assertFalse("Third attack rejected", testEngine.declareAttack(0, null))
        assertEquals(6000, testEngine.opponent.lifePoints)
    }

    @Test
    fun testTier1DecksAuthenticCardCounts() {
        listOf("Tristan Taylor", "Tea Gardner", "Yugi Muto", "Joey Wheeler", "Bakura Ryou").forEach { duelist ->
            val deck = GbaCampaignManager.buildOpponentDeck(duelist)
            assertEquals("$duelist must have a 40 card deck", 40, deck.size)
            assertFalse("$duelist must not have Vorse Raider", deck.any { it.name == "Vorse Raider" })
            assertFalse("$duelist must not have Gemini Elf", deck.any { it.name == "Gemini Elf" })
        }
    }

    @Test
    fun testOpponentSetTrapAndDefenseMonsterAreFacedown() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")
        testEngine.passTurn(autoRunAi = false)
        testEngine.executeAiStep(AiTurnStep.PhaseTransition(DuelPhase.MAIN_PHASE_1, "Main Phase 1"))

        val trapCard = Card(id = 99, name = "Mirror Force", atk = 0, def = 0, level = 0, attribute = CardAttribute.TRAP, cardType = CardType.TRAP, monsterType = MonsterType.NONE)
        testEngine.opponent.hand.add(DuelCard(card = trapCard))
        testEngine.executeAiStep(AiTurnStep.SetTrap(trapCard, 0, "Joey sets a card"))

        val setTrap = testEngine.opponent.spellTrapZones[0]
        assertNotNull("Opponent trap should be on the field", setTrap)
        assertTrue("Opponent set trap position must be isFaceDown", setTrap!!.position.isFaceDown)

        val monsterCard = Card(id = 100, name = "Man-Eater Bug", atk = 450, def = 600, level = 2, attribute = CardAttribute.EARTH, cardType = CardType.MONSTER, monsterType = MonsterType.INSECT)
        testEngine.opponent.hand.add(DuelCard(card = monsterCard))
        testEngine.executeAiStep(AiTurnStep.SummonMonster(monsterCard, 0, isDefense = true, announcement = "Joey sets a monster"))

        val setMonster = testEngine.opponent.monsterZones[0]
        assertNotNull("Opponent set monster should be on the field", setMonster)
        assertTrue("Opponent set defense monster must be isFaceDown", setMonster!!.position.isFaceDown)
    }

    @Test
    fun testDuelSpeedSettingsHierarchy() {
        assertEquals("Slow speed multiplier should be 1.5", 1.5f, DuelSpeed.SLOW.multiplier, 0.01f)
        assertEquals("Normal speed multiplier should be 1.0", 1.0f, DuelSpeed.NORMAL.multiplier, 0.01f)
        assertEquals("Fast speed multiplier should be 0.57", 0.57f, DuelSpeed.FAST.multiplier, 0.01f)
        assertEquals("Turbo speed multiplier should be 0.33", 0.33f, DuelSpeed.TURBO.multiplier, 0.01f)
    }

    @Test
    fun testFlipEffectManEaterBugDestroysAttackerInBattle() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        // Player controls Vorse Raider in Attack position
        val vorseRaider = Card(id = 10, name = "Vorse Raider", atk = 1900, def = 1200, level = 4, attribute = CardAttribute.DARK, cardType = CardType.MONSTER, monsterType = MonsterType.BEAST_WARRIOR)
        testEngine.player.monsterZones[0] = DuelCard(card = vorseRaider, position = CardPosition.FACE_UP_ATTACK)

        // Opponent controls Man-Eater Bug in face-down defense position
        val manEaterBug = Card(id = 11, name = "Man-Eater Bug", atk = 450, def = 600, level = 2, attribute = CardAttribute.EARTH, cardType = CardType.MONSTER, monsterType = MonsterType.INSECT)
        testEngine.opponent.monsterZones[0] = DuelCard(card = manEaterBug, position = CardPosition.FACE_DOWN_DEFENSE)

        testEngine.nextPhase() // MAIN_PHASE_1 -> BATTLE_PHASE
        assertTrue("Player declares attack on face-down monster", testEngine.declareAttack(0, 0))

        // Man-Eater Bug should destroy Vorse Raider
        assertNull("Vorse Raider should be destroyed by Man-Eater Bug flip effect", testEngine.player.monsterZones[0])
        assertTrue("Vorse Raider should be in player graveyard", testEngine.player.graveyard.any { it.name == "Vorse Raider" })
    }

    @Test
    fun testFieldSpellYamiStatModifiers() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        val darkMagician = Card(id = 1, name = "Dark Magician", atk = 2500, def = 2100, level = 7, attribute = CardAttribute.DARK, cardType = CardType.MONSTER, monsterType = MonsterType.SPELLCASTER)
        val kuriboh = Card(id = 2, name = "Kuriboh", atk = 300, def = 200, level = 1, attribute = CardAttribute.DARK, cardType = CardType.MONSTER, monsterType = MonsterType.FIEND)
        val shiningFriendship = Card(id = 3, name = "Shining Friendship", atk = 1300, def = 1100, level = 4, attribute = CardAttribute.LIGHT, cardType = CardType.MONSTER, monsterType = MonsterType.FAIRY)

        // Before field spell
        assertEquals(2500, testEngine.getEffectiveAtk(darkMagician))
        assertEquals(300, testEngine.getEffectiveAtk(kuriboh))
        assertEquals(1300, testEngine.getEffectiveAtk(shiningFriendship))

        // Activate Yami
        val yami = Card(id = 99, name = "Yami", atk = 0, def = 0, level = 0, attribute = CardAttribute.SPELL, cardType = CardType.SPELL, monsterType = MonsterType.NONE)
        testEngine.player.hand.add(DuelCard(card = yami))
        testEngine.activateSpell(isPlayer = true, handIndex = testEngine.player.hand.size - 1)

        assertEquals("Yami should be active field spell", "Yami", testEngine.activeFieldSpell?.name)
        assertEquals("Dark Magician (Spellcaster) gains 200 ATK", 2700, testEngine.getEffectiveAtk(darkMagician))
        assertEquals("Dark Magician (Spellcaster) gains 200 DEF", 2300, testEngine.getEffectiveDef(darkMagician))
        assertEquals("Kuriboh (Fiend) gains 200 ATK", 500, testEngine.getEffectiveAtk(kuriboh))
        assertEquals("Shining Friendship (Fairy) loses 200 ATK", 1100, testEngine.getEffectiveAtk(shiningFriendship))
    }

    @Test
    fun testWhiteMagicalHatDiscardsCardOnDamage() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        val hat = Card(id = 15, name = "White Magical Hat", atk = 1000, def = 700, level = 3, attribute = CardAttribute.LIGHT, cardType = CardType.MONSTER, monsterType = MonsterType.SPELLCASTER)
        testEngine.player.monsterZones[0] = DuelCard(card = hat, position = CardPosition.FACE_UP_ATTACK)

        // Opponent has empty field and 5 cards in hand
        testEngine.opponent.monsterZones.fill(null)
        val initialHandSize = testEngine.opponent.hand.size
        assertTrue(initialHandSize > 0)

        testEngine.nextPhase() // BATTLE_PHASE
        assertTrue("Direct attack succeeds", testEngine.declareAttack(0, null))

        assertEquals("Opponent hand size should decrease by 1 due to White Magical Hat effect", initialHandSize - 1, testEngine.opponent.hand.size)
        assertTrue("Opponent graveyard should contain the discarded card", testEngine.opponent.graveyard.isNotEmpty())
    }

    @Test
    fun testMagicJammerChainsAndNegatesSpell() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        // Opponent has set Magic Jammer and at least 1 card in hand to discard
        val magicJammer = Card(id = 889, name = "Magic Jammer", atk = 0, def = 0, level = 0, attribute = CardAttribute.TRAP, cardType = CardType.TRAP, monsterType = MonsterType.NONE)
        testEngine.opponent.spellTrapZones[0] = DuelCard(card = magicJammer, position = CardPosition.SET_SPELL_TRAP)
        assertTrue("Opponent must have discard material in hand", testEngine.opponent.hand.isNotEmpty())

        // Player activates Raigeki
        val raigeki = Card(id = 20, name = "Raigeki", atk = 0, def = 0, level = 0, attribute = CardAttribute.SPELL, cardType = CardType.SPELL, monsterType = MonsterType.NONE)
        testEngine.player.hand.add(DuelCard(card = raigeki))

        // Also put a monster on opponent field to verify it is NOT destroyed by Raigeki
        testEngine.opponent.monsterZones[0] = DuelCard(card = Card(id = 5, name = "Kuriboh", atk = 300, def = 200, level = 1, attribute = CardAttribute.DARK, cardType = CardType.MONSTER, monsterType = MonsterType.FIEND))

        assertTrue("Player activates spell", testEngine.activateSpell(isPlayer = true, handIndex = testEngine.player.hand.size - 1))

        // Magic Jammer should negate Raigeki!
        assertNotNull("Kuriboh should survive because Raigeki was negated by Magic Jammer", testEngine.opponent.monsterZones[0])
        assertNull("Magic Jammer should be removed from field", testEngine.opponent.spellTrapZones[0])
        assertTrue("Magic Jammer should be in opponent graveyard", testEngine.opponent.graveyard.any { it.name == "Magic Jammer" })
        assertTrue("Raigeki should be in player graveyard", testEngine.player.graveyard.any { it.name == "Raigeki" })
    }

    @Test
    fun testSevenToolsOfBanditChainsAndNegatesMirrorForce() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        // Player controls Vorse Raider and has set Seven Tools of the Bandit
        val vorseRaider = Card(id = 10, name = "Vorse Raider", atk = 1900, def = 1200, level = 4, attribute = CardAttribute.DARK, cardType = CardType.MONSTER, monsterType = MonsterType.BEAST_WARRIOR)
        testEngine.player.monsterZones[0] = DuelCard(card = vorseRaider, position = CardPosition.FACE_UP_ATTACK)

        val sevenTools = Card(id = 891, name = "Seven Tools of the Bandit", atk = 0, def = 0, level = 0, attribute = CardAttribute.TRAP, cardType = CardType.TRAP, monsterType = MonsterType.NONE)
        testEngine.player.spellTrapZones[0] = DuelCard(card = sevenTools, position = CardPosition.SET_SPELL_TRAP)
        val initialPlayerLp = testEngine.player.lifePoints

        // Opponent controls set Mirror Force and an attack target
        val mirrorForce = Card(id = 99, name = "Mirror Force", atk = 0, def = 0, level = 0, attribute = CardAttribute.TRAP, cardType = CardType.TRAP, monsterType = MonsterType.NONE)
        testEngine.opponent.spellTrapZones[0] = DuelCard(card = mirrorForce, position = CardPosition.SET_SPELL_TRAP)
        testEngine.opponent.monsterZones[0] = DuelCard(card = Card(id = 2, name = "Mystical Elf", atk = 800, def = 2000, level = 4, attribute = CardAttribute.LIGHT, cardType = CardType.MONSTER, monsterType = MonsterType.SPELLCASTER), position = CardPosition.FACE_UP_DEFENSE)

        testEngine.nextPhase() // BATTLE_PHASE
        assertTrue("Attack declared", testEngine.declareAttack(attackerSlot = 0, targetSlot = 0))

        // Seven Tools should pay 1000 LP and negate Mirror Force, so Vorse Raider survives!
        assertEquals("Player should pay 1000 LP for Seven Tools", initialPlayerLp - 1000, testEngine.player.lifePoints)
        assertNotNull("Vorse Raider should NOT be destroyed because Mirror Force was negated!", testEngine.player.monsterZones[0])
        assertTrue("Seven Tools should be in player graveyard", testEngine.player.graveyard.any { it.name == "Seven Tools of the Bandit" })
    }

    @Test
    fun testWabokuPreventsBattleDamageAndDestruction() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        // Player activates Waboku
        val waboku = Card(id = 913, name = "Waboku", atk = 0, def = 0, level = 0, attribute = CardAttribute.TRAP, cardType = CardType.TRAP, monsterType = MonsterType.NONE)
        testEngine.player.spellTrapZones[0] = DuelCard(card = waboku, position = CardPosition.SET_SPELL_TRAP)
        assertTrue("Activate set Waboku", testEngine.activateSetTrap(isPlayer = true, slotIndex = 0))
        assertTrue("Waboku should be active on player", testEngine.player.isWabokuActive)

        // Player has Kuriboh in Attack position (300 ATK)
        testEngine.player.monsterZones[0] = DuelCard(card = Card(id = 5, name = "Kuriboh", atk = 300, def = 200, level = 1, attribute = CardAttribute.DARK, cardType = CardType.MONSTER, monsterType = MonsterType.FIEND))
        val initialPlayerLp = testEngine.player.lifePoints

        // Opponent has Blue-Eyes White Dragon (3000 ATK) and attacks
        testEngine.opponent.monsterZones[0] = DuelCard(card = Card(id = 1, name = "Blue-Eyes White Dragon", atk = 3000, def = 2500, level = 8, attribute = CardAttribute.LIGHT, cardType = CardType.MONSTER, monsterType = MonsterType.DRAGON))
        testEngine.passTurn(autoRunAi = false)
        while (testEngine.currentPhase != DuelPhase.BATTLE_PHASE) {
            testEngine.nextPhase()
        }

        assertTrue("Opponent attacks Kuriboh", testEngine.declareAttack(attackerSlot = 0, targetSlot = 0))

        // Kuriboh must NOT be destroyed, and player must take 0 damage!
        assertNotNull("Kuriboh must survive due to Waboku", testEngine.player.monsterZones[0])
        assertEquals("Player Life Points should not change", initialPlayerLp, testEngine.player.lifePoints)
    }

    @Test
    fun testJustDessertsDeals500PerMonster() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        val justDesserts = Card(id = 899, name = "Just Desserts", atk = 0, def = 0, level = 0, attribute = CardAttribute.TRAP, cardType = CardType.TRAP, monsterType = MonsterType.NONE)
        testEngine.player.spellTrapZones[0] = DuelCard(card = justDesserts, position = CardPosition.SET_SPELL_TRAP)

        // Opponent has 3 monsters on field
        testEngine.opponent.monsterZones[0] = DuelCard(card = Card(id = 1, name = "Mon1", atk = 1000, def = 1000, level = 4, attribute = CardAttribute.EARTH, cardType = CardType.MONSTER, monsterType = MonsterType.WARRIOR))
        testEngine.opponent.monsterZones[1] = DuelCard(card = Card(id = 2, name = "Mon2", atk = 1000, def = 1000, level = 4, attribute = CardAttribute.EARTH, cardType = CardType.MONSTER, monsterType = MonsterType.WARRIOR))
        testEngine.opponent.monsterZones[2] = DuelCard(card = Card(id = 3, name = "Mon3", atk = 1000, def = 1000, level = 4, attribute = CardAttribute.EARTH, cardType = CardType.MONSTER, monsterType = MonsterType.WARRIOR))

        val initialOppLp = testEngine.opponent.lifePoints
        assertTrue("Activate Just Desserts", testEngine.activateSetTrap(isPlayer = true, slotIndex = 0))

        // 3 monsters * 500 = 1500 damage
        assertEquals("Opponent should take 1500 damage", initialOppLp - 1500, testEngine.opponent.lifePoints)
    }
}
