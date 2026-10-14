package com.yugioh.recomp.core.rom

import com.yugioh.recomp.core.engine.Card
import com.yugioh.recomp.core.engine.CardAttribute
import com.yugioh.recomp.core.engine.CardType
import com.yugioh.recomp.core.engine.MonsterType
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.cos

/**
 * High-fidelity pixel-art & card frame synthesizer for Duel-decomp.
 * Renders authentic Yu-Gi-Oh! card frames, attribute gems, star counts,
 * and distinctive monster illustrations without relying on raw ROM noise.
 */
object CardArtRenderer {

    // Classic Yu-Gi-Oh! Card Frame Base Colors (ARGB)
    const val COLOR_FRAME_NORMAL_MONSTER = 0xFFD29C54.toInt() // Classic Yellow/Ochre
    const val COLOR_FRAME_EFFECT_MONSTER = 0xFFC96428.toInt() // Rich Orange
    const val COLOR_FRAME_SPELL = 0xFF19937E.toInt()          // Vibrant Teal
    const val COLOR_FRAME_TRAP = 0xFFBC2668.toInt()           // Deep Magenta/Pink
    const val COLOR_CARD_BORDER_OUTER = 0xFF120E09.toInt()    // Dark border
    const val COLOR_CARD_BORDER_INNER = 0xFF2B1F11.toInt()
    const val COLOR_TEXT_BOX_BG = 0xFFF7E6C4.toInt()          // Parchment text box
    const val COLOR_SPELL_TEXT_BG = 0xFFE0F3EE.toInt()
    const val COLOR_TRAP_TEXT_BG = 0xFFFCE7F0.toInt()
    const val COLOR_STAR_GOLD = 0xFFFFD700.toInt()
    const val COLOR_STAR_RED = 0xFFDC2626.toInt()
    const val COLOR_GOLD_FOIL = 0xFFF59E0B.toInt()

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
            card.level >= 5 || card.name.contains("Dragon") || card.name.contains("Magician") -> COLOR_FRAME_EFFECT_MONSTER
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

        // 2. Outer Card Border (Dark Yu-Gi-Oh! bevel)
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

