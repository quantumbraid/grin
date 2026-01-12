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
