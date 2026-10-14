package com.yugioh.recomp

import com.yugioh.recomp.core.rom.RomPlatform
import com.yugioh.recomp.core.rom.RomTarget
import com.yugioh.recomp.core.rom.RomValidationResult
import com.yugioh.recomp.core.rom.RomValidator
import org.junit.Assert.*
import org.junit.Test
import java.nio.charset.StandardCharsets

class RomValidatorTest {

    @Test
    fun testSha1Calculation() {
        val input = "Yu-Gi-Oh! Dark Duel Stories".toByteArray(StandardCharsets.UTF_8)
        val hash = RomValidator.computeSha1(input)
        assertNotNull(hash)
        assertEquals(40, hash.length)
    }

    @Test
    fun testTooSmallBufferReturnsCorruptedHeader() {
        val tiny = ByteArray(32)
        val result = RomValidator.validate(tiny)
        assertTrue(result is RomValidationResult.CorruptedHeader)
    }

    @Test
    fun testSyntheticDdsRomValidation() {
        val rom = RomValidator.createSyntheticDdsRom()
        assertEquals(2097152, rom.size)

        val result = RomValidator.validate(rom)
        assertNotNull(result)
        // Checks that header title is detected as YU-GI-OH! DDS
        when (result) {
            is RomValidationResult.ChecksumMismatch -> {
                assertEquals("YU-GI-OH! DDS", result.headerTitle)
                assertEquals(RomTarget.DARK_DUEL_STORIES_USA.expectedSha1, result.expectedSha1)
            }
            is RomValidationResult.Valid -> {
                assertEquals(RomPlatform.GBC, result.target.platform)
            }
            is RomValidationResult.UnsupportedRom -> {
                assertTrue(result.detectedTitle.contains("YU-GI-OH"))
            }
            else -> fail("Unexpected result type: ${result::class.simpleName}")
        }
    }

    @Test
    fun testTargetCatalogDefinitions() {
        val ddsUsa = RomTarget.DARK_DUEL_STORIES_USA
        assertEquals("1952d343bdfb5625bd23a8569cde6fd61a911502", ddsUsa.expectedSha1)
        assertEquals(RomPlatform.GBC, ddsUsa.platform)

        val edsUsa = RomTarget.ETERNAL_DUELIST_SOUL_USA
        assertEquals("510fbba212aca9bab95ea12f8fd933e62ee34dea", edsUsa.expectedSha1)
        assertEquals(RomPlatform.GBA, edsUsa.platform)

        val wweUsa = RomTarget.WORLDWIDE_EDITION_USA
        assertEquals(RomPlatform.GBA, wweUsa.platform)
    }
}
