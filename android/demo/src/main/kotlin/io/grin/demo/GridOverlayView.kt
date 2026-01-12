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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

// Draws grid lines and per-cell channel labels over the posterized preview.
class GridOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 18f
        textAlign = Paint.Align.LEFT
    }

    private var gridCols: Int = 0
    private var gridRows: Int = 0
    private var paletteIndices: IntArray = IntArray(0)
    private var paletteLabels: List<String> = emptyList()

    // Updates the grid metadata and invalidates the overlay for a new frame.
    fun updateGrid(
        cols: Int,
        rows: Int,
        indices: IntArray,
        labels: List<String>
    ) {
        gridCols = cols
        gridRows = rows
        paletteIndices = indices
        paletteLabels = labels
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (gridCols <= 0 || gridRows <= 0) {
            return
        }
        val cellWidth = width.toFloat() / gridCols.toFloat()
        val cellHeight = height.toFloat() / gridRows.toFloat()
        drawGridLines(canvas, cellWidth, cellHeight)
        drawLabels(canvas, cellWidth, cellHeight)
    }

    private fun drawGridLines(canvas: Canvas, cellWidth: Float, cellHeight: Float) {
        for (col in 0..gridCols) {
            val x = col * cellWidth
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
        }
        for (row in 0..gridRows) {
            val y = row * cellHeight
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
        }
    }

    private fun drawLabels(canvas: Canvas, cellWidth: Float, cellHeight: Float) {
        // Skip text if cells are too small to stay readable.
        if (cellWidth < 20f || cellHeight < 20f || paletteLabels.isEmpty()) {
            return
        }
        val maxLabelIndex = paletteLabels.size
        for (row in 0 until gridRows) {
            val y = row * cellHeight + textPaint.textSize
            for (col in 0 until gridCols) {
                val index = row * gridCols + col
                val paletteIndex = if (index < paletteIndices.size) paletteIndices[index] else 0
                val label = paletteLabels[paletteIndex % maxLabelIndex]
                val x = col * cellWidth + 4f
                canvas.drawText(label, x, y, textPaint)
            }
        }
    }
}
