package com.yugioh.recomp.core.engine

/**
 * Attributes used in Yu-Gi-Oh! Dark Duel Stories & classic Yu-Gi-Oh!
 */
enum class CardAttribute(val displayName: String, val colorHex: Long) {
    LIGHT("LIGHT", 0xFFF59E0B),
    DARK("DARK", 0xFF7C3AED),
    FIRE("FIRE", 0xFFEF4444),
    WATER("WATER", 0xFF0284C7),
    EARTH("EARTH", 0xFFB45309),
    WIND("WIND", 0xFF10B981),
    THUNDER("THUNDER", 0xFFEAB308),
    FOREST("FOREST", 0xFF059669),
    FIEND("FIEND", 0xFF9333EA),
    DREAMS("DREAMS", 0xFFEC4899),
    DIVINE("DIVINE", 0xFFFACC15),
    SPELL("SPELL", 0xFF0D9488),
    TRAP("TRAP", 0xFFBE185D);

    /**
     * Dark Duel Stories (GBC) elemental alignment dominance wheel.
     * When a monster with an alignment advantage battles a disadvantaged monster,
     * the disadvantaged monster is instantly destroyed regardless of ATK/DEF.
     */
    fun hasAlignmentAdvantageOver(other: CardAttribute): Boolean {
        return when (this) {
            DARK -> other == LIGHT
            LIGHT -> other == FIEND
            FIEND -> other == DREAMS
            DREAMS -> other == FIRE
            FIRE -> other == FOREST
            FOREST -> other == WIND
            WIND -> other == EARTH
            EARTH -> other == THUNDER
            THUNDER -> other == WATER
            WATER -> other == FIRE
            else -> false
        }
    }
}

enum class CardType(val displayName: String) {
    MONSTER("Monster"),
    SPELL("Spell"),
    TRAP("Trap")
}

enum class MonsterType(val displayName: String) {
    DRAGON("Dragon"),
    SPELLCASTER("Spellcaster"),
    ZOMBIE("Zombie"),
    WARRIOR("Warrior"),
    BEAST_WARRIOR("Beast-Warrior"),
    BEAST("Beast"),
    WINGED_BEAST("Winged Beast"),
    FIEND("Fiend"),
    FAIRY("Fairy"),
    INSECT("Insect"),
    DINOSAUR("Dinosaur"),
    REPTILE("Reptile"),
    FISH("Fish"),
    SEA_SERPENT("Sea Serpent"),
    MACHINE("Machine"),
    THUNDER("Thunder"),
    AQUA("Aqua"),
    PYRO("Pyro"),
    ROCK("Rock"),
    PLANT("Plant"),
    NONE("None")
}

/**
 * Canonical card definition parsed dynamically from user ROM.
 */
data class Card(
    val id: Int,
    val name: String,
    val atk: Int,
    val def: Int,
    val level: Int,
    val attribute: CardAttribute,
    val cardType: CardType,
    val monsterType: MonsterType = MonsterType.NONE,
    val description: String = "",
    val password: String? = null
) {
    val isMonster: Boolean get() = cardType == CardType.MONSTER
    val isSpell: Boolean get() = cardType == CardType.SPELL
    val isTrap: Boolean get() = cardType == CardType.TRAP
    val isEquipSpell: Boolean get() = name in listOf("Axe of Despair", "Malevolent Nuzzler", "Black Pendant", "Megamorph", "Horn of the Unicorn")
    val tributeCountRequired: Int get() = when {
        level <= 4 -> 0
        level in 5..6 -> 1
        else -> 2
    }

    /**
     * Authentic Dark Duel Stories (GBC) Deck Cost.
     * Monsters: (ATK + DEF) / 100.
     * Spells/Traps: Specific canonical values.
     */
    val deckCost: Int get() {
        if (!isMonster) {
            return when (name) {
                "Raigeki" -> 200
                "Dark Hole" -> 180
                "Harpy's Feather Duster", "Harpie's Feather Duster" -> 190
                "Pot of Greed" -> 150
                "Change of Heart" -> 180
                "Monster Reborn" -> 150
                "Dian Keto the Cure Maiden" -> 60
                "Sparks" -> 10
                "Hinotama" -> 15
                "Mooyan Curry" -> 10
                "Mirror Force" -> 200
                "Trap Hole" -> 90
                "Reinforcements" -> 30
                "Waboku" -> 50
                else -> 25
            }
        }
        return ((atk + def) / 100).coerceAtLeast(1)
    }
}
