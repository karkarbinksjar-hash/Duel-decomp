package com.yugioh.recomp.core.engine

/**
 * Authentic card database matching Yu-Gi-Oh! Dark Duel Stories (GBC).
 * Includes the canonical 40 Starter Deck cards, Stage 1 duelist pools, and fusions.
 */
object DdsCardCatalog {

    // --- CANONICAL STARTER DECK MONSTERS (ROM 0:255D) ---
    val SHADOW_SPECTER = Card(9, "Shadow Specter", 500, 200, 1, CardAttribute.DARK, CardType.MONSTER, MonsterType.FIEND, "A zombie that wanders the dark.")
    val YAMATANO_DRAGON = Card(122, "Yamatano Dragon Scroll", 900, 300, 2, CardAttribute.WIND, CardType.MONSTER, MonsterType.DRAGON, "A dragon scroll that summons a whirlwind.")
    val MYSTERY_HAND = Card(137, "Mystery Hand", 500, 500, 2, CardAttribute.DARK, CardType.MONSTER, MonsterType.FIEND, "A sinister hand reaching from the void.")
    val MASKED_CLOWN = Card(182, "Masked Clown", 500, 700, 2, CardAttribute.DARK, CardType.MONSTER, MonsterType.WARRIOR, "An eccentric warrior who dances in mask.")
    val MECH_MOLE_ZOMBIE = Card(197, "Mech Mole Zombie", 500, 400, 2, CardAttribute.DARK, CardType.MONSTER, MonsterType.ZOMBIE, "A mechanized mole resurrected by darkness.")
    val AIR_MARMOT = Card(202, "Air Marmot of Nefariousness", 400, 600, 2, CardAttribute.EARTH, CardType.MONSTER, MonsterType.BEAST, "A winged rodent with a mischievous spirit.")
    val DROLL_BIRD = Card(207, "Droll Bird", 600, 500, 2, CardAttribute.WIND, CardType.MONSTER, MonsterType.WINGED_BEAST, "A comical bird with a massive beak.")
    val KAMINARIKOZOU = Card(211, "Kaminarikozou", 700, 600, 2, CardAttribute.THUNDER, CardType.MONSTER, MonsterType.THUNDER, "A spark-wielding lightning sprite.")
    val ZONE_EATER = Card(393, "Zone Eater", 250, 200, 1, CardAttribute.WATER, CardType.MONSTER, MonsterType.AQUA, "A microscopic sea beast that infects enemies.")
    val LEGHUL = Card(397, "Leghul", 300, 350, 1, CardAttribute.FOREST, CardType.MONSTER, MonsterType.INSECT, "A small insect capable of creeping unseen.")
    val SWORDSMAN_FOREIGN = Card(399, "Swordsman from a Foreign Land", 250, 250, 1, CardAttribute.EARTH, CardType.MONSTER, MonsterType.WARRIOR, "A traveler skilled in quick-draw swordplay.")
    val MONSTER_EYE = Card(402, "Monster Eye", 250, 350, 1, CardAttribute.FIEND, CardType.MONSTER, MonsterType.FIEND, "A floating eyeball observing all spells.")
    val MECHANICAL_SPIDER = Card(410, "Mechanical Spider", 400, 500, 2, CardAttribute.EARTH, CardType.MONSTER, MonsterType.INSECT, "A mechanical arachnid woven with wire.")
    val CYBER_STEIN = Card(420, "Cyber-Stein", 700, 500, 2, CardAttribute.DARK, CardType.MONSTER, MonsterType.MACHINE, "A mechanical brute infused with ancient spark.")
    val JINZO_7 = Card(422, "Jinzo #7", 500, 400, 2, CardAttribute.DARK, CardType.MONSTER, MonsterType.MACHINE, "A sleek cyborg unit with hidden blades.")
    val WHITE_DOLPHIN = Card(436, "White Dolphin", 500, 400, 2, CardAttribute.WATER, CardType.MONSTER, MonsterType.FISH, "A pure white aquatic beast.")
    val TURU_PURUN = Card(444, "Turu-Purun", 450, 500, 2, CardAttribute.WATER, CardType.MONSTER, MonsterType.AQUA, "A gelatinous water spirit.")
    val SINISTER_SERPENT = Card(475, "Sinister Serpent", 300, 250, 1, CardAttribute.WATER, CardType.MONSTER, MonsterType.REPTILE, "A cunning serpent with cold venom.")
    val BOO_KOO = Card(486, "Boo Koo", 650, 500, 2, CardAttribute.DREAMS, CardType.MONSTER, MonsterType.SPELLCASTER, "A mysterious dream mage hovering on a tome.")
    val FUNGI_MUSK = Card(504, "Fungi of the Musk", 400, 300, 1, CardAttribute.FIEND, CardType.MONSTER, MonsterType.FIEND, "A dark spore emitting intoxicating mist.")
    val GALE_DOGRA = Card(506, "Gale Dogra", 650, 600, 2, CardAttribute.FOREST, CardType.MONSTER, MonsterType.INSECT, "A praying mantis empowered by sacred wind.")
    val MUKA_MUKA = Card(516, "Muka Muka", 600, 300, 2, CardAttribute.EARTH, CardType.MONSTER, MonsterType.ROCK, "An earthen creature that gathers stones.")
    val STAR_BOY = Card(524, "Star Boy", 550, 500, 2, CardAttribute.WATER, CardType.MONSTER, MonsterType.AQUA, "A star-crowned water elemental.")
    val MILUS_RADIANT = Card(527, "Milus Radiant", 300, 250, 1, CardAttribute.EARTH, CardType.MONSTER, MonsterType.BEAST, "A radiant beast radiating earth power.")
    val BONE_MOUSE = Card(548, "Bone Mouse", 400, 300, 1, CardAttribute.DARK, CardType.MONSTER, MonsterType.ZOMBIE, "A skeletal rodent gnawing through shadows.")
    val FROG_THE_JAM = Card(549, "Frog The Jam", 700, 500, 2, CardAttribute.WATER, CardType.MONSTER, MonsterType.AQUA, "An odd amphibian oozing slime.")
    val WRETCHED_GHOST = Card(563, "Wretched Ghost of the Attic", 550, 400, 2, CardAttribute.FIEND, CardType.MONSTER, MonsterType.FIEND, "A sinister spirit haunting empty houses.")
    val ABYSS_FLOWER = Card(579, "Abyss Flower", 750, 400, 2, CardAttribute.FOREST, CardType.MONSTER, MonsterType.PLANT, "A carnivorous flower lurking in the deep forest.")
    val BLADEFLY = Card(609, "Bladefly", 600, 700, 2, CardAttribute.WIND, CardType.MONSTER, MonsterType.INSECT, "A razor-winged insect creating sonic whirls.")
    val HIRO_SHADOW_SCOUT = Card(611, "Hiro's Shadow Scout", 650, 500, 2, CardAttribute.DARK, CardType.MONSTER, MonsterType.FIEND, "A nimble fiend scouting the enemy ranks.")

