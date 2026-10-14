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

        // 1. Extract Cards
        val cards = CardDataExtractor.extractCards(romBytes)
        for (card in cards) {
            cardDatabase[card.id] = card
        }

        // 2. Synthesize High-Fidelity Artwork & Full Framed Card Textures
        for (card in cards) {
            // Render beautiful central illustration (64x64)
            val artPixels = CardArtRenderer.renderCardArtwork(card, 64, 64)
            cardArtSprites[card.id] = DecodedSprite(64, 64, artPixels)

            // Render complete Yu-Gi-Oh! card frame (96x140)
            val fullPixels = CardArtRenderer.renderFullCard(card, 96, 140)
            cardFullTextures[card.id] = DecodedSprite(96, 140, fullPixels)
        }

        // 3. Render Card Back
        val backPixels = CardArtRenderer.renderCardBack(96, 140)
        cardBackSprite = DecodedSprite(96, 140, backPixels)

        isAssetPipelineReady = true
    }

    /**
     * Pre-populates the cache with canonical Dark Duel Stories card database
     * so the engine can duel immediately in demo/instant mode without waiting for a ROM.
     */
    fun populateWithCanonicalDatabase() {
        val dummyTarget = RomTarget.DARK_DUEL_STORIES_USA
        populateFromRom(dummyTarget, ByteArray(0))
    }

    fun getAllCards(): List<Card> {
        if (cardDatabase.isEmpty()) {
            populateWithCanonicalDatabase()
        }
        return cardDatabase.values.toList()
    }

    fun getCard(id: Int): Card? {
        if (cardDatabase.isEmpty()) {
            populateWithCanonicalDatabase()
        }
        return cardDatabase[id]
    }

    fun getCardSprite(cardId: Int): DecodedSprite {
        return cardArtSprites[cardId] ?: run {
            val card = getCard(cardId) ?: CardDataExtractor.getCanonicalDdsCardDatabase().firstOrNull { it.id == cardId }
            val pixels = if (card != null) {
                CardArtRenderer.renderCardArtwork(card, 64, 64)
            } else {
                GbcTileDecoder.generateProceduralCardArt(cardId.toLong() * 41L, 64, 64)
            }
            DecodedSprite(64, 64, pixels).also { cardArtSprites[cardId] = it }
        }
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
