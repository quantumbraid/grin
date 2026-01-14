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
package io.grin.demo

import android.graphics.Bitmap
import android.graphics.Color
import java.io.File
import java.io.OutputStream
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.max

// Encodes a list of ARGB_8888 frames into a looping GIF.
class GifEncoder {
    fun encode(frames: List<Bitmap>, delayMs: Int, output: File) {
        output.outputStream().use { stream ->
            encode(frames, delayMs, stream)
        }
    }

    fun encode(frames: List<Bitmap>, delayMs: Int, output: OutputStream) {
        require(frames.isNotEmpty()) { "GIF export requires at least one frame" }
        val width = frames.first().width
        val height = frames.first().height
        frames.forEach { frame ->
            require(frame.width == width && frame.height == height) {
                "All GIF frames must share the same dimensions"
            }
        }

        val palette = buildPalette(frames.first())
        val paletteSize = nextPowerOfTwo(max(2, palette.colors.size))
        val colorDepth = max(2, ceil(log2(paletteSize.toDouble())).toInt())
        val globalColorTable = buildColorTable(palette.colors, paletteSize)

        writeHeader(output)
        writeLogicalScreenDescriptor(output, width, height, colorDepth)
        output.write(globalColorTable)
        writeLoopExtension(output)

        frames.forEach { frame ->
            val pixels = mapPixelsToPalette(frame, palette)
            writeGraphicsControlExtension(output, delayMs)
            writeImageDescriptor(output, width, height)
            GifLzwEncoder(width, height, pixels, colorDepth).encode(output)
        }

        output.write(0x3B) // GIF trailer.
    }

    private fun buildPalette(bitmap: Bitmap): PaletteResult {
        // Build a stable palette ordered by frequency for posterized frames.
        val counts = linkedMapOf<Int, Int>()
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val color = bitmap.getPixel(x, y)
                counts[color] = (counts[color] ?: 0) + 1
            }
        }
        val sortedColors = counts.entries
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            .map { it.key }
        require(sortedColors.size <= 256) { "GIF palette exceeds 256 colors" }
        val mapping = sortedColors.mapIndexed { index, color -> color to index }.toMap()
        return PaletteResult(sortedColors, mapping)
    }

    private fun buildColorTable(colors: List<Int>, size: Int): ByteArray {
        // Build the RGB color table padded to a power-of-two length.
        val table = ByteArray(size * 3)
        colors.forEachIndexed { index, color ->
            table[index * 3] = Color.red(color).toByte()
            table[index * 3 + 1] = Color.green(color).toByte()
            table[index * 3 + 2] = Color.blue(color).toByte()
        }
        return table
    }

    private fun mapPixelsToPalette(bitmap: Bitmap, palette: PaletteResult): ByteArray {
        // Convert ARGB pixels to palette indices for LZW encoding.
        val pixels = ByteArray(bitmap.width * bitmap.height)
        var offset = 0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val color = bitmap.getPixel(x, y)
                pixels[offset] = (palette.colorToIndex[color] ?: 0).toByte()
                offset += 1
            }
        }
        return pixels
    }

    private fun writeHeader(stream: OutputStream) {
        // Write the GIF header signature.
        stream.write("GIF89a".toByteArray())
    }

    private fun writeLogicalScreenDescriptor(stream: OutputStream, width: Int, height: Int, colorDepth: Int) {
        // Define global palette usage and canvas size.
        writeShort(stream, width)
        writeShort(stream, height)
        val packed = 0x80 or ((colorDepth - 1) shl 4) or (colorDepth - 1)
        stream.write(packed)
        stream.write(0) // Background color index.
        stream.write(0) // Pixel aspect ratio.
    }

    private fun writeLoopExtension(stream: OutputStream) {
        // Enable infinite looping for playback.
        stream.write(0x21)
        stream.write(0xFF)
        stream.write(0x0B)
        stream.write("NETSCAPE2.0".toByteArray())
        stream.write(0x03)
        stream.write(0x01)
        writeShort(stream, 0)
        stream.write(0x00)
    }

    private fun writeGraphicsControlExtension(stream: OutputStream, delayMs: Int) {
        // Configure per-frame delay and disposal.
        stream.write(0x21)
        stream.write(0xF9)
        stream.write(0x04)
        stream.write(0x00)
        writeShort(stream, (delayMs / 10).coerceAtLeast(1))
        stream.write(0x00) // Transparent index.
        stream.write(0x00)
    }

    private fun writeImageDescriptor(stream: OutputStream, width: Int, height: Int) {
        // Describe the image frame and use the global color table.
        stream.write(0x2C)
        writeShort(stream, 0)
        writeShort(stream, 0)
        writeShort(stream, width)
        writeShort(stream, height)
        stream.write(0x00)
    }

    private fun writeShort(stream: OutputStream, value: Int) {
        // Write a 16-bit little-endian value.
        stream.write(value and 0xFF)
        stream.write((value shr 8) and 0xFF)
    }

    private fun nextPowerOfTwo(value: Int): Int {
        // Compute the next power-of-two size for the GIF color table.
        var result = 1
        while (result < value) {
            result = result shl 1
        }
        return result
    }

    private data class PaletteResult(
        val colors: List<Int>,
        val colorToIndex: Map<Int, Int>
    )
}

