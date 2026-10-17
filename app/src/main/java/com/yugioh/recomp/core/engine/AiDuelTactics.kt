package com.yugioh.recomp.core.engine

/**
 * Character-specific AI personalities, strategic behaviors, and dynamic combat dialogue.
 * Implements distinct duelist tactics matching anime and video game lore (EDS / DDS).
 */
enum class AiArchetype {
    DRAGON_POWER,       // Seto Kaiba: Aggressive beatdown, high ATK boss rushes, Dragon tributes
    TACTICAL_CONTROL,   // Yami Yugi: Balanced defense, strategic traps, Monster Reborn timing
    GAMBLER_BEATDOWN,   // Joey Wheeler: All-out attack, high risk warrior gambles
    PSYCHIC_LOCK,       // Espa Roba: Trap negations, Jinzo priority
    MIND_CONTROL,       // Maximillion Pegasus: Change of Heart, Relinquished, psychological play
    AGGRESSIVE_BEATDOWN,// Bandit Keith / Rex Raptor: Raw machine & dinosaur offensive
    BALANCED_ROOKIE     // Tristan Taylor / Tea Gardner: Standard fundamental gameplay
}

data class CharacterTacticsProfile(
    val name: String,
    val archetype: AiArchetype,
    val signatureBossCard: String,
    val openingQuote: String,
    val bossSummonQuote: String,
    val spellTauntQuote: String,
    val trapTriggerQuote: String,
    val attackQuote: String
)

object AiDuelTactics {

    private val profiles = mapOf(
        "Seto Kaiba" to CharacterTacticsProfile(
            name = "Seto Kaiba",
            archetype = AiArchetype.DRAGON_POWER,
            signatureBossCard = "Blue-Eyes White Dragon",
            openingQuote = "You're a third-rate duelist with a fourth-rate deck!",
            bossSummonQuote = "Burst Stream of Destruction! Behold the ultimate creature, Blue-Eyes White Dragon!",
            spellTauntQuote = "A true duelist crushes opposition with overwhelming force!",
            trapTriggerQuote = "Fool! Did you really think you could outplay KaibaCorp technology?",
            attackQuote = "Eliminate that pathetic obstacle! Attack!"
        ),
        "Yami Yugi" to CharacterTacticsProfile(
            name = "Yami Yugi",
            archetype = AiArchetype.TACTICAL_CONTROL,
            signatureBossCard = "Dark Magician",
            openingQuote = "It's time to duel! I believe in the Heart of the Cards!",
            bossSummonQuote = "Dark Magic Attack! The ultimate wizard in terms of attack and defense, Dark Magician!",
            spellTauntQuote = "A spell is only as powerful as the duelist's faith in it!",
            trapTriggerQuote = "You walked straight into my trap! The shadows reveal your fate!",
            attackQuote = "Trust in your destiny! Direct your strike!"
        ),
        "Joey Wheeler" to CharacterTacticsProfile(
            name = "Joey Wheeler",
            archetype = AiArchetype.GAMBLER_BEATDOWN,
            signatureBossCard = "Red-Eyes B. Dragon",
            openingQuote = "I'm Joey Wheeler, and my Brooklyn grit is gonna clobber ya!",
            bossSummonQuote = "Here comes my ace! Red-Eyes B. Dragon, show 'em Inferno Fire Blast!",
            spellTauntQuote = "Hey, check this card out! Betcha didn't see that comin'!",
            trapTriggerQuote = "Aha! Fell for my trick, sucker!",
            attackQuote = "All right, let's smash 'em up! Go get 'em!"
        ),
        "Espa Roba" to CharacterTacticsProfile(
            name = "Espa Roba",
            archetype = AiArchetype.PSYCHIC_LOCK,
            signatureBossCard = "Jinzo",
            openingQuote = "My psychic receptors have already read every card in your deck!",
            bossSummonQuote = "Behold the machine that crushes all psychic resistance: Jinzo!",
            spellTauntQuote = "My ESP predicted your every move!",
            trapTriggerQuote = "Your foolish tricks are useless against my telepathy!",
            attackQuote = "Cyber Energy Shock! Strike!"
        ),
        "Maximillion Pegasus" to CharacterTacticsProfile(
            name = "Maximillion Pegasus",
            archetype = AiArchetype.MIND_CONTROL,
            signatureBossCard = "Relinquished",
            openingQuote = "Welcome, my dear duelist! My Millennium Eye sees right into your soul!",
            bossSummonQuote = "Open your eyes to the darkness of Relinquished!",
            spellTauntQuote = "Oh my, what a lovely card! Let me borrow it with Change of Heart!",
            trapTriggerQuote = "Ooh, so close! But you cannot deceive my Millennium Eye!",
            attackQuote = "Time for a little cartoon magic! Attack!"
        ),
        "Bandit Keith" to CharacterTacticsProfile(
            name = "Bandit Keith",
            archetype = AiArchetype.AGGRESSIVE_BEATDOWN,
            signatureBossCard = "Barrel Dragon",
            openingQuote = "You're in America now! Let's see if you can handle real heavy metal!",
            bossSummonQuote = "Lock and load! Barrel Dragon, spin those chambers and fire!",
            spellTauntQuote = "Eat lead and smoke!",
            trapTriggerQuote = "Heh, you just stepped right into my ambush!",
            attackQuote = "Blast 'em to pieces!"
        ),
        "Mai Valentine" to CharacterTacticsProfile(
            name = "Mai Valentine",
            archetype = AiArchetype.AGGRESSIVE_BEATDOWN,
            signatureBossCard = "Harpie Lady",
            openingQuote = "A rookie like you won't survive the talons of my Harpie Lady!",
            bossSummonQuote = "Spread your wings! Harpie Lady, take flight!",
            spellTauntQuote = "Beauty and power combined in one deadly card!",
            trapTriggerQuote = "Did you really think a woman wouldn't see right through that?",
            attackQuote = "Slice through their defenses!"
        )
    )

