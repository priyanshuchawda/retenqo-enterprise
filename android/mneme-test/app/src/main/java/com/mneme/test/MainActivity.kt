package com.mneme.test

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var logText: TextView
    private lateinit var runAllButton: MaterialButton
    private val runner by lazy { ModelTestRunner(applicationContext) }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            maybeRequestAllFilesAccess()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        logText = findViewById(R.id.logText)
        runAllButton = findViewById(R.id.runAllButton)

        runAllButton.setOnClickListener {
            if (!hasStorageAccess()) {
                requestStorageAccess()
                return@setOnClickListener
            }
            startTests()
        }

        if (!hasStorageAccess()) {
            requestStorageAccess()
        } else if (intent.getBooleanExtra("run_tests", false)) {
            window.decorView.post { startTests() }
        }
    }

    private fun hasStorageAccess(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager()
        }
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                statusText.setText(R.string.status_idle)
                appendLog("Grant “All files access” so models under /sdcard/Mneme can be read.")
                startActivity(
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                )
            }
            return
        }
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_MEDIA_AUDIO,
            )
        )
    }

    private fun maybeRequestAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            requestStorageAccess()
        }
    }

    private fun startTests() {
        runAllButton.isEnabled = false
        statusText.text = "Running tests…"
        logText.text = ""

        lifecycleScope.launch {
            try {
                runner.runAll { line ->
                    withContext(Dispatchers.Main) { appendLog(line) }
                }
                statusText.text = "Tests finished"
            } catch (e: Exception) {
                appendLog("FATAL: ${e.message}")
                statusText.text = "Tests failed"
            } finally {
                runAllButton.isEnabled = true
            }
        }
    }

    private fun appendLog(line: String) {
        logText.append(line)
        logText.append("\n")
    }
}
