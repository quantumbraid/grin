package io.grin.lib

class GrinImage(
    val header: GrinHeader,
    val pixels: MutableList<GrinPixel>,
    val rules: List<GrinRule>
) {
    init {
        val expectedPixelCount = safePixelCount(header.width, header.height)
        require(expectedPixelCount != null) { "Pixel count exceeds safe integer range" }
        require(pixels.size == expectedPixelCount) {
            "Pixel array length does not match header dimensions"
        }
        require(rules.size == header.ruleCount) {
            "Rule array length does not match header ruleCount"
        }
    }

    fun getPixel(x: Int, y: Int): GrinPixel {
        val index = indexFor(x, y)
        return pixels[index]
    }

    fun setPixel(x: Int, y: Int, pixel: GrinPixel) {
        val index = indexFor(x, y)
        pixels[index] = pixel
    }

    fun getPixelsByGroup(groupId: Int): List<GrinPixel> {
        val target = groupId and 0x0F
        return pixels.filter { it.getGroupId() == target }
    }

    fun getLockedPixels(): List<GrinPixel> {
        return pixels.filter { it.isLocked() }
    }

    fun getUnlockedPixels(): List<GrinPixel> {
        return pixels.filter { !it.isLocked() }
    }

    private fun indexFor(x: Int, y: Int): Int {
        require(x >= 0 && y >= 0) { "Pixel coordinates must be non-negative" }
        val width = safeInt(header.width, "Width")
        val height = safeInt(header.height, "Height")
        require(x < width && y < height) { "Pixel coordinates out of bounds" }
        return y * width + x
    }
}

private fun safePixelCount(width: Long, height: Long): Int? {
    val pixelCount = width * height
    if (width != 0L && pixelCount / width != height) {
        return null
    }
    return safeInt(pixelCount, "Pixel count")
}

private fun safeInt(value: Long, label: String): Int {
    require(value <= Int.MAX_VALUE.toLong()) { "$label exceeds Int range" }
    require(value >= 0L) { "$label must be non-negative" }
    return value.toInt()
}
