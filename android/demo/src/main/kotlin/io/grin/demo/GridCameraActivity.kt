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
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import io.grin.demo.databinding.ActivityGridCameraBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// Displays the live, posterized camera preview with a grid overlay.
class GridCameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGridCameraBinding
    private lateinit var cameraExecutor: ExecutorService

    private val permissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(this, getString(R.string.camera_permission_denied), Toast.LENGTH_LONG).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGridCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.closeButton.setOnClickListener {
            // Return to the main demo screen.
            finish()
        }

        if (hasCameraPermission()) {
            startCamera()
        } else {
            permissionRequest.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Shut down the camera executor to release resources.
        cameraExecutor.shutdown()
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                bindAnalysisUseCase(cameraProvider)
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun bindAnalysisUseCase(cameraProvider: ProcessCameraProvider) {
        cameraProvider.unbindAll()
        val palette = PosterizedPalette.defaultPalette()
        val performanceConfig = PosterizationPerformanceConfig(
            targetFps = 30,
            fallbackFps = 24,
            slowdownThresholdMs = 42,
            fallbackGridScale = 0.75f,
            fallbackPaletteSize = 12
        )
        val analyzer = PosterizedCameraAnalyzer(
            baseGridCols = 48,
            baseGridRows = 64,
            palette = palette,
            performanceConfig = performanceConfig
        ) { frame ->
            runOnUiThread {
                // Update the preview image and overlay metadata on the main thread.
                binding.posterizedPreview.setImageBitmap(frame.bitmap)
                binding.gridOverlay.updateGrid(
                    frame.gridCols,
                    frame.gridRows,
                    frame.paletteIndices,
                    frame.paletteLabels
                )
                binding.performanceText.text = getString(
                    R.string.camera_performance_format,
                    frame.gridCols,
                    frame.gridRows,
                    frame.paletteSize
                )
            }
        }

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(cameraExecutor, analyzer) }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, analysis)
        } catch (exception: Exception) {
            Toast.makeText(this, getString(R.string.camera_start_failed), Toast.LENGTH_LONG).show()
        }
    }
}
