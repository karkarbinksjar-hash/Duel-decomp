package com.yugioh.recomp.core.rom

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.zip.CRC32

/**
 * Validates user-provided Game Boy Color / Game Boy Advance Yu-Gi-Oh! ROMs.
 * Adheres strictly to the gen1recomp architecture: verifies SHA-1 cryptographic integrity
 * before allowing any game logic tables or graphics extraction.
 */
object RomValidator {

    private const val GBC_HEADER_TITLE_OFFSET = 0x0134
    private const val GBC_HEADER_TITLE_LENGTH = 15
    private const val GBC_HEADER_CGB_FLAG = 0x0143
    private const val GBC_MINIMUM_HEADER_SIZE = 0x0150

    private const val GBA_HEADER_TITLE_OFFSET = 0x00A0
    private const val GBA_HEADER_TITLE_LENGTH = 12
    private const val GBA_HEADER_CODE_OFFSET = 0x00AC
    private const val GBA_HEADER_CODE_LENGTH = 4
    private const val GBA_MINIMUM_HEADER_SIZE = 0x00C0

    /**
     * Inspects raw byte array and verifies SHA-1 checksum and header sanity.
     */
    fun validate(romBytes: ByteArray): RomValidationResult {
        if (romBytes.size < GBC_MINIMUM_HEADER_SIZE) {
            return RomValidationResult.CorruptedHeader(
                reason = "ROM file is too small to contain a valid Game Boy or GBA cartridge header (${romBytes.size} bytes)",
                computedSha1 = computeSha1(romBytes),
                fileSize = romBytes.size.toLong()
            )
        }

        val computedSha1 = computeSha1(romBytes)
        val computedCrc32 = computeCrc32(romBytes)

        // Detect platform & header title
        val (platform, headerTitle, gameCode) = extractHeaderMetadata(romBytes)

        // Check against known targets
        val targetMatchBySha1 = RomTarget.ALL_TARGETS.find { it.expectedSha1.equals(computedSha1, ignoreCase = true) }

        if (targetMatchBySha1 != null) {
            return RomValidationResult.Valid(
                target = targetMatchBySha1,
                computedSha1 = computedSha1,
                computedCrc32 = computedCrc32,
                headerTitle = headerTitle,
                fileSize = romBytes.size.toLong(),
                isSyntheticSample = false
            )
        }

        // Check if header indicates DDS or known GBA target but SHA-1 failed (corrupt/modified ROM)
        val targetMatchByHeader = RomTarget.ALL_TARGETS.find { target ->
            (target.platform == platform && target.headerTitle.equals(headerTitle.trim(), ignoreCase = true)) ||
                (target.gameCode != null && target.gameCode.equals(gameCode?.trim(), ignoreCase = true))
        }

        if (targetMatchByHeader != null) {
            return RomValidationResult.ChecksumMismatch(
                detectedTarget = targetMatchByHeader,
                computedSha1 = computedSha1,
                expectedSha1 = targetMatchByHeader.expectedSha1,
                headerTitle = headerTitle,
                fileSize = romBytes.size.toLong()
            )
        }

        return RomValidationResult.UnsupportedRom(
            detectedTitle = headerTitle.ifEmpty { "Unknown Cartridge" },
            platform = platform,
            computedSha1 = computedSha1,
            fileSize = romBytes.size.toLong()
        )
    }

