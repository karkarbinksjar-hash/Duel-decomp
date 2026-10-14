package com.yugioh.recomp.core.engine

/**
 * Registry of canonical 8-digit Dark Duel Stories (GBC) card passwords.
 * Allows looking up, unlocking, and constructing decks with specific DDS cards.
 */
object CardPasscodeRegistry {
    private val passcodes = mapOf(
        "89631139" to "Blue-Eyes White Dragon",
        "46986414" to "Dark Magician",
        "74677422" to "Red-Eyes B. Dragon",
        "33396948" to "Celtic Guardian",
        "26202165" to "Curse of Dragon",
        "15341821" to "Gaia The Fierce Knight",
        "70781052" to "Summoned Skull",
        "06368038" to "Dark Hole",
        "12580477" to "Raigeki",
        "55144522" to "Pot of Greed",
        "44095762" to "Mirror Force",
        "21011044" to "Trap Hole",
        "38033121" to "Reinforcements",
        "07902349" to "Left Arm of the Forbidden One",
        "70903634" to "Right Arm of the Forbidden One",
        "44519536" to "Left Leg of the Forbidden One",
        "08124921" to "Right Leg of the Forbidden One"
    )

    fun findCardNameByPasscode(code: String): String? = passcodes[code.trim()]
    fun isValidPasscode(code: String): Boolean = passcodes.containsKey(code.trim())
    val allPasscodes: Map<String, String> get() = passcodes
}
