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