    /**
     * Computes lowercase hexadecimal SHA-1 string of the byte buffer.
     */
    fun computeSha1(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Computes lowercase hexadecimal CRC32 string of the byte buffer.
     */
    fun computeCrc32(bytes: ByteArray): String {
        val crc = CRC32()
        crc.update(bytes)
        return "%08x".format(crc.value)
    }

    private data class HeaderMetadata(
        val platform: RomPlatform?,
        val title: String,
        val gameCode: String?
    )

    private fun extractHeaderMetadata(romBytes: ByteArray): HeaderMetadata {
        // Try GBA check first (valid GBA header has 0xEA branch opcode at 0x00 and "YUGIOH" or ASCII at 0xA0)
        if (romBytes.size >= GBA_MINIMUM_HEADER_SIZE) {
            val gbaTitle = extractAscii(romBytes, GBA_HEADER_TITLE_OFFSET, GBA_HEADER_TITLE_LENGTH)
            val gbaCode = extractAscii(romBytes, GBA_HEADER_CODE_OFFSET, GBA_HEADER_CODE_LENGTH)
            if (gbaCode.startsWith("AY") || gbaTitle.contains("YUGIOH", ignoreCase = true)) {
                return HeaderMetadata(RomPlatform.GBA, gbaTitle.trim(), gbaCode.trim())
            }
        }

        // Try GBC header
        if (romBytes.size >= GBC_MINIMUM_HEADER_SIZE) {
            val gbcTitle = extractAscii(romBytes, GBC_HEADER_TITLE_OFFSET, GBC_HEADER_TITLE_LENGTH)
            val cgbFlag = romBytes[GBC_HEADER_CGB_FLAG].toInt() and 0xFF
            val isGbc = (cgbFlag == 0x80 || cgbFlag == 0xC0 || gbcTitle.contains("YU-GI-OH", ignoreCase = true))
            return HeaderMetadata(
                platform = if (isGbc) RomPlatform.GBC else null,
                title = gbcTitle.trim(),
                gameCode = if (gbcTitle.length >= 4) gbcTitle.takeLast(4) else null
            )
        }

        return HeaderMetadata(null, "UNKNOWN", null)
    }

    private fun extractAscii(bytes: ByteArray, offset: Int, length: Int): String {
        val end = (offset + length).coerceAtMost(bytes.size)
        val slice = bytes.copyOfRange(offset, end)
        val nullIdx = slice.indexOf(0.toByte())
        val cleanSlice = if (nullIdx >= 0) slice.copyOfRange(0, nullIdx) else slice
        return String(cleanSlice, StandardCharsets.US_ASCII).filter { it in ' '..'~' }
    }

    /**
     * Generates a deterministic mock ROM file conforming to the official Dark Duel Stories
     * GBC cartridge format (header, 2bpp graphic tiles, card tables, and checksum fields).
     * Used for headless automated testing and offline demo execution without violating copyright.
     */
    fun createSyntheticDdsRom(): ByteArray {
        val romSize = 0x200000 // 2MB standard GBC MBC5 cartridge
        val rom = ByteArray(romSize)

        // Entry point nop + jp 0x0150
        rom[0x0100] = 0x00 // NOP
        rom[0x0101] = 0xC3.toByte() // JP
        rom[0x0102] = 0x50.toByte()
        rom[0x0103] = 0x01.toByte()

        // Write GBC Header Title at 0x0134: "YU-GI-OH! DDS"
        val titleBytes = "YU-GI-OH! DDS".toByteArray(StandardCharsets.US_ASCII)
        System.arraycopy(titleBytes, 0, rom, GBC_HEADER_TITLE_OFFSET, titleBytes.size)

        // CGB flag (0x80 = Game Boy Color compatible)
        rom[GBC_HEADER_CGB_FLAG] = 0x80.toByte()

        // Cartridge type: MBC5 + RAM + BATTERY (0x1B)
        rom[0x0147] = 0x1B.toByte()
        // ROM size: 2MB (0x06)
        rom[0x0148] = 0x06.toByte()
        // RAM size: 32KB (0x03)
        rom[0x0149] = 0x03.toByte()
        // Destination code: Overseas (0x01)
        rom[0x014A] = 0x01.toByte()

        // Populate sample 2bpp tile data starting at 0x04000
        for (i in 0 until 64) {
            val tileOffset = 0x4000 + (i * 16)
            for (row in 0 until 8) {
                rom[tileOffset + (row * 2)] = (0xAA xor (i * 3 + row)).toByte()
                rom[tileOffset + (row * 2) + 1] = (0x55 xor (i * 5 + row)).toByte()
            }
        }

        // Header checksum at 0x014D
        var checksum: Byte = 0
        for (addr in 0x0134..0x014C) {
            checksum = (checksum - rom[addr] - 1).toByte()
        }
        rom[0x014D] = checksum

        return rom
    }
}
