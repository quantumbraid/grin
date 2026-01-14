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
import android.net.Uri
import android.util.Log
import io.grin.lib.BaseOpcodes
import io.grin.lib.GrinFile
import io.grin.lib.GrinFormat
import io.grin.lib.GrinHeader
import io.grin.lib.GrinPixel
import io.grin.lib.GrinRule
import java.io.File
import java.io.OutputStream
import java.util.UUID
import kotlin.math.roundToInt

// Handles disk persistence for GRIM/GRIN assets, previews, and metadata JSON.
class GrinAssetStore(private val context: Context) {
    private val baseDir = File(context.filesDir, "grin_gallery")
    private val metadataDir = File(baseDir, "metadata")
    private val previewDir = File(baseDir, "preview")
    private val thumbnailDir = File(baseDir, "thumbnails")
    private val grinDir = File(baseDir, "grin")
    private val grimDir = File(baseDir, "grim")
    private val exportDir = File(baseDir, "exports")

    init {
        // Ensure storage directories exist before any writes.
        listOf(metadataDir, previewDir, thumbnailDir, grinDir, grimDir, exportDir).forEach { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
    }

    fun createAssetFromFrame(frame: PosterizedFrame, tickMicros: Long): GrinAssetMetadata {
        // Build channel mapping and histogram from the captured frame.
        val assignment = assignBinsToChannels(frame.paletteHistogram)
        val channelMap = assignment.channelMap
        val channelOrder = assignment.channelOrder
        val channelSettings = buildDefaultChannelSettings(channelOrder)
        val pixels = buildGrinPixels(frame, channelMap)
        val rules = buildRules(channelSettings, assignment.activeChannels)

        val header = buildHeader(
            width = frame.gridCols,
            height = frame.gridRows,
            tickMicros = tickMicros,
            ruleCount = rules.size
        )
        val grinFile = GrinFile(header, pixels, rules)

        val id = UUID.randomUUID().toString()
        val previewPath = File(previewDir, "$id.png").absolutePath
        val thumbnailPath = File(thumbnailDir, "$id.png").absolutePath
        val grinPath = File(grinDir, "$id.grin").absolutePath
        val grimPath = File(grimDir, "$id.grim").absolutePath

        // Save preview and thumbnail to disk for the gallery grid.
        writeBitmap(frame.bitmap, previewPath)
        writeBitmap(Bitmap.createScaledBitmap(frame.bitmap, 192, 256, true), thumbnailPath)

        // Persist both GRIN and GRIM payloads.
        grinFile.save(grinPath)
        grinFile.save(grimPath)

        val timestamp = System.currentTimeMillis()
        val metadata = GrinAssetMetadata(
            id = id,
            createdAt = timestamp,
            lastEditedAt = timestamp,
            width = frame.gridCols,
            height = frame.gridRows,
            gridCols = frame.gridCols,
            gridRows = frame.gridRows,
            paletteBins = frame.paletteSize,
            tickMicros = tickMicros,
            ruleCount = rules.size,
            channelMap = channelMap,
            channelOrder = channelOrder,
            paletteHistogram = frame.paletteHistogram.toList(),
            previewPath = previewPath,
            thumbnailPath = thumbnailPath,
            grinPath = grinPath,
            grimPath = grimPath,
            channelSettings = channelSettings
        )

        // Log capture metadata for gallery indexing and debugging.
        Log.i(
            "GrinCapture",
            "Captured ${metadata.width}x${metadata.height} bins=${metadata.paletteBins} at=${metadata.createdAt}"
        )

        saveMetadata(metadata)
        return metadata
    }

    fun saveMetadata(metadata: GrinAssetMetadata) {
        // Write the metadata JSON to disk for persistence across sessions.
        val file = File(metadataDir, "${metadata.id}.json")
        file.writeText(metadata.toJson().toString())
    }

    fun loadAllMetadata(): List<GrinAssetMetadata> {
        // Load metadata entries sorted by most recent capture first.
        if (!metadataDir.exists()) {
            return emptyList()
        }
        return metadataDir.listFiles { file -> file.extension == "json" }
            ?.mapNotNull { file ->
                runCatching {
                    GrinAssetMetadata.fromJson(org.json.JSONObject(file.readText()))
                }.getOrNull()
            }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    fun loadMetadata(id: String): GrinAssetMetadata? {
        // Read a single metadata record by ID.
        val file = File(metadataDir, "$id.json")
        if (!file.exists()) {
            return null
        }
        return GrinAssetMetadata.fromJson(org.json.JSONObject(file.readText()))
    }

    fun loadPreviewBitmap(metadata: GrinAssetMetadata): Bitmap? {
        // Load a preview bitmap from disk for gallery or editor use.
        val file = File(metadata.previewPath)
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    fun loadThumbnailBitmap(metadata: GrinAssetMetadata): Bitmap? {
        // Load cached thumbnails for the gallery grid.
        val file = File(metadata.thumbnailPath)
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    fun loadGrinFile(metadata: GrinAssetMetadata): GrinFile? {
        // Load the GRIN payload for editor previews and exports.
        val file = File(metadata.grinPath)
        return if (file.exists()) GrinFile.load(file.absolutePath) else null
    }

    fun exportPng(bitmap: Bitmap, fileName: String): File {
        // Write a PNG snapshot to the export directory.
        val output = File(exportDir, fileName)
        writeBitmap(bitmap, output.absolutePath)
        return output
    }

    fun exportGif(fileName: String, frames: List<Bitmap>, delayMs: Int): File {
        // Encode a looping GIF from preview frames.
        val output = File(exportDir, fileName)
        GifEncoder().encode(frames, delayMs, output)
        return output
    }

    fun exportPngToUri(bitmap: Bitmap, uri: Uri): Boolean {
        return context.contentResolver.openOutputStream(uri)?.use { output ->
            writeBitmap(bitmap, output)
            true
        } ?: false
    }

    fun exportGifToUri(frames: List<Bitmap>, delayMs: Int, uri: Uri): Boolean {
        return context.contentResolver.openOutputStream(uri)?.use { output ->
            GifEncoder().encode(frames, delayMs, output)
            true
        } ?: false
    }

    fun exportGrinToUri(grinFile: GrinFile, uri: Uri): Boolean {
        return context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(grinFile.toBytes())
            true
        } ?: false
    }

    fun exportGrinAndGrim(
        metadata: GrinAssetMetadata,
        grinFile: GrinFile,
        settings: List<ChannelSetting>,
        baseName: String
    ): Pair<File, File> {
        // Persist updated GRIN and GRIM files to the export directory.
        val grinOutput = File(exportDir, "$baseName.grin")
        val grimOutput = File(exportDir, "$baseName.grim")
        grinFile.save(grinOutput.absolutePath)
        grinFile.save(grimOutput.absolutePath)
        metadata.tickMicros = grinFile.header.tickMicros
        metadata.ruleCount = grinFile.header.ruleCount
        metadata.lastEditedAt = System.currentTimeMillis()
        metadata.channelSettings = settings.map { it.copy() }
        saveMetadata(metadata)
        return Pair(grinOutput, grimOutput)
    }

    fun updateAssetMetadata(metadata: GrinAssetMetadata, settings: List<ChannelSetting>, preview: Bitmap?) {
        // Persist channel overrides and updated previews back to disk.
        metadata.channelSettings = settings
        metadata.lastEditedAt = System.currentTimeMillis()
        if (preview != null) {
            writeBitmap(preview, metadata.previewPath)
            writeBitmap(Bitmap.createScaledBitmap(preview, 192, 256, true), metadata.thumbnailPath)
        }
        saveMetadata(metadata)
    }

    fun buildUpdatedGrinFile(metadata: GrinAssetMetadata, baseFile: GrinFile, settings: List<ChannelSetting>): GrinFile {
        // Apply channel overrides to the rule block while keeping pixel data intact.
        val activeChannels = metadata.channelOrder.distinct()
        val rules = buildRules(settings, activeChannels)
        val header = buildHeader(
            width = metadata.width,
            height = metadata.height,
            tickMicros = metadata.tickMicros,
            ruleCount = rules.size
        )
        return GrinFile(header, baseFile.pixels, rules)
    }

    private fun writeBitmap(bitmap: Bitmap, path: String) {
        // Write an ARGB_8888 bitmap as a PNG file.
        File(path).outputStream().use { output ->
            writeBitmap(bitmap, output)
        }
    }

    private fun writeBitmap(bitmap: Bitmap, output: OutputStream) {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    }

    private fun assignBinsToChannels(histogram: IntArray): ChannelAssignment {
        // Keep channel IDs aligned with palette indices while ordering channels by frequency.
        val bins = histogram.indices.map { index ->
            PaletteBin(
                index = index,
                count = histogram[index]
            )
        }
        val sortedBins = bins.sortedWith(
            compareByDescending<PaletteBin> { it.count }
                .thenBy { it.index }
        )

        val channelMap = IntArray(histogram.size)
        val channelOrder = mutableListOf<Int>()
        sortedBins.forEach { bin ->
            val channelId = bin.index.coerceIn(0, 15)
            channelMap[bin.index] = channelId
            channelOrder.add(channelId)
        }
        val activeChannels = channelOrder.distinct()
        return ChannelAssignment(channelMap.toList(), channelOrder, activeChannels)
    }

    private fun buildDefaultChannelSettings(channelOrder: List<Int>): List<ChannelSetting> {
        // Initialize channel settings for every channel with defaults.
        val settings = mutableListOf<ChannelSetting>()
        for (channelId in 0..15) {
            settings.add(
                ChannelSetting(
                    channelId = channelId,
                    frequency = 50,
                    intonation = 50,
                    transparency = 100
                )
            )
        }
        // Keep the most frequent channel slightly emphasized.
        val defaultChannel = channelOrder.firstOrNull() ?: 0
        settings.firstOrNull { it.channelId == defaultChannel }?.frequency = 60
        return settings
    }

    private fun buildGrinPixels(frame: PosterizedFrame, channelMap: List<Int>): List<GrinPixel> {
        // Convert the posterized preview into GRIN pixels with group IDs.
        val pixels = mutableListOf<GrinPixel>()
        for (index in frame.paletteIndices.indices) {
            val paletteIndex = frame.paletteIndices[index]
            val color = frame.paletteColors[paletteIndex]
            val channelId = channelMap[paletteIndex]
            val pixel = GrinPixel(
                r = Color.red(color),
                g = Color.green(color),
                b = Color.blue(color),
                a = Color.alpha(color),
                c = 0
            )
            pixel.setGroupId(channelId)
            pixels.add(pixel)
        }
        return pixels
    }

    private fun buildRules(settings: List<ChannelSetting>, activeChannels: List<Int>): List<GrinRule> {
        // Build rule entries for each active channel using the current settings.
        val rules = mutableListOf<GrinRule>()
        activeChannels.forEach { channelId ->
            val setting = settings.firstOrNull { it.channelId == channelId }
            if (setting != null) {
                val timing = mapFrequencyToTiming(setting.frequency)
                val opcode = mapIntonationToOpcode(setting.intonation)
                val groupMask = 1 shl (channelId and 0x0F)
                rules.add(GrinRule(groupMask, opcode, timing))
            }
        }
        return rules
    }

    private fun mapFrequencyToTiming(frequency: Int): Int {
        // Map 0-100 slider values to an 8-bit timing value.
        val clamped = frequency.coerceIn(0, 100)
        return (clamped / 100.0 * 255).roundToInt().coerceIn(0, 255)
    }

    private fun mapIntonationToOpcode(intonation: Int): Int {
        // Map intonation to a GRIN opcode bucket for header persistence.
        return when {
            intonation >= 70 -> BaseOpcodes.ROTATE_HUE
            intonation >= 55 -> BaseOpcodes.SHIFT_R
            intonation <= 30 -> BaseOpcodes.SHIFT_B
            else -> BaseOpcodes.NOP
        }
    }

    private fun buildHeader(width: Int, height: Int, tickMicros: Long, ruleCount: Int): GrinHeader {
        // Compose a GRIN header with correct lengths for saving.
        val pixelDataLength = width.toLong() * height.toLong() * GrinFormat.PIXEL_SIZE_BYTES.toLong()
        val fileLength = GrinFormat.HEADER_SIZE_BYTES.toLong() + pixelDataLength
        return GrinHeader(
            magic = GrinFormat.MAGIC_BYTES,
            versionMajor = GrinFormat.VERSION_MAJOR,
            versionMinor = GrinFormat.VERSION_MINOR,
            headerSize = GrinFormat.HEADER_SIZE_BYTES,
            width = width.toLong(),
            height = height.toLong(),
            tickMicros = tickMicros,
            ruleCount = ruleCount,
            opcodeSetId = 0,
            flags = 0,
            pixelDataLength = pixelDataLength,
            fileLength = fileLength,
            pixelDataOffset = GrinFormat.HEADER_SIZE_BYTES.toLong(),
            reservedA = 0,
            reservedB = 0,
            rulesBlock = ByteArray(GrinFormat.RULES_BLOCK_SIZE)
        )
    }

    private data class ChannelAssignment(
        val channelMap: List<Int>,
        val channelOrder: List<Int>,
        val activeChannels: List<Int>
    )

    private data class PaletteBin(
        val index: Int,
        val count: Int
    )
}
