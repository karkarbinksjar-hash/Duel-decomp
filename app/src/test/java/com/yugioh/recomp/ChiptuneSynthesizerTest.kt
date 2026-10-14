package com.yugioh.recomp

import com.yugioh.recomp.core.audio.ChiptuneSynthesizer
import org.junit.Assert.*
import org.junit.Test

class ChiptuneSynthesizerTest {

    @Test
    fun testSynthesizePulse() {
        val samples = ChiptuneSynthesizer.synthesizePulse(frequency = 440.0, durationMs = 100)
        assertTrue(samples.isNotEmpty())
        assertTrue(samples.any { it > 0 })
        assertTrue(samples.any { it < 0 })
    }

    @Test
    fun testSynthesizeNoise() {
        val noise = ChiptuneSynthesizer.synthesizeNoise(durationMs = 50)
        assertTrue(noise.isNotEmpty())
        assertTrue(noise.any { it != 0.toShort() })
    }

    @Test
    fun testSfxWaveformCreation() {
        val drawSfx = ChiptuneSynthesizer.createCardDrawSfx()
        val summonSfx = ChiptuneSynthesizer.createSummonSfx()
        val attackSfx = ChiptuneSynthesizer.createAttackClashSfx()
        val victorySfx = ChiptuneSynthesizer.createVictoryFanfare()

        assertTrue(drawSfx.isNotEmpty())
        assertTrue(summonSfx.isNotEmpty())
        assertTrue(attackSfx.isNotEmpty())
        assertTrue(victorySfx.isNotEmpty())
    }
}
