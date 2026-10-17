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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * On-Demand High-Resolution Card Artwork Loader.
 *
 * Strict Architectural Compliance:
 * - NEVER bundles copyrighted ROM bits or card images inside APK packages.
 * - Downloads authentic Kazuki Takahashi card illustrations dynamically on-demand at runtime
 *   via public card database CDNs using canonical 8-digit passcodes.
 * - Handles leading zeros, OCG/TCG name variations, and fallback cardinfo API lookups.
 * - Caches images locally in device cache directory for offline persistence.
 * - Seamlessly falls back to authentic extracted ROM tiles or clean vectors when offline.
 */
object CardArtworkLoader {

    private var cacheDir: File? = null
    private val memoryCache = ConcurrentHashMap<String, ImageBitmap>()
    private val failedCodes = ConcurrentHashMap<String, Long>()

    // Canonical card name alias normalizer for consistent API lookup
    private val nameAliases = mapOf(
        "Frog The Jam" to "Slime Toad",
        "Dian Keto the Cure Maiden" to "Dian Keto the Cure Master",
        "Air Marmot of Nefariousness" to "Air Marmot of Nefariousness",
        "Cyber Harpie" to "Cyber Harpie Lady",
        "Winged Dragon #1" to "Winged Dragon, Guardian of the Fortress #1",
        "Zombie Dragon" to "Dragon Zombie"
    )

    fun init(context: Context) {
        if (cacheDir == null) {
            val dir = File(context.cacheDir, "card_art")
            if (!dir.exists()) dir.mkdirs()
            cacheDir = dir
        }
    }

    fun getPasscode(card: Card): String? {
        val direct = card.password?.trim()
        if (!direct.isNullOrBlank() && direct.length in 6..8) {
            return direct
        }
        val fromRegistry = CardPasscodeRegistry.findPasscodeByCardName(card.name)
        if (!fromRegistry.isNullOrBlank()) {
            return fromRegistry
        }
        val alias = nameAliases[card.name]
        if (alias != null) {
            val fromAlias = CardPasscodeRegistry.findPasscodeByCardName(alias)
            if (!fromAlias.isNullOrBlank()) return fromAlias
        }
        return null
    }

    fun getCached(key: String): ImageBitmap? = memoryCache[key]

    suspend fun loadArtwork(card: Card, isFullCard: Boolean = false): ImageBitmap? = withContext(Dispatchers.IO) {
        val rawCode = getPasscode(card)
        val cleanId = rawCode?.trimStart('0')?.takeIf { it.isNotEmpty() }
        val cardKey = cleanId ?: rawCode ?: card.name.lowercase().replace(" ", "_")
        val key = if (isFullCard) "full_$cardKey" else "crop_$cardKey"

        memoryCache[key]?.let { return@withContext it }

        // Throttle repeated failed downloads (cooldown 15s)
        val lastFail = failedCodes[key]
        if (lastFail != null && System.currentTimeMillis() - lastFail < 15_000) {
            return@withContext null
        }

        // 1. Check local device disk cache
        val localDir = cacheDir ?: File("/data/data/com.yugioh.recomp/cache/card_art").takeIf { it.exists() }
        val candidateFileNames = listOfNotNull(
            "$key.jpg",
            cleanId?.let { if (isFullCard) "full_$it.jpg" else "crop_$it.jpg" },
            rawCode?.let { if (isFullCard) "full_$it.jpg" else "crop_$it.jpg" }
        )
        for (fName in candidateFileNames) {
            val diskFile = localDir?.let { File(it, fName) }
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
        }

        // 2. Build candidate URLs to try from public CDN
        val candidateUrls = mutableListOf<String>()
        if (cleanId != null) {
            if (isFullCard) {
                candidateUrls.add("https://images.ygoprodeck.com/images/cards/$cleanId.jpg")
                candidateUrls.add("https://images.ygoprodeck.com/images/cards_small/$cleanId.jpg")
            } else {
                candidateUrls.add("https://images.ygoprodeck.com/images/cards_cropped/$cleanId.jpg")
                candidateUrls.add("https://images.ygoprodeck.com/images/cards/$cleanId.jpg")
            }
        }
        if (rawCode != null && rawCode != cleanId) {
            if (isFullCard) {
                candidateUrls.add("https://images.ygoprodeck.com/images/cards/$rawCode.jpg")
            } else {
                candidateUrls.add("https://images.ygoprodeck.com/images/cards_cropped/$rawCode.jpg")
            }
        }

        // Attempt downloads from direct image CDN
        for (urlStr in candidateUrls) {
            val bytes = downloadBytes(urlStr)
            if (bytes != null && bytes.isNotEmpty()) {
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) {
                    saveToDiskCache(localDir, key, bytes)
                    cleanId?.let { saveToDiskCache(localDir, if (isFullCard) "full_$it" else "crop_$it", bytes) }
                    val imgBmp = bmp.asImageBitmap()
                    memoryCache[key] = imgBmp
                    return@withContext imgBmp
                }
            }
        }

