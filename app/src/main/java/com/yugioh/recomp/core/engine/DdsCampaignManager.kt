package com.yugioh.recomp.core.engine

/**
 * Manages authentic Yu-Gi-Oh! Dark Duel Stories (GBC) campaign progression,
 * Duelist Level, Deck Capacity (DC), Starter Deck (ROM 0:255D), and Trunk inventory.
 */
object DdsCampaignManager {

    /**
     * Starting Deck Capacity in authentic Dark Duel Stories (GBC).
     */
    var deckCapacity: Int = 400

    /**
     * Authentic Dark Duel Stories Duelist Level formula (ROM 5:6819):
     * Duelist Level = ((Deck Capacity - 400) / 3) + 15, capped at 255.
     */
    val duelistLevel: Int
        get() = (((deckCapacity - 400) / 3) + 15).coerceIn(15, 255)

    /**
     * Active 40-Card Deck currently used in Duels.
     */
    val activeDeck = mutableListOf<Card>()

    /**
     * Reserve Trunk / Chest of all unlocked cards.
     */
    val trunk = mutableListOf<Card>()

    /**
     * Campaign Win Records against Stage 1 & Boss opponents.
     */
    val opponentWins = mutableMapOf<String, Int>(
        "Tristan Taylor" to 0,
        "Joey Wheeler" to 0,
        "Mai Valentine" to 0,
        "Bakura" to 0,
        "Seto Kaiba" to 0
    )

    /**
     * Current selected opponent for duels.
     */
    var selectedOpponentName: String = "Tristan Taylor"

    /**
     * GBC LCD dot-matrix shader toggle state.
     */
    var isGbcLcdShaderEnabled: Boolean = true

    /**
     * Starchips currency earned from duel victories, used in the Password Machine.
     */
    var starchips: Int = 30

    init {
        resetToNewGame()
    }

    /**
     * Initializes a new campaign save matching Dark Duel Stories initial state.
     */
    fun resetToNewGame() {
        deckCapacity = 400
        starchips = 30
        activeDeck.clear()
        activeDeck.addAll(getCanonicalStarterDeck())
        trunk.clear()
        // Provide starter reserve pool in Trunk
        trunk.addAll(getStarterTrunkCards())
        opponentWins["Tristan Taylor"] = 0
        opponentWins["Joey Wheeler"] = 0
        opponentWins["Mai Valentine"] = 0
        opponentWins["Bakura"] = 0
        opponentWins["Seto Kaiba"] = 0
    }

    fun totalDeckCost(): Int = activeDeck.sumOf { it.deckCost }

    /**
     * Validates whether a card can be added to the active 40-card deck.
     */
    fun canAddCard(card: Card): Pair<Boolean, String> {
        if (activeDeck.size >= 40) {
            return Pair(false, "Deck cannot exceed 40 cards! Remove a card first.")
        }
        if (card.deckCost > duelistLevel) {
            return Pair(false, "[${card.name}] Cost (${card.deckCost}) exceeds your Duelist Level ($duelistLevel)!")
        }
        if (totalDeckCost() + card.deckCost > deckCapacity) {
            return Pair(false, "Total Cost would exceed Deck Capacity ($deckCapacity DC)!")
        }
        val copiesInDeck = activeDeck.count { it.id == card.id }
        if (copiesInDeck >= 3) {
            return Pair(false, "Cannot include more than 3 copies of [${card.name}]!")
        }
        return Pair(true, "OK")
    }

    /**
     * Moves a card from the Trunk into the active 40-card Deck.
     */
    fun addCardToDeck(card: Card): Boolean {
        val (canAdd, _) = canAddCard(card)
        if (!canAdd) return false
        val idx = trunk.indexOfFirst { it.id == card.id }
        if (idx != -1) {
            val moved = trunk.removeAt(idx)
            activeDeck.add(moved)
            return true
        }
        return false
    }

    /**
     * Moves a card from the active Deck back into the Trunk.
     */
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

