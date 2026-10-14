package com.yugioh.recomp.core.rom

/**
 * Modular decoder for Game Boy Advance 4bpp and 8bpp graphics.
 * Prepares the engine architecture for GBA expansion ROMs:
 * Yu-Gi-Oh! The Eternal Duelist Soul (USA) & Worldwide Edition (USA).
 */
object GbaTileDecoder {

    val DEFAULT_GBA_PALETTE = IntArray(16) { i ->
        val v = (i * 17).coerceIn(0, 255)
        (0xFF shl 24) or (v shl 16) or (v shl 8) or v
    }

    /**
     * Decodes a single 8x8 4bpp GBA tile (32 bytes) into a 64-element ARGB array.
     */
    fun decodeTile4bpp(
        rom: ByteArray,
        offset: Int,
        palette: IntArray = DEFAULT_GBA_PALETTE
    ): IntArray {
        val pixels = IntArray(64)
        if (offset + 32 > rom.size) return pixels

        var byteIdx = 0
        for (row in 0 until 8) {
            for (col in 0 until 8 step 2) {
                val b = rom[offset + byteIdx].toInt() and 0xFF
                byteIdx++
                val p0 = b and 0x0F
                val p1 = (b shr 4) and 0x0F
                pixels[row * 8 + col] = palette.getOrElse(p0) { 0xFF000000.toInt() }
                pixels[row * 8 + col + 1] = palette.getOrElse(p1) { 0xFF000000.toInt() }
            }
        }
        return pixels
    }

    /**
     * Decodes a single 8x8 8bpp GBA tile (64 bytes) into a 64-element ARGB array.
     */
    fun decodeTile8bpp(
        rom: ByteArray,
        offset: Int,
        palette: IntArray
    ): IntArray {
        val pixels = IntArray(64)
        if (offset + 64 > rom.size) return pixels

        for (i in 0 until 64) {
            val colorIndex = rom[offset + i].toInt() and 0xFF
            pixels[i] = palette.getOrElse(colorIndex) { 0xFF000000.toInt() }
        }
        return pixels
    }

    /**
     * Converts a GBA 16-color palette (32 bytes little-endian 15-bit BGR words) to ARGB.
     */
    fun decodeGbaPalette16(rom: ByteArray, offset: Int): IntArray {
        val palette = IntArray(16)
        if (offset + 32 > rom.size) return palette

        for (i in 0 until 16) {
            val low = rom[offset + (i * 2)].toInt() and 0xFF
            val high = rom[offset + (i * 2) + 1].toInt() and 0xFF
            val word = (high shl 8) or low
            val r = (word and 0x1F) * 255 / 31
            val g = ((word shr 5) and 0x1F) * 255 / 31
            val b = ((word shr 10) and 0x1F) * 255 / 31
            palette[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        return palette
    }
}