        // 3. Fallback: Query YGOPRODeck API by exact or aliased card name
        val lookupName = nameAliases[card.name] ?: card.name
        try {
            val encodedName = URLEncoder.encode(lookupName, "UTF-8")
            val apiUrl = "https://db.ygoprodeck.com/api/v7/cardinfo.php?name=$encodedName"
            val jsonText = downloadString(apiUrl)
            if (!jsonText.isNullOrBlank()) {
                val extractedUrl = extractImageUrlFromJson(jsonText, isFullCard)
                if (extractedUrl != null) {
                    val bytes = downloadBytes(extractedUrl)
                    if (bytes != null && bytes.isNotEmpty()) {
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bmp != null) {
                            saveToDiskCache(localDir, key, bytes)
                            val imgBmp = bmp.asImageBitmap()
                            memoryCache[key] = imgBmp
                            return@withContext imgBmp
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            failedCodes[key] = System.currentTimeMillis()
        }

        failedCodes[key] = System.currentTimeMillis()
        return@withContext null
    }

    private fun downloadBytes(urlStr: String): ByteArray? {
        return try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 7000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "DuelDecomp/2.1.0 (Android Native Recomp)")
            if (conn.responseCode == 200) {
                conn.inputStream.use { it.readBytes() }
            } else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun downloadString(urlStr: String): String? {
        return try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 6000
            conn.setRequestProperty("User-Agent", "DuelDecomp/2.1.0")
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun extractImageUrlFromJson(json: String, isFullCard: Boolean): String? {
        val targetKey = if (isFullCard) "\"image_url\":" else "\"image_url_cropped\":"
        val idx = json.indexOf(targetKey)
        if (idx == -1) return null
        val startQuote = json.indexOf("\"", idx + targetKey.length)
        if (startQuote == -1) return null
        val endQuote = json.indexOf("\"", startQuote + 1)
        if (endQuote == -1) return null
        return json.substring(startQuote + 1, endQuote).replace("\\/", "/")
    }

    private fun saveToDiskCache(dir: File?, baseName: String, data: ByteArray) {
        if (dir == null) return
        try {
            val file = File(dir, "$baseName.jpg")
            FileOutputStream(file).use { it.write(data) }
        } catch (_: Throwable) {}
    }

    fun isCardCached(card: Card, isFullCard: Boolean = false): Boolean {
        val rawCode = getPasscode(card)
        val cleanId = rawCode?.trimStart('0')?.takeIf { it.isNotEmpty() }
        val cardKey = cleanId ?: rawCode ?: card.name.lowercase().replace(" ", "_")
        val key = if (isFullCard) "full_$cardKey" else "crop_$cardKey"

        if (memoryCache.containsKey(key)) return true
        val localDir = cacheDir ?: File("/data/data/com.yugioh.recomp/cache/card_art").takeIf { it.exists() }
        val candidateFileNames = listOfNotNull(
            "$key.jpg",
            cleanId?.let { if (isFullCard) "full_$it.jpg" else "crop_$it.jpg" },
            rawCode?.let { if (isFullCard) "full_$it.jpg" else "crop_$it.jpg" }
        )
        return candidateFileNames.any { fName ->
            val file = localDir?.let { File(it, fName) }
            file != null && file.exists() && file.length() > 0
        }
    }

    fun getSyncStatus(cards: List<Card>): Pair<Int, Int> {
        val cached = cards.count { isCardCached(it, isFullCard = false) }
        return Pair(cached, cards.size)
    }

    suspend fun syncAllCardArtwork(
        cards: List<Card>,
        onProgress: (current: Int, total: Int, cardName: String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        var newlyCached = 0
        val total = cards.size
        for ((index, card) in cards.withIndex()) {
            val wasCached = isCardCached(card, isFullCard = false)
            if (!wasCached) {
                val loadedCrop = loadArtwork(card, isFullCard = false)
                val loadedFull = loadArtwork(card, isFullCard = true)
                if (loadedCrop != null || loadedFull != null) {
                    newlyCached++
                }
            }
            onProgress(index + 1, total, card.name)
        }
        newlyCached
    }

    fun preloadCards(cards: List<Card>) {
        CoroutineScope(Dispatchers.IO).launch {
            for (card in cards) {
                try {
                    loadArtwork(card, isFullCard = false)
                    loadArtwork(card, isFullCard = true)
                } catch (_: Throwable) {}
            }
        }
    }
}

/**
 * Compose helper to asynchronously load and remember official high-res card artwork.
 */
@Composable
fun rememberCardArtwork(card: Card?, isFullCard: Boolean = false): ImageBitmap? {
    if (card == null) return null
    val rawCode = CardArtworkLoader.getPasscode(card)
    val cleanId = rawCode?.trimStart('0')?.takeIf { it.isNotEmpty() }
    val cardKey = cleanId ?: rawCode ?: card.name.lowercase().replace(" ", "_")
    val key = if (isFullCard) "full_$cardKey" else "crop_$cardKey"

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
 * Returns authentic high-res card artwork when loaded, falling back gracefully
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
