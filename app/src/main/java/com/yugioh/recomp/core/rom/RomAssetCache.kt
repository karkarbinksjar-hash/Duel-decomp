package com.yugioh.recomp.core.rom

import com.yugioh.recomp.core.engine.Card

/**
 * In-memory and runtime texture cache for extracted ROM assets.
 * Guarantees zero copyrighted assets bundled with the binary release.
 */
object RomAssetCache {

    var activeTarget: RomTarget? = null
        private set

    var isAssetPipelineReady: Boolean = false
        private set

    private val cardDatabase = mutableMapOf<Int, Card>()
    private val cardArtSprites = mutableMapOf<Int, DecodedSprite>()

    /**
     * Initializes the asset cache from raw extracted ROM data.
     */
    fun populateFromRom(target: RomTarget, romBytes: ByteArray) {
        cardDatabase.clear()
        cardArtSprites.clear()
        activeTarget = target

        // 1. Extract Cards
        val cards = CardDataExtractor.extractCards(romBytes)
        for (card in cards) {
            cardDatabase[card.id] = card
        }

        // 2. Decode Graphics / Procedural or Planar 2bpp Sprite Tiles
        for (card in cards) {
            val sprite = if (target.platform == RomPlatform.GBC && romBytes.size >= 0x4400) {
                // Try decoding tile grid from ROM offset derived from card id
                val tileOffset = (0x4000 + ((card.id * 64) % (romBytes.size - 0x100))).coerceAtMost(romBytes.size - 256)
                GbcTileDecoder.decodeTileGrid(romBytes, tileOffset, 4, 4)
            } else {
                val pixels = GbcTileDecoder.generateProceduralCardArt(card.id.toLong() * 31L + 17L, 32, 32)
                DecodedSprite(32, 32, pixels)
            }
            cardArtSprites[card.id] = sprite
        }

        isAssetPipelineReady = true
    }

    fun getAllCards(): List<Card> = cardDatabase.values.toList()

    fun getCard(id: Int): Card? = cardDatabase[id]

    fun getCardSprite(cardId: Int): DecodedSprite {
        return cardArtSprites[cardId] ?: run {
            val pixels = GbcTileDecoder.generateProceduralCardArt(cardId.toLong() * 41L, 32, 32)
            DecodedSprite(32, 32, pixels).also { cardArtSprites[cardId] = it }
        }
    }

    fun reset() {
        activeTarget = null
        isAssetPipelineReady = false
        cardDatabase.clear()
        cardArtSprites.clear()
    }
}
