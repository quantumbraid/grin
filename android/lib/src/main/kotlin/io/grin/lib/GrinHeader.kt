package io.grin.lib

import java.nio.ByteBuffer
import java.nio.ByteOrder

class GrinHeader(
    magic: ByteArray,
    val versionMajor: Int,
    val versionMinor: Int,
    val headerSize: Int,
    val width: Long,
    val height: Long,
    val tickMicros: Long,
    val ruleCount: Int,
    val opcodeSetId: Int,
    val flags: Int,
    val pixelDataLength: Long,
    val fileLength: Long,
    val pixelDataOffset: Long,
    val reservedA: Long,
    val reservedB: Long,
    rulesBlock: ByteArray
) {
    val magic: ByteArray = magic.copyOf()
    val rulesBlock: ByteArray = rulesBlock.copyOf()

    init {
        require(this.magic.size == GrinHeaderLayout.MAGIC_SIZE) {
            "Magic bytes must be ${GrinHeaderLayout.MAGIC_SIZE} bytes"
        }
        require(this.rulesBlock.size == GrinFormat.RULES_BLOCK_SIZE) {
            "RulesBlock must be ${GrinFormat.RULES_BLOCK_SIZE} bytes"
        }
    }

    fun serialize(): ByteArray {
        val buffer = ByteBuffer.allocate(GrinFormat.HEADER_SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        putBytes(buffer, GrinHeaderLayout.MAGIC_OFFSET, magic)
        putUInt8(buffer, GrinHeaderLayout.VERSION_MAJOR_OFFSET, versionMajor)
        putUInt8(buffer, GrinHeaderLayout.VERSION_MINOR_OFFSET, versionMinor)
        putUInt16(buffer, GrinHeaderLayout.HEADER_SIZE_OFFSET, headerSize)
        putUInt32(buffer, GrinHeaderLayout.WIDTH_OFFSET, width)
        putUInt32(buffer, GrinHeaderLayout.HEIGHT_OFFSET, height)
        putUInt32(buffer, GrinHeaderLayout.TICK_MICROS_OFFSET, tickMicros)
        putUInt8(buffer, GrinHeaderLayout.RULE_COUNT_OFFSET, ruleCount)
        putUInt8(buffer, GrinHeaderLayout.OPCODE_SET_ID_OFFSET, opcodeSetId)
        putUInt16(buffer, GrinHeaderLayout.FLAGS_OFFSET, flags)
        putUInt64(buffer, GrinHeaderLayout.PIXEL_DATA_LENGTH_OFFSET, pixelDataLength)
        putUInt64(buffer, GrinHeaderLayout.FILE_LENGTH_OFFSET, fileLength)
        putUInt64(buffer, GrinHeaderLayout.PIXEL_DATA_OFFSET_OFFSET, pixelDataOffset)
        putUInt64(buffer, GrinHeaderLayout.RESERVED_A_OFFSET, reservedA)
        putUInt64(buffer, GrinHeaderLayout.RESERVED_B_OFFSET, reservedB)
        putBytes(buffer, GrinHeaderLayout.RULES_BLOCK_OFFSET, rulesBlock)
        return buffer.array()
    }

    fun validate(): GrinValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (!magic.contentEquals(GrinFormat.MAGIC_BYTES)) {
            errors.add("Magic bytes do not match GRIN")
        }
        if (headerSize != GrinFormat.HEADER_SIZE_BYTES) {
            errors.add("HeaderSize must be ${GrinFormat.HEADER_SIZE_BYTES}")
        }
        if (ruleCount > GrinFormat.MAX_RULE_COUNT) {
            errors.add("RuleCount exceeds ${GrinFormat.MAX_RULE_COUNT}")
        }
        if (pixelDataOffset != GrinFormat.HEADER_SIZE_BYTES.toLong()) {
            errors.add("PixelDataOffset64 must be 128")
        }

        val expectedPixelLength = expectedPixelLength(width, height)
        if (expectedPixelLength == null) {
            errors.add("PixelDataLength overflow for width/height")
        } else if (pixelDataLength != expectedPixelLength) {
            errors.add("PixelDataLength does not match width * height * 5")
        }

        val minFileLength = GrinFormat.HEADER_SIZE_BYTES.toLong() + pixelDataLength
        if (fileLength != 0L && fileLength < minFileLength) {
            errors.add("FileLength is smaller than header + pixel data")
        }

        if (flags != 0) {
            warnings.add("Flags field is non-zero")
        }
        if (reservedA != 0L || reservedB != 0L) {
            warnings.add("Reserved header fields are non-zero")
        }
        if (versionMajor != GrinFormat.VERSION_MAJOR || versionMinor != GrinFormat.VERSION_MINOR) {
            warnings.add("Unexpected GRIN version")
        }

        return GrinValidationResult(errors.isEmpty(), errors, warnings)
    }

    fun copy(
        magic: ByteArray = this.magic,
        versionMajor: Int = this.versionMajor,
        versionMinor: Int = this.versionMinor,
        headerSize: Int = this.headerSize,
        width: Long = this.width,
        height: Long = this.height,
        tickMicros: Long = this.tickMicros,
        ruleCount: Int = this.ruleCount,
        opcodeSetId: Int = this.opcodeSetId,
        flags: Int = this.flags,
        pixelDataLength: Long = this.pixelDataLength,
        fileLength: Long = this.fileLength,
        pixelDataOffset: Long = this.pixelDataOffset,
        reservedA: Long = this.reservedA,
        reservedB: Long = this.reservedB,
        rulesBlock: ByteArray = this.rulesBlock
    ): GrinHeader {
        return GrinHeader(
            magic = magic,
            versionMajor = versionMajor,
            versionMinor = versionMinor,
            headerSize = headerSize,
            width = width,
            height = height,
            tickMicros = tickMicros,
            ruleCount = ruleCount,
            opcodeSetId = opcodeSetId,
            flags = flags,
            pixelDataLength = pixelDataLength,
            fileLength = fileLength,
            pixelDataOffset = pixelDataOffset,
            reservedA = reservedA,
            reservedB = reservedB,
            rulesBlock = rulesBlock
        )
    }

    companion object {
        fun deserialize(bytes: ByteArray): GrinHeader {
            require(bytes.size >= GrinFormat.HEADER_SIZE_BYTES) {
                "Header requires ${GrinFormat.HEADER_SIZE_BYTES} bytes"
            }
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

            val magic = bytes.copyOfRange(
                GrinHeaderLayout.MAGIC_OFFSET,
                GrinHeaderLayout.MAGIC_OFFSET + GrinHeaderLayout.MAGIC_SIZE
            )
            val versionMajor = readUInt8(buffer, GrinHeaderLayout.VERSION_MAJOR_OFFSET)
            val versionMinor = readUInt8(buffer, GrinHeaderLayout.VERSION_MINOR_OFFSET)
            val headerSize = readUInt16(buffer, GrinHeaderLayout.HEADER_SIZE_OFFSET)
            val width = readUInt32(buffer, GrinHeaderLayout.WIDTH_OFFSET)
            val height = readUInt32(buffer, GrinHeaderLayout.HEIGHT_OFFSET)
            val tickMicros = readUInt32(buffer, GrinHeaderLayout.TICK_MICROS_OFFSET)
            val ruleCount = readUInt8(buffer, GrinHeaderLayout.RULE_COUNT_OFFSET)
            val opcodeSetId = readUInt8(buffer, GrinHeaderLayout.OPCODE_SET_ID_OFFSET)
            val flags = readUInt16(buffer, GrinHeaderLayout.FLAGS_OFFSET)
            val pixelDataLength = readUInt64(buffer, GrinHeaderLayout.PIXEL_DATA_LENGTH_OFFSET)
            val fileLength = readUInt64(buffer, GrinHeaderLayout.FILE_LENGTH_OFFSET)
            val pixelDataOffset = readUInt64(buffer, GrinHeaderLayout.PIXEL_DATA_OFFSET_OFFSET)
            val reservedA = readUInt64(buffer, GrinHeaderLayout.RESERVED_A_OFFSET)
            val reservedB = readUInt64(buffer, GrinHeaderLayout.RESERVED_B_OFFSET)
            val rulesBlock = bytes.copyOfRange(
                GrinHeaderLayout.RULES_BLOCK_OFFSET,
                GrinHeaderLayout.RULES_BLOCK_OFFSET + GrinFormat.RULES_BLOCK_SIZE
            )

            return GrinHeader(
                magic = magic,
                versionMajor = versionMajor,
                versionMinor = versionMinor,
                headerSize = headerSize,
                width = width,
                height = height,
                tickMicros = tickMicros,
                ruleCount = ruleCount,
                opcodeSetId = opcodeSetId,
                flags = flags,
                pixelDataLength = pixelDataLength,
                fileLength = fileLength,
                pixelDataOffset = pixelDataOffset,
                reservedA = reservedA,
                reservedB = reservedB,
                rulesBlock = rulesBlock
            )
        }
    }
}

