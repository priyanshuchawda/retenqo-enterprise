package com.mneme.app.ui.timeline

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mneme.app.data.CardStatus
import com.mneme.app.data.CardReviewState
import com.mneme.app.data.CardType
import com.mneme.app.data.CommitmentHistoryEvent
import com.mneme.app.data.NoteEntity
import com.mneme.app.data.FollowThroughSuggestion
import java.text.DateFormat
import java.util.Date
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MnemeHomeScreen(
    viewModel: MnemeViewModel,
    onPulse: () -> Unit,
    onRequestMic: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onRequestMedia: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onImportLocalModel: () -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onImportMedia: () -> Unit,
) {
    val notes by viewModel.timeline.collectAsStateWithLifecycle()
    val followThroughSuggestions by viewModel.followThroughSuggestions.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val shown = ui.pinnedNotes ?: ui.searchResults ?: notes
    val snackbarHostState = remember { SnackbarHostState() }
    var previousStatus by remember { mutableStateOf(ui.status) }
    LaunchedEffect(ui.status, ui.askBusy, ui.pulsing, ui.mediaBusy) {
        if (!ui.askBusy && !ui.pulsing && !ui.mediaBusy && ui.status != previousStatus && ui.status.isNotBlank()) {
            snackbarHostState.showSnackbar(ui.status)
        }
        previousStatus = ui.status
    }
    if (ui.pitchOverlay == PitchOverlay.Messy) {
        PitchMessyScreen()
        return
    }
    if (ui.pitchOverlay == PitchOverlay.Gallery) {
        PitchGalleryScreen()
        return
    }
    if (ui.pitchOverlay == PitchOverlay.Action) {
        var done by remember { mutableStateOf(false) }
        PitchActionScreen(done = done, onDone = { done = true })
        return
    }
    if (ui.pitchOverlay == PitchOverlay.Live) {
        PitchLiveFlowScreen()
        return
    }
    if (ui.pitchOverlay == PitchOverlay.Meeting) {
        PitchMeetingScreen()
        return
    }
    if (ui.pitchOverlay == PitchOverlay.Brief) {
        PitchDailyBriefScreen()
        return
    }
    if (ui.pitchOverlay == PitchOverlay.Chat) {
        PitchAssistantChatScreen()
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (ui.tab == MnemeTab.Memory && ui.pinnedNotes == null && ui.pitchOverlay == PitchOverlay.None) {
                val fabScale by animateFloatAsState(
                    targetValue = if (ui.pulsing) 0.92f else 1f,
                    animationSpec = tween(180),
                    label = "pulse-scale",
                )
                ExtendedFloatingActionButton(
                    onClick = onPulse,
                    modifier = Modifier.graphicsLayer {
                        scaleX = fabScale
                        scaleY = fabScale
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.RadioButtonChecked,
                            contentDescription = null,
                        )
                    },
                    text = { Text(if (ui.pulsing) "Reading…" else "Capture screen") },
                )
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp)),
                containerColor = Color(0xFF111A17),
                tonalElevation = 0.dp,
            ) {
                NavigationBarItem(
                    selected = ui.tab == MnemeTab.Memory,
                    onClick = { viewModel.setTab(MnemeTab.Memory) },
                    icon = { Icon(Icons.Outlined.AutoStories, contentDescription = null) },
                    label = { Text("Memory") },
                    colors = navItemColors(),
                )
                NavigationBarItem(
                    selected = ui.tab == MnemeTab.Tasks,
                    onClick = { viewModel.setTab(MnemeTab.Tasks) },
                    icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                    label = { Text("Tasks") },
                    colors = navItemColors(),
                )
                NavigationBarItem(
                    selected = ui.tab == MnemeTab.Ask,
                    onClick = { viewModel.setTab(MnemeTab.Ask) },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
                    label = { Text("Ask") },
                    colors = navItemColors(),
                )
                NavigationBarItem(
                    selected = ui.tab == MnemeTab.Settings,
                    onClick = { viewModel.setTab(MnemeTab.Settings) },
                    icon = {
                        Icon(
                            if (ui.tab == MnemeTab.Settings) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = null,
                        )
                    },
                    label = { Text("Settings") },
                    colors = navItemColors(),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .statusBarsPadding()
                .padding(horizontal = 18.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("retenqo", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text("ON DEVICE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = when (ui.tab) {
                    MnemeTab.Ask -> "Ask"
                    MnemeTab.Tasks -> "Tasks"
                    MnemeTab.Settings -> "Settings"
                    MnemeTab.Memory -> "Memory"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(12.dp))

            when (ui.tab) {
                MnemeTab.Memory -> MemoryPane(
                    shown = shown,
                    query = ui.searchQuery,
                    onQuery = viewModel::setSearch,
                    onDelete = viewModel::deleteNote,
                    onDone = viewModel::markDone,
                    onSnooze = viewModel::snoozeOneDay,
                    onReopen = viewModel::reopenCard,
                    onAcceptCapturedCard = viewModel::acceptCapturedCard,
                    onDismissCapturedCard = viewModel::dismissCapturedCard,
                    showBrief = ui.showBrief,
                    briefHeadline = ui.briefHeadline,
                    onDismissBrief = viewModel::dismissBrief,
                    onShowBrief = viewModel::showBrief,
                    hideSearch = ui.pinnedNotes != null,
                    lead = {
                        when (ui.pitchOverlay) {
                            PitchOverlay.Pulse -> PitchPulseBody()
                            PitchOverlay.Focus -> PitchFocusBody()
                            else -> {}
                        }
                    },
                )
                MnemeTab.Ask -> AskPane(
                    query = ui.askQuery,
                    busy = ui.askBusy,
                    listening = ui.listening,
                    answer = ui.askAnswer,
                    citations = ui.askCitations,
                    useScreenContext = ui.useScreenContext,
                    screenContextSource = ui.screenContextSource,
                    onQuery = viewModel::setAskQuery,
                    onAsk = viewModel::submitAsk,
                    onToggleScreenContext = viewModel::toggleScreenContext,
                    onMicDown = onRequestMic,
                    onMicUp = { viewModel.stopListening(submit = true) },
                )
                MnemeTab.Tasks -> TaskPane(
                    notes = notes,
                    suggestions = followThroughSuggestions,
                    onDelete = viewModel::deleteNote,
                    onDone = viewModel::markDone,
                    onSnooze = viewModel::snoozeOneDay,
                    onReopen = viewModel::reopenCard,
                    onAcceptCapturedCard = viewModel::acceptCapturedCard,
                    onDismissCapturedCard = viewModel::dismissCapturedCard,
                    commitments = notes,
                    onAcceptSuggestion = viewModel::acceptCompletionSuggestion,
                    onDismissSuggestion = viewModel::dismissCompletionSuggestion,
                    onSaveTask = viewModel::saveTaskEdits,
                    taskHistory = ui.taskHistory,
                    onLoadTaskHistory = viewModel::loadTaskHistory,
                )
                MnemeTab.Settings -> SettingsPane(
                    setup = ui.setup,
                    mediaBusy = ui.mediaBusy,
                    localModelReady = ui.localModelReady,
                    modelDownloading = ui.modelDownloading,
                    modelDownloadProgress = ui.modelDownloadProgress,
                    modelDownloadStatus = ui.modelDownloadStatus,
                    onDownloadRecommendedModel = viewModel::downloadRecommendedModel,
                    onCancelModelDownload = viewModel::cancelModelDownload,
                    onImportLocalModel = onImportLocalModel,
                    backgroundMediaScan = ui.backgroundMediaScan,
                    notificationSources = ui.notificationSources,
                    onToggleNotificationSource = viewModel::setNotificationSourceEnabled,
                    onToggleBackgroundMediaScan = viewModel::setBackgroundMediaScan,
                    showBanner = ui.showSetupBanner,
                    onDismissBanner = viewModel::dismissSetupBanner,
                    onRequestPostNotifications = onRequestPostNotifications,
                    onOpenNotifListener = viewModel::openNotifAccessSettings,
                    onRequestMedia = onRequestMedia,
                    onOpenAccessibility = viewModel::openAccessibilitySettings,
                    onRequestMic = onRequestMicPermission,
                    onOpenExactAlarm = viewModel::openExactAlarmSettings,
                    onScanWhatsApp = viewModel::scanWhatsAppMedia,
                    onImportMedia = onImportMedia,
                    onOpenAppDetails = viewModel::openAppDetails,
                    onExportBackup = onExportBackup,
                    onRestoreBackup = onRestoreBackup,
                )
            }
        }
    }
}

@Composable
private fun MemoryPane(
    shown: List<NoteEntity>,
    query: String,
    onQuery: (String) -> Unit,
    onDelete: (Long) -> Unit,
    onDone: (Long) -> Unit,
    onSnooze: (Long) -> Unit,
    onReopen: (Long) -> Unit,
    onAcceptCapturedCard: (Long) -> Unit,
    onDismissCapturedCard: (Long) -> Unit,
    showBrief: Boolean,
    briefHeadline: String,
    onDismissBrief: () -> Unit,
    onShowBrief: () -> Unit,
    hideSearch: Boolean = false,
    lead: @Composable () -> Unit = {},
) {
    lead()
    if (!hideSearch) {
        TextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(text = "Search memory") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            },
            shape = RoundedCornerShape(16.dp),
            colors = fieldColors(),
        )
        Spacer(modifier = Modifier.height(12.dp))
    } else {
        Spacer(modifier = Modifier.height(8.dp))
    }

    if (showBrief) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF10261F), RoundedCornerShape(18.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .18f), RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("New since last open", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(briefHeadline, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = onDismissBrief, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Dismiss") }
        }
        Spacer(modifier = Modifier.height(14.dp))
    }

    if (shown.isEmpty()) {
        EmptyMemory(
            modifier = Modifier.fillMaxWidth().padding(vertical = 52.dp),
            title = if (showBrief) "No saved cards yet" else "Nothing saved yet",
            body = if (showBrief) "New activity is shown above. Save a note or use Pulse to build memory." else "Tap Pulse on anything worth remembering.",
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 148.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(if (hideSearch) "Due now" else "Your memories", style = MaterialTheme.typography.titleMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${shown.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!hideSearch) {
                            TextButton(onClick = onShowBrief, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                Text("Today")
                            }
                        }
                    }
                }
            }
            items(shown, key = { it.id }) { note ->
                NoteRow(
                    note = note,
                    showActions = !hideSearch,
                    onDelete = { onDelete(note.id) },
                    onDone = { onDone(note.id) },
                    onSnooze = { onSnooze(note.id) },
                    onReopen = { onReopen(note.id) },
                    onAcceptReview = { onAcceptCapturedCard(note.id) },
                    onDismissReview = { onDismissCapturedCard(note.id) },
                )
            }
        }
    }
}

