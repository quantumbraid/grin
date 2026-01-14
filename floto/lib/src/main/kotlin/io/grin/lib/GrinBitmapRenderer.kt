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

class GrinBitmapRenderer {
    private var argbBuffer: ByteArray = ByteArray(0)

    fun render(buffer: DisplayBuffer, reuse: Bitmap? = null): Bitmap {
        val bitmap = if (reuse != null && reuse.width == buffer.width && reuse.height == buffer.height) {
            reuse
        } else {
            Bitmap.createBitmap(buffer.width, buffer.height, Bitmap.Config.ARGB_8888)
        }

        val required = buffer.width * buffer.height * 4
        if (argbBuffer.size != required) {
            argbBuffer = ByteArray(required)
        }

        var srcIndex = 0
        var dstIndex = 0
        while (srcIndex < buffer.rgbaData.size) {
            val r = buffer.rgbaData[srcIndex].toInt() and 0xFF
            val g = buffer.rgbaData[srcIndex + 1].toInt() and 0xFF
            val b = buffer.rgbaData[srcIndex + 2].toInt() and 0xFF
            val a = buffer.rgbaData[srcIndex + 3].toInt() and 0xFF

            argbBuffer[dstIndex] = a.toByte()
            argbBuffer[dstIndex + 1] = r.toByte()
            argbBuffer[dstIndex + 2] = g.toByte()
            argbBuffer[dstIndex + 3] = b.toByte()

            srcIndex += 4
            dstIndex += 4
        }

        bitmap.copyPixelsFromBuffer(java.nio.ByteBuffer.wrap(argbBuffer))
        return bitmap
    }
}
