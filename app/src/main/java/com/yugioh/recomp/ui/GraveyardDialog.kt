package com.yugioh.recomp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun GraveyardDialog(
    title: String,
    cards: List<Card>,
    onCardClick: (Card) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF141724),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.7f)
                .border(2.dp, MillenniumGold, RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = MillenniumGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${cards.size} Cards",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFF262C42))
                Spacer(modifier = Modifier.height(10.dp))

                if (cards.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Graveyard is empty.",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(cards) { card ->
                            GraveyardCardRow(card = card, onClick = { onCardClick(card) })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MillenniumGold),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Text("Close", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GraveyardCardRow(
    card: Card,
    onClick: () -> Unit
) {
    val fullCardBitmap = BitmapUtils.spriteToImageBitmap(RomAssetCache.getCardFullTexture(card.id))

    Surface(
        color = Color(0xFF0F121C),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF262C42), RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                bitmap = fullCardBitmap,
                contentDescription = card.name,
                modifier = Modifier
                    .width(36.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (card.isMonster) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "★".repeat(card.level),
                            color = Color(0xFFFFD700),
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ATK ${card.atk} / DEF ${card.def}",
                            color = Color(0xFFEF4444),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "[${card.cardType.displayName}]",
                        color = Color(card.attribute.colorHex),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
