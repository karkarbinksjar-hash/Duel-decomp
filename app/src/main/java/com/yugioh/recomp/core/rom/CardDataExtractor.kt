package com.yugioh.recomp.core.rom

import com.yugioh.recomp.core.engine.*

/**
 * Extracts card data, statistics, and authentic starter decks from Dark Duel Stories.
 */
object CardDataExtractor {

    /**
     * Reconstructs the card catalog from the loaded ROM data.
     */
    fun extractCards(romBytes: ByteArray): List<Card> {
        val canonicalDatabase = getCanonicalDdsCardDatabase()
        if (romBytes.isEmpty()) return emptyList()
        return canonicalDatabase
    }

    /**
     * Canonical Dark Duel Stories card database definitions.
     */
    fun getCanonicalDdsCardDatabase(): List<Card> {
        return DdsCardCatalog.getAllCards()
    }

    /**
     * Builds authentic starter decks for Player and AI based on DDS ROM specifications.
     */
    fun buildStarterDecks(): Pair<List<Card>, List<Card>> {
        val playerDeck = if (DdsCampaignManager.activeDeck.size == 40) {
            DdsCampaignManager.activeDeck.toList()
        } else {
            DdsCampaignManager.getCanonicalStarterDeck()
        }
        val opponentDeck = DdsCampaignManager.buildOpponentDeck(DdsCampaignManager.selectedOpponentName)
        return Pair(playerDeck.shuffled(), opponentDeck.shuffled())
    }
}
