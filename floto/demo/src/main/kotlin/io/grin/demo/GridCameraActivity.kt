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
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import io.grin.demo.databinding.ActivityGridCameraBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// Displays the live, posterized camera preview with a grid overlay.
class GridCameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGridCameraBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var ioExecutor: ExecutorService
    private lateinit var assetStore: GrinAssetStore
    private var latestFrame: PosterizedFrame? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var activeCamera: Camera? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var torchEnabled: Boolean = false
    private var useHsvPreset: Boolean = false
    private var storageUri: Uri? = null
    private var exportPngEnabled: Boolean = false
    private var storageButton: MaterialButton? = null
    // Keep shared color adjustments that are updated by the settings sliders.
    private val colorAdjustments = CameraColorAdjustments(
        contrast = 1.0f,
        saturation = 1.0f,
        brightness = 1.0f
    )

    private val permissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(this, getString(R.string.camera_permission_denied), Toast.LENGTH_LONG).show()
                finish()
            }
        }
    private val storageLocationPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, flags)
                storageUri = uri
                StoragePreferences.setStorageUri(this, uri)
                storageButton?.text = storageButtonLabel(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGridCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        ioExecutor = Executors.newSingleThreadExecutor()
        assetStore = GrinAssetStore(this)
        storageUri = StoragePreferences.getStorageUri(this)

        binding.captureButton.setOnClickListener {
            val frame = latestFrame
            if (frame == null) {
                Toast.makeText(this, getString(R.string.capture_no_frame), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveCapture(frame)
        }
        binding.galleryButton.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }
        binding.presetButton.setOnClickListener {
            useHsvPreset = !useHsvPreset
            updatePresetButton()
            cameraProvider?.let { provider -> bindAnalysisUseCase(provider) }
        }
        binding.flashButton.setOnClickListener { toggleFlash() }
        binding.flipButton.setOnClickListener { toggleCamera() }
        binding.settingsButton.setOnClickListener { showSettingsDialog() }

        updatePresetButton()

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
        ioExecutor.shutdown()
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
                this.cameraProvider = cameraProvider
                bindAnalysisUseCase(cameraProvider)
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun bindAnalysisUseCase(cameraProvider: ProcessCameraProvider) {
        cameraProvider.unbindAll()
        val palette = PosterizedPalette.defaultPalette()
        val (baseGridCols, baseGridRows) = computeBaseGridDimensions()
        val performanceConfig = PosterizationPerformanceConfig(
            targetFps = 30,
            fallbackFps = 24,
            slowdownThresholdMs = 42,
            fallbackGridScale = 0.75f,
            fallbackPaletteSize = palette.colors.size
        )
        val analyzer = PosterizedCameraAnalyzer(
            baseGridCols = baseGridCols,
            baseGridRows = baseGridRows,
            palette = palette,
            paletteMode = if (useHsvPreset) PaletteMode.HSV_PRESET else PaletteMode.CLASSIC,
            isRearFacing = lensFacing == CameraSelector.LENS_FACING_BACK,
            colorAdjustments = colorAdjustments,
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
                // Cache the latest frame for capture review.
                latestFrame = frame
            }
        }

        val analysis = ImageAnalysis.Builder()
            .setTargetRotation(binding.posterizedPreview.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(cameraExecutor, analyzer) }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
        try {
            activeCamera = cameraProvider.bindToLifecycle(this, cameraSelector, analysis)
            updateFlashButtonState()
        } catch (exception: Exception) {
            Toast.makeText(this, getString(R.string.camera_start_failed), Toast.LENGTH_LONG).show()
        }
    }

    private fun computeBaseGridDimensions(): Pair<Int, Int> {
        val (width, height) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            metrics.widthPixels to metrics.heightPixels
        }
        val cols = (width / 10).coerceAtLeast(1)
        val rows = (height / 10).coerceAtLeast(1)
        return cols to rows
    }

    private fun updatePresetButton() {
        binding.presetButton.text = getString(
            if (useHsvPreset) R.string.preset_hsv else R.string.preset_classic
        )
    }

    private fun showSettingsDialog() {
        // Inflate the slider UI and bind the current adjustment values.
        val dialogView = layoutInflater.inflate(R.layout.dialog_camera_settings, null)
        val contrastSlider = dialogView.findViewById<Slider>(R.id.contrastSlider)
        val saturationSlider = dialogView.findViewById<Slider>(R.id.saturationSlider)
        val brightnessSlider = dialogView.findViewById<Slider>(R.id.brightnessSlider)
        val storageLocationButton = dialogView.findViewById<MaterialButton>(R.id.storageLocationButton)
        val exportPngCheckbox = dialogView.findViewById<MaterialCheckBox>(R.id.exportPngCheckbox)

        contrastSlider.value = colorAdjustments.contrast
        saturationSlider.value = colorAdjustments.saturation
        brightnessSlider.value = colorAdjustments.brightness
        storageButton = storageLocationButton
        storageLocationButton.text = storageButtonLabel(storageUri)
        exportPngCheckbox.isChecked = exportPngEnabled

        // Update the shared adjustments as the sliders move.
        contrastSlider.addOnChangeListener { _, value, _ ->
            colorAdjustments.contrast = value
        }
        saturationSlider.addOnChangeListener { _, value, _ ->
            colorAdjustments.saturation = value
        }
        brightnessSlider.addOnChangeListener { _, value, _ ->
            colorAdjustments.brightness = value
        }
        storageLocationButton.setOnClickListener {
            storageLocationPicker.launch(null)
        }
        exportPngCheckbox.setOnCheckedChangeListener { _, isChecked ->
            exportPngEnabled = isChecked
        }

        val settingsDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.camera_settings)
            .setView(dialogView)
            .setPositiveButton(R.string.close) { dialog, _ ->
                // Close the settings dialog when the user is done.
                dialog.dismiss()
            }
            .create()

        var dialogDimAmount = 0.6f
        settingsDialog.setOnShowListener {
            dialogDimAmount = settingsDialog.window?.attributes?.dimAmount ?: dialogDimAmount
        }
        settingsDialog.setOnDismissListener {
            storageButton = null
        }

        fun setSettingsDialogVisible(visible: Boolean) {
            val window = settingsDialog.window ?: return
            window.decorView.alpha = if (visible) 1f else 0f
            window.setDimAmount(if (visible) dialogDimAmount else 0f)
        }

        val touchListener = object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                setSettingsDialogVisible(false)
            }

            override fun onStopTrackingTouch(slider: Slider) {
                setSettingsDialogVisible(true)
            }
        }
        contrastSlider.addOnSliderTouchListener(touchListener)
        saturationSlider.addOnSliderTouchListener(touchListener)
        brightnessSlider.addOnSliderTouchListener(touchListener)

        settingsDialog.show()
    }


    private fun toggleCamera() {
        val provider = cameraProvider ?: return
        val nextFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        val selector = CameraSelector.Builder()
            .requireLensFacing(nextFacing)
            .build()
        val hasCamera = try {
            provider.hasCamera(selector)
        } catch (exception: CameraInfoUnavailableException) {
            false
        }
        if (!hasCamera) {
            Toast.makeText(this, getString(R.string.camera_unavailable), Toast.LENGTH_SHORT).show()
            return
        }
        lensFacing = nextFacing
        torchEnabled = false
        bindAnalysisUseCase(provider)
    }

    private fun toggleFlash() {
        val camera = activeCamera
        if (camera == null || !camera.cameraInfo.hasFlashUnit()) {
            torchEnabled = false
            updateFlashButtonState()
            Toast.makeText(this, getString(R.string.flash_unavailable), Toast.LENGTH_SHORT).show()
            return
        }
        torchEnabled = !torchEnabled
        camera.cameraControl.enableTorch(torchEnabled)
        updateFlashButtonState()
    }

    private fun updateFlashButtonState() {
        val hasFlash = activeCamera?.cameraInfo?.hasFlashUnit() == true
        if (!hasFlash) {
            torchEnabled = false
        }
        binding.flashButton.isEnabled = hasFlash
        binding.flashButton.text = getString(if (torchEnabled) R.string.flash_on else R.string.flash_off)
    }

    private fun saveCapture(frame: PosterizedFrame) {
        binding.captureButton.isEnabled = false
        ioExecutor.execute {
            val result = runCatching { assetStore.createAssetFromFrame(frame, DEFAULT_TICK_MICROS) }
            result.getOrNull()?.let { metadata ->
                runCatching {
                    exportCaptureOutputs(metadata, frame, storageUri, exportPngEnabled)
                }
            }
            runOnUiThread {
                binding.captureButton.isEnabled = true
                val messageId = if (result.isSuccess) R.string.capture_saved else R.string.capture_failed
                Toast.makeText(this, getString(messageId), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportCaptureOutputs(
        metadata: GrinAssetMetadata,
        frame: PosterizedFrame,
        exportUri: Uri?,
        exportPng: Boolean
    ) {
        if (exportUri == null) {
            return
        }
        val folder = DocumentFile.fromTreeUri(this, exportUri) ?: return
        val grinFile = assetStore.loadGrinFile(metadata) ?: return
        val baseName = metadata.id
        folder.createFile("application/octet-stream", "$baseName.grin")?.let { target ->
            assetStore.exportGrinToUri(grinFile, target.uri)
        }
        if (exportPng) {
            folder.createFile("image/png", "$baseName.png")?.let { target ->
                assetStore.exportPngToUri(frame.bitmap, target.uri)
            }
        }
    }

    private fun storageButtonLabel(uri: Uri?): String {
        if (uri == null) {
            return getString(R.string.storage_select)
        }
        val name = DocumentFile.fromTreeUri(this, uri)?.name
        return if (name.isNullOrBlank()) {
            getString(R.string.storage_selected)
        } else {
            getString(R.string.storage_selected_format, name)
        }
    }

    private companion object {
        // Use the preview cadence for initial tick micros.
        const val DEFAULT_TICK_MICROS = 33_333L
    }
}
