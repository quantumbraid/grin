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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.grin.demo.databinding.ActivityGrinViewerBinding

// Displays a single GRIN file with playback controls.
class GrinViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGrinViewerBinding
    private lateinit var store: GrinAssetStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGrinViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        store = GrinAssetStore(this)

        val assetId = intent.getStringExtra(EXTRA_ASSET_ID)
        if (assetId == null) {
            Toast.makeText(this, getString(R.string.viewer_missing_asset), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val metadata = store.loadMetadata(assetId)
        val grinFile = metadata?.let { store.loadGrinFile(it) }
        if (metadata == null || grinFile == null) {
            Toast.makeText(this, getString(R.string.viewer_missing_asset), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.grinView.load(grinFile)
        binding.metadataText.text = getString(
            R.string.gallery_asset_metadata,
            metadata.width,
            metadata.height,
            metadata.paletteBins
        )

        binding.playButton.setOnClickListener { binding.grinView.play() }
        binding.pauseButton.setOnClickListener { binding.grinView.pause() }
        binding.stopButton.setOnClickListener { binding.grinView.stop() }
        binding.backButton.setOnClickListener { finish() }
    }
}
