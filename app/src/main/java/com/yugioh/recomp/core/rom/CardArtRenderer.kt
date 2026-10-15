package com.yugioh.recomp.core.rom

import com.yugioh.recomp.core.engine.*
import kotlin.math.*

/**
 * High-fidelity Game Boy Color pixel-art renderer and integer upscaler for Duel-decomp.
 * Generates authentic retro pixel art for cards matching Yu-Gi-Oh! Dark Duel Stories (GBC),
 * with nearest-neighbor integer scaling to preserve razor-sharp pixel art on mobile displays.
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

        // 6. Text Box (Parchment/Spell/Trap)
        val textTop = artTop + artHeight + 5
        val textHeight = height - textTop - 8
        val textLeft = 10
        val textWidth = width - 20
        drawRect(pixels, width, textLeft, textTop, textWidth, textHeight, textBoxBg)
        drawRectBorder(pixels, width, textLeft, textTop, textWidth, textHeight, COLOR_CARD_BORDER_OUTER)

        // Text Box Header (Monster Type or Card Type)
        val typeBarHeight = 11
        drawRect(pixels, width, textLeft + 2, textTop + 2, textWidth - 4, typeBarHeight, 0x22000000)

        // ATK / DEF Display Bar at bottom of text box for monsters
        if (card.isMonster) {
            val statTop = textTop + textHeight - 14
            drawRect(pixels, width, textLeft + 2, statTop, textWidth - 4, 12, 0x33000000)
            // Visual divider for stats
            drawLine(pixels, width, textLeft + (textWidth / 2), statTop + 1, textLeft + (textWidth / 2), statTop + 11, 0xFF785B28.toInt())
            // Red ATK dot
            drawCircle(pixels, width, textLeft + 8, statTop + 6, 2, 0xFFDC2626.toInt())
            // Blue DEF dot
            drawCircle(pixels, width, textLeft + (textWidth / 2) + 8, statTop + 6, 2, 0xFF2563EB.toInt())
        }

        // 7. Hologram Foil Stamp (Bottom-Right)
        val holoSize = 8
        val holoX = width - 14
        val holoY = height - 12
        drawRect(pixels, width, holoX, holoY, holoSize, holoSize, 0xFFF59E0B.toInt())
        drawCircle(pixels, width, holoX + 3, holoY + 3, 2, 0xFFFFFFFF.toInt())

        return pixels
    }

    /**
     * Authentic Yu-Gi-Oh! Card Back texture.
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
                    val spiral = sin(dist * 12.0 + atan2(y - cy, x - cx) * 3.0)
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

    /**
     * Authentic GBC Pixel-Art Card Illustration Renderer.
     * Generates rich, authentic pixel art matching the original Game Boy Color DDS sprites.
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

        // Draw card-specific authentic pixel art
        when (card.name) {
            "Gale Dogra" -> {
                drawGaleDogra(pixels, width, height)
            }
            "Zone Eater" -> {
                drawZoneEater(pixels, width, height)
            }
            "Leghul" -> {
                drawLeghul(pixels, width, height)
            }
            "Shadow Specter" -> {
                drawShadowSpecter(pixels, width, height)
            }
            "Wretched Ghost of the Attic" -> {
                drawWretchedGhost(pixels, width, height)
            }
            "Blue-Eyes White Dragon" -> {
                drawBlueEyesWhiteDragon(pixels, width, height)
            }
            "Dark Magician" -> {
                drawDarkMagician(pixels, width, height)
            }
            "Red-Eyes B. Dragon" -> {
                drawRedEyesBlackDragon(pixels, width, height)
            }
            "Summoned Skull" -> {
                drawSummonedSkull(pixels, width, height)
            }
            "Celtic Guardian" -> {
                drawCelticGuardian(pixels, width, height)
            }
            "Curse of Dragon" -> {
                drawCurseOfDragon(pixels, width, height)
            }
            "Mystical Elf" -> {
                drawMysticalElf(pixels, width, height)
            }
            "Pot of Greed" -> {
                drawPotOfGreed(pixels, width, height)
            }
            "Raigeki" -> {
                drawRaigeki(pixels, width, height)
            }
            "Dark Hole" -> {
                drawDarkHole(pixels, width, height)
            }
            "Sparks" -> {
                drawSparks(pixels, width, height)
            }
            "Hinotama" -> {
                drawHinotama(pixels, width, height)
            }
            "Mooyan Curry" -> {
                drawMooyanCurry(pixels, width, height)
            }
            "Dian Keto the Cure Maiden" -> {
                drawDianKeto(pixels, width, height)
            }
            "Mirror Force" -> {
                drawMirrorForce(pixels, width, height)
            }
            "Trap Hole" -> {
                drawTrapHole(pixels, width, height)
            }
            "Waboku" -> {
                drawWaboku(pixels, width, height)
            }
            "Reinforcements" -> {
                drawReinforcements(pixels, width, height)
            }
            "Mech Mole Zombie" -> {
                drawMechMoleZombie(pixels, width, height)
            }
            "Air Marmot of Nefariousness" -> {
                drawAirMarmot(pixels, width, height)
            }
            "Droll Bird" -> {
                drawDrollBird(pixels, width, height)
            }
            "Kaminarikozou" -> {
                drawKaminarikozou(pixels, width, height)
            }
            "Swordsman from a Foreign Land" -> {
                drawSwordsmanForeign(pixels, width, height)
            }
            "Monster Eye" -> {
                drawMonsterEye(pixels, width, height)
            }
            "Mechanical Spider" -> {
                drawMechanicalSpider(pixels, width, height)
            }
            "Cyber-Stein" -> {
                drawCyberStein(pixels, width, height)
            }
            "Jinzo #7" -> {
                drawJinzo7(pixels, width, height)
            }
            "White Dolphin" -> {
                drawWhiteDolphin(pixels, width, height)
            }
            "Turu-Purun" -> {
                drawTuruPurun(pixels, width, height)
            }
            "Sinister Serpent" -> {
                drawSinisterSerpent(pixels, width, height)
            }
            "Boo Koo" -> {
                drawBooKoo(pixels, width, height)
            }
            "Fungi of the Musk" -> {
                drawFungiMusk(pixels, width, height)
            }
            "Muka Muka" -> {
                drawMukaMuka(pixels, width, height)
            }
            "Star Boy" -> {
                drawStarBoy(pixels, width, height)
            }
            "Milus Radiant" -> {
                drawMilusRadiant(pixels, width, height)
            }
            "Bone Mouse" -> {
                drawBoneMouse(pixels, width, height)
            }
            "Frog The Jam" -> {
                drawFrogTheJam(pixels, width, height)
            }
            "Abyss Flower" -> {
                drawAbyssFlower(pixels, width, height)
            }
            "Bladefly" -> {
                drawBladefly(pixels, width, height)
            }
            "Hiro's Shadow Scout" -> {
                drawHiroShadowScout(pixels, width, height)
            }
            "Yamatano Dragon Scroll" -> {
                drawYamatanoDragon(pixels, width, height)
            }
            "Mystery Hand" -> {
                drawMysteryHand(pixels, width, height)
            }
            "Masked Clown" -> {
                drawMaskedClown(pixels, width, height)
            }
            "Oscillo Hero #2" -> {
                drawOscilloHero2(pixels, width, height)
            }
            "Swordsman of Landstar" -> {
                drawSwordsmanLandstar(pixels, width, height)
            }
            "Mountain Warrior" -> {
                drawMountainWarrior(pixels, width, height)
            }
            "Time Wizard" -> {
                drawTimeWizard(pixels, width, height)
            }
            "Harpie Lady" -> {
                drawHarpieLady(pixels, width, height)
            }
            "Sonic Maid" -> {
                drawSonicMaid(pixels, width, height)
            }
            "White Magical Hat" -> {
                drawWhiteMagicalHat(pixels, width, height)
            }
            "Morphing Jar" -> {
                drawMorphingJar(pixels, width, height)
            }
            "Battle Ox" -> {
                drawBattleOx(pixels, width, height)
            }
            "Hitotsu-Me Giant" -> {
                drawHitotsuMeGiant(pixels, width, height)
            }
            "Ryu-Kishin" -> {
                drawRyuKishin(pixels, width, height)
            }
            "Kuriboh" -> {
                drawKuriboh(pixels, width, height)
            }
            "Twin-Headed Thunder Dragon" -> {
                drawTwinHeadedThunderDragon(pixels, width, height)
            }
            "Zombie Dragon" -> {
                drawZombieDragon(pixels, width, height)
            }
            "Flame Cerebrus" -> {
                drawFlameCerebrus(pixels, width, height)
            }
            "Thousand Dragon" -> {
                drawThousandDragon(pixels, width, height)
            }
            "Cyber Soldier" -> {
                drawCyberSoldier(pixels, width, height)
            }
            else -> {
                // Archetype fallback based on monster type
                when {
                    card.isSpell -> drawSpellGlyph(pixels, width, height, colorLight, colorMid2)
                    card.isTrap -> drawTrapGlyph(pixels, width, height, colorLight, colorMid2)
                    card.name.contains("Dragon") -> drawDragonSprite(pixels, width, height, colorLight, colorMid2, colorDark)
                    card.name.contains("Magician") || card.name.contains("Wizard") -> drawSpellcasterSprite(pixels, width, height, colorLight, colorMid2, colorDark)
                    card.name.contains("Skull") || card.name.contains("Specter") || card.name.contains("Ghost") -> drawSkullSprite(pixels, width, height, colorLight, colorMid2, colorDark)
                    card.name.contains("Warrior") || card.name.contains("Swordsman") -> drawWarriorSprite(pixels, width, height, colorLight, colorMid2, colorDark)
                    card.name.contains("Bird") || card.name.contains("Winged") -> drawBirdSprite(pixels, width, height, colorLight, colorMid2, colorDark)
                    else -> drawGenericMonsterSprite(pixels, width, height, colorLight, colorMid2, colorDark)
                }
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

    // =========================================================================
    // AUTHENTIC GBC DDS PIXEL ART SPRITES
    // =========================================================================

    /**
     * Gale Dogra: Authentic GBC pixel art matching Dark Duel Stories.
     * Praying mantis / moth creature with green segmented abdomen, menacing compound red eyes,
     * triangular head, mandibles, long antennae, and magnificent purple/violet wings spread wide
     * against a cosmic starry/dithered void.
     */
    private fun drawGaleDogra(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2

        // 1. Cosmic Void Background with Purple Dither Dots
        val bgDark = 0xFF110722.toInt()
        val bgDither1 = 0xFF3B1E54.toInt()
        val bgDither2 = 0xFF8B5CF6.toInt()
        val bgStar = 0xFFE9D5FF.toInt()

        p.fill(bgDark)
        for (y in 0 until h) {
            for (x in 0 until w) {
                if ((x + y) % 6 == 0 && (x * y) % 5 == 0) {
                    p[y * w + x] = bgDither1
                } else if ((x * 3 + y * 7) % 23 == 0) {
                    p[y * w + x] = bgDither2
                }
            }
        }
        // Mystic Wind Stars
        val stars = listOf(Pair(cx - 20, cy - 18), Pair(cx + 22, cy - 20), Pair(cx - 16, cy + 18), Pair(cx + 18, cy + 16))
        for (s in stars) {
            if (s.first in 0 until w && s.second in 0 until h) {
                p[s.second * w + s.first] = bgStar
            }
        }

        // 2. Magnificent Spread Wings (Purple / Magenta / Violet)
        val wingDark = 0xFF3B1E54.toInt()
        val wingMid = 0xFF7E22CE.toInt()
        val wingLight = 0xFFC084FC.toInt()
        val wingEdge = 0xFFE9D5FF.toInt()

        // Left Wing Fan
        drawTriangle(p, w, cx - 2, cy - 2, cx - 26, cy - 18, cx - 18, cy + 8, wingDark)
        drawTriangle(p, w, cx - 4, cy - 4, cx - 24, cy - 16, cx - 14, cy + 4, wingMid)
        drawTriangle(p, w, cx - 6, cy - 6, cx - 20, cy - 14, cx - 12, cy, wingLight)
        drawLine(p, w, cx - 4, cy - 4, cx - 26, cy - 18, wingEdge)
        drawLine(p, w, cx - 6, cy - 2, cx - 22, cy - 8, wingLight)

        // Right Wing Fan
        drawTriangle(p, w, cx + 2, cy - 2, cx + 26, cy - 18, cx + 18, cy + 8, wingDark)
        drawTriangle(p, w, cx + 4, cy - 4, cx + 24, cy - 16, cx + 14, cy + 4, wingMid)
        drawTriangle(p, w, cx + 6, cy - 6, cx + 20, cy - 14, cx + 12, cy, wingLight)
        drawLine(p, w, cx + 4, cy - 4, cx + 26, cy - 18, wingEdge)
        drawLine(p, w, cx + 6, cy - 2, cx + 22, cy - 8, wingLight)

        // Lower Winglets
        drawTriangle(p, w, cx - 4, cy + 4, cx - 18, cy + 16, cx - 8, cy + 14, wingMid)
        drawTriangle(p, w, cx + 4, cy + 4, cx + 18, cy + 16, cx + 8, cy + 14, wingMid)

        // 3. Green Segmented Mantis Body
        val mantisDark = 0xFF052E16.toInt()
        val mantisMid = 0xFF15803D.toInt()
        val mantisLight = 0xFF22C55E.toInt()
        val mantisHighlight = 0xFF86EFAC.toInt()

        // Curled Ribbed Abdomen (segmented with horizontal stripes)
        val abX = cx
        val abY = cy + 10
        drawCircle(p, w, abX, abY, 8, mantisMid)
        drawCircle(p, w, abX - 1, abY + 6, 6, mantisMid)
        drawCircle(p, w, abX - 3, abY + 11, 4, mantisDark) // curled tip
        // Abdomen Segment Ribs
        for (i in 0 until 4) {
            val ry = abY - 4 + (i * 4)
            drawLine(p, w, abX - 6 + i, ry, abX + 6 - i, ry, mantisLight)
            drawLine(p, w, abX - 4 + i, ry + 1, abX + 4 - i, ry + 1, mantisDark)
        }

        // Thorax & Neck
        drawRect(p, w, cx - 3, cy - 4, 6, 8, mantisMid)
        drawRect(p, w, cx - 2, cy - 3, 4, 6, mantisLight)

        // Triangular Mantis Head
        drawTriangle(p, w, cx - 7, cy - 8, cx + 7, cy - 8, cx, cy - 2, mantisMid)
        drawTriangle(p, w, cx - 5, cy - 8, cx + 5, cy - 8, cx, cy - 4, mantisLight)

        // Compound Red Eyes
        drawCircle(p, w, cx - 4, cy - 8, 2, 0xFFDC2626.toInt())
        drawCircle(p, w, cx + 4, cy - 8, 2, 0xFFDC2626.toInt())
        p[(cy - 8) * w + (cx - 4)] = 0xFFFFFFFF.toInt() // eye sparkle
        p[(cy - 8) * w + (cx + 4)] = 0xFFFFFFFF.toInt()

        // Curved Mandibles
        drawLine(p, w, cx - 2, cy - 2, cx - 1, cy, 0xFFFDE047.toInt())
        drawLine(p, w, cx + 2, cy - 2, cx + 1, cy, 0xFFFDE047.toInt())

        // Antennae (Long sweeping backward)
        drawLine(p, w, cx - 3, cy - 10, cx - 8, cy - 22, mantisHighlight)
        drawLine(p, w, cx - 8, cy - 22, cx - 12, cy - 24, mantisHighlight)
        drawLine(p, w, cx + 3, cy - 10, cx + 8, cy - 22, mantisHighlight)
        drawLine(p, w, cx + 8, cy - 22, cx + 12, cy - 24, mantisHighlight)

        // Raptorial Forelegs (Mantis Spurred Prayer Blades)
        drawLine(p, w, cx - 3, cy - 3, cx - 9, cy + 3, mantisDark)
        drawLine(p, w, cx - 9, cy + 3, cx - 4, cy + 7, mantisLight)
        drawLine(p, w, cx + 3, cy - 3, cx + 9, cy + 3, mantisDark)
        drawLine(p, w, cx + 9, cy + 3, cx + 4, cy + 7, mantisLight)
    }

    /** Zone Eater: Microscopic azure aquatic parasite with jagged maw of teeth */
    private fun drawZoneEater(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF04182B.toInt())
        // Aquatic energy ring
        drawCircleBorder(p, w, cx, cy, 22, 0xFF0284C7.toInt())
        drawCircle(p, w, cx, cy, 16, 0xFF0369A1.toInt())
        drawCircle(p, w, cx, cy, 11, 0xFF021020.toInt()) // Void maw
        // Sharp ivory fangs ring
        for (i in 0 until 8) {
            val ang = i * (Math.PI / 4.0)
            val fx = (cx + cos(ang) * 12).toInt()
            val fy = (cy + sin(ang) * 12).toInt()
            drawLine(p, w, fx, fy, cx, cy, 0xFFF8FAFC.toInt())
        }
        // Glowing cyan tentacles
        for (i in 0 until 6) {
            val ang = (i * 60) * (Math.PI / 180.0)
            val tx1 = (cx + cos(ang) * 16).toInt()
            val ty1 = (cy + sin(ang) * 16).toInt()
            val tx2 = (cx + cos(ang + 0.3) * 26).toInt()
            val ty2 = (cy + sin(ang + 0.3) * 26).toInt()
            drawLine(p, w, tx1, ty1, tx2, ty2, 0xFF38BDF8.toInt())
        }
    }

    /** Leghul: Forest insect with emerald carapace and curved horns */
    private fun drawLeghul(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF031A0D.toInt())
        drawCircle(p, w, cx, cy + 6, 14, 0xFF059669.toInt())
        drawCircle(p, w, cx, cy + 6, 11, 0xFF10B981.toInt())
        drawCircle(p, w, cx, cy - 8, 8, 0xFF047857.toInt())
        // Horns
        drawLine(p, w, cx - 4, cy - 12, cx - 12, cy - 22, 0xFFFDE047.toInt())
        drawLine(p, w, cx + 4, cy - 12, cx + 12, cy - 22, 0xFFFDE047.toInt())
        // Golden eyes
        drawCircle(p, w, cx - 3, cy - 8, 2, 0xFFFACC15.toInt())
        drawCircle(p, w, cx + 3, cy - 8, 2, 0xFFFACC15.toInt())
    }

    /** Shadow Specter: Spooky purple hooded phantom with glowing yellow eyes */
    private fun drawShadowSpecter(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF0F0B18.toInt())
        // Swirling phantom mist
        drawCircle(p, w, cx, cy + 12, 16, 0xFF2E1065.toInt())
        // Hood
        drawTriangle(p, w, cx - 14, cy + 10, cx + 14, cy + 10, cx, cy - 16, 0xFF581C87.toInt())
        drawTriangle(p, w, cx - 10, cy + 6, cx + 10, cy + 6, cx, cy - 12, 0xFF7E22CE.toInt())
        // Face Void
        drawCircle(p, w, cx, cy - 2, 6, 0xFF000000.toInt())
        // Piercing Yellow Specter Eyes
        drawCircle(p, w, cx - 3, cy - 2, 2, 0xFFFACC15.toInt())
        drawCircle(p, w, cx + 3, cy - 2, 2, 0xFFFACC15.toInt())
    }

    /** Wretched Ghost of the Attic: Pale spooky attic wraith with wide jagged grin */
    private fun drawWretchedGhost(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF140E0A.toInt())
        // Ghostly white body
        drawCircle(p, w, cx, cy - 4, 15, 0xFFE2E8F0.toInt())
        drawTriangle(p, w, cx - 12, cy, cx + 12, cy, cx, cy + 22, 0xFFCBD5E1.toInt())
        // Hollow Black Eyes
        drawCircle(p, w, cx - 5, cy - 6, 3, 0xFF0F172A.toInt())
        drawCircle(p, w, cx + 5, cy - 6, 3, 0xFF0F172A.toInt())
        // Sinister Jagged Grin
        drawLine(p, w, cx - 7, cy + 3, cx + 7, cy + 3, 0xFF0F172A.toInt())
        for (i in 0 until 4) {
            val tx = cx - 5 + (i * 3)
            drawLine(p, w, tx, cy + 1, tx, cy + 5, 0xFFEF4444.toInt())
        }
    }

    /** Blue-Eyes White Dragon: Radiant silver dragon with piercing blue eyes */
    private fun drawBlueEyesWhiteDragon(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF0A1526.toInt())
        drawCircle(p, w, cx, cy, 22, 0x443B82F6.toInt())
        // Silver Wings
        drawTriangle(p, w, cx - 2, cy, cx - 26, cy - 18, cx - 16, cy + 10, 0xFFE2E8F0.toInt())
        drawTriangle(p, w, cx + 2, cy, cx + 26, cy - 18, cx + 18, cy + 10, 0xFFCBD5E1.toInt())
        // Head & Horns
        drawCircle(p, w, cx, cy - 12, 8, 0xFFF8FAFC.toInt())
        drawLine(p, w, cx - 4, cy - 14, cx - 14, cy - 24, 0xFFE2E8F0.toInt())
        drawLine(p, w, cx + 4, cy - 14, cx + 14, cy - 24, 0xFFE2E8F0.toInt())
        // Piercing Cyan Eyes
        drawCircle(p, w, cx - 3, cy - 12, 2, 0xFF00E5FF.toInt())
        drawCircle(p, w, cx + 3, cy - 12, 2, 0xFF00E5FF.toInt())
    }

    /** Dark Magician: Sorcerer in purple robes with staff */
    private fun drawDarkMagician(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF160628.toInt())
        drawCircleBorder(p, w, cx, cy, 20, 0xFF10B981.toInt())
        // Robe & Hat
        drawTriangle(p, w, cx - 12, cy + 20, cx + 12, cy + 20, cx, cy - 2, 0xFF6B21A8.toInt())
        drawTriangle(p, w, cx - 10, cy - 8, cx + 10, cy - 8, cx, cy - 26, 0xFF581C87.toInt())
        drawCircle(p, w, cx, cy - 6, 5, 0xFFFFDFBA.toInt())
        // Emerald Staff
        val stX = cx + 16
        drawLine(p, w, stX, cy - 20, stX - 6, cy + 20, 0xFFD4AF37.toInt())
        drawCircle(p, w, stX, cy - 20, 4, 0xFF10B981.toInt())
    }

    /** Red-Eyes B. Dragon: Menacing black dragon with ruby eyes */
    private fun drawRedEyesBlackDragon(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF1E0606.toInt())
        drawTriangle(p, w, cx - 2, cy, cx - 24, cy - 16, cx - 14, cy + 10, 0xFF1F2937.toInt())
        drawTriangle(p, w, cx + 2, cy, cx + 24, cy - 16, cx + 18, cy + 10, 0xFF111827.toInt())
        drawCircle(p, w, cx, cy - 10, 7, 0xFF1F2937.toInt())
        drawLine(p, w, cx - 4, cy - 12, cx - 12, cy - 22, 0xFF374151.toInt())
        drawLine(p, w, cx + 4, cy - 12, cx + 12, cy - 22, 0xFF374151.toInt())
        // Ruby Red Eyes
        drawCircle(p, w, cx - 3, cy - 10, 2, 0xFFEF4444.toInt())
        drawCircle(p, w, cx + 3, cy - 10, 2, 0xFFEF4444.toInt())
    }

    /** Summoned Skull: Horned demon skeleton with lightning aura */
    private fun drawSummonedSkull(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF190624.toInt())
        drawLine(p, w, cx - 6, cy - 10, cx - 20, cy - 22, 0xFFE2E8F0.toInt())
        drawLine(p, w, cx + 6, cy - 10, cx + 20, cy - 22, 0xFFE2E8F0.toInt())
        drawCircle(p, w, cx, cy - 6, 8, 0xFFF8FAFC.toInt())
        drawCircle(p, w, cx - 3, cy - 6, 2, 0xFF831843.toInt())
        drawCircle(p, w, cx + 3, cy - 6, 2, 0xFF831843.toInt())
        // Ribcage
        for (i in 0 until 4) {
            val ry = cy + 4 + (i * 4)
            drawLine(p, w, cx - 8 + i, ry, cx + 8 - i, ry, 0xFFE2E8F0.toInt())
        }
    }

    /** Celtic Guardian: Elven swordsman with broadsword */
    private fun drawCelticGuardian(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF042018.toInt())
        drawTriangle(p, w, cx - 10, cy + 20, cx + 10, cy + 20, cx, cy, 0xFF059669.toInt())
        drawCircle(p, w, cx, cy - 8, 6, 0xFFFFDFBA.toInt())
        drawTriangle(p, w, cx - 7, cy - 8, cx + 7, cy - 8, cx, cy - 16, 0xFFFCD34D.toInt())
        // Sword
        val swX = cx + 14
        drawLine(p, w, swX, cy - 18, swX, cy + 18, 0xFFE2E8F0.toInt())
        drawLine(p, w, swX - 4, cy + 8, swX + 4, cy + 8, 0xFFF59E0B.toInt())
    }

    /** Curse of Dragon: Skeletal winged bone dragon */
    private fun drawCurseOfDragon(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF1E1004.toInt())
        drawTriangle(p, w, cx - 2, cy, cx - 24, cy - 14, cx - 14, cy + 10, 0xFF785B28.toInt())
        drawTriangle(p, w, cx + 2, cy, cx + 24, cy - 14, cx + 14, cy + 10, 0xFF785B28.toInt())
        drawCircle(p, w, cx, cy - 10, 7, 0xFFD4AF37.toInt())
        drawCircle(p, w, cx - 3, cy - 10, 2, 0xFFEF4444.toInt())
        drawCircle(p, w, cx + 3, cy - 10, 2, 0xFFEF4444.toInt())
    }

    /** Mystical Elf: Delicate elf priestess */
    private fun drawMysticalElf(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF062036.toInt())
        drawCircleBorder(p, w, cx, cy - 8, 14, 0xFFFDE047.toInt())
        drawTriangle(p, w, cx - 12, cy + 20, cx + 12, cy + 20, cx, cy, 0xFFF8FAFC.toInt())
        drawCircle(p, w, cx, cy - 6, 6, 0xFFFFDFBA.toInt())
        drawTriangle(p, w, cx - 7, cy - 4, cx + 7, cy - 4, cx, cy - 14, 0xFF38BDF8.toInt())
    }

    /** Pot of Greed: Grinning green earthenware jar with golden teeth */
    private fun drawPotOfGreed(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF021B14.toInt())
        drawCircle(p, w, cx, cy + 4, 18, 0xFF047857.toInt())
        drawCircle(p, w, cx, cy + 4, 14, 0xFF10B981.toInt())
        drawRect(p, w, cx - 12, cy - 14, 24, 5, 0xFFDC2626.toInt())
        // Maniacal Grin
        drawTriangle(p, w, cx - 10, cy + 4, cx + 10, cy + 4, cx, cy + 14, 0xFF0F172A.toInt())
        for (i in 0 until 5) {
            val tx = cx - 8 + (i * 4)
            drawRect(p, w, tx, cy + 4, 2, 3, 0xFFFDE047.toInt())
        }
        drawCircle(p, w, cx - 5, cy - 4, 3, 0xFFDC2626.toInt())
        drawCircle(p, w, cx + 5, cy - 4, 3, 0xFFDC2626.toInt())
    }

    /** Raigeki: Golden thunderbolt striking */
    private fun drawRaigeki(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        p.fill(0xFF2E0808.toInt())
        val path = listOf(Pair(cx, 8), Pair(cx - 8, 20), Pair(cx + 4, 22), Pair(cx - 6, 36), Pair(cx + 4, 38), Pair(cx, h - 6))
        for (i in 0 until path.size - 1) {
            val p1 = path[i]
            val p2 = path[i + 1]
            drawLine(p, w, p1.first, p1.second, p2.first, p2.second, 0xFFFFFFFF.toInt())
            drawLine(p, w, p1.first + 1, p1.second, p2.first + 1, p2.second, 0xFFFDE047.toInt())
        }
        drawCircle(p, w, cx, h - 6, 7, 0xFFF59E0B.toInt())
    }

    /** Dark Hole: Cosmic black hole vortex */
    private fun drawDarkHole(p: IntArray, w: Int, h: Int) {
        val cx = w / 2.0
        val cy = h / 2.0
        val maxR = w * 0.44
        for (y in 0 until h) {
            for (x in 0 until w) {
                val dist = hypot(x - cx, y - cy)
                if (dist <= maxR) {
                    val spiral = sin(dist * 0.4 + atan2(y - cy, x - cx) * 3.0)
                    p[y * w + x] = when {
                        dist < maxR * 0.3 -> 0xFF000000.toInt()
                        spiral > 0.2 -> 0xFF7C3AED.toInt()
                        else -> 0xFF3B0764.toInt()
                    }
                } else {
                    p[y * w + x] = 0xFF090616.toInt()
                }
            }
        }
    }

    /** Sparks: Burst of fiery embers */
    private fun drawSparks(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF240A04.toInt())
        drawCircle(p, w, cx, cy, 10, 0xFFEA580C.toInt())
        drawCircle(p, w, cx, cy, 5, 0xFFFDE047.toInt())
        for (i in 0 until 8) {
            val ang = i * (Math.PI / 4.0)
            val sx = (cx + cos(ang) * 16).toInt()
            val sy = (cy + sin(ang) * 16).toInt()
            drawLine(p, w, cx, cy, sx, sy, 0xFFF97316.toInt())
        }
    }

    /** Hinotama: Streaking fireball meteor */
    private fun drawHinotama(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF260505.toInt())
        // Flaming tail
        drawLine(p, w, cx - 18, cy - 18, cx, cy, 0xFFEF4444.toInt())
        drawLine(p, w, cx - 14, cy - 22, cx + 4, cy, 0xFFF97316.toInt())
        // Fireball Core
        drawCircle(p, w, cx + 4, cy + 4, 12, 0xFFEA580C.toInt())
        drawCircle(p, w, cx + 4, cy + 4, 8, 0xFFFDE047.toInt())
        drawCircle(p, w, cx + 4, cy + 4, 4, 0xFFFFFFFF.toInt())
    }

    /** Mooyan Curry: Steaming fragrant bowl of curry */
    private fun drawMooyanCurry(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF1E1408.toInt())
        // Bowl
        drawCircle(p, w, cx, cy + 6, 16, 0xFFF8FAFC.toInt())
        drawCircle(p, w, cx, cy + 4, 13, 0xFFD97706.toInt())
        // Steam wisps
        drawLine(p, w, cx - 6, cy - 6, cx - 3, cy - 14, 0xFFE2E8F0.toInt())
        drawLine(p, w, cx + 6, cy - 6, cx + 3, cy - 14, 0xFFE2E8F0.toInt())
    }

    /** Dian Keto: Sacred golden chalice */
    private fun drawDianKeto(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF041E16.toInt())
        drawTriangle(p, w, cx - 12, cy - 6, cx + 12, cy - 6, cx, cy + 12, 0xFFF59E0B.toInt())
        drawRect(p, w, cx - 2, cy + 10, 4, 8, 0xFFF59E0B.toInt())
        drawCircle(p, w, cx, cy + 18, 7, 0xFFF59E0B.toInt())
        drawCircle(p, w, cx, cy - 6, 8, 0xFF38BDF8.toInt())
    }

    /** Mirror Force: Prismatic hexagonal shield */
    private fun drawMirrorForce(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF0E0824.toInt())
        drawCircleBorder(p, w, cx, cy, 20, 0xFFEC4899.toInt())
        drawCircleBorder(p, w, cx, cy, 15, 0xFF38BDF8.toInt())
        drawCircleBorder(p, w, cx, cy, 10, 0xFFFDE047.toInt())
        drawLine(p, w, cx - 18, cy - 18, cx + 18, cy + 18, 0xFFFFFFFF.toInt())
        drawLine(p, w, cx + 18, cy - 18, cx - 18, cy + 18, 0xFFFFFFFF.toInt())
    }

    /** Trap Hole: Spiked stone pit trap */
    private fun drawTrapHole(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF121214.toInt())
        drawCircle(p, w, cx, cy, 18, 0xFF52525B.toInt())
        drawCircle(p, w, cx, cy, 13, 0xFF000000.toInt())
        for (i in 0 until 5) {
            val sx = cx - 8 + (i * 4)
            drawLine(p, w, sx, cy + 2, sx, cy + 10, 0xFF71717A.toInt())
        }
    }

    /** Waboku: Sacred maidens radiating golden peace light */
    private fun drawWaboku(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF1E1404.toInt())
        drawCircleBorder(p, w, cx, cy, 22, 0xFFFDE047.toInt())
        drawCircle(p, w, cx, cy, 16, 0x55F59E0B.toInt())
        drawTriangle(p, w, cx - 10, cy + 16, cx + 10, cy + 16, cx, cy - 4, 0xFFF8FAFC.toInt())
        drawCircle(p, w, cx, cy - 8, 5, 0xFFFFDFBA.toInt())
    }

    /** Reinforcements: Winged warrior crest */
    private fun drawReinforcements(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF1E0E04.toInt())
        drawLine(p, w, cx - 16, cy - 16, cx + 16, cy + 16, 0xFFFDE047.toInt())
        drawLine(p, w, cx + 16, cy - 16, cx - 16, cy + 16, 0xFFFDE047.toInt())
        drawCircle(p, w, cx, cy, 10, 0xFFDC2626.toInt())
        drawCircleBorder(p, w, cx, cy, 10, 0xFFF59E0B.toInt())
    }

    // --- OTHER DDS MONSTERS ---

    private fun drawMechMoleZombie(pixels: IntArray, w: Int, h: Int) {
        val cyanBg = 0xFF00B0F0.toInt()
        val darkCyan = 0xFF0070B8.toInt()
        val sparkleWhite = 0xFFFFFFFF.toInt()
        val black = 0xFF000000.toInt()
        val moleDark = 0xFF581515.toInt()
        val moleMid = 0xFF882424.toInt()
        val mouthCavity = 0xFF280404.toInt()
        val toothWhite = 0xFFF0F8FF.toInt()
        val goldMain = 0xFFE6B800.toInt()
        val goldLight = 0xFFFFF066.toInt()
        val goldDark = 0xFF997A00.toInt()
        val armorRed = 0xFFDC2626.toInt()
        val armorDarkRed = 0xFF991B1B.toInt()
        val armorWhiteTrim = 0xFFF8FAFC.toInt()
        val metalGrey = 0xFF64748B.toInt()

        // 1. Cyan Electric Sky Background with retro GBC dither dots
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                pixels[row + x] = if ((x * 7 + y * 13) % 19 == 0) darkCyan else cyanBg
            }
        }
        // Background star sparkles
        pixels[((h * 0.15).toInt() * w) + (w * 0.25).toInt()] = sparkleWhite
        pixels[((h * 0.20).toInt() * w) + (w * 0.15).toInt()] = sparkleWhite
        pixels[((h * 0.35).toInt() * w) + (w * 0.10).toInt()] = sparkleWhite
        pixels[((h * 0.10).toInt() * w) + (w * 0.85).toInt()] = sparkleWhite

        // 2. Red Chest Armor / Torso (Lower Section)
        val chestTop = (h * 0.65).toInt()
        val chestBottom = h - 1
        for (y in chestTop..chestBottom) {
            val prog = (y - chestTop).toFloat() / (chestBottom - chestTop)
            val left = (w * (0.15 - 0.05 * prog)).toInt().coerceIn(0, w - 1)
            val right = (w * (0.80 + 0.10 * prog)).toInt().coerceIn(0, w - 1)
            for (x in left..right) {
                val isRim = (y == chestTop || y == chestTop + 1 || x == left || x == right)
                pixels[y * w + x] = when {
                    isRim -> black
                    y < chestTop + 4 -> armorWhiteTrim
                    x < w * 0.5 -> armorRed
                    else -> armorDarkRed
                }
            }
        }

        // 3. Cybernetic Mole Head & Snarling Jaw (Left/Center)
        val headCx = (w * 0.40).toInt()
        val headCy = (h * 0.45).toInt()
        drawCircle(pixels, w, headCx, headCy, (w * 0.28).toInt(), moleDark)
        drawCircle(pixels, w, headCx + 2, headCy - 2, (w * 0.24).toInt(), moleMid)

        // Head mechanical brow / metal headband
        drawLine(pixels, w, headCx - 10, headCy - 8, headCx + 8, headCy - 12, metalGrey)
        drawLine(pixels, w, headCx - 10, headCy - 7, headCx + 8, headCy - 11, metalGrey)

        // Glowing red optic / cyborg eye
        drawCircle(pixels, w, headCx - 4, headCy - 6, 2, 0xFFFF2222.toInt())
        pixels[(headCy - 6) * w + (headCx - 4)] = sparkleWhite

        // Open Snarling Mouth (Dark Cavity + Sharp Fangs)
        val mouthX = headCx - 2
        val mouthY = headCy + 4
        drawRect(pixels, w, mouthX - 8, mouthY - 4, 18, 12, mouthCavity)
        drawRectBorder(pixels, w, mouthX - 8, mouthY - 4, 18, 12, black)

        // Upper Fangs (White triangles)
        drawLine(pixels, w, mouthX - 7, mouthY - 3, mouthX - 6, mouthY, toothWhite)
        drawLine(pixels, w, mouthX - 3, mouthY - 3, mouthX - 2, mouthY + 1, toothWhite)
        drawLine(pixels, w, mouthX + 1, mouthY - 3, mouthX + 2, mouthY + 1, toothWhite)
        drawLine(pixels, w, mouthX + 5, mouthY - 3, mouthX + 6, mouthY, toothWhite)

        // Lower Fangs
        drawLine(pixels, w, mouthX - 5, mouthY + 6, mouthX - 4, mouthY + 3, toothWhite)
        drawLine(pixels, w, mouthX - 1, mouthY + 6, mouthX, mouthY + 2, toothWhite)
        drawLine(pixels, w, mouthX + 3, mouthY + 6, mouthX + 4, mouthY + 3, toothWhite)

        // 4. Massive Golden Drill / Cannon Forearm (Upper Right)
        val cannonBaseX = (w * 0.55).toInt()
        val cannonBaseY = (h * 0.45).toInt()
        val cannonTipX = (w * 0.85).toInt()
        val cannonTipY = (h * 0.12).toInt()

        // Cannon funnel body
        drawTriangle(pixels, w, cannonBaseX - 4, cannonBaseY, cannonBaseX + 10, cannonBaseY + 6, cannonTipX + 8, cannonTipY, goldDark)
        drawTriangle(pixels, w, cannonBaseX - 4, cannonBaseY, cannonTipX - 8, cannonTipY - 6, cannonTipX + 8, cannonTipY, goldMain)
        drawTriangle(pixels, w, cannonBaseX, cannonBaseY - 4, cannonTipX - 4, cannonTipY - 4, cannonTipX + 4, cannonTipY - 2, goldLight)

        // Hollow muzzle rim / dark opening
        drawCircle(pixels, w, cannonTipX, cannonTipY, 5, black)
        drawCircle(pixels, w, cannonTipX, cannonTipY, 3, goldDark)

        // Mechanical Claws / Spikes atop forearm
        drawLine(pixels, w, cannonTipX - 6, cannonTipY - 2, cannonTipX - 12, cannonTipY - 8, goldLight)
        drawLine(pixels, w, cannonTipX - 2, cannonTipY - 4, cannonTipX - 6, cannonTipY - 14, goldLight)
        drawLine(pixels, w, cannonTipX + 4, cannonTipY - 4, cannonTipX + 2, cannonTipY - 12, goldLight)

        // Black outline definition
        drawLine(pixels, w, cannonBaseX - 5, cannonBaseY + 1, cannonTipX - 9, cannonTipY - 6, black)
        drawLine(pixels, w, cannonBaseX + 11, cannonBaseY + 7, cannonTipX + 9, cannonTipY + 1, black)
    }

    private fun drawAirMarmot(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF1A1408.toInt())
        // Wings
        drawTriangle(p, w, cx - 2, cy, cx - 22, cy - 12, cx - 12, cy + 10, 0xFF785B28.toInt())
        drawTriangle(p, w, cx + 2, cy, cx + 22, cy - 12, cx + 12, cy + 10, 0xFF785B28.toInt())
        drawCircle(p, w, cx, cy + 2, 9, 0xFFB45309.toInt())
        drawCircle(p, w, cx, cy - 8, 6, 0xFFF59E0B.toInt())
    }

    private fun drawDrollBird(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF041B18.toInt())
        drawCircle(p, w, cx - 4, cy + 2, 10, 0xFF10B981.toInt())
        drawCircle(p, w, cx - 2, cy - 8, 6, 0xFF059669.toInt())
        // Big Beak
        drawTriangle(p, w, cx + 2, cy - 8, cx + 20, cy - 4, cx + 2, cy, 0xFFFACC15.toInt())
    }

    private fun drawKaminarikozou(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF1E1906.toInt())
        drawCircle(p, w, cx, cy + 4, 11, 0xFFEAB308.toInt())
        drawCircle(p, w, cx, cy - 6, 7, 0xFFFEF08A.toInt())
        // Spark in hand
        drawStar(p, w, cx + 14, cy, 4, 0xFFFFFFFF.toInt(), 0xFFEAB308.toInt())
    }

    private fun drawSwordsmanForeign(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF191206.toInt())
        // Conical straw hat
        drawTriangle(p, w, cx - 18, cy - 6, cx + 18, cy - 6, cx, cy - 18, 0xFFD97706.toInt())
        drawRect(p, w, cx - 8, cy - 4, 16, 18, 0xFF451A03.toInt())
        // Katana
        drawLine(p, w, cx + 10, cy - 10, cx + 18, cy + 18, 0xFFE2E8F0.toInt())
    }

    private fun drawMonsterEye(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF190620.toInt())
        drawCircle(p, w, cx, cy, 18, 0xFFF8FAFC.toInt())
        drawCircle(p, w, cx, cy, 10, 0xFFF59E0B.toInt())
        drawCircle(p, w, cx, cy, 5, 0xFF000000.toInt())
        // Veins
        drawLine(p, w, cx - 14, cy - 8, cx - 8, cy - 4, 0xFFEF4444.toInt())
        drawLine(p, w, cx + 14, cy + 8, cx + 8, cy + 4, 0xFFEF4444.toInt())
    }

    private fun drawMechanicalSpider(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF161208.toInt())
        drawCircle(p, w, cx, cy + 4, 10, 0xFF785B28.toInt())
        drawCircle(p, w, cx, cy - 6, 6, 0xFFD4AF37.toInt())
        // 8 Brass legs
        for (i in 0 until 4) {
            val ly = cy - 8 + (i * 6)
            drawLine(p, w, cx - 6, ly, cx - 22, ly - 4 + (i * 3), 0xFFD4AF37.toInt())
            drawLine(p, w, cx + 6, ly, cx + 22, ly - 4 + (i * 3), 0xFFD4AF37.toInt())
        }
    }

    private fun drawCyberStein(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF0F172A.toInt())
        drawRect(p, w, cx - 10, cy - 14, 20, 26, 0xFF64748B.toInt())
        // Neck bolts
        drawRect(p, w, cx - 16, cy + 2, 6, 4, 0xFFCBD5E1.toInt())
        drawRect(p, w, cx + 10, cy + 2, 6, 4, 0xFFCBD5E1.toInt())
        // Red optic sensor
        drawCircle(p, w, cx - 4, cy - 6, 2, 0xFFEF4444.toInt())
    }

    private fun drawJinzo7(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF1A0A10.toInt())
        drawCircle(p, w, cx, cy - 4, 11, 0xFF71717A.toInt())
        // Red Visor
        drawRect(p, w, cx - 8, cy - 6, 16, 4, 0xFFEF4444.toInt())
    }

    private fun drawWhiteDolphin(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF031E36.toInt())
        drawCircle(p, w, cx, cy, 14, 0xFFF8FAFC.toInt())
        drawTriangle(p, w, cx + 8, cy - 2, cx + 24, cy - 12, cx + 18, cy + 8, 0xFFF8FAFC.toInt())
        drawCircle(p, w, cx - 6, cy - 2, 2, 0xFF0284C7.toInt())
    }

    private fun drawTuruPurun(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF042036.toInt())
        drawCircle(p, w, cx, cy + 4, 16, 0xFF38BDF8.toInt())
        drawCircle(p, w, cx - 5, cy, 3, 0xFF0F172A.toInt())
        drawCircle(p, w, cx + 5, cy, 3, 0xFF0F172A.toInt())
    }

    private fun drawSinisterSerpent(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF041E1C.toInt())
        drawCircleBorder(p, w, cx, cy, 15, 0xFF10B981.toInt())
        drawCircle(p, w, cx + 10, cy - 8, 6, 0xFF059669.toInt())
        drawCircle(p, w, cx + 8, cy - 8, 2, 0xFFDC2626.toInt())
    }

    private fun drawBooKoo(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF1E0A24.toInt())
        // Spellbook
        drawRect(p, w, cx - 14, cy + 6, 28, 8, 0xFF785B28.toInt())
        drawCircle(p, w, cx, cy - 4, 8, 0xFFC084FC.toInt())
        drawTriangle(p, w, cx - 7, cy - 6, cx + 7, cy - 6, cx, cy - 20, 0xFF9333EA.toInt())
    }

    private fun drawFungiMusk(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF1A0A20.toInt())
        // Mushroom cap
        drawCircle(p, w, cx, cy - 4, 16, 0xFF7E22CE.toInt())
        drawRect(p, w, cx - 5, cy - 4, 10, 18, 0xFFF3E8FF.toInt())
        // Spore dots
        drawCircle(p, w, cx - 6, cy - 8, 2, 0xFFFACC15.toInt())
        drawCircle(p, w, cx + 6, cy - 8, 2, 0xFFFACC15.toInt())
    }

    private fun drawMukaMuka(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF1A1208.toInt())
        drawCircle(p, w, cx, cy + 4, 14, 0xFF785B28.toInt())
        // Stone spikes
        drawTriangle(p, w, cx - 10, cy - 2, cx - 4, cy - 14, cx, cy - 2, 0xFFB45309.toInt())
        drawTriangle(p, w, cx, cy - 2, cx + 4, cy - 14, cx + 10, cy - 2, 0xFFB45309.toInt())
    }

    private fun drawStarBoy(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF041E34.toInt())
        drawStar(p, w, cx, cy, 14, 0xFF38BDF8.toInt(), 0xFF0284C7.toInt())
        // Crown
        drawTriangle(p, w, cx - 6, cy - 10, cx + 6, cy - 10, cx, cy - 18, 0xFFFDE047.toInt())
    }

    private fun drawMilusRadiant(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF1E1408.toInt())
        drawCircle(p, w, cx, cy + 4, 12, 0xFFFDE68A.toInt())
        drawCircle(p, w, cx, cy - 6, 8, 0xFFFEF08A.toInt())
        drawLine(p, w, cx - 4, cy - 12, cx - 10, cy - 20, 0xFFB45309.toInt())
        drawLine(p, w, cx + 4, cy - 12, cx + 10, cy - 20, 0xFFB45309.toInt())
    }

    private fun drawBoneMouse(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF140D18.toInt())
        drawCircle(p, w, cx, cy + 2, 10, 0xFFE2E8F0.toInt())
        drawCircle(p, w, cx, cy - 8, 6, 0xFFF8FAFC.toInt())
        drawLine(p, w, cx, cy + 12, cx + 16, cy + 18, 0xFFCBD5E1.toInt()) // tail
    }

    private fun drawFrogTheJam(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF041E26.toInt())
        drawCircle(p, w, cx, cy + 4, 15, 0xFF10B981.toInt())
        drawCircle(p, w, cx - 6, cy - 6, 4, 0xFFF8FAFC.toInt())
        drawCircle(p, w, cx + 6, cy - 6, 4, 0xFFF8FAFC.toInt())
        drawCircle(p, w, cx - 6, cy - 6, 2, 0xFF000000.toInt())
        drawCircle(p, w, cx + 6, cy - 6, 2, 0xFF000000.toInt())
        // Lolling red tongue
        drawLine(p, w, cx, cy + 4, cx + 10, cy + 14, 0xFFEF4444.toInt())
    }

    private fun drawAbyssFlower(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF041A10.toInt())
        for (i in 0 until 5) {
            val ang = i * (Math.PI * 2 / 5.0)
            val px = (cx + cos(ang) * 14).toInt()
            val py = (cy + sin(ang) * 14).toInt()
            drawCircle(p, w, px, py, 6, 0xFF059669.toInt())
        }
        drawCircle(p, w, cx, cy, 8, 0xFFEF4444.toInt())
    }

    private fun drawBladefly(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF061E1A.toInt())
        drawRect(p, w, cx - 2, cy - 14, 4, 28, 0xFF10B981.toInt())
        // Scythe wings
        drawLine(p, w, cx, cy - 4, cx - 24, cy - 16, 0xFFE2E8F0.toInt())
        drawLine(p, w, cx, cy - 4, cx + 24, cy - 16, 0xFFE2E8F0.toInt())
    }

    private fun drawHiroShadowScout(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF0F0B18.toInt())
        drawCircle(p, w, cx, cy - 4, 8, 0xFF1F2937.toInt())
        // Ninja cowl eyes
        drawCircle(p, w, cx - 3, cy - 4, 2, 0xFFEF4444.toInt())
        drawCircle(p, w, cx + 3, cy - 4, 2, 0xFFEF4444.toInt())
        // Dagger
        drawLine(p, w, cx + 8, cy - 2, cx + 18, cy + 14, 0xFFE2E8F0.toInt())
    }

    private fun drawYamatanoDragon(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF041E1C.toInt())
        // Ancient scroll
        drawRect(p, w, cx - 18, cy - 14, 36, 28, 0xFFFDE68A.toInt())
        // Serpentine dragon
        drawCircleBorder(p, w, cx, cy, 10, 0xFF059669.toInt())
    }

    private fun drawMysteryHand(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF0F0B18.toInt())
        drawCircle(p, w, cx, cy, 14, 0xFF2E1065.toInt())
        // Spectral hand fingers
        for (i in 0 until 4) {
            val fx = cx - 6 + (i * 4)
            drawLine(p, w, fx, cy + 4, fx, cy - 12, 0xFFC084FC.toInt())
        }
    }

    private fun drawMaskedClown(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF140D1C.toInt())
        drawCircle(p, w, cx, cy - 2, 10, 0xFFF8FAFC.toInt())
        // Two-tone smiling mask
        drawCircle(p, w, cx - 3, cy - 4, 2, 0xFFDC2626.toInt())
        drawCircle(p, w, cx + 3, cy - 4, 2, 0xFF2563EB.toInt())
        drawLine(p, w, cx - 5, cy + 2, cx + 5, cy + 2, 0xFFDC2626.toInt())
    }

    private fun drawOscilloHero2(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF1E1A04.toInt())
        drawCircle(p, w, cx, cy - 4, 10, 0xFFEAB308.toInt())
        // Lightning fists
        drawStar(p, w, cx - 14, cy + 6, 4, 0xFFFFFFFF.toInt(), 0xFFEAB308.toInt())
        drawStar(p, w, cx + 14, cy + 6, 4, 0xFFFFFFFF.toInt(), 0xFFEAB308.toInt())
    }

    private fun drawSwordsmanLandstar(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF1E0E06.toInt())
        drawCircle(p, w, cx, cy - 6, 7, 0xFFFFDFBA.toInt())
        // Red helmet
        drawTriangle(p, w, cx - 7, cy - 6, cx + 7, cy - 6, cx, cy - 16, 0xFFDC2626.toInt())
        // Shield & Sword
        drawCircle(p, w, cx - 12, cy + 4, 6, 0xFFD4AF37.toInt())
        drawLine(p, w, cx + 8, cy - 8, cx + 16, cy + 12, 0xFFE2E8F0.toInt())
    }

    private fun drawMountainWarrior(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF1C1206.toInt())
        drawCircle(p, w, cx, cy - 6, 8, 0xFFB45309.toInt())
        // Battleaxe
        val axX = cx + 14
        drawLine(p, w, axX, cy - 16, axX - 4, cy + 18, 0xFF785B28.toInt())
        drawTriangle(p, w, axX - 2, cy - 14, axX + 8, cy - 8, axX - 2, cy - 2, 0xFFE2E8F0.toInt())
    }

    private fun drawTimeWizard(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF1E1804.toInt())
        // Clock face
        drawCircle(p, w, cx, cy, 14, 0xFFFACC15.toInt())
        drawCircle(p, w, cx, cy, 11, 0xFFFFFBEB.toInt())
        // Clock hands
        drawLine(p, w, cx, cy, cx, cy - 7, 0xFF000000.toInt())
        drawLine(p, w, cx, cy, cx + 6, cy, 0xFF000000.toInt())
    }

    private fun drawHarpieLady(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF041E18.toInt())
        drawTriangle(p, w, cx - 2, cy, cx - 24, cy - 14, cx - 12, cy + 10, 0xFF10B981.toInt())
        drawTriangle(p, w, cx + 2, cy, cx + 24, cy - 14, cx + 12, cy + 10, 0xFF10B981.toInt())
        drawCircle(p, w, cx, cy - 6, 6, 0xFFFFDFBA.toInt())
    }

    private fun drawSonicMaid(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF041E1A.toInt())
        drawCircle(p, w, cx, cy - 6, 6, 0xFFFFDFBA.toInt())
        drawTriangle(p, w, cx - 8, cy + 14, cx + 8, cy + 14, cx, cy, 0xFF38BDF8.toInt())
    }

    private fun drawWhiteMagicalHat(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF140D1E.toInt())
        // White top hat
        drawRect(p, w, cx - 8, cy - 18, 16, 12, 0xFFF8FAFC.toInt())
        drawRect(p, w, cx - 12, cy - 6, 24, 3, 0xFFF8FAFC.toInt())
        drawCircle(p, w, cx, cy + 2, 7, 0xFFFFDFBA.toInt())
    }

    private fun drawMorphingJar(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF1E1408.toInt())
        drawCircle(p, w, cx, cy + 4, 16, 0xFFB45309.toInt())
        drawCircle(p, w, cx, cy + 2, 8, 0xFF000000.toInt())
        drawCircle(p, w, cx - 4, cy, 2, 0xFF10B981.toInt())
        drawCircle(p, w, cx + 4, cy, 2, 0xFF10B981.toInt())
    }

    private fun drawBattleOx(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF1C1004.toInt())
        drawCircle(p, w, cx, cy - 4, 9, 0xFF785B28.toInt())
        // Curved bull horns
        drawLine(p, w, cx - 6, cy - 8, cx - 18, cy - 18, 0xFFE2E8F0.toInt())
        drawLine(p, w, cx + 6, cy - 8, cx + 18, cy - 18, 0xFFE2E8F0.toInt())
        drawRect(p, w, cx - 10, cy + 4, 20, 16, 0xFF451A03.toInt())
    }

    private fun drawHitotsuMeGiant(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF1C1204.toInt())
        drawCircle(p, w, cx, cy - 6, 10, 0xFFD97706.toInt())
        // Single giant eye
        drawCircle(p, w, cx, cy - 6, 5, 0xFFF8FAFC.toInt())
        drawCircle(p, w, cx, cy - 6, 2, 0xFFDC2626.toInt())
    }

    private fun drawRyuKishin(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF0F0B18.toInt())
        drawCircle(p, w, cx, cy - 4, 8, 0xFF64748B.toInt())
        drawLine(p, w, cx - 4, cy - 10, cx - 12, cy - 20, 0xFF94A3B8.toInt())
        drawLine(p, w, cx + 4, cy - 10, cx + 12, cy - 20, 0xFF94A3B8.toInt())
    }

    private fun drawKuriboh(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF140D18.toInt())
        drawCircle(p, w, cx, cy, 14, 0xFF785B28.toInt())
        drawCircle(p, w, cx, cy, 11, 0xFFB45309.toInt())
        // Round Kuriboh eyes
        drawCircle(p, w, cx - 4, cy - 2, 3, 0xFFF8FAFC.toInt())
        drawCircle(p, w, cx + 4, cy - 2, 3, 0xFFF8FAFC.toInt())
        drawCircle(p, w, cx - 4, cy - 2, 1, 0xFF10B981.toInt())
        drawCircle(p, w, cx + 4, cy - 2, 1, 0xFF10B981.toInt())
    }

    private fun drawTwinHeadedThunderDragon(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF1E1A04.toInt())
        drawCircle(p, w, cx - 8, cy - 8, 7, 0xFFEAB308.toInt())
        drawCircle(p, w, cx + 8, cy - 8, 7, 0xFFEAB308.toInt())
        drawLine(p, w, cx - 8, cy - 8, cx + 8, cy - 8, 0xFFFEF08A.toInt())
    }

    private fun drawZombieDragon(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF120E18.toInt())
        drawCircle(p, w, cx, cy - 8, 8, 0xFF6B7280.toInt())
        for (i in 0 until 4) {
            val ry = cy + (i * 4)
            drawLine(p, w, cx - 8 + i, ry, cx + 8 - i, ry, 0xFFE2E8F0.toInt())
        }
    }

    private fun drawFlameCerebrus(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF260808.toInt())
        drawCircle(p, w, cx - 10, cy - 6, 6, 0xFFDC2626.toInt())
        drawCircle(p, w, cx, cy - 8, 7, 0xFFEF4444.toInt())
        drawCircle(p, w, cx + 10, cy - 6, 6, 0xFFDC2626.toInt())
    }

    private fun drawThousandDragon(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF240606.toInt())
        drawCircle(p, w, cx, cy - 8, 8, 0xFFDC2626.toInt())
        drawLine(p, w, cx, cy - 12, cx, cy - 24, 0xFFFEF08A.toInt()) // Ivory horn
    }

    private fun drawCyberSoldier(p: IntArray, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        p.fill(0xFF0F172A.toInt())
        drawRect(p, w, cx - 8, cy - 12, 16, 24, 0xFF64748B.toInt())
        drawRect(p, w, cx - 6, cy - 8, 12, 4, 0xFF38BDF8.toInt())
    }

    // --- FALLBACK ARCHETYPE GLYPHS ---

    private fun drawDragonSprite(p: IntArray, w: Int, h: Int, colMain: Int, colSec: Int, colEye: Int) {
        val cx = w / 2
        val cy = h / 2
        drawTriangle(p, w, cx - 14, cy - 8, cx + 14, cy - 8, cx, cy + 18, colMain)
        drawTriangle(p, w, cx - 12, cy - 8, cx - 22, cy - 24, cx - 6, cy - 14, colSec)
        drawTriangle(p, w, cx + 12, cy - 8, cx + 22, cy - 24, cx + 6, cy - 14, colSec)
        drawCircle(p, w, cx - 4, cy - 4, 2, colEye)
        drawCircle(p, w, cx + 4, cy - 4, 2, colEye)
    }

    private fun drawSpellcasterSprite(p: IntArray, w: Int, h: Int, colMain: Int, colSec: Int, colEye: Int) {
        val cx = w / 2
        val cy = h / 2
        drawTriangle(p, w, cx - 12, cy - 4, cx + 12, cy - 4, cx, cy - 22, colMain)
        drawCircle(p, w, cx, cy + 2, 7, colSec)
        drawCircle(p, w, cx - 3, cy + 2, 1, colEye)
        drawCircle(p, w, cx + 3, cy + 2, 1, colEye)
    }

    private fun drawSkullSprite(p: IntArray, w: Int, h: Int, colMain: Int, colSec: Int, colEye: Int) {
        val cx = w / 2
        val cy = h / 2
        drawCircle(p, w, cx, cy - 4, 10, colMain)
        drawCircle(p, w, cx - 4, cy - 4, 3, colEye)
        drawCircle(p, w, cx + 4, cy - 4, 3, colEye)
    }

    private fun drawWarriorSprite(p: IntArray, w: Int, h: Int, colMain: Int, colSec: Int, colEye: Int) {
        val cx = w / 2
        val cy = h / 2
        drawLine(p, w, cx - 14, cy - 14, cx + 14, cy + 14, colMain)
        drawLine(p, w, cx + 14, cy - 14, cx - 14, cy + 14, colMain)
        drawCircle(p, w, cx, cy, 6, colSec)
    }

    private fun drawBirdSprite(p: IntArray, w: Int, h: Int, colMain: Int, colSec: Int, colEye: Int) {
        val cx = w / 2
        val cy = h / 2
        drawTriangle(p, w, cx, cy - 6, cx - 22, cy - 16, cx - 10, cy + 8, colMain)
        drawTriangle(p, w, cx, cy - 6, cx + 22, cy - 16, cx + 10, cy + 8, colMain)
        drawCircle(p, w, cx, cy - 6, 5, colSec)
    }

    private fun drawSpellGlyph(p: IntArray, w: Int, h: Int, colLight: Int, colMid: Int) {
        val cx = w / 2
        val cy = h / 2
        drawCircleBorder(p, w, cx, cy, 18, colLight)
        drawStar(p, w, cx, cy, 8, colLight, colMid)
    }

    private fun drawTrapGlyph(p: IntArray, w: Int, h: Int, colLight: Int, colMid: Int) {
        val cx = w / 2
        val cy = h / 2
        drawCircle(p, w, cx, cy, 14, colMid)
        drawCircle(p, w, cx, cy, 9, 0xFF000000.toInt())
        drawTriangle(p, w, cx - 8, cy, cx + 8, cy, cx, cy - 12, colLight)
    }

    private fun drawGenericMonsterSprite(p: IntArray, w: Int, h: Int, colMain: Int, colSec: Int, colEye: Int) {
        val cx = w / 2
        val cy = h / 2
        drawCircle(p, w, cx, cy, 12, colMain)
        drawCircle(p, w, cx, cy, 8, colSec)
        drawCircle(p, w, cx - 3, cy - 2, 2, colEye)
        drawCircle(p, w, cx + 3, cy - 2, 2, colEye)
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
}
