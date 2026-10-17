package com.yugioh.recomp.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugioh.recomp.core.audio.ChiptuneSynthesizer
import com.yugioh.recomp.core.engine.Card
import com.yugioh.recomp.core.engine.DdsCampaignManager
import com.yugioh.recomp.core.engine.GbaCampaignManager
import com.yugioh.recomp.core.rom.RomAssetCache
import com.yugioh.recomp.core.rom.RomPlatform
import com.yugioh.recomp.ui.theme.*

enum class DeckBuilderTab {
    ACTIVE_DECK,
    TRUNK
}

enum class CardTypeFilter(val label: String) {
    ALL("All"),
    MONSTER("Monsters"),
    SPELL("Spells"),
    TRAP("Traps")
}

enum class CardSortOption(val label: String) {
    ATK_DESC("ATK ↓"),
    DEF_DESC("DEF ↓"),
    LEVEL_DESC("★ Level ↓"),
    NAME_ASC("A-Z"),
    COST_DESC("Cost ↓")
}

@Composable
fun DeckConstructionScreen(
    onNavigateBack: () -> Unit
) {
    val isGba = RomAssetCache.activeTarget?.platform == RomPlatform.GBA

    var selectedTab by remember { mutableStateOf(DeckBuilderTab.ACTIVE_DECK) }
    var inspectingCard by remember { mutableStateOf<Card?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var updateCounter by remember { mutableStateOf(0) }
    fun refresh() { updateCounter++ }

    // Search, Filter & Sort states
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf(CardTypeFilter.ALL) }
    var selectedLevelFilter by remember { mutableStateOf(0) } // 0=All, 1=Lv1-4, 2=Lv5-6, 3=Lv7+
    var selectedSortOption by remember { mutableStateOf(CardSortOption.ATK_DESC) }

    var selectedRecipeSlot by remember { mutableStateOf(1) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importInputText by remember { mutableStateOf("") }

    val activeDeck = if (isGba) GbaCampaignManager.activeDeck else DdsCampaignManager.activeDeck
    val trunk = if (isGba) GbaCampaignManager.trunk else DdsCampaignManager.trunk
    val totalCost = if (isGba) 0 else DdsCampaignManager.totalDeckCost()
    val maxDc = if (isGba) 0 else DdsCampaignManager.deckCapacity
    val duelistLevel = if (isGba) 0 else DdsCampaignManager.duelistLevel
    val isDeckValid = if (isGba) activeDeck.size in 40..60 else (activeDeck.size == 40 && totalCost <= maxDc)

    val rawCards = if (selectedTab == DeckBuilderTab.ACTIVE_DECK) activeDeck else trunk

    val displayedCards = remember(rawCards, searchQuery, selectedTypeFilter, selectedLevelFilter, selectedSortOption, updateCounter) {
        rawCards
            .filter { card ->
                val matchesQuery = searchQuery.isBlank() || card.name.contains(searchQuery.trim(), ignoreCase = true)
                val matchesType = when (selectedTypeFilter) {
                    CardTypeFilter.ALL -> true
                    CardTypeFilter.MONSTER -> card.isMonster
                    CardTypeFilter.SPELL -> card.isSpell
                    CardTypeFilter.TRAP -> card.isTrap
                }
                val matchesLevel = when (selectedLevelFilter) {
                    1 -> card.isMonster && card.level in 1..4
                    2 -> card.isMonster && card.level in 5..6
                    3 -> card.isMonster && card.level >= 7
                    else -> true
                }
                matchesQuery && matchesType && matchesLevel
            }
            .let { list ->
                when (selectedSortOption) {
                    CardSortOption.ATK_DESC -> list.sortedWith(compareByDescending<Card> { it.atk }.thenBy { it.name })
                    CardSortOption.DEF_DESC -> list.sortedWith(compareByDescending<Card> { it.def }.thenBy { it.name })
                    CardSortOption.LEVEL_DESC -> list.sortedWith(compareByDescending<Card> { it.level }.thenByDescending { it.atk })
                    CardSortOption.NAME_ASC -> list.sortedBy { it.name }
                    CardSortOption.COST_DESC -> list.sortedWith(compareByDescending<Card> { it.deckCost }.thenBy { it.name })
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkFieldBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MillenniumGold)
                }
                Text(
                    text = if (isGba) "GBA DECK CONSTRUCTION" else "DECK CONSTRUCTION",
                    color = MillenniumGold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (isGba) "TCG EXPERT" else "LV $duelistLevel",
                        color = Color(0xFFFDE047),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Deck Capacity & Card Count Meter
            Surface(
                color = Color(0xFF111827),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isDeckValid) Color(0xFF374151) else AttackRed, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isGba) "Format: Battle City TCG" else "Deck Capacity: $totalCost / $maxDc DC",
                            color = if (isGba || totalCost <= maxDc) MillenniumGold else AttackRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Cards: ${activeDeck.size} / 40",
                            color = if (activeDeck.size in 40..60) LifePointGreen else AttackRed,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Breakdown counts
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Deck: ⚔ ${activeDeck.count { it.isMonster }} Mon | ❇ ${activeDeck.count { it.isSpell }} Spell | ⚠ ${activeDeck.count { it.isTrap }} Trap",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Trunk: ${trunk.size}",
                            color = Color(0xFFCBD5E1),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!isDeckValid) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (activeDeck.size < 40) {
                                "⚠ A Deck must contain at least 40 cards! (${40 - activeDeck.size} needed)"
                            } else if (activeDeck.size > 60) {
                                "⚠ A Deck cannot exceed 60 cards!"
                            } else {
                                "⚠ Total Cost exceeds Deck Capacity ($maxDc DC)!"
                            },
                            color = AttackRed,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isGba) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().border(0.5.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val activeRecipe = GbaCampaignManager.savedDeckRecipes.find { it.id == selectedRecipeSlot }
                            Text(
                                text = "RECIPES: ${activeRecipe?.name ?: "Slot $selectedRecipeSlot"}",
                                color = MillenniumGold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Button(
                                    onClick = {
                                        GbaCampaignManager.saveCurrentDeckToSlot(selectedRecipeSlot)
                                        statusMessage = "💾 Saved active deck to Slot $selectedRecipeSlot!"
                                        refresh()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.height(22.dp)
                                ) {
                                    Text("Save", fontSize = 8.5.sp, color = Color(0xFF93C5FD))
                                }
                                Button(
                                    onClick = {
                                        val ok = GbaCampaignManager.loadDeckFromSlot(selectedRecipeSlot)
                                        if (ok) {
                                            statusMessage = "📂 Loaded Slot $selectedRecipeSlot into active deck!"
                                        } else {
                                            statusMessage = "⚠ Slot $selectedRecipeSlot is empty! Save a deck first."
                                        }
                                        refresh()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.height(22.dp)
                                ) {
                                    Text("Load", fontSize = 8.5.sp, color = Color(0xFF86EFAC))
                                }
                                Button(
                                    onClick = { showExportDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.height(22.dp)
                                ) {
                                    Text("📤 Export", fontSize = 8.5.sp, color = Color(0xFFFDE047))
                                }
                                Button(
                                    onClick = { showImportDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.height(22.dp)
                                ) {
                                    Text("📥 Import", fontSize = 8.5.sp, color = Color(0xFFF472B6))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (slot in 1..5) {
                                val isSelected = selectedRecipeSlot == slot
                                Surface(
                                    color = if (isSelected) MillenniumGold else Color(0xFF1E293B),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedRecipeSlot = slot }
                                ) {
                                    Text(
                                        text = "Slot $slot",
                                        color = if (isSelected) Color.Black else Color(0xFF94A3B8),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Status flash message
            statusMessage?.let { msg ->
                Spacer(modifier = Modifier.height(3.dp))
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MillenniumGold, RoundedCornerShape(4.dp))
                ) {
                    Text(
                        text = msg,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Tab Selector: Active Deck vs Trunk
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TabButton(
                    title = "Active Deck (${activeDeck.size})",
                    isSelected = selectedTab == DeckBuilderTab.ACTIVE_DECK,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedTab = DeckBuilderTab.ACTIVE_DECK
                        statusMessage = null
                    }
                )
                TabButton(
                    title = "Trunk Chest (${trunk.size})",
                    isSelected = selectedTab == DeckBuilderTab.TRUNK,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedTab = DeckBuilderTab.TRUNK
                        statusMessage = null
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Search Bar
            Surface(
                color = Color(0xFF131826),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MillenniumGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search card name...", color = Color(0xFF64748B), fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f).height(44.dp)
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Filter Chips Row: Type & Level
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Type filters
                items(CardTypeFilter.values()) { type ->
                    val isSelected = selectedTypeFilter == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTypeFilter = type },
                        label = { Text(type.label, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MillenniumGold,
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF1E2433),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) MillenniumGold else Color(0xFF334155)
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }

                // Level filters (Monsters)
                item {
                    val isSelected = selectedLevelFilter == 1
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedLevelFilter = if (isSelected) 0 else 1 },
                        label = { Text("★ 1-4", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2563EB),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E2433),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }

                item {
                    val isSelected = selectedLevelFilter == 2
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedLevelFilter = if (isSelected) 0 else 2 },
                        label = { Text("★ 5-6 (1 Trib)", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF7C3AED),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E2433),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }

                item {
                    val isSelected = selectedLevelFilter == 3
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedLevelFilter = if (isSelected) 0 else 3 },
                        label = { Text("★ 7+ (2 Trib)", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFDC2626),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E2433),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Sort Options Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sort by:",
                    color = Color(0xFF64748B),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CardSortOption.values().forEach { opt ->
                        val isSelected = selectedSortOption == opt
                        Surface(
                            color = if (isSelected) Color(0xFF334155) else Color(0xFF141926),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .border(1.dp, if (isSelected) MillenniumGold else Color(0xFF252D3F), RoundedCornerShape(4.dp))
                                .clickable { selectedSortOption = opt }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = opt.label,
                                color = if (isSelected) MillenniumGold else Color(0xFF94A3B8),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Card Grid View
            if (displayedCards.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No cards matching the selected filters.",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(displayedCards) { card ->
                        DeckCardItem(
                            card = card,
                            duelistLevel = if (isGba) 999 else duelistLevel,
                            isFromDeck = selectedTab == DeckBuilderTab.ACTIVE_DECK,
                            onClick = {
                                if (selectedTab == DeckBuilderTab.ACTIVE_DECK) {
                                    val removed = if (isGba) GbaCampaignManager.removeCardFromDeck(card) else DdsCampaignManager.removeCardFromDeck(card)
                                    if (removed) {
                                        ChiptuneSynthesizer.playSfx(ChiptuneSynthesizer.createCardDrawSfx())
                                        statusMessage = "Moved [${card.name}] to Trunk."
                                        refresh()
                                    }
                                } else {
                                    if (isGba) {
                                        val added = GbaCampaignManager.addCardToDeck(card)
                                        if (added) {
                                            ChiptuneSynthesizer.playSfx(ChiptuneSynthesizer.createCardDrawSfx())
                                            statusMessage = "Added [${card.name}] to Deck."
                                            refresh()
                                        } else {
                                            statusMessage = "Cannot add more than 3 copies of [${card.name}] or exceed 60 cards!"
                                        }
                                    } else {
                                        val (canAdd, reason) = DdsCampaignManager.canAddCard(card)
                                        if (canAdd) {
                                            DdsCampaignManager.addCardToDeck(card)
                                            ChiptuneSynthesizer.playSfx(ChiptuneSynthesizer.createCardDrawSfx())
                                            statusMessage = "Added [${card.name}] to Deck."
                                            refresh()
                                        } else {
                                            statusMessage = reason
                                        }
                                    }
                                }
                            },
                            onLongClick = {
                                inspectingCard = card
                            }
                        )
                    }
                }
            }
        }

        // Optional GBC LCD Shader
        if (!isGba && DdsCampaignManager.isGbcLcdShaderEnabled) {
            GbcShaderOverlay(alpha = 0.18f)
        }

        // Card Detail Dialog
        inspectingCard?.let { card ->
            CardDetailDialog(card = card, onDismiss = { inspectingCard = null })
        }

        if (showExportDialog) {
            val passcodes = GbaCampaignManager.exportDeckToPasscodes()
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Export Deck Passcodes", color = MillenniumGold, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Copy these official 8-digit passcodes to share or backup your deck:", color = Color.White, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        SelectionContainer {
                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                            ) {
                                Text(
                                    text = passcodes,
                                    color = Color(0xFF86EFAC),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Close", color = MillenniumGold, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color(0xFF1A1C29)
            )
        }

        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("Import Deck Passcodes", color = MillenniumGold, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Paste comma-separated or space-separated 8-digit card passcodes:", color = Color.White, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = importInputText,
                            onValueChange = { importInputText = it },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            placeholder = { Text("e.g. 15480588, 89631139, 46986414...", fontSize = 10.sp, color = Color(0xFF64748B)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MillenniumGold,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val (count, msg) = GbaCampaignManager.importDeckFromPasscodes(importInputText)
                            statusMessage = msg
                            showImportDialog = false
                            importInputText = ""
                            refresh()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MillenniumGold)
                    ) {
                        Text("Import", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                },
                containerColor = Color(0xFF1A1C29)
            )
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MillenniumGold else Color(0xFF1E293B)
        ),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier.height(36.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.Black else Color(0xFF94A3B8),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeckCardItem(
    card: Card,
    duelistLevel: Int,
    isFromDeck: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isOverLevel = card.deckCost > duelistLevel
    val fullCardBitmap = com.yugioh.recomp.core.rom.rememberCardDisplayBitmap(card, isFullCard = true)

    Surface(
        color = Color(0xFF131722),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .border(
                width = 1.dp,
                color = if (isOverLevel) AttackRed else Color(0xFF334155),
                shape = RoundedCornerShape(6.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier.padding(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Card Thumbnail
            Image(
                bitmap = fullCardBitmap,
                contentDescription = card.name,
                modifier = Modifier
                    .width(46.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Card Name
            Text(
                text = card.name,
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // ATK/DEF or Spell/Trap label
            if (card.isMonster) {
                Text(
                    text = "${card.atk}/${card.def}",
                    color = MillenniumGold,
                    fontSize = 7.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = if (card.isSpell) "SPELL" else "TRAP",
                    color = if (card.isSpell) Color(0xFF34D399) else Color(0xFFF472B6),
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