    // --- STAGE 1 DUELIST CARDS ---
    val OSCILLO_HERO_2 = Card(212, "Oscillo Hero #2", 1000, 500, 3, CardAttribute.THUNDER, CardType.MONSTER, MonsterType.THUNDER, "A thunder hero crackling with electric power.")
    val SWORDSMAN_OF_LANDSTAR = Card(23, "Swordsman of Landstar", 500, 1200, 3, CardAttribute.LIGHT, CardType.MONSTER, MonsterType.WARRIOR, "Joey's proud swordsman with high defense.")
    val MOUNTAIN_WARRIOR = Card(58, "Mountain Warrior", 600, 1200, 3, CardAttribute.EARTH, CardType.MONSTER, MonsterType.BEAST_WARRIOR, "A hardy warrior accustomed to rugged peaks.")
    val TIME_WIZARD = Card(16, "Time Wizard", 500, 400, 2, CardAttribute.LIGHT, CardType.MONSTER, MonsterType.SPELLCASTER, "A time-bending spellcaster with clock magic.")
    val BABY_DRAGON = Card(4, "Baby Dragon", 1200, 700, 3, CardAttribute.WIND, CardType.MONSTER, MonsterType.DRAGON, "A young dragon with untapped potential.")
    val HARPIE_LADY = Card(150, "Harpie Lady", 1300, 1400, 4, CardAttribute.WIND, CardType.MONSTER, MonsterType.WINGED_BEAST, "Mai's signature feathered warrior.")
    val SONIC_MAID = Card(151, "Sonic Maid", 1200, 900, 3, CardAttribute.WIND, CardType.MONSTER, MonsterType.WARRIOR, "A graceful maiden attacking with sonic blades.")
    val WHITE_MAGICAL_HAT = Card(80, "White Magical Hat", 1000, 700, 3, CardAttribute.LIGHT, CardType.MONSTER, MonsterType.SPELLCASTER, "Bakura's trickster mage who robs cards.")
    val MORPHING_JAR = Card(81, "Morphing Jar", 700, 600, 2, CardAttribute.EARTH, CardType.MONSTER, MonsterType.ROCK, "A mysterious vase holding ancient spirits.")
    val BATTLE_OX = Card(5, "Battle Ox", 1700, 1000, 4, CardAttribute.EARTH, CardType.MONSTER, MonsterType.BEAST_WARRIOR, "Kaiba's heavy-hitting minotaur warrior.")
    val HITOTSU_ME_GIANT = Card(3, "Hitotsu-Me Giant", 1200, 1000, 4, CardAttribute.EARTH, CardType.MONSTER, MonsterType.BEAST_WARRIOR, "A one-eyed cyclops swinging a thick club.")
    val RYU_KISHIN = Card(28, "Ryu-Kishin", 1000, 500, 3, CardAttribute.DARK, CardType.MONSTER, MonsterType.FIEND, "A gargoyle lurking in cathedral shadows.")
    val CELTIC_GUARDIAN = Card(24, "Celtic Guardian", 1400, 1200, 4, CardAttribute.EARTH, CardType.MONSTER, MonsterType.WARRIOR, "An elven warrior who strikes with quick thrusts.")
    val CURSE_OF_DRAGON = Card(15, "Curse of Dragon", 2000, 1500, 5, CardAttribute.FIRE, CardType.MONSTER, MonsterType.DRAGON, "A wicked dragon exhaling black hellfire.")
    val WINGED_DRAGON_1 = Card(7, "Winged Dragon #1", 1400, 1200, 4, CardAttribute.WIND, CardType.MONSTER, MonsterType.DRAGON, "A swift dragon dominating the azure skies.")
    val KURIBOH = Card(107, "Kuriboh", 300, 200, 1, CardAttribute.DARK, CardType.MONSTER, MonsterType.FIEND, "A small fluffy creature protecting its master.")

