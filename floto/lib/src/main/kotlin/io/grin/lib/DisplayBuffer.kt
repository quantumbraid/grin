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
package io.grin.lib

import android.graphics.Bitmap

class DisplayBuffer(val width: Int, val height: Int) {
    val rgbaData: ByteArray = ByteArray(width * height * 4)

    init {
        require(width > 0 && height > 0) { "DisplayBuffer dimensions must be positive" }
    }

    fun clear() {
        rgbaData.fill(0)
    }

    fun setPixel(x: Int, y: Int, r: Int, g: Int, b: Int, a: Int) {
        require(x >= 0 && y >= 0 && x < width && y < height) { "Pixel coordinates out of bounds" }
        val offset = (y * width + x) * 4
        rgbaData[offset] = r.toByte()
        rgbaData[offset + 1] = g.toByte()
        rgbaData[offset + 2] = b.toByte()
        rgbaData[offset + 3] = a.toByte()
    }

    fun toBitmap(): Bitmap {
        val pixels = IntArray(width * height)
        var index = 0
        var pixelIndex = 0
        while (index < rgbaData.size) {
            val r = rgbaData[index].toInt() and 0xFF
            val g = rgbaData[index + 1].toInt() and 0xFF
            val b = rgbaData[index + 2].toInt() and 0xFF
            val a = rgbaData[index + 3].toInt() and 0xFF
            pixels[pixelIndex] = (a shl 24) or (r shl 16) or (g shl 8) or b
            index += 4
            pixelIndex += 1
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }
}