        // 6. Text / Lore Box (Bottom)
        val loreTop = artTop + artHeight + 4
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
            // Small ATK/DEF score indicator bar at the bottom right of the lore box
            val barStart = loreLeft + loreWidth - 54
            drawRect(pixels, width, barStart, statsY, 50, 6, 0xFF2A1C0E.toInt())
            // ATK dot (Red) & DEF dot (Blue)
            drawCircle(pixels, width, barStart + 4, statsY + 3, 2, 0xFFEF4444.toInt())
            drawCircle(pixels, width, barStart + 28, statsY + 3, 2, 0xFF3B82F6.toInt())
        }

        // 8. Hologram Foil Square (Bottom Right Corner)
        val holoSize = 6
        val holoX = width - 12
        val holoY = height - 12
        drawRect(pixels, width, holoX, holoY, holoSize, holoSize, COLOR_GOLD_FOIL)
        drawCircle(pixels, width, holoX + 3, holoY + 3, 2, 0xFFFFFFFF.toInt())

        return pixels
    }

    /**
     * Renders a distinctive, iconic illustration for the given card.
     */
    fun renderCardArtwork(card: Card, width: Int = 64, height: Int = 64): IntArray {
        val pixels = IntArray(width * height)

        when (card.name) {
            "Blue-Eyes White Dragon" -> drawBlueEyesWhiteDragon(pixels, width, height)
            "Dark Magician" -> drawDarkMagician(pixels, width, height)
            "Red-Eyes B. Dragon" -> drawRedEyesBlackDragon(pixels, width, height)
            "Summoned Skull" -> drawSummonedSkull(pixels, width, height)
            "Celtic Guardian" -> drawCelticGuardian(pixels, width, height)
            "Curse of Dragon" -> drawCurseOfDragon(pixels, width, height)
            "Gaia The Fierce Knight" -> drawGaiaKnight(pixels, width, height)
            "Baby Dragon" -> drawBabyDragon(pixels, width, height)
            "Time Wizard" -> drawTimeWizard(pixels, width, height)
            "Mystical Elf" -> drawMysticalElf(pixels, width, height)
            "Pot of Greed" -> drawPotOfGreed(pixels, width, height)
            "Raigeki" -> drawRaigeki(pixels, width, height)
            "Dark Hole" -> drawDarkHole(pixels, width, height)
            "Dian Keto the Cure Maiden" -> drawDianKeto(pixels, width, height)
            "Sparks" -> drawSparks(pixels, width, height)
            "Mirror Force" -> drawMirrorForce(pixels, width, height)
            "Trap Hole" -> drawTrapHole(pixels, width, height)
            "Reinforcements" -> drawReinforcements(pixels, width, height)
            "Exodia the Forbidden One",
            "Right Arm of the Forbidden One",
            "Left Arm of the Forbidden One",
            "Right Leg of the Forbidden One",
            "Left Leg of the Forbidden One" -> drawExodia(pixels, width, height)
            else -> drawArchetypeArt(pixels, width, height, card)
        }

        return pixels
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

        // 1. Fill Base Brown
        pixels.fill(brownBase)

        // 2. Outer Border
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (x < 5 || x >= width - 5 || y < 5 || y >= height - 5) {
                    pixels[y * width + x] = outerBorder
                }
            }
        }

        // 3. Central Oval Swirl
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
                    // Inside dark vortex
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

        // 4. Center Millennium Eye emblem
        drawCircle(pixels, width, cx.toInt(), cy.toInt(), 12, goldRing)
        drawCircle(pixels, width, cx.toInt(), cy.toInt(), 9, blackCore)
        drawCircle(pixels, width, cx.toInt(), cy.toInt(), 5, eyeOrange)
        drawCircle(pixels, width, cx.toInt(), cy.toInt(), 2, 0xFF000000.toInt())

        return pixels
    }

    // =========================================================================
    // ICONIC MONSTER & CARD ART ILLUSTRATIONS
    // =========================================================================

    /** Blue-Eyes White Dragon: Silver dragon with radiant blue energy & majestic wings */
    private fun drawBlueEyesWhiteDragon(pixels: IntArray, w: Int, h: Int) {
        fillGradientRadial(pixels, w, h, 0xFF0F1E36.toInt(), 0xFF050B14.toInt())

        val cx = w / 2
        val cy = h / 2

        // Radiant blue aura
        drawCircle(pixels, w, cx, cy, (w * 0.45).toInt(), 0x333B82F6.toInt())

        // Dragon Wings (Ivory/Silver with cyan highlights)
        drawTriangle(pixels, w, cx - 4, cy, cx - 26, cy - 20, cx - 18, cy + 10, 0xFFE2E8F0.toInt())
        drawTriangle(pixels, w, cx + 4, cy, cx + 26, cy - 20, cx + 18, cy + 10, 0xFFCBD5E1.toInt())
        drawTriangle(pixels, w, cx - 2, cy - 6, cx - 22, cy - 16, cx - 14, cy + 6, 0xFF93C5FD.toInt())
        drawTriangle(pixels, w, cx + 2, cy - 6, cx + 22, cy - 16, cx + 14, cy + 6, 0xFF60A5FA.toInt())

        // Dragon Torso & Neck
        drawTriangle(pixels, w, cx - 6, cy + 22, cx + 6, cy + 22, cx, cy - 8, 0xFFE2E8F0.toInt())
        drawTriangle(pixels, w, cx - 4, cy - 4, cx + 4, cy - 4, cx, cy - 18, 0xFFF1F5F9.toInt())

        // Dragon Head & Horns
        drawCircle(pixels, w, cx, cy - 16, 7, 0xFFF8FAFC.toInt())
        drawLine(pixels, w, cx - 4, cy - 18, cx - 12, cy - 26, 0xFFE2E8F0.toInt())
        drawLine(pixels, w, cx + 4, cy - 18, cx + 12, cy - 26, 0xFFE2E8F0.toInt())
        drawLine(pixels, w, cx, cy - 20, cx, cy - 28, 0xFF93C5FD.toInt())

        // Piercing Blue Eyes!
        drawCircle(pixels, w, cx - 3, cy - 16, 2, 0xFF00E5FF.toInt())
        drawCircle(pixels, w, cx + 3, cy - 16, 2, 0xFF00E5FF.toInt())
        drawCircle(pixels, w, cx - 3, cy - 16, 1, 0xFFFFFFFF.toInt())
        drawCircle(pixels, w, cx + 3, cy - 16, 1, 0xFFFFFFFF.toInt())
    }

    /** Dark Magician: Sorcerer in purple robes, pointed hat, with glowing green spell staff */
    private fun drawDarkMagician(pixels: IntArray, w: Int, h: Int) {
        fillGradientRadial(pixels, w, h, 0xFF2E1065.toInt(), 0xFF0F051D.toInt())
        val cx = w / 2
        val cy = h / 2

        // Mystic Green Spell Ring
        drawCircleBorder(pixels, w, cx, cy, (w * 0.38).toInt(), 0xFF10B981.toInt())

        // Purple Robe Torso
        drawTriangle(pixels, w, cx - 14, cy + 24, cx + 14, cy + 24, cx, cy - 2, 0xFF6B21A8.toInt())
        drawTriangle(pixels, w, cx - 8, cy + 24, cx + 8, cy + 24, cx, cy + 4, 0xFF9333EA.toInt())

        // Pointed Sorcerer Hat
        drawTriangle(pixels, w, cx - 12, cy - 10, cx + 12, cy - 10, cx, cy - 28, 0xFF581C87.toInt())
        drawTriangle(pixels, w, cx - 6, cy - 10, cx + 6, cy - 10, cx - 2, cy - 26, 0xFF7E22CE.toInt())

        // Face & Visage
        drawCircle(pixels, w, cx, cy - 8, 5, 0xFFFFDFBA.toInt())
        drawCircle(pixels, w, cx - 2, cy - 8, 1, 0xFF10B981.toInt())
        drawCircle(pixels, w, cx + 2, cy - 8, 1, 0xFF10B981.toInt())

        // Mystic Staff with Glowing Emerald Orb
        val staffX = cx + 18
        drawLine(pixels, w, staffX, cy - 24, staffX - 8, cy + 24, 0xFFD4AF37.toInt())
        drawCircle(pixels, w, staffX, cy - 24, 5, 0xFF10B981.toInt())
        drawCircle(pixels, w, staffX, cy - 24, 2, 0xFF6EE7B7.toInt())
    }

    /** Red-Eyes B. Dragon: Menacing black dragon with ruby eyes and crimson breath */
    private fun drawRedEyesBlackDragon(pixels: IntArray, w: Int, h: Int) {
        fillGradientRadial(pixels, w, h, 0xFF450A0A.toInt(), 0xFF0F0404.toInt())
        val cx = w / 2
        val cy = h / 2

        // Obsidian Wings
        drawTriangle(pixels, w, cx - 4, cy, cx - 26, cy - 18, cx - 18, cy + 12, 0xFF1F2937.toInt())
        drawTriangle(pixels, w, cx + 4, cy, cx + 26, cy - 18, cx + 18, cy + 12, 0xFF111827.toInt())
        drawTriangle(pixels, w, cx - 2, cy - 4, cx - 22, cy - 14, cx - 14, cy + 8, 0xFF7F1D1D.toInt())
        drawTriangle(pixels, w, cx + 2, cy - 4, cx + 22, cy - 14, cx + 14, cy + 8, 0xFF991B1B.toInt())

        // Dark Body
        drawTriangle(pixels, w, cx - 7, cy + 22, cx + 7, cy + 22, cx, cy - 8, 0xFF1F2937.toInt())

        // Head & Horns
        drawCircle(pixels, w, cx, cy - 16, 7, 0xFF111827.toInt())
        drawLine(pixels, w, cx - 4, cy - 18, cx - 14, cy - 26, 0xFF374151.toInt())
        drawLine(pixels, w, cx + 4, cy - 18, cx + 14, cy - 26, 0xFF374151.toInt())

        // Glowing Ruby Red Eyes!
        drawCircle(pixels, w, cx - 3, cy - 16, 2, 0xFFEF4444.toInt())
        drawCircle(pixels, w, cx + 3, cy - 16, 2, 0xFFEF4444.toInt())
        drawCircle(pixels, w, cx - 3, cy - 16, 1, 0xFFFCA5A5.toInt())
        drawCircle(pixels, w, cx + 3, cy - 16, 1, 0xFFFCA5A5.toInt())

        // Fire Embers
        drawCircle(pixels, w, cx, cy - 8, 2, 0xFFF97316.toInt())
        drawCircle(pixels, w, cx + 4, cy - 4, 1, 0xFFEF4444.toInt())
    }

    /** Summoned Skull: Horned demon skeleton with lightning aura */
    private fun drawSummonedSkull(pixels: IntArray, w: Int, h: Int) {
        fillGradientRadial(pixels, w, h, 0xFF311042.toInt(), 0xFF0D0312.toInt())
        val cx = w / 2
        val cy = h / 2

        // Skeletal Horns
        drawLine(pixels, w, cx - 6, cy - 12, cx - 22, cy - 24, 0xFFE2E8F0.toInt())
        drawLine(pixels, w, cx + 6, cy - 12, cx + 22, cy - 24, 0xFFE2E8F0.toInt())

        // Demon Ribcage & Armor
        drawTriangle(pixels, w, cx - 12, cy + 24, cx + 12, cy + 24, cx, cy + 2, 0xFF4A0E4E.toInt())
        for (i in 0 until 4) {
            val ry = cy + 6 + (i * 4)
            drawLine(pixels, w, cx - 10 + i, ry, cx + 10 - i, ry, 0xFFE2E8F0.toInt())
        }

        // Skull Face
        drawCircle(pixels, w, cx, cy - 8, 9, 0xFFF8FAFC.toInt())
        drawCircle(pixels, w, cx - 3, cy - 8, 2, 0xFF831843.toInt())
        drawCircle(pixels, w, cx + 3, cy - 8, 2, 0xFF831843.toInt())
        drawCircle(pixels, w, cx, cy - 2, 2, 0xFF1E293B.toInt()) // Nasal cavity

        // Purple Lightning Arcs
        drawLine(pixels, w, cx - 18, cy - 10, cx - 8, cy + 2, 0xFFC084FC.toInt())
        drawLine(pixels, w, cx + 18, cy - 10, cx + 8, cy + 2, 0xFFC084FC.toInt())
    }

    /** Celtic Guardian: Elven swordsman in green tunic with broadsword */
    private fun drawCelticGuardian(pixels: IntArray, w: Int, h: Int) {
        fillGradientRadial(pixels, w, h, 0xFF064E3B.toInt(), 0xFF021E17.toInt())
        val cx = w / 2
        val cy = h / 2

        // Green Tunic
        drawTriangle(pixels, w, cx - 10, cy + 22, cx + 10, cy + 22, cx, cy + 2, 0xFF059669.toInt())
        // Gold Pauldrons
        drawCircle(pixels, w, cx - 10, cy + 2, 4, 0xFFF59E0B.toInt())
        drawCircle(pixels, w, cx + 10, cy + 2, 4, 0xFFF59E0B.toInt())

        // Head & Elven Ears
        drawCircle(pixels, w, cx, cy - 8, 7, 0xFFFFDFBA.toInt())
        drawLine(pixels, w, cx - 6, cy - 8, cx - 14, cy - 14, 0xFFFFDFBA.toInt())
        drawLine(pixels, w, cx + 6, cy - 8, cx + 14, cy - 14, 0xFFFFDFBA.toInt())
        // Golden Hair
        drawTriangle(pixels, w, cx - 8, cy - 10, cx + 8, cy - 10, cx, cy - 18, 0xFFFCD34D.toInt())

        // Silver Broadsword
        val swX = cx + 14
        drawLine(pixels, w, swX, cy - 22, swX, cy + 18, 0xFFE2E8F0.toInt())
        drawLine(pixels, w, swX - 4, cy + 10, swX + 4, cy + 10, 0xFFF59E0B.toInt()) // Crossguard
        drawCircle(pixels, w, swX, cy + 20, 2, 0xFFF59E0B.toInt()) // Pommel
    }

    /** Curse of Dragon: Skeletal winged bone dragon */
    private fun drawCurseOfDragon(pixels: IntArray, w: Int, h: Int) {
        fillGradientRadial(pixels, w, h, 0xFF3F2E05.toInt(), 0xFF140F02.toInt())
        val cx = w / 2
        val cy = h / 2

        // Bone Wings
        drawTriangle(pixels, w, cx - 2, cy, cx - 24, cy - 16, cx - 14, cy + 12, 0xFFFEF08A.toInt())
        drawTriangle(pixels, w, cx + 2, cy, cx + 24, cy - 16, cx + 14, cy + 12, 0xFFFDE047.toInt())

        // Serpentine Ribs
        for (i in 0 until 5) {
            val sy = cy - 10 + (i * 6)
            drawLine(pixels, w, cx - 8 + (i % 2), sy, cx + 8 - (i % 2), sy, 0xFFE2E8F0.toInt())
        }
        drawCircle(pixels, w, cx, cy - 18, 6, 0xFFFEF9C3.toInt())
        drawCircle(pixels, w, cx - 2, cy - 18, 1, 0xFF15803D.toInt())
        drawCircle(pixels, w, cx + 2, cy - 18, 1, 0xFF15803D.toInt())
    }

    /** Gaia The Fierce Knight: Armored cavalry knight with golden lance */
    private fun drawGaiaKnight(pixels: IntArray, w: Int, h: Int) {
        fillGradientRadial(pixels, w, h, 0xFF1E3A8A.toInt(), 0xFF08122E.toInt())
        val cx = w / 2
        val cy = h / 2

        // Horse silhouette
        drawCircle(pixels, w, cx - 6, cy + 14, 12, 0xFF3B82F6.toInt())
        // Knight Torso
        drawTriangle(pixels, w, cx - 8, cy + 12, cx + 8, cy + 12, cx, cy - 6, 0xFF1D4ED8.toInt())
        // Golden Helm & Crest
        drawCircle(pixels, w, cx, cy - 10, 6, 0xFFF59E0B.toInt())
        drawTriangle(pixels, w, cx - 4, cy - 12, cx + 4, cy - 12, cx, cy - 24, 0xFFEF4444.toInt())

        // Spiral Lance
        val lanceX = cx + 12
        drawLine(pixels, w, lanceX - 4, cy + 16, lanceX + 10, cy - 26, 0xFFFDE047.toInt())
        drawLine(pixels, w, lanceX - 2, cy + 16, lanceX + 12, cy - 26, 0xFFFFFFFF.toInt())
    }

    /** Baby Dragon: Cute winged orange dragon hatchling */
    private fun drawBabyDragon(pixels: IntArray, w: Int, h: Int) {
        fillGradientRadial(pixels, w, h, 0xFF9A3412.toInt(), 0xFF270902.toInt())
        val cx = w / 2
        val cy = h / 2

        // Small Dragon Wings
        drawTriangle(pixels, w, cx - 4, cy, cx - 18, cy - 12, cx - 10, cy + 8, 0xFFFDBA74.toInt())
        drawTriangle(pixels, w, cx + 4, cy, cx + 18, cy - 12, cx + 10, cy + 8, 0xFFFB923C.toInt())

        // Round Chubby Body
        drawCircle(pixels, w, cx, cy + 6, 12, 0xFFEA580C.toInt())
        drawCircle(pixels, w, cx, cy + 6, 9, 0xFFF97316.toInt())

        // Head & Horns
        drawCircle(pixels, w, cx, cy - 8, 10, 0xFFFB923C.toInt())
        drawTriangle(pixels, w, cx - 6, cy - 12, cx - 2, cy - 12, cx - 4, cy - 20, 0xFFFDE047.toInt())
        drawTriangle(pixels, w, cx + 2, cy - 12, cx + 6, cy - 12, cx + 4, cy - 20, 0xFFFDE047.toInt())

        // Big Anime Eyes
        drawCircle(pixels, w, cx - 4, cy - 8, 3, 0xFF1E293B.toInt())
        drawCircle(pixels, w, cx + 4, cy - 8, 3, 0xFF1E293B.toInt())
        drawCircle(pixels, w, cx - 4, cy - 9, 1, 0xFFFFFFFF.toInt())
        drawCircle(pixels, w, cx + 4, cy - 9, 1, 0xFFFFFFFF.toInt())

        // Little Flame Puff
        drawCircle(pixels, w, cx, cy - 1, 3, 0xFFFACC15.toInt())
    }

    /** Time Wizard: Golden pocket-watch sorcerer with spiral eyes */
    private fun drawTimeWizard(pixels: IntArray, w: Int, h: Int) {
        fillGradientRadial(pixels, w, h, 0xFF713F12.toInt(), 0xFF1C0D03.toInt())
        val cx = w / 2
        val cy = h / 2

        // Golden Clock Body
        drawCircle(pixels, w, cx, cy, 18, 0xFFF59E0B.toInt())
        drawCircle(pixels, w, cx, cy, 14, 0xFFFEF3C7.toInt())

        // Clock Hands
        drawLine(pixels, w, cx, cy, cx + 8, cy - 6, 0xFF1F2937.toInt())
        drawLine(pixels, w, cx, cy, cx - 5, cy - 8, 0xFFDC2626.toInt())

        // Wizard Hat
        drawTriangle(pixels, w, cx - 12, cy - 12, cx + 12, cy - 12, cx, cy - 26, 0xFFDC2626.toInt())

        // Hypnotic Spiral Eyes
        drawCircle(pixels, w, cx - 5, cy - 4, 3, 0xFF8B5CF6.toInt())
        drawCircle(pixels, w, cx + 5, cy - 4, 3, 0xFF8B5CF6.toInt())
    }

    /** Mystical Elf: Priestess with radiant aura and flowing robes */
    private fun drawMysticalElf(pixels: IntArray, w: Int, h: Int) {
        fillGradientRadial(pixels, w, h, 0xFF0284C7.toInt(), 0xFF031E2E.toInt())
        val cx = w / 2
        val cy = h / 2

        // Sacred Halo
        drawCircleBorder(pixels, w, cx, cy - 10, 14, 0xFFFDE047.toInt())

        // Flowing White & Gold Robe
        drawTriangle(pixels, w, cx - 14, cy + 24, cx + 14, cy + 24, cx, cy + 2, 0xFFF8FAFC.toInt())
        drawTriangle(pixels, w, cx - 8, cy + 24, cx + 8, cy + 24, cx, cy + 6, 0xFFE2E8F0.toInt())

        // Blue Hair & Elf Visage
        drawCircle(pixels, w, cx, cy - 8, 7, 0xFFFFDFBA.toInt())
        drawTriangle(pixels, w, cx - 8, cy - 6, cx + 8, cy - 6, cx, cy - 18, 0xFF38BDF8.toInt())
        drawLine(pixels, w, cx - 6, cy - 8, cx - 12, cy - 12, 0xFFFFDFBA.toInt())
        drawLine(pixels, w, cx + 6, cy - 8, cx + 12, cy - 12, 0xFFFFDFBA.toInt())
    }

    /** Pot of Greed: Iconic grinning green earthenware jar with golden teeth! */
    private fun drawPotOfGreed(pixels: IntArray, w: Int, h: Int) {
        fillGradientRadial(pixels, w, h, 0xFF065F46.toInt(), 0xFF021B14.toInt())
        val cx = w / 2
        val cy = h / 2

        // Pot Body (Earthen Green)
        drawCircle(pixels, w, cx, cy + 4, 18, 0xFF047857.toInt())
        drawCircle(pixels, w, cx, cy + 4, 15, 0xFF10B981.toInt())

        // Pot Rim & Handles
        drawRect(pixels, w, cx - 12, cy - 16, 24, 6, 0xFFDC2626.toInt()) // Red jar rim
        drawCircleBorder(pixels, w, cx - 16, cy + 2, 6, 0xFF047857.toInt()) // Left handle
        drawCircleBorder(pixels, w, cx + 16, cy + 2, 6, 0xFF047857.toInt()) // Right handle

        // The Legendary Maniacal Grin!
        drawTriangle(pixels, w, cx - 12, cy + 4, cx + 12, cy + 4, cx, cy + 16, 0xFF0F172A.toInt())
        // Gold Teeth
        for (i in 0 until 5) {
            val tx = cx - 10 + (i * 4)
            drawRect(pixels, w, tx, cy + 4, 3, 4, 0xFFFDE047.toInt())
            drawRect(pixels, w, tx, cy + 10, 3, 3, 0xFFFDE047.toInt())
        }

        // Sinister Eyes
        drawCircle(pixels, w, cx - 6, cy - 4, 3, 0xFFDC2626.toInt())
        drawCircle(pixels, w, cx + 6, cy - 4, 3, 0xFFDC2626.toInt())
        drawCircle(pixels, w, cx - 6, cy - 4, 1, 0xFFFFFFFF.toInt())
        drawCircle(pixels, w, cx + 6, cy - 4, 1, 0xFFFFFFFF.toInt())
    }

    /** Raigeki: Golden lightning bolt striking earth with crimson thunder */
    private fun drawRaigeki(pixels: IntArray, w: Int, h: Int) {
        fillGradientRadial(pixels, w, h, 0xFF7F1D1D.toInt(), 0xFF1A0505.toInt())
        val cx = w / 2

        // Storm clouds
        drawCircle(pixels, w, cx - 12, 10, 10, 0xFF374151.toInt())
        drawCircle(pixels, w, cx + 12, 10, 10, 0xFF1F2937.toInt())
        drawCircle(pixels, w, cx, 8, 12, 0xFF4B5563.toInt())

        // Massive Jagged Lightning Bolt
        val path = listOf(
            Pair(cx, 12),
            Pair(cx - 10, 24),
            Pair(cx + 4, 26),
            Pair(cx - 8, 40),
            Pair(cx + 6, 42),
            Pair(cx, h - 6)
        )
        for (i in 0 until path.size - 1) {
            val p1 = path[i]
            val p2 = path[i + 1]
            drawLine(pixels, w, p1.first - 1, p1.second, p2.first - 1, p2.second, 0xFFFDE047.toInt())
            drawLine(pixels, w, p1.first, p1.second, p2.first, p2.second, 0xFFFFFFFF.toInt())
            drawLine(pixels, w, p1.first + 1, p1.second, p2.first + 1, p2.second, 0xFFFDE047.toInt())
        }

        // Ground Impact Crater
        drawCircle(pixels, w, cx, h - 6, 8, 0xFFF59E0B.toInt())
        drawCircle(pixels, w, cx, h - 6, 4, 0xFFFFFFFF.toInt())
    }

    /** Dark Hole: Cosmic black hole vortex with purple accretion disc */
    private fun drawDarkHole(pixels: IntArray, w: Int, h: Int) {
        val cx = w / 2.0
        val cy = h / 2.0
        val maxR = w * 0.45

        for (y in 0 until h) {
            for (x in 0 until w) {
                val dist = hypot(x - cx, y - cy)
                if (dist <= maxR) {
                    val angle = Math.atan2(y - cy, x - cx)
                    val spiral = sin(dist * 0.4 + angle * 3.0)

                    val color = when {
                        dist < maxR * 0.28 -> 0xFF000000.toInt() // Absolute Singularity
                        dist < maxR * 0.36 -> 0xFF8B5CF6.toInt() // Event Horizon
                        spiral > 0.3 -> 0xFF7C3AED.toInt()
                        spiral > -0.2 -> 0xFF4C1D95.toInt()
                        else -> 0xFF1E1B4B.toInt()
                    }
                    pixels[y * w + x] = color
                } else {
                    pixels[y * w + x] = 0xFF09081E.toInt()
                }
            }
        }
    }

    /** Dian Keto: Sacred chalice of healing water */
    private fun drawDianKeto(pixels: IntArray, w: Int, h: Int) {
        fillGradientRadial(pixels, w, h, 0xFF065F46.toInt(), 0xFF021B14.toInt())
        val cx = w / 2
        val cy = h / 2

        // Golden Goblet
        drawTriangle(pixels, w, cx - 14, cy - 8, cx + 14, cy - 8, cx, cy + 12, 0xFFF59E0B.toInt())
        drawRect(pixels, w, cx - 2, cy + 10, 4, 10, 0xFFF59E0B.toInt()) // Stem
        drawCircle(pixels, w, cx, cy + 20, 8, 0xFFF59E0B.toInt()) // Base

        // Sacred Radiant Water
        drawCircle(pixels, w, cx, cy - 8, 10, 0xFF38BDF8.toInt())
        drawCircle(pixels, w, cx, cy - 8, 6, 0xFFBAE6FD.toInt())
        drawStar(pixels, w, cx, cy - 8, 4, 0xFFFFFFFF.toInt(), 0xFF38BDF8.toInt())
    }

    /** Sparks: Fiery burst of embers */
    private fun drawSparks(pixels: IntArray, w: Int, h: Int) {
        fillGradientRadial(pixels, w, h, 0xFF7C2D12.toInt(), 0xFF1A0702.toInt())
        val cx = w / 2
        val cy = h / 2

        drawCircle(pixels, w, cx, cy, 10, 0xFFEA580C.toInt())
        drawCircle(pixels, w, cx, cy, 6, 0xFFFDE047.toInt())
        drawCircle(pixels, w, cx, cy, 3, 0xFFFFFFFF.toInt())

        // Ray Sparks
        for (i in 0 until 8) {
            val ang = i * (Math.PI / 4.0)
            val sx = (cx + cos(ang) * 18).toInt()
            val sy = (cy + sin(ang) * 18).toInt()
            drawLine(pixels, w, cx, cy, sx, sy, 0xFFF97316.toInt())
            drawCircle(pixels, w, sx, sy, 2, 0xFFFDE047.toInt())
        }
    }

    /** Mirror Force: Iridescent crystalline barrier */
    private fun drawMirrorForce(pixels: IntArray, w: Int, h: Int) {
        fillGradientRadial(pixels, w, h, 0xFF1E1B4B.toInt(), 0xFF08061B.toInt())
        val cx = w / 2
        val cy = h / 2

        // Prismatic Hexagonal Shield
        drawCircleBorder(pixels, w, cx, cy, 22, 0xFFEC4899.toInt())
        drawCircleBorder(pixels, w, cx, cy, 18, 0xFF38BDF8.toInt())
        drawCircleBorder(pixels, w, cx, cy, 14, 0xFFFDE047.toInt())

        // Reflected Energy Beams
        drawLine(pixels, w, cx - 20, cy - 20, cx + 20, cy + 20, 0xFFFFFFFF.toInt())
        drawLine(pixels, w, cx + 20, cy - 20, cx - 20, cy + 20, 0xFFFFFFFF.toInt())
    }

    /** Trap Hole: Deep pit trap with stone border */
    private fun drawTrapHole(pixels: IntArray, w: Int, h: Int) {
        fillGradientRadial(pixels, w, h, 0xFF27272A.toInt(), 0xFF09090B.toInt())
        val cx = w / 2
        val cy = h / 2

        // Cobblestone Pit Entrance
        drawCircle(pixels, w, cx, cy, 20, 0xFF52525B.toInt())
        drawCircle(pixels, w, cx, cy, 16, 0xFF18181B.toInt())
        drawCircle(pixels, w, cx, cy, 10, 0xFF000000.toInt()) // Deep Void

        // Spikes at the bottom
        for (i in 0 until 6) {
            val sx = cx - 10 + (i * 4)
            drawLine(pixels, w, sx, cy + 4, sx, cy + 12, 0xFF71717A.toInt())
        }
    }

    /** Reinforcements: Winged warrior crest */
    private fun drawReinforcements(pixels: IntArray, w: Int, h: Int) {
        fillGradientRadial(pixels, w, h, 0xFF451A03.toInt(), 0xFF150801.toInt())
        val cx = w / 2
        val cy = h / 2

        // Crossed Golden Swords
        drawLine(pixels, w, cx - 18, cy - 18, cx + 18, cy + 18, 0xFFFDE047.toInt())
        drawLine(pixels, w, cx + 18, cy - 18, cx - 18, cy + 18, 0xFFFDE047.toInt())

        // Warrior Crest Shield
        drawCircle(pixels, w, cx, cy, 12, 0xFFDC2626.toInt())
        drawCircleBorder(pixels, w, cx, cy, 12, 0xFFF59E0B.toInt())
        drawStar(pixels, w, cx, cy, 5, 0xFFFFFFFF.toInt(), 0xFFF59E0B.toInt())
    }

    /** Exodia: Ancient Pharaoh with golden horned headpiece & broken chains */
    private fun drawExodia(pixels: IntArray, w: Int, h: Int) {
        fillGradientRadial(pixels, w, h, 0xFF3F2E05.toInt(), 0xFF120C01.toInt())
        val cx = w / 2
        val cy = h / 2

        // Pharaoh Gold Horns / Nemes Crown
        drawTriangle(pixels, w, cx - 16, cy - 18, cx + 16, cy - 18, cx, cy - 28, 0xFFF59E0B.toInt())
        drawTriangle(pixels, w, cx - 22, cy - 10, cx - 6, cy - 20, cx - 8, cy + 2, 0xFFD97706.toInt())
        drawTriangle(pixels, w, cx + 22, cy - 10, cx + 6, cy - 20, cx + 8, cy + 2, 0xFFD97706.toInt())

        // Golden Face
        drawCircle(pixels, w, cx, cy - 6, 9, 0xFFFDE047.toInt())
        drawCircle(pixels, w, cx - 3, cy - 6, 1, 0xFFDC2626.toInt()) // Red Eyes
        drawCircle(pixels, w, cx + 3, cy - 6, 1, 0xFFDC2626.toInt())

        // Broken Mystical Chains
        drawLine(pixels, w, cx - 24, cy + 12, cx - 8, cy + 16, 0xFFE2E8F0.toInt())
        drawLine(pixels, w, cx + 24, cy + 12, cx + 8, cy + 16, 0xFFE2E8F0.toInt())
    }

    /** Fallback Archetype Art Generator (Class-specific geometric monster emblems) */
    private fun drawArchetypeArt(pixels: IntArray, w: Int, h: Int, card: Card) {
        val baseColor = when (card.attribute) {
            CardAttribute.DARK -> 0xFF3B0764.toInt()
            CardAttribute.LIGHT -> 0xFF713F12.toInt()
            CardAttribute.FIRE -> 0xFF7F1D1D.toInt()
            CardAttribute.WATER -> 0xFF0C4A6E.toInt()
            CardAttribute.EARTH -> 0xFF451A03.toInt()
            CardAttribute.WIND -> 0xFF064E3B.toInt()
            CardAttribute.SPELL -> 0xFF065F46.toInt()
            CardAttribute.TRAP -> 0xFF4A044E.toInt()
            else -> 0xFF1E293B.toInt()
        }
        fillGradientRadial(pixels, w, h, baseColor, 0xFF090D16.toInt())

        val cx = w / 2
        val cy = h / 2

        // Primary Archetype Glyph
        when (card.monsterType) {
            MonsterType.DRAGON -> {
                drawTriangle(pixels, w, cx - 18, cy - 12, cx + 18, cy - 12, cx, cy + 16, 0xFFDC2626.toInt())
                drawTriangle(pixels, w, cx - 12, cy - 16, cx + 12, cy - 16, cx, cy - 26, 0xFFF59E0B.toInt())
            }
            MonsterType.SPELLCASTER -> {
                drawCircleBorder(pixels, w, cx, cy, 16, 0xFF8B5CF6.toInt())
                drawStar(pixels, w, cx, cy, 6, 0xFFE9D5FF.toInt(), 0xFF7C3AED.toInt())
            }
            MonsterType.WARRIOR -> {
                drawLine(pixels, w, cx - 14, cy - 18, cx + 14, cy + 18, 0xFFE2E8F0.toInt())
                drawLine(pixels, w, cx + 14, cy - 18, cx - 14, cy + 18, 0xFFE2E8F0.toInt())
                drawCircle(pixels, w, cx, cy, 8, 0xFFF59E0B.toInt())
            }
            MonsterType.FIEND, MonsterType.ZOMBIE -> {
                drawCircle(pixels, w, cx, cy - 4, 10, 0xFF7E22CE.toInt())
                drawLine(pixels, w, cx - 6, cy - 10, cx - 14, cy - 20, 0xFFE2E8F0.toInt())
                drawLine(pixels, w, cx + 6, cy - 10, cx + 14, cy - 20, 0xFFE2E8F0.toInt())
                drawCircle(pixels, w, cx - 3, cy - 4, 2, 0xFFEF4444.toInt())
                drawCircle(pixels, w, cx + 3, cy - 4, 2, 0xFFEF4444.toInt())
            }
            else -> {
                drawCircleBorder(pixels, w, cx, cy, 14, 0xFFD4AF37.toInt())
                drawStar(pixels, w, cx, cy, 5, 0xFFFDE047.toInt(), 0xFFB45309.toInt())
            }
        }
    }

    // =========================================================================
    // PRIMITIVE DRAWING UTILITIES
    // =========================================================================

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
        val totalH = pixels.size / w
        for (px in x until (x + rw)) {
            if (px in 0 until w) {
                if (y in 0 until totalH) pixels[y * w + px] = color
                val bY = y + rh - 1
                if (bY in 0 until totalH) pixels[bY * w + px] = color
            }
        }
        for (py in y until (y + rh)) {
            if (py in 0 until totalH) {
                if (x in 0 until w) pixels[py * w + x] = color
                val rX = x + rw - 1
                if (rX in 0 until w) pixels[py * w + rX] = color
            }
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
                if (px in 0 until w && (dx * dx + dy * dy <= rSq)) {
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
                    pixels[rowOffset + pxClamp(x, w)] = color
                }
            }
        }
    }

    private fun sign(p1x: Int, p1y: Int, p2x: Int, p2y: Int, p3x: Int, p3y: Int): Float {
        return ((p1x - p3x) * (p2y - p3y) - (p2x - p3x) * (p1y - p3y)).toFloat()
    }

    private fun drawLine(pixels: IntArray, w: Int, x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
        val totalH = pixels.size / w
        var cx0 = x0
        var cy0 = y0
        val dx = Math.abs(x1 - cx0)
        val dy = Math.abs(y1 - cy0)
        val sx = if (cx0 < x1) 1 else -1
        val sy = if (cy0 < y1) 1 else -1
        var err = dx - dy

        while (true) {
            if (cx0 in 0 until w && cy0 in 0 until totalH) {
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

    private fun fillGradientRadial(pixels: IntArray, w: Int, h: Int, centerColor: Int, edgeColor: Int) {
        val cx = w / 2.0
        val cy = h / 2.0
        val maxDist = hypot(cx, cy)

        val cr = (centerColor shr 16) and 0xFF
        val cg = (centerColor shr 8) and 0xFF
        val cb = centerColor and 0xFF

        val er = (edgeColor shr 16) and 0xFF
        val eg = (edgeColor shr 8) and 0xFF
        val eb = edgeColor and 0xFF

        for (y in 0 until h) {
            val rowOffset = y * w
            for (x in 0 until w) {
                val dist = (hypot(x - cx, y - cy) / maxDist).coerceIn(0.0, 1.0).toFloat()
                val r = (cr + (er - cr) * dist).toInt().coerceIn(0, 255)
                val g = (cg + (eg - cg) * dist).toInt().coerceIn(0, 255)
                val b = (cb + (eb - cb) * dist).toInt().coerceIn(0, 255)
                pixels[rowOffset + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
    }

    private fun brighten(color: Int, amount: Int): Int {
        val r = (((color shr 16) and 0xFF) + amount).coerceIn(0, 255)
        val g = (((color shr 8) and 0xFF) + amount).coerceIn(0, 255)
        val b = ((color and 0xFF) + amount).coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun pxClamp(x: Int, w: Int): Int = max(0, min(w - 1, x))
}
