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
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.grin.demo.databinding.ActivityDiffViewerBinding
import io.grin.lib.BaseOpcodes
import io.grin.lib.GrinFile
import io.grin.lib.GrinFormat
import io.grin.lib.GrinPixel
import io.grin.lib.GrinRule

// Shows a diff preview by toggling between two GRIN images.
class DiffViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDiffViewerBinding
    private lateinit var store: GrinAssetStore
    private var diffFile: GrinFile? = null
    private var diffOutputPath: String? = null
    private val toggleHandler = Handler(Looper.getMainLooper())
    private var showFirst = true
    private var firstBitmap: Bitmap? = null
    private var secondBitmap: Bitmap? = null
    private val toggleRunnable = object : Runnable {
        override fun run() {
            val next = if (showFirst) secondBitmap else firstBitmap
            if (next != null) {
                binding.diffPreview.setImageBitmap(next)
                showFirst = !showFirst
            }
            toggleHandler.postDelayed(this, TOGGLE_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiffViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        store = GrinAssetStore(this)
        binding.backButton.setOnClickListener { finish() }

        val firstId = intent.getStringExtra(EXTRA_ASSET_ID_A)
        val secondId = intent.getStringExtra(EXTRA_ASSET_ID_B)
        if (firstId == null || secondId == null) {
            Toast.makeText(this, getString(R.string.diff_missing_asset), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val firstMeta = store.loadMetadata(firstId)
        val secondMeta = store.loadMetadata(secondId)
        val firstFile = firstMeta?.let { store.loadGrinFile(it) }
        val secondFile = secondMeta?.let { store.loadGrinFile(it) }
        if (firstMeta == null || secondMeta == null || firstFile == null || secondFile == null) {
            Toast.makeText(this, getString(R.string.diff_missing_asset), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (firstMeta.width != secondMeta.width || firstMeta.height != secondMeta.height) {
            Toast.makeText(this, getString(R.string.diff_size_mismatch), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val palette = buildPalette(secondFile.pixels)
        diffFile = buildDiffGrinFile(firstFile, secondFile, palette)
        diffFile?.let { file ->
            val preview = renderBitmap(file)
            store.createAssetFromGrinFile(file, preview, palette.size)
        }
        diffOutputPath = saveDiffFile(firstId, secondId, diffFile)
        diffOutputPath?.let { path ->
            Toast.makeText(this, getString(R.string.diff_saved_format, path), Toast.LENGTH_LONG).show()
        }
        firstBitmap = renderBitmap(firstFile)
        secondBitmap = renderBitmap(secondFile)
        binding.diffPreview.setImageBitmap(firstBitmap ?: secondBitmap)
    }

    override fun onResume() {
        super.onResume()
        startToggle()
    }

    override fun onPause() {
        toggleHandler.removeCallbacks(toggleRunnable)
        super.onPause()
    }

    private fun startToggle() {
        toggleHandler.removeCallbacks(toggleRunnable)
        showFirst = true
        binding.diffPreview.setImageBitmap(firstBitmap ?: secondBitmap)
        if (firstBitmap != null && secondBitmap != null) {
            toggleHandler.postDelayed(toggleRunnable, TOGGLE_INTERVAL_MS)
        }
    }

    private fun buildDiffGrinFile(
        baseFile: GrinFile,
        compareFile: GrinFile,
        palette: IntArray
    ): GrinFile {
        val colorToIndex = mutableMapOf<Int, Int>()
        palette.forEachIndexed { index, color -> colorToIndex[color] = index }

        val diffPixels = mutableListOf<GrinPixel>()
        val usedGroups = BooleanArray(MAX_PALETTE_SIZE)
        for (index in baseFile.pixels.indices) {
            val base = baseFile.pixels[index]
            val compare = compareFile.pixels[index]
            val compareColor = rgbKey(compare)
            val groupId = colorToIndex[compareColor] ?: findNearestPaletteIndex(compareColor, palette)
            val changed = !colorsMatch(base, compare)
            val alpha = if (changed) 0 else compare.a
            val pixel = GrinPixel(compare.r, compare.g, compare.b, alpha, 0)
            pixel.setGroupId(groupId)
            pixel.setLocked(!changed)
            if (changed) {
                usedGroups[groupId] = true
            }
            diffPixels.add(pixel)
        }

        val rules = buildBlinkRules(usedGroups)
        val header = baseFile.header.copy(
            ruleCount = rules.size,
            tickMicros = TOGGLE_INTERVAL_MICROS,
            rulesBlock = ByteArray(GrinFormat.RULES_BLOCK_SIZE)
        )
        return GrinFile(header, diffPixels, rules)
    }

    private fun saveDiffFile(firstId: String, secondId: String, diffFile: GrinFile?): String? {
        if (diffFile == null) {
            return null
        }
        val fileName = "diff_${firstId}_$secondId.grin"
        val storageUri = StoragePreferences.getStorageUri(this)
        if (storageUri != null) {
            val folder = androidx.documentfile.provider.DocumentFile.fromTreeUri(this, storageUri)
            val target = folder?.createFile("application/octet-stream", fileName)
            if (target != null && store.exportGrinToUri(diffFile, target.uri)) {
                return target.name ?: fileName
            }
        }
        val outputDir = getDir(DIFF_OUTPUT_DIR, MODE_PRIVATE)
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val outputPath = java.io.File(outputDir, fileName).absolutePath
        diffFile.save(outputPath)
        return outputPath
    }

    private fun buildBlinkRules(usedGroups: BooleanArray): List<GrinRule> {
        val rules = mutableListOf<GrinRule>()
        for (groupId in usedGroups.indices) {
            if (usedGroups[groupId]) {
                val groupMask = 1 shl groupId
                rules.add(GrinRule(groupMask, BaseOpcodes.SHIFT_A, BLINK_TIMING))
            }
        }
        return rules
    }

    private fun buildPalette(pixels: List<GrinPixel>): IntArray {
        val counts = linkedMapOf<Int, Int>()
        for (pixel in pixels) {
            val key = rgbKey(pixel)
            counts[key] = (counts[key] ?: 0) + 1
        }
        val sorted = counts.entries.sortedWith(
            compareByDescending<Map.Entry<Int, Int>> { it.value }
                .thenBy { it.key }
        )
        return sorted.take(MAX_PALETTE_SIZE).map { it.key }.toIntArray()
    }

    private fun rgbKey(pixel: GrinPixel): Int {
        return (pixel.r shl 16) or (pixel.g shl 8) or pixel.b
    }

    private fun colorsMatch(first: GrinPixel, second: GrinPixel): Boolean {
        return first.r == second.r && first.g == second.g && first.b == second.b && first.a == second.a
    }

    private fun findNearestPaletteIndex(color: Int, palette: IntArray): Int {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        var bestIndex = 0
        var bestDistance = Int.MAX_VALUE
        for (index in palette.indices) {
            val pr = (palette[index] shr 16) and 0xFF
            val pg = (palette[index] shr 8) and 0xFF
            val pb = palette[index] and 0xFF
            val dr = r - pr
            val dg = g - pg
            val db = b - pb
            val distance = dr * dr + dg * dg + db * db
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
            }
        }
        return bestIndex
    }

    private fun renderBitmap(file: GrinFile): android.graphics.Bitmap {
        val width = safeInt(file.header.width, "Width")
        val height = safeInt(file.header.height, "Height")
        val pixels = IntArray(width * height)
        for (index in file.pixels.indices) {
            val pixel = file.pixels[index]
            pixels[index] =
                (pixel.a shl 24) or (pixel.r shl 16) or (pixel.g shl 8) or pixel.b
        }
        return android.graphics.Bitmap.createBitmap(pixels, width, height, android.graphics.Bitmap.Config.ARGB_8888)
    }

    private fun safeInt(value: Long, label: String): Int {
        require(value <= Int.MAX_VALUE.toLong()) { "$label exceeds Int range" }
        require(value >= 0L) { "$label must be non-negative" }
        return value.toInt()
    }

    private companion object {
        const val MAX_PALETTE_SIZE = 16
        const val TOGGLE_INTERVAL_MICROS = 700_000L
        const val TOGGLE_INTERVAL_MS = 700L
        const val BLINK_TIMING = 0b10_00_0001
        const val DIFF_OUTPUT_DIR = "diff_outputs"
    }
}
