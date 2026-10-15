package com.yugioh.recomp.core.engine

/**
 * Canonical Game Boy Advance (GBA) card catalog for Yu-Gi-Oh! The Eternal Duelist Soul
 * and Worldwide Edition: Stairway to the Destined Duel.
 * Features iconic TCG Series 1, 2, and 3 cards with official stats, tribute tiers, and effects.
 */
object GbaCardCatalog {

    // --- Iconic GBA Bosses & Tribute Monsters ---
    val JINZO = Card(
        id = 801,
        name = "Jinzo",
        atk = 2400,
        def = 1500,
        level = 6,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.MACHINE,
        description = "As long as this card remains face-up on the field, all Trap Cards cannot be activated. The effects of all face-up Trap Cards are negated."
    )

    val BUSTER_BLADER = Card(
        id = 802,
        name = "Buster Blader",
        atk = 2600,
        def = 2300,
        level = 7,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.WARRIOR,
        description = "Increases this card's ATK by 500 points for each Dragon-Type monster on your opponent's side of the field and in your opponent's Graveyard."
    )

    val DARK_MAGICIAN_GIRL = Card(
        id = 803,
        name = "Dark Magician Girl",
        atk = 2000,
        def = 1700,
        level = 6,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.SPELLCASTER,
        description = "Increases this card's ATK by 300 points for every Dark Magician or Magician of Black Chaos in either player's Graveyard."
    )

    val GEARFRIED_IRON_KNIGHT = Card(
        id = 804,
        name = "Gearfried the Iron Knight",
        atk = 1800,
        def = 1600,
        level = 4,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.WARRIOR,
        description = "Any Equip Card equipped to this card is automatically destroyed."
    )

    val SUMMONED_SKULL_GBA = Card(
        id = 805,
        name = "Summoned Skull",
        atk = 2500,
        def = 1200,
        level = 6,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.FIEND,
        description = "A fiend with dark powers for confusing the enemy. Among the Fiend-Type monsters, this monster boasts considerable strength."
    )

    // --- Key GBA Traps & Spells ---
    val CALL_OF_THE_HAUNTED = Card(
        id = 806,
        name = "Call of the Haunted",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.TRAP,
        cardType = CardType.TRAP,
        monsterType = MonsterType.NONE,
        description = "Special Summon 1 monster from your Graveyard in face-up Attack Position."
    )

    val IMPERIAL_ORDER = Card(
        id = 807,
        name = "Imperial Order",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.TRAP,
        cardType = CardType.TRAP,
        monsterType = MonsterType.NONE,
        description = "Negate all Spell Card effects on the field."
    )

    val SNATCH_STEAL = Card(
        id = 808,
        name = "Snatch Steal",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        description = "Take control of 1 of your opponent's face-up monsters."
    )

    val GEMINI_ELF = Card(
        id = 809,
        name = "Gemini Elf",
        atk = 1900,
        def = 900,
        level = 4,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.SPELLCASTER,
        description = "Elf twins that alternate their attacks, overpowering enemies with raw magic power."
    )

    val VORSE_RAIDER = Card(
        id = 810,
        name = "Vorse Raider",
        atk = 1900,
        def = 1200,
        level = 4,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.BEAST_WARRIOR,
        description = "This wicked beast-warrior does every horrible thing imaginable and simply loves it."
    )

    val LA_JINN = Card(
        id = 811,
        name = "La Jinn the Mystical Genie of the Lamp",
        atk = 1800,
        def = 1000,
        level = 4,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.FIEND,
        description = "A genie of the lamp that is at the complete command of its master."
    )

    val MYSTICAL_ELF = Card(
        id = 812,
        name = "Mystical Elf",
        atk = 800,
        def = 2000,
        level = 4,
        attribute = CardAttribute.LIGHT,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.SPELLCASTER,
        description = "A delicate elf that lacks offense, but has a terrific defense backed by mystical power."
    )

    val GIANT_SOLDIER_OF_STONE = Card(
        id = 813,
        name = "Giant Soldier of Stone",
        atk = 1300,
        def = 2000,
        level = 3,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.ROCK,
        description = "A giant warrior made of stone. A punch from this creature has an earth-shaking impact."
    )

