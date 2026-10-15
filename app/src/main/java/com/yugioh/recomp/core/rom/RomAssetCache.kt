package com.yugioh.recomp.core.rom

import com.yugioh.recomp.core.engine.Card

/**
 * In-memory and runtime texture cache for extracted ROM assets and high-fidelity card frames.
 * Guarantees zero copyrighted assets bundled with the binary release while ensuring
 * crystal-clear, authentic Yu-Gi-Oh! visual presentation without static or noise artifacts.
 */
object RomAssetCache {

    var activeTarget: RomTarget? = null
        private set

    var isAssetPipelineReady: Boolean = false
        private set

    private val cardDatabase = mutableMapOf<Int, Card>()
    private val cardArtSprites = mutableMapOf<Int, DecodedSprite>()
    private val cardFullTextures = mutableMapOf<Int, DecodedSprite>()
    private var cardBackSprite: DecodedSprite? = null

    /**
     * Initializes the asset cache from raw extracted ROM data.
     */
    fun populateFromRom(target: RomTarget, romBytes: ByteArray) {
        cardDatabase.clear()
        cardArtSprites.clear()
        cardFullTextures.clear()
        activeTarget = target

        // 1. Extract Cards directly from user-supplied ROM bytes
        val cards = DdsRomParser.extractCardsFromRom(romBytes)
        for (card in cards) {
            cardDatabase[card.id] = card
        }

        // 2. Extract and decode 2bpp tiles with elemental palettes from ROM
        for (card in cards) {
            val sprite = DdsRomParser.extractCardSprite(romBytes, card, 64, 64)
            cardArtSprites[card.id] = sprite

            // Render complete Yu-Gi-Oh! card frame (96x140)
            val fullPixels = CardArtRenderer.renderFullCard(card, 96, 140)
            cardFullTextures[card.id] = DecodedSprite(96, 140, fullPixels)
        }

        // 3. Render Card Back
        val backPixels = CardArtRenderer.renderCardBack(96, 140)
        cardBackSprite = DecodedSprite(96, 140, backPixels)

        isAssetPipelineReady = true
    }

    fun getAllCards(): List<Card> = cardDatabase.values.toList()

    fun getCard(id: Int): Card? = cardDatabase[id]

    fun getCardSprite(cardId: Int): DecodedSprite {
        return cardArtSprites[cardId] ?: run {
            val card = getCard(cardId) ?: CardDataExtractor.getCanonicalDdsCardDatabase().firstOrNull { it.id == cardId }
            val pixels = if (card != null) {
                CardArtRenderer.renderCardArtwork(card, 64, 64)
            } else {
                GbcTileDecoder.generateProceduralCardArt(cardId.toLong() * 41L, 64, 64)
            }
            // Crisp integer-upscaled sprite
            val upscaled = CardArtRenderer.upscaleSprite(pixels, 64, 64, scale = 2)
            upscaled.also { cardArtSprites[cardId] = it }
        }
    }

    /**
     * Returns a crisp 3x integer-upscaled retro sprite (192x192) for high-DPI display.
     */
    fun getCardSpriteUpscaled(cardId: Int, scale: Int = 3): DecodedSprite {
        val baseSprite = getCardSprite(cardId)
        val card = getCard(cardId) ?: CardDataExtractor.getCanonicalDdsCardDatabase().firstOrNull { it.id == cardId }
        val rawPixels = if (card != null) {
            CardArtRenderer.renderCardArtwork(card, 64, 64)
        } else {
            baseSprite.pixels
        }
        return CardArtRenderer.upscaleSprite(rawPixels, 64, 64, scale)
    }

    fun getCardFullTexture(cardId: Int): DecodedSprite {
        return cardFullTextures[cardId] ?: run {
            val card = getCard(cardId) ?: CardDataExtractor.getCanonicalDdsCardDatabase().firstOrNull { it.id == cardId }
            if (card != null) {
                val pixels = CardArtRenderer.renderFullCard(card, 96, 140)
                DecodedSprite(96, 140, pixels).also { cardFullTextures[cardId] = it }
            } else {
                getCardSprite(cardId)
            }
        }
    }

    fun getCardBack(): DecodedSprite {
        return cardBackSprite ?: run {
            val pixels = CardArtRenderer.renderCardBack(96, 140)
            DecodedSprite(96, 140, pixels).also { cardBackSprite = it }
        }
    }

    fun reset() {
        activeTarget = null
        isAssetPipelineReady = false
        cardDatabase.clear()
        cardArtSprites.clear()
        cardFullTextures.clear()
        cardBackSprite = null
    }
}
