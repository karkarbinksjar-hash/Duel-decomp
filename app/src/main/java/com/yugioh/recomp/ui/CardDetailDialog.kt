package com.yugioh.recomp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.yugioh.recomp.core.engine.Card
import com.yugioh.recomp.core.rom.RomAssetCache
import com.yugioh.recomp.ui.theme.MillenniumGold

/**
 * High-Definition Card Inspection Dialog.
 * Shows authentic, full-resolution Kazuki Takahashi card artwork by default,
 * complete with stat ribbons, lore description, and a retro GBC toggle.
 */
@Composable
fun CardDetailDialog(
    card: Card,
    onDismiss: () -> Unit
) {
    var showTcgFrame by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        if (showTcgFrame) {
            // =====================================================================
            // FULL AUTHENTIC HIGH-RESOLUTION TCG CARD PRESENTATION
            // =====================================================================
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF101422),
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .border(2.dp, MillenniumGold, RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Bar: Card Name + Attribute Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = card.name,
                            color = MillenniumGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(card.attribute.colorHex),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = card.attribute.displayName.uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Authentic Full Card Render
                    val officialFull = com.yugioh.recomp.core.rom.rememberCardArtwork(card, isFullCard = true)
                    val fullCardBitmap = officialFull ?: BitmapUtils.spriteToImageBitmap(RomAssetCache.getCardFullTexture(card.id))

                    Box(
                        modifier = Modifier
                            .width(220.dp)
                            .height(320.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.dp, MillenniumGold.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                            .background(Color(0xFF0B0E17)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = fullCardBitmap,
                            contentDescription = card.name,
                            filterQuality = FilterQuality.Medium,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Stats Scoreboard (Monsters: Level, Type, ATK, DEF / Spells: DC)
                    if (card.isMonster) {
                        Surface(
                            color = Color(0xFF1E2538),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${card.level} ★",
                                        color = Color(0xFFF59E0B),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "[${card.monsterType.displayName}]",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "ATK ",
                                        color = Color(0xFFDC2626),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "${card.atk}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "DEF ",
                                        color = Color(0xFF2563EB),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "${card.def}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = Color(0xFF1E2538),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (card.isSpell) "SPELL CARD" else "TRAP CARD",
                                    color = if (card.isSpell) Color(0xFF14B8A6) else Color(0xFFEC4899),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Cost: ${card.deckCost} DC",
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Lore / Effect Description Box
                    Surface(
                        color = Color(0xFF181D2D),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = card.description,
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Footer Action Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showTcgFrame = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFCBD5E1))
                        ) {
                            Text("🎮 Retro Status", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = MillenniumGold),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Text("Close", color = Color(0xFF101422), fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            // =====================================================================
            // RETRO GAME BOY COLOR (GBC) STATUS SCREEN
            // =====================================================================
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFFBF198),
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .border(3.dp, Color(0xFF18150D), RoundedCornerShape(6.dp))
                    .padding(2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(Color(0xFFF7EC85))
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, Color(0xFF282315), RoundedCornerShape(2.dp))
                            .background(Color(0xFFFFFAD6))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = card.name,
                            color = Color(0xFF1A1608),
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(card.attribute.colorHex),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text(
                                text = card.attribute.displayName.uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Upper Section: Framed Artwork + Stats Column
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        val cropArtwork = com.yugioh.recomp.core.rom.rememberCardArtwork(card, isFullCard = false)
                        val upscaledSprite = RomAssetCache.getCardSpriteUpscaled(card.id, scale = 4)
                        val spriteBitmap = cropArtwork ?: BitmapUtils.spriteToImageBitmap(upscaledSprite)

                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .border(3.dp, Color(0xFF2E1065))
                                .border(1.dp, Color(0xFF000000))
                                .background(Color(0xFF0F0B18)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = spriteBitmap,
                                contentDescription = card.name,
                                filterQuality = FilterQuality.Medium,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.5.dp, Color(0xFF282315), RoundedCornerShape(2.dp))
                                .background(Color(0xFFFFFDF0))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("No.", color = Color(0xFF524A3A), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    "${card.id}",
                                    color = Color(0xFF18150D),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            if (card.isMonster) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Level", color = Color(0xFF524A3A), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${card.level} ★",
                                        color = Color(0xFFB45309),
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("DC", color = Color(0xFF524A3A), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${card.deckCost}",
                                        color = Color(0xFF047857),
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                HorizontalDivider(color = Color(0xFFD4C79B), thickness = 1.dp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("ATK", color = Color(0xFFDC2626), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                                    Text(
                                        "${card.atk}",
                                        color = Color(0xFFB91C1C),
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("DEF", color = Color(0xFF2563EB), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                                    Text(
                                        "${card.def}",
                                        color = Color(0xFF1D4ED8),
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                HorizontalDivider(color = Color(0xFFD4C79B), thickness = 1.dp)

                                Text(
                                    text = card.monsterType.displayName,
                                    color = Color(0xFF1F2937),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = if (card.isSpell) "SPELL CARD" else "TRAP CARD",
                                    color = if (card.isSpell) Color(0xFF0D9488) else Color(0xFFBE185D),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Cost: ${card.deckCost} DC",
                                    color = Color(0xFF047857),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Lore Text Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, Color(0xFF282315), RoundedCornerShape(2.dp))
                            .background(Color(0xFFFFFDF0))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = card.description,
                            color = Color(0xFF1E1B15),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showTcgFrame = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1E293B))
                        ) {
                            Text("🃏 View Full Card", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18150D)),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("Close", color = Color(0xFFFFFAD6), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
