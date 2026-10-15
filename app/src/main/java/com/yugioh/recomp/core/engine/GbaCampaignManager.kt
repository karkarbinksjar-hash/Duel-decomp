package com.yugioh.recomp.core.engine

import androidx.compose.ui.graphics.Color
import com.yugioh.recomp.ui.OpponentProfile

/**
 * Manages the authentic Game Boy Advance (GBA) campaign progression,
 * Battle City tournament tiers, official TCG 40-60 card decks, and Trunk inventory
 * for Yu-Gi-Oh! The Eternal Duelist Soul and Worldwide Edition.
 */
object GbaCampaignManager {

    /**
     * Active 40-card Deck currently used in GBA Duels.
     */
    val activeDeck = mutableListOf<Card>()

    /**
     * Reserve Trunk / Chest of all unlocked GBA cards.
     */
    val trunk = mutableListOf<Card>()

    /**
     * Win records against Battle City opponents.
     */
    val opponentWins = mutableMapOf<String, Int>()

    /**
     * Current selected opponent for duels.
     */
    var selectedOpponentName: String = "Joey Wheeler"

    /**
     * Authentic Battle City Tournament Opponents from The Eternal Duelist Soul / Worldwide Edition.
     */
    val opponents = listOf(
        OpponentProfile(
            name = "Joey Wheeler",
            title = "Underdog Champion",
            quote = "I'm JOEY! Let's see if ya got what it takes to beat my Red-Eyes!",
            avatarColor = Color(0xFFF59E0B),
            difficulty = "★★☆☆☆",
            keyCard = "Red-Eyes B. Dragon"
        ),
        OpponentProfile(
            name = "Tristan Taylor",
            title = "Domino High Rookie",
            quote = "I may not have fancy cards, but my monsters never quit!",
            avatarColor = Color(0xFF64748B),
            difficulty = "★☆☆☆☆",
            keyCard = "Battle Ox"
        ),
        OpponentProfile(
            name = "Tea Gardner",
            title = "Heart of Friendship",
            quote = "Duel with your heart, and your deck will always answer!",
            avatarColor = Color(0xFFEC4899),
            difficulty = "★☆☆☆☆",
            keyCard = "Mystical Elf"
        ),
        OpponentProfile(
            name = "Mai Valentine",
            title = "Harpie Duelist",
            quote = "Can you withstand the supersonic gusts of my Harpie Lady?",
            avatarColor = Color(0xFFD946EF),
            difficulty = "★★★☆☆",
            keyCard = "Cyber Harpie"
        ),
        OpponentProfile(
            name = "Bakura Ryou",
            title = "Occult Shadow",
            quote = "The Millennium Ring has awakened... prepare for the Shadows!",
            avatarColor = Color(0xFF8B5CF6),
            difficulty = "★★★☆☆",
            keyCard = "Morphing Jar"
        ),
        OpponentProfile(
            name = "Bandit Keith",
            title = "American Champion",
            quote = "In America, machines crush everything in their path! Play ball!",
            avatarColor = Color(0xFFEF4444),
            difficulty = "★★★★☆",
            keyCard = "Barrel Dragon"
        ),
        OpponentProfile(
            name = "Seto Kaiba",
            title = "KaibaCorp President",
            quote = "You're a third-rate duelist with a fourth-rate deck! Face Blue-Eyes!",
            avatarColor = Color(0xFF2563EB),
            difficulty = "★★★★★",
            keyCard = "Blue-Eyes White Dragon"
        ),
        OpponentProfile(
            name = "Maximillion Pegasus",
            title = "Creator of Duel Monsters",
            quote = "Oh, did you really think your cards could hide from my Millennium Eye?",
            avatarColor = Color(0xFFBE185D),
            difficulty = "★★★★★",
            keyCard = "Relinquished"
        ),
        OpponentProfile(
            name = "Yami Yugi",
            title = "King of Games",
            quote = "It's time to duel! I believe in the Heart of the Cards!",
            avatarColor = Color(0xFFEAB308),
            difficulty = "★★★★★",
            keyCard = "Dark Magician"
        ),
        OpponentProfile(
            name = "Marik Ishtar",
            title = "Tombkeeper of Darkness",
            quote = "Welcome to the Shadow Realm! Embrace eternal agony!",
            avatarColor = Color(0xFF991B1B),
            difficulty = "★★★★★",
            keyCard = "Raigeki"
        )
    )

    init {
        resetToNewGame()
    }

