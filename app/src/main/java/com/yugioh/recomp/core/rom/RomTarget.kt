package com.yugioh.recomp.core.rom

/**
 * Defines the official Yu-Gi-Oh! Game Boy Color and Game Boy Advance target ROMs
 * supported by the decompression, parsing, and reconstruction pipeline.
 */
enum class RomPlatform {
    GBC,
    GBA
}

data class RomTarget(
    val id: String,
    val title: String,
    val platform: RomPlatform,
    val expectedSha1: String,
    val expectedCrc32: String? = null,
    val headerTitle: String,
    val gameCode: String? = null,
    val description: String
) {
    companion object {
        /**
         * Primary base target: Yu-Gi-Oh! Dark Duel Stories (USA) for GBC
         * Canonical No-Intro / Redump SHA-1: 1952d343bdfb5625bd23a8569cde6fd61a911502
         */
        val DARK_DUEL_STORIES_USA = RomTarget(
            id = "dds_usa_gbc",
            title = "Yu-Gi-Oh! Dark Duel Stories (USA)",
            platform = RomPlatform.GBC,
            expectedSha1 = "1952d343bdfb5625bd23a8569cde6fd61a911502",
            expectedCrc32 = "803a56ae",
            headerTitle = "YU-GI-OH! DDS",
            gameCode = "BY3E",
            description = "Game Boy Color entry point - 800 cards, 2bpp monster artwork, classic duel rules."
        )

        /**
         * Regional variant: Yu-Gi-Oh! Dark Duel Stories (Europe) for GBC
         */
        val DARK_DUEL_STORIES_EUR = RomTarget(
            id = "dds_eur_gbc",
            title = "Yu-Gi-Oh! Dark Duel Stories (Europe)",
            platform = RomPlatform.GBC,
            expectedSha1 = "782b88b1bf9768a2b1b139177a0c1c33",
            expectedCrc32 = "ba7182c3",
            headerTitle = "YU-GI-OH! DDS",
            gameCode = "BY3P",
            description = "European multi-language GBC release."
        )

        /**
         * GBA Expansion Target: Yu-Gi-Oh! The Eternal Duelist Soul (USA)
         */
        val ETERNAL_DUELIST_SOUL_USA = RomTarget(
            id = "eds_usa_gba",
            title = "Yu-Gi-Oh! The Eternal Duelist Soul (USA)",
            platform = RomPlatform.GBA,
            expectedSha1 = "510fbba212aca9bab95ea12f8fd933e62ee34dea",
            expectedCrc32 = "dfd07a36",
            headerTitle = "YUGIOH EDS",
            gameCode = "AYCE",
            description = "GBA expansion target - 4bpp/8bpp higher resolution card artwork and audio tables."
        )

        /**
         * GBA Expansion Target: Yu-Gi-Oh! Worldwide Edition (USA)
         */
        val WORLDWIDE_EDITION_USA = RomTarget(
            id = "wwe_usa_gba",
            title = "Yu-Gi-Oh! Worldwide Edition: Stairway to the Destined Duel (USA)",
            platform = RomPlatform.GBA,
            expectedSha1 = "1e52d66f5f8a7c4f207d80ba4fccf55e",
            expectedCrc32 = "39b5e771",
            headerTitle = "YUGIOH WWE",
            gameCode = "AYWE",
            description = "GBA multi-region expansion target - expanded card pool and battle animations."
        )

        val ALL_TARGETS = listOf(
            DARK_DUEL_STORIES_USA,
            DARK_DUEL_STORIES_EUR,
            ETERNAL_DUELIST_SOUL_USA,
            WORLDWIDE_EDITION_USA
        )
    }
}
