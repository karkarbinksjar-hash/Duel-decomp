package com.yugioh.recomp.core.rom

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.yugioh.recomp.core.engine.Card
import com.yugioh.recomp.core.engine.CardPasscodeRegistry
import com.yugioh.recomp.ui.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * On-Demand High-Resolution Card Artwork Loader.
 *
 * Strict Architectural Compliance:
 * - NEVER bundles copyrighted ROM bits or card images inside APK packages.
 * - Downloads authentic Kazuki Takahashi card illustrations dynamically on-demand at runtime
 *   via public card database CDNs using canonical 8-digit passcodes.
 * - Caches images locally in device cache directory for offline persistence.
 * - Seamlessly falls back to authentic extracted ROM tiles or clean vectors when offline.
 */
object CardArtworkLoader {

    private var cacheDir: File? = null
    private val memoryCache = ConcurrentHashMap<String, ImageBitmap>()
    private val failedCodes = ConcurrentHashMap<String, Long>()

    fun init(context: Context) {
        if (cacheDir == null) {
            val dir = File(context.cacheDir, "card_art")
            if (!dir.exists()) dir.mkdirs()
            cacheDir = dir
        }
    }

    fun getPasscode(card: Card): String? {
        val direct = card.password?.trim()
        if (!direct.isNullOrBlank() && direct.length in 7..8) {
            return direct.padStart(8, '0')
        }
        val fromRegistry = CardPasscodeRegistry.allPasscodes.entries.firstOrNull {
            it.value.equals(card.name, ignoreCase = true)
        }?.key
        return fromRegistry?.padStart(8, '0')
    }

    fun getCached(key: String): ImageBitmap? = memoryCache[key]

    suspend fun loadArtwork(card: Card, isFullCard: Boolean = false): ImageBitmap? = withContext(Dispatchers.IO) {
        val passcode = getPasscode(card) ?: return@withContext null
        val key = if (isFullCard) "full_$passcode" else "crop_$passcode"

        memoryCache[key]?.let { return@withContext it }

        // Throttle failed downloads (cooldown 30s)
        val lastFail = failedCodes[key]
        if (lastFail != null && System.currentTimeMillis() - lastFail < 30_000) {
            return@withContext null
        }

        // 1. Check local device disk cache
        val localDir = cacheDir ?: File("/data/data/com.yugioh.recomp/cache/card_art").takeIf { it.exists() }
        val diskFile = localDir?.let { File(it, "$key.jpg") }
        if (diskFile != null && diskFile.exists() && diskFile.length() > 0) {
            try {
                val bmp = BitmapFactory.decodeFile(diskFile.absolutePath)
                if (bmp != null) {
                    val imgBmp = bmp.asImageBitmap()
                    memoryCache[key] = imgBmp
                    return@withContext imgBmp
                }
            } catch (_: Throwable) {}
        }

        // 2. Fetch on-demand from public card CDN
        val urlStr = if (isFullCard) {
            "https://images.ygoprodeck.com/images/cards/$passcode.jpg"
        } else {
            "https://images.ygoprodeck.com/images/cards_cropped/$passcode.jpg"
        }

        try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 8000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "DuelDecomp/1.9.0 (Android Native Recomp)")

            if (conn.responseCode == 200) {
                val bytes = conn.inputStream.use { it.readBytes() }
                if (bytes.isNotEmpty()) {
                    // Write to local disk cache
                    diskFile?.let { f ->
                        try {
                            FileOutputStream(f).use { out -> out.write(bytes) }
                        } catch (_: Throwable) {}
                    }
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        val imgBmp = bmp.asImageBitmap()
                        memoryCache[key] = imgBmp
                        return@withContext imgBmp
                    }
                }
            } else {
                failedCodes[key] = System.currentTimeMillis()
            }
        } catch (_: Throwable) {
            failedCodes[key] = System.currentTimeMillis()
        }

        return@withContext null
    }
}

/**
 * Compose helper to asynchronously load and remember official high-res card artwork.
 */
@Composable
fun rememberCardArtwork(card: Card?, isFullCard: Boolean = false): ImageBitmap? {
    if (card == null) return null
    val passcode = CardArtworkLoader.getPasscode(card) ?: return null
    val key = if (isFullCard) "full_$passcode" else "crop_$passcode"

    var bitmapState by remember(key) { mutableStateOf(CardArtworkLoader.getCached(key)) }

    LaunchedEffect(key) {
        if (bitmapState == null) {
            val loaded = CardArtworkLoader.loadArtwork(card, isFullCard)
            if (loaded != null) {
                bitmapState = loaded
            }
        }
    }

    return bitmapState
}

/**
 * Universal Card Display Bitmap Resolver:
 * Returns authentic high-res card artwork when loaded, falling back seamlessly
 * to high-definition rendered textures if still downloading or offline.
 */
@Composable
fun rememberCardDisplayBitmap(card: Card?, isFullCard: Boolean = true): ImageBitmap {
    if (card == null) {
        val back = RomAssetCache.getCardBack()
        return remember(back) { BitmapUtils.spriteToImageBitmap(back) }
    }
    val officialArt = rememberCardArtwork(card, isFullCard)
    val fallback: ImageBitmap = remember(card.id, isFullCard) {
        if (isFullCard) {
            BitmapUtils.spriteToImageBitmap(RomAssetCache.getCardFullTexture(card.id))
        } else {
            BitmapUtils.spriteToImageBitmap(RomAssetCache.getCardSpriteUpscaled(card.id, scale = 4))
        }
    }
    return officialArt ?: fallback
}