    val MAN_EATER_BUG = Card(
        id = 814,
        name = "Man-Eater Bug",
        atk = 450,
        def = 600,
        level = 2,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.INSECT,
        description = "FLIP: Target 1 monster on the field; destroy that target."
    )

    val MONSTER_REBORN = Card(
        id = 815,
        name = "Monster Reborn",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        description = "Target 1 monster in either player's Graveyard; Special Summon it to your field."
    )

    val CHANGE_OF_HEART = Card(
        id = 816,
        name = "Change of Heart",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        description = "Target 1 monster your opponent controls; take control of it until the End Phase."
    )

    val SWORDS_OF_REVEALING_LIGHT = Card(
        id = 817,
        name = "Swords of Revealing Light",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        description = "Opponent's monsters cannot declare attacks for 3 turns."
    )

    val GRACEFUL_CHARITY = Card(
        id = 818,
        name = "Graceful Charity",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        description = "Draw 3 cards, then discard 2 cards from your hand."
    )

    val HARPIES_FEATHER_DUSTER = Card(
        id = 819,
        name = "Harpie's Feather Duster",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        description = "Destroy all Spell and Trap Cards your opponent controls."
    )

    val HEAVY_STORM = Card(
        id = 820,
        name = "Heavy Storm",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        description = "Destroy all Spell and Trap Cards on the field."
    )

    val MAGIC_CYLINDER = Card(
        id = 821,
        name = "Magic Cylinder",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.TRAP,
        cardType = CardType.TRAP,
        monsterType = MonsterType.NONE,
        description = "When an opponent's monster declares an attack: Negate the attack, and inflict damage to your opponent equal to its ATK."
    )

    val CYBER_HARPIE = Card(
        id = 822,
        name = "Cyber Harpie",
        atk = 1800,
        def = 1300,
        level = 4,
        attribute = CardAttribute.WIND,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.WINGED_BEAST,
        description = "A Harpie Lady outfitted with cybernetic armor, boosting its aerial assault prowess."
    )

    val BARREL_DRAGON = Card(
        id = 823,
        name = "Barrel Dragon",
        atk = 2600,
        def = 2200,
        level = 7,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.MACHINE,
        description = "Bandit Keith's devastating mechanized dragon armed with triple rotating gun barrels."
    )

    val RELINQUISHED = Card(
        id = 824,
        name = "Relinquished",
        atk = 0,
        def = 0,
        level = 1,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.SPELLCASTER,
        description = "Pegasus's bizarre ritual beast that absorbs opponent monsters and uses them as shields."
    )

    val ALLIGATORS_SWORD = Card(
        id = 825,
        name = "Alligator's Sword",
        atk = 1500,
        def = 1200,
        level = 4,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.BEAST_WARRIOR,
        description = "Joey's sword-wielding alligator that flies through the air on its trusty dragon mount."
    )

    val TORRENTIAL_TRIBUTE = Card(
        id = 826,
        name = "Torrential Tribute",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.TRAP,
        cardType = CardType.TRAP,
        monsterType = MonsterType.NONE,
        description = "When a monster is Summoned: Destroy all monsters on the field."
    )

    val ALL_GBA_CARDS = listOf(
        JINZO,
        BUSTER_BLADER,
        DARK_MAGICIAN_GIRL,
        GEARFRIED_IRON_KNIGHT,
        SUMMONED_SKULL_GBA,
        CALL_OF_THE_HAUNTED,
        IMPERIAL_ORDER,
        SNATCH_STEAL,
        GEMINI_ELF,
        VORSE_RAIDER,
        LA_JINN,
        MYSTICAL_ELF,
        GIANT_SOLDIER_OF_STONE,
        MAN_EATER_BUG,
        MONSTER_REBORN,
        CHANGE_OF_HEART,
        SWORDS_OF_REVEALING_LIGHT,
        GRACEFUL_CHARITY,
        HARPIES_FEATHER_DUSTER,
        HEAVY_STORM,
        MAGIC_CYLINDER,
        CYBER_HARPIE,
        BARREL_DRAGON,
        RELINQUISHED,
        ALLIGATORS_SWORD,
        TORRENTIAL_TRIBUTE
    )
}
