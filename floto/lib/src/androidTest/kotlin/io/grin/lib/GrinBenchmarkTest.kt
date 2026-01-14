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
import android.graphics.Canvas
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GrinBenchmarkTest {
    @Test
    fun benchmarkBitmapRenderer() {
        val width = 128
        val height = 128
        val buffer = DisplayBuffer(width, height)
        var index = 0
        while (index < buffer.rgbaData.size) {
            buffer.rgbaData[index] = 0x20
            buffer.rgbaData[index + 1] = 0x40
            buffer.rgbaData[index + 2] = 0x60
            buffer.rgbaData[index + 3] = 0xFF.toByte()
            index += 4
        }

        val renderer = GrinBitmapRenderer()
        var bitmap: Bitmap? = null
        val start = System.nanoTime()
        repeat(30) {
            bitmap = renderer.render(buffer, bitmap)
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        Log.i("GrinBenchmark", "Bitmap render x30: ${elapsedMs}ms")
        assertTrue(elapsedMs >= 0)
    }

    @Test
    fun benchmarkCanvasDraw() {
        val width = 256
        val height = 256
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val start = System.nanoTime()
        repeat(60) {
            canvas.drawBitmap(bitmap, 0f, 0f, null)
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        Log.i("GrinBenchmark", "Canvas draw x60: ${elapsedMs}ms")
        assertTrue(elapsedMs >= 0)
    }
}