    /**
     * Handles post-duel victory or defeat according to DDS rules:
     * - Victory: +3 Deck Capacity, records win, awards 1 card drop from opponent, +5 Starchips.
     * - Defeat: +2 Deck Capacity.
     */
    fun recordDuelResult(opponentName: String, won: Boolean): Card? {
        if (won) {
            deckCapacity = (deckCapacity + 3).coerceAtMost(9999)
            starchips = (starchips + 5).coerceAtMost(9999)
            opponentWins[opponentName] = (opponentWins[opponentName] ?: 0) + 1
            val droppedCard = rollOpponentDrop(opponentName)
            if (droppedCard != null) {
                trunk.add(droppedCard)
            }
            return droppedCard
        } else {
            deckCapacity = (deckCapacity + 2).coerceAtMost(9999)
            return null
        }
    }

    /**
     * Unlocks a card via 8-digit official card passcode using Starchips.
     */
    fun unlockCardViaPasscode(code: String): Pair<Boolean, String> {
        val cleanCode = code.trim().padStart(8, '0')
        val cardName = CardPasscodeRegistry.findCardNameByPasscode(cleanCode)
            ?: return Pair(false, "Invalid passcode: No card found for [$cleanCode].")

        val card = DdsCardCatalog.getCardByName(cardName)
            ?: Card(id = 8000 + trunk.size, name = cardName, atk = 1500, def = 1200, level = 4, attribute = CardAttribute.EARTH, cardType = CardType.MONSTER, monsterType = MonsterType.WARRIOR)

        val cost = (card.deckCost * 2).coerceIn(5, 50)
        if (starchips < cost) {
            return Pair(false, "Requires $cost Starchips! You currently have $starchips ⭐.")
        }

        starchips -= cost
        trunk.add(card)
        return Pair(true, "Successfully unlocked [${card.name}] for $cost Starchips! (Remaining: $starchips ⭐)")
    }

    /**
     * Canonical 40-Card Starter Deck from Dark Duel Stories ROM (0:255D).
     * Every card has Deck Cost <= 15 and sum <= 400 DC.
     */
    fun getCanonicalStarterDeck(): List<Card> {
        val all = DdsCardCatalog.getAllCards()
        val deck = mutableListOf<Card>()

        fun add(id: Int, count: Int = 1) {
            val card = all.find { it.id == id } ?: DdsCardCatalog.SHADOW_SPECTER
            repeat(count) { deck.add(card) }
        }

        add(DdsCardCatalog.SINISTER_SERPENT.id, 3)
        add(DdsCardCatalog.MILUS_RADIANT.id, 1)
        add(DdsCardCatalog.ZONE_EATER.id, 1)
        add(DdsCardCatalog.SWORDSMAN_FOREIGN.id, 1)
        add(DdsCardCatalog.LEGHUL.id, 2)
        add(DdsCardCatalog.FUNGI_MUSK.id, 1)
        add(DdsCardCatalog.SHADOW_SPECTER.id, 3)
        add(DdsCardCatalog.MONSTER_EYE.id, 1)
        add(DdsCardCatalog.BONE_MOUSE.id, 1)
        add(DdsCardCatalog.KAMINARIKOZOU.id, 1)
        add(DdsCardCatalog.WHITE_DOLPHIN.id, 1)
        add(DdsCardCatalog.WRETCHED_GHOST.id, 2)
        add(DdsCardCatalog.TURU_PURUN.id, 2)
        add(DdsCardCatalog.JINZO_7.id, 1)
        add(DdsCardCatalog.MUKA_MUKA.id, 1)
        add(DdsCardCatalog.MECH_MOLE_ZOMBIE.id, 1)
        add(DdsCardCatalog.MECHANICAL_SPIDER.id, 1)
        add(DdsCardCatalog.BOO_KOO.id, 1)
        add(DdsCardCatalog.STAR_BOY.id, 1)
        add(DdsCardCatalog.ABYSS_FLOWER.id, 1)
        add(DdsCardCatalog.MYSTERY_HAND.id, 3)
        add(DdsCardCatalog.HIRO_SHADOW_SCOUT.id, 2)
        add(DdsCardCatalog.AIR_MARMOT.id, 1)
        add(DdsCardCatalog.DROLL_BIRD.id, 1)
        add(DdsCardCatalog.YAMATANO_DRAGON.id, 1)
        add(DdsCardCatalog.BLADEFLY.id, 1)
        add(DdsCardCatalog.FROG_THE_JAM.id, 1)
        add(DdsCardCatalog.GALE_DOGRA.id, 1)
        add(DdsCardCatalog.MASKED_CLOWN.id, 1)
        add(DdsCardCatalog.CYBER_STEIN.id, 1)

        return deck
    }

