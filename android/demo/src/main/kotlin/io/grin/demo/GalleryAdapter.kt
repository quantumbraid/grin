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
import android.util.LruCache
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.grin.demo.databinding.ItemGalleryAssetBinding

// Adapter that renders posterized thumbnails in a grid for the gallery screen.
class GalleryAdapter(
    private val store: GrinAssetStore,
    private val onItemSelected: (GrinAssetMetadata) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {

    private val assets: MutableList<GrinAssetMetadata> = mutableListOf()
    private val thumbnailCache = object : LruCache<String, Bitmap>(32) {}

    fun setItems(items: List<GrinAssetMetadata>) {
        // Replace the adapter contents with a new list of assets.
        assets.clear()
        assets.addAll(items)
        notifyDataSetChanged()
    }

    fun appendItems(items: List<GrinAssetMetadata>) {
        // Append assets when paging additional results.
        val start = assets.size
        assets.addAll(items)
        notifyItemRangeInserted(start, items.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val binding = ItemGalleryAssetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GalleryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        val asset = assets[position]
        holder.bind(asset)
    }

    override fun getItemCount(): Int = assets.size

    inner class GalleryViewHolder(private val binding: ItemGalleryAssetBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(asset: GrinAssetMetadata) {
            // Load cached thumbnail, falling back to disk.
            val cached = thumbnailCache.get(asset.id)
            val thumbnail = cached ?: store.loadThumbnailBitmap(asset)
            if (thumbnail != null && cached == null) {
                thumbnailCache.put(asset.id, thumbnail)
            }
            binding.thumbnail.setImageBitmap(thumbnail)
            binding.assetMetadata.text = itemView.context.getString(
                R.string.gallery_asset_metadata,
                asset.width,
                asset.height,
                asset.paletteBins
            )
            binding.root.setOnClickListener {
                onItemSelected(asset)
            }
        }
    }
}
