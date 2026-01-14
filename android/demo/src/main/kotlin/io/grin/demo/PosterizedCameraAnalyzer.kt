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
    private val colorAdjustments: CameraColorAdjustments,
    private val onFrame: (PosterizedFrame) -> Unit
) : ImageAnalysis.Analyzer {
    private val whiteColor = Color.rgb(0xFF, 0xFF, 0xFF)
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

        val frame = buildPosterizedFrame(
            image,
            effectiveGridCols,
            effectiveGridRows,
            effectivePalette,
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
        palette: PosterizedPalette,
        rotationDegrees: Int
    ): PosterizedFrame {
        val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
        val adjustedRotation = (normalizedRotation + 180) % 360
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
        var paletteIndices = IntArray(pixelCount)
        // Track per-bin counts for channel assignment and gallery metadata.
        val histogram = IntArray(palette.colors.size)

        if (paletteMode == PaletteMode.HSV_PRESET) {
            val paletteHsv = Array(palette.colors.size) { FloatArray(3) }
            for (index in palette.colors.indices) {
                val color = palette.colors[index]
                Color.RGBToHSV(
                    Color.red(color),
                    Color.green(color),
                    Color.blue(color),
                    paletteHsv[index]
                )
            }

            for (row in 0 until gridRows) {
                for (col in 0 until gridCols) {
                    val representative = sampleMedianColor(
                        image,
                        crop,
                        gridCols,
                        gridRows,
                        row,
                        col,
                        adjustedRotation
                    )
                    val hsv = FloatArray(3)
                    Color.RGBToHSV(
                        Color.red(representative),
                        Color.green(representative),
                        Color.blue(representative),
                        hsv
                    )
                    val adjustedHsv = adjustHsvAdaptive(hsv, colorAdjustments)
                    val paletteIndex = findNearestPresetIndexByHsv(
                        adjustedHsv,
                        paletteHsv,
                        isSkinTone(adjustedHsv)
                    )
                    paletteIndices[row * gridCols + col] = paletteIndex
                }
            }

            paletteIndices = stabilizePaletteIndices(
                paletteIndices,
                gridCols,
                gridRows,
                palette.colors.size
            )

            for (row in 0 until gridRows) {
                for (col in 0 until gridCols) {
                    val index = row * gridCols + col
                    val paletteIndex = paletteIndices[index]
                    val paletteColor = palette.colors[paletteIndex]
                    if (paletteColor != whiteColor) {
                        bitmap.setPixel(col, row, paletteColor)
                    }
                    histogram[paletteIndex] += 1
                }
            }
        } else {
            val rawColors = IntArray(pixelCount)
            val lumaHistogram = IntArray(256)

            for (row in 0 until gridRows) {
                val rotatedY = crop.top + ((row + 0.5f) * crop.height / gridRows).roundToInt()
                for (col in 0 until gridCols) {
                    val rotatedX = crop.left + ((col + 0.5f) * crop.width / gridCols).roundToInt()
                    val sourceX: Int
                    val sourceY: Int
                    when (adjustedRotation) {
                        90 -> {
                            sourceX = image.width - 1 - rotatedY
                            sourceY = rotatedX
                        }
                        180 -> {
                            sourceX = image.width - 1 - rotatedX
                            sourceY = image.height - 1 - rotatedY
                        }
                        270 -> {
                            sourceX = rotatedY
                            sourceY = image.height - 1 - rotatedX
                        }
                        else -> {
                            sourceX = rotatedX
                            sourceY = rotatedY
                        }
                    }
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
                    val index = row * gridCols + col
                    val adjusted = adjustColor(rawColors[index], blackPoint, whitePoint, colorAdjustments)
                    val paletteIndex = findNearestPaletteIndex(adjusted, palette.colors)
                    val paletteColor = palette.colors[paletteIndex]
                    if (paletteColor != whiteColor) {
                        bitmap.setPixel(col, row, paletteColor)
                    }
                    paletteIndices[index] = paletteIndex
                    histogram[paletteIndex] += 1
                }
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

    private fun sampleMedianColor(
        image: ImageProxy,
        crop: CropRect,
        gridCols: Int,
        gridRows: Int,
        row: Int,
        col: Int,
        rotation: Int
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
                val sourceX: Int
                val sourceY: Int
                when (rotation) {
                    90 -> {
                        sourceX = image.width - 1 - rotatedY
                        sourceY = rotatedX
                    }
                    180 -> {
                        sourceX = image.width - 1 - rotatedX
                        sourceY = image.height - 1 - rotatedY
                    }
                    270 -> {
                        sourceX = rotatedY
                        sourceY = image.height - 1 - rotatedX
                    }
                    else -> {
                        sourceX = rotatedX
                        sourceY = rotatedY
                    }
                }
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

    private fun isSkinTone(hsv: FloatArray): Boolean {
        val hue = hsv[0]
        val saturation = hsv[1]
        val value = hsv[2]
        return hue >= 10f && hue <= 45f &&
            saturation >= 0.18f && saturation <= 0.6f &&
            value >= 0.35f && value <= 0.95f
    }

    private fun findNearestPresetIndexByHsv(
        hsv: FloatArray,
        paletteHsv: Array<FloatArray>,
        skinBias: Boolean
    ): Int {
        val hue = hsv[0]
        val saturation = hsv[1]
        val value = hsv[2]
        val neutral = saturation < 0.15f || value < 0.12f || value > 0.92f
        val hueWeight = if (neutral) {
            0.1f
        } else {
            0.6f + 1.4f * saturation
        }
        val saturationWeight = if (neutral) 1.4f else 1.0f
        val valueWeight = if (neutral) 1.2f else 1.0f
        var bestIndex = 0
        var bestDistance = Float.MAX_VALUE
        for (index in paletteHsv.indices) {
            val target = paletteHsv[index]
            val hueDelta = hueDistance(hue, target[0]) / 180f
            val saturationDelta = saturation - target[1]
            val valueDelta = value - target[2]
            var distance =
                hueDelta * hueDelta * hueWeight +
                    saturationDelta * saturationDelta * saturationWeight +
                    valueDelta * valueDelta * valueWeight
            if (skinBias && index == PALETTE_LIGHT_RED) {
                distance *= 0.7f
            }
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
            }
        }
        return bestIndex
    }

    private fun stabilizePaletteIndices(
        indices: IntArray,
        gridCols: Int,
        gridRows: Int,
        paletteSize: Int
    ): IntArray {
        val output = indices.copyOf()
        val counts = IntArray(paletteSize)
        for (row in 0 until gridRows) {
            for (col in 0 until gridCols) {
                counts.fill(0)
                val index = row * gridCols + col
                for (dy in -1..1) {
                    val neighborRow = row + dy
                    if (neighborRow < 0 || neighborRow >= gridRows) continue
                    for (dx in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        val neighborCol = col + dx
                        if (neighborCol < 0 || neighborCol >= gridCols) continue
                        val neighborIndex = neighborRow * gridCols + neighborCol
                        counts[indices[neighborIndex]] += 1
                    }
                }
                val current = indices[index]
                var majority = current
                var majorityCount = counts[current]
                for (paletteIndex in 0 until paletteSize) {
                    if (counts[paletteIndex] > majorityCount) {
                        majorityCount = counts[paletteIndex]
                        majority = paletteIndex
                    }
                }
                if (majority != current && majorityCount >= 4 && counts[current] <= 1) {
                    output[index] = majority
                }
            }
        }
        return output
    }

    private fun hueDistance(a: Float, b: Float): Float {
        val delta = kotlin.math.abs(a - b)
        return kotlin.math.min(delta, 360f - delta)
    }

    private fun findNearestPaletteIndex(rgb: Int, palette: IntArray): Int {
        // Find the palette entry with the smallest Euclidean distance in RGB space.
        val red = Color.red(rgb)
        val green = Color.green(rgb)
        val blue = Color.blue(rgb)
        val inputSaturation = colorSaturation(red, green, blue)
        var bestIndex = 0
        var bestDistance = Double.MAX_VALUE
        for (index in palette.indices) {
            val paletteColor = palette[index]
            val paletteRed = Color.red(paletteColor)
            val paletteGreen = Color.green(paletteColor)
            val paletteBlue = Color.blue(paletteColor)
            val paletteSaturation = colorSaturation(paletteRed, paletteGreen, paletteBlue)
            val saturationPenalty = if (inputSaturation > 0.04) {
                (1.0 - paletteSaturation) * 120.0 * inputSaturation
            } else {
                0.0
            }
            val distance = colorDistance(
                red,
                green,
                blue,
                paletteRed,
                paletteGreen,
                paletteBlue
            ) + saturationPenalty
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
            }
        }
        return bestIndex
    }

    private fun colorSaturation(red: Int, green: Int, blue: Int): Double {
        val maxChannel = maxOf(red, green, blue).toDouble()
        val minChannel = minOf(red, green, blue).toDouble()
        if (maxChannel <= 0.0) {
            return 0.0
        }
        return (maxChannel - minChannel) / maxChannel
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

    private companion object {
        const val PALETTE_ORANGE = 0
        const val PALETTE_RED = 1
        const val PALETTE_LIGHT_RED = 2
        const val PALETTE_PINK = 3
        const val PALETTE_PURPLE = 4
        const val PALETTE_BLUE = 5
        const val PALETTE_LIGHT_BLUE = 6
        const val PALETTE_GREEN = 7
        const val PALETTE_LIGHT_GREEN = 8
        const val PALETTE_YELLOW = 9
        const val PALETTE_OFF_WHITE = 10
        const val PALETTE_WHITE = 11
        const val PALETTE_GREY = 12
        const val PALETTE_BLACK = 13
        const val PALETTE_GOLDEN_BROWN = 14
        const val PALETTE_DARK_BROWN = 15
    }
}