    private val defaultRookieProfile = CharacterTacticsProfile(
        name = "Duelist",
        archetype = AiArchetype.BALANCED_ROOKIE,
        signatureBossCard = "Battle Ox",
        openingQuote = "Let's have an honorable duel!",
        bossSummonQuote = "I summon my strongest monster to the field!",
        spellTauntQuote = "I activate this Spell card!",
        trapTriggerQuote = "You triggered my trap card!",
        attackQuote = "Monster, attack!"
    )

    fun getProfile(opponentName: String): CharacterTacticsProfile =
        profiles[opponentName] ?: defaultRookieProfile

    /**
     * Determines whether the AI should activate a given Spell card based on tactical character archetype.
     */
    fun shouldActivateSpell(
        opponentName: String,
        spellCard: Card,
        aiMonsters: List<DuelCard?>,
        playerMonsters: List<DuelCard?>,
        aiLp: Int,
        playerLp: Int
    ): Boolean {
        val profile = getProfile(opponentName)
        val activePlayerMonsters = playerMonsters.filterNotNull()
        val activeAiMonsters = aiMonsters.filterNotNull()

        return when (spellCard.name) {
            "Raigeki", "Dark Hole" -> {
                when (profile.archetype) {
                    AiArchetype.DRAGON_POWER -> activePlayerMonsters.isNotEmpty() // Kaiba clears unconditionally
                    AiArchetype.TACTICAL_CONTROL -> activePlayerMonsters.size >= 2 || activePlayerMonsters.any { it.card.atk > (activeAiMonsters.maxOfOrNull { m -> m.card.atk } ?: 0) }
                    else -> activePlayerMonsters.size >= 2 || (activePlayerMonsters.isNotEmpty() && activeAiMonsters.isEmpty())
                }
            }
            "Swords of Revealing Light" -> {
                // Tactical duelists activate when facing threat; aggressive duelists activate immediately
                when (profile.archetype) {
                    AiArchetype.TACTICAL_CONTROL -> aiLp < 4000 || activePlayerMonsters.size > activeAiMonsters.size
                    else -> true
                }
            }
            "Monster Reborn" -> {
                // Always useful if any monster is in graveyard
                true
            }
            "Change of Heart", "Snatch Steal" -> {
                // Best used when opponent has at least 1 monster
                activePlayerMonsters.isNotEmpty()
            }
            "Pot of Greed", "Graceful Charity" -> {
                // Free card advantage is always optimal
                true
            }
            "Dian Keto the Cure Maiden" -> {
                aiLp < 7500
            }
            "Sparks", "Hinotama" -> {
                true
            }
            else -> true
        }
    }

    /**
     * Returns a dramatic announcement quote when an AI opponent activates a signature card.
     */
    fun getBossSummonDialogue(opponentName: String, cardName: String): String? {
        val profile = getProfile(opponentName)
        return if (cardName.equals(profile.signatureBossCard, ignoreCase = true)) {
            profile.bossSummonQuote
        } else null
    }
}
