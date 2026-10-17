package com.yugioh.recomp.core.engine

/**
 * Registry of canonical Fusion Monsters, Fusion Recipes, Ritual Monsters, and Ritual Spells.
 * Supports authentic TCG / video game Extra Deck and Polymerization & Ritual summon mechanics.
 */
data class FusionRecipe(
    val fusionMonster: Card,
    val materialNames: List<String>
)

data class RitualPair(
    val ritualSpell: Card,
    val ritualMonster: Card,
    val requiredLevel: Int
)

object FusionRitualRegistry {

    // === CANONICAL FUSION MONSTERS ===
    val BLUE_EYES_ULTIMATE_DRAGON = Card(
        id = 9101,
        name = "Blue-Eyes Ultimate Dragon",
        atk = 4500,
        def = 3800,
        level = 12,
        attribute = CardAttribute.LIGHT,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.DRAGON,
        password = "23995346"
    )

    val FLAME_SWORDSMAN = Card(
        id = 9102,
        name = "Flame Swordsman",
        atk = 1800,
        def = 1600,
        level = 5,
        attribute = CardAttribute.FIRE,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.WARRIOR,
        password = "45231177"
    )

    val THOUSAND_DRAGON = Card(
        id = 9103,
        name = "Thousand Dragon",
        atk = 2400,
        def = 2000,
        level = 7,
        attribute = CardAttribute.WIND,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.DRAGON,
        password = "41462083"
    )

    val TWIN_HEADED_THUNDER_DRAGON = Card(
        id = 9104,
        name = "Twin-Headed Thunder Dragon",
        atk = 2800,
        def = 2100,
        level = 7,
        attribute = CardAttribute.LIGHT,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.THUNDER,
        password = "54752875"
    )

    val GAIA_THE_DRAGON_CHAMPION = Card(
        id = 9105,
        name = "Gaia the Dragon Champion",
        atk = 2600,
        def = 2100,
        level = 7,
        attribute = CardAttribute.WIND,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.DRAGON,
        password = "66889139"
    )

    val B_SKULL_DRAGON = Card(
        id = 9106,
        name = "B. Skull Dragon",
        atk = 3200,
        def = 2500,
        level = 9,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.DRAGON,
        password = "11901678"
    )

    // === CANONICAL FUSION SPELL ===
    val POLYMERIZATION = Card(
        id = 9201,
        name = "Polymerization",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        password = "24094653"
    )

    // === CANONICAL RITUAL MONSTERS & SPELLS ===
    val BLACK_LUSTER_SOLDIER = Card(
        id = 9301,
        name = "Black Luster Soldier",
        atk = 3000,
        def = 2500,
        level = 8,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.WARRIOR,
        password = "05405694"
    )

    val BLACK_LUSTER_RITUAL = Card(
        id = 9302,
        name = "Black Luster Ritual",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        password = "55761792"
    )

    val MAGICIAN_OF_BLACK_CHAOS = Card(
        id = 9303,
        name = "Magician of Black Chaos",
        atk = 2800,
        def = 2600,
        level = 8,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.SPELLCASTER,
        password = "50702124"
    )

    val BLACK_MAGIC_RITUAL = Card(
        id = 9304,
        name = "Black Magic Ritual",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        password = "41426869"
    )

    val RELINQUISHED = Card(
        id = 9305,
        name = "Relinquished",
        atk = 0,
        def = 0,
        level = 1,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.SPELLCASTER,
        password = "64631466"
    )

    val BLACK_ILLUSION_RITUAL = Card(
        id = 9306,
        name = "Black Illusion Ritual",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        password = "64631466"
    )

    // === CANONICAL EQUIP SPELLS ===
    val AXE_OF_DESPAIR = Card(
        id = 9401,
        name = "Axe of Despair",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        password = "40619825"
    )

    val MALEVOLENT_NUZZLER = Card(
        id = 9402,
        name = "Malevolent Nuzzler",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        password = "99590524"
    )

    val HORN_OF_THE_UNICORN = Card(
        id = 9403,
        name = "Horn of the Unicorn",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        password = "64047146"
    )

