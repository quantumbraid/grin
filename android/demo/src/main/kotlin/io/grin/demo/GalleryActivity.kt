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
import androidx.recyclerview.widget.RecyclerView
import io.grin.demo.databinding.ActivityGalleryBinding

// Displays a grid of captured GRIN assets with lazy paging.
class GalleryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGalleryBinding
    private lateinit var adapter: GalleryAdapter
    private lateinit var store: GrinAssetStore

    private val allAssets: MutableList<GrinAssetMetadata> = mutableListOf()
    private val loadedAssets: MutableList<GrinAssetMetadata> = mutableListOf()
    private var pageIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        store = GrinAssetStore(this)
        adapter = GalleryAdapter(store) { asset ->
            startActivity(Intent(this, EditorActivity::class.java).putExtra(EXTRA_ASSET_ID, asset.id))
        }

        binding.galleryRecycler.layoutManager = GridLayoutManager(this, 2)
        binding.galleryRecycler.adapter = adapter
        binding.galleryRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    val layoutManager = recyclerView.layoutManager as? GridLayoutManager ?: return
                    val lastVisible = layoutManager.findLastVisibleItemPosition()
                    if (lastVisible >= loadedAssets.size - 4) {
                        loadNextPage()
                    }
                }
            }
        })

        binding.captureNewButton.setOnClickListener {
            startActivity(Intent(this, GridCameraActivity::class.java))
        }
        binding.backButton.setOnClickListener {
            finish()
        }

        refreshAssets()
    }

    override fun onResume() {
        super.onResume()
        // Refresh the gallery list to reflect any edits or exports.
        refreshAssets()
    }

    private fun refreshAssets() {
        // Reload metadata from disk and reset paging.
        allAssets.clear()
        allAssets.addAll(store.loadAllMetadata())
        loadedAssets.clear()
        pageIndex = 0
        adapter.setItems(emptyList())
        binding.emptyState.visibility = if (allAssets.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        loadNextPage()
    }

    private fun loadNextPage() {
        // Append the next page of gallery assets.
        val start = pageIndex * PAGE_SIZE
        if (start >= allAssets.size) {
            return
        }
        val end = (start + PAGE_SIZE).coerceAtMost(allAssets.size)
        val nextPage = allAssets.subList(start, end)
        if (pageIndex == 0) {
            adapter.setItems(nextPage)
        } else {
            adapter.appendItems(nextPage)
        }
        loadedAssets.addAll(nextPage)
        pageIndex += 1
    }

    companion object {
        // Keep page size small to simulate lazy loading and caching.
        private const val PAGE_SIZE = 20
    }
}
