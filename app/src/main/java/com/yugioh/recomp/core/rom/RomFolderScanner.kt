package com.yugioh.recomp.core.rom

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Metadata for a Yu-Gi-Oh! ROM file discovered during storage directory scanning.
 */
data class DiscoveredRom(
    val uri: Uri,
    val fileName: String,
    val fileSize: Long,
    val target: RomTarget?,
    val platform: RomPlatform,
    val isVerified: Boolean,
    val computedSha1: String,
    val title: String,
    val description: String
)

/**
 * Handles persistent storage folder permissions (ACTION_OPEN_DOCUMENT_TREE),
 * automatic directory scanning, and Yu-Gi-Oh! ROM identification.
 */
object RomFolderScanner {

    private const val PREFS_NAME = "duel_decomp_storage_prefs"
    private const val KEY_SAVED_FOLDER_URI = "persistent_rom_folder_uri"

    private val SUPPORTED_EXTENSIONS = setOf("gb", "gbc", "gba", "bin", "zip")

    fun getSavedFolderUri(context: Context): Uri? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriStr = prefs.getString(KEY_SAVED_FOLDER_URI, null) ?: return null
        return try {
            val uri = Uri.parse(uriStr)
            val hasPerm = context.contentResolver.persistedUriPermissions.any {
                it.uri == uri && it.isReadPermission
            }
            if (hasPerm) uri else {
                // Return anyway as fallback in case permissions are retained by provider
                uri
            }
        } catch (e: Exception) {
            null
        }
    }

    fun saveFolderUri(context: Context, uri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: Exception) {
            // Some document providers do not support persistable flags; still save URI
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SAVED_FOLDER_URI, uri.toString()).apply()
    }

    fun clearSavedFolderUri(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriStr = prefs.getString(KEY_SAVED_FOLDER_URI, null)
        if (uriStr != null) {
            try {
                val uri = Uri.parse(uriStr)
                context.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
        }
        prefs.edit().remove(KEY_SAVED_FOLDER_URI).apply()
    }

    /**
     * Asynchronously scans the specified directory tree for Yu-Gi-Oh! ROMs.
     */
    suspend fun scanDirectory(context: Context, treeUri: Uri): List<DiscoveredRom> = withContext(Dispatchers.IO) {
        val results = mutableListOf<DiscoveredRom>()
        val rootDoc = try {
            DocumentFile.fromTreeUri(context, treeUri)
        } catch (e: Exception) {
            null
        } ?: return@withContext results

        val files = try {
            rootDoc.listFiles()
        } catch (e: Exception) {
            emptyArray()
        }

        for (file in files) {
            if (file.isDirectory) continue
            val name = file.name ?: continue
            val ext = name.substringAfterLast('.', "").lowercase()
            if (ext !in SUPPORTED_EXTENSIONS) continue

            // Limit inspection to files up to 32MB (typical GBA max size)
            val length = file.length()
            if (length > 32 * 1024 * 1024) continue

            try {
                val bytes = context.contentResolver.openInputStream(file.uri)?.use { it.readBytes() } ?: continue
                val validation = RomValidator.validate(bytes)
                when (validation) {
                    is RomValidationResult.Valid -> {
                        results.add(
                            DiscoveredRom(
                                uri = file.uri,
                                fileName = name,
                                fileSize = length,
                                target = validation.target,
                                platform = validation.target.platform,
                                isVerified = true,
                                computedSha1 = validation.computedSha1,
                                title = validation.target.title,
                                description = validation.target.description
                            )
                        )
                    }
                    is RomValidationResult.ChecksumMismatch -> {
                        val target = validation.detectedTarget
                        results.add(
                            DiscoveredRom(
                                uri = file.uri,
                                fileName = name,
                                fileSize = length,
                                target = target,
                                platform = target?.platform ?: RomPlatform.GBC,
                                isVerified = false,
                                computedSha1 = validation.computedSha1,
                                title = target?.title ?: validation.headerTitle,
                                description = "Modified or unofficial cartridge dump (SHA-1 checksum mismatch)."
                            )
                        )
                    }
                    is RomValidationResult.UnsupportedRom -> {
                        if (validation.detectedTitle.contains("YUGIOH", ignoreCase = true) ||
                            validation.detectedTitle.contains("YU-GI-OH", ignoreCase = true) ||
                            name.contains("yugioh", ignoreCase = true) ||
                            name.contains("yu-gi-oh", ignoreCase = true)
                        ) {
                            val platform = validation.platform ?: if (ext == "gba") RomPlatform.GBA else RomPlatform.GBC
                            results.add(
                                DiscoveredRom(
                                    uri = file.uri,
                                    fileName = name,
                                    fileSize = length,
                                    target = null,
                                    platform = platform,
                                    isVerified = false,
                                    computedSha1 = validation.computedSha1,
                                    title = if (validation.detectedTitle.isNotBlank()) validation.detectedTitle else name,
                                    description = "Unrecognized Yu-Gi-Oh! cartridge format or revision."
                                )
                            )
                        }
                    }
                    is RomValidationResult.CorruptedHeader -> {
                        // Skip corrupted/invalid files
                    }
                }
            } catch (_: Exception) {
                // Ignore unreadable files
            }
        }

        // Sort verified games to top, then GBA, then GBC
        results.sortedWith(
            compareByDescending<DiscoveredRom> { it.isVerified }
                .thenBy { it.platform }
                .thenBy { it.title }
        )
    }
}
