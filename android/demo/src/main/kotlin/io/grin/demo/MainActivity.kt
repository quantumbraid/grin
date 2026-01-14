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

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.slider.Slider
import io.grin.demo.databinding.ActivityMainBinding
import io.grin.lib.GrinFile
import io.grin.lib.GrinUriLoader

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val filePicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                loadUri(uri)
            }
        }
    private val storagePermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val denied = result.filterValues { !it }.keys
            if (denied.isNotEmpty()) {
                Toast.makeText(this, getString(R.string.storage_permission_denied), Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ensureStoragePermissions()

        binding.loadButton.setOnClickListener {
            filePicker.launch(arrayOf("application/octet-stream"))
        }
        binding.cameraButton.setOnClickListener {
            // Launch the grid camera preview workflow.
            startActivity(Intent(this, GridCameraActivity::class.java))
        }
        binding.galleryButton.setOnClickListener {
            // Open the capture gallery grid.
            startActivity(Intent(this, GalleryActivity::class.java))
        }
        binding.playButton.setOnClickListener { binding.grinView.play() }
        binding.pauseButton.setOnClickListener { binding.grinView.pause() }
        binding.stopButton.setOnClickListener { binding.grinView.stop() }

        binding.seekBar.addOnChangeListener { _: Slider, value: Float, fromUser: Boolean ->
            if (fromUser) {
                binding.grinView.seek(value.toLong())
            }
        }

        setupSampleList()

        intent?.data?.let { loadUri(it) }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let { loadUri(it) }
    }

    private fun setupSampleList() {
        val samples = assets.list("samples")?.sorted() ?: emptyList()
        if (samples.isEmpty()) {
            binding.samplesTitle.text = getString(R.string.no_samples)
            return
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, samples)
        binding.samplesList.adapter = adapter
        binding.samplesList.setOnItemClickListener { _, _, position, _ ->
            val assetName = samples[position]
            val file = GrinUriLoader.fromAsset(this, "samples/$assetName")
            loadFile(file)
        }
    }

    private fun loadUri(uri: Uri) {
        try {
            val file = GrinUriLoader.load(this, uri)
            loadFile(file)
        } catch (exception: Exception) {
            Toast.makeText(this, getString(R.string.error_loading), Toast.LENGTH_LONG).show()
        }
    }

    private fun loadFile(file: GrinFile) {
        binding.grinView.load(file)
        binding.metadataText.text = buildMetadata(file)
    }

    private fun buildMetadata(file: GrinFile): String {
        val header = file.header
        val dimensions = getString(R.string.dimensions_format, header.width.toInt(), header.height.toInt())
        val rules = getString(R.string.rule_count_format, header.ruleCount)
        val tickRate = getString(R.string.tick_rate_format, header.tickMicros.toInt())
        return listOf(dimensions, rules, tickRate).joinToString(separator = "\n")
    }

    private fun ensureStoragePermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            storagePermissionRequest.launch(toRequest.toTypedArray())
        }
    }
}
