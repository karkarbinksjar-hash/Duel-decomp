package com.yugioh.recomp.core.rom

import com.yugioh.recomp.core.engine.*
import kotlin.math.*

/**
 * High-fidelity Game Boy Color pixel-art renderer for cards in Duel-decomp.
 * Generates authentic 4-color GBC card frames, elemental glyphs, star ranks,
 * and recognizable retro sprite illustrations without procedural slash-line artifacts.
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

    /**
     * Renders a complete Yu-Gi-Oh! card texture including border, name header,
     * attribute orb, star row, framed illustration window, text lore box, and ATK/DEF scores.
     */
    fun renderFullCard(
        card: Card,
        width: Int = 128,
        height: Int = 186,
        artPixelsOverride: IntArray? = null
    ): IntArray {
        val pixels = IntArray(width * height)

        val frameColor = when {
            card.isSpell -> COLOR_FRAME_SPELL
            card.isTrap -> COLOR_FRAME_TRAP
            card.level >= 5 || card.name.contains("Dragon") || card.name.contains("Magician") || card.name.contains("Skull") -> COLOR_FRAME_EFFECT_MONSTER
            else -> COLOR_FRAME_NORMAL_MONSTER
        }

        val textBoxBg = when {
            card.isSpell -> COLOR_SPELL_TEXT_BG
            card.isTrap -> COLOR_TRAP_TEXT_BG
            else -> COLOR_TEXT_BOX_BG
        }

        // 1. Fill Card Base with subtle textured gradient
        for (y in 0 until height) {
            val vGrad = ((y.toFloat() / height) * 20).toInt() - 10
            val baseR = (((frameColor shr 16) and 0xFF) + vGrad).coerceIn(0, 255)
            val baseG = (((frameColor shr 8) and 0xFF) + vGrad).coerceIn(0, 255)
            val baseB = ((frameColor and 0xFF) + vGrad).coerceIn(0, 255)
            val rowColor = (0xFF shl 24) or (baseR shl 16) or (baseG shl 8) or baseB

            for (x in 0 until width) {
                pixels[y * width + x] = rowColor
            }
        }

        // 2. Outer Card Border
        val borderWidth = 4
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (x < borderWidth || x >= width - borderWidth || y < borderWidth || y >= height - borderWidth) {
                    pixels[y * width + x] = COLOR_CARD_BORDER_OUTER
                } else if (x == borderWidth || x == width - borderWidth - 1 || y == borderWidth || y == height - borderWidth - 1) {
                    pixels[y * width + x] = COLOR_CARD_BORDER_INNER
                }
            }
        }

        // 3. Name Header Bar (Top)
        val nameBarTop = 8
        val nameBarHeight = 16
        val nameBarLeft = 8
        val nameBarRight = width - 8
        drawRect(pixels, width, nameBarLeft, nameBarTop, nameBarRight - nameBarLeft, nameBarHeight, 0xFF1E222D.toInt())
        drawRectBorder(pixels, width, nameBarLeft, nameBarTop, nameBarRight - nameBarLeft, nameBarHeight, 0xFF4B5563.toInt())

        // Attribute Orb (Top-Right of Name Bar)
        val orbRadius = 6
        val orbCenterX = nameBarRight - orbRadius - 2
        val orbCenterY = nameBarTop + orbRadius + 2
        val attrColor = getAttributeColor(card.attribute)
        drawCircle(pixels, width, orbCenterX, orbCenterY, orbRadius, attrColor)
        drawCircle(pixels, width, orbCenterX, orbCenterY, orbRadius - 2, brighten(attrColor, 40))

        // 4. Star Rating Row (For Monsters)
        val artTop = if (card.isMonster) 34 else 28
        if (card.isMonster && card.level > 0) {
            val starRowY = 25
            val starCount = min(12, card.level)
            val starSpacing = 8
            val startX = width - 14 - (starCount * starSpacing)
            for (i in 0 until starCount) {
                val sx = startX + (i * starSpacing)
                drawStar(pixels, width, sx, starRowY, 3, COLOR_STAR_GOLD, COLOR_STAR_RED)
            }
        }

        // 5. Central Artwork Box
        val artLeft = 14
        val artWidth = width - 28
        val artHeight = 72
        drawRect(pixels, width, artLeft - 2, artTop - 2, artWidth + 4, artHeight + 4, 0xFF1E2430.toInt())
        drawRectBorder(pixels, width, artLeft - 2, artTop - 2, artWidth + 4, artHeight + 4, 0xFF785B28.toInt())

        // Render Card Illustration into Art Window
        val artPixels = artPixelsOverride ?: renderCardArtwork(card, artWidth, artHeight)
        for (ay in 0 until artHeight) {
            for (ax in 0 until artWidth) {
                val px = artLeft + ax
                val py = artTop + ay
                if (px in 0 until width && py in 0 until height && ay * artWidth + ax < artPixels.size) {
                    pixels[py * width + px] = artPixels[ay * artWidth + ax]
                }
            }
        }

        // 6. Text Lore Box (Bottom)
        val loreTop = artTop + artHeight + 6
        val loreLeft = 10
        val loreWidth = width - 20
        val loreHeight = height - loreTop - 10
        drawRect(pixels, width, loreLeft, loreTop, loreWidth, loreHeight, textBoxBg)
        drawRectBorder(pixels, width, loreLeft, loreTop, loreWidth, loreHeight, 0xFF785B28.toInt())

        // Simulated Text Lines inside Lore Box
        val lineStart = loreTop + 6
        val lineCount = min(4, max(2, loreHeight / 8))
        for (l in 0 until lineCount) {
            val ly = lineStart + (l * 5)
            val lineLen = if (l == lineCount - 1) (loreWidth * 0.6f).toInt() else (loreWidth - 8)
            for (lx in 4 until lineLen) {
                val px = loreLeft + lx
                if (px in 0 until width && ly in 0 until height) {
                    pixels[ly * width + px] = 0xFF5A4428.toInt()
                }
            }
        }

        // 7. ATK / DEF Score Ribbon (Monsters)
        if (card.isMonster) {
            val statsY = loreTop + loreHeight - 8
            val barStart = loreLeft + loreWidth - 54
            drawRect(pixels, width, barStart, statsY, 50, 6, 0xFF2A1C0E.toInt())
            drawCircle(pixels, width, barStart + 4, statsY + 3, 2, 0xFFEF4444.toInt())
            drawCircle(pixels, width, barStart + 28, statsY + 3, 2, 0xFF3B82F6.toInt())
        }

        // 8. Hologram Foil Square
        val holoSize = 6
        val holoX = width - 12
        val holoY = height - 12
        drawRect(pixels, width, holoX, holoY, holoSize, holoSize, 0xFFF59E0B.toInt())
        drawCircle(pixels, width, holoX + 3, holoY + 3, 2, 0xFFFFFFFF.toInt())

        return pixels
    }

    /**
     * Authentic GBC Pixel-Art Card Illustration Renderer.
     * Generates clean, crisp, high-contrast retro sprites with authentic 4-color palettes.
     */
    fun renderCardArtwork(card: Card, width: Int = 64, height: Int = 64): IntArray {
        val pixels = IntArray(width * height)

        val palette = getGbcPalette(card.attribute)
        val colorDark = palette[0]
        val colorMid1 = palette[1]
        val colorMid2 = palette[2]
        val colorLight = palette[3]

        // 1. Fill background with dark GBC tone
        pixels.fill(colorDark)

        val cx = width / 2
        val cy = height / 2

        // Background elemental circle badge
        drawCircle(pixels, width, cx, cy, min(cx, cy) - 4, colorMid1)

        // Draw iconic monster / spell / trap silhouette sprite
        when {
            card.name.contains("Dragon") -> {
                drawDragonSprite(pixels, width, height, colorLight, colorMid2, colorDark)
            }
            card.name.contains("Magician") || card.name.contains("Wizard") || card.name.contains("Elf") -> {
                drawSpellcasterSprite(pixels, width, height, colorLight, colorMid2, colorDark)
            }
            card.name.contains("Skull") || card.name.contains("Specter") || card.name.contains("Ghost") || card.name.contains("Bone") -> {
                drawSkullSprite(pixels, width, height, colorLight, colorMid2, colorDark)
            }
            card.name.contains("Serpent") || card.name.contains("Eater") || card.name.contains("Dolphin") || card.name.contains("Fish") -> {
                drawSerpentSprite(pixels, width, height, colorLight, colorMid2, colorDark)
            }
            card.name.contains("Bird") || card.name.contains("Harpie") || card.name.contains("Winged") -> {
                drawBirdSprite(pixels, width, height, colorLight, colorMid2, colorDark)
            }
            card.name.contains("Warrior") || card.name.contains("Swordsman") || card.name.contains("Guardian") || card.name.contains("Ox") || card.name.contains("Giant") -> {
                drawWarriorSprite(pixels, width, height, colorLight, colorMid2, colorDark)
            }
            card.name.contains("Spider") || card.name.contains("Bug") || card.name.contains("Fly") || card.name.contains("Leghul") || card.name.contains("Dogra") -> {
                drawInsectSprite(pixels, width, height, colorLight, colorMid2, colorDark)
            }
            card.isSpell -> {
                drawSpellGlyph(pixels, width, height, colorLight, colorMid2)
            }
            card.isTrap -> {
                drawTrapGlyph(pixels, width, height, colorLight, colorMid2)
            }
            else -> {
                drawGenericMonsterSprite(pixels, width, height, colorLight, colorMid2, colorDark)
            }
        }

        return pixels
    }

    /**
     * Authentic 4-color Game Boy Color elemental palettes.
     */
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

    // --- ICONIC GBC RETRO SPRITE DRAWING HELPERS ---

    private fun drawDragonSprite(p: IntArray, w: Int, h: Int, colMain: Int, colSec: Int, colEye: Int) {
        val cx = w / 2
        val cy = h / 2
        // Dragon Head silhouette
        drawTriangle(p, w, cx - 14, cy - 8, cx + 14, cy - 8, cx, cy + 18, colMain)
        // Horns
        drawTriangle(p, w, cx - 12, cy - 8, cx - 22, cy - 24, cx - 6, cy - 14, colSec)
        drawTriangle(p, w, cx + 12, cy - 8, cx + 22, cy - 24, cx + 6, cy - 14, colSec)
        // Snout & Mouth
        drawRect(p, w, cx - 6, cy + 6, 12, 10, colSec)
        // Glowing Eyes
        drawRect(p, w, cx - 8, cy - 2, 4, 3, colEye)
        drawRect(p, w, cx + 4, cy - 2, 4, 3, colEye)
    }

    private fun drawSpellcasterSprite(p: IntArray, w: Int, h: Int, colMain: Int, colSec: Int, colEye: Int) {
        val cx = w / 2
        val cy = h / 2
        // Pointed Sorcerer Hat
        drawTriangle(p, w, cx - 16, cy - 2, cx + 16, cy - 2, cx, cy - 26, colMain)
        // Hat Brim
        drawRect(p, w, cx - 20, cy - 4, 40, 4, colSec)
        // Robe Torso
        drawTriangle(p, w, cx - 14, cy + 24, cx + 14, cy + 24, cx, cy + 2, colMain)
        // Visage & Eyes
        drawRect(p, w, cx - 6, cy, 12, 8, colSec)
        drawCircle(p, w, cx - 3, cy + 3, 2, colEye)
        drawCircle(p, w, cx + 3, cy + 3, 2, colEye)
    }

    private fun drawSkullSprite(p: IntArray, w: Int, h: Int, colMain: Int, colSec: Int, colDark: Int) {
        val cx = w / 2
        val cy = h / 2
        // Cranium
        drawCircle(p, w, cx, cy - 4, 15, colMain)
        // Cheekbones & Jaw
        drawRect(p, w, cx - 10, cy + 4, 20, 14, colMain)
        // Eye sockets
        drawCircle(p, w, cx - 6, cy - 2, 4, colDark)
        drawCircle(p, w, cx + 6, cy - 2, 4, colDark)
        // Nasal cavity & Teeth
        drawRect(p, w, cx - 2, cy + 4, 4, 4, colDark)
        for (i in 0 until 4) {
            drawRect(p, w, cx - 8 + (i * 4), cy + 12, 2, 4, colDark)
        }
    }

    private fun drawSerpentSprite(p: IntArray, w: Int, h: Int, colMain: Int, colSec: Int, colEye: Int) {
        val cx = w / 2
        val cy = h / 2
        // Coiled body rings
        drawCircleBorder(p, w, cx, cy + 6, 16, colMain)
        drawCircleBorder(p, w, cx, cy + 6, 10, colSec)
        // Serpent Head
        drawTriangle(p, w, cx - 8, cy - 6, cx + 8, cy - 6, cx, cy - 22, colMain)
        // Slit Eyes
        drawRect(p, w, cx - 4, cy - 14, 2, 3, colEye)
        drawRect(p, w, cx + 2, cy - 14, 2, 3, colEye)
    }

    private fun drawBirdSprite(p: IntArray, w: Int, h: Int, colMain: Int, colSec: Int, colEye: Int) {
        val cx = w / 2
        val cy = h / 2
        // Outstretched Wings
        drawTriangle(p, w, cx - 24, cy - 12, cx, cy + 4, cx - 6, cy + 18, colMain)
        drawTriangle(p, w, cx + 24, cy - 12, cx, cy + 4, cx + 6, cy + 18, colMain)
        // Avian Head & Beak
        drawCircle(p, w, cx, cy - 10, 8, colSec)
        drawTriangle(p, w, cx - 4, cy - 6, cx + 4, cy - 6, cx, cy + 4, colEye)
    }

    private fun drawWarriorSprite(p: IntArray, w: Int, h: Int, colMain: Int, colSec: Int, colDark: Int) {
        val cx = w / 2
        val cy = h / 2
        // Helmet & Crest
        drawTriangle(p, w, cx - 12, cy - 10, cx + 12, cy - 10, cx, cy - 24, colSec)
        drawRect(p, w, cx - 10, cy - 10, 20, 12, colMain)
        // Visor slit
        drawRect(p, w, cx - 7, cy - 5, 14, 3, colDark)
        // Broadsword blade
        drawRect(p, w, cx - 18, cy - 18, 4, 36, colSec)
        drawRect(p, w, cx - 22, cy + 8, 12, 3, colMain)
    }

    private fun drawInsectSprite(p: IntArray, w: Int, h: Int, colMain: Int, colSec: Int, colEye: Int) {
        val cx = w / 2
        val cy = h / 2
        // Carapace & Abdomen
        drawCircle(p, w, cx, cy + 8, 12, colMain)
        drawCircle(p, w, cx, cy - 6, 8, colSec)
        // Compound Eyes
        drawCircle(p, w, cx - 5, cy - 8, 3, colEye)
        drawCircle(p, w, cx + 5, cy - 8, 3, colEye)
        // Antennae
        drawRect(p, w, cx - 8, cy - 20, 2, 10, colMain)
        drawRect(p, w, cx + 6, cy - 20, 2, 10, colMain)
    }

    private fun drawSpellGlyph(p: IntArray, w: Int, h: Int, colLight: Int, colMid: Int) {
        val cx = w / 2
        val cy = h / 2
        // Magic Arcane Ring
        drawCircleBorder(p, w, cx, cy, 18, colLight)
        drawCircleBorder(p, w, cx, cy, 12, colMid)
        // Mystic Star Crest
        drawStar(p, w, cx, cy, 8, colLight, colMid)
    }

    private fun drawTrapGlyph(p: IntArray, w: Int, h: Int, colLight: Int, colMid: Int) {
        val cx = w / 2
        val cy = h / 2
        // Spiked Trap Hole emblem
        drawCircle(p, w, cx, cy, 14, colMid)
        drawCircle(p, w, cx, cy, 9, 0xFF000000.toInt())
        // Danger Diamond
        drawTriangle(p, w, cx - 8, cy, cx + 8, cy, cx, cy - 12, colLight)
        drawTriangle(p, w, cx - 8, cy, cx + 8, cy, cx, cy + 12, colLight)
    }

    private fun drawGenericMonsterSprite(p: IntArray, w: Int, h: Int, colMain: Int, colSec: Int, colEye: Int) {
        val cx = w / 2
        val cy = h / 2
        drawCircle(p, w, cx, cy, 14, colMain)
        drawCircle(p, w, cx, cy, 9, colSec)
        drawCircle(p, w, cx - 4, cy - 2, 2, colEye)
        drawCircle(p, w, cx + 4, cy - 2, 2, colEye)
    }

    /**
     * Authentic Yu-Gi-Oh! Card Back (Deep Burgundy border, dark spiral, Millennium Eye vortex).
     */
    fun renderCardBack(width: Int = 128, height: Int = 186): IntArray {
        val pixels = IntArray(width * height)
        val outerBorder = 0xFF140D07.toInt()
        val brownBase = 0xFF5C2B14.toInt()
        val blackCore = 0xFF0B0704.toInt()
        val goldRing = 0xFFD4AF37.toInt()
        val eyeOrange = 0xFFF97316.toInt()

        pixels.fill(brownBase)

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (x < 5 || x >= width - 5 || y < 5 || y >= height - 5) {
                    pixels[y * width + x] = outerBorder
                }
            }
        }

        val cx = width / 2.0
        val cy = height / 2.0
        val radiusX = width * 0.36
        val radiusY = height * 0.36

        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = (x - cx) / radiusX
                val dy = (y - cy) / radiusY
                val dist = dx * dx + dy * dy
                if (dist <= 1.0) {
                    val spiral = sin(dist * 12.0 + Math.atan2(y - cy, x - cx) * 3.0)
                    if (dist > 0.88) {
                        pixels[y * width + x] = goldRing
                    } else {
                        val c = if (spiral > 0.2) 0xFF1A120B.toInt() else blackCore
                        pixels[y * width + x] = c
                    }
                }
            }
        }

        drawCircle(pixels, width, cx.toInt(), cy.toInt(), 12, goldRing)
        drawCircle(pixels, width, cx.toInt(), cy.toInt(), 9, blackCore)
        drawCircle(pixels, width, cx.toInt(), cy.toInt(), 5, eyeOrange)
        drawCircle(pixels, width, cx.toInt(), cy.toInt(), 2, 0xFF000000.toInt())

        return pixels
    }

    // --- PRIMITIVE DRAWING UTILITIES ---

    private fun getAttributeColor(attr: CardAttribute): Int {
        return when (attr) {
            CardAttribute.LIGHT -> 0xFFFACC15.toInt()
            CardAttribute.DARK -> 0xFF7C3AED.toInt()
            CardAttribute.FIRE -> 0xFFEF4444.toInt()
            CardAttribute.WATER -> 0xFF38BDF8.toInt()
            CardAttribute.EARTH -> 0xFFD97706.toInt()
            CardAttribute.WIND -> 0xFF34D399.toInt()
            CardAttribute.THUNDER -> 0xFFFBBF24.toInt()
            CardAttribute.FOREST -> 0xFF10B981.toInt()
            CardAttribute.FIEND -> 0xFFA855F7.toInt()
            CardAttribute.DREAMS -> 0xFFF472B6.toInt()
            CardAttribute.SPELL -> 0xFF2DD4BF.toInt()
            CardAttribute.TRAP -> 0xFFF43F5E.toInt()
            else -> 0xFF94A3B8.toInt()
        }
    }

    private fun brighten(color: Int, amount: Int): Int {
        val r = (((color shr 16) and 0xFF) + amount).coerceIn(0, 255)
        val g = (((color shr 8) and 0xFF) + amount).coerceIn(0, 255)
        val b = ((color and 0xFF) + amount).coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun drawRect(pixels: IntArray, w: Int, x: Int, y: Int, rw: Int, rh: Int, color: Int) {
        val totalH = pixels.size / w
        for (cy in y until (y + rh)) {
            if (cy !in 0 until totalH) continue
            val rowOffset = cy * w
            for (cx in x until (x + rw)) {
                if (cx in 0 until w) {
                    pixels[rowOffset + cx] = color
                }
            }
        }
    }

    private fun drawRectBorder(pixels: IntArray, w: Int, x: Int, y: Int, rw: Int, rh: Int, color: Int) {
        val totalH = pixels.size / w
        for (cx in x until (x + rw)) {
            if (y in 0 until totalH && cx in 0 until w) pixels[y * w + cx] = color
            if ((y + rh - 1) in 0 until totalH && cx in 0 until w) pixels[(y + rh - 1) * w + cx] = color
        }
        for (cy in y until (y + rh)) {
            if (cy in 0 until totalH && x in 0 until w) pixels[cy * w + x] = color
            if (cy in 0 until totalH && (x + rw - 1) in 0 until w) pixels[cy * w + (x + rw - 1)] = color
        }
    }

    private fun drawCircle(pixels: IntArray, w: Int, cx: Int, cy: Int, r: Int, color: Int) {
        val totalH = pixels.size / w
        val rSq = r * r
        for (dy in -r..r) {
            val py = cy + dy
            if (py !in 0 until totalH) continue
            val rowOffset = py * w
            for (dx in -r..r) {
                val px = cx + dx
                if (px in 0 until w && (dx * dx + dy * dy) <= rSq) {
                    pixels[rowOffset + px] = color
                }
            }
        }
    }

    private fun drawCircleBorder(pixels: IntArray, w: Int, cx: Int, cy: Int, r: Int, color: Int) {
        val totalH = pixels.size / w
        val rMinSq = (r - 1) * (r - 1)
        val rMaxSq = r * r
        for (dy in -r..r) {
            val py = cy + dy
            if (py !in 0 until totalH) continue
            val rowOffset = py * w
            for (dx in -r..r) {
                val px = cx + dx
                val dSq = dx * dx + dy * dy
                if (px in 0 until w && dSq >= rMinSq && dSq <= rMaxSq) {
                    pixels[rowOffset + px] = color
                }
            }
        }
    }

    private fun drawStar(pixels: IntArray, w: Int, cx: Int, cy: Int, r: Int, outerColor: Int, innerColor: Int) {
        drawCircle(pixels, w, cx, cy, r, outerColor)
        drawCircle(pixels, w, cx, cy, max(1, r / 2), innerColor)
    }

    private fun drawTriangle(pixels: IntArray, w: Int, x1: Int, y1: Int, x2: Int, y2: Int, x3: Int, y3: Int, color: Int) {
        val totalH = pixels.size / w
        val minX = max(0, min(x1, min(x2, x3)))
        val maxX = min(w - 1, max(x1, max(x2, x3)))
        val minY = max(0, min(y1, min(y2, y3)))
        val maxY = min(totalH - 1, max(y1, max(y2, y3)))

        for (y in minY..maxY) {
            val rowOffset = y * w
            for (x in minX..maxX) {
                val b1 = sign(x, y, x1, y1, x2, y2) < 0.0f
                val b2 = sign(x, y, x2, y2, x3, y3) < 0.0f
                val b3 = sign(x, y, x3, y3, x1, y1) < 0.0f
                if ((b1 == b2) && (b2 == b3)) {
                    pixels[rowOffset + x] = color
                }
            }
        }
    }

    private fun sign(p1x: Int, p1y: Int, p2x: Int, p2y: Int, p3x: Int, p3y: Int): Float {
        return ((p1x - p3x) * (p2y - p3y) - (p2x - p3x) * (p1y - p3y)).toFloat()
    }
}
