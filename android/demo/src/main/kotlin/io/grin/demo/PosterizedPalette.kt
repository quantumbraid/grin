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

import android.graphics.Color

// Defines posterization palettes and label helpers for the grid camera preview.
data class PosterizedPalette(
    val colors: IntArray,
    val labels: List<String>
) {
    // Limits palette size to the available labels.
    fun clampSize(size: Int): PosterizedPalette {
        val clamped = size.coerceIn(1, colors.size)
        return PosterizedPalette(colors.copyOf(clamped), labels)
    }

    companion object {
        // Returns the default 14-color palette with hex channel labels.
        fun defaultPalette(): PosterizedPalette {
            val paletteColors = intArrayOf(
                Color.rgb(0x00, 0x00, 0x00),
                Color.rgb(0x1E, 0x1E, 0x1E),
                Color.rgb(0x4F, 0x4F, 0x4F),
                Color.rgb(0x7F, 0x7F, 0x7F),
                Color.rgb(0xB2, 0xB2, 0xB2),
                Color.rgb(0xE0, 0xE0, 0xE0),
                Color.rgb(0xFF, 0xFF, 0xFF),
                Color.rgb(0xD9, 0x32, 0x32),
                Color.rgb(0xF2, 0x7C, 0x2A),
                Color.rgb(0xF2, 0xC2, 0x2A),
                Color.rgb(0x6E, 0xD9, 0x32),
                Color.rgb(0x2A, 0xC2, 0xF2),
                Color.rgb(0x2A, 0x7C, 0xF2),
                Color.rgb(0x8C, 0x2A, 0xF2)
            )
            val labels = (0..15).map { index -> index.toString(16).uppercase() }
            return PosterizedPalette(paletteColors, labels)
        }
    }
}
