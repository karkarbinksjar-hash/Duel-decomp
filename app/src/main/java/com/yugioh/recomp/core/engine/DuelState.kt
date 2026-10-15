package com.yugioh.recomp.core.engine

import java.util.concurrent.atomic.AtomicLong

enum class CardPosition(
    val isDefense: Boolean,
    val isFaceDown: Boolean,
    val displayName: String
) {
    FACE_UP_ATTACK(false, false, "Attack Position"),
    FACE_UP_DEFENSE(true, false, "Face-Up Defense Position"),
    FACE_DOWN_DEFENSE(true, true, "Face-Down Defense Position"),
    SET_SPELL_TRAP(false, true, "Set"),
    FACE_UP_SPELL_TRAP(false, false, "Face-Up")
}

data class DuelCard(
    val instanceId: Long = ID_GEN.incrementAndGet(),
    val card: Card,
    var position: CardPosition = CardPosition.FACE_UP_ATTACK,
    var hasAttackedThisTurn: Boolean = false,
    var canChangePosition: Boolean = true,
    var turnsOnField: Int = 0
) {
    companion object {
        private val ID_GEN = AtomicLong(1000)
    }
}

enum class DuelPhase(val displayName: String, val shortName: String) {
    DRAW_PHASE("Draw Phase", "DP"),
    STANDBY_PHASE("Standby Phase", "SP"),
    MAIN_PHASE_1("Main Phase 1", "M1"),
    BATTLE_PHASE("Battle Phase", "BP"),
    MAIN_PHASE_2("Main Phase 2", "M2"),
    END_PHASE("End Phase", "EP")
}

data class ClashState(
    val attackerCard: Card,
    val defenderCard: Card?,
    val isDirectAttack: Boolean,
    val damageDealt: Int,
    val damageToPlayer: Boolean,
    val resultSummary: String
)

sealed class AiTurnStep {
    data class PhaseTransition(
        val phase: DuelPhase,
        val announcement: String
    ) : AiTurnStep()

    data class Draw(
        val card: Card?,
        val announcement: String
    ) : AiTurnStep()

    data class ActivateSpell(
        val card: Card,
        val announcement: String
    ) : AiTurnStep()

    data class SetTrap(
        val card: Card,
        val slotIndex: Int,
        val announcement: String
    ) : AiTurnStep()

    data class SummonMonster(
        val card: Card,
        val slotIndex: Int,
        val isDefense: Boolean,
        val announcement: String
    ) : AiTurnStep()

    data class DeclareAttack(
        val attackerSlot: Int,
        val attackerCard: Card,
        val defenderSlot: Int?, // null = direct attack
        val defenderCard: Card?,
        val predictedDamage: Int,
        val isDirect: Boolean,
        val announcement: String
    ) : AiTurnStep()

    data class EndTurn(
        val announcement: String
    ) : AiTurnStep()
}

class Duelist(
    val name: String,
    val isHuman: Boolean,
    var lifePoints: Int = 8000,
    val deck: MutableList<Card> = mutableListOf(),
    val hand: MutableList<DuelCard> = mutableListOf(),
    val graveyard: MutableList<Card> = mutableListOf(),
    val monsterZones: Array<DuelCard?> = arrayOfNulls(5),
    val spellTrapZones: Array<DuelCard?> = arrayOfNulls(5)
) {
    fun activeMonstersCount(): Int = monsterZones.count { it != null }
    fun activeSpellTrapsCount(): Int = spellTrapZones.count { it != null }
    fun firstEmptyMonsterSlot(): Int = monsterZones.indexOfFirst { it == null }
    fun firstEmptySpellSlot(): Int = spellTrapZones.indexOfFirst { it == null }

    fun resetTurnActions() {
        for (monster in monsterZones) {
            if (monster != null) {
                monster.hasAttackedThisTurn = false
                monster.canChangePosition = true
                monster.turnsOnField++
            }
        }
    }
}

sealed class AiTurnStep {
    abstract val announcement: String

    data class PhaseTransition(
        val phase: DuelPhase,
        override val announcement: String
    ) : AiTurnStep()

    data class Draw(
        val card: Card?,
        override val announcement: String
    ) : AiTurnStep()

    data class ActivateSpell(
        val card: Card,
        override val announcement: String
    ) : AiTurnStep()

    data class SetTrap(
        val card: Card,
        val slotIndex: Int,
        override val announcement: String
    ) : AiTurnStep()

    data class SummonMonster(
        val card: Card,
        val slotIndex: Int,
        val isDefense: Boolean,
        override val announcement: String
    ) : AiTurnStep()

    data class DeclareAttack(
        val attackerSlot: Int,
        val defenderSlot: Int?,
        val attackerCard: Card,
        val defenderCard: Card? = null,
        val isDirect: Boolean,
        val predictedDamage: Int,
        override val announcement: String
    ) : AiTurnStep()

    data class EndTurn(
        override val announcement: String
    ) : AiTurnStep()
}
