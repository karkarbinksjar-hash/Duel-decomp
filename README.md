# Duel-decomp (Native Android Engine)

A native Android, portrait-oriented Yu-Gi-Oh! card game engine inspired by the architecture of [`bryanthaboi/gen1recomp`](https://github.com/bryanthaboi/gen1recomp).

---

## Architecture & Legal Compliance

The engine **does not bundle or ship with copyrighted game assets, ROM dumps, or proprietary image files**. It is strictly a clean-room decompiled native runner for users to play their legally obtained cartridge dumps. The engine dynamically extracts, parses, and rebuilds game graphics, audio, card data, and text at runtime from user-supplied official Game Boy Advance (GBA) and Game Boy Color (GBC) Yu-Gi-Oh! cartridges.

### Base Target & Checksum Verification
- **GBA Primary Target**: *Yu-Gi-Oh! The Eternal Duelist Soul* (USA)
  - Canonical SHA-1: `510fbba212aca9bab95ea12f8fd933e62ee34dea`
  - Internal Cartridge Header: `AGB-AYGE`
- **GBC Entry Point**: *Yu-Gi-Oh! Dark Duel Stories* (USA)
  - Canonical SHA-1: `1952d343bdfb5625bd23a8569cde6fd61a911502`
  - CRC-32: `803a56ae`
  - Internal Cartridge Header: `YU-GI-OH! DDS`
- **Strict Anti-Bypass Architecture**: Gameplay is strictly locked until a genuine, untrimmed cartridge dump matching canonical SHA-1 checksums is provided via the native file selector. Zero synthetic test dumps or bypass buttons are exposed in production builds.

---

## Key Features

1. **Consistent 100% Full-Resolution Card Artwork Pipeline (`CardArtworkLoader`)**:
   - Zero copyrighted art files packaged into the binary release.
   - At runtime, authentic Kazuki Takahashi card artwork scans (matching physical TCG card scans, including *Armored Lizard* `15480588`, *Petit Dragon* `75356564`, *Call of the Haunted* `97077563`, *Big Insect*, *Battle Ox*, etc.) are streamed dynamically on-demand using canonical 8-digit passcodes.
   - **Leading-Zero & Dual-ID Resilient Streaming**: Seamlessly resolves numeric CDN IDs stripped of leading zeros (e.g. `5053103`, `4031928`) alongside canonical Konami 8-digit codes with automatic API fallback and alias normalization.
   - **Preloading & Two-Tier Caching**: Preloads all duel cards on launch in background coroutines with in-memory caching and persistent local disk caching (`cacheDir/card_art/`) so cards render instantly offline.
   - **Modern Card Detail Inspection Modal**: Full high-resolution card view by default with stats scoreboard, lore box, and retro GBC mode switcher.

2. **Physical Card Movement & Fluid Battle Animations**:
   - **Attacking Monster Lunge Animation**: Monsters physically spring out of their field zones and lunge towards the opponent or target card (`translationY = ±32dp`, scaling `1.15f`), impact with electric clash rays and screen shake, and bounce smoothly back into position.
   - **Summoning Spring Bounce**: Summoned monsters pop into their zone with spring-based scaling and glowing Millennium summon rings.
   - **Card Destruction Dissolve**: Defeated monsters scale down, spin (`35°`), and fade into crimson particle bursts.
   - **Hand Card Elevation & Glow**: Selected hand cards smoothly raise up (`-16dp`) with drop shadow elevation and a pulsing Millennium Gold halo border.

3. **Damage Step Rules, Flip Mechanics & Smart AI Battle Targeting (v2.3.0)**:
   - **Damage Step Face-Down Monster Flip**: In accordance with official Yu-Gi-Oh! (TCG/OCG, EDS, and DDS) battle rules, when a monster in `FACE_DOWN_DEFENSE` is attacked, it is immediately flipped face-up into `FACE_UP_DEFENSE` in the Damage Step before damage calculation.
   - **Surviving Defense Walls**: If the defender's DEF exceeds or equals the attacker's ATK, the monster survives and **remains face-up on the field** in `FACE_UP_DEFENSE`, displaying its full artwork, card name, and DEF stat. Attacking players or AI take reflective battle damage.
   - **Flip Effect Engine**: Monsters flipped face-up in battle or via Flip Summon trigger authentic Flip Effects:
     - **Man-Eater Bug**: Automatically destroys the attacking monster (or an opponent monster on Flip Summon).
     - **Needle Worm**: Forces the opponent to discard the top 5 cards of their deck to the graveyard.
     - **Morphing Jar**: Forces both players to discard their hands and draw 5 cards.
   - **Intelligent Enemy AI Target Evaluation**:
     - The AI dynamically evaluates all potential targets with weighted heuristics: prioritizing known vulnerable attack monsters (`10000 + diff`) and known defense monsters with lower DEF (`5000 + diff`).
     - AI **never** attacks a known face-up defense monster whose DEF is greater than or equal to its attacker's ATK, eliminating self-inflicted suicide bounce damage.
     - Probing attacks on unknown face-down monsters require high ATK ($\ge 1400$) and sufficient LP ($> 1200$).
     - During multi-monster AI battle phase simulations, surviving defense monsters are immediately updated to `FACE_UP_DEFENSE` on the virtual board so subsequent AI monsters on the same turn never attack the revealed wall.
   - **Opponent Face-Down Privacy**: Face-down opponent cards cannot be inspected until legitimately revealed or flipped face-up in combat.

4. **Authentic *The Eternal Duelist Soul* (EDS) Starting Decks & 5-Tier Campaign Ladder**:
   - **Choice of 3 Canonical Starting Decks**:
     - **Black Deck** (Default): Earth & Dark beatdown featuring **Armored Lizard** (`15480588`), *Summoned Skull*, *Sangan*, *Bean Soldier*, *Raigeki*, *Pot of Greed*, *Change of Heart*, *Mirror Force*, and *Trap Hole*.
     - **Red Deck**: Fiery offensive deck featuring *Axe Raider*, *7 Colored Fish*, *Harpie Lady*, *Dark Hole*, *Pot of Greed*, *Snatch Steal*, and *Swords of Revealing Light*.
     - **Green Deck**: Heavy warrior deck featuring *Judge Man*, *Battle Ox*, *Armored Lizard*, *Premature Burial*, *Ring of Destruction*, and *Magic Jammer*.
   - **5-Tier Battle City Tournament Opponent Ladder**:
     - **Tier 1**: Tristan Taylor, Tea Gardner, Yugi Muto, Joey Wheeler, Bakura Ryou (Unlocked by default).
     - **Tier 2**: Espa Roba, Mako Tsunami, Rex Raptor, Weevil Underwood, Mai Valentine (Unlocked after 2 wins each or 10 total wins).
     - **Tier 3**: Lumis and Umbra, Arkana, Rare Hunter, Strings, Marik Ishtar (Unlocked after 3 wins each or 25 total wins).
     - **Tier 4**: Yami Bakura, Ishizu Ishtar, Seto Kaiba, Shadi, Yami Yugi (Unlocked after 4 wins each or 45 total wins).
     - **Tier 5**: Maximillion Pegasus, Simon Muran, Duel Computer, Solomon Muto (Unlocked after 5 wins each or 70 total wins).
   - **Interactive Campaign Hub**: Starting deck switcher buttons, Tier filter tabs (`All`, `T1`, `T2`, `T3`, `T4`, `T5`) with lock indicators (`🔒`), win counters, and opponent profiles.

5. **Intelligent, Aggressive Combat AI Engine**:
   - **Simulated Turn Planning**: The AI evaluates board states after planned Normal and Tribute Summons, ensuring newly summoned monsters (e.g. *Armored Lizard*, *Jinzo*, *Vorse Raider*) actively declare attacks.
   - **Competitive Attack Targeting**:
     - Prioritizes destroying Attack position monsters with lower ATK and inflicting battle damage to the player.
     - Destroys Defense position monsters whose DEF is lower than the attacker's ATK.
     - Attacks face-down Defense position monsters to clear opponent back lines.
     - Trades evenly matched monsters (equal ATK) when holding high-stat attackers ($\ge 1400$ ATK).
     - Declares Direct Attacks whenever the player's monster field is clear.
     - Avoids reckless suicide attacks against stronger cards.
   - **Paced Step-by-Step Sequencer**: Turn progression is visibly observable with banner announcements, phase transitions, attack laser beams, screen shake, clash viewports, and floating damage numbers.

6. **Authentic Dark Duel Stories (GBC) Deck & Campaign Mechanics**:
   - **Canonical 40-Card Starter Deck**: Strictly composed of cards with Deck Cost $\le 15$.
   - **Deck Capacity & Duelist Level Progression**:
     - Player starts at **Duelist Level 15** and **Deck Capacity 400 DC**.
     - Monster Deck Cost: $\frac{\text{ATK} + \text{DEF}}{100}$.
     - Winning duels awards **+3 Deck Capacity** and cards won from opponent drop pools added directly to Trunk.
   - **GBC Duel Rules**: 5-card hand limit, no-tribute normal summons bounded by DC, elemental alignment dominance wheel (*Shadow* $\rightarrow$ *Light* $\rightarrow$ *Fiend* $\rightarrow$ *Dreams* $\rightarrow$ *Pyro* $\rightarrow$ *Forest* $\rightarrow$ *Wind* $\rightarrow$ *Earth* $\rightarrow$ *Thunder* $\rightarrow$ *Aqua* $\rightarrow$ *Pyro*).

7. **High-Definition Visual Presentation & Modernized Neon Arena**:
   - **144×208 Ultra-Sharp Resolution**: Official 59:86 Yu-Gi-Oh! card ratio with realistic 3D bevels, brushed metal nameplates, and drop shadow borders.
   - **Millennium Eye Card Back**: Deep swirled mahogany/chocolate background with dual concentric gold rings and Millennium Eye emblem.
   - **3D Glassmorphic Attribute Orbs & Star Level Rows**: Glassmorphic attribute gems with specular glints and facet star borders.
   - **Holographic Prismatic Shimmer**: Prismatic foil shimmer overlay across Ultra Rare cards.
   - **Dynamic Duel Animation Suite**: Golden Millennium summoning portal, clash impact bursts with screen shake, shattered card destruction fragments, and animated rolling Life Points counter.
   - **CRT/LCD Dot-Matrix Shader**: High-performance optional scanline overlay with toggle in the Campaign Hub.

8. **Portrait Architecture & Mobile UX**:
   - Locked to **Portrait Mode**.
   - **Top Section**: Opponent playfield (Monster zones, Spell/Trap zones, facedown hand cards, animated Life Points, floating damage indicators, graveyard counter).
   - **Middle Section**: Active duel stage featuring phase ribbons (`DP`, `SP`, `M1`, `BP`, `M2`, `EP`), combat ticker, attack rays, and clash impact viewports.
   - **Bottom Section**: Player playfield, persistent selected-card HUD bar, interactive hand carousel, and action buttons.

9. **CI/CD, Permanent Developer Keystore & In-Place Updates**:
   - **Fixed Package ID**: Application ID strictly locked to `com.yugioh.recomp`.
   - **Permanent Developer Keystore Signing**: Every release is signed with the permanent repository release keystore (`ANDROID_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) stored in GitHub Actions secrets and `.android/duel_decomp_release.keystore`, guaranteeing cryptographic certificate continuity so every subsequent release installs seamlessly as an in-place update without package conflict errors.
   - **Automated GitHub Release Pipeline**: Automated tag trigger (`v*.*.*`) compiling, running unit tests, assembling, signing, verifying with `apksigner`, and publishing signed APK assets.
   - Automated Release workflow (`.github/workflows/release.yml`) triggered on version tags (`v*.*.*`), building, signing, and publishing official APK releases.

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
