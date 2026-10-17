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
     * Starchips currency for unlocking cards via the Password Terminal.
     */
    var starchips: Int = 50

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

    val activeSideDeck = mutableListOf<Card>()

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
        activeSideDeck.clear()
        activeSideDeck.addAll(getStarterSideDeck())
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
        activeSideDeck.clear()
        activeSideDeck.addAll(getStarterSideDeck())
    }

    fun getStarterSideDeck(): List<Card> = listOf(
        GbaCardCatalog.MYSTICAL_SPACE_TYPHOON,
        GbaCardCatalog.WABOKU_GBA,
        GbaCardCatalog.MAGIC_CYLINDER,
        GbaCardCatalog.DIAN_KETO_GBA,
        GbaCardCatalog.TRAP_HOLE,
        GbaCardCatalog.SEVEN_TOOLS_BANDIT,
        GbaCardCatalog.MAGIC_JAMMER,
        GbaCardCatalog.SKELENGEL,
        GbaCardCatalog.AQUA_MADOOR,
        GbaCardCatalog.BIG_EYE,
        GbaCardCatalog.CYBER_FALCON,
        GbaCardCatalog.FERAL_IMP,
        GbaCardCatalog.DARK_GRAY,
        GbaCardCatalog.BEAVER_WARRIOR,
        GbaCardCatalog.SKULL_DICE
    )

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
     * Constructs the authentic opponent deck according to the duelist's canonical theme in The Eternal Duelist Soul.
     * Tier 1 duelists use beginner rookie decks with normal monsters (600-1400 ATK, at most 1 key card)
     * so that starter decks can battle and progress fairly with real tactical card interactions.
     */
    fun buildOpponentDeck(opponentName: String): List<Card> {
        val deck = mutableListOf<Card>()

        when (opponentName) {
            "Tristan Taylor" -> {
                // Authentic EDS Rookie "Cookie Deck": weak normal monsters, defensive walls, and at most 1 Battle Ox
                deck.addAll(listOf(
                    GbaCardCatalog.AQUA_MADOOR, GbaCardCatalog.AQUA_MADOOR,
                    GbaCardCatalog.CYBER_FALCON, GbaCardCatalog.CYBER_FALCON, GbaCardCatalog.CYBER_FALCON,
                    GbaCardCatalog.FERAL_IMP, GbaCardCatalog.FERAL_IMP, GbaCardCatalog.FERAL_IMP,
                    GbaCardCatalog.HITOTSU_ME_GIANT, GbaCardCatalog.HITOTSU_ME_GIANT, GbaCardCatalog.HITOTSU_ME_GIANT,
                    GbaCardCatalog.BIG_EYE, GbaCardCatalog.BIG_EYE, GbaCardCatalog.BIG_EYE,
                    GbaCardCatalog.BEAN_SOLDIER, GbaCardCatalog.BEAN_SOLDIER, GbaCardCatalog.BEAN_SOLDIER,
                    GbaCardCatalog.MINOMUSHI_WARRIOR, GbaCardCatalog.MINOMUSHI_WARRIOR, GbaCardCatalog.MINOMUSHI_WARRIOR,
                    GbaCardCatalog.SKELENGEL, GbaCardCatalog.SKELENGEL, GbaCardCatalog.SKELENGEL,
                    GbaCardCatalog.GRIFFORE, GbaCardCatalog.GRIFFORE, GbaCardCatalog.GRIFFORE,
                    GbaCardCatalog.DARK_GRAY, GbaCardCatalog.DARK_GRAY, GbaCardCatalog.DARK_GRAY,
                    GbaCardCatalog.TOMOZAURUS, GbaCardCatalog.TOMOZAURUS,
                    GbaCardCatalog.PETIT_DRAGON, GbaCardCatalog.PETIT_DRAGON,
                    GbaCardCatalog.BATTLE_OX_GBA, // Key card (1 copy max)
                    GbaCardCatalog.POT_OF_GREED,
                    GbaCardCatalog.SWORDS_OF_REVEALING_LIGHT,
                    GbaCardCatalog.TRAP_HOLE, GbaCardCatalog.TRAP_HOLE,
                    GbaCardCatalog.WABOKU_GBA, GbaCardCatalog.WABOKU_GBA
                ))
            }
            "Tea Gardner" -> {
                // Authentic EDS Fairy & Friendship Deck: modest attack female/fairy monsters with LP recovery
                deck.addAll(listOf(
                    GbaCardCatalog.ENCHANTING_MERMAID, GbaCardCatalog.ENCHANTING_MERMAID, GbaCardCatalog.ENCHANTING_MERMAID,
                    GbaCardCatalog.SHINING_FRIENDSHIP, GbaCardCatalog.SHINING_FRIENDSHIP, GbaCardCatalog.SHINING_FRIENDSHIP,
                    GbaCardCatalog.PETIT_ANGEL, GbaCardCatalog.PETIT_ANGEL, GbaCardCatalog.PETIT_ANGEL,
                    GbaCardCatalog.HAPPY_LOVER, GbaCardCatalog.HAPPY_LOVER, GbaCardCatalog.HAPPY_LOVER,
                    GbaCardCatalog.MYSTICAL_ELF, GbaCardCatalog.MYSTICAL_ELF, GbaCardCatalog.MYSTICAL_ELF,
                    GbaCardCatalog.ROGUE_DOLL, GbaCardCatalog.ROGUE_DOLL,
                    GbaCardCatalog.LALA_LI_OON, GbaCardCatalog.LALA_LI_OON, GbaCardCatalog.LALA_LI_OON,
                    GbaCardCatalog.WICKED_MIRROR, GbaCardCatalog.WICKED_MIRROR, GbaCardCatalog.WICKED_MIRROR,
                    GbaCardCatalog.CANDLE_OF_FATE, GbaCardCatalog.CANDLE_OF_FATE, GbaCardCatalog.CANDLE_OF_FATE,
                    GbaCardCatalog.SKELENGEL, GbaCardCatalog.SKELENGEL, GbaCardCatalog.SKELENGEL,
                    GbaCardCatalog.DIAN_KETO_GBA, GbaCardCatalog.DIAN_KETO_GBA, GbaCardCatalog.DIAN_KETO_GBA,
                    GbaCardCatalog.POT_OF_GREED,
                    GbaCardCatalog.WABOKU_GBA, GbaCardCatalog.WABOKU_GBA, GbaCardCatalog.WABOKU_GBA,
                    GbaCardCatalog.CHANGE_OF_HEART,
                    GbaCardCatalog.TRAP_HOLE, GbaCardCatalog.TRAP_HOLE, GbaCardCatalog.TRAP_HOLE
                ))
            }
            "Yugi Muto" -> {
                // Authentic EDS Beginner Yugi Deck: Classic spellcaster/warrior beatdown with signature boss monsters
                deck.addAll(listOf(
                    GbaCardCatalog.CELTIC_GUARDIAN_GBA, GbaCardCatalog.CELTIC_GUARDIAN_GBA, GbaCardCatalog.CELTIC_GUARDIAN_GBA,
                    GbaCardCatalog.WINGED_DRAGON_GUARDIAN, GbaCardCatalog.WINGED_DRAGON_GUARDIAN, GbaCardCatalog.WINGED_DRAGON_GUARDIAN,
                    GbaCardCatalog.BEAVER_WARRIOR, GbaCardCatalog.BEAVER_WARRIOR, GbaCardCatalog.BEAVER_WARRIOR,
                    GbaCardCatalog.FERAL_IMP, GbaCardCatalog.FERAL_IMP, GbaCardCatalog.FERAL_IMP,
                    GbaCardCatalog.HITOTSU_ME_GIANT, GbaCardCatalog.HITOTSU_ME_GIANT, GbaCardCatalog.HITOTSU_ME_GIANT,
                    GbaCardCatalog.MYSTICAL_ELF, GbaCardCatalog.MYSTICAL_ELF, GbaCardCatalog.MYSTICAL_ELF,
                    GbaCardCatalog.SANGAN, GbaCardCatalog.SANGAN,
                    GbaCardCatalog.CURSE_OF_DRAGON_GBA, GbaCardCatalog.CURSE_OF_DRAGON_GBA, // Tribute Level 5
                    GbaCardCatalog.SUMMONED_SKULL_GBA, // Tribute Level 6
                    DdsCardCatalog.DARK_MAGICIAN, // 2-Tribute Level 7
                    GbaCardCatalog.POT_OF_GREED,
                    GbaCardCatalog.SWORDS_OF_REVEALING_LIGHT,
                    GbaCardCatalog.MONSTER_REBORN,
                    GbaCardCatalog.CHANGE_OF_HEART,
                    GbaCardCatalog.DIAN_KETO_GBA, GbaCardCatalog.DIAN_KETO_GBA,
                    GbaCardCatalog.MAN_EATER_BUG, GbaCardCatalog.MAN_EATER_BUG,
                    GbaCardCatalog.TRAP_HOLE, GbaCardCatalog.TRAP_HOLE,
                    GbaCardCatalog.WABOKU_GBA, GbaCardCatalog.WABOKU_GBA, GbaCardCatalog.WABOKU_GBA,
                    GbaCardCatalog.MAGIC_JAMMER
                ))
            }
            "Joey Wheeler" -> {
                // Authentic EDS Joey Underdog Deck: Beast-warriors, Red-Eyes, and gamble cards
                deck.addAll(listOf(
                    GbaCardCatalog.ALLIGATORS_SWORD, GbaCardCatalog.ALLIGATORS_SWORD, GbaCardCatalog.ALLIGATORS_SWORD,
                    GbaCardCatalog.BABY_DRAGON_GBA, GbaCardCatalog.BABY_DRAGON_GBA, GbaCardCatalog.BABY_DRAGON_GBA,
                    GbaCardCatalog.KOUMORI_DRAGON, GbaCardCatalog.KOUMORI_DRAGON, GbaCardCatalog.KOUMORI_DRAGON,
                    GbaCardCatalog.TIME_WIZARD_GBA, GbaCardCatalog.TIME_WIZARD_GBA,
                    GbaCardCatalog.AXE_RAIDER, GbaCardCatalog.AXE_RAIDER, GbaCardCatalog.AXE_RAIDER,
                    GbaCardCatalog.BATTLE_OX_GBA, GbaCardCatalog.BATTLE_OX_GBA,
                    GbaCardCatalog.ARMORED_LIZARD, GbaCardCatalog.ARMORED_LIZARD, GbaCardCatalog.ARMORED_LIZARD,
                    GbaCardCatalog.GOKIBORE, GbaCardCatalog.GOKIBORE, GbaCardCatalog.GOKIBORE,
                    GbaCardCatalog.SANGAN, GbaCardCatalog.SANGAN,
                    GbaCardCatalog.BATTLE_STEER, GbaCardCatalog.BATTLE_STEER, // Level 5 Tribute
                    GbaCardCatalog.SWORD_ARM_OF_DRAGON, // Level 6 Tribute
                    DdsCardCatalog.RED_EYES_B_DRAGON, // 2-Tribute Level 7
                    GbaCardCatalog.POT_OF_GREED,
                    GbaCardCatalog.MONSTER_REBORN,
                    GbaCardCatalog.CHANGE_OF_HEART,
                    GbaCardCatalog.SKULL_DICE, GbaCardCatalog.SKULL_DICE,
                    GbaCardCatalog.TRAP_HOLE, GbaCardCatalog.TRAP_HOLE,
                    GbaCardCatalog.WABOKU_GBA, GbaCardCatalog.WABOKU_GBA, GbaCardCatalog.WABOKU_GBA
                ))
            }
            "Bakura Ryou" -> {
                // Authentic EDS Zombie / Occult Deck: Flip-effect traps, undead hordes, and disruption
                deck.addAll(listOf(
                    DdsCardCatalog.MORPHING_JAR, DdsCardCatalog.MORPHING_JAR, DdsCardCatalog.MORPHING_JAR,
                    GbaCardCatalog.MAN_EATER_BUG, GbaCardCatalog.MAN_EATER_BUG, GbaCardCatalog.MAN_EATER_BUG,
                    GbaCardCatalog.NEEDLE_WORM, GbaCardCatalog.NEEDLE_WORM, GbaCardCatalog.NEEDLE_WORM,
                    GbaCardCatalog.WHITE_ZOMBIE, GbaCardCatalog.WHITE_ZOMBIE, GbaCardCatalog.WHITE_ZOMBIE,
                    GbaCardCatalog.ARMORED_ZOMBIE, GbaCardCatalog.ARMORED_ZOMBIE, GbaCardCatalog.ARMORED_ZOMBIE,
                    GbaCardCatalog.DRAGON_ZOMBIE, GbaCardCatalog.DRAGON_ZOMBIE, GbaCardCatalog.DRAGON_ZOMBIE,
                    GbaCardCatalog.THE_WANDERING_DOOMED, GbaCardCatalog.THE_WANDERING_DOOMED, GbaCardCatalog.THE_WANDERING_DOOMED,
                    GbaCardCatalog.SKULL_STALKER, GbaCardCatalog.SKULL_STALKER, GbaCardCatalog.SKULL_STALKER,
                    GbaCardCatalog.FIENDS_HAND, GbaCardCatalog.FIENDS_HAND, GbaCardCatalog.FIENDS_HAND,
                    GbaCardCatalog.DARKWORLD_THORNS, GbaCardCatalog.DARKWORLD_THORNS, GbaCardCatalog.DARKWORLD_THORNS,
                    GbaCardCatalog.SANGAN, GbaCardCatalog.SANGAN,
                    GbaCardCatalog.POT_OF_GREED,
                    GbaCardCatalog.DARK_HOLE,
                    GbaCardCatalog.CHANGE_OF_HEART,
                    GbaCardCatalog.SWORDS_OF_REVEALING_LIGHT,
                    GbaCardCatalog.TRAP_HOLE, GbaCardCatalog.TRAP_HOLE,
                    GbaCardCatalog.WABOKU_GBA, GbaCardCatalog.WABOKU_GBA
                ))
            }
            "Rex Raptor" -> {
                deck.addAll(listOf(
                    GbaCardCatalog.TWO_HEADED_KING_REX, GbaCardCatalog.TWO_HEADED_KING_REX, GbaCardCatalog.TWO_HEADED_KING_REX,
                    GbaCardCatalog.URABY, GbaCardCatalog.URABY, GbaCardCatalog.URABY,
                    GbaCardCatalog.CRAWLING_DRAGON_2, GbaCardCatalog.CRAWLING_DRAGON_2, GbaCardCatalog.CRAWLING_DRAGON_2,
                    GbaCardCatalog.ANTHROSAURUS, GbaCardCatalog.ANTHROSAURUS, GbaCardCatalog.ANTHROSAURUS,
                    GbaCardCatalog.TOMOZAURUS, GbaCardCatalog.TOMOZAURUS, GbaCardCatalog.TOMOZAURUS,
                    GbaCardCatalog.ARMORED_LIZARD, GbaCardCatalog.ARMORED_LIZARD, GbaCardCatalog.ARMORED_LIZARD,
                    GbaCardCatalog.CRAWLING_DRAGON, GbaCardCatalog.CRAWLING_DRAGON,
                    GbaCardCatalog.SWORD_ARM_OF_DRAGON, GbaCardCatalog.SWORD_ARM_OF_DRAGON,
                    GbaCardCatalog.POT_OF_GREED, GbaCardCatalog.RAIGEKI, GbaCardCatalog.DARK_HOLE,
                    GbaCardCatalog.MONSTER_REBORN, GbaCardCatalog.CHANGE_OF_HEART,
                    GbaCardCatalog.TRAP_HOLE, GbaCardCatalog.TRAP_HOLE, GbaCardCatalog.TRAP_HOLE,
                    GbaCardCatalog.WABOKU_GBA, GbaCardCatalog.WABOKU_GBA, GbaCardCatalog.WABOKU_GBA,
                    GbaCardCatalog.DIAN_KETO_GBA, GbaCardCatalog.DIAN_KETO_GBA,
                    GbaCardCatalog.MAGIC_JAMMER, GbaCardCatalog.SEVEN_TOOLS_BANDIT
                ))
            }
            "Weevil Underwood" -> {
                deck.addAll(listOf(
                    GbaCardCatalog.BIG_INSECT, GbaCardCatalog.BIG_INSECT, GbaCardCatalog.BIG_INSECT,
                    GbaCardCatalog.KILLER_NEEDLE, GbaCardCatalog.KILLER_NEEDLE, GbaCardCatalog.KILLER_NEEDLE,
                    GbaCardCatalog.GOKIBORE, GbaCardCatalog.GOKIBORE, GbaCardCatalog.GOKIBORE,
                    GbaCardCatalog.NEEDLE_WORM, GbaCardCatalog.NEEDLE_WORM, GbaCardCatalog.NEEDLE_WORM,
                    GbaCardCatalog.MAN_EATER_BUG, GbaCardCatalog.MAN_EATER_BUG, GbaCardCatalog.MAN_EATER_BUG,
                    GbaCardCatalog.GANIGUMO, GbaCardCatalog.GANIGUMO, GbaCardCatalog.GANIGUMO,
                    GbaCardCatalog.WINGED_CLEAVER, GbaCardCatalog.WINGED_CLEAVER, GbaCardCatalog.WINGED_CLEAVER,
                    GbaCardCatalog.DARKWORLD_THORNS, GbaCardCatalog.DARKWORLD_THORNS, GbaCardCatalog.DARKWORLD_THORNS,
                    GbaCardCatalog.POT_OF_GREED, GbaCardCatalog.SWORDS_OF_REVEALING_LIGHT,
                    GbaCardCatalog.MONSTER_REBORN, GbaCardCatalog.CHANGE_OF_HEART,
                    GbaCardCatalog.TRAP_HOLE, GbaCardCatalog.TRAP_HOLE, GbaCardCatalog.TRAP_HOLE,
                    GbaCardCatalog.WABOKU_GBA, GbaCardCatalog.WABOKU_GBA, GbaCardCatalog.WABOKU_GBA,
                    GbaCardCatalog.MAGIC_JAMMER, GbaCardCatalog.SEVEN_TOOLS_BANDIT,
                    GbaCardCatalog.SANGAN, GbaCardCatalog.SANGAN
                ))
            }
            "Mako Tsunami" -> {
                deck.addAll(listOf(
                    GbaCardCatalog.SEVEN_COLORED_FISH, GbaCardCatalog.SEVEN_COLORED_FISH, GbaCardCatalog.SEVEN_COLORED_FISH,
                    GbaCardCatalog.GREAT_WHITE, GbaCardCatalog.GREAT_WHITE, GbaCardCatalog.GREAT_WHITE,
                    GbaCardCatalog.TAKRIMINOS, GbaCardCatalog.TAKRIMINOS, GbaCardCatalog.TAKRIMINOS,
                    GbaCardCatalog.FIEND_KRAKEN, GbaCardCatalog.FIEND_KRAKEN, GbaCardCatalog.FIEND_KRAKEN,
                    GbaCardCatalog.ENCHANTING_MERMAID, GbaCardCatalog.ENCHANTING_MERMAID, GbaCardCatalog.ENCHANTING_MERMAID,
                    GbaCardCatalog.AQUA_MADOOR, GbaCardCatalog.AQUA_MADOOR, GbaCardCatalog.AQUA_MADOOR,
                    GbaCardCatalog.POT_OF_GREED, GbaCardCatalog.MONSTER_REBORN, GbaCardCatalog.CHANGE_OF_HEART,
                    GbaCardCatalog.SWORDS_OF_REVEALING_LIGHT,
                    GbaCardCatalog.TRAP_HOLE, GbaCardCatalog.TRAP_HOLE, GbaCardCatalog.TRAP_HOLE,
                    GbaCardCatalog.TORRENTIAL_TRIBUTE, GbaCardCatalog.TORRENTIAL_TRIBUTE,
                    GbaCardCatalog.WABOKU_GBA, GbaCardCatalog.WABOKU_GBA, GbaCardCatalog.WABOKU_GBA,
                    GbaCardCatalog.MAGIC_JAMMER, GbaCardCatalog.SEVEN_TOOLS_BANDIT,
                    GbaCardCatalog.DIAN_KETO_GBA, GbaCardCatalog.DIAN_KETO_GBA,
                    GbaCardCatalog.SANGAN, GbaCardCatalog.SANGAN
                ))
            }
            "Mai Valentine" -> {
                deck.addAll(listOf(
                    GbaCardCatalog.CYBER_HARPIE, GbaCardCatalog.CYBER_HARPIE, GbaCardCatalog.CYBER_HARPIE,
                    GbaCardCatalog.HARPIE_LADY_GBA, GbaCardCatalog.HARPIE_LADY_GBA, GbaCardCatalog.HARPIE_LADY_GBA,
                    DdsCardCatalog.SONIC_MAID, DdsCardCatalog.SONIC_MAID, DdsCardCatalog.SONIC_MAID,
                    GbaCardCatalog.SHINING_FRIENDSHIP, GbaCardCatalog.SHINING_FRIENDSHIP, GbaCardCatalog.SHINING_FRIENDSHIP,
                    GbaCardCatalog.ENCHANTING_MERMAID, GbaCardCatalog.ENCHANTING_MERMAID, GbaCardCatalog.ENCHANTING_MERMAID,
                    GbaCardCatalog.HARPIES_FEATHER_DUSTER,
                    GbaCardCatalog.POT_OF_GREED, GbaCardCatalog.MONSTER_REBORN, GbaCardCatalog.CHANGE_OF_HEART,
                    GbaCardCatalog.MIRROR_FORCE,
                    GbaCardCatalog.TRAP_HOLE, GbaCardCatalog.TRAP_HOLE, GbaCardCatalog.TRAP_HOLE,
                    GbaCardCatalog.WABOKU_GBA, GbaCardCatalog.WABOKU_GBA, GbaCardCatalog.WABOKU_GBA,
                    GbaCardCatalog.DIAN_KETO_GBA, GbaCardCatalog.DIAN_KETO_GBA,
                    GbaCardCatalog.MAGIC_JAMMER, GbaCardCatalog.SEVEN_TOOLS_BANDIT,
                    GbaCardCatalog.SANGAN, GbaCardCatalog.SANGAN,
                    GbaCardCatalog.SKELENGEL, GbaCardCatalog.SKELENGEL
                ))
            }
            "Espa Roba" -> {
                deck.addAll(listOf(
                    GbaCardCatalog.JINZO, GbaCardCatalog.JINZO,
                    GbaCardCatalog.CYBER_FALCON, GbaCardCatalog.CYBER_FALCON, GbaCardCatalog.CYBER_FALCON,
                    GbaCardCatalog.GEARFRIED_IRON_KNIGHT, GbaCardCatalog.GEARFRIED_IRON_KNIGHT,
                    GbaCardCatalog.BEAN_SOLDIER, GbaCardCatalog.BEAN_SOLDIER, GbaCardCatalog.BEAN_SOLDIER,
                    GbaCardCatalog.FERAL_IMP, GbaCardCatalog.FERAL_IMP, GbaCardCatalog.FERAL_IMP,
                    GbaCardCatalog.BIG_EYE, GbaCardCatalog.BIG_EYE, GbaCardCatalog.BIG_EYE,
                    GbaCardCatalog.SKELENGEL, GbaCardCatalog.SKELENGEL, GbaCardCatalog.SKELENGEL,
                    GbaCardCatalog.POT_OF_GREED, GbaCardCatalog.RAIGEKI, GbaCardCatalog.MONSTER_REBORN,
                    GbaCardCatalog.CHANGE_OF_HEART, GbaCardCatalog.SWORDS_OF_REVEALING_LIGHT,
                    GbaCardCatalog.TRAP_HOLE, GbaCardCatalog.TRAP_HOLE,
                    GbaCardCatalog.WABOKU_GBA, GbaCardCatalog.WABOKU_GBA, GbaCardCatalog.WABOKU_GBA,
                    GbaCardCatalog.SANGAN, GbaCardCatalog.SANGAN,
                    GbaCardCatalog.MAN_EATER_BUG, GbaCardCatalog.MAN_EATER_BUG,
                    GbaCardCatalog.DIAN_KETO_GBA, GbaCardCatalog.DIAN_KETO_GBA,
                    GbaCardCatalog.MAGIC_JAMMER
                ))
            }
            "Seto Kaiba" -> {
                // Tier 4 Boss: True Kaiba powerhouse beatdown with 3x Blue-Eyes, Vorse Raiders, and La Jinn
                deck.addAll(listOf(
                    DdsCardCatalog.BLUE_EYES_WHITE_DRAGON, DdsCardCatalog.BLUE_EYES_WHITE_DRAGON, DdsCardCatalog.BLUE_EYES_WHITE_DRAGON,
                    GbaCardCatalog.VORSE_RAIDER, GbaCardCatalog.VORSE_RAIDER, GbaCardCatalog.VORSE_RAIDER,
                    GbaCardCatalog.LA_JINN, GbaCardCatalog.LA_JINN, GbaCardCatalog.LA_JINN,
                    GbaCardCatalog.BATTLE_OX_GBA, GbaCardCatalog.BATTLE_OX_GBA, GbaCardCatalog.BATTLE_OX_GBA,
                    GbaCardCatalog.MYSTICAL_ELF, GbaCardCatalog.MYSTICAL_ELF,
                    GbaCardCatalog.GIANT_SOLDIER_OF_STONE, GbaCardCatalog.GIANT_SOLDIER_OF_STONE,
                    GbaCardCatalog.SANGAN, GbaCardCatalog.SANGAN,
                    GbaCardCatalog.POT_OF_GREED,
                    GbaCardCatalog.RAIGEKI,
                    GbaCardCatalog.MONSTER_REBORN,
                    GbaCardCatalog.CHANGE_OF_HEART,
                    GbaCardCatalog.SWORDS_OF_REVEALING_LIGHT,
                    GbaCardCatalog.MIRROR_FORCE,
                    GbaCardCatalog.TORRENTIAL_TRIBUTE,
                    GbaCardCatalog.RING_OF_DESTRUCTION,
                    GbaCardCatalog.TRAP_HOLE, GbaCardCatalog.TRAP_HOLE,
                    GbaCardCatalog.MAGIC_JAMMER, GbaCardCatalog.SEVEN_TOOLS_BANDIT,
                    GbaCardCatalog.MAN_EATER_BUG, GbaCardCatalog.MAN_EATER_BUG,
                    GbaCardCatalog.WABOKU_GBA, GbaCardCatalog.WABOKU_GBA,
                    GbaCardCatalog.SKELENGEL, GbaCardCatalog.SKELENGEL
                ))
            }
            "Yami Yugi" -> {
                // Tier 4 Champion: Master of Magicians & Spellcasters
                deck.addAll(listOf(
                    DdsCardCatalog.DARK_MAGICIAN, DdsCardCatalog.DARK_MAGICIAN,
                    GbaCardCatalog.DARK_MAGICIAN_GIRL,
                    GbaCardCatalog.BUSTER_BLADER,
                    GbaCardCatalog.SUMMONED_SKULL_GBA, GbaCardCatalog.SUMMONED_SKULL_GBA,
                    GbaCardCatalog.CELTIC_GUARDIAN_GBA, GbaCardCatalog.CELTIC_GUARDIAN_GBA,
                    GbaCardCatalog.MYSTICAL_ELF, GbaCardCatalog.MYSTICAL_ELF,
                    GbaCardCatalog.GIANT_SOLDIER_OF_STONE, GbaCardCatalog.GIANT_SOLDIER_OF_STONE,
                    GbaCardCatalog.SANGAN, GbaCardCatalog.SANGAN,
                    GbaCardCatalog.POT_OF_GREED,
                    GbaCardCatalog.MONSTER_REBORN,
                    GbaCardCatalog.CHANGE_OF_HEART,
                    GbaCardCatalog.SWORDS_OF_REVEALING_LIGHT,
                    GbaCardCatalog.MAGIC_CYLINDER,
                    GbaCardCatalog.MIRROR_FORCE,
                    GbaCardCatalog.TORRENTIAL_TRIBUTE,
                    GbaCardCatalog.TRAP_HOLE, GbaCardCatalog.TRAP_HOLE,
                    GbaCardCatalog.MAGIC_JAMMER, GbaCardCatalog.SEVEN_TOOLS_BANDIT,
                    GbaCardCatalog.MAN_EATER_BUG, GbaCardCatalog.MAN_EATER_BUG,
                    GbaCardCatalog.WABOKU_GBA, GbaCardCatalog.WABOKU_GBA,
                    GbaCardCatalog.SKELENGEL, GbaCardCatalog.SKELENGEL,
                    GbaCardCatalog.FERAL_IMP, GbaCardCatalog.FERAL_IMP,
                    GbaCardCatalog.CURSE_OF_DRAGON_GBA
                ))
            }
            "Maximillion Pegasus" -> {
                deck.addAll(listOf(
                    GbaCardCatalog.RELINQUISHED, GbaCardCatalog.RELINQUISHED,
                    GbaCardCatalog.SUMMONED_SKULL_GBA, GbaCardCatalog.SUMMONED_SKULL_GBA,
                    GbaCardCatalog.GEMINI_ELF, GbaCardCatalog.GEMINI_ELF,
                    GbaCardCatalog.ROGUE_DOLL, GbaCardCatalog.ROGUE_DOLL,
                    GbaCardCatalog.MYSTICAL_ELF, GbaCardCatalog.MYSTICAL_ELF,
                    GbaCardCatalog.SANGAN, GbaCardCatalog.SANGAN,
                    GbaCardCatalog.POT_OF_GREED,
                    GbaCardCatalog.CHANGE_OF_HEART,
                    GbaCardCatalog.SNATCH_STEAL,
                    GbaCardCatalog.MIRROR_FORCE,
                    GbaCardCatalog.MAGIC_CYLINDER,
                    GbaCardCatalog.SWORDS_OF_REVEALING_LIGHT,
                    GbaCardCatalog.TRAP_HOLE, GbaCardCatalog.TRAP_HOLE,
                    GbaCardCatalog.WABOKU_GBA, GbaCardCatalog.WABOKU_GBA,
                    GbaCardCatalog.MAN_EATER_BUG, GbaCardCatalog.MAN_EATER_BUG,
                    GbaCardCatalog.SKELENGEL, GbaCardCatalog.SKELENGEL,
                    GbaCardCatalog.DIAN_KETO_GBA, GbaCardCatalog.DIAN_KETO_GBA,
                    GbaCardCatalog.MAGIC_JAMMER, GbaCardCatalog.SEVEN_TOOLS_BANDIT,
                    GbaCardCatalog.GIANT_SOLDIER_OF_STONE, GbaCardCatalog.GIANT_SOLDIER_OF_STONE,
                    GbaCardCatalog.NEO_MAGIC_SWORDSMAN, GbaCardCatalog.NEO_MAGIC_SWORDSMAN
                ))
            }
            else -> {
                // Tier-appropriate rookie fallback: balanced normal monsters (800-1400 ATK) - NO Vorse Raider or Gemini Elf
                deck.addAll(listOf(
                    GbaCardCatalog.CELTIC_GUARDIAN_GBA, GbaCardCatalog.CELTIC_GUARDIAN_GBA,
                    GbaCardCatalog.FERAL_IMP, GbaCardCatalog.FERAL_IMP,
                    GbaCardCatalog.HITOTSU_ME_GIANT, GbaCardCatalog.HITOTSU_ME_GIANT,
                    GbaCardCatalog.BEAN_SOLDIER, GbaCardCatalog.BEAN_SOLDIER,
                    GbaCardCatalog.GRIFFORE, GbaCardCatalog.GRIFFORE,
                    GbaCardCatalog.MYSTICAL_ELF, GbaCardCatalog.MYSTICAL_ELF,
                    GbaCardCatalog.POT_OF_GREED,
                    GbaCardCatalog.TRAP_HOLE, GbaCardCatalog.TRAP_HOLE,
                    GbaCardCatalog.WABOKU_GBA, GbaCardCatalog.WABOKU_GBA
                ))
            }
        }

        // Tier-safe filler that NEVER injects tournament-level 1800-1900 ATK monsters into casual matches
        val filler = listOf(
            GbaCardCatalog.FERAL_IMP,
            GbaCardCatalog.HITOTSU_ME_GIANT,
            GbaCardCatalog.BEAN_SOLDIER,
            GbaCardCatalog.GRIFFORE,
            GbaCardCatalog.MYSTICAL_ELF,
            GbaCardCatalog.SKELENGEL,
            GbaCardCatalog.TRAP_HOLE,
            GbaCardCatalog.WABOKU_GBA
        )
        var fillerIdx = 0
        while (deck.size < 40) {
            deck.add(filler[fillerIdx % filler.size])
            fillerIdx++
        }

        return deck
    }

    /**
     * Handles victory against a GBA opponent and rewards an authentic card drop.
     */
    fun recordDuelResult(opponentName: String, won: Boolean): Card? {
        if (won) {
            opponentWins[opponentName] = (opponentWins[opponentName] ?: 0) + 1
            starchips = (starchips + 5).coerceAtMost(9999)
            val drop = when (opponentName) {
                "Tristan Taylor" -> GbaCardCatalog.BATTLE_OX_GBA
                "Tea Gardner" -> GbaCardCatalog.SHINING_FRIENDSHIP
                "Yugi Muto" -> GbaCardCatalog.CELTIC_GUARDIAN_GBA
                "Joey Wheeler" -> GbaCardCatalog.ALLIGATORS_SWORD
                "Bakura Ryou" -> DdsCardCatalog.MORPHING_JAR
                "Seto Kaiba" -> DdsCardCatalog.BLUE_EYES_WHITE_DRAGON
                "Yami Yugi" -> GbaCardCatalog.BUSTER_BLADER
                "Espa Roba" -> GbaCardCatalog.JINZO
                "Mai Valentine" -> GbaCardCatalog.CYBER_HARPIE
                "Rex Raptor" -> GbaCardCatalog.TWO_HEADED_KING_REX
                "Weevil Underwood" -> GbaCardCatalog.BIG_INSECT
                "Mako Tsunami" -> GbaCardCatalog.SEVEN_COLORED_FISH
                "Maximillion Pegasus" -> GbaCardCatalog.RELINQUISHED
                else -> GbaCardCatalog.BATTLE_OX_GBA
            }
            trunk.add(drop)
            return drop
        }
        return null
    }

    /**
     * Unlocks a card into Trunk using 8-digit official Konami passcode and Starchips.
     */
    fun unlockCardViaPasscode(code: String): Pair<Boolean, String> {
        val cleanCode = code.trim().padStart(8, '0')
        val cardName = CardPasscodeRegistry.findCardNameByPasscode(cleanCode)
            ?: return Pair(false, "Invalid passcode: No card found for [$cleanCode].")

        val card = GbaCardCatalog.getCardByName(cardName)
            ?: DdsCardCatalog.getCardByName(cardName)
            ?: Card(id = 9000 + trunk.size, name = cardName, atk = 1600, def = 1400, level = 4, attribute = CardAttribute.DARK, cardType = CardType.MONSTER, monsterType = MonsterType.WARRIOR)

        val cost = (card.level * 5).coerceIn(5, 40)
        if (starchips < cost) {
            return Pair(false, "Requires $cost Starchips! You currently have $starchips ⭐.")
        }
        starchips -= cost
        trunk.add(card)
        return Pair(true, "Successfully unlocked [${card.name}] into Trunk for $cost Starchips! (Remaining: $starchips ⭐)")
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

    val savedDeckRecipes = mutableListOf(
        SavedDeckRecipe(1, "Main Tournament Deck", mutableListOf()),
        SavedDeckRecipe(2, "Dragon Beatdown", mutableListOf()),
        SavedDeckRecipe(3, "Spellcaster & Ritual", mutableListOf()),
        SavedDeckRecipe(4, "Warrior & Fusion", mutableListOf()),
        SavedDeckRecipe(5, "Trap Control & Burn", mutableListOf())
    )

    var currentActiveSlot: Int = 1

    var customDuelSettings = CustomDuelSettings()

    fun saveCurrentDeckToSlot(slotId: Int, customName: String? = null) {
        val recipe = savedDeckRecipes.find { it.id == slotId } ?: return
        if (!customName.isNullOrBlank()) {
            recipe.name = customName
        }
        recipe.cardIds.clear()
        recipe.cardIds.addAll(activeDeck.map { it.id })
    }

    fun loadDeckFromSlot(slotId: Int): Boolean {
        val recipe = savedDeckRecipes.find { it.id == slotId } ?: return false
        if (recipe.cardIds.isEmpty()) return false
        trunk.addAll(activeDeck)
        activeDeck.clear()

        for (cId in recipe.cardIds) {
            val idx = trunk.indexOfFirst { it.id == cId }
            if (idx != -1) {
                activeDeck.add(trunk.removeAt(idx))
            } else {
                val card = GbaCardCatalog.getCardById(cId)
                    ?: DdsCardCatalog.getCardById(cId)
                if (card != null) {
                    activeDeck.add(card)
                }
            }
        }
        currentActiveSlot = slotId
        return true
    }

    fun exportDeckToPasscodes(): String {
        return activeDeck.mapNotNull { it.password ?: CardPasscodeRegistry.findPasscodeByCardName(it.name) }.joinToString(",")
    }

    fun importDeckFromPasscodes(input: String): Pair<Int, String> {
        val codes = input.split(",", "\n", " ", ";")
            .map { it.trim().padStart(8, '0') }
            .filter { it.length == 8 && it.all { c -> c.isDigit() } }
        if (codes.isEmpty()) return Pair(0, "No valid 8-digit passcodes found in input.")

        trunk.addAll(activeDeck)
        activeDeck.clear()
        var importedCount = 0

        for (code in codes) {
            val name = CardPasscodeRegistry.findCardNameByPasscode(code) ?: continue
            val card = GbaCardCatalog.getCardByName(name)
                ?: DdsCardCatalog.getCardByName(name)
                ?: Card(
                    id = 8000 + activeDeck.size,
                    name = name,
                    atk = 1500,
                    def = 1200,
                    level = 4,
                    attribute = CardAttribute.DARK,
                    cardType = CardType.MONSTER,
                    password = code
                )
            activeDeck.add(card)
            importedCount++
            if (activeDeck.size >= 60) break
        }
        return Pair(importedCount, "Successfully imported $importedCount cards into active deck!")
    }
}

data class SavedDeckRecipe(
    val id: Int,
    var name: String,
    val cardIds: MutableList<Int>
)

data class CustomDuelSettings(
    var startingLp: Int = 8000,
    var enableDdsAlignment: Boolean = false,
    var opponentName: String = "Yugi Muto",
    var isMirrorMatch: Boolean = false
)
