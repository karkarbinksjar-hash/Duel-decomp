# Duel-decomp (Native Android Engine)

A native Android, portrait-oriented Yu-Gi-Oh! card game engine inspired by the architecture of [`bryanthaboi/gen1recomp`](https://github.com/bryanthaboi/gen1recomp).

---

## Architecture & Legal Compliance

The engine **does not ship with copyrighted game assets or full ROMs**. It is strictly a clean-room decompiled native runner for users to play their legally obtained cartridge dumps. The engine dynamically extracts, parses, and rebuilds game graphics, audio, card data, and text at runtime from user-supplied official Game Boy Color (GBC) and Game Boy Advance (GBA) Yu-Gi-Oh! cartridges.

### Base Target & Checksum Verification
- **Primary GBC Entry Point**: *Yu-Gi-Oh! Dark Duel Stories* (USA)
  - Canonical SHA-1: `1952d343bdfb5625bd23a8569cde6fd61a911502`
  - CRC-32: `803a56ae`
  - Internal Cartridge Header: `YU-GI-OH! DDS`
- **GBA Expansion Architecture**: Ready for *Yu-Gi-Oh! The Eternal Duelist Soul* (`510fbba212aca9bab95ea12f8fd933e62ee34dea`) and *Yu-Gi-Oh! Worldwide Edition* (`1e52d66f5f8a7c4f207d80ba4fccf55e`).

---

## Key Features

1. **Strict Clean-Room ROM Ingestion Pipeline**:
   - `DdsRomParser`: Parses Nintendo Game Boy Color cartridge headers (Title, MBC5 controller, CGB mode flag, ROM/RAM size, checksums) directly from ROM streams.
   - `RomValidator`: Cryptographic SHA-1 and CRC-32 validation against canonical No-Intro cartridge headers. Gameplay remains strictly locked until an official cartridge dump is validated.
   - `GbcTileDecoder`: Decodes 2bpp Game Boy Color planar graphics and 15-bit BGR color palettes (`0b0BBBBBGGGGGRRRRR`) into Android native textures.
   - `CardArtRenderer`: Renders authentic Yu-Gi-Oh! card frames (tan for Normal Monsters, amber-orange for Effect Monsters, teal for Spells, magenta for Traps), attribute gems, star counters, and framed illustrations without brown static noise.
   - `CardDataExtractor`: Rebuilds 800-card statistical tables (ATK/DEF, Level, Attributes, Alignment, 8-digit Passcodes, Lore).
   - `RomAssetCache`: Dynamic in-memory texture cache populated exclusively upon verified ROM load.

2. **Mobile UX & Portrait Architecture**:
   - Strictly locked to **Portrait Mode**.
   - **Top Section**: Opponent playfield (Monster zones, Spell/Trap zones, facedown hand cards, animated Life Points bar, floating damage indicators, and clickable Graveyard counter).
   - **Middle Section**: Active duel stage featuring phase ribbons (`DP`, `SP`, `M1`, `BP`, `M2`, `EP`), AI step-by-step announcement overlay, energy attack travel rays, clash impact bursts, phase transition banners, and high-visibility direct attack prompts.
   - **Bottom Section**: Player playfield, persistent selected-card HUD bar (name, attribute gem, stars, monster type, ATK/DEF scores, and lore quick-view), interactive horizontal hand carousel with visible card titles and stats badges, context-sensitive action controls, and Graveyard counter.

3. **Step-by-Step Opponent Action Sequencer**:
   - Asynchronous coroutine-driven AI turn sequencer that paces opponent actions at human-readable speeds.
   - Live announcement banner and audio cues for every AI step: Draw Phase, Spell Activation (with card preview), Trap Sets, Normal Summons, Attack Declarations (with target callouts), and Turn End transitions.

4. **Authentic Dark Duel Stories Rules & Traps**:
   - **DDS Elemental Alignment Dominance Wheel**:
     $$\text{Shadow/Dark} \rightarrow \text{Light} \rightarrow \text{Fiend} \rightarrow \text{Dreams} \rightarrow \text{Pyro} \rightarrow \text{Forest} \rightarrow \text{Wind} \rightarrow \text{Earth} \rightarrow \text{Thunder} \rightarrow \text{Aqua} \rightarrow \text{Pyro}$$
     Monsters with an elemental advantage automatically overcome raw ATK/DEF statistics.
   - **Counter Traps**: Implemented *Mirror Force* (destroys attacking monsters on declaration) and *Trap Hole* (destroys monsters with $\ge 1000$ ATK upon summon).
   - **Card Passcodes**: `CardPasscodeRegistry` supporting canonical 8-digit DDS passcodes.

5. **Game Boy APU Chiptune Synthesizer**:
   - Pure Kotlin sound generator recreating the 4 Game Boy sound channels:
     - Pulse 1 & Pulse 2 (variable duty cycles and pitch sweeps)
     - Channel 4 LFSR pseudo-random noise generator
   - Authentic chiptune audio synthesized for card draws, monster summons, attack clashes, and victory fanfares.

6. **CI/CD & Release Automation**:
   - Full Gradle Version Catalog (`gradle/libs.versions.toml`).
   - GitHub Actions CI workflow (`.github/workflows/ci.yml`) running `./gradlew test` and `./gradlew assembleRelease` on every Pull Request and push to `main`.
   - Automated Release workflow (`.github/workflows/release.yml`) triggered on version tags (`v*.*.*`), building and signing APK binaries with Android SDK `apksigner`.

---

## Building & Testing

### Prerequisites
- JDK 17 or JDK 21
- Android SDK 34 (Build Tools 34.0.0)

### Run Unit Tests
```bash
./gradlew test
```

### Build Signed Release APK
```bash
./gradlew assembleRelease
```
The output APK is generated at `app/build/outputs/apk/release/app-release.apk`.
