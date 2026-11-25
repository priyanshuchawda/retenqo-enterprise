package com.mneme.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mneme.app.ai.LlmBridge
import com.mneme.app.ai.LocalModelManager
import com.mneme.app.ai.MemoryAsk
import com.mneme.app.ai.MnemeEmbedder
import com.mneme.app.data.MnemeDatabase
import com.mneme.app.data.NoteRepository
import com.mneme.app.media.MediaIngestPipeline
import com.mneme.app.pulse.ProjectionService
import com.mneme.app.pulse.PulsePipeline
import com.mneme.app.pulse.ScreenCapture
import com.mneme.app.ui.theme.MnemeTheme
import com.mneme.app.ui.timeline.MnemeHomeScreen
import com.mneme.app.ui.timeline.MnemeViewModel
import com.mneme.app.voice.AlarmScheduler
import com.mneme.app.voice.TtsSpeaker
import com.mneme.app.voice.WhisperBridge
import com.mneme.app.widget.RetenqoWidgetProvider
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val llm by lazy { LlmBridge(this) }
    private val embedder by lazy { MnemeEmbedder(this, llm) }
    private val notes by lazy {
        val db = MnemeDatabase.get(this)
        NoteRepository(db.notes(), embedder, db.followThrough())
    }
    private val whisper by lazy { WhisperBridge(this) }
    private val tts by lazy { TtsSpeaker(this) }
    private val alarms by lazy { AlarmScheduler(this) }
    private val pulse by lazy { PulsePipeline(this, notes, llm) }
    private val media by lazy { MediaIngestPipeline(this, pulse) }
    private val asker by lazy { MemoryAsk(notes, llm) }
    private val capture by lazy { ScreenCapture(this) }

    private val viewModel: MnemeViewModel by viewModels {
        MnemeViewModel.factory(
            application,
            notes,
            pulse,
            media,
            asker,
            llm,
            whisper,
            tts,
            alarms,
        )
    }

    private val micPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.refreshSetup()
            if (granted) {
                Toast.makeText(this, "Microphone ready for voice Ask", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Microphone permission needed for voice Ask", Toast.LENGTH_SHORT).show()
            }
        }

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.refreshSetup()
        }

    private val mediaPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            viewModel.refreshSetup()
            if (grants.values.none { it }) {
                Toast.makeText(
                    this,
                    "Photos permission helps Scan WhatsApp media",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

    private val modelImportLauncher = registerForActivityResult(OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            try {
                llm.unload()
                val imported = LocalModelManager(this@MainActivity).import(uri)
                viewModel.refreshModelStatus()
                val sizeMb = imported.sizeBytes / (1024L * 1024L)
                Toast.makeText(
                    this@MainActivity,
                    "Imported ${imported.displayName} · ${sizeMb} MB · SHA-256 ${imported.sha256.take(12)}…",
                    Toast.LENGTH_LONG,
                ).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Model import failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val backupExportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.retenqo.backup")) { uri ->
        val password = viewModel.takePendingBackupExportPassword()
        if (uri != null && password != null) {
            viewModel.exportBackup(uri, password)
        } else {
            password?.fill('\u0000')
        }
    }

    private val backupImportLauncher = registerForActivityResult(OpenDocument()) { uri ->
        if (uri != null) promptBackupRestorePassword(uri)
    }

    private val mediaImportLauncher = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.ingestSelectedMedia(uris)
    }

    private val projectionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK || result.data == null) {
                Toast.makeText(this, "Screen capture permission needed for Pulse", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            val data = result.data!!
            lifecycleScope.launch {
                try {
                    startForegroundService(Intent(this@MainActivity, ProjectionService::class.java))
                    // Hide Retenqo so MediaProjection mirrors the previous app (e.g. WhatsApp).
                    moveTaskToBack(true)
                    kotlinx.coroutines.delay(450)
                    val bitmap = capture.capture(result.resultCode, data)
                    // Return to Retenqo for OCR / save status.
                    startActivity(
                        Intent(this@MainActivity, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    )
                    viewModel.onPulseBitmap(bitmap)
                } catch (e: Exception) {
                    startActivity(
                        Intent(this@MainActivity, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    )
                    Toast.makeText(this@MainActivity, "Pulse failed: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    stopService(Intent(this@MainActivity, ProjectionService::class.java))
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MnemeTheme {
                val ui by viewModel.ui.collectAsStateWithLifecycle()
                MnemeHomeScreen(
                    viewModel = viewModel,
                    onPulse = {
                        if (ui.pulsing) return@MnemeHomeScreen
                        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        projectionLauncher.launch(mgr.createScreenCaptureIntent())
                    },
                    onRequestMic = {
                        when {
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.RECORD_AUDIO,
                            ) == PackageManager.PERMISSION_GRANTED -> viewModel.startListening()
                            else -> micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onRequestPostNotifications = ::requestPostNotifications,
                    onRequestMedia = ::requestMediaPermission,
                    onRequestMicPermission = ::requestMicPermissionOnly,
                    onImportLocalModel = { modelImportLauncher.launch(arrayOf("*/*")) },
                    onExportBackup = ::promptBackupExportPassword,
                    onRestoreBackup = { backupImportLauncher.launch(arrayOf("application/vnd.retenqo.backup", "application/json", "text/json", "application/octet-stream")) },
                    onImportMedia = { mediaImportLauncher.launch(arrayOf("image/*", "application/pdf")) },
                )
            }
        }
        applyPitchFromIntent(intent)
        consumeShareIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyPitchFromIntent(intent)
        consumeShareIntent(intent)
    }

    private fun consumeShareIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return

        val uris = linkedSetOf<android.net.Uri>()
        intent.clipData?.let { clip ->
            for (index in 0 until clip.itemCount) clip.getItemAt(index).uri?.let(uris::add)
        }
        if (action == Intent.ACTION_SEND_MULTIPLE) {
            intent.getParcelableArrayListExtra<android.net.Uri>(Intent.EXTRA_STREAM)?.forEach(uris::add)
        } else {
            intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)?.let(uris::add)
        }
        if (uris.isNotEmpty()) viewModel.ingestSelectedMedia(uris.toList())
        val sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        if (sharedText != null && uris.isEmpty()) viewModel.ingestSharedText(sharedText)
        // Prevent this payload from being consumed again after activity recreation.
        intent.action = null
    }

    private fun applyPitchFromIntent(intent: Intent?) {
        val shot = intent?.getStringExtra("pitch") ?: return
        if (shot.isBlank()) return
        viewModel.applyPitchShot(shot)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSetup()
        viewModel.refreshModelStatus()
        RetenqoWidgetProvider.updateAll(this)
    }

    private fun requestPostNotifications() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.refreshSetup()
            return
        }
        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun promptBackupExportPassword() {
        val password = newPasswordField("Backup password · at least 12 characters")
        val confirmation = newPasswordField("Confirm password")
        val content = passwordDialogContent(password, confirmation)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Encrypt backup")
            .setMessage("Retenqo will not save this password. You will need it to restore the backup.")
            .setView(content)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Choose location", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val first = password.text.toString()
                val second = confirmation.text.toString()
                if (first.length < 12 || first != second) {
                    Toast.makeText(
                        this,
                        if (first.length < 12) "Use at least 12 characters" else "Passwords do not match",
                        Toast.LENGTH_SHORT,
                    ).show()
                    return@setOnClickListener
                }
                viewModel.setPendingBackupExportPassword(first.toCharArray())
                dialog.dismiss()
                backupExportLauncher.launch("Retenqo-backup-${System.currentTimeMillis()}.retenqo")
            }
        }
        dialog.setOnDismissListener {
            password.text?.clear()
            confirmation.text?.clear()
        }
        dialog.show()
    }

    private fun promptBackupRestorePassword(uri: Uri) {
        val password = newPasswordField("Backup password (leave blank for older JSON backups)")
        val dialog = AlertDialog.Builder(this)
            .setTitle("Restore backup")
            .setMessage("Enter the password used when this backup was exported. Retenqo does not store it.")
            .setView(passwordDialogContent(password))
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Restore", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val secret = password.text.toString().toCharArray()
                password.text?.clear()
                dialog.dismiss()
                viewModel.restoreBackup(uri, secret)
            }
        }
        dialog.setOnDismissListener { password.text?.clear() }
        dialog.show()
    }

    private fun newPasswordField(hintText: String): EditText = EditText(this).apply {
        hint = hintText
        contentDescription = hintText
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        maxLines = 1
    }

    private fun passwordDialogContent(vararg fields: EditText): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        val margin = (20 * resources.displayMetrics.density).toInt()
        setPadding(margin, 0, margin, 0)
        fields.forEachIndexed { index, field ->
            addView(
                field,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    if (index > 0) topMargin = (8 * resources.displayMetrics.density).toInt()
                },
            )
        }
    }

    private fun requestMediaPermission() {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= 34) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            )
        } else {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        }
        if (permissions.any {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }
        ) {
            viewModel.refreshSetup()
            return
        }
        mediaPermissionLauncher.launch(permissions)
    }

    private fun requestMicPermissionOnly() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Microphone already allowed", Toast.LENGTH_SHORT).show()
            viewModel.refreshSetup()
            return
        }
        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
}
