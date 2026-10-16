package com.yugioh.recomp.core.engine

/**
 * Canonical Game Boy Advance (GBA) card catalog for Yu-Gi-Oh! The Eternal Duelist Soul
 * and Worldwide Edition: Stairway to the Destined Duel.
 * Features iconic TCG Series 1, 2, and 3 cards with official stats, tribute tiers, and passcodes.
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
        description = "As long as this card remains face-up on the field, all Trap Cards cannot be activated. The effects of all face-up Trap Cards are negated.",
        password = "77585513"
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
        description = "Increases this card's ATK by 500 points for each Dragon-Type monster on your opponent's side of the field and in your opponent's Graveyard.",
        password = "78193831"
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
        description = "Increases this card's ATK by 300 points for every Dark Magician or Magician of Black Chaos in either player's Graveyard.",
        password = "38033121"
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
        description = "Any Equip Card equipped to this card is automatically destroyed.",
        password = "00423705"
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
        description = "A fiend with dark powers for confusing the enemy. Among the Fiend-Type monsters, this monster boasts considerable strength.",
        password = "70781052"
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
        description = "Special Summon 1 monster from your Graveyard in face-up Attack Position.",
        password = "97077563"
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
        description = "Negate all Spell Card effects on the field.",
        password = "61740673"
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
        description = "Take control of 1 of your opponent's face-up monsters.",
        password = "45986603"
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
        description = "Elf twins that alternate their attacks, overpowering enemies with raw magic power.",
        password = "69140098"
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
        description = "This wicked beast-warrior does every horrible thing imaginable and simply loves it.",
        password = "14898066"
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
        description = "A genie of the lamp that is at the complete command of its master.",
        password = "97590747"
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
        description = "A delicate elf that lacks offense, but has a terrific defense backed by mystical power.",
        password = "15025844"
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
        description = "A giant warrior made of stone. A punch from this creature has an earth-shaking impact.",
        password = "13039848"
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
        description = "FLIP: Target 1 monster on the field; destroy that target.",
        password = "54652250"
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
        description = "Target 1 monster in either player's Graveyard; Special Summon it to your field.",
        password = "83764718"
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
        description = "Target 1 monster your opponent controls; take control of it until the End Phase.",
        password = "04031928"
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
        description = "Opponent's monsters cannot declare attacks for 3 turns.",
        password = "72302403"
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
        description = "Draw 3 cards, then discard 2 cards from your hand.",
        password = "79571449"
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
        description = "Destroy all Spell and Trap Cards your opponent controls.",
        password = "18144506"
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
        description = "Destroy all Spell and Trap Cards on the field.",
        password = "19613556"
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
        description = "When an opponent's monster declares an attack: Negate the attack, and inflict damage to your opponent equal to its ATK.",
        password = "62279055"
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
        description = "A Harpie Lady outfitted with cybernetic armor, boosting its aerial assault prowess.",
        password = "80316585"
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
        description = "Bandit Keith's devastating mechanized dragon armed with triple rotating gun barrels.",
        password = "81480460"
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
        description = "Pegasus's bizarre ritual beast that absorbs opponent monsters and uses them as shields.",
        password = "64631466"
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
        description = "Joey's sword-wielding alligator that flies through the air on its trusty dragon mount.",
        password = "64428736"
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
        description = "When a monster is Summoned: Destroy all monsters on the field.",
        password = "53582587"
    )

    // --- Authentic The Eternal Duelist Soul (EDS) Starter Deck Cards ---
    val ARMORED_LIZARD = Card(
        id = 827,
        name = "Armored Lizard",
        atk = 1500,
        def = 1200,
        level = 4,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.REPTILE,
        description = "A lizard with a very tough hide and a vicious bite.",
        password = "15480588"
    )

    val BEAN_SOLDIER = Card(
        id = 828,
        name = "Bean Soldier",
        atk = 1400,
        def = 1300,
        level = 4,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.PLANT,
        description = "A plant warrior that attacks with bean seeds and a sword.",
        password = "84916669"
    )

    val BIG_INSECT = Card(
        id = 829,
        name = "Big Insect",
        atk = 1200,
        def = 1500,
        level = 4,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.INSECT,
        description = "A giant insect with a tough carapace and giant pincers.",
        password = "53606874"
    )

    val CELTIC_GUARDIAN_GBA = Card(
        id = 830,
        name = "Celtic Guardian",
        atk = 1400,
        def = 1200,
        level = 4,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.WARRIOR,
        description = "An elf who learned to wield a sword, he baffles enemies with lightning-fast attacks.",
        password = "91152256"
    )

    val CURSE_OF_DRAGON_GBA = Card(
        id = 831,
        name = "Curse of Dragon",
        atk = 2000,
        def = 1500,
        level = 5,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.DRAGON,
        description = "A dragon with bone wings and a skeletal visage, capable of releasing blue hellfire.",
        password = "28279543"
    )

    val ENCHANTING_MERMAID = Card(
        id = 832,
        name = "Enchanting Mermaid",
        atk = 1200,
        def = 900,
        level = 3,
        attribute = CardAttribute.WATER,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.FISH,
        description = "A beautiful mermaid that lures voyagers with her mesmerizing melody.",
        password = "75376965"
    )

    val FIEND_KRAKEN = Card(
        id = 833,
        name = "Fiend Kraken",
        atk = 1200,
        def = 1400,
        level = 4,
        attribute = CardAttribute.WATER,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.AQUA,
        description = "A giant squid that drags ocean vessels down into the abyssal depths.",
        password = "77456781"
    )

    val FIENDS_HAND = Card(
        id = 834,
        name = "Fiend's Hand",
        atk = 600,
        def = 600,
        level = 2,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.ZOMBIE,
        description = "Arms that reach out from the underworld to pull victims to their doom.",
        password = "52800428"
    )

    val GREAT_WHITE = Card(
        id = 835,
        name = "Great White",
        atk = 1600,
        def = 800,
        level = 4,
        attribute = CardAttribute.WATER,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.FISH,
        description = "A giant white shark with razor-sharp teeth that tears prey apart.",
        password = "13429800"
    )

    val GRIFFORE = Card(
        id = 836,
        name = "Griffore",
        atk = 1200,
        def = 1500,
        level = 4,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.BEAST,
        description = "This monster's tough hide deflects almost any blow.",
        password = "53829412"
    )

    val HORN_IMP = Card(
        id = 837,
        name = "Horn Imp",
        atk = 1300,
        def = 1000,
        level = 4,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.FIEND,
        description = "A small fiend with sharp horns that lurks in the shadows.",
        password = "69669405"
    )

    val ILLUSIONIST_FACELESS_MAGE = Card(
        id = 838,
        name = "Illusionist Faceless Mage",
        atk = 1200,
        def = 2200,
        level = 5,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.SPELLCASTER,
        description = "Manipulates enemy attacks with illusions and blinding enchantments.",
        password = "28546905"
    )

    val KILLER_NEEDLE = Card(
        id = 839,
        name = "Killer Needle",
        atk = 1200,
        def = 1000,
        level = 4,
        attribute = CardAttribute.WIND,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.INSECT,
        description = "A huge bee with exceptional strength that stings with fatal venom.",
        password = "88979991"
    )

    val MINOMUSHI_WARRIOR = Card(
        id = 840,
        name = "Minomushi Warrior",
        atk = 1300,
        def = 1200,
        level = 4,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.ROCK,
        description = "A warrior wrapped in a heavy stone cloak for enhanced defense.",
        password = "46864966"
    )

    val NEO_MAGIC_SWORDSMAN = Card(
        id = 841,
        name = "Neo the Magic Swordsman",
        atk = 1700,
        def = 1000,
        level = 4,
        attribute = CardAttribute.LIGHT,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.SPELLCASTER,
        description = "A dimensional drifter who fights with a blade imbued with mystical space energy.",
        password = "50930991"
    )

    val PETIT_DRAGON = Card(
        id = 842,
        name = "Petit Dragon",
        atk = 600,
        def = 700,
        level = 2,
        attribute = CardAttribute.WIND,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.DRAGON,
        description = "A very small dragon that breathes gentle flames.",
        password = "75356564"
    )

    val STONE_ARMADILLER = Card(
        id = 843,
        name = "Stone Armadiller",
        atk = 1000,
        def = 1200,
        level = 3,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.ROCK,
        description = "A monster with a rock-hard back that curls into a ball to repel attacks.",
        password = "63102017"
    )

    val SUPPORTER_IN_THE_SHADOWS = Card(
        id = 844,
        name = "Supporter in the Shadows",
        atk = 1000,
        def = 1000,
        level = 2,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.WARRIOR,
        description = "A warrior that assists its comrades from the covert shroud of darkness.",
        password = "56282322"
    )

    val THE_DRDEK = Card(
        id = 845,
        name = "The Drdek",
        atk = 700,
        def = 800,
        level = 3,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.FIEND,
        description = "A bipedal eye fiend that hypnotizes enemies with its unblinking stare.",
        password = "08944575"
    )

    val TOMOZAURUS = Card(
        id = 846,
        name = "Tomozaurus",
        atk = 500,
        def = 400,
        level = 2,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.DINOSAUR,
        description = "Small and fast, this dinosaur snaps viciously with tiny jaws.",
        password = "46457856"
    )

    val TWO_HEADED_KING_REX = Card(
        id = 847,
        name = "Two-Headed King Rex",
        atk = 1600,
        def = 1200,
        level = 4,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.DINOSAUR,
        description = "A monstrous two-headed dinosaur with twice the biting savagery.",
        password = "94119974"
    )

    val WICKED_MIRROR = Card(
        id = 848,
        name = "Wicked Mirror",
        atk = 700,
        def = 600,
        level = 2,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.FIEND,
        description = "A cursed looking glass that reflects evil intentions back upon the viewer.",
        password = "15150365"
    )

    val WINGED_CLEAVER = Card(
        id = 849,
        name = "Winged Cleaver",
        atk = 700,
        def = 700,
        level = 2,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.INSECT,
        description = "An insect with razor-sharp scythe forelegs that flies through foliage.",
        password = "39175997"
    )

    val SANGAN = Card(
        id = 850,
        name = "Sangan",
        atk = 1000,
        def = 600,
        level = 3,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.FIEND,
        description = "When this card is sent to the Graveyard: Add 1 monster with 1500 or less ATK from your Deck to your hand.",
        password = "26202165"
    )

    val NEEDLE_WORM = Card(
        id = 851,
        name = "Needle Worm",
        atk = 750,
        def = 600,
        level = 2,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.INSECT,
        description = "FLIP: Discard the top 5 cards of your opponent's Deck to the Graveyard.",
        password = "81843628"
    )

    val SEVEN_COLORED_FISH = Card(
        id = 852,
        name = "7 Colored Fish",
        atk = 1800,
        def = 800,
        level = 4,
        attribute = CardAttribute.WATER,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.FISH,
        description = "A rare rainbow-hued fish that moves gracefully beneath deep waters.",
        password = "23771716"
    )

    val ANTHROSAURUS = Card(
        id = 853,
        name = "Anthrosaurus",
        atk = 1000,
        def = 850,
        level = 3,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.DINOSAUR,
        description = "A bipedal man-dinosaur with high intelligence and primal savagery.",
        password = "89904598"
    )

    val AXE_RAIDER = Card(
        id = 854,
        name = "Axe Raider",
        atk = 1700,
        def = 1150,
        level = 4,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.WARRIOR,
        description = "An axe-wielding monster of formidable strength and martial prowess.",
        password = "48305365"
    )

    val BATTLE_STEER = Card(
        id = 855,
        name = "Battle Steer",
        atk = 1800,
        def = 1300,
        level = 5,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.BEAST_WARRIOR,
        description = "A bull warrior with immense horns that impales all who cross its path.",
        password = "18246479"
    )

    val CANDLE_OF_FATE = Card(
        id = 856,
        name = "Candle of Fate",
        atk = 600,
        def = 600,
        level = 2,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.FIEND,
        description = "A candle that burns away the lifespan of its unlucky victims.",
        password = "47695416"
    )

    val CRAWLING_DRAGON = Card(
        id = 857,
        name = "Crawling Dragon",
        atk = 1600,
        def = 1400,
        level = 5,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.DRAGON,
        description = "A serpentine dragon that creeps along the earth spewing toxic breath.",
        password = "67494157"
    )

    val CRAWLING_DRAGON_2 = Card(
        id = 858,
        name = "Crawling Dragon #2",
        atk = 1600,
        def = 1200,
        level = 4,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.DINOSAUR,
        description = "A powerful bipedal reptile with sharp claws and iron teeth.",
        password = "38289717"
    )

    val DARKWORLD_THORNS = Card(
        id = 859,
        name = "Darkworld Thorns",
        atk = 1200,
        def = 900,
        level = 3,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.PLANT,
        description = "Thorned vines from the dark realm that entangle unsuspecting prey.",
        password = "43500484"
    )

    val GANIGUMO = Card(
        id = 860,
        name = "Ganigumo",
        atk = 600,
        def = 800,
        level = 2,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.INSECT,
        description = "A crab-like arachnid that binds prey in sticky silk webbing.",
        password = "34539176"
    )

    val GOKIBORE = Card(
        id = 861,
        name = "Gokibore",
        atk = 1200,
        def = 1400,
        level = 4,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.INSECT,
        description = "A giant cockroach that scurries at blinding speed and never surrenders.",
        password = "15367030"
    )

    val HARPIE_LADY_GBA = Card(
        id = 862,
        name = "Harpie Lady",
        atk = 1300,
        def = 1400,
        level = 4,
        attribute = CardAttribute.WIND,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.WINGED_BEAST,
        description = "This human-shaped animal with wings is beautiful to watch but deadly in battle.",
        password = "76812113"
    )

    val KAMINARIKOZOU = Card(
        id = 863,
        name = "Kaminarikozou",
        atk = 700,
        def = 600,
        level = 2,
        attribute = CardAttribute.LIGHT,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.THUNDER,
        description = "A mischievous thunder child that summons sudden lightning strikes.",
        password = "15510988"
    )

    val KOUMORI_DRAGON = Card(
        id = 864,
        name = "Koumori Dragon",
        atk = 1500,
        def = 1200,
        level = 4,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.DRAGON,
        description = "A bat-winged dragon that flies in darkness and bites viciously.",
        password = "67724856"
    )

    val LALA_LI_OON = Card(
        id = 865,
        name = "LaLa Li-oon",
        atk = 600,
        def = 600,
        level = 2,
        attribute = CardAttribute.WIND,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.THUNDER,
        description = "A cloud creature capable of producing localized electric storms.",
        password = "09076207"
    )

    val LESSER_DRAGON = Card(
        id = 866,
        name = "Lesser Dragon",
        atk = 1200,
        def = 1000,
        level = 4,
        attribute = CardAttribute.WIND,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.DRAGON,
        description = "A minor dragon with fledgling wings and fiery claws.",
        password = "55444629"
    )

    val SKULL_STALKER = Card(
        id = 867,
        name = "Skull Stalker",
        atk = 900,
        def = 800,
        level = 3,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.WARRIOR,
        description = "A skeletal assassin that hunts victims silently through the night.",
        password = "54844984"
    )

    val TAKRIMINOS = Card(
        id = 868,
        name = "Takriminos",
        atk = 1500,
        def = 1200,
        level = 4,
        attribute = CardAttribute.WATER,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.SEA_SERPENT,
        description = "A sea serpent that creates whirlpools to swallow seafaring vessels.",
        password = "44073668"
    )

    val BATTLE_OX_GBA = Card(
        id = 869,
        name = "Battle Ox",
        atk = 1700,
        def = 1000,
        level = 4,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.BEAST_WARRIOR,
        description = "A monster with tremendous power, it destroys all opponents with its battleaxe.",
        password = "05053103"
    )

    val DARK_GRAY = Card(
        id = 870,
        name = "Dark Gray",
        atk = 800,
        def = 900,
        level = 3,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.BEAST,
        description = "Entirely gray in color, this beast blends with shadow and strikes silently.",
        password = "09159938"
    )

    val FERAL_IMP = Card(
        id = 871,
        name = "Feral Imp",
        atk = 1300,
        def = 1400,
        level = 4,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.FIEND,
        description = "A playful little imp that lurks in the dark, waiting to attack an unwary enemy.",
        password = "41392891"
    )

    val HITOTSU_ME_GIANT = Card(
        id = 872,
        name = "Hitotsu-Me Giant",
        atk = 1200,
        def = 1000,
        level = 4,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.BEAST_WARRIOR,
        description = "A one-eyed behemoth with thick arms, beware of its crushing blow.",
        password = "76184692"
    )

    val JUDGE_MAN = Card(
        id = 873,
        name = "Judge Man",
        atk = 2200,
        def = 1500,
        level = 6,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.WARRIOR,
        description = "This warrior will never give up on his quest for pure justice.",
        password = "30113694"
    )

    val LARVAS = Card(
        id = 874,
        name = "Larvas",
        atk = 800,
        def = 1000,
        level = 3,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.BEAST,
        description = "A bird with ferocious claws that slices with surprising velocity.",
        password = "21217011"
    )

    val MYSTIC_HORSEMAN = Card(
        id = 875,
        name = "Mystic Horseman",
        atk = 1300,
        def = 1550,
        level = 4,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.BEAST,
        description = "Half man and half horse, this creature is known for extreme speed.",
        password = "68516705"
    )

    val ROGUE_DOLL = Card(
        id = 876,
        name = "Rogue Doll",
        atk = 1600,
        def = 1000,
        level = 4,
        attribute = CardAttribute.LIGHT,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.SPELLCASTER,
        description = "A seemingly lovely doll with mystical powers beyond human comprehension.",
        password = "91939608"
    )

    val SNAKEYASHI = Card(
        id = 877,
        name = "Snakeyashi",
        atk = 1000,
        def = 1200,
        level = 4,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.PLANT,
        description = "A plant resembling a serpent that shoots poisoned barbs.",
        password = "29802344"
    )

    val SWORD_ARM_OF_DRAGON = Card(
        id = 878,
        name = "Sword Arm of Dragon",
        atk = 1750,
        def = 2030,
        level = 6,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.DINOSAUR,
        description = "A dinosaur whose front limbs resemble jagged swords.",
        password = "13069066"
    )

    val THE_WANDERING_DOOMED = Card(
        id = 879,
        name = "The Wandering Doomed",
        atk = 800,
        def = 600,
        level = 2,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.ZOMBIE,
        description = "A skeletal monster that wanders aimlessly through the netherworld.",
        password = "93788854"
    )

    val URABY = Card(
        id = 880,
        name = "Uraby",
        atk = 1500,
        def = 800,
        level = 4,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.DINOSAUR,
        description = "Fast on its feet, this dinosaur runs down opponents with terrifying tenacity.",
        password = "01784619"
    )

    val WINGED_DRAGON_GUARDIAN = Card(
        id = 881,
        name = "Winged Dragon, Guardian of the Fortress #1",
        atk = 1400,
        def = 1200,
        level = 4,
        attribute = CardAttribute.WIND,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.DRAGON,
        description = "A dragon commonly found guarding mountain fortresses. Its signature attack is a diving fire glide.",
        password = "87796900"
    )

    val BLACK_PENDANT = Card(
        id = 882,
        name = "Black Pendant",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        description = "The equipped monster gains 500 ATK. When this card is sent to the Graveyard, inflict 500 damage to your opponent.",
        password = "65169794"
    )

    val MEGAMORPH = Card(
        id = 883,
        name = "Megamorph",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        description = "While your LP is lower than your opponent's, the equipped monster's ATK is doubled.",
        password = "22046459"
    )

    val MYSTICAL_SPACE_TYPHOON = Card(
        id = 884,
        name = "Mystical Space Typhoon",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        description = "Target 1 Spell/Trap on the field; destroy that target.",
        password = "05318639"
    )

    val POT_OF_GREED = Card(
        id = 885,
        name = "Pot of Greed",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        description = "Draw 2 cards from your Deck.",
        password = "55144522"
    )

    val RAIGEKI = Card(
        id = 886,
        name = "Raigeki",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        description = "Destroy all monsters your opponent controls.",
        password = "12580477"
    )

    val DARK_HOLE = Card(
        id = 887,
        name = "Dark Hole",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        description = "Destroy all monsters on the field.",
        password = "53129443"
    )

    val PREMATURE_BURIAL = Card(
        id = 888,
        name = "Premature Burial",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        description = "Pay 800 LP, then target 1 monster in your Graveyard; Special Summon it in Attack Position and equip it with this card.",
        password = "70828912"
    )

    val MAGIC_JAMMER = Card(
        id = 889,
        name = "Magic Jammer",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.TRAP,
        cardType = CardType.TRAP,
        monsterType = MonsterType.NONE,
        description = "When a Spell Card is activated: Discard 1 card; negate the activation, and if you do, destroy it.",
        password = "77414702"
    )

    val MIRROR_FORCE = Card(
        id = 890,
        name = "Mirror Force",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.TRAP,
        cardType = CardType.TRAP,
        monsterType = MonsterType.NONE,
        description = "When an opponent's monster declares an attack: Destroy all Attack Position monsters your opponent controls.",
        password = "44095762"
    )

    val SEVEN_TOOLS_BANDIT = Card(
        id = 891,
        name = "Seven Tools of the Bandit",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.TRAP,
        cardType = CardType.TRAP,
        monsterType = MonsterType.NONE,
        description = "When a Trap Card is activated: Pay 1000 LP; negate the activation, and if you do, destroy it.",
        password = "03819470"
    )

    val TRAP_HOLE = Card(
        id = 892,
        name = "Trap Hole",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.TRAP,
        cardType = CardType.TRAP,
        monsterType = MonsterType.NONE,
        description = "When your opponent Summons a monster with 1000 or more ATK: Destroy that monster.",
        password = "04206964"
    )

    val RING_OF_DESTRUCTION = Card(
        id = 893,
        name = "Ring of Destruction",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.TRAP,
        cardType = CardType.TRAP,
        monsterType = MonsterType.NONE,
        description = "Target 1 face-up monster; destroy that target, and if you do, inflict damage to both players equal to its ATK.",
        password = "83555666"
    )

    val AQUA_MADOOR = Card(
        id = 900,
        name = "Aqua Madoor",
        atk = 1200,
        def = 2000,
        level = 4,
        attribute = CardAttribute.WATER,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.SPELLCASTER,
        description = "A wizard of the waters that summons a massive wall of liquid to defend against attacks.",
        password = "85639857"
    )

    val CYBER_FALCON = Card(
        id = 901,
        name = "Cyber Falcon",
        atk = 1400,
        def = 1200,
        level = 4,
        attribute = CardAttribute.WIND,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.MACHINE,
        description = "A jet-powered mechanical bird that attacks with razor-sharp talons.",
        password = "30655537"
    )

    val BIG_EYE = Card(
        id = 902,
        name = "Big Eye",
        atk = 1200,
        def = 1000,
        level = 4,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.FIEND,
        description = "A curious fiend with a giant hypnotic eye that peers into the unseen.",
        password = "16768380"
    )

    val SKELENGEL = Card(
        id = 903,
        name = "Skelengel",
        atk = 900,
        def = 400,
        level = 2,
        attribute = CardAttribute.LIGHT,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.FAIRY,
        description = "FLIP: Draw 1 card from your Deck.",
        password = "60694662"
    )

    val BEAVER_WARRIOR = Card(
        id = 904,
        name = "Beaver Warrior",
        atk = 1200,
        def = 1500,
        level = 4,
        attribute = CardAttribute.EARTH,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.BEAST_WARRIOR,
        description = "What this creature lacks in size it makes up for in defense when battling in the prairie.",
        password = "32452818"
    )

    val PETIT_ANGEL = Card(
        id = 905,
        name = "Petit Angel",
        atk = 600,
        def = 900,
        level = 3,
        attribute = CardAttribute.LIGHT,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.FAIRY,
        description = "A quick-moving and mischievous little angel.",
        password = "38142739"
    )

    val SHINING_FRIENDSHIP = Card(
        id = 906,
        name = "Shining Friendship",
        atk = 1300,
        def = 1100,
        level = 4,
        attribute = CardAttribute.LIGHT,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.FAIRY,
        description = "The peacemaker who brings peace to the hearts of all living creatures.",
        password = "82085619"
    )

    val HAPPY_LOVER = Card(
        id = 907,
        name = "Happy Lover",
        atk = 800,
        def = 500,
        level = 2,
        attribute = CardAttribute.LIGHT,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.FAIRY,
        description = "A tiny fairy with radiant wings that shoots heart-shaped beams from its wand.",
        password = "99001362"
    )

    val WHITE_ZOMBIE = Card(
        id = 908,
        name = "White Zombie",
        atk = 1200,
        def = 400,
        level = 3,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.ZOMBIE,
        description = "A decaying zombie with sunken eyes and ghastly white flesh.",
        password = "73891874"
    )

    val ARMORED_ZOMBIE = Card(
        id = 909,
        name = "Armored Zombie",
        atk = 1500,
        def = 0,
        level = 3,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.ZOMBIE,
        description = "A ferocious zombie warrior clad in rusted chainmail.",
        password = "20277860"
    )

    val DRAGON_ZOMBIE = Card(
        id = 910,
        name = "Dragon Zombie",
        atk = 1600,
        def = 0,
        level = 3,
        attribute = CardAttribute.DARK,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.ZOMBIE,
        description = "A dragon revived by dark magic; its breath is toxic and lethal.",
        password = "66672569"
    )

    val BABY_DRAGON_GBA = Card(
        id = 911,
        name = "Baby Dragon",
        atk = 1200,
        def = 700,
        level = 3,
        attribute = CardAttribute.WIND,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.DRAGON,
        description = "Much more than just a child, this dragon is gifted with untapped power.",
        password = "88819587"
    )

    val TIME_WIZARD_GBA = Card(
        id = 912,
        name = "Time Wizard",
        atk = 500,
        def = 400,
        level = 2,
        attribute = CardAttribute.LIGHT,
        cardType = CardType.MONSTER,
        monsterType = MonsterType.SPELLCASTER,
        description = "Joey's lucky pendulum wizard capable of aging monsters forward in time.",
        password = "71625222"
    )

    val WABOKU_GBA = Card(
        id = 913,
        name = "Waboku",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.TRAP,
        cardType = CardType.TRAP,
        monsterType = MonsterType.NONE,
        description = "Make all battle damage inflicted by monsters on your opponent's side of the field 0 during this turn.",
        password = "12607053"
    )

    val DIAN_KETO_GBA = Card(
        id = 914,
        name = "Dian Keto the Cure Maiden",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.SPELL,
        cardType = CardType.SPELL,
        monsterType = MonsterType.NONE,
        description = "Increases your Life Points by 1000 points.",
        password = "84257640"
    )

    val SKULL_DICE = Card(
        id = 915,
        name = "Skull Dice",
        atk = 0,
        def = 0,
        level = 0,
        attribute = CardAttribute.TRAP,
        cardType = CardType.TRAP,
        monsterType = MonsterType.NONE,
        description = "Roll a 6-sided die. All monsters currently controlled by your opponent lose ATK and DEF equal to the result x 100.",
        password = "00126218"
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
        TORRENTIAL_TRIBUTE,
        ARMORED_LIZARD,
        BEAN_SOLDIER,
        BIG_INSECT,
        CELTIC_GUARDIAN_GBA,
        CURSE_OF_DRAGON_GBA,
        ENCHANTING_MERMAID,
        FIEND_KRAKEN,
        FIENDS_HAND,
        GREAT_WHITE,
        GRIFFORE,
        HORN_IMP,
        ILLUSIONIST_FACELESS_MAGE,
        KILLER_NEEDLE,
        MINOMUSHI_WARRIOR,
        NEO_MAGIC_SWORDSMAN,
        PETIT_DRAGON,
        STONE_ARMADILLER,
        SUPPORTER_IN_THE_SHADOWS,
        THE_DRDEK,
        TOMOZAURUS,
        TWO_HEADED_KING_REX,
        WICKED_MIRROR,
        WINGED_CLEAVER,
        SANGAN,
        NEEDLE_WORM,
        SEVEN_COLORED_FISH,
        ANTHROSAURUS,
        AXE_RAIDER,
        BATTLE_STEER,
        CANDLE_OF_FATE,
        CRAWLING_DRAGON,
        CRAWLING_DRAGON_2,
        DARKWORLD_THORNS,
        GANIGUMO,
        GOKIBORE,
        HARPIE_LADY_GBA,
        KAMINARIKOZOU,
        KOUMORI_DRAGON,
        LALA_LI_OON,
        LESSER_DRAGON,
        SKULL_STALKER,
        TAKRIMINOS,
        BATTLE_OX_GBA,
        DARK_GRAY,
        FERAL_IMP,
        HITOTSU_ME_GIANT,
        JUDGE_MAN,
        LARVAS,
        MYSTIC_HORSEMAN,
        ROGUE_DOLL,
        SNAKEYASHI,
        SWORD_ARM_OF_DRAGON,
        THE_WANDERING_DOOMED,
        URABY,
        WINGED_DRAGON_GUARDIAN,
        BLACK_PENDANT,
        MEGAMORPH,
        MYSTICAL_SPACE_TYPHOON,
        POT_OF_GREED,
        RAIGEKI,
        DARK_HOLE,
        PREMATURE_BURIAL,
        MAGIC_JAMMER,
        MIRROR_FORCE,
        SEVEN_TOOLS_BANDIT,
        TRAP_HOLE,
        RING_OF_DESTRUCTION,
        AQUA_MADOOR,
        CYBER_FALCON,
        BIG_EYE,
        SKELENGEL,
        BEAVER_WARRIOR,
        PETIT_ANGEL,
        SHINING_FRIENDSHIP,
        HAPPY_LOVER,
        WHITE_ZOMBIE,
        ARMORED_ZOMBIE,
        DRAGON_ZOMBIE,
        BABY_DRAGON_GBA,
        TIME_WIZARD_GBA,
        WABOKU_GBA,
        DIAN_KETO_GBA,
        SKULL_DICE
    )
}