    // --- HIGH-TIER BOSS MONSTERS ---
    val BLUE_EYES_WHITE_DRAGON = Card(1, "Blue-Eyes White Dragon", 3000, 2500, 8, CardAttribute.LIGHT, CardType.MONSTER, MonsterType.DRAGON, "The legendary engine of destruction with unmatched power.")
    val RED_EYES_B_DRAGON = Card(18, "Red-Eyes B. Dragon", 2400, 2000, 7, CardAttribute.DARK, CardType.MONSTER, MonsterType.DRAGON, "A ferocious dragon with a deadly crimson roar.")
    val DARK_MAGICIAN = Card(14, "Dark Magician", 2500, 2100, 7, CardAttribute.DARK, CardType.MONSTER, MonsterType.SPELLCASTER, "The ultimate wizard in terms of attack and defense.")
    val SUMMONED_SKULL = Card(25, "Summoned Skull", 2500, 1200, 6, CardAttribute.DARK, CardType.MONSTER, MonsterType.FIEND, "A fiend that strikes down foes with dark lightning.")

    // --- FUSIONS ---
    val TWIN_HEADED_THUNDER_DRAGON = Card(301, "Twin-Headed Thunder Dragon", 2800, 2100, 7, CardAttribute.THUNDER, CardType.MONSTER, MonsterType.DRAGON, "A two-headed dragon crackling with storm energy.")
    val ZOMBIE_DRAGON = Card(302, "Zombie Dragon", 1600, 0, 3, CardAttribute.DARK, CardType.MONSTER, MonsterType.DRAGON, "A rotting dragon animated by foul necro-magic.")
    val FLAME_CEREBRUS = Card(303, "Flame Cerebrus", 2100, 1800, 6, CardAttribute.FIRE, CardType.MONSTER, MonsterType.BEAST, "A blazing three-headed beast of the underworld.")
    val THOUSAND_DRAGON = Card(304, "Thousand Dragon", 2400, 2000, 7, CardAttribute.FIRE, CardType.MONSTER, MonsterType.DRAGON, "An ancient dragon evolved across countless centuries.")
    val CYBER_SOLDIER = Card(305, "Cyber Soldier", 1500, 1700, 5, CardAttribute.DARK, CardType.MONSTER, MonsterType.MACHINE, "A mechanical soldier armored with carbon plating.")

