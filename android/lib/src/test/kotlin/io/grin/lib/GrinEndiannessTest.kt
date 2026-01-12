package io.grin.lib

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class GrinEndiannessTest {
    @Test
    fun writeUint16LEWritesExpectedBytes() {
        val bytes = writeUint16LE(0x1234)
        assertArrayEquals(byteArrayOf(0x34, 0x12), bytes)
    }

    @Test
    fun writeUint32LEWritesExpectedBytes() {
        val bytes = writeUint32LE(0x78563412)
        assertArrayEquals(byteArrayOf(0x12, 0x34, 0x56, 0x78), bytes)
    }

    @Test
    fun writeUint64LEWritesExpectedBytes() {
        val bytes = writeUint64LE(0x0807060504030201)
        assertArrayEquals(
            byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08),
            bytes
        )
    }

    @Test
    fun readUint16LEReadsExpectedValue() {
        val value = readUint16LE(byteArrayOf(0x34, 0x12), 0)
        assertEquals(0x1234, value)
    }

    @Test
    fun readUint32LEReadsExpectedValue() {
        val value = readUint32LE(byteArrayOf(0x12, 0x34, 0x56, 0x78), 0)
        assertEquals(0x78563412, value)
    }

    @Test
    fun readUint64LEReadsExpectedValue() {
        val value = readUint64LE(
            byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08),
            0
        )
        assertEquals(0x0807060504030201, value)
    }
}
