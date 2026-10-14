package com.yugioh.recomp.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.yugioh.recomp.core.rom.DecodedSprite

object BitmapUtils {
    /**
     * Converts raw decoded ARGB pixel arrays into an Android ImageBitmap.
     */
    fun spriteToImageBitmap(sprite: DecodedSprite): ImageBitmap {
        val bitmap = Bitmap.createBitmap(sprite.width, sprite.height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(sprite.pixels, 0, sprite.width, 0, 0, sprite.width, sprite.height)
        return bitmap.asImageBitmap()
    }
}
