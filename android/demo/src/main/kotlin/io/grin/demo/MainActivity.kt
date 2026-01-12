package io.grin.demo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loadButton.setOnClickListener {
            filePicker.launch(arrayOf("application/octet-stream"))
        }
        binding.playButton.setOnClickListener { binding.grinView.play() }
        binding.pauseButton.setOnClickListener { binding.grinView.pause() }
        binding.stopButton.setOnClickListener { binding.grinView.stop() }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.grinView.seek(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

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
}
