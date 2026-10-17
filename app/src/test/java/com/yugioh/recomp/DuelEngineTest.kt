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

    @Test
    fun testTributeSummonWithSpecificSlots() {
        val testEngine = DuelEngine()
        com.yugioh.recomp.core.rom.RomAssetCache.populateFromRom(
            com.yugioh.recomp.core.rom.RomTarget.ETERNAL_DUELIST_SOUL_USA,
            ByteArray(100)
        )
        testEngine.startDuel("Joey Wheeler")

        // Player has 2 monsters: Celtic Guardian in Zone 0, Kuriboh in Zone 1
        val celticGuardian = Card(id = 24, name = "Celtic Guardian", atk = 1400, def = 1200, level = 4, attribute = CardAttribute.EARTH, cardType = CardType.MONSTER, monsterType = MonsterType.WARRIOR)
        val kuriboh = Card(id = 5, name = "Kuriboh", atk = 300, def = 200, level = 1, attribute = CardAttribute.DARK, cardType = CardType.MONSTER, monsterType = MonsterType.FIEND)
        testEngine.player.monsterZones[0] = DuelCard(card = celticGuardian, position = CardPosition.FACE_UP_ATTACK)
        testEngine.player.monsterZones[1] = DuelCard(card = kuriboh, position = CardPosition.FACE_UP_ATTACK)

        // Hand has Summoned Skull (Level 6, 1 tribute)
        val summonedSkull = Card(id = 35, name = "Summoned Skull", atk = 2500, def = 1200, level = 6, attribute = CardAttribute.DARK, cardType = CardType.MONSTER, monsterType = MonsterType.FIEND)
        testEngine.player.hand.add(DuelCard(card = summonedSkull))
        val handIdx = testEngine.player.hand.size - 1

        // Tribute Kuriboh specifically (Zone 1) and summon Summoned Skull into Zone 1
        val success = testEngine.summonMonster(
            isPlayer = true,
            handIndex = handIdx,
            slotIndex = 1,
            tributeSlots = listOf(1),
            setDefense = false
        )
        assertTrue("Tribute summon should succeed", success)
        assertNotNull("Celtic Guardian in Zone 0 should still be intact", testEngine.player.monsterZones[0])
        assertEquals("Celtic Guardian", testEngine.player.monsterZones[0]?.card?.name)
        assertNotNull("Summoned Skull should be in Zone 1", testEngine.player.monsterZones[1])
        assertEquals("Summoned Skull", testEngine.player.monsterZones[1]?.card?.name)
        assertTrue("Kuriboh should be in Graveyard", testEngine.player.graveyard.any { it.name == "Kuriboh" })
    }

    @Test
    fun testTributeSummonFailsWhenInsufficientTributes() {
        val testEngine = DuelEngine()
        com.yugioh.recomp.core.rom.RomAssetCache.populateFromRom(
            com.yugioh.recomp.core.rom.RomTarget.ETERNAL_DUELIST_SOUL_USA,
            ByteArray(100)
        )
        testEngine.startDuel("Joey Wheeler")

        // Player has only 1 monster on field
        val kuriboh = Card(id = 5, name = "Kuriboh", atk = 300, def = 200, level = 1, attribute = CardAttribute.DARK, cardType = CardType.MONSTER, monsterType = MonsterType.FIEND)
        testEngine.player.monsterZones[0] = DuelCard(card = kuriboh, position = CardPosition.FACE_UP_ATTACK)

        // Dark Magician needs 2 tributes (Level 7)
        val darkMagician = Card(id = 14, name = "Dark Magician", atk = 2500, def = 2100, level = 7, attribute = CardAttribute.DARK, cardType = CardType.MONSTER, monsterType = MonsterType.SPELLCASTER)
        testEngine.player.hand.add(DuelCard(card = darkMagician))
        val handIdx = testEngine.player.hand.size - 1

        val (canSummon, reason) = testEngine.canSummon(isPlayer = true, darkMagician)
        assertFalse("Cannot summon 2-tribute monster with only 1 monster", canSummon)
        assertTrue("Reason should mention tributes required", reason.contains("Requires 2 Tribute(s)"))

        val summoned = testEngine.summonMonster(isPlayer = true, handIndex = handIdx, slotIndex = 0)
        assertFalse("Summon should fail", summoned)
        assertNotNull("Kuriboh should not have been tributed", testEngine.player.monsterZones[0])
    }

    @Test
    fun testSequentialDirectAttacks() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        // Opponent has 0 monsters
        for (i in 0 until 5) testEngine.opponent.monsterZones[i] = null
        val initialLp = testEngine.opponent.lifePoints

        // Player has 2 monsters in Attack position
        val mon1 = Card(id = 1, name = "Mon1", atk = 1500, def = 1000, level = 4, attribute = CardAttribute.EARTH, cardType = CardType.MONSTER, monsterType = MonsterType.WARRIOR)
        val mon2 = Card(id = 2, name = "Mon2", atk = 1800, def = 1000, level = 4, attribute = CardAttribute.EARTH, cardType = CardType.MONSTER, monsterType = MonsterType.WARRIOR)
        testEngine.player.monsterZones[0] = DuelCard(card = mon1, position = CardPosition.FACE_UP_ATTACK)
        testEngine.player.monsterZones[1] = DuelCard(card = mon2, position = CardPosition.FACE_UP_ATTACK)

        testEngine.nextPhase() // BATTLE_PHASE

        // First direct attack
        assertTrue("Mon1 attacks directly", testEngine.declareAttack(attackerSlot = 0, targetSlot = null))
        assertEquals("Opponent takes 1500 damage", initialLp - 1500, testEngine.opponent.lifePoints)
        assertFalse("Mon1 cannot attack again", testEngine.player.monsterZones[0]!!.canDeclareAttack)
        assertTrue("Mon2 is still eligible to attack", testEngine.player.monsterZones[1]!!.canDeclareAttack)

        // Dismiss clash / second direct attack in same phase
        testEngine.dismissClash()
        assertTrue("Mon2 attacks directly", testEngine.declareAttack(attackerSlot = 1, targetSlot = null))
        assertEquals("Opponent takes additional 1800 damage", initialLp - 1500 - 1800, testEngine.opponent.lifePoints)
        assertFalse("Mon2 cannot attack again", testEngine.player.monsterZones[1]!!.canDeclareAttack)
    }

    @Test
    fun testTrapHoleRecordsTriggerStateAndDestroysMonster() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        // Opponent has set Trap Hole
        val trapHole = Card(id = 160, name = "Trap Hole", atk = 0, def = 0, level = 0, attribute = CardAttribute.TRAP, cardType = CardType.TRAP, monsterType = MonsterType.NONE)
        testEngine.opponent.spellTrapZones[0] = DuelCard(card = trapHole, position = CardPosition.SET_SPELL_TRAP)

        // Player summons monster with ATK >= 1000 (Vorse Raider, 1900 ATK)
        val vorseRaider = Card(id = 10, name = "Vorse Raider", atk = 1900, def = 1200, level = 4, attribute = CardAttribute.DARK, cardType = CardType.MONSTER, monsterType = MonsterType.BEAST_WARRIOR)
        testEngine.player.hand.add(DuelCard(card = vorseRaider))
        val handIdx = testEngine.player.hand.size - 1

        assertTrue("Summon executed", testEngine.summonMonster(isPlayer = true, handIndex = handIdx, slotIndex = 0, setDefense = false))

        // Monster should be destroyed by Trap Hole and trigger recorded
        assertNull("Vorse Raider should be destroyed by Trap Hole", testEngine.player.monsterZones[0])
        assertEquals("Trap Hole trigger recorded", Pair("Trap Hole", 0), testEngine.lastTriggeredTrap)
        assertTrue("Vorse Raider should be in Graveyard", testEngine.player.graveyard.any { it.name == "Vorse Raider" })
        assertTrue("Trap Hole should be in opponent Graveyard", testEngine.opponent.graveyard.any { it.name == "Trap Hole" })
    }

    @Test
    fun testMonsterRebornRevivesHighestAtkMonsterFromGraveyard() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        // Put a powerful monster in opponent's Graveyard (Blue-Eyes White Dragon 3000 ATK)
        val blueEyes = Card(id = 1, name = "Blue-Eyes White Dragon", atk = 3000, def = 2500, level = 8, attribute = CardAttribute.LIGHT, cardType = CardType.MONSTER, monsterType = MonsterType.DRAGON)
        val kuriboh = Card(id = 5, name = "Kuriboh", atk = 300, def = 200, level = 1, attribute = CardAttribute.DARK, cardType = CardType.MONSTER, monsterType = MonsterType.FIEND)
        testEngine.opponent.graveyard.add(blueEyes)
        testEngine.player.graveyard.add(kuriboh)

        // Player activates Monster Reborn from hand
        val monsterReborn = Card(id = 500, name = "Monster Reborn", atk = 0, def = 0, level = 0, attribute = CardAttribute.SPELL, cardType = CardType.SPELL, monsterType = MonsterType.NONE)
        testEngine.player.hand.add(DuelCard(card = monsterReborn))
        val handIdx = testEngine.player.hand.size - 1

        val activated = testEngine.activateSpell(isPlayer = true, handIndex = handIdx)
        assertTrue("Monster Reborn activation succeeds", activated)

        // Player should now control Blue-Eyes White Dragon on their field!
        val summoned = testEngine.player.monsterZones.filterNotNull().firstOrNull()
        assertNotNull("Monster Reborn must summon a monster to field", summoned)
        assertEquals("Should revive highest ATK monster across Graveyards (Blue-Eyes)", "Blue-Eyes White Dragon", summoned?.card?.name)
        assertFalse("Blue-Eyes removed from opponent Graveyard", testEngine.opponent.graveyard.any { it.name == "Blue-Eyes White Dragon" })
    }

    @Test
    fun testSwordsOfRevealingLightFlipsFaceDownAndBlocksAttack() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        // Opponent has 1 facedown defense monster
        val stoneGiant = Card(id = 12, name = "Giant Soldier of Stone", atk = 1300, def = 2000, level = 3, attribute = CardAttribute.EARTH, cardType = CardType.MONSTER, monsterType = MonsterType.ROCK)
        testEngine.opponent.monsterZones[0] = DuelCard(card = stoneGiant, position = CardPosition.FACE_DOWN_DEFENSE)

        // Player activates Swords of Revealing Light
        val swords = Card(id = 501, name = "Swords of Revealing Light", atk = 0, def = 0, level = 0, attribute = CardAttribute.SPELL, cardType = CardType.SPELL, monsterType = MonsterType.NONE)
        testEngine.player.hand.add(DuelCard(card = swords))
        val handIdx = testEngine.player.hand.size - 1

        val activated = testEngine.activateSpell(isPlayer = true, handIndex = handIdx)
        assertTrue("Swords of Revealing Light activated", activated)

        // Monster should be flipped face-up defense
        val oppMon = testEngine.opponent.monsterZones[0]
        assertNotNull(oppMon)
        assertEquals("Facedown monster flipped to FACE_UP_DEFENSE", CardPosition.FACE_UP_DEFENSE, oppMon?.position)
        assertEquals("Swords turns set to 3", 3, testEngine.opponent.swordsTurnsRemaining)

        // Opponent turn: Battle phase attacks should be blocked
        testEngine.passTurn(autoRunAi = false)
        testEngine.nextPhase() // BATTLE_PHASE
        assertFalse("Opponent cannot declare attack while Swords active", testEngine.declareAttack(0, null))
    }

    @Test
    fun testChangeOfHeartStealsAndReturnsAtEndOfTurn() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        // Opponent has Vorse Raider
        val vorseRaider = Card(id = 10, name = "Vorse Raider", atk = 1900, def = 1200, level = 4, attribute = CardAttribute.DARK, cardType = CardType.MONSTER, monsterType = MonsterType.BEAST_WARRIOR)
        testEngine.opponent.monsterZones[0] = DuelCard(card = vorseRaider, position = CardPosition.FACE_UP_ATTACK)

        // Player activates Change of Heart
        val changeOfHeart = Card(id = 502, name = "Change of Heart", atk = 0, def = 0, level = 0, attribute = CardAttribute.SPELL, cardType = CardType.SPELL, monsterType = MonsterType.NONE)
        testEngine.player.hand.add(DuelCard(card = changeOfHeart))
        val handIdx = testEngine.player.hand.size - 1

        assertTrue("Change of Heart activates", testEngine.activateSpell(isPlayer = true, handIndex = handIdx))

        // Player controls Vorse Raider now
        assertNull("Opponent lost control of Vorse Raider", testEngine.opponent.monsterZones[0])
        val stolen = testEngine.player.monsterZones.filterNotNull().firstOrNull()
        assertNotNull("Player now has stolen monster", stolen)
        assertEquals("Vorse Raider", stolen?.card?.name)

        // End player turn -> monster returns to opponent
        testEngine.passTurn(autoRunAi = false)
        assertNull("Player no longer has stolen monster", testEngine.player.monsterZones[0])
        assertNotNull("Vorse Raider returned to opponent's field", testEngine.opponent.monsterZones[0])
        assertEquals("Vorse Raider", testEngine.opponent.monsterZones[0]?.card?.name)
    }

    @Test
    fun testPasswordMachineUnlocksValidCard() {
        com.yugioh.recomp.core.engine.DdsCampaignManager.resetToNewGame()
        com.yugioh.recomp.core.engine.DdsCampaignManager.starchips = 100

        val (success, message) = com.yugioh.recomp.core.engine.DdsCampaignManager.unlockCardViaPasscode("89631139")
        assertTrue("Valid passcode for Blue-Eyes White Dragon should succeed", success)
        assertTrue("Message confirms unlock", message.contains("Blue-Eyes White Dragon"))
        assertTrue("Card is in Trunk", com.yugioh.recomp.core.engine.DdsCampaignManager.trunk.any { it.name == "Blue-Eyes White Dragon" })
        assertTrue("Starchips were deducted", com.yugioh.recomp.core.engine.DdsCampaignManager.starchips < 100)
    }

    @Test
    fun testPasswordMachineRejectsInvalidPasscode() {
        val (success, message) = com.yugioh.recomp.core.engine.DdsCampaignManager.unlockCardViaPasscode("00000000")
        assertFalse("Invalid passcode should fail", success)
        assertTrue("Message indicates invalid passcode", message.contains("Invalid passcode"))
    }

    @Test
    fun testAiDuelTacticsProfilesAndDecisions() {
        val kaibaProfile = com.yugioh.recomp.core.engine.AiDuelTactics.getProfile("Seto Kaiba")
        assertEquals("Seto Kaiba", kaibaProfile.name)
        assertEquals("Blue-Eyes White Dragon", kaibaProfile.signatureBossCard)
        assertEquals(com.yugioh.recomp.core.engine.AiArchetype.DRAGON_POWER, kaibaProfile.archetype)

        val yugiProfile = com.yugioh.recomp.core.engine.AiDuelTactics.getProfile("Yami Yugi")
        assertEquals("Dark Magician", yugiProfile.signatureBossCard)
        assertEquals(com.yugioh.recomp.core.engine.AiArchetype.TACTICAL_CONTROL, yugiProfile.archetype)

        // Test boss summon dialogue
        val kaibaQuote = com.yugioh.recomp.core.engine.AiDuelTactics.getBossSummonDialogue("Seto Kaiba", "Blue-Eyes White Dragon")
        assertNotNull("Kaiba must have dialogue for Blue-Eyes", kaibaQuote)
        assertTrue(kaibaQuote!!.contains("Burst Stream of Destruction"))
    }

    @Test
    fun testFieldSpellActivationAndStatBuffs() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        val yami = Card(id = 600, name = "Yami", atk = 0, def = 0, level = 0, attribute = CardAttribute.SPELL, cardType = CardType.SPELL, monsterType = MonsterType.NONE)
        testEngine.player.hand.add(DuelCard(card = yami))
        val handIdx = testEngine.player.hand.size - 1

        assertTrue("Activate Yami field spell", testEngine.activateSpell(isPlayer = true, handIndex = handIdx))
        assertNotNull("Active field spell set to Yami", testEngine.activeFieldSpell)
        assertEquals("Yami", testEngine.activeFieldSpell?.name)

        // Dark Magician (SPELLCASTER) should gain +200 ATK/DEF from Yami
        val darkMagician = Card(id = 14, name = "Dark Magician", atk = 2500, def = 2100, level = 7, attribute = CardAttribute.DARK, cardType = CardType.MONSTER, monsterType = MonsterType.SPELLCASTER)
        assertEquals(2700, testEngine.getEffectiveAtk(darkMagician))
        assertEquals(2300, testEngine.getEffectiveDef(darkMagician))
    }

    @Test
    fun testActivateSetSpellCard() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Tea Gardner")

        val potOfGreed = Card(
            id = 501,
            name = "Pot of Greed",
            atk = 0,
            def = 0,
            level = 0,
            attribute = CardAttribute.SPELL,
            cardType = CardType.SPELL
        )
        testEngine.player.hand.add(DuelCard(card = potOfGreed))
        val handIdx = testEngine.player.hand.size - 1

        // Set the Spell card onto zone 0
        assertTrue("Set Spell card to zone 0", testEngine.setSpellTrap(isPlayer = true, handIndex = handIdx, slotIndex = 0))
        assertNotNull("Zone 0 contains set card", testEngine.player.spellTrapZones[0])
        assertEquals("Pot of Greed", testEngine.player.spellTrapZones[0]?.card?.name)

        val handSizeBefore = testEngine.player.hand.size

        // Activate the set Spell card
        val activated = testEngine.activateSetSpell(isPlayer = true, slotIndex = 0)
        assertTrue("Set spell activation succeeds in Main Phase", activated)
        assertNull("Set spell leaves spell/trap zone after resolution", testEngine.player.spellTrapZones[0])
        assertEquals("Pot of Greed draws 2 cards", handSizeBefore + 2, testEngine.player.hand.size)
        assertTrue("Pot of Greed is sent to Graveyard", testEngine.player.graveyard.any { it.name == "Pot of Greed" })
    }

    @Test
    fun testFusionSummoningWithPolymerization() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        // Extra Deck contains classic Fusion Monsters
        assertTrue("Extra Deck initialized with Fusion Monsters", testEngine.player.extraDeck.isNotEmpty())
        assertTrue("Extra Deck contains Twin-Headed Thunder Dragon", testEngine.player.extraDeck.any { it.name == "Twin-Headed Thunder Dragon" })

        // Give player 2x Thunder Dragon materials and 1x Polymerization
        val thunderDragon1 = Card(id = 701, name = "Thunder Dragon", atk = 1600, def = 1500, level = 5, attribute = CardAttribute.LIGHT, cardType = CardType.MONSTER, monsterType = MonsterType.THUNDER)
        val thunderDragon2 = Card(id = 702, name = "Thunder Dragon", atk = 1600, def = 1500, level = 5, attribute = CardAttribute.LIGHT, cardType = CardType.MONSTER, monsterType = MonsterType.THUNDER)
        val poly = FusionRitualRegistry.POLYMERIZATION

        testEngine.player.hand.add(DuelCard(card = thunderDragon1))
        testEngine.player.hand.add(DuelCard(card = thunderDragon2))
        testEngine.player.hand.add(DuelCard(card = poly))

        val polyIndex = testEngine.player.hand.size - 1
        assertTrue("Activate Polymerization", testEngine.activateSpell(isPlayer = true, handIndex = polyIndex))

        // Monster zone must now contain Twin-Headed Thunder Dragon (2800 ATK)
        val summonedFusion = testEngine.player.monsterZones.filterNotNull().find { it.card.name == "Twin-Headed Thunder Dragon" }
        assertNotNull("Twin-Headed Thunder Dragon was Fusion Summoned to field", summonedFusion)
        assertEquals(2800, summonedFusion?.card?.atk)
        assertEquals(CardPosition.FACE_UP_ATTACK, summonedFusion?.position)

        // Materials must be in the Graveyard
        assertEquals(2, testEngine.player.graveyard.count { it.name == "Thunder Dragon" })
    }

    @Test
    fun testRitualSummoningSystem() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Yugi Muto")

        val bls = FusionRitualRegistry.BLACK_LUSTER_SOLDIER
        val ritualSpell = FusionRitualRegistry.BLACK_LUSTER_RITUAL
        val tribute1 = Card(id = 801, name = "Judge Man", atk = 2200, def = 1500, level = 6, attribute = CardAttribute.EARTH, cardType = CardType.MONSTER)
        val tribute2 = Card(id = 802, name = "Celtic Guardian", atk = 1400, def = 1200, level = 4, attribute = CardAttribute.EARTH, cardType = CardType.MONSTER)

        // Hand has BLS, Ritual Spell, and tributes totaling 10 levels (>= 8 required)
        testEngine.player.hand.add(DuelCard(card = bls))
        testEngine.player.hand.add(DuelCard(card = tribute1))
        testEngine.player.hand.add(DuelCard(card = tribute2))
        testEngine.player.hand.add(DuelCard(card = ritualSpell))

        val spellIdx = testEngine.player.hand.size - 1
        assertTrue("Activate Black Luster Ritual", testEngine.activateSpell(isPlayer = true, handIndex = spellIdx))

        val blsOnField = testEngine.player.monsterZones.filterNotNull().find { it.card.name == "Black Luster Soldier" }
        assertNotNull("Black Luster Soldier was Ritual Summoned", blsOnField)
        assertEquals(3000, blsOnField?.card?.atk)
        assertEquals(2500, blsOnField?.card?.def)
    }

    @Test
    fun testEquipSpellStatBoostAndDestructionBurn() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Seto Kaiba")

        // Summon a monster
        val monster = Card(id = 901, name = "Axe Raider", atk = 1700, def = 1150, level = 4, attribute = CardAttribute.EARTH, cardType = CardType.MONSTER)
        testEngine.player.monsterZones[0] = DuelCard(card = monster, position = CardPosition.FACE_UP_ATTACK)

        // Equip Axe of Despair (+1000 ATK)
        val axe = FusionRitualRegistry.AXE_OF_DESPAIR
        testEngine.player.hand.add(DuelCard(card = axe))
        testEngine.activateSpell(isPlayer = true, handIndex = testEngine.player.hand.size - 1)

        val duelMonster = testEngine.player.monsterZones[0]!!
        assertEquals("Axe of Despair equipped", 1, duelMonster.equippedCards.size)
        assertEquals(2700, testEngine.getEffectiveAtk(duelMonster.card, duelMonster))

        // Equip Black Pendant (+500 ATK)
        val pendant = Card(id = 902, name = "Black Pendant", atk = 0, def = 0, level = 0, attribute = CardAttribute.SPELL, cardType = CardType.SPELL)
        testEngine.player.hand.add(DuelCard(card = pendant))
        testEngine.activateSpell(isPlayer = true, handIndex = testEngine.player.hand.size - 1)

        assertEquals("Two equip cards attached", 2, duelMonster.equippedCards.size)
        assertEquals(3200, testEngine.getEffectiveAtk(duelMonster.card, duelMonster))

        // Put higher ATK monster on opponent's field and declare battle to trigger destruction
        val oppBoss = Card(id = 999, name = "Blue-Eyes White Dragon", atk = 3500, def = 2500, level = 8, attribute = CardAttribute.LIGHT, cardType = CardType.MONSTER)
        testEngine.opponent.monsterZones[0] = DuelCard(card = oppBoss, position = CardPosition.FACE_UP_ATTACK)

        testEngine.nextPhase() // Advance from MAIN_PHASE_1 to BATTLE_PHASE
        val oppLpBefore = testEngine.opponent.lifePoints
        testEngine.declareAttack(attackerSlot = 0, targetSlot = 0)

        assertNull("Axe Raider was destroyed by Blue-Eyes", testEngine.player.monsterZones[0])
        // Black Pendant sent to graveyard inflicts 500 damage to opponent!
        assertEquals("Opponent took 500 effect damage from Black Pendant destruction", oppLpBefore - 500, testEngine.opponent.lifePoints)
    }

    @Test
    fun testSavedDeckRecipesAndPasscodeExportImport() {
        GbaCampaignManager.activeDeck.clear()
        GbaCampaignManager.trunk.clear()

        val card1 = GbaCardCatalog.ARMORED_LIZARD
        val card2 = GbaCardCatalog.SUMMONED_SKULL_GBA
        val card3 = GbaCardCatalog.RAIGEKI

        GbaCampaignManager.activeDeck.addAll(listOf(card1, card2, card3))

        // Save to Slot 2
        GbaCampaignManager.saveCurrentDeckToSlot(2, "My Beatdown Deck")
        val recipe = GbaCampaignManager.savedDeckRecipes.find { it.id == 2 }
        assertNotNull("Recipe saved", recipe)
        assertEquals("My Beatdown Deck", recipe?.name)
        assertEquals(3, recipe?.cardIds?.size)

        // Export passcodes
        val exportStr = GbaCampaignManager.exportDeckToPasscodes()
        assertTrue("Export string contains Armored Lizard passcode", exportStr.contains("15480588"))
        assertTrue("Export string contains Summoned Skull passcode", exportStr.contains("70781052"))

        // Import passcodes
        val (count, msg) = GbaCampaignManager.importDeckFromPasscodes("15480588, 70781052, 12580477")
        assertEquals(3, count)
        assertTrue(msg.contains("Successfully imported"))
        assertEquals(3, GbaCampaignManager.activeDeck.size)
    }

    @Test
    fun testCustomDuelSettingsAndSandboxArena() {
        val testEngine = DuelEngine()
        // Test custom 4000 LP speed duel
        testEngine.startDuel(opponentName = "Joey Wheeler", startingLp = 4000)
        assertEquals(4000, testEngine.player.lifePoints)
        assertEquals(4000, testEngine.opponent.lifePoints)

        // Test mirror match with custom opponent deck
        val customDeck = listOf(GbaCardCatalog.ARMORED_LIZARD, GbaCardCatalog.BATTLE_OX_GBA)
        val mirrorEngine = DuelEngine()
        mirrorEngine.startDuel(opponentName = "Mirror Clone", startingLp = 2000, customOpponentDeck = customDeck)
        assertEquals(2000, mirrorEngine.player.lifePoints)
        assertEquals("Mirror Clone", mirrorEngine.opponent.name)
    }

    @Test
    fun testExodiaObliterateVictoryInHand() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Seto Kaiba")
        testEngine.player.hand.clear()

        val p1 = Card(id = 101, name = "Exodia the Forbidden One", atk = 1000, def = 1000, level = 3, attribute = CardAttribute.DARK, cardType = CardType.MONSTER)
        val p2 = Card(id = 102, name = "Right Arm of the Forbidden One", atk = 200, def = 300, level = 1, attribute = CardAttribute.DARK, cardType = CardType.MONSTER)
        val p3 = Card(id = 103, name = "Left Arm of the Forbidden One", atk = 200, def = 300, level = 1, attribute = CardAttribute.DARK, cardType = CardType.MONSTER)
        val p4 = Card(id = 104, name = "Right Leg of the Forbidden One", atk = 200, def = 300, level = 1, attribute = CardAttribute.DARK, cardType = CardType.MONSTER)
        val p5 = Card(id = 105, name = "Left Leg of the Forbidden One", atk = 200, def = 300, level = 1, attribute = CardAttribute.DARK, cardType = CardType.MONSTER)

        testEngine.player.hand.addAll(listOf(DuelCard(card = p1), DuelCard(card = p2), DuelCard(card = p3), DuelCard(card = p4)))
        assertFalse("4 pieces does not win", ToonAnimeArchetype.hasAllExodiaPieces(testEngine.player.hand))

        // Draw the 5th piece
        testEngine.player.deck.clear()
        testEngine.player.deck.add(p5)
        testEngine.drawCard(isPlayer = true)

        assertTrue("All 5 pieces assembled", ToonAnimeArchetype.hasAllExodiaPieces(testEngine.player.hand))
        assertEquals("Player wins immediately", testEngine.player.name, testEngine.winner)
        assertTrue("Exodia win flag set", testEngine.isExodiaWin)
    }

    @Test
    fun testToonWorldSummonRequirementAndDestructionCollapse() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Maximillion Pegasus")

        val toonMermaid = ToonAnimeArchetype.TOON_MERMAID
        testEngine.player.hand.clear()
        testEngine.player.hand.add(DuelCard(card = toonMermaid))

        // Without Toon World on field, summoning Toon Mermaid should fail
        val summonWithoutToonWorld = testEngine.summonMonster(isPlayer = true, handIndex = 0, slotIndex = 0)
        assertFalse("Cannot summon Toon monster without Toon World active", summonWithoutToonWorld)
        assertNull("Monster slot remains empty", testEngine.player.monsterZones[0])

        // Activate Toon World
        testEngine.player.hand.add(DuelCard(card = ToonAnimeArchetype.TOON_WORLD))
        val toonWorldHandIdx = testEngine.player.hand.size - 1
        val initialLp = testEngine.player.lifePoints
        val activated = testEngine.activateSpell(isPlayer = true, handIndex = toonWorldHandIdx)
        assertTrue("Toon World activated successfully", activated)
        assertEquals("1000 LP cost deducted for Toon World", initialLp - 1000, testEngine.player.lifePoints)
        assertTrue("Toon World is active", testEngine.isToonWorldActive(testEngine.player))

        // Now summon Toon Mermaid
        val mermaidHandIdx = testEngine.player.hand.indexOfFirst { it.card.name == "Toon Mermaid" }
        val summonWithToonWorld = testEngine.summonMonster(isPlayer = true, handIndex = mermaidHandIdx, slotIndex = 0)
        assertTrue("Toon Mermaid summoned successfully with Toon World active", summonWithToonWorld)
        assertNotNull("Monster slot 0 has Toon Mermaid", testEngine.player.monsterZones[0])

        // Destroy Toon World: Toon Mermaid should collapse and be sent to Graveyard!
        testEngine.player.spellTrapZones[0] = null // Remove Toon World
        testEngine.checkToonWorldDestruction(testEngine.player)
        assertNull("Toon Mermaid collapsed when Toon World left field", testEngine.player.monsterZones[0])
        assertTrue("Toon Mermaid moved to Graveyard", testEngine.player.graveyard.any { it.name == "Toon Mermaid" })
    }

    @Test
    fun testToonDirectAttackBypassAndSummoningSickness() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        val toonSkull = ToonAnimeArchetype.TOON_SUMMONED_SKULL
        testEngine.player.monsterZones[0] = DuelCard(card = toonSkull, position = CardPosition.FACE_UP_ATTACK, turnsOnField = 0)
        testEngine.opponent.monsterZones[0] = DuelCard(card = GbaCardCatalog.ARMORED_LIZARD, position = CardPosition.FACE_UP_ATTACK)

        testEngine.nextPhase() // Enter Battle Phase

        // Summoning sickness test: turnsOnField == 0
        val attackSickness = testEngine.declareAttack(attackerSlot = 0, targetSlot = null)
        assertFalse("Toon cannot attack directly turn it is summoned", attackSickness)

        // Advance turnsOnField to 1
        testEngine.player.monsterZones[0]?.turnsOnField = 1
        val oppLpBefore = testEngine.opponent.lifePoints
        val attackBypass = testEngine.declareAttack(attackerSlot = 0, targetSlot = null)
        assertTrue("Toon can bypass non-Toon monsters for direct attack", attackBypass)
        assertEquals("Direct attack damage dealt (2500 ATK)", oppLpBefore - 2500, testEngine.opponent.lifePoints)
    }

    @Test
    fun testSanganAndWitchOfTheBlackForestSearchers() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        testEngine.player.deck.clear()
        val kuriboh = Card(id = 201, name = "Kuriboh", atk = 300, def = 200, level = 1, attribute = CardAttribute.DARK, cardType = CardType.MONSTER)
        val highDefMonster = Card(id = 202, name = "Big Shield Gardna", atk = 100, def = 2600, level = 4, attribute = CardAttribute.EARTH, cardType = CardType.MONSTER)
        val lowDefMonster = Card(id = 203, name = "Dark Magician", atk = 2500, def = 1200, level = 7, attribute = CardAttribute.DARK, cardType = CardType.MONSTER)
        testEngine.player.deck.addAll(listOf(kuriboh, highDefMonster, lowDefMonster))

        val sangan = Card(id = 204, name = "Sangan", atk = 1000, def = 600, level = 3, attribute = CardAttribute.DARK, cardType = CardType.MONSTER)
        val witch = ToonAnimeArchetype.WITCH_OF_THE_BLACK_FOREST

        // Trigger Sangan searcher (ATK <= 1500)
        val handBeforeSangan = testEngine.player.hand.size
        testEngine.triggerGraveyardSearcher(sangan, testEngine.player)
        assertEquals("Sangan adds 1 monster with ATK <= 1500 to hand", handBeforeSangan + 1, testEngine.player.hand.size)
        assertTrue("Kuriboh (300 ATK) was searched", testEngine.player.hand.any { it.card.name == "Kuriboh" })

        // Trigger Witch searcher (DEF <= 1500)
        val handBeforeWitch = testEngine.player.hand.size
        testEngine.triggerGraveyardSearcher(witch, testEngine.player)
        assertEquals("Witch adds 1 monster with DEF <= 1500 to hand", handBeforeWitch + 1, testEngine.player.hand.size)
        assertTrue("Dark Magician (1200 DEF) was searched", testEngine.player.hand.any { it.card.name == "Dark Magician" })
    }

    @Test
    fun testRevivalAndGambleCards() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        val summonSkull = GbaCardCatalog.SUMMONED_SKULL_GBA
        testEngine.player.graveyard.add(summonSkull)

        // Premature Burial (800 LP cost, resurrects monster)
        val initLp = testEngine.player.lifePoints
        testEngine.player.hand.add(DuelCard(card = ToonAnimeArchetype.PREMATURE_BURIAL))
        val pbIdx = testEngine.player.hand.size - 1
        val pbSuccess = testEngine.activateSpell(isPlayer = true, handIndex = pbIdx)
        assertTrue("Premature Burial activated", pbSuccess)
        assertEquals("800 LP paid for Premature Burial", initLp - 800, testEngine.player.lifePoints)
        assertEquals("Summoned Skull revived in monster zone 0", "Summoned Skull", testEngine.player.monsterZones[0]?.card?.name)

        // Call of the Haunted (Trap resurrection)
        val blueEyes = Card(id = 301, name = "Blue-Eyes White Dragon", atk = 3000, def = 2500, level = 8, attribute = CardAttribute.LIGHT, cardType = CardType.MONSTER)
        testEngine.player.graveyard.add(blueEyes)
        testEngine.player.spellTrapZones[1] = DuelCard(card = ToonAnimeArchetype.CALL_OF_THE_HAUNTED, position = CardPosition.SET_SPELL_TRAP)
        val cothSuccess = testEngine.activateSetTrap(isPlayer = true, slotIndex = 1)
        assertTrue("Call of the Haunted activated", cothSuccess)
        assertEquals("Blue-Eyes revived in monster zone 1", "Blue-Eyes White Dragon", testEngine.player.monsterZones[1]?.card?.name)

        // Canonical passcodes validation
        assertEquals("Toon World canonical passcode", "15259703", CardPasscodeRegistry.findPasscodeByCardName("Toon World"))
        assertEquals("Cup of Ace canonical passcode", "00441788", CardPasscodeRegistry.findPasscodeByCardName("Cup of Ace"))
        assertEquals("Witch of the Black Forest canonical passcode", "04335645", CardPasscodeRegistry.findPasscodeByCardName("Witch of the Black Forest"))
    }

    @Test
    fun testKuribohHandTrapNegatesBattleDamage() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        val kuriboh = Card(id = 401, name = "Kuriboh", atk = 300, def = 200, level = 1, attribute = CardAttribute.DARK, cardType = CardType.MONSTER)
        testEngine.opponent.hand.clear()
        testEngine.opponent.hand.add(DuelCard(card = kuriboh))

        // Player attacks directly with 3000 ATK Blue-Eyes White Dragon
        val blueEyes = Card(id = 402, name = "Blue-Eyes White Dragon", atk = 3000, def = 2500, level = 8, attribute = CardAttribute.LIGHT, cardType = CardType.MONSTER)
        testEngine.player.monsterZones[0] = DuelCard(card = blueEyes, position = CardPosition.FACE_UP_ATTACK)
        testEngine.nextPhase() // Enter Battle Phase

        val oppLpBefore = testEngine.opponent.lifePoints
        val attackSuccess = testEngine.declareAttack(attackerSlot = 0, targetSlot = null)
        assertTrue("Attack resolves", attackSuccess)
        assertEquals("Kuriboh in hand reduced battle damage to 0!", oppLpBefore, testEngine.opponent.lifePoints)
        assertTrue("Kuriboh moved to opponent graveyard", testEngine.opponent.graveyard.any { it.name == "Kuriboh" })
        assertFalse("Kuriboh removed from hand", testEngine.opponent.hand.any { it.card.name == "Kuriboh" })
        assertEquals("Active chain link 1 recorded Kuriboh", "Kuriboh", testEngine.activeChain.firstOrNull()?.card?.name)
    }

    @Test
    fun testSolemnJudgmentNegatesNormalSummonWithHalfLp() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Seto Kaiba")

        val solemn = Card(id = 403, name = "Solemn Judgment", atk = 0, def = 0, level = 0, attribute = CardAttribute.TRAP, cardType = CardType.TRAP)
        testEngine.opponent.spellTrapZones[0] = DuelCard(card = solemn, position = CardPosition.SET_SPELL_TRAP)

        val lizard = GbaCardCatalog.ARMORED_LIZARD
        testEngine.player.hand.clear()
        testEngine.player.hand.add(DuelCard(card = lizard))

        val oppLpBefore = testEngine.opponent.lifePoints
        val summonSuccess = testEngine.summonMonster(isPlayer = true, handIndex = 0, slotIndex = 0, setDefense = false)
        assertTrue("Summon action processed", summonSuccess)
        assertNull("Lizard summon was negated, zone 0 empty", testEngine.player.monsterZones[0])
        assertTrue("Lizard sent to player graveyard", testEngine.player.graveyard.any { it.name == "Armored Lizard" })
        assertEquals("Solemn Judgment paid half LP", oppLpBefore / 2, testEngine.opponent.lifePoints)
        assertTrue("Solemn Judgment sent to opponent graveyard", testEngine.opponent.graveyard.any { it.name == "Solemn Judgment" })
    }

    @Test
    fun testMagicJammerNegatesSpellWithDiscard() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        val magicJammer = Card(id = 404, name = "Magic Jammer", atk = 0, def = 0, level = 0, attribute = CardAttribute.TRAP, cardType = CardType.TRAP)
        testEngine.opponent.spellTrapZones[0] = DuelCard(card = magicJammer, position = CardPosition.SET_SPELL_TRAP)
        val discardFodder = Card(id = 405, name = "Baby Dragon", atk = 1200, def = 700, level = 3, attribute = CardAttribute.WIND, cardType = CardType.MONSTER)
        testEngine.opponent.hand.clear()
        testEngine.opponent.hand.add(DuelCard(card = discardFodder))

        // Player activates Raigeki
        testEngine.player.hand.clear()
        testEngine.player.hand.add(DuelCard(card = GbaCardCatalog.RAIGEKI))
        // Put a monster on opponent's field to verify it survives because Raigeki was negated
        testEngine.opponent.monsterZones[0] = DuelCard(card = GbaCardCatalog.ARMORED_LIZARD, position = CardPosition.FACE_UP_ATTACK)

        val activated = testEngine.activateSpell(isPlayer = true, handIndex = 0)
        assertTrue("Spell activation processed", activated)
        assertNotNull("Armored Lizard survived because Raigeki was negated", testEngine.opponent.monsterZones[0])
        assertTrue("Raigeki in player graveyard", testEngine.player.graveyard.any { it.name == "Raigeki" })
        assertTrue("Magic Jammer in opponent graveyard", testEngine.opponent.graveyard.any { it.name == "Magic Jammer" })
        assertTrue("Discard fodder in opponent graveyard", testEngine.opponent.graveyard.any { it.name == "Baby Dragon" })
    }

    @Test
    fun testSevenToolsNegatesTrapWith1000Lp() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        // Player has Seven Tools set
        val sevenTools = Card(id = 406, name = "Seven Tools of the Bandit", atk = 0, def = 0, level = 0, attribute = CardAttribute.TRAP, cardType = CardType.TRAP)
        testEngine.player.spellTrapZones[0] = DuelCard(card = sevenTools, position = CardPosition.SET_SPELL_TRAP)

        // Opponent sets and activates Waboku
        val waboku = Card(id = 407, name = "Waboku", atk = 0, def = 0, level = 0, attribute = CardAttribute.TRAP, cardType = CardType.TRAP)
        testEngine.opponent.spellTrapZones[0] = DuelCard(card = waboku, position = CardPosition.SET_SPELL_TRAP)

        val playerLpBefore = testEngine.player.lifePoints
        val activated = testEngine.activateSetTrap(isPlayer = false, slotIndex = 0)
        assertTrue("Trap activation processed", activated)
        assertEquals("Player paid 1000 LP for Seven Tools", playerLpBefore - 1000, testEngine.player.lifePoints)
        assertFalse("Waboku was negated and NOT active", testEngine.opponent.isWabokuActive)
        assertTrue("Seven Tools in player graveyard", testEngine.player.graveyard.any { it.name == "Seven Tools of the Bandit" })
        assertTrue("Waboku in opponent graveyard", testEngine.opponent.graveyard.any { it.name == "Waboku" })
    }

    @Test
    fun testSwordsOfRevealingLightPersistenceAndAutoDestruction() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        // Player activates Swords of Revealing Light from hand
        testEngine.player.hand.clear()
        testEngine.player.hand.add(DuelCard(card = GbaCardCatalog.SWORDS_OF_REVEALING_LIGHT))
        val activated = testEngine.activateSpell(isPlayer = true, handIndex = 0)
        assertTrue("Swords of Revealing Light activated", activated)

        // Swords should persist face-up in player's spell/trap zone
        val swordsZone = testEngine.player.spellTrapZones.firstOrNull { it?.card?.name == "Swords of Revealing Light" }
        assertNotNull("Swords persists on the field in spell/trap zone", swordsZone)
        assertEquals("Swords is face-up spell/trap", CardPosition.FACE_UP_SPELL_TRAP, swordsZone?.position)
        assertEquals("Opponent has 3 swords turns remaining", 3, testEngine.opponent.swordsTurnsRemaining)

        // Pass player turn -> opponent turn starts
        testEngine.passTurn(autoRunAi = false)
        assertFalse(testEngine.isPlayerTurn)
        // Pass opponent turn 1 -> ends opponent turn
        testEngine.passTurn(autoRunAi = false)
        assertTrue(testEngine.isPlayerTurn)
        assertEquals("Opponent has 2 swords turns remaining", 2, testEngine.opponent.swordsTurnsRemaining)
        assertNotNull("Swords still on field", testEngine.player.spellTrapZones.firstOrNull { it?.card?.name == "Swords of Revealing Light" })

        // Pass player turn -> opponent turn starts
        testEngine.passTurn(autoRunAi = false)
        // Pass opponent turn 2 -> ends opponent turn
        testEngine.passTurn(autoRunAi = false)
        assertEquals("Opponent has 1 swords turn remaining", 1, testEngine.opponent.swordsTurnsRemaining)
        assertNotNull("Swords still on field", testEngine.player.spellTrapZones.firstOrNull { it?.card?.name == "Swords of Revealing Light" })

        // Pass player turn -> opponent turn starts
        testEngine.passTurn(autoRunAi = false)
        // Pass opponent turn 3 -> Swords expires and is destroyed
        testEngine.passTurn(autoRunAi = false)
        assertEquals("Opponent has 0 swords turns remaining", 0, testEngine.opponent.swordsTurnsRemaining)
        assertNull("Swords removed from field after 3 turns", testEngine.player.spellTrapZones.firstOrNull { it?.card?.name == "Swords of Revealing Light" })
        assertTrue("Swords placed in player's graveyard", testEngine.player.graveyard.any { it.name == "Swords of Revealing Light" })
    }

    @Test
    fun testScapegoatSummonsSheepTokensAndBlocksTributes() {
        val synthGba = com.yugioh.recomp.core.rom.RomValidator.createSyntheticGbaRom()
        com.yugioh.recomp.core.rom.RomAssetCache.populateFromRom(
            com.yugioh.recomp.core.rom.RomTarget.ETERNAL_DUELIST_SOUL_USA,
            synthGba
        )
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")
        assertTrue("Running in GBA mode", testEngine.isGbaMode)

        // Player activates Scapegoat
        testEngine.player.hand.clear()
        testEngine.player.hand.add(DuelCard(card = ToonAnimeArchetype.SCAPEGOAT))
        val activated = testEngine.activateSpell(isPlayer = true, handIndex = 0)
        assertTrue("Scapegoat activated", activated)

        // All 5 monster zones were empty, so zones should be filled with Sheep Tokens
        val sheepTokens = testEngine.player.monsterZones.filterNotNull().filter { it.isToken }
        assertEquals("Scapegoat summoned tokens", 5, sheepTokens.size)
        assertTrue("Token has 0 ATK", sheepTokens.all { it.card.atk == 0 })
        assertTrue("Token has 0 DEF", sheepTokens.all { it.card.def == 0 })
        assertTrue("Token is in Defense position", sheepTokens.all { it.position == CardPosition.FACE_UP_DEFENSE })
        assertFalse("Token cannot be tributed", sheepTokens.all { it.canBeTributed })

        // Attempting to summon a tribute monster (Blue-Eyes White Dragon) should fail because sheep tokens cannot be tributed
        val blueEyes = Card(id = 89631139, name = "Blue-Eyes White Dragon", atk = 3000, def = 2500, level = 8, attribute = CardAttribute.LIGHT, cardType = CardType.MONSTER)
        val (canSummon, reason) = testEngine.canSummon(isPlayer = true, card = blueEyes)
        assertFalse("Cannot tribute sheep tokens for Tribute Summon", canSummon)
        assertTrue("Reason cites tribute requirements", reason.contains("Tribute"))
    }

    @Test
    fun testChainEnergyTaxDeducts500Lp() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        testEngine.player.spellTrapZones[0] = DuelCard(card = ToonAnimeArchetype.CHAIN_ENERGY, position = CardPosition.FACE_UP_SPELL_TRAP)

        val playerLpInitial = testEngine.player.lifePoints

        // Player sets a Spell/Trap
        testEngine.player.hand.clear()
        testEngine.player.hand.add(DuelCard(card = GbaCardCatalog.RAIGEKI))
        testEngine.setSpellTrap(isPlayer = true, handIndex = 0, slotIndex = 1)
        assertEquals("Player paid 500 LP for setting a card under Chain Energy", playerLpInitial - 500, testEngine.player.lifePoints)

        // Player summons a monster
        testEngine.player.hand.add(DuelCard(card = GbaCardCatalog.ARMORED_LIZARD))
        testEngine.summonMonster(isPlayer = true, handIndex = 0, slotIndex = 0, setDefense = false)
        assertEquals("Player paid another 500 LP for normal summoning under Chain Energy", playerLpInitial - 1000, testEngine.player.lifePoints)
    }

    @Test
    fun testTollTaxDeducts500LpOnAttackDeclaration() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        testEngine.player.spellTrapZones[0] = DuelCard(card = ToonAnimeArchetype.TOLL, position = CardPosition.FACE_UP_SPELL_TRAP)
        testEngine.player.monsterZones[0] = DuelCard(card = GbaCardCatalog.ARMORED_LIZARD, position = CardPosition.FACE_UP_ATTACK)

        while (testEngine.currentPhase != DuelPhase.BATTLE_PHASE) {
            testEngine.nextPhase()
        }
        val playerLpInitial = testEngine.player.lifePoints

        // Direct attack declaration with Armored Lizard
        val attacked = testEngine.declareAttack(attackerSlot = 0, targetSlot = null)
        assertTrue("Attack proceeded", attacked)
        assertEquals("Player paid 500 LP Toll tax to declare attack", playerLpInitial - 500, testEngine.player.lifePoints)

        // If player has less than 500 LP, cannot declare attack
        testEngine.player.lifePoints = 400
        testEngine.player.monsterZones[0]!!.resetTurnActions()
        assertFalse("Cannot attack when LP < 500 with Toll active", testEngine.canMonsterAttack(isPlayer = true, slotIndex = 0))
    }

    @Test
    fun testRingOfDestructionDestroysHighestAtkAndBurnsBothPlayers() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        // Put Ring of Destruction set in player's backrow
        testEngine.player.spellTrapZones[0] = DuelCard(card = GbaCardCatalog.RING_OF_DESTRUCTION, position = CardPosition.SET_SPELL_TRAP)

        // Put a 2400 ATK monster on opponent field
        val jinzo = Card(id = 77585513, name = "Jinzo Dummy", atk = 2400, def = 1500, level = 6, attribute = CardAttribute.DARK, cardType = CardType.MONSTER)
        testEngine.opponent.monsterZones[0] = DuelCard(card = jinzo, position = CardPosition.FACE_UP_ATTACK)

        val playerLpBefore = testEngine.player.lifePoints
        val oppLpBefore = testEngine.opponent.lifePoints

        val activated = testEngine.activateSetTrap(isPlayer = true, slotIndex = 0)
        assertTrue("Ring of Destruction activated", activated)
        assertNull("Monster destroyed by Ring of Destruction", testEngine.opponent.monsterZones[0])
        assertEquals("Player took 2400 burn damage", playerLpBefore - 2400, testEngine.player.lifePoints)
        assertEquals("Opponent took 2400 burn damage", oppLpBefore - 2400, testEngine.opponent.lifePoints)
        assertTrue("Monster sent to opponent GY", testEngine.opponent.graveyard.any { it.name == "Jinzo Dummy" })
    }

    @Test
    fun testTorrentialTributeDestroysAllMonstersOnSummon() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        // Opponent has Torrential Tribute set
        testEngine.opponent.spellTrapZones[0] = DuelCard(card = GbaCardCatalog.TORRENTIAL_TRIBUTE, position = CardPosition.SET_SPELL_TRAP)

        // Pre-existing monsters on both sides
        testEngine.opponent.monsterZones[1] = DuelCard(card = GbaCardCatalog.ARMORED_LIZARD, position = CardPosition.FACE_UP_ATTACK)
        testEngine.player.monsterZones[1] = DuelCard(card = GbaCardCatalog.ARMORED_LIZARD, position = CardPosition.FACE_UP_ATTACK)

        // Player summons a monster
        testEngine.player.hand.clear()
        testEngine.player.hand.add(DuelCard(card = GbaCardCatalog.ARMORED_LIZARD))
        val summoned = testEngine.summonMonster(isPlayer = true, handIndex = 0, slotIndex = 0, setDefense = false)
        assertTrue("Summon triggered Torrential Tribute", summoned)

        // All monsters on field should be wiped out
        assertTrue("Player monster zones empty", testEngine.player.monsterZones.all { it == null })
        assertTrue("Opponent monster zones empty", testEngine.opponent.monsterZones.all { it == null })
        assertTrue("Torrential Tribute in opponent graveyard", testEngine.opponent.graveyard.any { it.name == "Torrential Tribute" })
    }

    @Test
    fun testDelinquentDuoDiscardsTwoCardsWithLpCost() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        testEngine.player.hand.clear()
        testEngine.player.hand.add(DuelCard(card = GbaCardCatalog.DELINQUENT_DUO))

        testEngine.opponent.hand.clear()
        testEngine.opponent.hand.add(DuelCard(card = GbaCardCatalog.ARMORED_LIZARD))
        testEngine.opponent.hand.add(DuelCard(card = GbaCardCatalog.BEAN_SOLDIER))
        testEngine.opponent.hand.add(DuelCard(card = GbaCardCatalog.BIG_INSECT))

        val playerLpBefore = testEngine.player.lifePoints
        val activated = testEngine.activateSpell(isPlayer = true, handIndex = 0)
        assertTrue("Delinquent Duo activated", activated)
        assertEquals("Player paid 1000 LP", playerLpBefore - 1000, testEngine.player.lifePoints)
        assertEquals("Opponent hand reduced by 2 cards", 1, testEngine.opponent.hand.size)
        assertEquals("Opponent GY received 2 discarded cards", 2, testEngine.opponent.graveyard.size)
    }

    @Test
    fun testTheForcefulSentryReturnsHandCardToDeck() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        testEngine.player.hand.clear()
        testEngine.player.hand.add(DuelCard(card = GbaCardCatalog.THE_FORCEFUL_SENTRY))

        testEngine.opponent.hand.clear()
        val bossMonster = Card(id = 89631139, name = "Blue-Eyes White Dragon", atk = 3000, def = 2500, level = 8, attribute = CardAttribute.LIGHT, cardType = CardType.MONSTER)
        testEngine.opponent.hand.add(DuelCard(card = bossMonster))
        val oppDeckCountBefore = testEngine.opponent.deck.size

        val activated = testEngine.activateSpell(isPlayer = true, handIndex = 0)
        assertTrue("The Forceful Sentry activated", activated)
        assertEquals("Opponent hand empty", 0, testEngine.opponent.hand.size)
        assertEquals("Card returned to opponent deck", oppDeckCountBefore + 1, testEngine.opponent.deck.size)
        assertTrue("Boss monster returned to deck", testEngine.opponent.deck.any { it.name == "Blue-Eyes White Dragon" })
    }

    @Test
    fun testConfiscationDiscardsOpponentCardWith1000LpCost() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        testEngine.player.hand.clear()
        testEngine.player.hand.add(DuelCard(card = GbaCardCatalog.CONFISCATION))

        testEngine.opponent.hand.clear()
        testEngine.opponent.hand.add(DuelCard(card = GbaCardCatalog.ARMORED_LIZARD))

        val playerLpBefore = testEngine.player.lifePoints
        val activated = testEngine.activateSpell(isPlayer = true, handIndex = 0)
        assertTrue("Confiscation activated", activated)
        assertEquals("Player paid 1000 LP", playerLpBefore - 1000, testEngine.player.lifePoints)
        assertEquals("Opponent hand empty", 0, testEngine.opponent.hand.size)
        assertTrue("Armored Lizard discarded to GY", testEngine.opponent.graveyard.any { it.name == "Armored Lizard" })
    }

    @Test
    fun testCardDestructionDiscardsAndRedraws() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        testEngine.player.hand.clear()
        testEngine.player.hand.add(DuelCard(card = GbaCardCatalog.CARD_DESTRUCTION))
        testEngine.player.hand.add(DuelCard(card = GbaCardCatalog.ARMORED_LIZARD))
        testEngine.player.hand.add(DuelCard(card = GbaCardCatalog.BEAN_SOLDIER))

        testEngine.opponent.hand.clear()
        testEngine.opponent.hand.add(DuelCard(card = GbaCardCatalog.BIG_INSECT))
        testEngine.opponent.hand.add(DuelCard(card = GbaCardCatalog.CYBER_FALCON))

        val activated = testEngine.activateSpell(isPlayer = true, handIndex = 0)
        assertTrue("Card Destruction activated", activated)
        // Player hand had 2 remaining cards when Card Destruction resolved -> player draws 2 cards
        assertEquals("Player drew 2 new cards", 2, testEngine.player.hand.size)
        // Opponent had 2 cards -> opponent draws 2 cards
        assertEquals("Opponent drew 2 new cards", 2, testEngine.opponent.hand.size)
        assertTrue("Player discarded cards in GY", testEngine.player.graveyard.any { it.name == "Armored Lizard" })
        assertTrue("Opponent discarded cards in GY", testEngine.opponent.graveyard.any { it.name == "Big Insect" })
    }

    @Test
    fun testNoblemanOfCrossoutBanishesFaceDownAndExilesFlipCopiesFromDeck() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        // Opponent controls face-down Man-Eater Bug (Flip Effect)
        testEngine.opponent.monsterZones[0] = DuelCard(
            card = GbaCardCatalog.MAN_EATER_BUG,
            position = CardPosition.FACE_DOWN_DEFENSE
        )

        // Seed decks with copies of Man-Eater Bug
        testEngine.opponent.deck.add(GbaCardCatalog.MAN_EATER_BUG)
        testEngine.player.deck.add(GbaCardCatalog.MAN_EATER_BUG)

        testEngine.player.hand.clear()
        testEngine.player.hand.add(DuelCard(card = GbaCardCatalog.NOBLEMAN_OF_CROSSOUT))

        val activated = testEngine.activateSpell(isPlayer = true, handIndex = 0)
        assertTrue("Nobleman of Crossout activated", activated)
        assertNull("Field monster destroyed", testEngine.opponent.monsterZones[0])
        assertTrue("Field monster banished", testEngine.opponent.banishedZone.any { it.name == "Man-Eater Bug" })
        assertFalse("Field monster NOT in graveyard", testEngine.opponent.graveyard.any { it.name == "Man-Eater Bug" })

        // Copies banished from both decks
        assertFalse("Opponent deck stripped of Man-Eater Bug copies", testEngine.opponent.deck.any { it.name == "Man-Eater Bug" })
        assertFalse("Player deck stripped of Man-Eater Bug copies", testEngine.player.deck.any { it.name == "Man-Eater Bug" })
        assertTrue("Player banished zone received exiled deck copies", testEngine.player.banishedZone.any { it.name == "Man-Eater Bug" })
    }

    @Test
    fun testMysticalSpaceTyphoonDestroysSpellTrap() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        // Opponent controls Swords of Revealing Light
        testEngine.opponent.spellTrapZones[0] = DuelCard(
            card = GbaCardCatalog.SWORDS_OF_REVEALING_LIGHT,
            position = CardPosition.FACE_UP_SPELL_TRAP
        )

        testEngine.player.hand.clear()
        testEngine.player.hand.add(DuelCard(card = GbaCardCatalog.MYSTICAL_SPACE_TYPHOON))

        val activated = testEngine.activateSpell(isPlayer = true, handIndex = 0)
        assertTrue("MST activated", activated)
        assertNull("Swords destroyed from zone 0", testEngine.opponent.spellTrapZones[0])
        assertTrue("Swords placed into opponent graveyard", testEngine.opponent.graveyard.any { it.name == "Swords of Revealing Light" })
    }

    @Test
    fun testImperialOrderStandbyMaintenanceCost() {
        val testEngine = DuelEngine()
        testEngine.startDuel("Joey Wheeler")

        // Player controls Imperial Order face-up
        testEngine.player.spellTrapZones[0] = DuelCard(
            card = GbaCardCatalog.IMPERIAL_ORDER,
            position = CardPosition.FACE_UP_SPELL_TRAP
        )

        val playerLpBefore = testEngine.player.lifePoints
        testEngine.handleStandbyPhaseUpkeep()
        assertEquals("Player paid 700 LP maintenance cost", playerLpBefore - 700, testEngine.player.lifePoints)
        assertNotNull("Imperial Order survives when LP paid", testEngine.player.spellTrapZones[0])

        // When LP is low (e.g. 500 LP), cannot pay 700 LP -> destroyed
        testEngine.player.lifePoints = 500
        testEngine.handleStandbyPhaseUpkeep()
        assertNull("Imperial Order destroyed when maintenance cost cannot be paid", testEngine.player.spellTrapZones[0])
        assertTrue("Imperial Order sent to graveyard", testEngine.player.graveyard.any { it.name == "Imperial Order" })
    }

    @Test
    fun testBestOfThreeMatchDuelProgressionAndSideDeckSwapping() {
        val testEngine = DuelEngine()
        testEngine.startMatchDuel("Joey Wheeler")

        assertTrue("Duel is Best-of-3 Match", testEngine.isMatchDuel)
        assertEquals("Starting at Game 1", 1, testEngine.matchGameNumber)
        assertEquals("Initial player score 0", 0, testEngine.playerMatchWins)
        assertEquals("Initial opponent score 0", 0, testEngine.opponentMatchWins)
        assertFalse("Match not finished", testEngine.isMatchFinished)

        // Player wins Game 1
        testEngine.opponent.lifePoints = 0
        testEngine.surrender(isPlayer = false)
        assertEquals("Player score is 1 after Game 1", 1, testEngine.playerMatchWins)
        assertFalse("Match not finished after Game 1", testEngine.isMatchFinished)

        // Swap a card from main deck with side deck between games
        val oldCardName = testEngine.player.deck[0].name
        val sideCardName = testEngine.player.sideDeck[0].name
        val swapped = testEngine.swapSideDeckCard(isPlayer = true, mainDeckCardIndex = 0, sideDeckCardIndex = 0)
        assertTrue("Side deck card swap succeeded", swapped)
        assertEquals("Side card is now in main deck", sideCardName, testEngine.player.deck[0].name)
        assertEquals("Old main card is now in side deck", oldCardName, testEngine.player.sideDeck[0].name)

        // Advance to Game 2
        val advanced = testEngine.advanceMatchGame()
        assertTrue("Advanced to Game 2", advanced)
        assertEquals("Now at Game 2", 2, testEngine.matchGameNumber)
        assertNull("Winner reset for Game 2", testEngine.winner)
        assertEquals("Player LP reset to 8000", 8000, testEngine.player.lifePoints)
        assertEquals("Opponent LP reset to 8000", 8000, testEngine.opponent.lifePoints)
        assertEquals("Player hand drawn for Game 2", 5, testEngine.player.hand.size)

        // Player wins Game 2 -> Wins the entire Match!
        testEngine.opponent.lifePoints = 0
        testEngine.surrender(isPlayer = false)
        assertEquals("Player score is 2", 2, testEngine.playerMatchWins)
        assertTrue("Match is finished (2 wins reached)", testEngine.isMatchFinished)
        assertEquals("Player is match winner", testEngine.player, testEngine.matchWinner)
    }
}

