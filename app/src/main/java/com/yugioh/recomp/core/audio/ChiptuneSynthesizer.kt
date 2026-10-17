package com.yugioh.recomp.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.sin

/**
 * Recreates the Game Boy APU (Audio Processing Unit) 4-channel sound synthesizer:
 * Channel 1 & 2: Pulse (Square) wave with variable duty cycle & pitch envelope
 * Channel 3: Custom wave table
 * Channel 4: LFSR Pseudo-Random White Noise
 */
object ChiptuneSynthesizer {

    private const val SAMPLE_RATE = 22050

    /**
     * Synthesizes a pulse wave with duty cycle and volume envelope.
     */
    fun synthesizePulse(
        frequency: Double,
        durationMs: Int,
        dutyCycle: Double = 0.5,
        decay: Boolean = true
    ): ShortArray {
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        val period = SAMPLE_RATE / frequency

        for (i in 0 until numSamples) {
            val phase = (i % period) / period
            val value = if (phase < dutyCycle) 1.0 else -1.0
            val envelope = if (decay) 1.0 - (i.toDouble() / numSamples) else 1.0
            buffer[i] = (value * envelope * 16000.0).toInt().toShort()
        }
        return buffer
    }

    /**
     * Synthesizes Game Boy Channel 4 LFSR pseudo-random noise (clashes, impacts, card shuffle).
     */
    fun synthesizeNoise(durationMs: Int, decay: Boolean = true): ShortArray {
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        var lfsr = 0x7FFF

        for (i in 0 until numSamples) {
            // Game Boy 15-bit LFSR step
            val bit = (lfsr xor (lfsr shr 1)) and 1
            lfsr = (lfsr shr 1) or (bit shl 14)
            val sampleVal = if ((lfsr and 1) == 1) 1.0 else -1.0
            val envelope = if (decay) (1.0 - (i.toDouble() / numSamples)) else 1.0
            buffer[i] = (sampleVal * envelope * 14000.0).toInt().toShort()
        }
        return buffer
    }

    /**
     * Synthesizes an arpeggiated fanfare or sweep.
     */
    fun synthesizeArpeggio(notes: List<Double>, noteDurationMs: Int): ShortArray {
        val totalSamples = (SAMPLE_RATE * (notes.size * noteDurationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        var offset = 0

        for (freq in notes) {
            val noteSamples = synthesizePulse(freq, noteDurationMs, 0.5, decay = true)
            System.arraycopy(noteSamples, 0, buffer, offset, noteSamples.size.coerceAtMost(totalSamples - offset))
            offset += noteSamples.size
        }
        return buffer
    }

    /**
     * Card draw chiptune SFX
     */
    fun createCardDrawSfx(): ShortArray {
        return synthesizeArpeggio(listOf(523.25, 659.25, 783.99), 40)
    }

    /**
     * Monster Summon chiptune SFX (Ascending mystical sweep)
     */
    fun createSummonSfx(): ShortArray {
        return synthesizeArpeggio(listOf(440.0, 554.37, 659.25, 880.0, 1108.73, 1318.51), 40)
    }

    /**
     * Card Destruction burst chiptune SFX (Sharp noise crackle + descending pitch drop)
     */
    fun createCardDestructionSfx(): ShortArray {
        val noise = synthesizeNoise(180, decay = true)
        val pulse = synthesizePulse(120.0, 180, 0.5, decay = true)
        val combined = ShortArray(noise.size)
        for (i in combined.indices) {
            val sum = noise[i].toInt() + pulse[i].toInt()
            combined[i] = sum.coerceIn(-32768, 32767).toShort()
        }
        return combined
    }

    /**
     * Battle Attack Clash chiptune SFX (Pulse + Noise impact)
     */
    fun createAttackClashSfx(): ShortArray {
        val noise = synthesizeNoise(150, decay = true)
        val pulse = synthesizePulse(180.0, 150, 0.25, decay = true)
        val combined = ShortArray(noise.size)
        for (i in combined.indices) {
            val sum = noise[i].toInt() + pulse[i].toInt()
            combined[i] = sum.coerceIn(-32768, 32767).toShort()
        }
        return combined
    }

    /**
     * Direct Attack Blast chiptune SFX (Deep thunderous strike)
     */
    fun createDirectAttackSfx(): ShortArray {
        val noise = synthesizeNoise(280, decay = true)
        val pulse = synthesizePulse(90.0, 280, 0.4, decay = true)
        val combined = ShortArray(noise.size)
        for (i in combined.indices) {
            val sum = noise[i].toInt() + pulse[i].toInt()
            combined[i] = sum.coerceIn(-32768, 32767).toShort()
        }
        return combined
    }

    /**
     * Life Points damage tick SFX
     */
    fun createDamageTickSfx(): ShortArray {
        return synthesizePulse(220.0, 50, 0.125, decay = true)
    }

    /**
     * Victory Fanfare chiptune
     */
    fun createVictoryFanfare(): ShortArray {
        return synthesizeArpeggio(listOf(523.25, 523.25, 523.25, 659.25, 783.99, 1046.50), 100)
    }

    /**
     * Defeat Fanfare chiptune (Descending somber minor progression)
     */
    fun createDefeatFanfare(): ShortArray {
        return synthesizeArpeggio(listOf(440.0, 392.0, 349.23, 329.63, 293.66, 220.0), 130)
    }

    /**
     * Spell Activation shimmering chiptune SFX
     */
    fun createSpellActivationSfx(): ShortArray {
        return synthesizeArpeggio(listOf(329.63, 440.0, 554.37, 659.25, 880.0, 1108.73), 45)
    }

    /**
     * Card Flip snappy swoosh SFX
     */
    fun createCardFlipSfx(): ShortArray {
        val noise = synthesizeNoise(60, decay = true)
        val pulse = synthesizePulse(400.0, 60, 0.25, decay = true)
        val combined = ShortArray(noise.size)
        for (i in combined.indices) {
            combined[i] = ((noise[i].toInt() + pulse[i].toInt()) / 2).toShort()
        }
        return combined
    }

    /**
     * Safe playback on Android device/emulator. Fails gracefully in JVM unit tests.
     */
    fun playSfx(samples: ShortArray) {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track.play()
        } catch (_: Throwable) {
            // Ignore in headless test JVM
        }
    }
}
