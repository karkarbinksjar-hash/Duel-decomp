package com.yugioh.recomp.core.rom

/**
 * Decodes Game Boy Color 2bpp planar graphic tiles into 32-bit ARGB pixel buffers.
 * Game Boy planar encoding:
 * Each 8x8 tile is stored as 16 consecutive bytes (2 bytes per row of 8 horizontal pixels).
 */
object GbcTileDecoder {

    /** Default classic Game Boy Color card palette (Cream, Light Amber, Crimson, Black) */
    val DEFAULT_GBC_PALETTE = intArrayOf(
        0xFFF8F8F8.toInt(), // Index 0: Background / White
        0xFFE0B850.toInt(), // Index 1: Light Gold / Ochre
        0xFFA03020.toInt(), // Index 2: Crimson / Monster Hue
        0xFF181018.toInt()  // Index 3: Dark Border / Shadow
    )

    /**
     * Converts a GBC 15-bit BGR word (0b0BBBBBGGGGGRRRRR) to 32-bit ARGB.
     */
    fun gbcColorToArgb(gbcWord: Int): Int {
        val r = (gbcWord and 0x1F) * 255 / 31
        val g = ((gbcWord shr 5) and 0x1F) * 255 / 31
        val b = ((gbcWord shr 10) and 0x1F) * 255 / 31
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    /**
     * Decodes a single 8x8 2bpp tile starting at [offset] into a flat 64-element ARGB array.
     */
    fun decodeTile8x8(
        rom: ByteArray,
        offset: Int,
        palette: IntArray = DEFAULT_GBC_PALETTE
    ): IntArray {
        val pixels = IntArray(64)
        if (offset + 16 > rom.size) return pixels

        for (row in 0 until 8) {
            val byteLow = rom[offset + (row * 2)].toInt() and 0xFF
            val byteHigh = rom[offset + (row * 2) + 1].toInt() and 0xFF

            for (col in 0 until 8) {
                val shift = 7 - col
                val bit0 = (byteLow shr shift) and 1
                val bit1 = (byteHigh shr shift) and 1
                val colorIndex = (bit1 shl 1) or bit0
                val color = palette.getOrElse(colorIndex) { 0xFF000000.toInt() }
                pixels[row * 8 + col] = color
            }
        }
        return pixels
    }

    /**
     * Decodes a composite rectangular grid of [widthTiles] x [heightTiles] tiles
     * into a linear ARGB pixel array suitable for rendering.
     * Dimensions of result: (widthTiles * 8) x (heightTiles * 8).
     */
    fun decodeTileGrid(
        rom: ByteArray,
        startOffset: Int,
        widthTiles: Int,
        heightTiles: Int,
        palette: IntArray = DEFAULT_GBC_PALETTE
    ): DecodedSprite {
        val pixelWidth = widthTiles * 8
        val pixelHeight = heightTiles * 8
        val pixels = IntArray(pixelWidth * pixelHeight)
        var currentOffset = startOffset

        for (ty in 0 until heightTiles) {
            for (tx in 0 until widthTiles) {
                val tilePixels = decodeTile8x8(rom, currentOffset, palette)
                currentOffset += 16

                // Copy 8x8 into the destination canvas at (tx * 8, ty * 8)
                val destX = tx * 8
                val destY = ty * 8
                for (row in 0 until 8) {
                    val destRowStart = (destY + row) * pixelWidth + destX
                    System.arraycopy(tilePixels, row * 8, pixels, destRowStart, 8)
                }
            }
        }

        return DecodedSprite(
            width = pixelWidth,
            height = pixelHeight,
            pixels = pixels
        )
    }

    /**
     * Synthesizes a procedural card art portrait when raw ROM tile data is being indexed.
     */
    fun generateProceduralCardArt(seed: Long, width: Int = 48, height: Int = 48): IntArray {
        val pixels = IntArray(width * height)
        val rng = java.util.Random(seed)
        val baseColor = when ((seed % 6).toInt()) {
            0 -> 0xFF3B82F6.toInt() // Blue
            1 -> 0xFFEF4444.toInt() // Red
            2 -> 0xFF10B981.toInt() // Green
            3 -> 0xFFF59E0B.toInt() // Amber
            4 -> 0xFF8B5CF6.toInt() // Purple
            else -> 0xFFEC4899.toInt() // Pink
        }
        val darkColor = 0xFF1E293B.toInt()
        val accentColor = 0xFFFCD34D.toInt()

        for (y in 0 until height) {
            for (x in 0 until width) {
                // Symmetrical decorative pattern
                val sx = if (x < width / 2) x else width - 1 - x
                val dist = Math.hypot((x - width / 2.0), (y - height / 2.0))
                val ring = (dist.toInt() + rng.nextInt(3)) % 8

                val color = when {
                    x == 0 || y == 0 || x == width - 1 || y == height - 1 -> 0xFF0F172A.toInt()
                    dist < 6.0 -> accentColor
                    ring in 2..3 -> baseColor
                    (sx + y + (seed % 7).toInt()) % 4 == 0 -> darkColor
                    else -> 0xFF334155.toInt()
                }
                pixels[y * width + x] = color
            }
        }
        return pixels
    }
}

data class DecodedSprite(
    val width: Int,
    val height: Int,
    val pixels: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DecodedSprite
        return width == other.width && height == other.height && pixels.contentEquals(other.pixels)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + pixels.contentHashCode()
        return result
    }
}
