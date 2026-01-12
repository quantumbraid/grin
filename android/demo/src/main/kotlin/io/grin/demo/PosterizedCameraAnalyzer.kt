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
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Holds a posterized frame and grid metadata for UI rendering.
data class PosterizedFrame(
    val bitmap: Bitmap,
    val gridCols: Int,
    val gridRows: Int,
    val paletteIndices: IntArray,
    val paletteLabels: List<String>,
    val paletteSize: Int,
    val paletteHistogram: IntArray,
    val paletteColors: IntArray
)

// Encapsulates performance targets and fallback behavior for posterization.
data class PosterizationPerformanceConfig(
    val targetFps: Int,
    val fallbackFps: Int,
    val slowdownThresholdMs: Long,
    val fallbackGridScale: Float,
    val fallbackPaletteSize: Int
)

// Analyzer that converts camera frames into posterized, grid-aligned bitmaps.
class PosterizedCameraAnalyzer(
    private val baseGridCols: Int,
    private val baseGridRows: Int,
    private val palette: PosterizedPalette,
    private val performanceConfig: PosterizationPerformanceConfig,
    private val onFrame: (PosterizedFrame) -> Unit
) : ImageAnalysis.Analyzer {
    private var lastFrameTimestampNs: Long = 0L
    private var useFallbackSettings: Boolean = false

    override fun analyze(image: ImageProxy) {
        val timestampNs = image.imageInfo.timestamp
        val activeTargetFps = if (useFallbackSettings) {
            performanceConfig.fallbackFps
        } else {
            performanceConfig.targetFps
        }
        val frameIntervalNs = 1_000_000_000L / activeTargetFps
        if (timestampNs - lastFrameTimestampNs < frameIntervalNs) {
            // Throttle analysis to the target FPS to avoid overloading the CPU.
            image.close()
            return
        }
        lastFrameTimestampNs = timestampNs

        val startTimeMs = System.currentTimeMillis()
        val effectiveGridCols = if (useFallbackSettings) {
            (baseGridCols * performanceConfig.fallbackGridScale).roundToInt().coerceAtLeast(8)
        } else {
            baseGridCols
        }
        val effectiveGridRows = if (useFallbackSettings) {
            (baseGridRows * performanceConfig.fallbackGridScale).roundToInt().coerceAtLeast(8)
        } else {
            baseGridRows
        }
        val effectivePalette = if (useFallbackSettings) {
            palette.clampSize(performanceConfig.fallbackPaletteSize)
        } else {
            palette
        }

        val frame = buildPosterizedFrame(image, effectiveGridCols, effectiveGridRows, effectivePalette)
        val elapsedMs = System.currentTimeMillis() - startTimeMs
        useFallbackSettings = elapsedMs > performanceConfig.slowdownThresholdMs
        onFrame(frame)
        image.close()
    }

    private fun buildPosterizedFrame(
        image: ImageProxy,
        gridCols: Int,
        gridRows: Int,
        palette: PosterizedPalette
    ): PosterizedFrame {
        val crop = computeCenterCrop(image.width, image.height, gridCols, gridRows)
        val bitmap = Bitmap.createBitmap(gridCols, gridRows, Bitmap.Config.ARGB_8888)
        val paletteIndices = IntArray(gridCols * gridRows)
        // Track per-bin counts for channel assignment and gallery metadata.
        val histogram = IntArray(palette.colors.size)

        for (row in 0 until gridRows) {
            val sourceY = crop.top + ((row + 0.5f) * crop.height / gridRows).roundToInt()
            for (col in 0 until gridCols) {
                val sourceX = crop.left + ((col + 0.5f) * crop.width / gridCols).roundToInt()
                val rgb = readRgbFromYuv(image, sourceX, sourceY)
                val paletteIndex = findNearestPaletteIndex(rgb, palette.colors)
                bitmap.setPixel(col, row, palette.colors[paletteIndex])
                paletteIndices[row * gridCols + col] = paletteIndex
                histogram[paletteIndex] += 1
            }
        }

        return PosterizedFrame(
            bitmap = bitmap,
            gridCols = gridCols,
            gridRows = gridRows,
            paletteIndices = paletteIndices,
            paletteLabels = palette.labels,
            paletteSize = palette.colors.size,
            paletteHistogram = histogram,
            paletteColors = palette.colors.copyOf()
        )
    }

    private fun computeCenterCrop(
        imageWidth: Int,
        imageHeight: Int,
        gridCols: Int,
        gridRows: Int
    ): CropRect {
        val targetAspect = gridCols.toFloat() / gridRows.toFloat()
        val sourceAspect = imageWidth.toFloat() / imageHeight.toFloat()
        return if (sourceAspect > targetAspect) {
            val cropWidth = (imageHeight * targetAspect).roundToInt()
            val offsetX = ((imageWidth - cropWidth) / 2f).roundToInt()
            CropRect(offsetX, 0, cropWidth, imageHeight)
        } else {
            val cropHeight = (imageWidth / targetAspect).roundToInt()
            val offsetY = ((imageHeight - cropHeight) / 2f).roundToInt()
            CropRect(0, offsetY, imageWidth, cropHeight)
        }
    }

    private fun readRgbFromYuv(image: ImageProxy, x: Int, y: Int): Int {
        // Convert a single YUV_420_888 pixel to RGB using the standard formula.
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val clampedX = x.coerceIn(0, image.width - 1)
        val clampedY = y.coerceIn(0, image.height - 1)
        val yIndex = yPlane.rowStride * clampedY + yPlane.pixelStride * clampedX
        val uvX = clampedX / 2
        val uvY = clampedY / 2
        val uIndex = uPlane.rowStride * uvY + uPlane.pixelStride * uvX
        val vIndex = vPlane.rowStride * uvY + vPlane.pixelStride * uvX

        val yValue = yPlane.buffer.get(yIndex).toInt() and 0xFF
        val uValue = (uPlane.buffer.get(uIndex).toInt() and 0xFF) - 128
        val vValue = (vPlane.buffer.get(vIndex).toInt() and 0xFF) - 128

        val r = clampColorChannel(yValue + (1.402f * vValue).roundToInt())
        val g = clampColorChannel(yValue - (0.344f * uValue).roundToInt() - (0.714f * vValue).roundToInt())
        val b = clampColorChannel(yValue + (1.772f * uValue).roundToInt())
        return Color.rgb(r, g, b)
    }

    private fun clampColorChannel(value: Int): Int {
        return value.coerceIn(0, 255)
    }

    private fun findNearestPaletteIndex(rgb: Int, palette: IntArray): Int {
        // Find the palette entry with the smallest Euclidean distance in RGB space.
        val red = Color.red(rgb)
        val green = Color.green(rgb)
        val blue = Color.blue(rgb)
        var bestIndex = 0
        var bestDistance = Double.MAX_VALUE
        for (index in palette.indices) {
            val paletteColor = palette[index]
            val distance = colorDistance(
                red,
                green,
                blue,
                Color.red(paletteColor),
                Color.green(paletteColor),
                Color.blue(paletteColor)
            )
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
            }
        }
        return bestIndex
    }

    private fun colorDistance(
        red: Int,
        green: Int,
        blue: Int,
        paletteRed: Int,
        paletteGreen: Int,
        paletteBlue: Int
    ): Double {
        val redDelta = (red - paletteRed).toDouble()
        val greenDelta = (green - paletteGreen).toDouble()
        val blueDelta = (blue - paletteBlue).toDouble()
        return sqrt(redDelta * redDelta + greenDelta * greenDelta + blueDelta * blueDelta)
    }

    // Stores crop origin and dimensions for the grid-aligned sampling area.
    private data class CropRect(val left: Int, val top: Int, val width: Int, val height: Int)
}
