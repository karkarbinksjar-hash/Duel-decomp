package com.yugioh.recomp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.yugioh.recomp.core.engine.Card
import com.yugioh.recomp.core.rom.RomAssetCache
import com.yugioh.recomp.ui.theme.MillenniumGold

/**
 * Authentic Dark Duel Stories (GBC) card inspection status dialog.
 * Accurately replicates the classic Game Boy Color yellow status screen
 * with crisp integer-upscaled pixel art, card number, level, DC, ATK/DEF,
 * alignment, and lore, with a toggle to view the full TCG card frame.
 */
@Composable
fun CardDetailDialog(
    card: Card,
    onDismiss: () -> Unit
) {
    var showTcgFrame by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        if (!showTcgFrame) {
            // AUTHENTIC DARK DUEL STORIES (GBC) RETRO STATUS SCREEN
            // Replicates the classic GBC yellow card presentation (Data Crystal / Yugipedia reference)
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFFFBF198), // Classic GBC warm parchment yellow
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .border(3.dp, Color(0xFF18150D), RoundedCornerShape(4.dp))
                    .padding(2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(Color(0xFFF7EC85))
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    // Top Bar: Card Name in retro pixel font + Element Badge
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

                    // Upper Section: Framed Pixel Art (Left) + GBC Stats Column (Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Framed Monster Pixel Art Window with Purple/Dark double border
                        val cropArtwork = com.yugioh.recomp.core.rom.rememberCardArtwork(card, isFullCard = false)
                        val upscaledSprite = RomAssetCache.getCardSpriteUpscaled(card.id, scale = 4)
                        val spriteBitmap = cropArtwork ?: BitmapUtils.spriteToImageBitmap(upscaledSprite)

                        Box(
                            modifier = Modifier
                                .size(136.dp)
                                .border(3.dp, Color(0xFF2E1065)) // Deep Purple GBC frame border
                                .border(1.dp, Color(0xFF000000))
                                .background(Color(0xFF0F0B18)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = spriteBitmap,
                                contentDescription = card.name,
                                filterQuality = FilterQuality.None, // Crisp nearest-neighbor integer scaling
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Right Column: Authentic GBC Stats
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.5.dp, Color(0xFF282315), RoundedCornerShape(2.dp))
                                .background(Color(0xFFFFFAD6))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Card Number
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("No.", color = Color(0xFF524A3A), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                Text(
                                    String.format("%03d", card.id),
                                    color = Color(0xFF18150D),
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            if (card.isMonster) {
                                // Level
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

                                // Deck Cost (DC)
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

                                // ATK
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

                                // DEF
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

                                // Monster Type
                                Text(
                                    text = card.monsterType.displayName,
                                    color = Color(0xFF1F2937),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )

                                // Alignment
                                Text(
                                    text = card.attribute.displayName,
                                    color = Color(card.attribute.colorHex),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black
                                )
                            } else {
                                Text(
                                    text = if (card.isSpell) "SPELL CARD" else "TRAP CARD",
                                    color = Color(card.attribute.colorHex),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black
                                )
                                Text("Cost: ${card.deckCost} DC", color = Color(0xFF047857), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bottom Box: Lore Parchment Box
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

                    // Bottom Bar: Switch View & Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showTcgFrame = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1E293B))
                        ) {
                            Text("🃏 View TCG Frame", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18150D)),
                            shape = RoundedCornerShape(3.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("Close", color = Color(0xFFFFFAD6), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            // TCG PHYSICAL CARD FRAME VIEW
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF141724),
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .border(2.dp, MillenniumGold, RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header: Card Name & Attribute Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = card.name,
                            color = MillenniumGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(card.attribute.colorHex),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = card.attribute.displayName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Full Card Texture
                    val officialFull = com.yugioh.recomp.core.rom.rememberCardArtwork(card, isFullCard = true)
                    val fullCardBitmap = officialFull ?: BitmapUtils.spriteToImageBitmap(RomAssetCache.getCardFullTexture(card.id))

                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(218.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.5.dp, MillenniumGold, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = fullCardBitmap,
                            contentDescription = card.name,
                            filterQuality = FilterQuality.None,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showTcgFrame = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = MillenniumGold)
                        ) {
                            Text("🎮 GBC Retro View", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = MillenniumGold),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Close", color = Color(0xFF141724), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