// Minimal LZW encoder for GIF palette indices.
private class GifLzwEncoder(
    private val width: Int,
    private val height: Int,
    private val pixels: ByteArray,
    private val colorDepth: Int
) {
    private val initCodeSize = max(2, colorDepth)
    private val clearCode = 1 shl initCodeSize
    private val eofCode = clearCode + 1
    private var remaining = width * height
    private var pixelIndex = 0
    private var curAccum = 0
    private var curBits = 0
    private var aCount = 0
    private val accum = ByteArray(256)

    fun encode(output: OutputStream) {
        // Write initial code size for the image data.
        output.write(initCodeSize)
        compress(output)
        output.write(0x00)
    }

    private fun compress(output: OutputStream) {
        // Perform GIF LZW compression using a hash table.
        val hsize = 5003
        val htab = IntArray(hsize) { -1 }
        val codetab = IntArray(hsize)
        var freeEnt = eofCode + 1
        var nBits = initCodeSize + 1
        var maxCode = (1 shl nBits) - 1
        val maxMaxCode = 1 shl 12
        val clearFlag = BooleanArray(1)

        outputCode(clearCode, output, nBits, maxCode, clearFlag)

        var ent = nextPixel()
        while (true) {
            val c = nextPixel()
            if (c == -1) {
                break
            }
            val fcode = (c shl 12) + ent
            var i = (c shl 8) xor ent
            if (htab[i] == fcode) {
                ent = codetab[i]
                continue
            } else if (htab[i] >= 0) {
                val disp = hsize - i
                if (i == 0) {
                    i = 1
                }
                while (true) {
                    i -= disp
                    if (i < 0) {
                        i += hsize
                    }
                    if (htab[i] == fcode) {
                        ent = codetab[i]
                        break
                    }
                    if (htab[i] < 0) {
                        break
                    }
                }
                if (htab[i] == fcode) {
                    continue
                }
            }

            outputCode(ent, output, nBits, maxCode, clearFlag)
            ent = c
            if (freeEnt < maxMaxCode) {
                codetab[i] = freeEnt
                htab[i] = fcode
                freeEnt += 1
            } else {
                clearFlag[0] = true
                outputCode(clearCode, output, nBits, maxCode, clearFlag)
                htab.fill(-1)
                freeEnt = eofCode + 1
            }

            if (freeEnt > maxCode && !clearFlag[0]) {
                nBits += 1
                if (nBits == 12) {
                    maxCode = maxMaxCode - 1
                } else {
                    maxCode = (1 shl nBits) - 1
                }
            }
        }

        outputCode(ent, output, nBits, maxCode, clearFlag)
        outputCode(eofCode, output, nBits, maxCode, clearFlag)
    }

    private fun nextPixel(): Int {
        // Read the next palette index from the pixel stream.
        if (remaining == 0) {
            return -1
        }
        remaining -= 1
        val value = pixels[pixelIndex].toInt() and 0xFF
        pixelIndex += 1
        return value
    }

    private fun outputCode(code: Int, output: OutputStream, nBits: Int, maxCode: Int, clearFlag: BooleanArray) {
        // Buffer and output variable-length codes as GIF sub-blocks.
        if (clearFlag[0]) {
            curAccum = 0
            curBits = 0
            clearFlag[0] = false
        }
        curAccum = curAccum or (code shl curBits)
        curBits += nBits
        while (curBits >= 8) {
            charOut(curAccum and 0xFF, output)
            curAccum = curAccum shr 8
            curBits -= 8
        }
        if (code == eofCode) {
            // Flush trailing bits on EOF.
            while (curBits > 0) {
                charOut(curAccum and 0xFF, output)
                curAccum = curAccum shr 8
                curBits -= 8
            }
            flush(output)
        }
    }

    private fun charOut(value: Int, output: OutputStream) {
        // Write encoded bytes into the current sub-block.
        accum[aCount] = value.toByte()
        aCount += 1
        if (aCount >= 254) {
            flush(output)
        }
    }

    private fun flush(output: OutputStream) {
        // Flush the current sub-block to the output stream.
        if (aCount > 0) {
            output.write(aCount)
            output.write(accum, 0, aCount)
            aCount = 0
        }
    }
}
