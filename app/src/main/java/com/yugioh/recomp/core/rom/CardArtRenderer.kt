package com.yugioh.recomp.core.rom

import com.yugioh.recomp.core.engine.*
import kotlin.math.*

/**
 * High-fidelity pixel-art renderer and integer upscaler for Duel-decomp.
 * Provides authentic TCG card frames, Millennium card back textures, and elemental
 * crest fallbacks. Authentic card artwork is fetched dynamically at runtime via
 * CardArtworkLoader without bundling any copyrighted ROM graphics.
 */
object CardArtRenderer {

    // Authentic Yu-Gi-Oh! Card Frame Colors
    private const val COLOR_FRAME_NORMAL_MONSTER = 0xFFC98A44.toInt() // Classic Ochre Yellow
    private const val COLOR_FRAME_EFFECT_MONSTER = 0xFFB85924.toInt() // Rich Burnt Amber
    private const val COLOR_FRAME_SPELL = 0xFF167E6B.toInt()          // Deep Jade Green
    private const val COLOR_FRAME_TRAP = 0xFF991E60.toInt()           // Crimson Magenta

    private const val COLOR_CARD_BORDER_OUTER = 0xFF0F0B08.toInt()
    private const val COLOR_CARD_BORDER_INNER = 0xFF543D28.toInt()
    private const val COLOR_TEXT_BOX_BG = 0xFFEBD3B0.toInt()
    private const val COLOR_SPELL_TEXT_BG = 0xFFC2E4DD.toInt()
    private const val COLOR_TRAP_TEXT_BG = 0xFFE9C5D8.toInt()

    private const val COLOR_STAR_GOLD = 0xFFFFD700.toInt()
    private const val COLOR_STAR_RED = 0xFFDC2626.toInt()

    private val ULTRA_RARE_CARDS = setOf(
        "Blue-Eyes White Dragon", "Dark Magician", "Red-Eyes B. Dragon",
        "Summoned Skull", "Jinzo", "Buster Blader", "Exodia the Forbidden One",
        "Right Arm of the Forbidden One", "Left Arm of the Forbidden One",
        "Right Leg of the Forbidden One", "Left Leg of the Forbidden One",
        "Raigeki", "Mirror Force", "Monster Reborn", "Magic Cylinder",
        "Change of Heart", "Dark Hole", "Harpie's Feather Duster",
        "Barrel Dragon", "Relinquished", "Gearfried the Iron Knight",
        "Trap Hole", "Swords of Revealing Light"
    )

    /**
     * Integer pixel art upscaler: scales a sprite by an integer factor [scale]
     * using nearest-neighbor replication to preserve razor-sharp retro pixel edges.
     */
    fun upscaleSprite(pixels: IntArray, srcWidth: Int, srcHeight: Int, scale: Int = 3): DecodedSprite {
        val targetWidth = srcWidth * scale
        val targetHeight = srcHeight * scale
        val scaled = IntArray(targetWidth * targetHeight)

        for (y in 0 until srcHeight) {
            val srcRowOffset = y * srcWidth
            val targetRowBase = y * scale * targetWidth
            for (x in 0 until srcWidth) {
                val color = pixels[srcRowOffset + x]
                for (dy in 0 until scale) {
                    val row = targetRowBase + (dy * targetWidth)
                    for (dx in 0 until scale) {
                        scaled[row + (x * scale) + dx] = color
                    }
                }
            }
        }
        return DecodedSprite(targetWidth, targetHeight, scaled)
    }