private enum class TaskListFilter(val title: String) {
    REVIEW("Review"),
    ACTIVE("Active"),
    DUE("Due soon"),
    DONE("Done"),
}

@Composable
private fun TaskPane(
    notes: List<NoteEntity>,
    suggestions: List<FollowThroughSuggestion>,
    onDelete: (Long) -> Unit,
    onDone: (Long) -> Unit,
    onSnooze: (Long) -> Unit,
    onReopen: (Long) -> Unit,
    onAcceptCapturedCard: (Long) -> Unit,
    onDismissCapturedCard: (Long) -> Unit,
    commitments: List<NoteEntity>,
    onAcceptSuggestion: (FollowThroughSuggestion) -> Unit,
    onDismissSuggestion: (Long) -> Unit,
    onSaveTask: (Long, String, String, String?, Long?, CardType) -> Unit,
    taskHistory: List<CommitmentHistoryEvent>,
    onLoadTaskHistory: (Long) -> Unit,
) {
    val now = System.currentTimeMillis()
    val dueWindow = now + 24L * 60L * 60L * 1000L
    val taskTypes = setOf(CardType.DUE, CardType.PROMISE, CardType.WAITING, CardType.ASK_FOR_YOU)
    val taskCards = notes.filter { it.type() in taskTypes }
    val reviewCards = taskCards.filter { it.review() == CardReviewState.PENDING }
    val confirmed = taskCards.filter { it.review() == CardReviewState.CONFIRMED }
    val openCards = confirmed.filter { it.status() == CardStatus.OPEN || it.status() == CardStatus.SNOOZED }
        .sortedWith(compareBy<NoteEntity> { it.dueAt ?: Long.MAX_VALUE }.thenByDescending { it.createdAt })
    val dueCards = openCards.filter { it.isActive(now) && it.dueAt != null && it.dueAt <= dueWindow }
    val doneCards = confirmed.filter { it.status() == CardStatus.DONE }
        .sortedByDescending { it.statusChangedAt ?: it.createdAt }
    val reviewCount = reviewCards.size + suggestions.size
    val counts = mapOf(
        TaskListFilter.REVIEW to reviewCount,
        TaskListFilter.ACTIVE to openCards.size,
        TaskListFilter.DUE to dueCards.size,
        TaskListFilter.DONE to doneCards.size,
    )
    var selectedFilter by remember { mutableStateOf(TaskListFilter.ACTIVE) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var historyNote by remember { mutableStateOf<NoteEntity?>(null) }
    val filtered = when (selectedFilter) {
        TaskListFilter.REVIEW -> reviewCards
        TaskListFilter.ACTIVE -> openCards
        TaskListFilter.DUE -> dueCards
        TaskListFilter.DONE -> doneCards
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (reviewCount > 0 || openCards.isNotEmpty()) {
            Text(
                "$reviewCount to review · ${openCards.size} active",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TaskListFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text("${filter.title} · ${counts[filter] ?: 0}") },
                )
            }
        }
        if (selectedFilter == TaskListFilter.DUE) {
            Text(
                "Overdue and due in the next 24 hours",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 148.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (selectedFilter == TaskListFilter.REVIEW && suggestions.isNotEmpty()) {
                item(key = "suggestion-heading") {
                    Text("Possible completions", style = MaterialTheme.typography.titleLarge)
                }
                items(suggestions, key = { "suggestion-${it.id}" }) { suggestion ->
                    val commitment = commitments.firstOrNull { it.id == suggestion.commitmentId }
                    FollowThroughSuggestionCard(
                        suggestion = suggestion,
                        commitmentTitle = commitment?.title?.removePrefix("Task:")?.trim() ?: "Saved commitment",
                        onConfirm = { onAcceptSuggestion(suggestion) },
                        onDismiss = { onDismissSuggestion(suggestion.id) },
                    )
                }
            }
            if (filtered.isEmpty() && !(selectedFilter == TaskListFilter.REVIEW && suggestions.isNotEmpty())) {
                item(key = "empty-${selectedFilter.name}") {
                    EmptyMemory(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                        title = when (selectedFilter) {
                            TaskListFilter.REVIEW -> "You’re all caught up"
                            TaskListFilter.ACTIVE -> "No open commitments"
                            TaskListFilter.DUE -> "Nothing due soon"
                            TaskListFilter.DONE -> "No completed tasks yet"
                        },
                        body = when (selectedFilter) {
                            TaskListFilter.REVIEW -> "New commitments and completion suggestions will wait here for your decision."
                            TaskListFilter.ACTIVE -> "Accept a reviewed commitment or ask Retenqo to remember a task."
                            TaskListFilter.DUE -> "Tasks with a due date in the next 24 hours will show here."
                            TaskListFilter.DONE -> "When you complete a commitment, it will be kept here."
                        },
                    )
                }
            }
            items(filtered, key = { "task-${it.id}" }) { note ->
                NoteRow(
                    note = note,
                    showActions = true,
                    showDelete = false,
                    showEdit = true,
                    onEdit = { editingNote = note },
                    showHistory = true,
                    onHistory = {
                        historyNote = note
                        onLoadTaskHistory(note.id)
                    },
                    onDelete = { onDelete(note.id) },
                    onDone = { onDone(note.id) },
                    onSnooze = { onSnooze(note.id) },
                    onReopen = { onReopen(note.id) },
                    onAcceptReview = { onAcceptCapturedCard(note.id) },
                    onDismissReview = { onDismissCapturedCard(note.id) },
                )
            }
        }
        editingNote?.let { note ->
            TaskEditDialog(
                note = note,
                onDismiss = { editingNote = null },
                onSave = { title, body, who, dueAt, type ->
                    onSaveTask(note.id, title, body, who, dueAt, type)
                    editingNote = null
                },
            )
        }
        historyNote?.let { note ->
            TaskHistoryDialog(
                note = note,
                events = taskHistory,
                onDismiss = { historyNote = null },
            )
        }
    }
}

