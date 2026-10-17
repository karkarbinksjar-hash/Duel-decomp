package com.yugioh.recomp

import com.yugioh.recomp.core.engine.Card
import com.yugioh.recomp.core.engine.CardAttribute
import com.yugioh.recomp.core.engine.CardType
import com.yugioh.recomp.core.engine.MonsterType
import com.yugioh.recomp.core.rom.CardArtworkLoader
import org.junit.Assert.*
import org.junit.Test

class RomFolderScannerTest {

    @Test
    fun testCardArtworkPasscodeResolution() {
        val blueEyes = Card(
            id = 1,
            name = "Blue-Eyes White Dragon",
            atk = 3000,
            def = 2500,
            level = 8,
            attribute = CardAttribute.LIGHT,
            cardType = CardType.MONSTER,
            monsterType = MonsterType.DRAGON,
            password = "89631139"
        )
        assertEquals("89631139", CardArtworkLoader.getPasscode(blueEyes))

        // Card without explicit password should resolve via registry
        val darkMagician = Card(
            id = 2,
            name = "Dark Magician",
            atk = 2500,
            def = 2100,
            level = 7,
            attribute = CardAttribute.DARK,
            cardType = CardType.MONSTER,
            monsterType = MonsterType.SPELLCASTER
        )
        val code = CardArtworkLoader.getPasscode(darkMagician)
        assertNotNull("Dark Magician passcode should resolve via registry", code)
        assertEquals("46986414", code)
    }

    @Test
    fun testNameAliasNormalization() {
        val dianKeto = Card(
            id = 50,
            name = "Dian Keto the Cure Maiden",
            atk = 0,
            def = 0,
            level = 0,
            attribute = CardAttribute.SPELL,
            cardType = CardType.SPELL,
            monsterType = MonsterType.NONE
        )
        val code = CardArtworkLoader.getPasscode(dianKeto)
        assertNotNull("Dian Keto alias should resolve to passcode", code)
    }

    @Test
    fun testSyncStatusCalculation() {
        val sampleCards = listOf(
            Card(id = 1, name = "Blue-Eyes White Dragon", atk = 3000, def = 2500, level = 8, attribute = CardAttribute.LIGHT, cardType = CardType.MONSTER, monsterType = MonsterType.DRAGON, password = "89631139"),
            Card(id = 2, name = "Dark Magician", atk = 2500, def = 2100, level = 7, attribute = CardAttribute.DARK, cardType = CardType.MONSTER, monsterType = MonsterType.SPELLCASTER, password = "46986414")
        )
        val (cached, total) = CardArtworkLoader.getSyncStatus(sampleCards)
        assertEquals(2, total)
        assertTrue(cached >= 0)
    }
}