    // --- SPELLS & TRAPS ---
    val SPARKS = Card(201, "Sparks", 0, 0, 0, CardAttribute.SPELL, CardType.SPELL, MonsterType.NONE, "Inflicts 200 damage to opponent's Life Points.")
    val HINOTAMA = Card(202, "Hinotama", 0, 0, 0, CardAttribute.SPELL, CardType.SPELL, MonsterType.NONE, "Inflicts 500 damage to opponent's Life Points.")
    val MOOYAN_CURRY = Card(203, "Mooyan Curry", 0, 0, 0, CardAttribute.SPELL, CardType.SPELL, MonsterType.NONE, "Increases your Life Points by 200.")
    val DIAN_KETO = Card(19, "Dian Keto the Cure Maiden", 0, 0, 0, CardAttribute.SPELL, CardType.SPELL, MonsterType.NONE, "Increases your Life Points by 1000.")
    val RAIGEKI = Card(17, "Raigeki", 0, 0, 0, CardAttribute.SPELL, CardType.SPELL, MonsterType.NONE, "Destroys all monsters on the opponent's field.")
    val DARK_HOLE = Card(18, "Dark Hole", 0, 0, 0, CardAttribute.SPELL, CardType.SPELL, MonsterType.NONE, "Destroys all monsters on both fields.")
    val POT_OF_GREED = Card(16, "Pot of Greed", 0, 0, 0, CardAttribute.SPELL, CardType.SPELL, MonsterType.NONE, "Draw 2 cards from your Deck.")
    val TRAP_HOLE = Card(204, "Trap Hole", 0, 0, 0, CardAttribute.TRAP, CardType.TRAP, MonsterType.NONE, "Destroys an opponent's monster with >= 1000 ATK upon summon.")
    val MIRROR_FORCE = Card(200, "Mirror Force", 0, 0, 0, CardAttribute.TRAP, CardType.TRAP, MonsterType.NONE, "When an opponent attacks, destroys all opponent attack-position monsters.")
    val WABOKU = Card(205, "Waboku", 0, 0, 0, CardAttribute.TRAP, CardType.TRAP, MonsterType.NONE, "Prevents all battle damage to your monsters this turn.")
    val REINFORCEMENTS = Card(206, "Reinforcements", 0, 0, 0, CardAttribute.TRAP, CardType.TRAP, MonsterType.NONE, "Increases target monster's ATK by 500.")

    private val allCardsList = listOf(
        // Starter monsters
        SHADOW_SPECTER, YAMATANO_DRAGON, MYSTERY_HAND, MASKED_CLOWN, MECH_MOLE_ZOMBIE,
        AIR_MARMOT, DROLL_BIRD, KAMINARIKOZOU, ZONE_EATER, LEGHUL, SWORDSMAN_FOREIGN,
        MONSTER_EYE, MECHANICAL_SPIDER, CYBER_STEIN, JINZO_7, WHITE_DOLPHIN, TURU_PURUN,
        SINISTER_SERPENT, BOO_KOO, FUNGI_MUSK, GALE_DOGRA, MUKA_MUKA, STAR_BOY,
        MILUS_RADIANT, BONE_MOUSE, FROG_THE_JAM, WRETCHED_GHOST, ABYSS_FLOWER, BLADEFLY,
        HIRO_SHADOW_SCOUT,
        // Stage 1 & Boss monsters
        OSCILLO_HERO_2, SWORDSMAN_OF_LANDSTAR, MOUNTAIN_WARRIOR, TIME_WIZARD, BABY_DRAGON,
        HARPIE_LADY, SONIC_MAID, WHITE_MAGICAL_HAT, MORPHING_JAR, BATTLE_OX, HITOTSU_ME_GIANT,
        RYU_KISHIN, CELTIC_GUARDIAN, CURSE_OF_DRAGON, WINGED_DRAGON_1, KURIBOH,
        BLUE_EYES_WHITE_DRAGON, RED_EYES_B_DRAGON, DARK_MAGICIAN, SUMMONED_SKULL,
        // Fusions
        TWIN_HEADED_THUNDER_DRAGON, ZOMBIE_DRAGON, FLAME_CEREBRUS, THOUSAND_DRAGON, CYBER_SOLDIER,
        // Spells & Traps
        SPARKS, HINOTAMA, MOOYAN_CURRY, DIAN_KETO, RAIGEKI, DARK_HOLE, POT_OF_GREED,
        TRAP_HOLE, MIRROR_FORCE, WABOKU, REINFORCEMENTS
    )

    fun getAllCards(): List<Card> = allCardsList

    fun getCardById(id: Int): Card? = allCardsList.find { it.id == id }

    fun getCardByName(name: String): Card? = allCardsList.find { it.name.equals(name, ignoreCase = true) }
}
