package io.grin.lib

import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class GrinFile(
    val header: GrinHeader,
    val pixels: List<GrinPixel>,
    val rules: List<GrinRule>
) {
    fun toBytes(): ByteArray {
        val rulesBlock = buildRulesBlock(rules)
        val pixelDataLength = safeMultiply(
            pixels.size.toLong(),
            GrinFormat.PIXEL_SIZE_BYTES.toLong(),
            "PixelDataLength"
        )
        val updatedHeader = header.copy(
            ruleCount = rules.size,
            rulesBlock = rulesBlock,
            pixelDataLength = pixelDataLength,
            pixelDataOffset = GrinFormat.HEADER_SIZE_BYTES.toLong()
        )

        val totalLength = safeInt(
            GrinFormat.HEADER_SIZE_BYTES.toLong() + pixelDataLength,
            "Total file length"
        )
        val output = ByteArray(totalLength)
        val headerBytes = updatedHeader.serialize()
        System.arraycopy(headerBytes, 0, output, 0, headerBytes.size)

        var offset = GrinFormat.HEADER_SIZE_BYTES
        for (pixel in pixels) {
            output[offset] = pixel.r.toByte()
            output[offset + 1] = pixel.g.toByte()
            output[offset + 2] = pixel.b.toByte()
            output[offset + 3] = pixel.a.toByte()
            val control = pixel.c and 0xFF
            val sanitized = control and GrinControlByte.RESERVED_MASK.inv() and 0xFF
            output[offset + 4] = sanitized.toByte()
            offset += GrinFormat.PIXEL_SIZE_BYTES
        }
        return output
    }

    fun save(path: String) {
        File(path).outputStream().use { output ->
            output.write(toBytes())
        }
    }

    companion object {
        fun load(path: String): GrinFile {
            FileInputStream(File(path)).use { input ->
                return load(input)
            }
        }

        fun load(inputStream: InputStream): GrinFile {
            val bytes = inputStream.readBytes()
            return load(bytes)
        }

        fun load(bytes: ByteArray): GrinFile {
            require(bytes.size >= GrinFormat.HEADER_SIZE_BYTES) {
                "Buffer too small for GRIN header"
            }
            val headerBytes = bytes.copyOfRange(0, GrinFormat.HEADER_SIZE_BYTES)
            val header = GrinHeader.deserialize(headerBytes)
            val validation = header.validate()
            require(validation.ok) { "Invalid GRIN header: ${validation.errors.joinToString("; ")}" }

            val rules = parseRules(header.rulesBlock, header.ruleCount)
            val pixelOffset = safeInt(header.pixelDataOffset, "PixelDataOffset64")
            val pixelLength = safeInt(header.pixelDataLength, "PixelDataLength")
            require(bytes.size >= pixelOffset + pixelLength) {
                "Buffer does not contain full pixel data"
            }

            val pixelBytes = bytes.copyOfRange(pixelOffset, pixelOffset + pixelLength)
            val pixels = parsePixels(pixelBytes, header.width, header.height)
            val image = GrinImage(header, pixels, rules)
            return GrinFile(image.header, image.pixels, image.rules)
        }
    }
}

private fun parseRules(rulesBlock: ByteArray, ruleCount: Int): List<GrinRule> {
    require(ruleCount <= GrinFormat.MAX_RULE_COUNT) {
        "RuleCount exceeds ${GrinFormat.MAX_RULE_COUNT}"
    }
    val rules = mutableListOf<GrinRule>()
    var index = 0
    while (index < ruleCount) {
        val offset = index * GrinFormat.RULE_ENTRY_SIZE
        val slice = rulesBlock.copyOfRange(offset, offset + GrinFormat.RULE_ENTRY_SIZE)
        rules.add(GrinRule.deserialize(slice))
        index += 1
    }
    return rules
}

private fun parsePixels(pixelBytes: ByteArray, width: Long, height: Long): MutableList<GrinPixel> {
    require(width >= 0L && height >= 0L) { "Image dimensions must be non-negative" }
    val pixelCount = safeMultiply(width, height, "Pixel count")
    val expectedLength = safeMultiply(pixelCount, GrinFormat.PIXEL_SIZE_BYTES.toLong(), "Pixel data length")
    val expectedInt = safeInt(expectedLength, "Pixel data length")
    require(pixelBytes.size == expectedInt) {
        "Pixel data length does not match header dimensions"
    }

    val pixels = mutableListOf<GrinPixel>()
    var offset = 0
    while (offset < pixelBytes.size) {
        val slice = pixelBytes.copyOfRange(offset, offset + GrinFormat.PIXEL_SIZE_BYTES)
        pixels.add(GrinPixel.fromBytes(slice))
        offset += GrinFormat.PIXEL_SIZE_BYTES
    }
    return pixels
}

private fun buildRulesBlock(rules: List<GrinRule>): ByteArray {
    require(rules.size <= GrinFormat.MAX_RULE_COUNT) {
        "Rule count exceeds ${GrinFormat.MAX_RULE_COUNT}"
    }
    val rulesBlock = ByteArray(GrinFormat.RULES_BLOCK_SIZE)
    var index = 0
    while (index < rules.size) {
        val offset = index * GrinFormat.RULE_ENTRY_SIZE
        val ruleBytes = rules[index].serialize()
        System.arraycopy(ruleBytes, 0, rulesBlock, offset, ruleBytes.size)
        index += 1
    }
    return rulesBlock
}

private fun safeInt(value: Long, label: String): Int {
    require(value >= 0L) { "$label must be non-negative" }
    require(value <= Int.MAX_VALUE.toLong()) { "$label exceeds Int range" }
    return value.toInt()
}

private fun safeLong(value: Long, label: String): Long {
    require(value >= 0L) { "$label must be non-negative" }
    return value
}

private fun safeMultiply(a: Long, b: Long, label: String): Long {
    if (a == 0L || b == 0L) {
        return 0L
    }
    val result = a * b
    require(result / a == b) { "$label exceeds Long range" }
    return result
}