    private fun getStarterTrunkCards(): List<Card> {
        return listOf(
            DdsCardCatalog.SPARKS,
            DdsCardCatalog.MOOYAN_CURRY,
            DdsCardCatalog.HINOTAMA,
            DdsCardCatalog.KURIBOH,
            DdsCardCatalog.MOUNTAIN_WARRIOR,
            DdsCardCatalog.TIME_WIZARD
        )
    }

    /**
     * Generates the opponent's authentic 40-card deck based on DDS character profiles.
     */
    fun buildOpponentDeck(opponentName: String): List<Card> {
        val deck = mutableListOf<Card>()
        val all = DdsCardCatalog.getAllCards()

        fun addCardByName(name: String, count: Int = 1) {
            val c = all.find { it.name.equals(name, ignoreCase = true) } ?: DdsCardCatalog.SHADOW_SPECTER
            repeat(count) { deck.add(c) }
        }

        when (opponentName) {
            "Tristan Taylor" -> {
                // Novice deck: weak monsters <= 600 ATK
                addCardByName("Bone Mouse", 4)
                addCardByName("Shadow Specter", 4)
                addCardByName("Monster Eye", 3)
                addCardByName("Wretched Ghost of the Attic", 4)
                addCardByName("Zone Eater", 3)
                addCardByName("Fungi of the Musk", 3)
                addCardByName("Milus Radiant", 3)
                addCardByName("Turu-Purun", 3)
                addCardByName("Mechanical Spider", 3)
                addCardByName("Sparks", 4)
                addCardByName("Mooyan Curry", 3)
                addCardByName("Oscillo Hero #2", 3)
            }
            "Joey Wheeler" -> {
                // Joey's classic battle city starter mix
                addCardByName("Swordsman of Landstar", 4)
                addCardByName("Mountain Warrior", 4)
                addCardByName("Baby Dragon", 4)
                addCardByName("Time Wizard", 3)
                addCardByName("Air Marmot of Nefariousness", 3)
                addCardByName("Muka Muka", 3)
                addCardByName("Kaminarikozou", 3)
                addCardByName("Yamatano Dragon Scroll", 3)
                addCardByName("Hinotama", 3)
                addCardByName("Sparks", 3)
                addCardByName("Reinforcements", 3)
                addCardByName("Red-Eyes B. Dragon", 1) // Trump card!
                addCardByName("Flame Cerebrus", 3)
            }
            "Mai Valentine" -> {
                // Winged Beasts, Harpie archetype
                addCardByName("Harpie Lady", 4)
                addCardByName("Droll Bird", 4)
                addCardByName("Winged Dragon #1", 4)
                addCardByName("Bladefly", 4)
                addCardByName("Sonic Maid", 4)
                addCardByName("Air Marmot of Nefariousness", 4)
                addCardByName("Boo Koo", 4)
                addCardByName("Sparks", 4)
                addCardByName("Hinotama", 4)
                addCardByName("Mooyan Curry", 4)
            }
            "Bakura" -> {
                // Spooky fiends, hand disruption
                addCardByName("White Magical Hat", 4)
                addCardByName("Wretched Ghost of the Attic", 4)
                addCardByName("Mystery Hand", 4)
                addCardByName("Hiro's Shadow Scout", 4)
                addCardByName("Shadow Specter", 4)
                addCardByName("Mech Mole Zombie", 4)
                addCardByName("Fungi of the Musk", 4)
                addCardByName("Morphing Jar", 4)
                addCardByName("Sparks", 4)
                addCardByName("Trap Hole", 4)
            }
            "Seto Kaiba" -> {
                // Stage Boss: high ATK beatdown & Blue-Eyes
                addCardByName("Battle Ox", 4)
                addCardByName("Hitotsu-Me Giant", 4)
                addCardByName("Ryu-Kishin", 4)
                addCardByName("Curse of Dragon", 4)
                addCardByName("Celtic Guardian", 4)
                addCardByName("Kaminarikozou", 4)
                addCardByName("Sparks", 4)
                addCardByName("Dark Hole", 2)
                addCardByName("Trap Hole", 3)
                addCardByName("Mirror Force", 3)
                addCardByName("Blue-Eyes White Dragon", 2)
                addCardByName("Twin-Headed Thunder Dragon", 2)
            }
            else -> {
                return getCanonicalStarterDeck()
            }
        }

        // Ensure exactly 40 cards
        while (deck.size < 40) {
            deck.add(DdsCardCatalog.SHADOW_SPECTER)
        }
        return deck.take(40).shuffled()
    }

