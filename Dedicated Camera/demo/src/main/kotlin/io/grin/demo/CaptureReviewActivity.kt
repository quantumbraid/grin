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
import io.grin.demo.databinding.ActivityCaptureReviewBinding

// Displays the frozen posterized frame and actions to accept or retake.
class CaptureReviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCaptureReviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCaptureReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val frame = CaptureBuffer.frame
        if (frame == null) {
            Toast.makeText(this, getString(R.string.capture_no_frame), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.previewImage.setImageBitmap(frame.bitmap)
        binding.gridOverlay.updateGrid(
            frame.gridCols,
            frame.gridRows,
            frame.paletteIndices,
            frame.paletteLabels
        )
        binding.captureMetadata.text = getString(
            R.string.capture_metadata_format,
            frame.gridCols,
            frame.gridRows,
            frame.paletteSize
        )

        binding.retakeButton.setOnClickListener {
            // Return to the camera preview without saving.
            finish()
        }
        binding.doneButton.setOnClickListener {
            // Clear the cached frame and return to the camera preview.
            CaptureBuffer.frame = null
            finish()
        }
    }
}
