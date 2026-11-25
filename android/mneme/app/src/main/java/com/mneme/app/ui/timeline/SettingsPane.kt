package com.mneme.app.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.mneme.app.settings.AppSetupStatus
import com.mneme.app.notif.NotificationCaptureSettings

@Composable
fun SettingsPane(
    setup: AppSetupStatus,
    mediaBusy: Boolean,
    localModelReady: Boolean,
    modelDownloading: Boolean,
    modelDownloadProgress: Int,
    modelDownloadStatus: String,
    onDownloadRecommendedModel: () -> Unit,
    onCancelModelDownload: () -> Unit,
    onImportLocalModel: () -> Unit,
    notificationSources: Map<String, Boolean>,
    onToggleNotificationSource: (String, Boolean) -> Unit,
    backgroundMediaScan: Boolean,
    onToggleBackgroundMediaScan: (Boolean) -> Unit,
    showBanner: Boolean,
    onDismissBanner: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onOpenNotifListener: () -> Unit,
    onRequestMedia: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onRequestMic: () -> Unit,
    onOpenExactAlarm: () -> Unit,
    onScanWhatsApp: () -> Unit,
    onImportMedia: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onDeleteAllPersonalData: () -> Unit,
) {
    var modelOpen by remember { mutableStateOf(true) }
    var backupOpen by remember { mutableStateOf(false) }
    var captureOpen by remember { mutableStateOf(false) }
    var permissionsOpen by remember { mutableStateOf(false) }
    var toolsOpen by remember { mutableStateOf(false) }
    val enabledCaptureSources = notificationSources.values.count { it }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SettingsGroup(
            title = "On-device AI",
            summary = if (localModelReady) "Model ready on this phone" else "Set up a local model for offline answers",
            expanded = modelOpen,
            onToggle = { modelOpen = !modelOpen },
        ) {
            Text("Inference stays on this phone. Internet is used only to download model files.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SetupRow(
                title = "Recommended model · 563 MB",
                subtitle = modelDownloadStatus.ifBlank { "Qwen3.5 0.8B · Q4_0" },
                ok = localModelReady,
                actionLabel = when {
                    modelDownloading -> "Cancel · $modelDownloadProgress%"
                    localModelReady -> "Download again"
                    else -> "Download"
                },
                onAction = if (modelDownloading) onCancelModelDownload else onDownloadRecommendedModel,
            )
            SetupRow(
                title = "Import another model",
                subtitle = if (localModelReady) "Replace the installed GGUF model" else "Choose a compatible GGUF file",
                ok = localModelReady,
                actionLabel = "Choose",
                onAction = onImportLocalModel,
                showStatus = false,
            )
        }

        SettingsGroup(
            title = "Backups",
            summary = "Export encrypted backups or restore older JSON backups",
            expanded = backupOpen,
            onToggle = { backupOpen = !backupOpen },
        ) {
            Text("New backups are encrypted with a password you choose. Keep it safe: Retenqo cannot recover it. Older unencrypted JSON backups remain restorable. Model files are not included.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SetupRow("Export backup", "Save a copy to a location you choose", false, "Export", onExportBackup, showStatus = false)
            SetupRow("Restore backup", "Merge with the data on this phone", false, "Choose file", onRestoreBackup, showStatus = false)
            var confirmDelete by remember { mutableStateOf(false) }
            SetupRow("Delete all personal data", "Remove memories, pending captures, and temporary files", false, "Delete", { confirmDelete = true }, showStatus = false)
            if (confirmDelete) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { confirmDelete = false },
                    title = { Text("Delete all personal data?") },
                    text = { Text("This permanently removes saved memories and clears pending capture data on this phone.") },
                    confirmButton = {
                        TextButton(onClick = { confirmDelete = false; onDeleteAllPersonalData() }) { Text("Delete all") }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
                    },
                )
            }
        }

        SettingsGroup(
            title = "Capture sources",
            summary = if (enabledCaptureSources == 0) {
                "Paused · choose which apps may contribute notification text"
            } else {
                "$enabledCaptureSources of ${NotificationCaptureSettings.sources.size} apps enabled"
            },
            expanded = captureOpen,
            onToggle = { captureOpen = !captureOpen },
        ) {
            NotificationCaptureSettings.sources.forEach { source ->
                val enabled = notificationSources[source.id] == true
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(source.label, style = MaterialTheme.typography.titleMedium)
                        Text(if (enabled) "Capturing notifications" else "Paused", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = enabled, onCheckedChange = { onToggleNotificationSource(source.id, it) })
                }
            }
            SetupRow(
                title = "Background WhatsApp scan",
                subtitle = "Checks recent WhatsApp photos every 30 minutes",
                ok = backgroundMediaScan,
                actionLabel = if (backgroundMediaScan) "Pause" else "Enable",
                onAction = { onToggleBackgroundMediaScan(!backgroundMediaScan) },
            )
        }

        SettingsGroup(
            title = "Permissions",
            summary = if (setup.readyForDailyUse) "Your chosen access and alerts" else "Optional access for capture, voice, and reminders",
            expanded = permissionsOpen,
            onToggle = { permissionsOpen = !permissionsOpen },
        ) {
            if (showBanner && setup.missingCritical) {
                Text("Notification access and alerts help Retenqo capture useful updates and send reminders.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onDismissBanner) { Text("Got it") }
            }
            SetupRow("Notification access", "Read notifications from apps you allow", setup.notifListener, if (setup.notifListener) "Manage" else "Enable", onOpenNotifListener)
            SetupRow("Due reminders", "Show reminders for tasks", setup.postNotifications, if (setup.postNotifications) "On" else "Allow", onRequestPostNotifications, enabled = !setup.postNotifications)
            SetupRow("Photo access", "Needed only for background WhatsApp photo scans", setup.canReadMedia, if (setup.canReadMedia) "On" else "Allow", onRequestMedia, enabled = !setup.canReadMedia)
            SetupRow("Exact alarms", "More precise due reminders", setup.exactAlarms, if (setup.exactAlarms) "Manage" else "Allow", onOpenExactAlarm)
            SetupRow("Microphone", "Optional voice input", setup.microphone, if (setup.microphone) "On" else "Allow", onRequestMic, enabled = !setup.microphone)
            SetupRow("App context", "Optional screen understanding", setup.accessibilityService, if (setup.accessibilityService) "Manage" else "Enable", onOpenAccessibility)
        }

        SettingsGroup(
            title = "Import and tools",
            summary = "Choose files or open app settings",
            expanded = toolsOpen,
            onToggle = { toolsOpen = !toolsOpen },
        ) {
            SetupRow("Import photos or PDFs", "OCR selected files on this phone", false, if (mediaBusy) "Importing…" else "Choose", onImportMedia, enabled = !mediaBusy, showStatus = false)
            SetupRow("Scan WhatsApp photos", "Read recent photos available in MediaStore", false, if (mediaBusy) "Scanning…" else "Scan", onScanWhatsApp, enabled = !mediaBusy, showStatus = false)
            SetupRow("App system settings", "Battery and notification controls", false, "Open", onOpenAppDetails, showStatus = false)
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    summary: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .28f), RoundedCornerShape(18.dp))
            .animateContentSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics {
                    stateDescription = if (expanded) "Expanded" else "Collapsed"
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(if (expanded) "−" else "+", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        }
        if (expanded) {
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = .25f)))
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) { content() }
        }
    }
}

@Composable
private fun SetupRow(
    title: String,
    subtitle: String,
    ok: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    enabled: Boolean = true,
    showStatus: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                RoundedCornerShape(18.dp),
            )
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .35f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            if (showStatus) {
                Text(
                    text = if (ok) "Ready" else "Needed",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                )
            }
            TextButton(
                onClick = onAction,
                enabled = enabled,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
