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
