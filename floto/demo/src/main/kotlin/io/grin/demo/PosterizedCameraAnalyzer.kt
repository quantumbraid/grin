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
import java.util.Arrays
import kotlin.math.roundToInt

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

// Holds user-tunable color adjustments for the camera pipeline.
class CameraColorAdjustments(
    contrast: Float,
    saturation: Float,
    brightness: Float
) {
    // Volatile fields keep analyzer reads in sync with UI slider updates.
    @Volatile var contrast: Float = contrast
    @Volatile var saturation: Float = saturation
    @Volatile var brightness: Float = brightness
}

enum class PaletteMode {
    CLASSIC,
    HSV_PRESET
}

// Analyzer that converts camera frames into posterized, grid-aligned bitmaps.
class PosterizedCameraAnalyzer(
    private val baseGridCols: Int,
    private val baseGridRows: Int,
    private val palette: PosterizedPalette,
    private val performanceConfig: PosterizationPerformanceConfig,
    private val paletteMode: PaletteMode,
    private val isRearFacing: Boolean,
    private val colorAdjustments: CameraColorAdjustments,
    private val onFrame: (PosterizedFrame) -> Unit
) : ImageAnalysis.Analyzer {
    private val whiteColor = Color.rgb(0xFF, 0xFF, 0xFF)
    private val adaptivePaletteColors = palette.colors.copyOf()
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
        val paletteSize = if (useFallbackSettings) {
            performanceConfig.fallbackPaletteSize.coerceIn(1, adaptivePaletteColors.size)
        } else {
            adaptivePaletteColors.size
        }

        val frame = buildPosterizedFrame(
            image,
            effectiveGridCols,
            effectiveGridRows,
            paletteSize,
            image.imageInfo.rotationDegrees
        )
        val elapsedMs = System.currentTimeMillis() - startTimeMs
        useFallbackSettings = elapsedMs > performanceConfig.slowdownThresholdMs
        onFrame(frame)
        image.close()
    }

    private fun buildPosterizedFrame(
        image: ImageProxy,
        gridCols: Int,
        gridRows: Int,
        paletteSize: Int,
        rotationDegrees: Int
    ): PosterizedFrame {
        val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
        val adjustedRotation = (normalizedRotation + 180) % 360
        val flipX = normalizedRotation == 90 || normalizedRotation == 270
        val flipY = true
        val flipOutputVertical = normalizedRotation == 90 || normalizedRotation == 270
        val rotatedWidth = if (adjustedRotation == 90 || adjustedRotation == 270) {
            image.height
        } else {
            image.width
        }
        val rotatedHeight = if (adjustedRotation == 90 || adjustedRotation == 270) {
            image.width
        } else {
            image.height
        }
        val crop = computeCenterCrop(rotatedWidth, rotatedHeight, gridCols, gridRows)
        val bitmap = Bitmap.createBitmap(gridCols, gridRows, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(whiteColor)
        val pixelCount = gridCols * gridRows
        val paletteIndices = IntArray(pixelCount)
        val paletteColors = adaptivePaletteColors.copyOf(paletteSize)
        // Track per-bin counts for channel assignment and gallery metadata.
        val histogram = IntArray(paletteColors.size)
        val sumR = LongArray(paletteColors.size)
        val sumG = LongArray(paletteColors.size)
        val sumB = LongArray(paletteColors.size)

        if (paletteMode == PaletteMode.HSV_PRESET) {
            for (row in 0 until gridRows) {
                for (col in 0 until gridCols) {
                    val representative = sampleMedianColor(
                        image,
                        crop,
                        gridCols,
                        gridRows,
                        row,
                        col,
                        adjustedRotation,
                        flipX,
                        flipY
                    )
                    val hsv = FloatArray(3)
                    Color.RGBToHSV(
                        Color.red(representative),
                        Color.green(representative),
                        Color.blue(representative),
                        hsv
                    )
                    val adjustedHsv = adjustHsvAdaptive(hsv, colorAdjustments)
                    val adjustedRgb = Color.HSVToColor(adjustedHsv)
                    val outputRow = if (flipOutputVertical) gridRows - 1 - row else row
                    val outputCol = if (isRearFacing) gridCols - 1 - col else col
                    val outputIndex = outputRow * gridCols + outputCol
                    val paletteIndex = findNearestPaletteIndex(adjustedRgb, paletteColors)
                    paletteIndices[outputIndex] = paletteIndex
                    histogram[paletteIndex] += 1
                    sumR[paletteIndex] += Color.red(adjustedRgb).toLong()
                    sumG[paletteIndex] += Color.green(adjustedRgb).toLong()
                    sumB[paletteIndex] += Color.blue(adjustedRgb).toLong()
                    val paletteColor = paletteColors[paletteIndex]
                    if (paletteColor != whiteColor) {
                        bitmap.setPixel(outputCol, outputRow, paletteColor)
                    }
                }
            }
        } else {
            val rawColors = IntArray(pixelCount)
            val lumaHistogram = IntArray(256)

            for (row in 0 until gridRows) {
                val rotatedY = crop.top + ((row + 0.5f) * crop.height / gridRows).roundToInt()
                for (col in 0 until gridCols) {
                    val rotatedX = crop.left + ((col + 0.5f) * crop.width / gridCols).roundToInt()
                    val (sourceX, sourceY) = mapRotatedToSource(
                        rotatedX,
                        rotatedY,
                        adjustedRotation,
                        image.width,
                        image.height,
                        flipX,
                        flipY
                    )
                    val rgb = readRgbFromYuv(image, sourceX, sourceY)
                    val index = row * gridCols + col
                    rawColors[index] = rgb
                    val luma = computeLuma(rgb)
                    lumaHistogram[luma] += 1
                }
            }

            val blackPoint = percentileFromHistogram(lumaHistogram, pixelCount, 0.02)
            val whitePoint = percentileFromHistogram(lumaHistogram, pixelCount, 0.98)

            for (row in 0 until gridRows) {
                for (col in 0 until gridCols) {
                    val sampleIndex = row * gridCols + col
                    val outputRow = if (flipOutputVertical) gridRows - 1 - row else row
                    val outputCol = if (isRearFacing) gridCols - 1 - col else col
                    val outputIndex = outputRow * gridCols + outputCol
                    val adjusted = adjustColor(rawColors[sampleIndex], blackPoint, whitePoint, colorAdjustments)
                    val paletteIndex = findNearestPaletteIndex(adjusted, paletteColors)
                    paletteIndices[outputIndex] = paletteIndex
                    histogram[paletteIndex] += 1
                    sumR[paletteIndex] += Color.red(adjusted).toLong()
                    sumG[paletteIndex] += Color.green(adjusted).toLong()
                    sumB[paletteIndex] += Color.blue(adjusted).toLong()
                    val paletteColor = paletteColors[paletteIndex]
                    if (paletteColor != whiteColor) {
                        bitmap.setPixel(outputCol, outputRow, paletteColor)
                    }
                }
            }
        }

        updateAdaptivePaletteColors(paletteColors, histogram, sumR, sumG, sumB)

        return PosterizedFrame(
            bitmap = bitmap,
            gridCols = gridCols,
            gridRows = gridRows,
            paletteIndices = paletteIndices,
            paletteLabels = palette.labels,
            paletteSize = paletteColors.size,
            paletteHistogram = histogram,
            paletteColors = paletteColors.copyOf()
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

    private fun sampleMedianColor(
        image: ImageProxy,
        crop: CropRect,
        gridCols: Int,
        gridRows: Int,
        row: Int,
        col: Int,
        rotation: Int,
        flipX: Boolean,
        flipY: Boolean
    ): Int {
        val samplesPerSide = 3
        val sampleCount = samplesPerSide * samplesPerSide
        val reds = IntArray(sampleCount)
        val greens = IntArray(sampleCount)
        val blues = IntArray(sampleCount)
        var sampleIndex = 0
        for (sampleRow in 0 until samplesPerSide) {
            val rotatedY = crop.top +
                ((row + (sampleRow + 0.5f) / samplesPerSide) * crop.height / gridRows).roundToInt()
            for (sampleCol in 0 until samplesPerSide) {
                val rotatedX = crop.left +
                    ((col + (sampleCol + 0.5f) / samplesPerSide) * crop.width / gridCols).roundToInt()
                val (sourceX, sourceY) = mapRotatedToSource(
                    rotatedX,
                    rotatedY,
                    rotation,
                    image.width,
                    image.height,
                    flipX,
                    flipY
                )
                val rgb = readRgbFromYuv(image, sourceX, sourceY)
                reds[sampleIndex] = Color.red(rgb)
                greens[sampleIndex] = Color.green(rgb)
                blues[sampleIndex] = Color.blue(rgb)
                sampleIndex += 1
            }
        }
        Arrays.sort(reds)
        Arrays.sort(greens)
        Arrays.sort(blues)
        val mid = sampleCount / 2
        return Color.rgb(reds[mid], greens[mid], blues[mid])
    }

    private fun mapRotatedToSource(
        rotatedX: Int,
        rotatedY: Int,
        rotation: Int,
        imageWidth: Int,
        imageHeight: Int,
        flipX: Boolean,
        flipY: Boolean
    ): Pair<Int, Int> {
        val (sourceX, sourceY) = when (rotation) {
            90 -> (imageWidth - 1 - rotatedY) to rotatedX
            180 -> (imageWidth - 1 - rotatedX) to (imageHeight - 1 - rotatedY)
            270 -> rotatedY to (imageHeight - 1 - rotatedX)
            else -> rotatedX to rotatedY
        }
        val flippedX = if (flipX) imageWidth - 1 - sourceX else sourceX
        val flippedY = if (flipY) imageHeight - 1 - sourceY else sourceY
        return flippedX to flippedY
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

    private fun computeLuma(rgb: Int): Int {
        val red = Color.red(rgb)
        val green = Color.green(rgb)
        val blue = Color.blue(rgb)
        val luma = (0.2126f * red + 0.7152f * green + 0.0722f * blue).roundToInt()
        return luma.coerceIn(0, 255)
    }

    private fun percentileFromHistogram(histogram: IntArray, total: Int, percentile: Double): Int {
        if (total <= 0) {
            return 0
        }
        val target = (total * percentile).roundToInt().coerceIn(0, total)
        var cumulative = 0
        for (value in histogram.indices) {
            cumulative += histogram[value]
            if (cumulative >= target) {
                return value
            }
        }
        return histogram.size - 1
    }

    private fun adjustColor(
        rgb: Int,
        blackPoint: Int,
        whitePoint: Int,
        adjustments: CameraColorAdjustments
    ): Int {
        val red = Color.red(rgb)
        val green = Color.green(rgb)
        val blue = Color.blue(rgb)
        val range = (whitePoint - blackPoint).coerceAtLeast(1)
        val normalizedRed = ((red - blackPoint) / range.toFloat()).coerceIn(0f, 1f)
        val normalizedGreen = ((green - blackPoint) / range.toFloat()).coerceIn(0f, 1f)
        val normalizedBlue = ((blue - blackPoint) / range.toFloat()).coerceIn(0f, 1f)

        // Apply user-tunable contrast before HSV adjustments.
        val contrast = 1.18f * adjustments.contrast
        val contrastedRed = ((normalizedRed - 0.5f) * contrast + 0.5f).coerceIn(0f, 1f)
        val contrastedGreen = ((normalizedGreen - 0.5f) * contrast + 0.5f).coerceIn(0f, 1f)
        val contrastedBlue = ((normalizedBlue - 0.5f) * contrast + 0.5f).coerceIn(0f, 1f)

        val hsv = FloatArray(3)
        Color.RGBToHSV(
            (contrastedRed * 255f).roundToInt(),
            (contrastedGreen * 255f).roundToInt(),
            (contrastedBlue * 255f).roundToInt(),
            hsv
        )
        // Apply user-tunable saturation/brightness before palette mapping.
        hsv[1] = (hsv[1] * 1.35f * adjustments.saturation).coerceIn(0f, 1f)
        hsv[2] = (hsv[2] * 1.08f * adjustments.brightness).coerceIn(0f, 1f)
        return Color.HSVToColor(hsv)
    }

    private fun adjustHsvAdaptive(hsv: FloatArray, adjustments: CameraColorAdjustments): FloatArray {
        val value = hsv[2]
        val saturation = hsv[1]
        val saturationBoost = when {
            value < 0.35f -> 1.25f
            value < 0.6f -> 1.12f
            else -> 1.0f
        }
        val valueBoost = when {
            value < 0.35f -> 1.18f
            value < 0.6f -> 1.06f
            else -> 1.0f
        }
        val adjusted = FloatArray(3)
        adjusted[0] = hsv[0]
        // Apply user-tunable adjustments while preserving the adaptive boost.
        adjusted[1] = (saturation * saturationBoost * adjustments.saturation).coerceIn(0f, 1f)
        val boostedValue = (value * valueBoost * adjustments.brightness).coerceIn(0f, 1f)
        adjusted[2] = ((boostedValue - 0.5f) * adjustments.contrast + 0.5f).coerceIn(0f, 1f)
        return adjusted
    }

    private fun updateAdaptivePaletteColors(
        paletteColors: IntArray,
        histogram: IntArray,
        sumR: LongArray,
        sumG: LongArray,
        sumB: LongArray
    ) {
        for (index in paletteColors.indices) {
            val count = histogram[index]
            if (count <= 0) {
                continue
            }
            val oldColor = paletteColors[index]
            val oldR = Color.red(oldColor)
            val oldG = Color.green(oldColor)
            val oldB = Color.blue(oldColor)
            val meanR = sumR[index].toFloat() / count.toFloat()
            val meanG = sumG[index].toFloat() / count.toFloat()
            val meanB = sumB[index].toFloat() / count.toFloat()
            val newR = (oldR + ADAPTATION_FACTOR * (meanR - oldR)).roundToInt().coerceIn(0, 255)
            val newG = (oldG + ADAPTATION_FACTOR * (meanG - oldG)).roundToInt().coerceIn(0, 255)
            val newB = (oldB + ADAPTATION_FACTOR * (meanB - oldB)).roundToInt().coerceIn(0, 255)
            adaptivePaletteColors[index] = Color.rgb(newR, newG, newB)
        }
    }

    private fun findNearestPaletteIndex(rgb: Int, palette: IntArray): Int {
        val red = Color.red(rgb)
        val green = Color.green(rgb)
        val blue = Color.blue(rgb)
        var bestIndex = 0
        var bestDistance = Int.MAX_VALUE
        for (index in palette.indices) {
            val paletteColor = palette[index]
            val dr = red - Color.red(paletteColor)
            val dg = green - Color.green(paletteColor)
            val db = blue - Color.blue(paletteColor)
            val distance = dr * dr + dg * dg + db * db
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
            }
        }
        return bestIndex
    }

    // Stores crop origin and dimensions for the grid-aligned sampling area.
    private data class CropRect(val left: Int, val top: Int, val width: Int, val height: Int)

    private companion object {
        const val ADAPTATION_FACTOR = 0.5f
    }
}
