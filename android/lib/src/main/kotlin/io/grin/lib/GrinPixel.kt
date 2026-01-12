package io.grin.lib

class GrinPixel(
    r: Int,
    g: Int,
    b: Int,
    a: Int,
    c: Int
) {
    var r: Int = r and 0xFF
    var g: Int = g and 0xFF
    var b: Int = b and 0xFF
    var a: Int = a and 0xFF
    var c: Int = c and 0xFF

    fun getGroupId(): Int = GrinControlByte.getGroupId(c)

    fun isLocked(): Boolean = GrinControlByte.isLocked(c)

    fun setGroupId(groupId: Int) {
        c = GrinControlByte.setGroupId(c, groupId) and 0xFF
    }

    fun setLocked(locked: Boolean) {
        c = GrinControlByte.setLocked(c, locked) and 0xFF
    }

    fun toBytes(): ByteArray {
        return byteArrayOf(
            r.toByte(),
            g.toByte(),
            b.toByte(),
            a.toByte(),
            c.toByte()
        )
    }

    companion object {
        fun fromBytes(bytes: ByteArray): GrinPixel {
            require(bytes.size == GrinFormat.PIXEL_SIZE_BYTES) {
                "GrinPixel requires exactly ${GrinFormat.PIXEL_SIZE_BYTES} bytes"
            }
            return GrinPixel(
                bytes[GrinPixelLayout.R_OFFSET].toInt() and 0xFF,
                bytes[GrinPixelLayout.G_OFFSET].toInt() and 0xFF,
                bytes[GrinPixelLayout.B_OFFSET].toInt() and 0xFF,
                bytes[GrinPixelLayout.A_OFFSET].toInt() and 0xFF,
                bytes[GrinPixelLayout.C_OFFSET].toInt() and 0xFF
            )
        }
    }
}