    /**
     * Renders a complete Yu-Gi-Oh! card texture including border, name header,
     * attribute orb, star row, framed illustration window, text lore box, and ATK/DEF scores.
     * High-definition standard resolution: 144x208 (official 59:86 aspect ratio).
     */
    fun renderFullCard(
        card: Card,
        width: Int = 144,
        height: Int = 208,
        artPixelsOverride: IntArray? = null
    ): IntArray {
        val pixels = IntArray(width * height)

        val frameColor = when {
            card.isSpell -> COLOR_FRAME_SPELL
            card.isTrap -> COLOR_FRAME_TRAP
            card.level >= 5 || card.name.contains("Dragon") || card.name.contains("Magician") || card.name.contains("Skull") || card.name.contains("Jinzo") || card.name.contains("Blader") -> COLOR_FRAME_EFFECT_MONSTER
            else -> COLOR_FRAME_NORMAL_MONSTER
        }

        val textBoxBg = when {
            card.isSpell -> COLOR_SPELL_TEXT_BG
            card.isTrap -> COLOR_TRAP_TEXT_BG
            else -> COLOR_TEXT_BOX_BG
        }

        // 1. Fill Card Base with subtle textured gradient
        for (y in 0 until height) {
            val vGrad = ((y.toFloat() / height) * 22).toInt() - 11
            val baseR = (((frameColor shr 16) and 0xFF) + vGrad).coerceIn(0, 255)
            val baseG = (((frameColor shr 8) and 0xFF) + vGrad).coerceIn(0, 255)
            val baseB = ((frameColor and 0xFF) + vGrad).coerceIn(0, 255)
            val rowColor = (0xFF shl 24) or (baseR shl 16) or (baseG shl 8) or baseB

            for (x in 0 until width) {
                pixels[y * width + x] = rowColor
            }
        }

        // 2. Realistic 3D Outer Border & Bevel with rounded corners
        val borderWidth = (width * 0.035f).toInt().coerceAtLeast(4)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val isCorner = (x < 3 && y < 3) || (x >= width - 3 && y < 3) ||
                               (x < 3 && y >= height - 3) || (x >= width - 3 && y >= height - 3)
                if (isCorner) {
                    pixels[y * width + x] = 0x00000000
                    continue
                }

                if (x < borderWidth || x >= width - borderWidth || y < borderWidth || y >= height - borderWidth) {
                    if (y < borderWidth || x < borderWidth) {
                        pixels[y * width + x] = COLOR_CARD_BORDER_INNER
                    } else {
                        pixels[y * width + x] = COLOR_CARD_BORDER_OUTER
                    }
                } else if (x == borderWidth || x == width - borderWidth - 1 || y == borderWidth || y == height - borderWidth - 1) {
                    pixels[y * width + x] = 0xFF2A1C12.toInt()
                }
            }
        }

        // 3. Name Header Bar (Top)
        val nameBarTop = (height * 0.045f).toInt()
        val nameBarHeight = (height * 0.088f).toInt()
        val nameBarLeft = borderWidth + 4
        val nameBarRight = width - borderWidth - 4
        val nameBarWidth = nameBarRight - nameBarLeft

        // Brushed metal background for name plate
        for (ny in nameBarTop until (nameBarTop + nameBarHeight)) {
            val shade = 0x1E + ((ny - nameBarTop) * 2)
            val plateColor = (0xFF shl 24) or (shade shl 16) or ((shade + 4) shl 8) or (shade + 12)
            for (nx in nameBarLeft until nameBarRight) {
                pixels[ny * width + nx] = plateColor
            }
        }
        drawRectBorder(pixels, width, nameBarLeft, nameBarTop, nameBarWidth, nameBarHeight, 0xFF4B5563.toInt())
        drawRectBorder(pixels, width, nameBarLeft + 1, nameBarTop + 1, nameBarWidth - 2, nameBarHeight - 2, 0xFF222938.toInt())

        // Render Card Name in Name Bar
        val nameText = if (card.name.length > 20) card.name.take(19) + "." else card.name
        val nameColor = if (card.level >= 7 || card.name in ULTRA_RARE_CARDS) 0xFFFDE047.toInt() else 0xFFFFFFFF.toInt()
        val nameY = nameBarTop + (nameBarHeight - 5) / 2
        drawText3x5(pixels, width, nameBarLeft + 4, nameY, nameText, nameColor, 0xFF000000.toInt())

        // 3D Glassmorphic Attribute Orb (Top-Right of Name Bar)
        val orbRadius = (nameBarHeight / 2) - 1
        val orbCenterX = nameBarRight - orbRadius - 2
        val orbCenterY = nameBarTop + (nameBarHeight / 2)
        val attrColor = getAttributeColor(card.attribute)

        // Outer golden bezel ring
        drawCircle(pixels, width, orbCenterX, orbCenterY, orbRadius + 1, 0xFFD4AF37.toInt())
        // Main attribute jewel base
        drawCircle(pixels, width, orbCenterX, orbCenterY, orbRadius, attrColor)
        // Inner depth gradient
        drawCircle(pixels, width, orbCenterX, orbCenterY, orbRadius - 2, brighten(attrColor, 35))
        // Specular highlight glint
        putPixel(pixels, width, height, orbCenterX - (orbRadius / 2), orbCenterY - (orbRadius / 2), 0xFFFFFFFF.toInt())
        putPixel(pixels, width, height, orbCenterX - (orbRadius / 2) + 1, orbCenterY - (orbRadius / 2), 0xFFFFFFFF.toInt())

        // 4. Star Level Row (For Monsters)
        val starRowY = nameBarTop + nameBarHeight + 3
        if (card.isMonster && card.level > 0) {
            val starCount = min(12, card.level)
            val starSize = 7
            val starSpacing = 9
            val totalStarsWidth = (starCount * starSpacing) - 2
            val starsStartX = nameBarRight - totalStarsWidth

            for (s in 0 until starCount) {
                val sx = starsStartX + (s * starSpacing) + (starSize / 2)
                val sy = starRowY + (starSize / 2)
                drawStar(pixels, width, sx, sy, 3, COLOR_STAR_GOLD, COLOR_STAR_RED)
            }
        }

        // 5. Framed Illustration Window
        val artTop = if (card.isMonster) starRowY + 11 else nameBarTop + nameBarHeight + 6
        val artLeft = borderWidth + 4
        val artWidth = width - (artLeft * 2)
        val artHeight = (height * 0.44f).toInt()

        // Double beveled border for art window
        drawRectBorder(pixels, width, artLeft - 2, artTop - 2, artWidth + 4, artHeight + 4, 0xFF3D2614.toInt())
        drawRectBorder(pixels, width, artLeft - 1, artTop - 1, artWidth + 2, artHeight + 2, 0xFF7A5835.toInt())

        // Render Card Illustration into Art Window with aspect-preserving sampling
        val baseSpritePixels = artPixelsOverride ?: renderCardArtwork(card, 64, 64)
        for (ay in 0 until artHeight) {
            val srcY = ((ay.toFloat() / artHeight) * 64).toInt().coerceIn(0, 63)
            for (ax in 0 until artWidth) {
                val srcX = ((ax.toFloat() / artWidth) * 64).toInt().coerceIn(0, 63)
                val srcColor = baseSpritePixels[srcY * 64 + srcX]
                val px = artLeft + ax
                val py = artTop + ay
                if (px in 0 until width && py in 0 until height) {
                    pixels[py * width + px] = srcColor
                }
            }
        }

        // Holographic Shimmer / Prismatic Rainbow Sheen for Ultra Rare Cards
        val isUltraRare = card.level >= 7 || card.name in ULTRA_RARE_CARDS
        if (isUltraRare) {
            for (ay in 0 until artHeight) {
                for (ax in 0 until artWidth) {
                    val px = artLeft + ax
                    val py = artTop + ay
                    val diag = (ax + ay) % 20
                    if (diag in 0..3) {
                        val sheen = (20 - (abs(diag - 1.5f) * 10)).toInt()
                        val orig = pixels[py * width + px]
                        val r = (((orig shr 16) and 0xFF) + sheen).coerceIn(0, 255)
                        val g = (((orig shr 8) and 0xFF) + (sheen * 0.8f).toInt()).coerceIn(0, 255)
                        val b = (((orig) and 0xFF) + (sheen * 1.3f).toInt()).coerceIn(0, 255)
                        pixels[py * width + px] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                    }
                }
            }
        }

        // 6. Text / Lore Box (Parchment/Spell/Trap)
        val textTop = artTop + artHeight + 4
        val textLeft = borderWidth + 4
        val textWidth = width - (textLeft * 2)
        val textHeight = height - textTop - borderWidth - 4

        // Ancient papyrus texture fill
        for (ty in textTop until (textTop + textHeight)) {
            val vNoise = (((ty * 13) xor (textWidth * 7)) % 9) - 4
            val baseR = (((textBoxBg shr 16) and 0xFF) + vNoise).coerceIn(0, 255)
            val baseG = (((textBoxBg shr 8) and 0xFF) + vNoise).coerceIn(0, 255)
            val baseB = ((textBoxBg and 0xFF) + vNoise).coerceIn(0, 255)
            val rowCol = (0xFF shl 24) or (baseR shl 16) or (baseG shl 8) or baseB
            for (tx in textLeft until (textLeft + textWidth)) {
                pixels[ty * width + tx] = rowCol
            }
        }
        drawRectBorder(pixels, width, textLeft, textTop, textWidth, textHeight, 0xFF4A3525.toInt())
        drawRectBorder(pixels, width, textLeft + 1, textTop + 1, textWidth - 2, textHeight - 2, 0xFF8B6B4C.toInt())

        // Lore Box Header Tag (Monster Type or Card Type)
        val typeTag = when {
            card.isMonster -> {
                val subtype = if (card.level >= 5 || card.name in ULTRA_RARE_CARDS) "EFFECT" else "NORMAL"
                "[ ${card.monsterType.displayName.uppercase()} / $subtype ]"
            }
            card.isSpell -> "[ SPELL CARD ]"
            card.isTrap -> "[ TRAP CARD ]"
            else -> "[ CARD ]"
        }
        drawText3x5(pixels, width, textLeft + 4, textTop + 4, typeTag, 0xFF1C130B.toInt())

        // Micro-script lore lines
        val scriptLineY = textTop + 12
        val maxLines = if (card.isMonster) 3 else 5
        val descWords = if (card.description.isNotBlank()) card.description.split(" ") else listOf(card.name)
        var curWordIdx = 0
        for (line in 0 until maxLines) {
            val ly = scriptLineY + (line * 7)
            if (ly >= textTop + textHeight - 14) break
            var lineStr = ""
            while (curWordIdx < descWords.size && (lineStr.length + descWords[curWordIdx].length + 1) < 26) {
                lineStr += (if (lineStr.isEmpty()) "" else " ") + descWords[curWordIdx]
                curWordIdx++
            }
            if (lineStr.isNotEmpty()) {
                drawText3x5(pixels, width, textLeft + 4, ly, lineStr, 0xFF2A1C12.toInt())
            }
        }

        // 7. ATK / DEF Scoreboard Plate for Monsters
        if (card.isMonster) {
            val scorePlateH = 10
            val scorePlateW = (textWidth * 0.72f).toInt().coerceAtLeast(70)
            val scorePlateX = textLeft + textWidth - scorePlateW - 2
            val scorePlateY = textTop + textHeight - scorePlateH - 2

            drawRect(pixels, width, scorePlateX, scorePlateY, scorePlateW, scorePlateH, 0xFF181B24.toInt())
            drawRectBorder(pixels, width, scorePlateX, scorePlateY, scorePlateW, scorePlateH, 0xFF6B7280.toInt())

            val statStr = "ATK/${card.atk} DEF/${card.def}"
            drawText3x5(pixels, width, scorePlateX + 3, scorePlateY + 3, statStr, 0xFFFFFFFF.toInt(), 0xFF000000.toInt())
        }

        // 8. Hologram Foil Square Stamp (Bottom-Right)
        val holoSize = 8
        val holoX = width - borderWidth - 9
        val holoY = height - borderWidth - 9
        drawRect(pixels, width, holoX, holoY, holoSize, holoSize, 0xFFF59E0B.toInt())
        drawRectBorder(pixels, width, holoX, holoY, holoSize, holoSize, 0xFFD97706.toInt())
        drawCircle(pixels, width, holoX + 4, holoY + 4, 2, 0xFFFFFFFF.toInt())
        putPixel(pixels, width, height, holoX + 4, holoY + 4, 0xFF78350F.toInt())

        return pixels
    }

    /**
     * Renders a high-resolution Game Boy Advance (16-color) card texture with enhanced
     * metallic borders, smooth gradient text boxes, and vibrant GBA color depth.
     */
    fun renderGbaFullCard(
        card: Card,
        width: Int = 144,
        height: Int = 208,
        artPixelsOverride: IntArray? = null
    ): IntArray {
        val pixels = renderFullCard(card, width, height, artPixelsOverride)
        val goldHighlight = 0xFFFDE047.toInt()
        val gbaTrim = 0xFFD4AF37.toInt()
        val borderWidth = (width * 0.035f).toInt().coerceAtLeast(4)

        // Gold foil corner highlights
        for (i in 0..4) {
            putPixel(pixels, width, height, borderWidth + 1 + i, borderWidth + 1, goldHighlight)
            putPixel(pixels, width, height, width - borderWidth - 2 - i, borderWidth + 1, goldHighlight)
            putPixel(pixels, width, height, borderWidth + 1 + i, height - borderWidth - 2, goldHighlight)
            putPixel(pixels, width, height, width - borderWidth - 2 - i, height - borderWidth - 2, goldHighlight)
        }

        // Top & bottom metallic accent lines
        for (x in (borderWidth + 6) until (width - borderWidth - 6)) {
            val c = if (x % 3 == 0) goldHighlight else gbaTrim
            pixels[(borderWidth + 1) * width + x] = c
            pixels[(height - borderWidth - 2) * width + x] = gbaTrim
        }
        return pixels
    }

    /**
     * Authentic Yu-Gi-Oh! Card Back texture with Millennium Eye emblem.
     */
    fun renderCardBack(width: Int = 144, height: Int = 208): IntArray {
        val pixels = IntArray(width * height)
        val outerBorder = 0xFF140D07.toInt()
        val brownBase = 0xFF5C2B14.toInt()
        val brownDark = 0xFF3D1A0B.toInt()
        val goldRing = 0xFFD4AF37.toInt()
        val goldBright = 0xFFFDE047.toInt()
        val eyeOrange = 0xFFF97316.toInt()
        val blackCore = 0xFF0A0704.toInt()

        // 1. Swirled Mahogany base texture
        val cx = width / 2.0
        val cy = height / 2.0
        val radiusX = width * 0.42
        val radiusY = height * 0.42

        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = (x - cx) / radiusX
                val dy = (y - cy) / radiusY
                val dist = sqrt(dx * dx + dy * dy)
                if (dist <= 1.0) {
                    val angle = atan2(dy, dx)
                    val swirl = sin(angle * 6.0 + dist * 8.0)
                    val baseCol = if (swirl > 0.1) brownBase else brownDark
                    pixels[y * width + x] = baseCol
                } else {
                    pixels[y * width + x] = outerBorder
                }
            }
        }

        // 2. Beveled Dark Border Frame
        val bWidth = (width * 0.05f).toInt()
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (x < bWidth || x >= width - bWidth || y < bWidth || y >= height - bWidth) {
                    pixels[y * width + x] = outerBorder
                }
            }
        }

        // 3. Central Cosmic Swirl & Dual Concentric Rings
        val outerRingR = (min(width, height) * 0.35).toInt()
        val innerRingR = (min(width, height) * 0.28).toInt()
        val coreVoidR = (min(width, height) * 0.20).toInt()

        drawCircle(pixels, width, cx.toInt(), cy.toInt(), outerRingR, 0xFF140C07.toInt())
        drawCircle(pixels, width, cx.toInt(), cy.toInt(), coreVoidR, blackCore)

        drawCircleBorder(pixels, width, cx.toInt(), cy.toInt(), outerRingR, goldRing)
        drawCircleBorder(pixels, width, cx.toInt(), cy.toInt(), outerRingR - 1, goldBright)
        drawCircleBorder(pixels, width, cx.toInt(), cy.toInt(), innerRingR, goldRing)
        drawCircleBorder(pixels, width, cx.toInt(), cy.toInt(), coreVoidR, 0xFF78350F.toInt())

        // 4. Authentic Millennium Eye Emblem in Center
        val icx = cx.toInt()
        val icy = cy.toInt()
        val eyeW = (min(width, height) * 0.12).toInt().coerceAtLeast(10)

        drawCircle(pixels, width, icx, icy, eyeW, goldRing)
        drawCircle(pixels, width, icx, icy, eyeW - 2, blackCore)
        drawCircle(pixels, width, icx, icy, (eyeW * 0.6).toInt(), eyeOrange)
        drawCircle(pixels, width, icx, icy, (eyeW * 0.3).toInt(), 0xFF000000.toInt())
        putPixel(pixels, width, height, icx - 1, icy - 1, 0xFFFFFFFF.toInt())

        drawLine(pixels, width, icx - eyeW, icy, icx - eyeW - 3, icy - 2, goldRing)
        drawLine(pixels, width, icx + eyeW, icy, icx + eyeW + 3, icy - 2, goldRing)
        drawLine(pixels, width, icx, icy + eyeW, icx, icy + eyeW + 4, goldRing)

        return pixels
    }

    /**
     * Crisp, elegant elemental crest illustration renderer used as an offline/runtime fallback.
     * Displays clean attribute runes and sigils with authentic palette depth.
     */
    fun renderCardArtwork(card: Card, width: Int = 64, height: Int = 64): IntArray {
        val pixels = IntArray(width * height)

        val palette = getGbcPalette(card.attribute)
        val colorDark = palette[0]
        val colorMid1 = palette[1]
        val colorMid2 = palette[2]
        val colorLight = palette[3]

        // 1. Fill background with dark attribute tone
        pixels.fill(colorDark)

        val cx = width / 2
        val cy = height / 2

        // Outer elemental disc
        drawCircle(pixels, width, cx, cy, min(cx, cy) - 4, colorMid1)
        drawCircle(pixels, width, cx, cy, min(cx, cy) - 8, colorDark)
        drawCircleBorder(pixels, width, cx, cy, min(cx, cy) - 4, colorLight)
        drawCircleBorder(pixels, width, cx, cy, min(cx, cy) - 8, colorMid2)

        // Attribute Crest Sigil
        val r = (min(width, height) * 0.28f).toInt()
        when (card.attribute) {
            CardAttribute.LIGHT -> {
                // Solar Starburst
                drawCircle(pixels, width, cx, cy, r / 2, colorLight)
                for (angle in 0 until 8) {
                    val rad = angle * (Math.PI / 4.0)
                    val x1 = (cx + cos(rad) * (r * 0.6)).toInt()
                    val y1 = (cy + sin(rad) * (r * 0.6)).toInt()
                    val x2 = (cx + cos(rad) * r).toInt()
                    val y2 = (cy + sin(rad) * r).toInt()
                    drawLine(pixels, width, x1, y1, x2, y2, colorLight)
                }
            }
            CardAttribute.DARK -> {
                // Crescent Moon & Void Rune
                drawCircle(pixels, width, cx, cy, r, colorLight)
                drawCircle(pixels, width, cx + (r * 0.45f).toInt(), cy - (r * 0.25f).toInt(), (r * 0.85f).toInt(), colorDark)
                drawCircle(pixels, width, cx - 2, cy + 2, (r * 0.25f).toInt(), colorMid2)
            }
            CardAttribute.FIRE -> {
                // Flame Crest
                drawTriangle(pixels, width, cx, cy - r, cx - r + 3, cy + r - 2, cx + r - 3, cy + r - 2, colorMid2)
                drawTriangle(pixels, width, cx, cy - (r * 0.7f).toInt(), cx - (r * 0.5f).toInt(), cy + r - 4, cx + (r * 0.5f).toInt(), cy + r - 4, colorLight)
                drawCircle(pixels, width, cx, cy + (r * 0.3f).toInt(), (r * 0.35f).toInt(), colorLight)
            }
            CardAttribute.WATER -> {
                // Tidal Wave Droplet
                drawCircle(pixels, width, cx, cy + 4, r - 4, colorLight)
                drawTriangle(pixels, width, cx, cy - r, cx - (r - 4), cy + 4, cx + (r - 4), cy + 4, colorLight)
                drawCircle(pixels, width, cx, cy + 5, (r * 0.45f).toInt(), colorMid1)
            }
            CardAttribute.EARTH -> {
                // Granite Geode Crystal
                drawRect(pixels, width, cx - r + 4, cy - r + 4, (r * 2) - 8, (r * 2) - 8, colorMid2)
                drawRectBorder(pixels, width, cx - r + 4, cy - r + 4, (r * 2) - 8, (r * 2) - 8, colorLight)
                drawLine(pixels, width, cx - r + 4, cy, cx + r - 4, cy, colorLight)
                drawLine(pixels, width, cx, cy - r + 4, cx, cy + r - 4, colorLight)
            }
            CardAttribute.WIND -> {
                // Gale Vortex Spiral
                drawCircleBorder(pixels, width, cx, cy, r, colorLight)
                drawCircleBorder(pixels, width, cx + 2, cy - 2, (r * 0.65f).toInt(), colorMid2)
                drawCircleBorder(pixels, width, cx - 2, cy + 2, (r * 0.35f).toInt(), colorLight)
            }
            CardAttribute.SPELL -> {
                // Spell Book / Arcane Eye
                drawRect(pixels, width, cx - r + 2, cy - (r * 0.7f).toInt(), (r * 2) - 4, (r * 1.4f).toInt(), colorMid2)
                drawRectBorder(pixels, width, cx - r + 2, cy - (r * 0.7f).toInt(), (r * 2) - 4, (r * 1.4f).toInt(), colorLight)
                drawLine(pixels, width, cx, cy - (r * 0.7f).toInt(), cx, cy + (r * 0.7f).toInt(), colorLight)
                drawCircle(pixels, width, cx, cy, (r * 0.3f).toInt(), colorLight)
            }
            CardAttribute.TRAP -> {
                // Trap Spikes Diamond
                drawTriangle(pixels, width, cx, cy - r, cx - r, cy, cx + r, cy, colorLight)
                drawTriangle(pixels, width, cx, cy + r, cx - r, cy, cx + r, cy, colorMid2)
                drawCircle(pixels, width, cx, cy, (r * 0.35f).toInt(), 0xFFFFFFFF.toInt())
            }
            else -> {
                // Universal Star
                drawStar(pixels, width, cx, cy, r, colorLight, colorMid2)
            }
        }

        return pixels
    }

    private fun getGbcPalette(attr: CardAttribute): IntArray {
        return when (attr) {
            CardAttribute.LIGHT -> intArrayOf(0xFF181512.toInt(), 0xFF785B28.toInt(), 0xFFD4AF37.toInt(), 0xFFFFFDD0.toInt())
            CardAttribute.DARK -> intArrayOf(0xFF0F0B18.toInt(), 0xFF3B1E54.toInt(), 0xFF7E22CE.toInt(), 0xFFE9D5FF.toInt())
            CardAttribute.FIRE -> intArrayOf(0xFF1A0A0A.toInt(), 0xFF7F1D1D.toInt(), 0xFFDC2626.toInt(), 0xFFFEF08A.toInt())
            CardAttribute.WATER -> intArrayOf(0xFF061528.toInt(), 0xFF0C4A6E.toInt(), 0xFF0284C7.toInt(), 0xFFBAE6FD.toInt())
            CardAttribute.EARTH -> intArrayOf(0xFF140D07.toInt(), 0xFF54381E.toInt(), 0xFFB45309.toInt(), 0xFFFDE68A.toInt())
            CardAttribute.WIND -> intArrayOf(0xFF081814.toInt(), 0xFF065F46.toInt(), 0xFF10B981.toInt(), 0xFFA7F3D0.toInt())
            CardAttribute.THUNDER -> intArrayOf(0xFF1A1706.toInt(), 0xFF785B12.toInt(), 0xFFEAB308.toInt(), 0xFFFEF9C3.toInt())
            CardAttribute.FOREST -> intArrayOf(0xFF051A10.toInt(), 0xFF064E3B.toInt(), 0xFF059669.toInt(), 0xFFBBF7D0.toInt())
            CardAttribute.FIEND -> intArrayOf(0xFF16061E.toInt(), 0xFF581C87.toInt(), 0xFF9333EA.toInt(), 0xFFF3E8FF.toInt())
            CardAttribute.DREAMS -> intArrayOf(0xFF1C0A18.toInt(), 0xFF831843.toInt(), 0xFFDB2777.toInt(), 0xFFFCE7F3.toInt())
            CardAttribute.SPELL -> intArrayOf(0xFF061A18.toInt(), 0xFF0F766E.toInt(), 0xFF14B8A6.toInt(), 0xFFCCFBF1.toInt())
            CardAttribute.TRAP -> intArrayOf(0xFF1A0818.toInt(), 0xFF831843.toInt(), 0xFFBE185D.toInt(), 0xFFFCE7F3.toInt())
            else -> intArrayOf(0xFF111827.toInt(), 0xFF374151.toInt(), 0xFF9CA3AF.toInt(), 0xFFF9FAFB.toInt())
        }
    }

    private fun getAttributeColor(attr: CardAttribute): Int {
        return when (attr) {
            CardAttribute.LIGHT -> 0xFFFACC15.toInt()
            CardAttribute.DARK -> 0xFF7C3AED.toInt()
            CardAttribute.FIRE -> 0xFFEF4444.toInt()
            CardAttribute.WATER -> 0xFF0284C7.toInt()
            CardAttribute.EARTH -> 0xFFB45309.toInt()
            CardAttribute.WIND -> 0xFF10B981.toInt()
            CardAttribute.THUNDER -> 0xFFEAB308.toInt()
            CardAttribute.FOREST -> 0xFF059669.toInt()
            CardAttribute.FIEND -> 0xFF9333EA.toInt()
            CardAttribute.DREAMS -> 0xFFEC4899.toInt()
            CardAttribute.DIVINE -> 0xFFF59E0B.toInt()
            CardAttribute.SPELL -> 0xFF0D9488.toInt()
            CardAttribute.TRAP -> 0xFFBE185D.toInt()
        }
    }

    private fun drawRect(pixels: IntArray, w: Int, x: Int, y: Int, rw: Int, rh: Int, color: Int) {
        val x0 = max(0, x)
        val y0 = max(0, y)
        val x1 = min(w, x + rw)
        val y1 = min(pixels.size / w, y + rh)

        for (py in y0 until y1) {
            val rowOffset = py * w
            for (px in x0 until x1) {
                pixels[rowOffset + px] = color
            }
        }
    }

    private fun drawRectBorder(pixels: IntArray, w: Int, x: Int, y: Int, rw: Int, rh: Int, color: Int) {
        drawLine(pixels, w, x, y, x + rw - 1, y, color)
        drawLine(pixels, w, x, y + rh - 1, x + rw - 1, y + rh - 1, color)
        drawLine(pixels, w, x, y, x, y + rh - 1, color)
        drawLine(pixels, w, x + rw - 1, y, x + rw - 1, y + rh - 1, color)
    }

    private fun drawLine(pixels: IntArray, w: Int, x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
        val h = pixels.size / w
        var cx0 = x0
        var cy0 = y0
        val dx = abs(x1 - cx0)
        val dy = abs(y1 - cy0)
        val sx = if (cx0 < x1) 1 else -1
        val sy = if (cy0 < y1) 1 else -1
        var err = dx - dy

        while (true) {
            if (cx0 in 0 until w && cy0 in 0 until h) {
                pixels[cy0 * w + cx0] = color
            }
            if (cx0 == x1 && cy0 == y1) break
            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                cx0 += sx
            }
            if (e2 < dx) {
                err += dx
                cy0 += sy
            }
        }
    }

    private fun drawCircle(pixels: IntArray, w: Int, cx: Int, cy: Int, r: Int, color: Int) {
        val h = pixels.size / w
        val r2 = r * r
        val y0 = max(0, cy - r)
        val y1 = min(h - 1, cy + r)

        for (y in y0..y1) {
            val dy = y - cy
            val dxMax = sqrt((r2 - dy * dy).toDouble()).toInt()
            val x0 = max(0, cx - dxMax)
            val x1 = min(w - 1, cx + dxMax)
            val rowOffset = y * w
            for (x in x0..x1) {
                pixels[rowOffset + x] = color
            }
        }
    }

    private fun drawCircleBorder(pixels: IntArray, w: Int, cx: Int, cy: Int, r: Int, color: Int) {
        val h = pixels.size / w
        var x = r
        var y = 0
        var err = 0

        while (x >= y) {
            putPixel(pixels, w, h, cx + x, cy + y, color)
            putPixel(pixels, w, h, cx + y, cy + x, color)
            putPixel(pixels, w, h, cx - y, cy + x, color)
            putPixel(pixels, w, h, cx - x, cy + y, color)
            putPixel(pixels, w, h, cx - x, cy - y, color)
            putPixel(pixels, w, h, cx - y, cy - x, color)
            putPixel(pixels, w, h, cx + y, cy - x, color)
            putPixel(pixels, w, h, cx + x, cy - y, color)

            y += 1
            err += 1 + 2 * y
            if (2 * (err - x) + 1 > 0) {
                x -= 1
                err += 1 - 2 * x
            }
        }
    }

    private fun drawTriangle(pixels: IntArray, w: Int, x1: Int, y1: Int, x2: Int, y2: Int, x3: Int, y3: Int, color: Int) {
        val h = pixels.size / w
        val minX = max(0, minOf(x1, x2, x3))
        val maxX = min(w - 1, maxOf(x1, x2, x3))
        val minY = max(0, minOf(y1, y2, y3))
        val maxY = min(h - 1, maxOf(y1, y2, y3))

        val d12 = (y2 - y1)
        val d23 = (y3 - y2)
        val d31 = (y1 - y3)

        val dx12 = (x1 - x2)
        val dx23 = (x2 - x3)
        val dx31 = (x3 - x1)

        for (y in minY..maxY) {
            val rowOffset = y * w
            for (x in minX..maxX) {
                val w1 = d23 * (x - x3) + dx23 * (y - y3)
                val w2 = d31 * (x - x1) + dx31 * (y - y1)
                val w3 = d12 * (x - x2) + dx12 * (y - y2)

                if ((w1 >= 0 && w2 >= 0 && w3 >= 0) || (w1 <= 0 && w2 <= 0 && w3 <= 0)) {
                    pixels[rowOffset + x] = color
                }
            }
        }
    }

    private fun drawStar(pixels: IntArray, w: Int, cx: Int, cy: Int, r: Int, outerColor: Int, innerColor: Int) {
        val points = 5
        val innerR = r * 0.45
        for (i in 0 until points) {
            val ang1 = (i * 2 * Math.PI / points) - (Math.PI / 2.0)
            val ang2 = ((i + 0.5) * 2 * Math.PI / points) - (Math.PI / 2.0)
            val ang3 = ((i + 1) * 2 * Math.PI / points) - (Math.PI / 2.0)

            val x1 = (cx + cos(ang1) * r).toInt()
            val y1 = (cy + sin(ang1) * r).toInt()
            val x2 = (cx + cos(ang2) * innerR).toInt()
            val y2 = (cy + sin(ang2) * innerR).toInt()
            val x3 = (cx + cos(ang3) * r).toInt()
            val y3 = (cy + sin(ang3) * r).toInt()

            drawTriangle(pixels, w, cx, cy, x1, y1, x2, y2, outerColor)
            drawTriangle(pixels, w, cx, cy, x2, y2, x3, y3, innerColor)
        }
    }

    private fun putPixel(pixels: IntArray, w: Int, h: Int, x: Int, y: Int, color: Int) {
        if (x in 0 until w && y in 0 until h) {
            pixels[y * w + x] = color
        }
    }

    private fun brighten(color: Int, amount: Int): Int {
        val a = (color shr 24) and 0xFF
        val r = (((color shr 16) and 0xFF) + amount).coerceIn(0, 255)
        val g = (((color shr 8) and 0xFF) + amount).coerceIn(0, 255)
        val b = ((color and 0xFF) + amount).coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private val FONT_3X5: Map<Char, IntArray> = mapOf(
        '0' to intArrayOf(0b111, 0b101, 0b101, 0b101, 0b111),
        '1' to intArrayOf(0b010, 0b110, 0b010, 0b010, 0b111),
        '2' to intArrayOf(0b111, 0b001, 0b111, 0b100, 0b111),
        '3' to intArrayOf(0b111, 0b001, 0b111, 0b001, 0b111),
        '4' to intArrayOf(0b101, 0b101, 0b111, 0b001, 0b001),
        '5' to intArrayOf(0b111, 0b100, 0b111, 0b001, 0b111),
        '6' to intArrayOf(0b111, 0b100, 0b111, 0b101, 0b111),
        '7' to intArrayOf(0b111, 0b001, 0b010, 0b010, 0b010),
        '8' to intArrayOf(0b111, 0b101, 0b111, 0b101, 0b111),
        '9' to intArrayOf(0b111, 0b101, 0b111, 0b001, 0b111),
        'A' to intArrayOf(0b010, 0b101, 0b111, 0b101, 0b101),
        'B' to intArrayOf(0b110, 0b101, 0b110, 0b101, 0b110),
        'C' to intArrayOf(0b011, 0b100, 0b100, 0b100, 0b011),
        'D' to intArrayOf(0b110, 0b101, 0b101, 0b101, 0b110),
        'E' to intArrayOf(0b111, 0b100, 0b110, 0b100, 0b111),
        'F' to intArrayOf(0b111, 0b100, 0b110, 0b100, 0b100),
        'G' to intArrayOf(0b011, 0b100, 0b101, 0b101, 0b011),
        'H' to intArrayOf(0b101, 0b101, 0b111, 0b101, 0b101),
        'I' to intArrayOf(0b111, 0b010, 0b010, 0b010, 0b111),
        'J' to intArrayOf(0b001, 0b001, 0b001, 0b101, 0b010),
        'K' to intArrayOf(0b101, 0b110, 0b100, 0b110, 0b101),
        'L' to intArrayOf(0b100, 0b100, 0b100, 0b100, 0b111),
        'M' to intArrayOf(0b101, 0b111, 0b101, 0b101, 0b101),
        'N' to intArrayOf(0b110, 0b101, 0b101, 0b101, 0b101),
        'O' to intArrayOf(0b010, 0b101, 0b101, 0b101, 0b010),
        'P' to intArrayOf(0b110, 0b101, 0b110, 0b100, 0b100),
        'Q' to intArrayOf(0b010, 0b101, 0b101, 0b110, 0b011),
        'R' to intArrayOf(0b110, 0b101, 0b110, 0b101, 0b101),
        'S' to intArrayOf(0b011, 0b100, 0b010, 0b001, 0b110),
        'T' to intArrayOf(0b111, 0b010, 0b010, 0b010, 0b010),
        'U' to intArrayOf(0b101, 0b101, 0b101, 0b101, 0b111),
        'V' to intArrayOf(0b101, 0b101, 0b101, 0b101, 0b010),
        'W' to intArrayOf(0b101, 0b101, 0b101, 0b111, 0b101),
        'X' to intArrayOf(0b101, 0b101, 0b010, 0b101, 0b101),
        'Y' to intArrayOf(0b101, 0b101, 0b010, 0b010, 0b010),
        'Z' to intArrayOf(0b111, 0b001, 0b010, 0b100, 0b111),
        '/' to intArrayOf(0b001, 0b010, 0b010, 0b010, 0b100),
        '-' to intArrayOf(0b000, 0b000, 0b111, 0b000, 0b000),
        '[' to intArrayOf(0b110, 0b100, 0b100, 0b100, 0b110),
        ']' to intArrayOf(0b011, 0b001, 0b001, 0b001, 0b011),
        '.' to intArrayOf(0b000, 0b000, 0b000, 0b000, 0b010),
        ':' to intArrayOf(0b000, 0b010, 0b000, 0b010, 0b000),
        '\'' to intArrayOf(0b010, 0b010, 0b000, 0b000, 0b000),
        ' ' to intArrayOf(0b000, 0b000, 0b000, 0b000, 0b000)
    )

    fun drawText3x5(pixels: IntArray, w: Int, startX: Int, startY: Int, text: String, color: Int, shadowColor: Int? = null) {
        val h = pixels.size / w
        var curX = startX
        val upper = text.uppercase()
        for (ch in upper) {
            val glyph = FONT_3X5[ch] ?: FONT_3X5[' ']!!
            if (shadowColor != null) {
                for (r in 0 until 5) {
                    val bits = glyph[r]
                    for (c in 0 until 3) {
                        if ((bits and (1 shl (2 - c))) != 0) {
                            putPixel(pixels, w, h, curX + c + 1, startY + r + 1, shadowColor)
                        }
                    }
                }
            }
            for (r in 0 until 5) {
                val bits = glyph[r]
                for (c in 0 until 3) {
                    if ((bits and (1 shl (2 - c))) != 0) {
                        putPixel(pixels, w, h, curX + c, startY + r, color)
                    }
                }
            }
            curX += 4
        }
    }
}