private fun expectedPixelLength(width: Long, height: Long): Long? {
    if (width < 0L || height < 0L) {
        return null
    }
    val pixelCount = width * height
    if (width != 0L && pixelCount / width != height) {
        return null
    }
    val pixelLength = pixelCount * GrinFormat.PIXEL_SIZE_BYTES.toLong()
    if (pixelCount != 0L && pixelLength / pixelCount != GrinFormat.PIXEL_SIZE_BYTES.toLong()) {
        return null
    }
    return pixelLength
}

private fun readUInt8(buffer: ByteBuffer, offset: Int): Int {
    return buffer.get(offset).toInt() and 0xFF
}

private fun readUInt16(buffer: ByteBuffer, offset: Int): Int {
    return buffer.getShort(offset).toInt() and 0xFFFF
}

private fun readUInt32(buffer: ByteBuffer, offset: Int): Long {
    return buffer.getInt(offset).toLong() and 0xFFFFFFFFL
}

private fun readUInt64(buffer: ByteBuffer, offset: Int): Long {
    return buffer.getLong(offset)
}

private fun putUInt8(buffer: ByteBuffer, offset: Int, value: Int) {
    buffer.put(offset, (value and 0xFF).toByte())
}

private fun putUInt16(buffer: ByteBuffer, offset: Int, value: Int) {
    buffer.putShort(offset, (value and 0xFFFF).toShort())
}

private fun putUInt32(buffer: ByteBuffer, offset: Int, value: Long) {
    buffer.putInt(offset, (value and 0xFFFFFFFFL).toInt())
}

private fun putUInt64(buffer: ByteBuffer, offset: Int, value: Long) {
    buffer.putLong(offset, value)
}

private fun putBytes(buffer: ByteBuffer, offset: Int, bytes: ByteArray) {
    var index = 0
    while (index < bytes.size) {
        buffer.put(offset + index, bytes[index])
        index += 1
    }
}