    /**
     * Initializes a fresh GBA save with the authentic GBA Starter Deck.
     */
    fun resetToNewGame() {
        activeDeck.clear()
        activeDeck.addAll(getGbaStarterDeck())
        trunk.clear()
        trunk.addAll(getGbaStarterTrunk())
        opponents.forEach { opponentWins[it.name] = 0 }
    }

    /**
     * Canonical 40-Card GBA Starter Deck for The Eternal Duelist Soul / Worldwide Edition.
     * Features balanced TCG ratios: Bosses, Level 4 Beaters, Walls, Power Spells, and Traps.
     */
    fun getGbaStarterDeck(): List<Card> {
        val deck = mutableListOf<Card>()

        // Tribute Monsters (Bosses) - 5
        deck.add(GbaCardCatalog.JINZO)
        deck.add(GbaCardCatalog.SUMMONED_SKULL_GBA)
        deck.add(DdsCardCatalog.DARK_MAGICIAN)
        deck.add(GbaCardCatalog.BUSTER_BLADER)
        deck.add(GbaCardCatalog.DARK_MAGICIAN_GIRL)

        // Level 4 Attack Powerhouses (1800 - 1900 ATK) - 10
        deck.add(GbaCardCatalog.GEMINI_ELF)
        deck.add(GbaCardCatalog.GEMINI_ELF)
        deck.add(GbaCardCatalog.VORSE_RAIDER)
        deck.add(GbaCardCatalog.VORSE_RAIDER)
        deck.add(GbaCardCatalog.GEARFRIED_IRON_KNIGHT)
        deck.add(GbaCardCatalog.GEARFRIED_IRON_KNIGHT)
        deck.add(GbaCardCatalog.LA_JINN)
        deck.add(GbaCardCatalog.LA_JINN)
        deck.add(DdsCardCatalog.BATTLE_OX)
        deck.add(DdsCardCatalog.CELTIC_GUARDIAN)

        // Defensive Walls & Utility Monsters - 7
        deck.add(GbaCardCatalog.MYSTICAL_ELF)
        deck.add(GbaCardCatalog.MYSTICAL_ELF)
        deck.add(GbaCardCatalog.GIANT_SOLDIER_OF_STONE)
        deck.add(GbaCardCatalog.GIANT_SOLDIER_OF_STONE)
        deck.add(GbaCardCatalog.MAN_EATER_BUG)
        deck.add(GbaCardCatalog.MAN_EATER_BUG)
        deck.add(DdsCardCatalog.KURIBOH)

        // Essential TCG Spells - 10
        deck.add(DdsCardCatalog.POT_OF_GREED)
        deck.add(DdsCardCatalog.RAIGEKI)
        deck.add(DdsCardCatalog.DARK_HOLE)
        deck.add(GbaCardCatalog.MONSTER_REBORN)
        deck.add(GbaCardCatalog.CHANGE_OF_HEART)
        deck.add(GbaCardCatalog.SNATCH_STEAL)
        deck.add(GbaCardCatalog.SWORDS_OF_REVEALING_LIGHT)
        deck.add(GbaCardCatalog.GRACEFUL_CHARITY)
        deck.add(GbaCardCatalog.HARPIES_FEATHER_DUSTER)
        deck.add(DdsCardCatalog.DIAN_KETO)

        // Iconic TCG Traps - 8
        deck.add(DdsCardCatalog.MIRROR_FORCE)
        deck.add(DdsCardCatalog.TRAP_HOLE)
        deck.add(DdsCardCatalog.TRAP_HOLE)
        deck.add(GbaCardCatalog.MAGIC_CYLINDER)
        deck.add(GbaCardCatalog.CALL_OF_THE_HAUNTED)
        deck.add(GbaCardCatalog.IMPERIAL_ORDER)
        deck.add(DdsCardCatalog.WABOKU)
        deck.add(GbaCardCatalog.TORRENTIAL_TRIBUTE)

        return deck
    }

    private fun getGbaStarterTrunk(): List<Card> {
        return listOf(
            DdsCardCatalog.BLUE_EYES_WHITE_DRAGON,
            DdsCardCatalog.RED_EYES_B_DRAGON,
            GbaCardCatalog.CYBER_HARPIE,
            GbaCardCatalog.BARREL_DRAGON,
            GbaCardCatalog.RELINQUISHED,
            GbaCardCatalog.ALLIGATORS_SWORD,
            GbaCardCatalog.HEAVY_STORM,
            DdsCardCatalog.TIME_WIZARD,
            DdsCardCatalog.BABY_DRAGON,
            DdsCardCatalog.HARPIE_LADY
        )
    }

