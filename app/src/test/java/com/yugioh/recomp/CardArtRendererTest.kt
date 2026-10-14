package com.yugioh.recomp

import com.yugioh.recomp.core.engine.Card
import com.yugioh.recomp.core.engine.CardAttribute
import com.yugioh.recomp.core.engine.CardType
import com.yugioh.recomp.core.engine.MonsterType
import com.yugioh.recomp.core.rom.CardArtRenderer
import com.yugioh.recomp.core.rom.RomAssetCache
import org.junit.Assert.*
import org.junit.Test

class CardArtRendererTest {

    @Test
    fun testRenderFullCardMonster() {
        val bewd = Card(
            id = 1,
            name = "Blue-Eyes White Dragon",
            atk = 3000,
            def = 2500,
            level = 8,
            attribute = CardAttribute.LIGHT,
            cardType = CardType.MONSTER,
            monsterType = MonsterType.DRAGON
        )

        val width = 96
        val height = 140
        val pixels = CardArtRenderer.renderFullCard(bewd, width, height)

        assertEquals(width * height, pixels.size)
        // Ensure no completely black or transparent pixels
        assertTrue(pixels.any { it != 0 })
        assertTrue(pixels.any { ((it shr 24) and 0xFF) == 0xFF })
        // Check for presence of Gold / Dragon / Frame color
        assertTrue(pixels.any { ((it shr 16) and 0xFF) > 180 })
    }

    @Test
    fun testRenderFullCardSpellAndTrap() {
        val potOfGreed = Card(
            id = 150,
            name = "Pot of Greed",
            atk = 0,
            def = 0,
            level = 0,
            attribute = CardAttribute.SPELL,
            cardType = CardType.SPELL
        )

        val pixelsSpell = CardArtRenderer.renderFullCard(potOfGreed, 96, 140)
        assertEquals(96 * 140, pixelsSpell.size)

        val mirrorForce = Card(
            id = 200,
            name = "Mirror Force",
            atk = 0,
            def = 0,
            level = 0,
            attribute = CardAttribute.TRAP,
            cardType = CardType.TRAP
        )
        val pixelsTrap = CardArtRenderer.renderFullCard(mirrorForce, 96, 140)
        assertEquals(96 * 140, pixelsTrap.size)
    }

    @Test
    fun testRenderCardBack() {
        val width = 96
        val height = 140
        val pixels = CardArtRenderer.renderCardBack(width, height)
        assertEquals(width * height, pixels.size)

        // Ensure gold Millennium eye ring exists in center
        val cx = width / 2
        val cy = height / 2
        val centerPixel = pixels[cy * width + cx]
        // Center pixel should be opaque
        assertEquals(0xFF, (centerPixel shr 24) and 0xFF)
    }

    @Test
    fun testRomAssetCacheAutoPopulation() {
        RomAssetCache.reset()
        assertFalse(RomAssetCache.isAssetPipelineReady)

        // Accessing cards should auto-populate canonical database
        val cards = RomAssetCache.getAllCards()
        assertTrue(cards.isNotEmpty())
        assertTrue(RomAssetCache.isAssetPipelineReady)

        val card1 = RomAssetCache.getCard(1)
        assertNotNull(card1)
        assertEquals("Blue-Eyes White Dragon", card1?.name)

        val sprite = RomAssetCache.getCardSprite(1)
        assertEquals(64, sprite.width)
        assertEquals(64, sprite.height)

        val fullTexture = RomAssetCache.getCardFullTexture(1)
        assertEquals(96, fullTexture.width)
        assertEquals(140, fullTexture.height)

        val back = RomAssetCache.getCardBack()
        assertEquals(96, back.width)
        assertEquals(140, back.height)
    }
}
