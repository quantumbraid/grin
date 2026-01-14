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

import java.util.Locale

// Helper for converting channel indices to palette labels and back.
object ChannelLabels {
    val labels: List<String> = listOf(
        "Orange",
        "Red",
        "Light Red",
        "Pink",
        "Purple",
        "Blue",
        "Light Blue",
        "Green",
        "Light Green",
        "Yellow",
        "Off White",
        "White",
        "Grey",
        "Black",
        "Golden Brown",
        "Dark Brown"
    )

    fun labelFor(channelId: Int): String {
        // Clamp to 0-15 so UI labels remain valid.
        return labels[channelId.coerceIn(0, 15)]
    }

    fun indexOf(label: String): Int {
        // Normalize input for consistent lookups.
        val normalized = label.trim().lowercase(Locale.US)
        return labels.indexOfFirst { it.lowercase(Locale.US) == normalized }.coerceAtLeast(0)
    }
}
