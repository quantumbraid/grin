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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Movie
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.grin.lib.GrinFile
import io.grin.lib.GrinFormat
import io.grin.lib.GrinHeader
import io.grin.lib.GrinPixel
import io.grin.lib.GrinRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class GrinAssetStoreTest {
    private lateinit var context: Context
    private lateinit var store: GrinAssetStore

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        purgeStoreFiles()
        store = GrinAssetStore(context)
    }

    @After
    fun tearDown() {
        purgeStoreFiles()
    }

    @Test
    fun assignBinsToChannelsStableOrder() {
        val paletteColors = intArrayOf(Color.RED, Color.GREEN, Color.BLUE)
        val histogram = intArrayOf(10, 3, 10)
        val paletteIndices = intArrayOf(0, 2, 0, 1)
        val frame = buildFrame(paletteColors, paletteIndices, histogram, 2, 2)

        val metadata = store.createAssetFromFrame(frame, tickMicros = 33_333L)

        assertEquals(3, metadata.paletteBins)
        assertEquals(listOf(0, 2, 1), metadata.channelMap)
        assertEquals(listOf(0, 1, 2), metadata.channelOrder)
    }

    @Test
    fun buildUpdatedGrinFileUpdatesHeaderAndRules() {
        val baseFile = buildBaseFile(width = 2, height = 2)
        val metadata = buildMetadata(
            width = 2,
            height = 2,
            tickMicros = 25_000L,
            channelOrder = listOf(2, 1, 2)
        )
        val settings = buildSettings()

        val updated = store.buildUpdatedGrinFile(metadata, baseFile, settings)

        assertEquals(2L, updated.header.width)
        assertEquals(2L, updated.header.height)
        assertEquals(25_000L, updated.header.tickMicros)
        assertEquals(2, updated.header.ruleCount)
        assertEquals(2, updated.rules.size)
        assertEquals(1 shl 2, updated.rules[0].groupMask)
        assertEquals(1 shl 1, updated.rules[1].groupMask)
        assertEquals(GrinFormat.HEADER_SIZE_BYTES.toLong(), updated.header.pixelDataOffset)
        assertEquals(20L, updated.header.pixelDataLength)
        assertEquals(GrinFormat.HEADER_SIZE_BYTES.toLong() + 20L, updated.header.fileLength)
    }

    @Test
    fun exportPngRoundTrip() {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
            setPixel(0, 0, Color.RED)
            setPixel(1, 0, Color.GREEN)
            setPixel(0, 1, Color.BLUE)
            setPixel(1, 1, Color.WHITE)
        }

        val file = store.exportPng(bitmap, "roundtrip.png")
        val decoded = BitmapFactory.decodeFile(file.absolutePath)

        assertTrue(file.exists())
        assertNotNull(decoded)
        assertEquals(2, decoded.width)
        assertEquals(2, decoded.height)
        assertEquals(Color.RED, decoded.getPixel(0, 0))
    }

    @Test
    fun exportGifRoundTrip() {
        val frameA = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
        }
        val frameB = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLUE)
        }

        val file = store.exportGif("loop.gif", listOf(frameA, frameB), delayMs = 80)
        val movie = Movie.decodeFile(file.absolutePath)

        assertTrue(file.exists())
        assertTrue(file.length() > 0)
        assertNotNull(movie)
        assertEquals(2, movie.width())
        assertEquals(2, movie.height())
    }

    private fun buildFrame(
        paletteColors: IntArray,
        paletteIndices: IntArray,
        histogram: IntArray,
        gridCols: Int,
        gridRows: Int
    ): PosterizedFrame {
        val bitmap = Bitmap.createBitmap(gridCols, gridRows, Bitmap.Config.ARGB_8888)
        for (index in paletteIndices.indices) {
            val color = paletteColors[paletteIndices[index]]
            bitmap.setPixel(index % gridCols, index / gridCols, color)
        }
        val labels = paletteColors.indices.map { ChannelLabels.labelFor(it) }
        return PosterizedFrame(
            bitmap = bitmap,
            gridCols = gridCols,
            gridRows = gridRows,
            paletteIndices = paletteIndices,
            paletteLabels = labels,
            paletteSize = paletteColors.size,
            paletteHistogram = histogram,
            paletteColors = paletteColors.copyOf()
        )
    }

    private fun buildBaseFile(width: Int, height: Int): GrinFile {
        val pixelDataLength = width.toLong() * height.toLong() * GrinFormat.PIXEL_SIZE_BYTES.toLong()
        val header = GrinHeader(
            magic = GrinFormat.MAGIC_BYTES,
            versionMajor = GrinFormat.VERSION_MAJOR,
            versionMinor = GrinFormat.VERSION_MINOR,
            headerSize = GrinFormat.HEADER_SIZE_BYTES,
            width = width.toLong(),
            height = height.toLong(),
            tickMicros = 33_333L,
            ruleCount = 0,
            opcodeSetId = 0,
            flags = 0,
            pixelDataLength = pixelDataLength,
            fileLength = GrinFormat.HEADER_SIZE_BYTES.toLong() + pixelDataLength,
            pixelDataOffset = GrinFormat.HEADER_SIZE_BYTES.toLong(),
            reservedA = 0,
            reservedB = 0,
            rulesBlock = ByteArray(GrinFormat.RULES_BLOCK_SIZE)
        )
        val pixels = List(width * height) { GrinPixel(0, 0, 0, 255, 0) }
        return GrinFile(header, pixels, emptyList())
    }

    private fun buildMetadata(
        width: Int,
        height: Int,
        tickMicros: Long,
        channelOrder: List<Int>
    ): GrinAssetMetadata {
        val baseDir = File(context.filesDir, "grin_gallery")
        val stubPath = File(baseDir, "stub").absolutePath
        return GrinAssetMetadata(
            id = "test",
            createdAt = 0L,
            lastEditedAt = 0L,
            width = width,
            height = height,
            gridCols = width,
            gridRows = height,
            paletteBins = channelOrder.size,
            tickMicros = tickMicros,
            ruleCount = 0,
            channelMap = channelOrder,
            channelOrder = channelOrder,
            paletteHistogram = List(channelOrder.size) { 0 },
            previewPath = stubPath,
            thumbnailPath = stubPath,
            grinPath = stubPath,
            grimPath = stubPath,
            channelSettings = buildSettings()
        )
    }

    private fun buildSettings(): List<ChannelSetting> {
        return (0..15).map { channelId ->
            ChannelSetting(
                channelId = channelId,
                frequency = 50,
                intonation = 50,
                transparency = 100
            )
        }
    }

    private fun purgeStoreFiles() {
        File(context.filesDir, "grin_gallery").deleteRecursively()
    }
}
