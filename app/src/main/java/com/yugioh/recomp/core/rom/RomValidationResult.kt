package com.yugioh.recomp.core.rom

/**
 * Encapsulates the verification outcome when inspecting a user-provided ROM.
 * Follows the gen1recomp architectural constraint:
 * Strictly verify official hashes before initiating dynamic extraction.
 */
sealed class RomValidationResult {
    data class Valid(
        val target: RomTarget,
        val computedSha1: String,
        val computedCrc32: String,
        val headerTitle: String,
        val fileSize: Long,
        val isSyntheticSample: Boolean = false
    ) : RomValidationResult()

    data class ChecksumMismatch(
        val detectedTarget: RomTarget?,
        val computedSha1: String,
        val expectedSha1: String,
        val headerTitle: String,
        val fileSize: Long
    ) : RomValidationResult()

    data class CorruptedHeader(
        val reason: String,
        val computedSha1: String,
        val fileSize: Long
    ) : RomValidationResult()

    data class UnsupportedRom(
        val detectedTitle: String,
        val platform: RomPlatform?,
        val computedSha1: String,
        val fileSize: Long
    ) : RomValidationResult()

    val isValid: Boolean get() = this is Valid
}