    /**
     * Builds authentic GBA opponent decks reflecting their anime & Battle City strategies.
     */
    fun buildGbaOpponentDeck(opponentName: String): List<Card> {
        val deck = mutableListOf<Card>()

        when (opponentName) {
            "Joey Wheeler" -> {
                deck.add(DdsCardCatalog.RED_EYES_B_DRAGON)
                deck.add(GbaCardCatalog.JINZO)
                deck.add(GbaCardCatalog.GEARFRIED_IRON_KNIGHT)
                deck.add(GbaCardCatalog.GEARFRIED_IRON_KNIGHT)
                deck.add(GbaCardCatalog.ALLIGATORS_SWORD)
                deck.add(GbaCardCatalog.ALLIGATORS_SWORD)
                deck.add(DdsCardCatalog.TIME_WIZARD)
                deck.add(DdsCardCatalog.BABY_DRAGON)
                deck.add(DdsCardCatalog.BATTLE_OX)
                deck.add(DdsCardCatalog.CELTIC_GUARDIAN)
                deck.add(GbaCardCatalog.LA_JINN)
                deck.add(DdsCardCatalog.POT_OF_GREED)
                deck.add(GbaCardCatalog.GRACEFUL_CHARITY)
                deck.add(DdsCardCatalog.MIRROR_FORCE)
                deck.add(DdsCardCatalog.TRAP_HOLE)
                deck.add(DdsCardCatalog.WABOKU)
            }
            "Seto Kaiba" -> {
                deck.add(DdsCardCatalog.BLUE_EYES_WHITE_DRAGON)
                deck.add(DdsCardCatalog.BLUE_EYES_WHITE_DRAGON)
                deck.add(DdsCardCatalog.BLUE_EYES_WHITE_DRAGON)
                deck.add(GbaCardCatalog.VORSE_RAIDER)
                deck.add(GbaCardCatalog.VORSE_RAIDER)
                deck.add(GbaCardCatalog.VORSE_RAIDER)
                deck.add(DdsCardCatalog.BATTLE_OX)
                deck.add(DdsCardCatalog.BATTLE_OX)
                deck.add(GbaCardCatalog.LA_JINN)
                deck.add(GbaCardCatalog.LA_JINN)
                deck.add(DdsCardCatalog.POT_OF_GREED)
                deck.add(DdsCardCatalog.RAIGEKI)
                deck.add(GbaCardCatalog.MONSTER_REBORN)
                deck.add(DdsCardCatalog.MIRROR_FORCE)
                deck.add(GbaCardCatalog.TORRENTIAL_TRIBUTE)
            }
            "Yami Yugi" -> {
                deck.add(DdsCardCatalog.DARK_MAGICIAN)
                deck.add(GbaCardCatalog.DARK_MAGICIAN_GIRL)
                deck.add(GbaCardCatalog.BUSTER_BLADER)
                deck.add(GbaCardCatalog.SUMMONED_SKULL_GBA)
                deck.add(DdsCardCatalog.CELTIC_GUARDIAN)
                deck.add(GbaCardCatalog.MYSTICAL_ELF)
                deck.add(GbaCardCatalog.GIANT_SOLDIER_OF_STONE)
                deck.add(DdsCardCatalog.KURIBOH)
                deck.add(DdsCardCatalog.POT_OF_GREED)
                deck.add(GbaCardCatalog.MONSTER_REBORN)
                deck.add(GbaCardCatalog.SWORDS_OF_REVEALING_LIGHT)
                deck.add(GbaCardCatalog.MAGIC_CYLINDER)
                deck.add(DdsCardCatalog.MIRROR_FORCE)
            }
            "Mai Valentine" -> {
                deck.add(GbaCardCatalog.CYBER_HARPIE)
                deck.add(GbaCardCatalog.CYBER_HARPIE)
                deck.add(DdsCardCatalog.HARPIE_LADY)
                deck.add(DdsCardCatalog.HARPIE_LADY)
                deck.add(DdsCardCatalog.SONIC_MAID)
                deck.add(DdsCardCatalog.SONIC_MAID)
                deck.add(GbaCardCatalog.HARPIES_FEATHER_DUSTER)
                deck.add(DdsCardCatalog.POT_OF_GREED)
                deck.add(GbaCardCatalog.MONSTER_REBORN)
                deck.add(DdsCardCatalog.MIRROR_FORCE)
                deck.add(DdsCardCatalog.WABOKU)
            }
            "Bandit Keith" -> {
                deck.add(GbaCardCatalog.BARREL_DRAGON)
                deck.add(GbaCardCatalog.BARREL_DRAGON)
                deck.add(GbaCardCatalog.JINZO)
                deck.add(GbaCardCatalog.VORSE_RAIDER)
                deck.add(GbaCardCatalog.LA_JINN)
                deck.add(DdsCardCatalog.CYBER_SOLDIER)
                deck.add(GbaCardCatalog.HEAVY_STORM)
                deck.add(DdsCardCatalog.RAIGEKI)
                deck.add(DdsCardCatalog.TRAP_HOLE)
                deck.add(DdsCardCatalog.MIRROR_FORCE)
            }
            "Maximillion Pegasus" -> {
                deck.add(GbaCardCatalog.RELINQUISHED)
                deck.add(GbaCardCatalog.RELINQUISHED)
                deck.add(GbaCardCatalog.SUMMONED_SKULL_GBA)
                deck.add(GbaCardCatalog.GEMINI_ELF)
                deck.add(GbaCardCatalog.VORSE_RAIDER)
                deck.add(DdsCardCatalog.POT_OF_GREED)
                deck.add(GbaCardCatalog.CHANGE_OF_HEART)
                deck.add(GbaCardCatalog.SNATCH_STEAL)
                deck.add(DdsCardCatalog.MIRROR_FORCE)
            }
            else -> {
                // Default / Tristan / Tea balanced deck
                deck.add(GbaCardCatalog.GEMINI_ELF)
                deck.add(GbaCardCatalog.VORSE_RAIDER)
                deck.add(DdsCardCatalog.BATTLE_OX)
                deck.add(DdsCardCatalog.CELTIC_GUARDIAN)
                deck.add(GbaCardCatalog.MYSTICAL_ELF)
                deck.add(GbaCardCatalog.GIANT_SOLDIER_OF_STONE)
                deck.add(DdsCardCatalog.POT_OF_GREED)
                deck.add(DdsCardCatalog.DIAN_KETO)
                deck.add(DdsCardCatalog.TRAP_HOLE)
                deck.add(DdsCardCatalog.WABOKU)
            }
        }

        // Fill up to 40 cards with competitive staples
        val filler = listOf(
            GbaCardCatalog.GEMINI_ELF,
            GbaCardCatalog.VORSE_RAIDER,
            GbaCardCatalog.LA_JINN,
            GbaCardCatalog.GEARFRIED_IRON_KNIGHT,
            GbaCardCatalog.MYSTICAL_ELF,
            GbaCardCatalog.GIANT_SOLDIER_OF_STONE,
            DdsCardCatalog.POT_OF_GREED,
            DdsCardCatalog.TRAP_HOLE,
            DdsCardCatalog.WABOKU
        )
        var fillerIdx = 0
        while (deck.size < 40) {
            deck.add(filler[fillerIdx % filler.size])
            fillerIdx++
        }

        return deck
    }