    /**
     * Determines the card drop upon defeating a duelist.
     */
    private fun rollOpponentDrop(opponentName: String): Card {
        return when (opponentName) {
            "Tristan Taylor" -> listOf(
                DdsCardCatalog.OSCILLO_HERO_2,
                DdsCardCatalog.BONE_MOUSE,
                DdsCardCatalog.SHADOW_SPECTER,
                DdsCardCatalog.MONSTER_EYE,
                DdsCardCatalog.SPARKS
            ).random()
            "Joey Wheeler" -> listOf(
                DdsCardCatalog.SWORDSMAN_OF_LANDSTAR,
                DdsCardCatalog.MOUNTAIN_WARRIOR,
                DdsCardCatalog.TIME_WIZARD,
                DdsCardCatalog.BABY_DRAGON,
                DdsCardCatalog.RED_EYES_B_DRAGON
            ).random()
            "Mai Valentine" -> listOf(
                DdsCardCatalog.HARPIE_LADY,
                DdsCardCatalog.SONIC_MAID,
                DdsCardCatalog.DROLL_BIRD,
                DdsCardCatalog.BLADEFLY
            ).random()
            "Bakura" -> listOf(
                DdsCardCatalog.WHITE_MAGICAL_HAT,
                DdsCardCatalog.MORPHING_JAR,
                DdsCardCatalog.HIRO_SHADOW_SCOUT,
                DdsCardCatalog.TRAP_HOLE
            ).random()
            "Seto Kaiba" -> listOf(
                DdsCardCatalog.BATTLE_OX,
                DdsCardCatalog.HITOTSU_ME_GIANT,
                DdsCardCatalog.TWIN_HEADED_THUNDER_DRAGON,
                DdsCardCatalog.BLUE_EYES_WHITE_DRAGON
            ).random()
            else -> DdsCardCatalog.SHADOW_SPECTER
        }
    }

    /**
     * Authentic Dark Duel Stories (GBC) On-The-Field Fusion Table.
     * When two eligible monsters are played on the same slot, they fuse into a stronger monster.
     */
    fun checkFusion(cardA: Card, cardB: Card): Card? {
        if (!cardA.isMonster || !cardB.isMonster) return null

        val types = setOf(cardA.monsterType, cardB.monsterType)
        val names = setOf(cardA.name, cardB.name)

        // Dragon + Thunder = Twin-Headed Thunder Dragon (2800/2100)
        if (types.contains(MonsterType.DRAGON) && types.contains(MonsterType.THUNDER)) {
            return DdsCardCatalog.TWIN_HEADED_THUNDER_DRAGON
        }
        // Dragon + Zombie = Zombie Dragon (1600/0)
        if (types.contains(MonsterType.DRAGON) && types.contains(MonsterType.ZOMBIE)) {
            return DdsCardCatalog.ZOMBIE_DRAGON
        }
        // Beast + Pyro = Flame Cerebrus (2100/1800)
        if (types.contains(MonsterType.BEAST) && (cardA.attribute == CardAttribute.FIRE || cardB.attribute == CardAttribute.FIRE || types.contains(MonsterType.PYRO))) {
            return DdsCardCatalog.FLAME_CEREBRUS
        }
        // Baby Dragon + Time Wizard = Thousand Dragon (2400/2000)
        if (names.contains("Baby Dragon") && names.contains("Time Wizard")) {
            return DdsCardCatalog.THOUSAND_DRAGON
        }
        // Machine + Warrior = Cyber Soldier (1500/1700)
        if (types.contains(MonsterType.MACHINE) && types.contains(MonsterType.WARRIOR)) {
            return DdsCardCatalog.CYBER_SOLDIER
        }

        return null
    }
}
