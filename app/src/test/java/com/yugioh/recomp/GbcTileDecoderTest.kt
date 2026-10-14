package com.yugioh.recomp

import com.yugioh.recomp.core.rom.GbcTileDecoder
import org.junit.Assert.*
import org.junit.Test

class GbcTileDecoderTest {

    @Test
    fun testGbcColorToArgb() {
        // Pure White: 15-bit 0x7FFF
        val whiteArgb = GbcTileDecoder.gbcColorToArgb(0x7FFF)
        assertEquals(0xFFFFFFFF.toInt(), whiteArgb)

        // Pure Black: 15-bit 0x0000
        val blackArgb = GbcTileDecoder.gbcColorToArgb(0x0000)
        assertEquals(0xFF000000.toInt(), blackArgb)
    }

    @Test
    fun testDecodeTile8x8() {
        // 16 bytes representing an 8x8 tile
        val tileData = ByteArray(16)
        // Set first row: low byte = 0xFF, high byte = 0x00 -> color index 1 across all 8 pixels
        tileData[0] = 0xFF.toByte()
        tileData[1] = 0x00.toByte()

        val pixels = GbcTileDecoder.decodeTile8x8(tileData, 0)
        assertEquals(64, pixels.size)

        for (col in 0 until 8) {
            assertEquals(GbcTileDecoder.DEFAULT_GBC_PALETTE[1], pixels[col])
        }
    }

    @Test
    fun testProceduralCardArtGeneration() {
        val pixels = GbcTileDecoder.generateProceduralCardArt(seed = 100L, width = 32, height = 32)
        assertEquals(1024, pixels.size)
        // Verify non-empty
        assertTrue(pixels.any { it != 0 })
    }
}