    val MEGAMORPH_CARD = Card(
        id = 9404,
        name = "Megamorph",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        password = "22046459"
    )

    // === ALL FUSION RECIPES ===
    val FUSION_RECIPES = listOf(
        FusionRecipe(
            BLUE_EYES_ULTIMATE_DRAGON,
            listOf("Blue-Eyes White Dragon", "Blue-Eyes White Dragon", "Blue-Eyes White Dragon")
        ),
        FusionRecipe(
            FLAME_SWORDSMAN,
            listOf("Flame Manipulator", "Masaki the Legendary Swordsman")
        ),
        FusionRecipe(
            THOUSAND_DRAGON,
            listOf("Time Wizard", "Baby Dragon")
        ),
        FusionRecipe(
            TWIN_HEADED_THUNDER_DRAGON,
            listOf("Thunder Dragon", "Thunder Dragon")
        ),
        FusionRecipe(
            GAIA_THE_DRAGON_CHAMPION,
            listOf("Gaia The Fierce Knight", "Curse of Dragon")
        ),
        FusionRecipe(
            B_SKULL_DRAGON,
            listOf("Red-Eyes B. Dragon", "Summoned Skull")
        )
    )

    // === ALL RITUAL PAIRS ===
    val RITUAL_PAIRS = listOf(
        RitualPair(BLACK_LUSTER_RITUAL, BLACK_LUSTER_SOLDIER, requiredLevel = 8),
        RitualPair(BLACK_MAGIC_RITUAL, MAGICIAN_OF_BLACK_CHAOS, requiredLevel = 8),
        RitualPair(BLACK_ILLUSION_RITUAL, RELINQUISHED, requiredLevel = 1)
    )

    /**
     * Standard starter Extra Deck provided to every duelist.
     */
    fun getDefaultExtraDeck(): List<Card> = listOf(
        BLUE_EYES_ULTIMATE_DRAGON,
        FLAME_SWORDSMAN,
        THOUSAND_DRAGON,
        TWIN_HEADED_THUNDER_DRAGON,
        GAIA_THE_DRAGON_CHAMPION,
        B_SKULL_DRAGON
    )

    /**
     * Determines which fusion monsters can be summoned from the extra deck given current materials in hand and field.
     */
    fun findPossibleFusions(extraDeck: List<Card>, availableMonsters: List<Card>): List<Pair<FusionRecipe, List<Card>>> {
        val results = mutableListOf<Pair<FusionRecipe, List<Card>>>()

        for (recipe in FUSION_RECIPES) {
            if (extraDeck.none { it.name.equals(recipe.fusionMonster.name, ignoreCase = true) }) continue

            val remainingPool = availableMonsters.toMutableList()
            val matchedMaterials = mutableListOf<Card>()
            var possible = true

            for (reqName in recipe.materialNames) {
                val found = remainingPool.firstOrNull { it.name.equals(reqName, ignoreCase = true) }
                if (found != null) {
                    matchedMaterials.add(found)
                    remainingPool.remove(found)
                } else {
                    possible = false
                    break
                }
            }

            if (possible) {
                results.add(Pair(recipe, matchedMaterials))
            }
        }
        return results
    }

    /**
     * Checks if a ritual spell can be activated to summon its corresponding ritual monster.
     */
    fun checkRitualRequirements(
        ritualSpellName: String,
        handCards: List<Card>,
        fieldMonsters: List<Card>
    ): Pair<Card, List<Card>>? {
        val pair = RITUAL_PAIRS.find { it.ritualSpell.name.equals(ritualSpellName, ignoreCase = true) }
            ?: return null

        val ritualMonster = handCards.find { it.name.equals(pair.ritualMonster.name, ignoreCase = true) }
            ?: return null

        val tributePool = (handCards.filter { it.isMonster && it !== ritualMonster } + fieldMonsters)
            .sortedByDescending { it.level }

        var accumulatedLevel = 0
        val selectedTributes = mutableListOf<Card>()

        for (t in tributePool) {
            selectedTributes.add(t)
            accumulatedLevel += t.level
            if (accumulatedLevel >= pair.requiredLevel) {
                return Pair(ritualMonster, selectedTributes)
            }
        }
        return null
    }
}