@Composable
private fun TaskHistoryDialog(
    note: NoteEntity,
    events: List<CommitmentHistoryEvent>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Commitment history") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    note.title.removePrefix("Task:").trim(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (events.isEmpty()) {
                    Text("Loading history…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                events.forEach { event ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(historyLabel(event.eventType), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(event.summary, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${event.source} · ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(event.createdAt))}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        event.evidenceText?.takeIf(String::isNotBlank)?.let { evidence ->
                            Text(
                                "“${evidence.take(360)}${if (evidence.length > 360) "…" else ""}”",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

private fun historyLabel(type: String): String = when (type) {
    CommitmentHistoryEvent.Type.CAPTURED -> "Captured"
    CommitmentHistoryEvent.Type.REVIEW_ACCEPTED -> "Accepted as a task"
    CommitmentHistoryEvent.Type.REVIEW_DISMISSED -> "Dismissed from tasks"
    CommitmentHistoryEvent.Type.EDITED -> "Edited"
    CommitmentHistoryEvent.Type.COMPLETED -> "Marked done"
    CommitmentHistoryEvent.Type.REOPENED -> "Reopened"
    CommitmentHistoryEvent.Type.SNOOZED -> "Snoozed"
    CommitmentHistoryEvent.Type.COMPLETION_SUGGESTED -> "Possible completion found"
    CommitmentHistoryEvent.Type.COMPLETION_ACCEPTED -> "Completion confirmed"
    CommitmentHistoryEvent.Type.COMPLETION_DISMISSED -> "Completion evidence dismissed"
    CommitmentHistoryEvent.Type.BASELINE -> "Existing record"
    else -> "Updated"
}

@Composable
private fun TaskEditDialog(
    note: NoteEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String?, Long?, CardType) -> Unit,
) {
    val context = LocalContext.current
    var title by remember(note.id) { mutableStateOf(note.title) }
    var body by remember(note.id) { mutableStateOf(note.body) }
    var who by remember(note.id) { mutableStateOf(note.who.orEmpty()) }
    var dueAt by remember(note.id) { mutableStateOf(note.dueAt) }
    var type by remember(note.id) { mutableStateOf(note.type()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit commitment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Details") },
                    minLines = 2,
                    maxLines = 4,
                )
                OutlinedTextField(
                    value = who,
                    onValueChange = { who = it },
                    label = { Text("Person (optional)") },
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CardType.entries.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = { Text(option.label) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            val calendar = Calendar.getInstance().apply { timeInMillis = dueAt ?: System.currentTimeMillis() }
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    dueAt = Calendar.getInstance().apply {
                                        set(year, month, day, 9, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH),
                            ).show()
                        },
                    ) {
                        Text(dueAt?.let { "Due ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it))}" } ?: "Set due date")
                    }
                    if (dueAt != null) TextButton(onClick = { dueAt = null }) { Text("Clear") }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, body, who, dueAt, type) },
                enabled = title.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun FollowThroughSuggestionCard(
    suggestion: FollowThroughSuggestion,
    commitmentTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF10261F), RoundedCornerShape(18.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .2f), RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Text("Might be complete", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(commitmentTitle, style = MaterialTheme.typography.titleMedium)
        Text(
            "${suggestion.evidenceSource}: “${suggestion.evidenceText}”",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onDismiss) { Text("Dismiss") }
            Button(onClick = onConfirm) { Text("Mark done") }
        }
    }
}

