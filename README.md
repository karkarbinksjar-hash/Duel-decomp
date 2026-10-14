# Duel-decomp (Native Android Engine)

A native Android, portrait-oriented Yu-Gi-Oh! card game engine inspired by the architecture of [`bryanthaboi/gen1recomp`](https://github.com/bryanthaboi/gen1recomp).

---

## Architecture & Legal Compliance

The engine **does not ship with copyrighted game assets or full ROMs**. Instead, it dynamically extracts, parses, and rebuilds game graphics, audio, card data, and text at runtime from user-supplied official Game Boy Color (GBC) and Game Boy Advance (GBA) Yu-Gi-Oh! ROM cartridges.

### Base Target & Checksum Verification
- **Primary GBC Entry Point**: *Yu-Gi-Oh! Dark Duel Stories* (USA)
  - Canonical SHA-1: `1952d343bdfb5625bd23a8569cde6fd61a911502`
  - CRC-32: `803a56ae`
  - Internal Cartridge Header: `YU-GI-OH! DDS`
- **GBA Expansion Architecture**: Ready for *Yu-Gi-Oh! The Eternal Duelist Soul* (`510fbba212aca9bab95ea12f8fd933e62ee34dea`) and *Yu-Gi-Oh! Worldwide Edition* (`1e52d66f5f8a7c4f207d80ba4fccf55e`).

---

## Key Features

1. **Clean ROM Ingestion Pipeline**:
   - `RomValidator`: Cryptographic SHA-1 and CRC-32 validation against canonical No-Intro cartridge headers.
   - `GbcTileDecoder`: Decodes 2bpp Game Boy Color planar graphics and 15-bit BGR color palettes into Android native textures.
   - `GbaTileDecoder`: Modular 4bpp/8bpp tile decoder for Game Boy Advance card artwork expansions.
   - `CardDataExtractor`: Rebuilds 800-card statistical tables (ATK/DEF, Level, Attributes, Alignment, Lore).
   - `RomAssetCache`: In-memory and disk texture cache for extracted assets.

2. **Mobile UX & Portrait Architecture**:
   - Strictly locked to **Portrait Mode**.
   - **Top Section**: Opponent playfield (Monster zones, Spell/Trap zones, animated Life Points counter).
   - **Middle Section**: Active duel stage featuring phase ribbons (`DP`, `SP`, `M1`, `BP`, `M2`, `EP`), clash animations, battle result popups, and real-time combat ticker.
   - **Bottom Section**: Player playfield, interactive horizontal hand carousel, and action controls (Summon, Set DEF, Activate Spell, Set Trap, Declare Attack, End Turn).

3. **Game Boy APU Chiptune Synthesizer**:
   - Pure Kotlin sound generator recreating the 4 Game Boy sound channels:
     - Pulse 1 & Pulse 2 (variable duty cycles and pitch sweeps)
     - Channel 4 LFSR pseudo-random noise generator
   - Authentic chiptune audio synthesized for card draws, monster summons, attack clashes, and victory fanfares.

4. **CI/CD & Release Automation**:
   - Full Gradle Version Catalog (`gradle/libs.versions.toml`).
   - GitHub Actions CI workflow (`.github/workflows/ci.yml`) running `./gradlew test` and `./gradlew assembleRelease` on every Pull Request.
   - Automated Release workflow (`.github/workflows/release.yml`) triggered on version tags (`v1.0.0`), supporting repository signing keys (`ANDROID_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) and automatic GitHub Release drafting.

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
