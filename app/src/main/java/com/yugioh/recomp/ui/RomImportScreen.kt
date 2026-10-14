package com.yugioh.recomp.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PlayArrow
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
import com.yugioh.recomp.core.rom.RomAssetCache
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
    var statusMessage by remember { mutableStateOf("Select or inject a Game Boy Color Yu-Gi-Oh! ROM") }

    fun processRomBytes(bytes: ByteArray, fileName: String) {
        coroutineScope.launch {
            isProcessing = true
            statusMessage = "Analyzing $fileName (Computing SHA-1)..."
            val result = withContext(Dispatchers.Default) {
                val res = RomValidator.validate(bytes)
                if (res is RomValidationResult.Valid) {
                    RomAssetCache.populateFromRom(res.target, bytes)
                } else if (res is RomValidationResult.UnsupportedRom && res.detectedTitle.contains("YU-GI-OH", ignoreCase = true)) {
                    // Fallback target for development/synthetic
                    RomAssetCache.populateFromRom(RomTarget.DARK_DUEL_STORIES_USA, bytes)
                }
                res
            }
            validationResult = result
            isProcessing = false
            statusMessage = when (result) {
                is RomValidationResult.Valid -> "Target ROM verified and assets extracted successfully!"
                is RomValidationResult.ChecksumMismatch -> "Cartridge title matches, but SHA-1 failed. Verify untrimmed No-Intro dump."
                is RomValidationResult.CorruptedHeader -> "Invalid header: ${result.reason}"
                is RomValidationResult.UnsupportedRom -> "Unsupported ROM header: ${result.detectedTitle}"
            }
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
                        processRomBytes(bytes, uri.lastPathSegment ?: "user_rom.gbc")
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
            text = "Dynamic Asset & Native Card Engine",
            color = Color.LightGray,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

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
                    text = "This APK contains zero copyrighted assets or ROM binaries. Provide an official Yu-Gi-Oh! Dark Duel Stories (GBC) cartridge dump to dynamically extract textures, card tables, and audio.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Action Buttons: File Picker & Verified Sample
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { filePickerLauncher.launch("*/*") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Select ROM", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    val synthBytes = RomValidator.createSyntheticDdsRom()
                    processRomBytes(synthBytes, "Yu-Gi-Oh! Dark Duel Stories (USA) [Synthetic].gbc")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Test Cartridge Dump", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                                    text = "SHA-1 MISMATCH",
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
                        text = RomTarget.DARK_DUEL_STORIES_USA.expectedSha1,
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
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
                PipelineStep("4-Channel Game Boy APU Synthesizer", isReady)
                PipelineStep("GBA Modular Expansion Framework (EDS/WWE)", true)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Launch Duel Arena
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
                text = if (isVerified) "ENTER DUEL ARENA" else "VERIFY OFFICIAL ROM TO PLAY",
                color = if (isVerified) Color.Black else Color.Gray,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
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
