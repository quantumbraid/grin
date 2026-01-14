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

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import io.grin.demo.databinding.ActivityGalleryBinding

// Displays a grid of captured GRIN assets.
class GalleryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGalleryBinding
    private lateinit var adapter: GalleryAdapter
    private lateinit var store: GrinAssetStore
    private var selectionMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        store = GrinAssetStore(this)
        adapter = GalleryAdapter(store) { asset ->
            if (selectionMode) {
                val count = adapter.toggleSelection(asset.id)
                if (count == 2) {
                    val selected = adapter.getSelectedIds()
                    openDiffViewer(selected[0], selected[1])
                }
            } else {
                startActivity(Intent(this, GrinViewerActivity::class.java).putExtra(EXTRA_ASSET_ID, asset.id))
            }
        }

        binding.galleryRecycler.layoutManager = GridLayoutManager(this, 2)
        binding.galleryRecycler.adapter = adapter
        binding.backButton.setOnClickListener { finish() }
        binding.editButton.setOnClickListener { toggleSelectionMode() }
    }

    override fun onResume() {
        super.onResume()
        refreshAssets()
    }

    private fun refreshAssets() {
        val assets = store.loadAllMetadata()
        adapter.setItems(assets)
        binding.emptyState.visibility = if (assets.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun toggleSelectionMode() {
        selectionMode = !selectionMode
        adapter.setSelectionMode(selectionMode)
        binding.editButton.text = getString(if (selectionMode) R.string.cancel else R.string.edit)
        if (selectionMode) {
            android.widget.Toast.makeText(this, getString(R.string.select_two_images), android.widget.Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun openDiffViewer(firstId: String, secondId: String) {
        selectionMode = false
        adapter.setSelectionMode(false)
        binding.editButton.text = getString(R.string.edit)
        val intent = Intent(this, DiffViewerActivity::class.java)
            .putExtra(EXTRA_ASSET_ID_A, firstId)
            .putExtra(EXTRA_ASSET_ID_B, secondId)
        startActivity(intent)
    }
}
