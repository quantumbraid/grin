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

import org.json.JSONArray
import org.json.JSONObject

// Represents editable channel metadata for a single GRIN channel.
data class ChannelSetting(
    val channelId: Int,
    var frequency: Int,
    var intonation: Int,
    var transparency: Int
)

// Captures persisted metadata for a GRIN/GRIM asset pair.
data class GrinAssetMetadata(
    val id: String,
    val createdAt: Long,
    var lastEditedAt: Long,
    val width: Int,
    val height: Int,
    val gridCols: Int,
    val gridRows: Int,
    val paletteBins: Int,
    var tickMicros: Long,
    var ruleCount: Int,
    val channelMap: List<Int>,
    val channelOrder: List<Int>,
    val paletteHistogram: List<Int>,
    val previewPath: String,
    val thumbnailPath: String,
    val grinPath: String,
    val grimPath: String,
    var channelSettings: List<ChannelSetting>
) {
    fun toJson(): JSONObject {
        // Serialize metadata to JSON for disk persistence and gallery indexing.
        val json = JSONObject()
        json.put("id", id)
        json.put("createdAt", createdAt)
        json.put("lastEditedAt", lastEditedAt)
        json.put("width", width)
        json.put("height", height)
        json.put("gridCols", gridCols)
        json.put("gridRows", gridRows)
        json.put("paletteBins", paletteBins)
        json.put("tickMicros", tickMicros)
        json.put("ruleCount", ruleCount)
        json.put("previewPath", previewPath)
        json.put("thumbnailPath", thumbnailPath)
        json.put("grinPath", grinPath)
        json.put("grimPath", grimPath)
        json.put("channelMap", JSONArray(channelMap))
        json.put("channelOrder", JSONArray(channelOrder))
        json.put("paletteHistogram", JSONArray(paletteHistogram))

        val settingsArray = JSONArray()
        channelSettings.forEach { setting ->
            val settingJson = JSONObject()
            settingJson.put("channelId", setting.channelId)
            settingJson.put("frequency", setting.frequency)
            settingJson.put("intonation", setting.intonation)
            settingJson.put("transparency", setting.transparency)
            settingsArray.put(settingJson)
        }
        json.put("channelSettings", settingsArray)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): GrinAssetMetadata {
            // Deserialize metadata from JSON stored on disk.
            val channelMap = json.getJSONArray("channelMap").toIntList()
            val channelOrder = json.getJSONArray("channelOrder").toIntList()
            val histogram = json.getJSONArray("paletteHistogram").toIntList()
            val settings = json.getJSONArray("channelSettings").toChannelSettings()
            return GrinAssetMetadata(
                id = json.getString("id"),
                createdAt = json.getLong("createdAt"),
                lastEditedAt = json.getLong("lastEditedAt"),
                width = json.getInt("width"),
                height = json.getInt("height"),
                gridCols = json.getInt("gridCols"),
                gridRows = json.getInt("gridRows"),
                paletteBins = json.getInt("paletteBins"),
                tickMicros = json.getLong("tickMicros"),
                ruleCount = json.getInt("ruleCount"),
                channelMap = channelMap,
                channelOrder = channelOrder,
                paletteHistogram = histogram,
                previewPath = json.getString("previewPath"),
                thumbnailPath = json.getString("thumbnailPath"),
                grinPath = json.getString("grinPath"),
                grimPath = json.getString("grimPath"),
                channelSettings = settings
            )
        }
    }
}

private fun JSONArray.toIntList(): List<Int> {
    // Parse a JSON array into a list of integers.
    val list = mutableListOf<Int>()
    for (index in 0 until length()) {
        list.add(getInt(index))
    }
    return list
}

private fun JSONArray.toChannelSettings(): List<ChannelSetting> {
    // Parse channel settings from a JSON array.
    val list = mutableListOf<ChannelSetting>()
    for (index in 0 until length()) {
        val item = getJSONObject(index)
        list.add(
            ChannelSetting(
                channelId = item.getInt("channelId"),
                frequency = item.getInt("frequency"),
                intonation = item.getInt("intonation"),
                transparency = item.getInt("transparency")
            )
        )
    }
    return list
}
