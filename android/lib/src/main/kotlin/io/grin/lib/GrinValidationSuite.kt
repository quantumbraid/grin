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

fun validateMagic(bytes: ByteArray): GrinValidationResult {
    if (bytes.size != GrinFormat.MAGIC_BYTES.size) {
        return fail("Magic must be ${GrinFormat.MAGIC_BYTES.size} bytes")
    }
    if (!bytes.contentEquals(GrinFormat.MAGIC_BYTES)) {
        return fail("Magic bytes do not match GRIN")
    }
    return ok()
}

fun validateVersion(major: Int, minor: Int): GrinValidationResult {
    if (major != GrinFormat.VERSION_MAJOR || minor != GrinFormat.VERSION_MINOR) {
        return warn("Unexpected GRIN version")
    }
    return ok()
}

fun validateHeaderSize(size: Int): GrinValidationResult {
    if (size != GrinFormat.HEADER_SIZE_BYTES) {
        return fail("HeaderSize must be ${GrinFormat.HEADER_SIZE_BYTES}")
    }
    return ok()
}

fun validateDimensions(width: Long, height: Long): GrinValidationResult {
    if (width < 0L || height < 0L) {
        return fail("Width and height must be non-negative")
    }
    if (width > 0xFFFFFFFFL || height > 0xFFFFFFFFL) {
        return fail("Width and height must fit in uint32")
    }
    return ok()
}

fun validateTickMicros(tick: Long): GrinValidationResult {
    if (tick < 0L || tick > 0xFFFFFFFFL) {
        return fail("TickMicros must fit in uint32")
    }
    return ok()
}

fun validateRuleCount(count: Int): GrinValidationResult {
    if (count < 0 || count > GrinFormat.MAX_RULE_COUNT) {
        return fail("RuleCount must be 0-${GrinFormat.MAX_RULE_COUNT}")
    }
    return ok()
}

fun validateOpcodeSetId(id: Int): GrinValidationResult {
    if (id < 0 || id > 0xFF) {
        return fail("OpcodeSetId must fit in uint8")
    }
    if (id != 0) {
        return fail("Unknown OpcodeSetId")
    }
    return ok()
}

fun validatePixelDataLength(length: Long, width: Long, height: Long): GrinValidationResult {
    if (length < 0L) {
        return fail("PixelDataLength must be non-negative")
    }
    val dimResult = validateDimensions(width, height)
    if (!dimResult.ok) {
        return dimResult
    }
    val pixelCount = safeMultiply(width, height)
        ?: return fail("PixelDataLength overflow for width/height")
    val expected = safeMultiply(pixelCount, GrinFormat.PIXEL_SIZE_BYTES.toLong())
        ?: return fail("PixelDataLength overflow for width/height")
    if (length != expected) {
        return fail("PixelDataLength does not match width * height * 5")
    }
    return ok()
}

fun validateFileLength(fileLen: Long, dataLen: Long): GrinValidationResult {
    if (fileLen < 0L) {
        return fail("FileLength must be non-negative")
    }
    val minLen = GrinFormat.HEADER_SIZE_BYTES.toLong() + dataLen
    if (fileLen != 0L && fileLen < minLen) {
        return fail("FileLength is smaller than header + pixel data")
    }
    return ok()
}

fun validatePixelDataOffset(offset: Long): GrinValidationResult {
    if (offset != GrinFormat.HEADER_SIZE_BYTES.toLong()) {
        return fail("PixelDataOffset64 must be 128")
    }
    return ok()
}

fun validateReservedFields(reservedA: Long, reservedB: Long, flags: Int): GrinValidationResult {
    if (reservedA != 0L || reservedB != 0L || flags != 0) {
        return warn("Reserved header fields are non-zero")
    }
    return ok()
}

fun validateControlByte(controlByte: Int): GrinValidationResult {
    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()

    if (controlByte < 0 || controlByte > 0xFF) {
        errors.add("Control byte must fit in uint8")
    } else {
        val groupResult = validateGroupId(controlByte and GrinControlByte.GROUP_ID_MASK)
        mergeResult(errors, warnings, groupResult)
        val reservedResult = validateReservedBits(controlByte)
        mergeResult(errors, warnings, reservedResult)
    }
    return finalize(errors, warnings)
}

fun validateGroupId(groupId: Int): GrinValidationResult {
    if (groupId < 0 || groupId > 0x0F) {
        return fail("Group ID must be 0-15")
    }
    return ok()
}

fun validateReservedBits(controlByte: Int): GrinValidationResult {
    if ((controlByte and GrinControlByte.RESERVED_MASK) != 0) {
        return warn("Control byte reserved bits are non-zero")
    }
    return ok()
}

