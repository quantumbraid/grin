package io.grin.lib

import java.io.InputStream
import java.io.OutputStream

data class GrinHeaderReadResult(
    val header: GrinHeader,
    val warnings: List<String>
)

data class GrinPixelReadResult(
    val pixels: List<GrinPixel>,
    val warnings: List<String>
)

data class GrinRulesReadResult(
    val rules: List<GrinRule>,
    val warnings: List<String>
)

fun readHeader(input: InputStream): GrinHeaderReadResult {
    val headerBytes = ByteArray(GrinFormat.HEADER_SIZE_BYTES)
    readFully(input, headerBytes)
    val header = GrinHeader.deserialize(headerBytes)
    val validation = header.validate()
    require(validation.ok) { "Invalid GRIN header: ${validation.errors.joinToString("; ")}" }
    return GrinHeaderReadResult(header, validation.warnings)
}

fun writeHeader(header: GrinHeader, output: OutputStream) {
    val pixelDataLength = safeMultiply(
        header.width,
        header.height
    )?.let { safeMultiply(it, GrinFormat.PIXEL_SIZE_BYTES.toLong()) }
        ?: throw IllegalArgumentException("PixelDataLength overflow for width/height")

    val fileLength = if (header.fileLength != 0L) {
        header.fileLength
    } else {
        GrinFormat.HEADER_SIZE_BYTES.toLong() + pixelDataLength
    }

    val normalized = header.copy(
        headerSize = GrinFormat.HEADER_SIZE_BYTES,
        pixelDataLength = pixelDataLength,
        fileLength = fileLength,
        pixelDataOffset = GrinFormat.HEADER_SIZE_BYTES.toLong(),
        flags = 0,
        reservedA = 0L,
        reservedB = 0L
    )

    output.write(normalized.serialize())
}

fun readPixelData(input: InputStream, width: Long, height: Long): GrinPixelReadResult {
    val pixelCount = safeMultiply(width, height)
        ?: throw IllegalArgumentException("Pixel count overflow for width/height")
    require(pixelCount <= Int.MAX_VALUE.toLong()) { "Pixel count exceeds Int range" }

    val pixels = ArrayList<GrinPixel>(pixelCount.toInt())
    var reservedCount = 0
    val buffer = ByteArray(GrinFormat.PIXEL_SIZE_BYTES)
    var index = 0L
    while (index < pixelCount) {
        readFully(input, buffer)
        val pixel = GrinPixel.fromBytes(buffer)
        if ((pixel.c and GrinControlByte.RESERVED_MASK) != 0) {
            reservedCount += 1
        }
        pixels.add(pixel)
        index += 1
    }

    val warnings = mutableListOf<String>()
    if (reservedCount > 0) {
        warnings.add("Control byte reserved bits set on $reservedCount pixels")
    }
    return GrinPixelReadResult(pixels, warnings)
}

fun writePixelData(pixels: List<GrinPixel>, output: OutputStream) {
    val buffer = ByteArray(GrinFormat.PIXEL_SIZE_BYTES)
    for (pixel in pixels) {
        buffer[0] = pixel.r.toByte()
        buffer[1] = pixel.g.toByte()
        buffer[2] = pixel.b.toByte()
        buffer[3] = pixel.a.toByte()
        val control = pixel.c and 0xFF
        val sanitized = control and GrinControlByte.RESERVED_MASK.inv() and 0xFF
        buffer[4] = sanitized.toByte()
        output.write(buffer)
    }
}

fun readRulesBlock(
    bytes: ByteArray,
    ruleCount: Int,
    opcodeSetId: Int
): GrinRulesReadResult {
    require(bytes.size == GrinFormat.RULES_BLOCK_SIZE) { "Rules block must be 64 bytes" }
    require(ruleCount <= GrinFormat.MAX_RULE_COUNT) { "RuleCount exceeds ${GrinFormat.MAX_RULE_COUNT}" }

    val rules = mutableListOf<GrinRule>()
    var index = 0
    while (index < ruleCount) {
        val offset = index * GrinFormat.RULE_ENTRY_SIZE
        val slice = bytes.copyOfRange(offset, offset + GrinFormat.RULE_ENTRY_SIZE)
        val rule = GrinRule.deserialize(slice)
        require(isValidOpcode(opcodeSetId, rule.opcode)) {
            "Unknown opcode ${rule.opcode} for opcode set $opcodeSetId"
        }
        rules.add(rule)
        index += 1
    }

    val warnings = mutableListOf<String>()
    var i = ruleCount * GrinFormat.RULE_ENTRY_SIZE
    while (i < bytes.size) {
        if (bytes[i] != 0.toByte()) {
            warnings.add("Unused rules block entries are non-zero")
            break
        }
        i += 1
    }

    return GrinRulesReadResult(rules, warnings)
}

fun writeRulesBlock(rules: List<GrinRule>, ruleCount: Int): ByteArray {
    require(ruleCount <= GrinFormat.MAX_RULE_COUNT) { "RuleCount exceeds ${GrinFormat.MAX_RULE_COUNT}" }
    require(rules.size >= ruleCount) { "Rule list is shorter than ruleCount" }

    val block = ByteArray(GrinFormat.RULES_BLOCK_SIZE)
    var index = 0
    while (index < ruleCount) {
        val rule = rules[index]
        require(rule.groupMask in 0..0xFFFF) { "GroupMask must fit in 16 bits" }
        val offset = index * GrinFormat.RULE_ENTRY_SIZE
        val ruleBytes = rule.serialize()
        System.arraycopy(ruleBytes, 0, block, offset, ruleBytes.size)
        index += 1
    }
    return block
}

private fun readFully(input: InputStream, buffer: ByteArray) {
    var offset = 0
    while (offset < buffer.size) {
        val read = input.read(buffer, offset, buffer.size - offset)
        if (read == -1) {
            throw IllegalArgumentException("Unexpected end of stream")
        }
        offset += read
    }
}

private fun safeMultiply(a: Long, b: Long): Long? {
    if (a < 0L || b < 0L) {
        return null
    }
    if (a == 0L || b == 0L) {
        return 0L
    }
    val result = a * b
    if (result / a != b) {
        return null
    }
    return result
}

private fun isValidOpcode(opcodeSetId: Int, opcode: Int): Boolean {
    if (opcodeSetId != 0) {
        return false
    }
    return opcode in 0x00..0x0C
}
