# Duel-decomp (Native Android Engine)

A native Android, portrait-oriented Yu-Gi-Oh! card game engine inspired by the architecture of [`bryanthaboi/gen1recomp`](https://github.com/bryanthaboi/gen1recomp).

---

## Architecture & Legal Compliance

The engine **does not bundle or ship with copyrighted game assets or full ROMs**. It is strictly a clean-room decompiled native runner for users to play their legally obtained cartridge dumps. The engine dynamically extracts, parses, and rebuilds game graphics, audio, card data, and text at runtime from user-supplied official Game Boy Color (GBC) and Game Boy Advance (GBA) Yu-Gi-Oh! cartridges.

### Base Target & Checksum Verification
- **Primary GBC Entry Point**: *Yu-Gi-Oh! Dark Duel Stories* (USA)
  - Canonical SHA-1: `1952d343bdfb5625bd23a8569cde6fd61a911502`
  - CRC-32: `803a56ae`
  - Internal Cartridge Header: `YU-GI-OH! DDS`
- **GBA Expansion Architecture**: Modular support for *Yu-Gi-Oh! The Eternal Duelist Soul* (`510fbba212aca9bab95ea12f8fd933e62ee34dea`) and *Yu-Gi-Oh! Worldwide Edition* (`1e52d66f5f8a7c4f207d80ba4fccf55e`).

---

## Key Features

1. **Strict Clean-Room ROM Ingestion Pipeline**:
   - `DdsRomParser`: Parses Nintendo Game Boy Color cartridge headers directly from ROM streams.
   - `RomValidator`: Cryptographic SHA-1 and CRC-32 validation against canonical No-Intro cartridge headers. Gameplay remains strictly locked until an official cartridge dump is validated.
   - `GbcTileDecoder`: Decodes 2bpp Game Boy Color planar graphics and 15-bit BGR color palettes (`0b0BBBBBGGGGGRRRRR`) into Android native textures.
   - `CardArtRenderer`: Generates authentic 4-color Game Boy Color elemental palettes and iconic retro pixel-art sprites, eliminating procedural diagonal slash-line placeholders.
   - `RomAssetCache`: Dynamic in-memory texture cache populated exclusively upon verified ROM load.

2. **Authentic Dark Duel Stories (GBC) Deck & Campaign Mechanics**:
   - **Canonical 40-Card Starter Deck**: Authentic starter card pool strictly composed of cards with Deck Cost $\le 15$ (e.g. *Shadow Specter*, *Mystery Hand*, *Masked Clown*, *Mech Mole Zombie*, *Frog The Jam*, *Sinister Serpent*, *Droll Bird*, *Kaminarikozou*, *Air Marmot*, *Yamatano Dragon Scroll*).
   - **Deck Capacity & Duelist Level Progression**:
     - Player starts at **Duelist Level 15** and **Deck Capacity 400 DC**.
     - Monster Deck Cost: $\frac{\text{ATK} + \text{DEF}}{100}$.
     - Duelist Level Formula: $\text{Level} = \left\lfloor \frac{\text{Deck Capacity} - 400}{3} \right\rfloor + 15$.
     - Total deck cost must not exceed current Deck Capacity, and cards with individual cost exceeding Duelist Level cannot be used.
     - Winning duels awards **+3 Deck Capacity** and a card won from the opponent's drop pool added directly to the Trunk Chest (+2 DC on defeat).
   - **Deck Construction Screen**: Dedicated 40-card active deck vs Trunk inventory chest manager with live Deck Capacity meter, card inspection modal, and DC validation before launching duels.
   - **Stage 1 Domino Town Campaign Hub**: Authentic campaign map featuring Tristan Taylor, Joey Wheeler, Mai Valentine, Bakura, and Seto Kaiba with win counters, duelist profile previews, and drop reward tracking.

3. **Authentic GBC Duel Engine Rules**:
   - **5-Card Hand Draw Limit**: Strictly adheres to the DDS maximum hand capacity of 5 cards; draw is skipped when hand is full.
   - **No Tribute Normal Summons**: High-level monsters (Level 5-8) can be summoned directly to the field without sacrificing tributes, bounded by Deck Capacity and Duelist Level.
   - **On-the-Field DDS Fusions**: Directly dragging or playing compatible monsters onto occupied field slots triggers authentic GBC fusions (e.g., Dragon + Thunder = *Twin-Headed Thunder Dragon*, Dragon + Zombie = *Zombie Dragon*, Time Wizard + Baby Dragon = *Thousand Dragon*).
   - **Elemental Alignment Dominance Wheel**:
     $$\text{Shadow/Dark} \rightarrow \text{Light} \rightarrow \text{Fiend} \rightarrow \text{Dreams} \rightarrow \text{Pyro} \rightarrow \text{Forest} \rightarrow \text{Wind} \rightarrow \text{Earth} \rightarrow \text{Thunder} \rightarrow \text{Aqua} \rightarrow \text{Pyro}$$
     Monsters with an elemental advantage immediately destroy the opponent regardless of raw ATK/DEF.
   - **Counter Traps & Spells**: Implemented *Mirror Force*, *Trap Hole*, *Waboku*, *Raigeki*, *Dark Hole*, *Pot of Greed*, and *Dian Keto*.

4. **Game Boy Color LCD Screen Shader Overlay**:
   - High-performance `GbcShaderOverlay` simulating authentic Game Boy Color TFT dot-matrix LCD subpixel grid lines and scanlines over the touchscreen.
   - Toggleable on/off instantly via the `[📺 GBC]` button in the Campaign Hub and Duel header.

5. **Mobile UX & Portrait Architecture**:
   - Strictly locked to **Portrait Mode**.
   - **Top Section**: Opponent playfield (Monster zones, Spell/Trap zones, facedown hand cards, animated Life Points, floating damage indicators, and graveyard counter).
   - **Middle Section**: Active duel stage featuring phase ribbons (`DP`, `SP`, `M1`, `BP`, `M2`, `EP`), AI step-by-step announcement overlay, energy attack travel rays, clash impact bursts, and prominent direct attack prompts.
   - **Bottom Section**: Player playfield, persistent selected-card HUD bar, interactive horizontal hand carousel with visible card titles and stats badges, context-sensitive action controls, and graveyard counter.

6. **Paced AI Turn Sequencer**:
   - Step-by-step asynchronous turn execution with visible delays, banner announcements, and audio cues for Draw, Spell activations, Trap sets, Summons, Attack declarations, and Turn End transitions.

7. **Game Boy APU Chiptune Synthesizer**:
   - Pure Kotlin sound generator recreating Game Boy sound channels:
     - Pulse 1 & Pulse 2 (variable duty cycles and pitch sweeps)
     - Channel 4 LFSR pseudo-random noise generator
   - Authentic chiptune audio synthesized for card draws, golden portal summons, attack clashes, damage ticks, card destructions, direct blasts, and victory fanfares.

8. **Dynamic Duel Animation Suite**:
   - **Golden Millennium Summoning Portal**: Animated golden Millennium rune seal with rotating glyphs, ascending light beams, and bounce-in card scaling.
   - **Attack Clash & Screen Shake**: Dynamic electric lightning beam connecting combatants, clashing swords emblem, expanding shockwave ring, and multi-frame arena screen shake.
   - **Explosive Card Destruction**: Shattered card fragments and expanding fiery shockwaves on monster destruction.
   - **Rolling Life Points Counter**: Smooth decrement steps with pulsing crimson text and rapid chiptune damage ticks.
   - **Direct Attack Blasts**: Golden beam strikes directly against the opponent's LP header.

9. **High-Definition Card Presentation & Modernized Neon Arena (v1.9.0)**:
   - **144x208 Ultra-Sharp Resolution**: Official 59:86 Yu-Gi-Oh! card ratio with realistic 3D bevels, rounded corner contours, and drop shadow edges.
   - **Authentic Millennium Eye Card Back**: Deep swirled mahogany/chocolate background with radial vortex lines, dual concentric gold rings, and detailed golden Millennium Eye emblem.
   - **3D Glassmorphic Attribute Orbs & Ruby Level Stars**: Attribute gems with golden bezels, specular highlights, and dimensional ruby stars with facet borders.
   - **Integrated 3x5 Bitmap Typography Engine**: Renders card names, type lines, and metallic `ATK/DEF` scoreboard ribbons directly into card textures.
   - **Ultra Rare Holographic Prismatic Shimmer**: Dynamic rainbow spectral shimmer foil overlay across iconic and high-level card illustrations.
   - **Modernized Duel Arena Aesthetics**: Glassmorphic zone pedestals with glowing neon borders, animated LED rolling LP counters with color gradient health gauges, soaring vertical golden summon beam pillars, explosive card destruction bursts, and spell/trap runic activation glows.

10. **CI/CD & Automated Release Pipeline**:
   - Application ID restored to `com.yugioh.recomp` to ensure in-place updates over previous versions without creating duplicate apps.
   - Full Gradle Version Catalog (`gradle/libs.versions.toml`).
   - GitHub Actions CI workflow (`.github/workflows/ci.yml`) running `./gradlew test` and `./gradlew assembleRelease` on every Pull Request and push to `main`.
   - Automated Release workflow (`.github/workflows/release.yml`) triggered on version tags (`v*.*.*`), building, signing, and attaching official `Duel-decomp-v*.*.*.apk` releases.

---

## Building & Testing

### Prerequisites
- JDK 17 or JDK 21
- Android SDK 34 (Build Tools 34.0.0)

### Run Unit Tests
```bash
./gradlew test --no-daemon
```

### Build Signed Release APK
```bash
./gradlew assembleRelease --no-daemon
```
The output APK is generated at `app/build/outputs/apk/release/Duel-decomp-release.apk`.