fun validateGroupMask(mask: Int): GrinValidationResult {
    if (mask < 0 || mask > 0xFFFF) {
        return fail("GroupMask must fit in uint16")
    }
    return ok()
}

fun validateOpcode(opcode: Int, opcodeSetId: Int): GrinValidationResult {
    if (opcode < 0 || opcode > 0xFF) {
        return fail("Opcode must fit in uint8")
    }
    if (opcodeSetId != 0) {
        return fail("Unknown OpcodeSetId")
    }
    if (opcode > 0x0C) {
        return fail("Unknown opcode for base set")
    }
    return ok()
}

fun validateTiming(timing: Int): GrinValidationResult {
    if (timing < 0 || timing > 0xFF) {
        return fail("Timing must fit in uint8")
    }
    return ok()
}

fun validateGrinFile(
    file: GrinFile,
    mode: ValidationMode = ValidationMode.PERMISSIVE
): ValidationReport {
    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()
    val info = mutableListOf<String>()

    val header = file.header
    mergeResult(errors, warnings, validateMagic(header.magic))
    mergeResult(errors, warnings, validateVersion(header.versionMajor, header.versionMinor))
    mergeResult(errors, warnings, validateHeaderSize(header.headerSize))
    mergeResult(errors, warnings, validateDimensions(header.width, header.height))
    mergeResult(errors, warnings, validateTickMicros(header.tickMicros))
    mergeResult(errors, warnings, validateRuleCount(header.ruleCount))
    mergeResult(errors, warnings, validateOpcodeSetId(header.opcodeSetId))
    mergeResult(errors, warnings, validatePixelDataLength(header.pixelDataLength, header.width, header.height))
    mergeResult(errors, warnings, validateFileLength(header.fileLength, header.pixelDataLength))
    mergeResult(errors, warnings, validatePixelDataOffset(header.pixelDataOffset))
    mergeResult(errors, warnings, validateReservedFields(header.reservedA, header.reservedB, header.flags))

    val expectedPixelCount = safeMultiply(header.width, header.height)
    if (expectedPixelCount == null) {
        errors.add("Pixel count exceeds safe range")
    } else if (expectedPixelCount > Int.MAX_VALUE.toLong()) {
        errors.add("Pixel count exceeds Int range")
    } else if (file.pixels.size != expectedPixelCount.toInt()) {
        errors.add("Pixel array length does not match header dimensions")
    }

    if (file.rules.size != header.ruleCount) {
        errors.add("Rule array length does not match header ruleCount")
    }

    var reservedBitsCount = 0
    for (pixel in file.pixels) {
        if ((pixel.c and GrinControlByte.RESERVED_MASK) != 0) {
            reservedBitsCount += 1
        }
    }
    if (reservedBitsCount > 0) {
        warnings.add("Control byte reserved bits set on $reservedBitsCount pixels")
    }

    file.rules.forEachIndexed { index, rule ->
        prefixResult(errors, warnings, validateGroupMask(rule.groupMask), "Rule $index: ")
        prefixResult(errors, warnings, validateOpcode(rule.opcode, header.opcodeSetId), "Rule $index: ")
        prefixResult(errors, warnings, validateTiming(rule.timing), "Rule $index: ")
    }

    info.add("Pixels: ${file.pixels.size}")
    info.add("Rules: ${file.rules.size}")

    val ok = if (mode == ValidationMode.STRICT) {
        errors.isEmpty() && warnings.isEmpty()
    } else {
        errors.isEmpty()
    }

    return ValidationReport(ok, errors, warnings, info)
}

private fun ok(): GrinValidationResult = GrinValidationResult(true, emptyList(), emptyList())

private fun warn(message: String): GrinValidationResult =
    GrinValidationResult(true, emptyList(), listOf(message))

private fun fail(message: String): GrinValidationResult =
    GrinValidationResult(false, listOf(message), emptyList())

private fun finalize(
    errors: List<String>,
    warnings: List<String>
): GrinValidationResult = GrinValidationResult(errors.isEmpty(), errors, warnings)

private fun mergeResult(
    errors: MutableList<String>,
    warnings: MutableList<String>,
    result: GrinValidationResult
) {
    errors.addAll(result.errors)
    warnings.addAll(result.warnings)
}

private fun prefixResult(
    errors: MutableList<String>,
    warnings: MutableList<String>,
    result: GrinValidationResult,
    prefix: String
) {
    errors.addAll(result.errors.map { "$prefix$it" })
    warnings.addAll(result.warnings.map { "$prefix$it" })
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
