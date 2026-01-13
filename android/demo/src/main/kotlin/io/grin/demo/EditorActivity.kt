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

import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.grin.demo.databinding.ActivityEditorBinding
import io.grin.lib.GrinFile

// Editor for adjusting channel metadata and exporting assets.
class EditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditorBinding
    private lateinit var store: GrinAssetStore
    private lateinit var metadata: GrinAssetMetadata
    private lateinit var grinFile: GrinFile
    private val renderer = GrinPreviewRenderer()
    private val playbackRenderer = GrinPlaybackRenderer()

    private var currentSettings: MutableList<ChannelSetting> = mutableListOf()
    private var originalSettings: List<ChannelSetting> = emptyList()
    private var selectedChannelId = 0
    private var isUpdatingSliders = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        store = GrinAssetStore(this)
        val assetId = intent.getStringExtra(EXTRA_ASSET_ID)
        if (assetId == null) {
            Toast.makeText(this, getString(R.string.editor_missing_asset), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val loadedMetadata = store.loadMetadata(assetId)
        val loadedFile = loadedMetadata?.let { store.loadGrinFile(it) }
        if (loadedMetadata == null || loadedFile == null) {
            Toast.makeText(this, getString(R.string.editor_missing_asset), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        metadata = loadedMetadata
        grinFile = loadedFile
        originalSettings = metadata.channelSettings.map { it.copy() }
        currentSettings = metadata.channelSettings.map { it.copy() }.toMutableList()

        setupChannelSpinner()
        setupSeekBars()
        updatePreview()

        binding.applyButton.setOnClickListener {
            applyEdits()
        }
        binding.revertButton.setOnClickListener {
            revertEdits()
        }
        binding.exportPngButton.setOnClickListener {
            exportPng()
        }
        binding.exportGifButton.setOnClickListener {
            exportGif()
        }
        binding.exportGrinButton.setOnClickListener {
            exportGrin()
        }
        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupChannelSpinner() {
        // Populate the channel selector with hex labels.
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ChannelLabels.labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.channelSpinner.adapter = adapter

        val defaultChannel = metadata.channelOrder.firstOrNull() ?: 0
        binding.channelSpinner.setSelection(defaultChannel)
        selectedChannelId = defaultChannel
        binding.channelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedChannelId = position
                updateSliderValues()
                updatePreview()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupSeekBars() {
        // Attach change listeners for live preview updates.
        binding.frequencySeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!isUpdatingSliders) {
                    updateSetting { it.frequency = progress }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        binding.intonationSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!isUpdatingSliders) {
                    updateSetting { it.intonation = progress }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        binding.transparencySeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!isUpdatingSliders) {
                    updateSetting { it.transparency = progress }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        updateSliderValues()
    }

    private fun updateSetting(update: (ChannelSetting) -> Unit) {
        // Apply slider updates to the selected channel and refresh preview.
        val setting = currentSettings.firstOrNull { it.channelId == selectedChannelId }
        if (setting != null) {
            update(setting)
            updatePreview()
        }
    }

    private fun updateSliderValues() {
        // Synchronize slider positions with the selected channel settings.
        val setting = currentSettings.firstOrNull { it.channelId == selectedChannelId } ?: return
        isUpdatingSliders = true
        binding.frequencySeek.progress = setting.frequency
        binding.intonationSeek.progress = setting.intonation
        binding.transparencySeek.progress = setting.transparency
        isUpdatingSliders = false
    }

    private fun updatePreview() {
        // Render a live preview using the current slider settings.
        val preview = renderer.renderPreview(grinFile, currentSettings, selectedChannelId)
        binding.editorPreview.setImageBitmap(preview)
    }

    private fun applyEdits() {
        // Persist updated channel overrides and GRIN rules.
        val updatedFile = store.buildUpdatedGrinFile(metadata, grinFile, currentSettings)
        grinFile = updatedFile
        updatedFile.save(metadata.grinPath)
        updatedFile.save(metadata.grimPath)
        val preview = renderer.renderPreview(grinFile, currentSettings, null)
        store.updateAssetMetadata(metadata, currentSettings.map { it.copy() }, preview)
        originalSettings = currentSettings.map { it.copy() }
        Toast.makeText(this, getString(R.string.editor_applied), Toast.LENGTH_SHORT).show()
    }

    private fun revertEdits() {
        // Restore channel settings to the last applied state.
        currentSettings = originalSettings.map { it.copy() }.toMutableList()
        updateSliderValues()
        updatePreview()
    }

    private fun exportPng() {
        // Export a PNG snapshot of the current GRIN preview.
        val updatedFile = store.buildUpdatedGrinFile(metadata, grinFile, currentSettings)
        val snapshot = playbackRenderer.renderSnapshot(updatedFile, tick = 0L)
        store.exportPng(snapshot, "${metadata.id}_snapshot.png")
        Toast.makeText(this, getString(R.string.export_complete), Toast.LENGTH_SHORT).show()
    }

    private fun exportGif() {
        // Render a short loop derived from the GRIN playback cadence.
        val updatedFile = store.buildUpdatedGrinFile(metadata, grinFile, currentSettings)
        val frameCount = 12
        val tickStep = 1L
        val frames = playbackRenderer.renderFrames(updatedFile, frameCount, tickStep)
        val delayMs = (updatedFile.header.tickMicros / 1000L).toInt().coerceAtLeast(50)
        store.exportGif("${metadata.id}_loop.gif", frames, delayMs)
        Toast.makeText(this, getString(R.string.export_complete), Toast.LENGTH_SHORT).show()
    }

    private fun exportGrin() {
        // Export the updated GRIN payload with current header settings.
        val updatedFile = store.buildUpdatedGrinFile(metadata, grinFile, currentSettings)
        store.exportGrinAndGrim(metadata, updatedFile, currentSettings, "${metadata.id}_updated")
        Toast.makeText(this, getString(R.string.export_complete), Toast.LENGTH_SHORT).show()
    }
}
