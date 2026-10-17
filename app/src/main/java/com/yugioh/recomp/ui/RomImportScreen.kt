package com.yugioh.recomp.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugioh.recomp.core.rom.CardDataExtractor
import com.yugioh.recomp.core.rom.CardArtworkLoader
import com.yugioh.recomp.core.rom.DiscoveredRom
import com.yugioh.recomp.core.rom.RomAssetCache
import com.yugioh.recomp.core.rom.RomFolderScanner
import com.yugioh.recomp.core.rom.RomPlatform
import com.yugioh.recomp.core.rom.RomTarget
import com.yugioh.recomp.core.rom.RomValidationResult
import com.yugioh.recomp.core.rom.RomValidator
import com.yugioh.recomp.ui.theme.MillenniumGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RomImportScreen(
    onDuelReady: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var validationResult by remember { mutableStateOf<RomValidationResult?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Select a persistent ROM folder or choose an official cartridge dump.") }

    // Persistent Storage Folder State
    var savedFolderUri by remember { mutableStateOf<Uri?>(null) }
    var discoveredRoms by remember { mutableStateOf<List<DiscoveredRom>>(emptyList()) }
    var isScanningFolder by remember { mutableStateOf(false) }

    // Offline Artwork Cache State
    val canonicalCards = remember { CardDataExtractor.getCanonicalDdsCardDatabase() }
    var cachedArtCount by remember { mutableIntStateOf(0) }
    var totalArtCount by remember { mutableIntStateOf(canonicalCards.size) }
    var isSyncingArt by remember { mutableStateOf(false) }
    var syncArtProgress by remember { mutableFloatStateOf(0f) }
    var syncCurrentCardName by remember { mutableStateOf("") }

    fun refreshArtCount() {
        val (cached, total) = CardArtworkLoader.getSyncStatus(canonicalCards)
        cachedArtCount = cached
        totalArtCount = total
    }

    fun scanFolder(folderUri: Uri) {
        coroutineScope.launch {
            isScanningFolder = true
            statusMessage = "Scanning storage folder for Yu-Gi-Oh! ROMs..."
            val roms = RomFolderScanner.scanDirectory(context, folderUri)
            discoveredRoms = roms
            isScanningFolder = false
            statusMessage = if (roms.isNotEmpty()) {
                "Found ${roms.size} Yu-Gi-Oh! ROM${if (roms.size == 1) "" else "s"} in library."
            } else {
                "No supported Yu-Gi-Oh! ROMs (.gbc / .gba) found in this folder."
            }
        }
    }

    // Auto-scan on screen launch if folder permission was granted previously
    LaunchedEffect(Unit) {
        refreshArtCount()
        val savedUri = RomFolderScanner.getSavedFolderUri(context)
        if (savedUri != null) {
            savedFolderUri = savedUri
            scanFolder(savedUri)
        }
    }

    fun processRomBytes(bytes: ByteArray, fileName: String, autoLaunch: Boolean = false) {
        coroutineScope.launch {
            isProcessing = true
            statusMessage = "Analyzing $fileName (Computing SHA-1)..."
            val result = withContext(Dispatchers.Default) {
                val res = RomValidator.validate(bytes)
                if (res is RomValidationResult.Valid) {
                    RomAssetCache.populateFromRom(res.target, bytes)
                }
                res
            }
            validationResult = result
            isProcessing = false
            statusMessage = when (result) {
                is RomValidationResult.Valid -> "Target ROM verified and assets extracted successfully!"
                is RomValidationResult.ChecksumMismatch -> "Cartridge title matches, but SHA-1 failed. Verify untrimmed No-Intro dump."
                is RomValidationResult.CorruptedHeader -> "Invalid header: ${result.reason}"
                is RomValidationResult.UnsupportedRom -> "Unsupported ROM: ${result.detectedTitle}. Please provide an authentic EDS (GBA) or DDS (GBC) dump."
            }
            if (autoLaunch && result is RomValidationResult.Valid) {
                onDuelReady()
            }
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            savedFolderUri = uri
            RomFolderScanner.saveFolderUri(context, uri)
            scanFolder(uri)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null) {
                        processRomBytes(bytes, uri.lastPathSegment ?: "user_rom")
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        statusMessage = "Error reading ROM file: ${e.localizedMessage}"
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F111A), Color(0xFF161928), Color(0xFF0A0C14))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Millennium Symbol & Header
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2208))
                .border(2.dp, MillenniumGold, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "𓂀",
                fontSize = 36.sp,
                color = MillenniumGold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Duel-decomp",
            color = MillenniumGold,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )

        Text(
            text = "Clean Dynamic Asset Recompilation Engine",
            color = Color.LightGray,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Architectural Compliance Notice
        Surface(
            color = Color(0xFF141724),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF2D3248), RoundedCornerShape(8.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Gen1Recomp Clean Pipeline Architecture",
                    color = MillenniumGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "This application ships with zero copyrighted assets, ROM dumps, or proprietary binary rips. To unlock duels, provide access to a folder containing your legal cartridge dumps (GBA or GBC). Assets and card tables are dynamically parsed and verified via SHA-1.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // =========================================================================
        // SECTION 1: PERSISTENT ROM STORAGE FOLDER
        // =========================================================================
        Surface(
            color = Color(0xFF131A2B),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF26324D), RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = MillenniumGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ROM Storage Library",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (savedFolderUri != null) {
                        Surface(
                            color = Color(0xFF166534),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "PERSISTENT",
                                color = Color(0xFF4ADE80),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (savedFolderUri != null) {
                    val folderDisplayName = savedFolderUri?.lastPathSegment
                        ?.substringAfterLast(':') ?: "Selected Folder"

                    Text(
                        text = "Folder: $folderDisplayName",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { savedFolderUri?.let { scanFolder(it) } },
                            enabled = !isScanningFolder,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rescan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { folderPickerLauncher.launch(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Change", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                RomFolderScanner.clearSavedFolderUri(context)
                                savedFolderUri = null
                                discoveredRoms = emptyList()
                                statusMessage = "ROM storage folder disconnected."
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Clear", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text(
                        text = "Give the app persistent access to a specific folder on your device that contains your Yu-Gi-Oh! ROMs to auto-detect and switch between titles seamlessly.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { folderPickerLauncher.launch(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Grant Folder Access", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Scanning Progress Indicator
                if (isScanningFolder) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MillenniumGold,
                        trackColor = Color(0xFF1E293B)
                    )
                }

                // Discovered ROMs List
                if (discoveredRoms.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Detected Games in Folder:",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    discoveredRoms.forEach { rom ->
                        DiscoveredRomCard(
                            rom = rom,
                            onPlay = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val bytes = context.contentResolver.openInputStream(rom.uri)?.use { it.readBytes() }
                                        if (bytes != null) {
                                            processRomBytes(bytes, rom.fileName, autoLaunch = true)
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            statusMessage = "Failed to load ${rom.fileName}: ${e.localizedMessage}"
                                        }
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // =========================================================================
        // SECTION 2: OFFLINE ARTWORK CACHE & SYNC
        // =========================================================================
        Surface(
            color = Color(0xFF131A2B),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF26324D), RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Offline Card Artwork",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "$cachedArtCount / $totalArtCount Cached",
                        color = if (cachedArtCount >= totalArtCount && totalArtCount > 0) Color(0xFF4ADE80) else Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Download and store official high-definition card artwork to internal storage so the entire game works completely offline without network connection.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                if (isSyncingArt) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { syncArtProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF38BDF8),
                        trackColor = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Downloading: $syncCurrentCardName (${(syncArtProgress * 100).toInt()}%)",
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isSyncingArt = true
                                CardArtworkLoader.syncAllCardArtwork(canonicalCards) { current, total, cardName ->
                                    syncArtProgress = current.toFloat() / total.toFloat()
                                    syncCurrentCardName = cardName
                                    cachedArtCount = current
                                }
                                refreshArtCount()
                                isSyncingArt = false
                                statusMessage = "Offline artwork sync complete ($cachedArtCount cards saved)."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (cachedArtCount >= totalArtCount && totalArtCount > 0) "Artwork Fully Cached (Update)" else "Download Artwork for Offline Play",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // =========================================================================
        // SECTION 3: INDIVIDUAL FILE PICKER
        // =========================================================================
        Button(
            onClick = { filePickerLauncher.launch("*/*") },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.Default.FileOpen, contentDescription = null, tint = MillenniumGold, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pick Single ROM File (.gba / .gbc)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Progress Bar
        if (isProcessing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MillenniumGold,
                trackColor = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Status Card
        Surface(
            color = Color(0xFF141724),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF2B3045), RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ROM Verification Status",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    when (validationResult) {
                        is RomValidationResult.Valid -> {
                            Surface(color = Color(0xFF166534), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = "VERIFIED SHA-1",
                                    color = Color(0xFF4ADE80),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        is RomValidationResult.ChecksumMismatch -> {
                            Surface(color = Color(0xFF991B1B), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = "HASH MISMATCH",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        else -> {
                            Surface(color = Color(0xFF334155), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = "READY",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = statusMessage,
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp
                )

                validationResult?.let { res ->
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFF252A3C))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Computed SHA-1:",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                    Text(
                        text = when (res) {
                            is RomValidationResult.Valid -> res.computedSha1
                            is RomValidationResult.ChecksumMismatch -> res.computedSha1
                            is RomValidationResult.CorruptedHeader -> res.computedSha1
                            is RomValidationResult.UnsupportedRom -> res.computedSha1
                        },
                        color = MillenniumGold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Target Checksum:",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                    Text(
                        text = (res as? RomValidationResult.Valid)?.target?.expectedSha1
                            ?: (res as? RomValidationResult.ChecksumMismatch)?.expectedSha1
                            ?: RomTarget.ETERNAL_DUELIST_SOUL_USA.expectedSha1,
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    val activePlatform = (res as? RomValidationResult.Valid)?.target?.platform
                    if (activePlatform != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Platform Mode: ${activePlatform.name} (${if (activePlatform == RomPlatform.GBA) "16-Color Advance • EDS Rules" else "2bpp Retro • DDS Rules"})",
                            color = if (activePlatform == RomPlatform.GBA) Color(0xFFA5B4FC) else MillenniumGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Pipeline Stages
        Surface(
            color = Color(0xFF141724),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF2B3045), RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Asset Rebuild Pipeline",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val isReady = RomAssetCache.isAssetPipelineReady
                PipelineStep("GBC / GBA Cartridge Header Parser", isReady)
                PipelineStep("2bpp Planar Tile & Palette Decoders", isReady)
                PipelineStep("800-Card Dark Duel Stories Database", isReady)
                PipelineStep("Persistent Storage ROM Library Scanner", savedFolderUri != null)
                PipelineStep("Offline Authentic Card Artwork Cache", cachedArtCount > 0)
                PipelineStep("GBA Modular Expansion Framework (EDS/WWE)", isReady && RomAssetCache.activeTarget?.platform == RomPlatform.GBA)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Launch Duel Campaign
        val isVerified = validationResult is RomValidationResult.Valid && RomAssetCache.isAssetPipelineReady
        Button(
            onClick = {
                if (isVerified) {
                    onDuelReady()
                }
            },
            enabled = isVerified,
            colors = ButtonDefaults.buttonColors(
                containerColor = MillenniumGold,
                disabledContainerColor = Color(0xFF1E2230)
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(
                if (isVerified) Icons.Default.PlayArrow else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isVerified) Color.Black else Color.Gray
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isVerified) "ENTER DUEL CAMPAIGN" else "VERIFY OFFICIAL ROM TO PLAY",
                color = if (isVerified) Color.Black else Color.Gray,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DiscoveredRomCard(
    rom: DiscoveredRom,
    onPlay: () -> Unit
) {
    Surface(
        color = Color(0xFF1A2234),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (rom.isVerified) Color(0xFF2563EB) else Color(0xFF475569), RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (rom.platform == RomPlatform.GBA) Color(0xFF6B21A8) else Color(0xFFB45309),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = rom.platform.name,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = rom.title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = rom.fileName,
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${(rom.fileSize / 1024)} KB",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(containerColor = MillenniumGold),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("PLAY", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun PipelineStep(title: String, complete: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (complete) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4ADE80),
                modifier = Modifier.size(15.dp)
            )
        } else {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = if (complete) Color.White else Color(0xFF94A3B8),
            fontSize = 11.sp
        )
    }
}
