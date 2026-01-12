/*
 * MIT License
 *
 * Copyright (c) 2025 GRIN Project Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.grin.demo

import android.graphics.Bitmap
import android.graphics.Color
import io.grin.lib.GrinFile
import io.grin.lib.GrinPixel
import kotlin.math.PI
import kotlin.math.sin

// Renders preview bitmaps from GRIN files with channel settings applied.
class GrinPreviewRenderer {
    fun renderPreview(grinFile: GrinFile, settings: List<ChannelSetting>, focusedChannelId: Int?): Bitmap {
        // Render a static preview with the latest slider settings applied.
        return renderFrame(grinFile, settings, focusedChannelId, frameIndex = 0, frameCount = 1)
    }

    fun renderFrame(
        grinFile: GrinFile,
        settings: List<ChannelSetting>,
        focusedChannelId: Int?,
        frameIndex: Int,
        frameCount: Int
    ): Bitmap {
        // Render a single frame with optional frequency-based modulation.
        val width = grinFile.header.width.toInt()
        val height = grinFile.header.height.toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val settingsByChannel = settings.associateBy { it.channelId }
        grinFile.pixels.forEachIndexed { index, pixel ->
            val channelId = pixel.getGroupId()
            val setting = settingsByChannel[channelId]
            val shouldApply = focusedChannelId == null || focusedChannelId == channelId
            val color = if (setting != null && shouldApply) {
                applySetting(pixel, setting, frameIndex, frameCount)
            } else {
                Color.argb(pixel.a, pixel.r, pixel.g, pixel.b)
            }
            val x = index % width
            val y = index / width
            bitmap.setPixel(x, y, color)
        }
        return bitmap
    }

    private fun applySetting(pixel: GrinPixel, setting: ChannelSetting, frameIndex: Int, frameCount: Int): Int {
        // Apply intonation (RGB bias) and transparency modulation to a pixel.
        val intonationDelta = ((setting.intonation - 50) * 2).coerceIn(-100, 100)
        val red = (pixel.r + intonationDelta).coerceIn(0, 255)
        val green = (pixel.g + intonationDelta).coerceIn(0, 255)
        val blue = (pixel.b + intonationDelta).coerceIn(0, 255)

        val alphaScale = setting.transparency.coerceIn(0, 100) / 100.0
        val frequencyScale = setting.frequency.coerceIn(0, 100) / 100.0
        val phase = 2 * PI * frameIndex / frameCount * (0.5 + frequencyScale)
        val pulse = 0.5 + 0.5 * sin(phase)
        val alpha = (pixel.a * alphaScale * pulse).coerceIn(0.0, 255.0).toInt()

        return Color.argb(alpha, red, green, blue)
    }
}
