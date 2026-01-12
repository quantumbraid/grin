package io.grin.lib

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test

class GrinReferenceTest {
    @Test
    fun `serializes minimal file to reference bytes`() {
        val header = GrinHeader(
            magic = GrinFormat.MAGIC_BYTES,
            versionMajor = GrinFormat.VERSION_MAJOR,
            versionMinor = GrinFormat.VERSION_MINOR,
            headerSize = GrinFormat.HEADER_SIZE_BYTES,
            width = 1,
            height = 1,
            tickMicros = 0,
            ruleCount = 0,
            opcodeSetId = 0,
            flags = 0,
            pixelDataLength = 5,
            fileLength = 133,
            pixelDataOffset = 128,
            reservedA = 0,
            reservedB = 0,
            rulesBlock = ByteArray(GrinFormat.RULES_BLOCK_SIZE)
        )
        val pixel = GrinPixel(1, 2, 3, 4, 0)
        val file = GrinFile(header, listOf(pixel), emptyList())

        val expected = ByteArray(GrinFormat.HEADER_SIZE_BYTES + 5)
        expected[0] = 0x47
        expected[1] = 0x52
        expected[2] = 0x49
        expected[3] = 0x4E
        expected[6] = 0x80.toByte()
        expected[8] = 0x01
        expected[12] = 0x01
        expected[24] = 0x05
        expected[32] = 0x85.toByte()
        expected[40] = 0x80.toByte()
        expected[GrinFormat.HEADER_SIZE_BYTES] = 1
        expected[GrinFormat.HEADER_SIZE_BYTES + 1] = 2
        expected[GrinFormat.HEADER_SIZE_BYTES + 2] = 3
        expected[GrinFormat.HEADER_SIZE_BYTES + 3] = 4
        expected[GrinFormat.HEADER_SIZE_BYTES + 4] = 0

        assertArrayEquals(expected, file.toBytes())
    }
}