@Composable
private fun AskPane(
    query: String,
    busy: Boolean,
    listening: Boolean,
    answer: String?,
    citations: List<NoteEntity>,
    useScreenContext: Boolean,
    screenContextSource: String?,
    onQuery: (String) -> Unit,
    onAsk: () -> Unit,
    onToggleScreenContext: () -> Unit,
    onMicDown: () -> Unit,
    onMicUp: () -> Unit,
) {
    val micScale by animateFloatAsState(
        targetValue = if (listening) 1.08f else 1f,
        animationSpec = tween(220),
        label = "mic-scale",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(22.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .45f), RoundedCornerShape(22.dp))
            .padding(10.dp),
    ) {
        TextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !busy && !listening,
            placeholder = { Text(text = "Ask your memory anything…") },
            shape = RoundedCornerShape(16.dp),
            colors = composerFieldColors(),
        )
        TextButton(onClick = onToggleScreenContext, enabled = !busy && !listening) {
            Text(
                if (useScreenContext) "Screen on · ${screenContextSource ?: "ready"}" else "Use current screen",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (useScreenContext) {
            Text(
                "Used for your next question only; not saved to Memory.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuickPrompt("What should I follow up on?", onClick = { onQuery("What should I follow up on?") })
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onAsk,
                enabled = !busy && !listening && query.isNotBlank(),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(
                onClick = { if (listening) onMicUp() else onMicDown() },
                enabled = !busy,
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        scaleX = micScale
                        scaleY = micScale
                    }
                    .background(
                        color = if (listening) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    ),
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = if (listening) "Stop recording and send" else "Start voice input",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
    if (listening || busy) {
        Text(
            text = if (listening) "Listening · tap the microphone to send" else "Thinking…",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp),
        )
    }
    Spacer(modifier = Modifier.height(8.dp))

    if (busy) {
        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 148.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!answer.isNullOrBlank()) {
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 4 } + expandVertically(tween(260)),
                    exit = fadeOut(tween(120)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF10261F), RoundedCornerShape(18.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .22f), RoundedCornerShape(18.dp))
                            .padding(16.dp),
                    ) {
                        Text(
                            text = "Answer",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = answer, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        if (citations.isNotEmpty()) {
            item {
                Text(
                    text = "Cited notes",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            items(citations, key = { "c-${it.id}" }) { note ->
                NoteRow(note = note, showActions = false)
            }
        }
    }
}

@Composable
private fun AssistantHero(listening: Boolean) {
    val orbScale by animateFloatAsState(
        targetValue = if (listening) 1.12f else 1f,
        animationSpec = tween(420),
        label = "assistant-orb",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(146.dp)
            .background(
                Brush.linearGradient(listOf(Color(0xFF183E35), Color(0xFF10191A), Color(0xFF10151D))),
                RoundedCornerShape(24.dp),
            )
            .border(1.dp, Color(0xFF4BAE91).copy(alpha = .26f), RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(82.dp)
                .graphicsLayer { scaleX = orbScale; scaleY = orbScale }
                .background(
                    Brush.radialGradient(listOf(Color(0xFFD5FFF0), Color(0xFF55D9B2), Color(0xFF1A5C4B))),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF092019).copy(alpha = .92f), CircleShape),
            )
        }
        Spacer(modifier = Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (listening) "Listening" else "Private memory online",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (listening) "Release the mic when you’re done." else "Ask by text or voice. Answers stay grounded in your data.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB7D6CC),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("LOCAL SEARCH  ·  CITED", style = MaterialTheme.typography.labelMedium, color = Color(0xFF72E0BE))
        }
    }
}

@Composable
private fun MemoryHero(memoryCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(Color(0xFF182A27), Color(0xFF11191A))),
                RoundedCornerShape(22.dp),
            )
            .border(1.dp, Color(0xFF3B6659).copy(alpha = .45f), RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Memory at a glance", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("Your private context, organized.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF9DB9B0))
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(memoryCount.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text("cards", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuickPrompt(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        shape = RoundedCornerShape(50),
        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
)

@Composable
private fun composerFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
)

@Composable
private fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
private fun EmptyMemory(
    modifier: Modifier = Modifier,
    title: String = "Nothing saved yet",
    body: String = "Tap Pulse on anything worth remembering.",
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NoteRow(
    note: NoteEntity,
    showActions: Boolean = true,
    showDelete: Boolean = true,
    showEdit: Boolean = false,
    showHistory: Boolean = false,
    onEdit: () -> Unit = {},
    onHistory: () -> Unit = {},
    onDelete: () -> Unit = {},
    onDone: () -> Unit = {},
    onSnooze: () -> Unit = {},
    onReopen: () -> Unit = {},
    onAcceptReview: () -> Unit = {},
    onDismissReview: () -> Unit = {},
) {
    var menuExpanded by remember(note.id) { mutableStateOf(false) }
    var detailsExpanded by remember(note.id) { mutableStateOf(false) }
    val type = note.type()
    val status = note.status()
    val review = note.review()
    val bodyText = note.body.trim()
    val titleText = note.title.removePrefix("Task:").trim()
    val cleanedBody = bodyText
        .removePrefix(note.source)
        .trimStart(' ', '\n', '·', ':')
        .trim()
    val showBody = cleanedBody.isNotBlank() &&
        cleanedBody != titleText &&
        !cleanedBody.contains(titleText, ignoreCase = true) &&
        !note.title.contains(cleanedBody, ignoreCase = true)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
                shape = RoundedCornerShape(18.dp),
            )
        .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = type.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                    if (status != CardStatus.OPEN) {
                        Text(
                            text = status.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (review == CardReviewState.PENDING || review == CardReviewState.DISMISSED) {
                        Text(
                            text = review.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = titleText,
                        style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(note.source)
                        note.dueAt?.let { due ->
                            append(" · due ")
                            append(
                                DateFormat.getDateInstance(DateFormat.MEDIUM)
                                    .format(Date(due)),
                            )
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showActions) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More actions")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        if (showBody) DropdownMenuItem(
                            text = { Text(if (detailsExpanded) "Hide details" else "Show details") },
                            onClick = { menuExpanded = false; detailsExpanded = !detailsExpanded },
                        )
                        if (showEdit) DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = { menuExpanded = false; onEdit() },
                        )
                        if (showHistory) DropdownMenuItem(
                            text = { Text("History") },
                            onClick = { menuExpanded = false; onHistory() },
                        )
                        if (status != CardStatus.DONE && review != CardReviewState.PENDING) DropdownMenuItem(
                            text = { Text("Snooze for a day") },
                            onClick = { menuExpanded = false; onSnooze() },
                        )
                        if (status == CardStatus.DONE) DropdownMenuItem(
                            text = { Text("Reopen") },
                            onClick = { menuExpanded = false; onReopen() },
                        )
                        if (showDelete) DropdownMenuItem(
                            text = { Text("Delete memory") },
                            onClick = { menuExpanded = false; onDelete() },
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (showBody && (!showActions || detailsExpanded)) {
            Text(
                text = cleanedBody,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (showActions) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                when {
                    review == CardReviewState.PENDING -> {
                        Button(onClick = onAcceptReview) { Text("Add to tasks") }
                        TextButton(onClick = onDismissReview) { Text("Dismiss") }
                    }
                    review == CardReviewState.DISMISSED -> {
                        Text("Saved as memory · not a task", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    status == CardStatus.DONE -> {
                        Text("Completed", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    else -> {
                        TextButton(onClick = onDone) { Text("Mark done") }
                    }
                }
            }
        }
    }
}
