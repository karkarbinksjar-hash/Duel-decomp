package com.yugioh.recomp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.yugioh.recomp.core.engine.Card
import com.yugioh.recomp.core.rom.RomAssetCache
import com.yugioh.recomp.ui.theme.MillenniumGold

@Composable
fun CardDetailDialog(
    card: Card,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF141724),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .border(2.dp, MillenniumGold, RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
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
                            text = card.attribute.displayName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // High-Fidelity Yu-Gi-Oh! Full Card Texture
                val fullCardBitmap = BitmapUtils.spriteToImageBitmap(RomAssetCache.getCardFullTexture(card.id))

                Box(
                    modifier = Modifier
                        .width(148.dp)
                        .height(215.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.5.dp, MillenniumGold, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = fullCardBitmap,
                        contentDescription = card.name,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Level / Stars & Monster Type
                if (card.isMonster) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "★".repeat(card.level),
                            color = Color(0xFFFFD700),
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "[${card.monsterType.displayName}]",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ATK / DEF Badges
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F121C), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF262C42), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("ATK", color = Color(0xFFEF4444), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${card.atk}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
                        }
                        VerticalDivider(modifier = Modifier.height(20.dp), color = Color(0xFF262C42))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("DEF", color = Color(0xFF3B82F6), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${card.def}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                } else {
                    Surface(
                        color = Color(0xFF0F121C),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "[${card.cardType.displayName} Card]",
                            color = Color(card.attribute.colorHex),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Card Lore / Description Box
                Surface(
                    color = Color(0xFF0B0E17),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1E2638), RoundedCornerShape(6.dp))
                ) {
                    Text(
                        text = card.description.ifEmpty { "Official Dark Duel Stories card table data." },
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 8-Digit DDS Passcode
                card.password?.let { code ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DDS Passcode:",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                        Text(
                            text = code,
                            color = MillenniumGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MillenniumGold),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Text("Close", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