    /**
     * Handles victory against a GBA opponent and rewards a rare card drop.
     */
    fun recordDuelResult(opponentName: String, won: Boolean): Card? {
        if (won) {
            opponentWins[opponentName] = (opponentWins[opponentName] ?: 0) + 1
            val drop = when (opponentName) {
                "Joey Wheeler" -> GbaCardCatalog.JINZO
                "Seto Kaiba" -> DdsCardCatalog.BLUE_EYES_WHITE_DRAGON
                "Yami Yugi" -> GbaCardCatalog.BUSTER_BLADER
                "Mai Valentine" -> GbaCardCatalog.CYBER_HARPIE
                "Bandit Keith" -> GbaCardCatalog.BARREL_DRAGON
                "Maximillion Pegasus" -> GbaCardCatalog.RELINQUISHED
                else -> GbaCardCatalog.GEMINI_ELF
            }
            trunk.add(drop)
            return drop
        }
        return null
    }

    fun addCardToDeck(card: Card): Boolean {
        if (activeDeck.size >= 60) return false
        val copies = activeDeck.count { it.id == card.id }
        if (copies >= 3) return false
        val idx = trunk.indexOfFirst { it.id == card.id }
        if (idx != -1) {
            val moved = trunk.removeAt(idx)
            activeDeck.add(moved)
            return true
        }
        return false
    }

    fun removeCardFromDeck(card: Card): Boolean {
        if (activeDeck.size <= 40 && activeDeck.none { it.id == card.id }) return false
        val idx = activeDeck.indexOfFirst { it.id == card.id }
        if (idx != -1) {
            val moved = activeDeck.removeAt(idx)
            trunk.add(moved)
            return true
        }
        return false
    }
}
