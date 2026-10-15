package com.yugioh.recomp.core.engine

import androidx.compose.ui.graphics.Color
import com.yugioh.recomp.ui.OpponentProfile

enum class StarterDeckType(
    val displayName: String,
    val badgeColor: Color,
    val featuredCard: String,
    val description: String
) {
    BLACK("Black Deck", Color(0xFF1E293B), "Armored Lizard", "Earth & Dark beatdown deck featuring Armored Lizard, Summoned Skull, and Raigeki."),
    RED("Red Deck", Color(0xFF991B1B), "Axe Raider", "High-velocity offensive deck featuring Axe Raider, 7 Colored Fish, and Dark Hole."),
    GREEN("Green Deck", Color(0xFF065F46), "Judge Man", "Heavy warrior deck featuring Judge Man, Premature Burial, and Ring of Destruction.")
}

/**
 * Manages the authentic Game Boy Advance (GBA) campaign progression,
 * 5-tier Battle City tournament duelist ladder, 3 authentic EDS Starter Decks,
 * and Trunk inventory for Yu-Gi-Oh! The Eternal Duelist Soul.
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
     * Current selected starter deck type. Default: Black Deck featuring Armored Lizard.
     */
    var currentStarterDeckType: StarterDeckType = StarterDeckType.BLACK
        private set

    /**
     * Current selected opponent for duels.
     */
    var selectedOpponentName: String = "Tristan Taylor"

    /**
     * Authentic 5-Tier Battle City Tournament Opponents from The Eternal Duelist Soul.
     */
    val opponents = listOf(
        // === TIER 1: Domino City Civilians & Apprentices ===
        OpponentProfile(
            name = "Tristan Taylor",
            title = "Domino High Rookie",
            quote = "I may not have fancy cards, but my monsters never quit!",
            avatarColor = Color(0xFF64748B),
            difficulty = "★☆☆☆☆",
            keyCard = "Battle Ox",
            tier = 1,
            winsRequired = 0
        ),
        OpponentProfile(
            name = "Tea Gardner",
            title = "Heart of Friendship",
            quote = "Duel with your heart, and your deck will always answer!",
            avatarColor = Color(0xFFEC4899),
            difficulty = "★☆☆☆☆",
            keyCard = "Mystical Elf",
            tier = 1,
            winsRequired = 0
        ),
        OpponentProfile(
            name = "Yugi Muto",
            title = "Friendship & Faith",
            quote = "Even when things look tough, I trust in my deck and my friends!",
            avatarColor = Color(0xFFEAB308),
            difficulty = "★★☆☆☆",
            keyCard = "Summoned Skull",
            tier = 1,
            winsRequired = 0
        ),
        OpponentProfile(
            name = "Joey Wheeler",
            title = "Underdog Champion",
            quote = "I'm JOEY! Let's see if ya got what it takes to beat my Red-Eyes!",
            avatarColor = Color(0xFFF59E0B),
            difficulty = "★★☆☆☆",
            keyCard = "Red-Eyes B. Dragon",
            tier = 1,
            winsRequired = 0
        ),
        OpponentProfile(
            name = "Bakura Ryou",
            title = "Occult Student",
            quote = "I enjoy playing Duel Monsters... though strange things sometimes happen.",
            avatarColor = Color(0xFF8B5CF6),
            difficulty = "★★☆☆☆",
            keyCard = "Morphing Jar",
            tier = 1,
            winsRequired = 0
        ),

        // === TIER 2: Battle City Tournament Rivals ===
        OpponentProfile(
            name = "Espa Roba",
            title = "Psychic Duelist",
            quote = "My psychic antennae foresee every card in your hand! Behold Jinzo!",
            avatarColor = Color(0xFF06B6D4),
            difficulty = "★★★☆☆",
            keyCard = "Jinzo",
            tier = 2,
            winsRequired = 2
        ),
        OpponentProfile(
            name = "Mako Tsunami",
            title = "Ocean Warrior",
            quote = "The raging tides of the Pacific will wash away your monsters!",
            avatarColor = Color(0xFF0284C7),
            difficulty = "★★★☆☆",
            keyCard = "The Legendary Fisherman",
            tier = 2,
            winsRequired = 2
        ),
        OpponentProfile(
            name = "Rex Raptor",
            title = "Dino Duelist",
            quote = "My prehistoric predators will stomp your puny cards into dust!",
            avatarColor = Color(0xFF84CC16),
            difficulty = "★★★☆☆",
            keyCard = "Two-Headed King Rex",
            tier = 2,
            winsRequired = 2
        ),
        OpponentProfile(
            name = "Weevil Underwood",
            title = "Insect Master",
            quote = "Hehehe! My swarming insects will trap you in an inescapable cocoon!",
            avatarColor = Color(0xFF10B981),
            difficulty = "★★★☆☆",
            keyCard = "Insect Queen",
            tier = 2,
            winsRequired = 2
        ),
        OpponentProfile(
            name = "Mai Valentine",
            title = "Harpie Duelist",
            quote = "Can you withstand the supersonic gusts of my Harpie Lady?",
            avatarColor = Color(0xFFD946EF),
            difficulty = "★★★☆☆",
            keyCard = "Cyber Harpie",
            tier = 2,
            winsRequired = 2
        ),

        // === TIER 3: The Rare Hunters & Dark Duelists ===
        OpponentProfile(
            name = "Lumis and Umbra",
            title = "Masked Ghouls",
            quote = "The Mask of the Accursed will choke the life from your deck!",
            avatarColor = Color(0xFF6366F1),
            difficulty = "★★★★☆",
            keyCard = "Masked Beast Des Gardius",
            tier = 3,
            winsRequired = 3
        ),
        OpponentProfile(
            name = "Arkana",
            title = "Master of Illusions",
            quote = "There is only one true Master of Magicians, and that is ME!",
            avatarColor = Color(0xFFDC2626),
            difficulty = "★★★★☆",
            keyCard = "Dark Magician",
            tier = 3,
            winsRequired = 3
        ),
        OpponentProfile(
            name = "Rare Hunter",
            title = "Exodia Seeker",
            quote = "The forbidden one shall be assembled! You cannot defeat destiny!",
            avatarColor = Color(0xFF78350F),
            difficulty = "★★★★☆",
            keyCard = "Exodia the Forbidden One",
            tier = 3,
            winsRequired = 3
        ),
        OpponentProfile(
            name = "Strings",
            title = "Silent Puppet",
            quote = "...",
            avatarColor = Color(0xFF475569),
            difficulty = "★★★★☆",
            keyCard = "Slifer the Sky Dragon",
            tier = 3,
            winsRequired = 3
        ),
        OpponentProfile(
            name = "Marik Ishtar",
            title = "Tombkeeper of Darkness",
            quote = "Welcome to the Shadow Realm! Embrace eternal agony!",
            avatarColor = Color(0xFF991B1B),
            difficulty = "★★★★☆",
            keyCard = "Lava Golem",
            tier = 3,
            winsRequired = 3
        ),

        // === TIER 4: Millennium Item Holders & Masters ===
        OpponentProfile(
            name = "Yami Bakura",
            title = "Shadow Fiend",
            quote = "The Millennium Ring has awakened... time to send your soul to the graveyard!",
            avatarColor = Color(0xFF7C3AED),
            difficulty = "★★★★★",
            keyCard = "Dark Necrofear",
            tier = 4,
            winsRequired = 4
        ),
        OpponentProfile(
            name = "Ishizu Ishtar",
            title = "Keeper of Destiny",
            quote = "The Millennium Necklace reveals all past, present, and future moves.",
            avatarColor = Color(0xFFFBBF24),
            difficulty = "★★★★★",
            keyCard = "Keldo",
            tier = 4,
            winsRequired = 4
        ),
        OpponentProfile(
            name = "Seto Kaiba",
            title = "KaibaCorp President",
            quote = "You're a third-rate duelist with a fourth-rate deck! Face Blue-Eyes!",
            avatarColor = Color(0xFF2563EB),
            difficulty = "★★★★★",
            keyCard = "Blue-Eyes White Dragon",
            tier = 4,
            winsRequired = 4
        ),
        OpponentProfile(
            name = "Shadi",
            title = "Millennium Guardian",
            quote = "I weigh the balance of your soul upon the Millennium Scales.",
            avatarColor = Color(0xFFF59E0B),
            difficulty = "★★★★★",
            keyCard = "Millennium Shield",
            tier = 4,
            winsRequired = 4
        ),
        OpponentProfile(
            name = "Yami Yugi",
            title = "King of Games",
            quote = "It's time to duel! I believe in the Heart of the Cards!",
            avatarColor = Color(0xFFEAB308),
            difficulty = "★★★★★",
            keyCard = "Dark Magician",
            tier = 4,
            winsRequired = 4
        ),

        // === TIER 5: Supreme Champions & Grandmasters ===
        OpponentProfile(
            name = "Maximillion Pegasus",
            title = "Creator of Duel Monsters",
            quote = "Oh, did you really think your cards could hide from my Millennium Eye?",
            avatarColor = Color(0xFFBE185D),
            difficulty = "★★★★★",
            keyCard = "Relinquished",
            tier = 5,
            winsRequired = 5
        ),
        OpponentProfile(
            name = "Simon Muran",
            title = "Ancient Vizier",
            quote = "Long before cards, ancient shadow games decided the fate of empires.",
            avatarColor = Color(0xFFB45309),
            difficulty = "★★★★★",
            keyCard = "Exodia the Forbidden One",
            tier = 5,
            winsRequired = 5
        ),
        OpponentProfile(
            name = "Duel Computer",
            title = "KaibaCorp Supercomputer",
            quote = "PROCESSING OPTIMAL DUEL PATTERN... VICTORY PROBABILITY: 99.8%.",
            avatarColor = Color(0xFF0284C7),
            difficulty = "★★★★★",
            keyCard = "Cyber Jar",
            tier = 5,
            winsRequired = 5
        ),
        OpponentProfile(
            name = "Solomon Muto",
            title = "Game Shop Master",
            quote = "An old man still has plenty of tricks up his sleeve! Let's duel!",
            avatarColor = Color(0xFFD97706),
            difficulty = "★★★★★",
            keyCard = "Blue-Eyes White Dragon",
            tier = 5,
            winsRequired = 5
        )
    )

    init {
        resetToNewGame()
    }

    /**
     * Initializes a fresh GBA save with the authentic Black Starter Deck.
     */
    fun resetToNewGame() {
        currentStarterDeckType = StarterDeckType.BLACK
        activeDeck.clear()
        activeDeck.addAll(getBlackStarterDeck())
        trunk.clear()
        trunk.addAll(getGbaStarterTrunk())
        opponents.forEach { opponentWins[it.name] = 0 }
    }

    fun selectStarterDeck(type: StarterDeckType) {
        currentStarterDeckType = type
        activeDeck.clear()
        when (type) {
            StarterDeckType.BLACK -> activeDeck.addAll(getBlackStarterDeck())
            StarterDeckType.RED -> activeDeck.addAll(getRedStarterDeck())
            StarterDeckType.GREEN -> activeDeck.addAll(getGreenStarterDeck())
        }
    }

    fun getGbaStarterDeck(): List<Card> = getBlackStarterDeck()

    fun buildGbaOpponentDeck(opponentName: String): List<Card> = buildOpponentDeck(opponentName)

    fun isTierUnlocked(tier: Int): Boolean {
        if (tier <= 1) return true
        val prevTier = tier - 1
        val prevTierOpponents = opponents.filter { it.tier == prevTier }
        val requiredWinsPerOpponent = when (tier) {
            2 -> 2
            3 -> 3
            4 -> 4
            5 -> 5
            else -> 1
        }
        val eachOpponentCleared = prevTierOpponents.all { (opponentWins[it.name] ?: 0) >= requiredWinsPerOpponent }
        val totalWinsThreshold = when (tier) {
            2 -> 10
            3 -> 25
            4 -> 45
            5 -> 70
            else -> 0
        }
        val totalWins = opponentWins.values.sum()
        return eachOpponentCleared || totalWins >= totalWinsThreshold
    }

    /**
     * Authentic Black Starter Deck from The Eternal Duelist Soul.
     * Features Armored Lizard (15480588), Summoned Skull, Sangan, and Raigeki.
     */
    fun getBlackStarterDeck(): List<Card> {
        return listOf(
            GbaCardCatalog.ARMORED_LIZARD,
            GbaCardCatalog.BEAN_SOLDIER,
            GbaCardCatalog.BIG_INSECT,
            GbaCardCatalog.CELTIC_GUARDIAN_GBA,
            GbaCardCatalog.CURSE_OF_DRAGON_GBA,
            GbaCardCatalog.ENCHANTING_MERMAID,
            GbaCardCatalog.FIEND_KRAKEN,
            GbaCardCatalog.FIENDS_HAND,
            GbaCardCatalog.GREAT_WHITE,
            GbaCardCatalog.GRIFFORE,
            GbaCardCatalog.HORN_IMP,
            GbaCardCatalog.ILLUSIONIST_FACELESS_MAGE,
            GbaCardCatalog.KILLER_NEEDLE,
            GbaCardCatalog.MINOMUSHI_WARRIOR,
            GbaCardCatalog.MYSTICAL_ELF,
            GbaCardCatalog.NEO_MAGIC_SWORDSMAN,
            GbaCardCatalog.PETIT_DRAGON,
            GbaCardCatalog.STONE_ARMADILLER,
            GbaCardCatalog.SUMMONED_SKULL_GBA,
            GbaCardCatalog.SUPPORTER_IN_THE_SHADOWS,
            GbaCardCatalog.THE_DRDEK,
            GbaCardCatalog.TOMOZAURUS,
            GbaCardCatalog.TWO_HEADED_KING_REX,
            GbaCardCatalog.WICKED_MIRROR,
            GbaCardCatalog.WINGED_CLEAVER,
            GbaCardCatalog.SANGAN,
            // Spells
            GbaCardCatalog.BLACK_PENDANT,
            GbaCardCatalog.CHANGE_OF_HEART,
            GbaCardCatalog.MEGAMORPH,
            GbaCardCatalog.MONSTER_REBORN,
            GbaCardCatalog.MYSTICAL_SPACE_TYPHOON,
            GbaCardCatalog.POT_OF_GREED,
            GbaCardCatalog.RAIGEKI,
            GbaCardCatalog.SWORDS_OF_REVEALING_LIGHT,
            // Traps
            GbaCardCatalog.MAGIC_JAMMER,
            GbaCardCatalog.MIRROR_FORCE,
            GbaCardCatalog.SEVEN_TOOLS_BANDIT,
            GbaCardCatalog.TRAP_HOLE,
            GbaCardCatalog.TORRENTIAL_TRIBUTE,
            GbaCardCatalog.CALL_OF_THE_HAUNTED
        )
    }

    /**
     * Authentic Red Starter Deck from The Eternal Duelist Soul.
     * Features 7 Colored Fish, Axe Raider, Harpie Lady, and Dark Hole.
     */
    fun getRedStarterDeck(): List<Card> {
        return listOf(
            GbaCardCatalog.SEVEN_COLORED_FISH,
            GbaCardCatalog.ANTHROSAURUS,
            GbaCardCatalog.AXE_RAIDER,
            GbaCardCatalog.BATTLE_STEER,
            GbaCardCatalog.CANDLE_OF_FATE,
            GbaCardCatalog.CELTIC_GUARDIAN_GBA,
            GbaCardCatalog.CRAWLING_DRAGON,
            GbaCardCatalog.CRAWLING_DRAGON_2,
            GbaCardCatalog.DARKWORLD_THORNS,
            GbaCardCatalog.FIEND_KRAKEN,
            GbaCardCatalog.GANIGUMO,
            GbaCardCatalog.GOKIBORE,
            GbaCardCatalog.GREAT_WHITE,
            GbaCardCatalog.HARPIE_LADY_GBA,
            GbaCardCatalog.KAMINARIKOZOU,
            GbaCardCatalog.KOUMORI_DRAGON,
            GbaCardCatalog.LALA_LI_OON,
            GbaCardCatalog.LESSER_DRAGON,
            GbaCardCatalog.MYSTICAL_ELF,
            GbaCardCatalog.SKULL_STALKER,
            GbaCardCatalog.SUMMONED_SKULL_GBA,
            GbaCardCatalog.TAKRIMINOS,
            GbaCardCatalog.THE_DRDEK,
            GbaCardCatalog.TOMOZAURUS,
            GbaCardCatalog.TWO_HEADED_KING_REX,
            GbaCardCatalog.NEEDLE_WORM,
            // Spells
            GbaCardCatalog.BLACK_PENDANT,
            GbaCardCatalog.CHANGE_OF_HEART,
            GbaCardCatalog.DARK_HOLE,
            GbaCardCatalog.MONSTER_REBORN,
            GbaCardCatalog.MYSTICAL_SPACE_TYPHOON,
            GbaCardCatalog.POT_OF_GREED,
            GbaCardCatalog.SNATCH_STEAL,
            GbaCardCatalog.SWORDS_OF_REVEALING_LIGHT,
            // Traps
            GbaCardCatalog.MAGIC_JAMMER,
            GbaCardCatalog.MIRROR_FORCE,
            GbaCardCatalog.SEVEN_TOOLS_BANDIT,
            GbaCardCatalog.TRAP_HOLE,
            GbaCardCatalog.TORRENTIAL_TRIBUTE,
            GbaCardCatalog.CALL_OF_THE_HAUNTED
        )
    }

    /**
     * Authentic Green Starter Deck from The Eternal Duelist Soul.
     * Features Judge Man, Battle Ox, Premature Burial, and Ring of Destruction.
     */
    fun getGreenStarterDeck(): List<Card> {
        return listOf(
            GbaCardCatalog.ARMORED_LIZARD,
            GbaCardCatalog.BATTLE_OX_GBA,
            GbaCardCatalog.CRAWLING_DRAGON_2,
            GbaCardCatalog.DARK_GRAY,
            GbaCardCatalog.FERAL_IMP,
            GbaCardCatalog.FIENDS_HAND,
            GbaCardCatalog.GOKIBORE,
            GbaCardCatalog.GRIFFORE,
            GbaCardCatalog.HITOTSU_ME_GIANT,
            GbaCardCatalog.JUDGE_MAN,
            GbaCardCatalog.LARVAS,
            GbaCardCatalog.MYSTIC_HORSEMAN,
            GbaCardCatalog.MYSTICAL_ELF,
            GbaCardCatalog.ROGUE_DOLL,
            GbaCardCatalog.SNAKEYASHI,
            GbaCardCatalog.STONE_ARMADILLER,
            GbaCardCatalog.SUMMONED_SKULL_GBA,
            GbaCardCatalog.SUPPORTER_IN_THE_SHADOWS,
            GbaCardCatalog.SWORD_ARM_OF_DRAGON,
            GbaCardCatalog.THE_DRDEK,
            GbaCardCatalog.THE_WANDERING_DOOMED,
            GbaCardCatalog.URABY,
            GbaCardCatalog.WICKED_MIRROR,
            GbaCardCatalog.WINGED_CLEAVER,
            GbaCardCatalog.WINGED_DRAGON_GUARDIAN,
            GbaCardCatalog.SANGAN,
            // Spells
            GbaCardCatalog.BLACK_PENDANT,
            GbaCardCatalog.CHANGE_OF_HEART,
            GbaCardCatalog.MONSTER_REBORN,
            GbaCardCatalog.MYSTICAL_SPACE_TYPHOON,
            GbaCardCatalog.POT_OF_GREED,
            GbaCardCatalog.PREMATURE_BURIAL,
            GbaCardCatalog.SWORDS_OF_REVEALING_LIGHT,
            GbaCardCatalog.RAIGEKI,
            // Traps
            GbaCardCatalog.RING_OF_DESTRUCTION,
            GbaCardCatalog.MAGIC_JAMMER,
            GbaCardCatalog.SEVEN_TOOLS_BANDIT,
            GbaCardCatalog.TRAP_HOLE,
            GbaCardCatalog.MIRROR_FORCE,
            GbaCardCatalog.TORRENTIAL_TRIBUTE
        )
    }

    private fun getGbaStarterTrunk(): List<Card> {
        return listOf(
            DdsCardCatalog.BLUE_EYES_WHITE_DRAGON,
            DdsCardCatalog.RED_EYES_B_DRAGON,
            DdsCardCatalog.DARK_MAGICIAN,
            GbaCardCatalog.JINZO,
            GbaCardCatalog.BUSTER_BLADER,
            GbaCardCatalog.DARK_MAGICIAN_GIRL,
            GbaCardCatalog.GEMINI_ELF,
            GbaCardCatalog.VORSE_RAIDER,
            GbaCardCatalog.LA_JINN,
            GbaCardCatalog.GIANT_SOLDIER_OF_STONE,
            GbaCardCatalog.MAN_EATER_BUG,
            GbaCardCatalog.GRACEFUL_CHARITY,
            GbaCardCatalog.HARPIES_FEATHER_DUSTER,
            GbaCardCatalog.HEAVY_STORM,
            GbaCardCatalog.MAGIC_CYLINDER,
            GbaCardCatalog.CYBER_HARPIE,
            GbaCardCatalog.BARREL_DRAGON,
            GbaCardCatalog.RELINQUISHED
        )
    }

    /**
     * Constructs the authentic opponent deck according to the duelist's canonical theme.
     */
    fun buildOpponentDeck(opponentName: String): List<Card> {
        val deck = mutableListOf<Card>()

        when (opponentName) {
            "Joey Wheeler" -> {
                deck.add(DdsCardCatalog.RED_EYES_B_DRAGON)
                deck.add(GbaCardCatalog.ALLIGATORS_SWORD)
                deck.add(GbaCardCatalog.GEARFRIED_IRON_KNIGHT)
                deck.add(GbaCardCatalog.AXE_RAIDER)
                deck.add(GbaCardCatalog.BATTLE_OX_GBA)
                deck.add(GbaCardCatalog.POT_OF_GREED)
                deck.add(GbaCardCatalog.MONSTER_REBORN)
                deck.add(GbaCardCatalog.MIRROR_FORCE)
                deck.add(GbaCardCatalog.TRAP_HOLE)
            }
            "Seto Kaiba" -> {
                deck.add(DdsCardCatalog.BLUE_EYES_WHITE_DRAGON)
                deck.add(DdsCardCatalog.BLUE_EYES_WHITE_DRAGON)
                deck.add(DdsCardCatalog.BLUE_EYES_WHITE_DRAGON)
                deck.add(GbaCardCatalog.VORSE_RAIDER)
                deck.add(GbaCardCatalog.VORSE_RAIDER)
                deck.add(GbaCardCatalog.VORSE_RAIDER)
                deck.add(GbaCardCatalog.LA_JINN)
                deck.add(GbaCardCatalog.LA_JINN)
                deck.add(GbaCardCatalog.POT_OF_GREED)
                deck.add(GbaCardCatalog.RAIGEKI)
                deck.add(GbaCardCatalog.MONSTER_REBORN)
                deck.add(GbaCardCatalog.MIRROR_FORCE)
                deck.add(GbaCardCatalog.TORRENTIAL_TRIBUTE)
            }
            "Yami Yugi" -> {
                deck.add(DdsCardCatalog.DARK_MAGICIAN)
                deck.add(GbaCardCatalog.DARK_MAGICIAN_GIRL)
                deck.add(GbaCardCatalog.BUSTER_BLADER)
                deck.add(GbaCardCatalog.SUMMONED_SKULL_GBA)
                deck.add(GbaCardCatalog.CELTIC_GUARDIAN_GBA)
                deck.add(GbaCardCatalog.MYSTICAL_ELF)
                deck.add(GbaCardCatalog.GIANT_SOLDIER_OF_STONE)
                deck.add(GbaCardCatalog.POT_OF_GREED)
                deck.add(GbaCardCatalog.MONSTER_REBORN)
                deck.add(GbaCardCatalog.SWORDS_OF_REVEALING_LIGHT)
                deck.add(GbaCardCatalog.MAGIC_CYLINDER)
                deck.add(GbaCardCatalog.MIRROR_FORCE)
            }
            "Espa Roba" -> {
                deck.add(GbaCardCatalog.JINZO)
                deck.add(GbaCardCatalog.JINZO)
                deck.add(GbaCardCatalog.GEMINI_ELF)
                deck.add(GbaCardCatalog.VORSE_RAIDER)
                deck.add(GbaCardCatalog.POT_OF_GREED)
                deck.add(GbaCardCatalog.RAIGEKI)
                deck.add(GbaCardCatalog.MONSTER_REBORN)
            }
            "Mai Valentine" -> {
                deck.add(GbaCardCatalog.CYBER_HARPIE)
                deck.add(GbaCardCatalog.CYBER_HARPIE)
                deck.add(GbaCardCatalog.HARPIE_LADY_GBA)
                deck.add(GbaCardCatalog.HARPIE_LADY_GBA)
                deck.add(GbaCardCatalog.HARPIES_FEATHER_DUSTER)
                deck.add(GbaCardCatalog.POT_OF_GREED)
                deck.add(GbaCardCatalog.MONSTER_REBORN)
                deck.add(GbaCardCatalog.MIRROR_FORCE)
            }
            "Rex Raptor" -> {
                deck.add(GbaCardCatalog.TWO_HEADED_KING_REX)
                deck.add(GbaCardCatalog.TWO_HEADED_KING_REX)
                deck.add(GbaCardCatalog.URABY)
                deck.add(GbaCardCatalog.URABY)
                deck.add(GbaCardCatalog.CRAWLING_DRAGON_2)
                deck.add(GbaCardCatalog.POT_OF_GREED)
                deck.add(GbaCardCatalog.RAIGEKI)
            }
            "Weevil Underwood" -> {
                deck.add(GbaCardCatalog.BIG_INSECT)
                deck.add(GbaCardCatalog.KILLER_NEEDLE)
                deck.add(GbaCardCatalog.GOKIBORE)
                deck.add(GbaCardCatalog.NEEDLE_WORM)
                deck.add(GbaCardCatalog.MAN_EATER_BUG)
                deck.add(GbaCardCatalog.POT_OF_GREED)
            }
            "Mako Tsunami" -> {
                deck.add(GbaCardCatalog.SEVEN_COLORED_FISH)
                deck.add(GbaCardCatalog.GREAT_WHITE)
                deck.add(GbaCardCatalog.FIEND_KRAKEN)
                deck.add(GbaCardCatalog.TAKRIMINOS)
                deck.add(GbaCardCatalog.ENCHANTING_MERMAID)
                deck.add(GbaCardCatalog.POT_OF_GREED)
                deck.add(GbaCardCatalog.MONSTER_REBORN)
            }
            "Maximillion Pegasus" -> {
                deck.add(GbaCardCatalog.RELINQUISHED)
                deck.add(GbaCardCatalog.RELINQUISHED)
                deck.add(GbaCardCatalog.SUMMONED_SKULL_GBA)
                deck.add(GbaCardCatalog.GEMINI_ELF)
                deck.add(GbaCardCatalog.VORSE_RAIDER)
                deck.add(GbaCardCatalog.POT_OF_GREED)
                deck.add(GbaCardCatalog.CHANGE_OF_HEART)
                deck.add(GbaCardCatalog.SNATCH_STEAL)
                deck.add(GbaCardCatalog.MIRROR_FORCE)
            }
            else -> {
                deck.add(GbaCardCatalog.GEMINI_ELF)
                deck.add(GbaCardCatalog.VORSE_RAIDER)
                deck.add(GbaCardCatalog.BATTLE_OX_GBA)
                deck.add(GbaCardCatalog.CELTIC_GUARDIAN_GBA)
                deck.add(GbaCardCatalog.MYSTICAL_ELF)
                deck.add(GbaCardCatalog.GIANT_SOLDIER_OF_STONE)
                deck.add(GbaCardCatalog.POT_OF_GREED)
                deck.add(GbaCardCatalog.TRAP_HOLE)
            }
        }

        val filler = listOf(
            GbaCardCatalog.GEMINI_ELF,
            GbaCardCatalog.VORSE_RAIDER,
            GbaCardCatalog.LA_JINN,
            GbaCardCatalog.GEARFRIED_IRON_KNIGHT,
            GbaCardCatalog.MYSTICAL_ELF,
            GbaCardCatalog.GIANT_SOLDIER_OF_STONE,
            GbaCardCatalog.POT_OF_GREED,
            GbaCardCatalog.TRAP_HOLE
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
                "Joey Wheeler" -> GbaCardCatalog.ALLIGATORS_SWORD
                "Seto Kaiba" -> DdsCardCatalog.BLUE_EYES_WHITE_DRAGON
                "Yami Yugi" -> GbaCardCatalog.BUSTER_BLADER
                "Espa Roba" -> GbaCardCatalog.JINZO
                "Mai Valentine" -> GbaCardCatalog.CYBER_HARPIE
                "Rex Raptor" -> GbaCardCatalog.TWO_HEADED_KING_REX
                "Weevil Underwood" -> GbaCardCatalog.BIG_INSECT
                "Mako Tsunami" -> GbaCardCatalog.SEVEN_COLORED_FISH
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
