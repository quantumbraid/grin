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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.grin.demo.databinding.ItemGalleryAssetBinding

class GalleryAdapter(
    private val store: GrinAssetStore,
    private val onItemClicked: (GrinAssetMetadata) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {
    private val assets: MutableList<GrinAssetMetadata> = mutableListOf()
    private val selectedIds: MutableSet<String> = mutableSetOf()
    private var selectionMode: Boolean = false

    fun setItems(items: List<GrinAssetMetadata>) {
        assets.clear()
        assets.addAll(items)
        notifyDataSetChanged()
    }

    fun setSelectionMode(enabled: Boolean) {
        selectionMode = enabled
        if (!enabled) {
            selectedIds.clear()
        }
        notifyDataSetChanged()
    }

    fun toggleSelection(id: String): Int {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id)
        } else if (selectedIds.size < MAX_SELECTIONS) {
            selectedIds.add(id)
        }
        notifyDataSetChanged()
        return selectedIds.size
    }

    fun getSelectedIds(): List<String> = selectedIds.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemGalleryAssetBinding.inflate(inflater, parent, false)
        return GalleryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        holder.bind(assets[position])
    }

    override fun getItemCount(): Int = assets.size

    inner class GalleryViewHolder(private val binding: ItemGalleryAssetBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(asset: GrinAssetMetadata) {
            val thumbnail = store.loadThumbnailBitmap(asset)
            if (thumbnail != null) {
                binding.thumbnail.setImageBitmap(thumbnail)
            } else {
                binding.thumbnail.setImageDrawable(null)
            }
            binding.metadata.text = binding.root.context.getString(
                R.string.gallery_asset_metadata,
                asset.width,
                asset.height,
                asset.paletteBins
            )
            binding.root.alpha = if (selectionMode && selectedIds.contains(asset.id)) 0.6f else 1.0f
            binding.root.setOnClickListener { onItemClicked(asset) }
        }
    }

    private companion object {
        const val MAX_SELECTIONS = 2
    }
}
