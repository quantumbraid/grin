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
