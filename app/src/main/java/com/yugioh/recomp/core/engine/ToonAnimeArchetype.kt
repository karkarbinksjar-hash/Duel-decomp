package com.yugioh.recomp.core.engine

/**
 * Registry of canonical anime archetypes: Toon World, Exodia pieces,
 * Searchers (Sangan / Witch of the Black Forest), Continuous Revival,
 * and Gamble Spells.
 */
object ToonAnimeArchetype {

    val TOON_WORLD = Card(
        id = 9501,
        name = "Toon World",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        password = "15259703"
    )

    val BLUE_EYES_TOON_DRAGON = Card(
        id = 9502,
        name = "Blue-Eyes Toon Dragon",
        atk = 3000,
        def = 2500,
        level = 8,
        attribute = CardAttribute.LIGHT,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.DRAGON,
        password = "53183600"
    )

    val TOON_SUMMONED_SKULL = Card(
        id = 9503,
        name = "Toon Summoned Skull",
        atk = 2500,
        def = 1200,
        level = 6,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.FIEND,
        password = "91842653"
    )

    val TOON_MERMAID = Card(
        id = 9504,
        name = "Toon Mermaid",
        atk = 1400,
        def = 1500,
        level = 4,
        attribute = CardAttribute.WATER,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.AQUA,
        password = "65458948"
    )

    val MANGA_RYU_RAN = Card(
        id = 9505,
        name = "Manga Ryu-Ran",
        atk = 2200,
        def = 2600,
        level = 7,
        attribute = CardAttribute.FIRE,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.DRAGON,
        password = "38369349"
    )

    val WITCH_OF_THE_BLACK_FOREST = Card(
        id = 9506,
        name = "Witch of the Black Forest",
        atk = 1100,
        def = 1200,
        level = 4,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.SPELLCASTER,
        password = "04335645"
    )

    val TIME_WIZARD = Card(
        id = 9507,
        name = "Time Wizard",
        atk = 500,
        def = 400,
        level = 2,
        attribute = CardAttribute.LIGHT,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.SPELLCASTER,
        password = "71625222"
    )

    val CUP_OF_ACE = Card(
        id = 9508,
        name = "Cup of Ace",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        password = "00441788"
    )

    val PREMATURE_BURIAL = Card(
        id = 9509,
        name = "Premature Burial",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        password = "70828912"
    )

    val CALL_OF_THE_HAUNTED = Card(
        id = 9510,
        name = "Call of the Haunted",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.TRAP,
        cardType = CardType.TRAP,
        monsterType = MonsterType.NONE,
        password = "97077563"
    )

    val SHEEP_TOKEN = Card(
        id = 9520,
        name = "Sheep Token",
        atk = 0,
        def = 0,
        level = 1,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.BEAST
    )

    val SCAPEGOAT = Card(
        id = 9521,
        name = "Scapegoat",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        password = "73915051"
    )

    val CHAIN_ENERGY = Card(
        id = 9522,
        name = "Chain Energy",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        password = "79323590"
    )

    val TOLL = Card(
        id = 9523,
        name = "Toll",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        password = "82003859"
    )

    val ALL_TOON_MONSTER_NAMES = setOf(
        "Blue-Eyes Toon Dragon",
        "Toon Summoned Skull",
        "Toon Mermaid",
        "Manga Ryu-Ran"
    )

    val EXODIA_CARD_NAMES = setOf(
        "Exodia the Forbidden One",
        "Right Arm of the Forbidden One",
        "Left Arm of the Forbidden One",
        "Right Leg of the Forbidden One",
        "Left Leg of the Forbidden One"
    )

    fun isToonMonster(card: Card): Boolean = ALL_TOON_MONSTER_NAMES.contains(card.name)
    fun isToonWorld(card: Card): Boolean = card.name.equals("Toon World", ignoreCase = true)

    /**
     * Checks if duelist has assembled all 5 pieces of Exodia in their hand.
     */
    fun hasAllExodiaPieces(hand: List<DuelCard>): Boolean {
        val namesInHand = hand.map { it.card.name }.toSet()
        return EXODIA_CARD_NAMES.all { namesInHand.contains(it) }
    }
}
