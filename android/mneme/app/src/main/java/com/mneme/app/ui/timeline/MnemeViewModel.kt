package com.mneme.app.ui.timeline

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import android.provider.CalendarContract
import androidx.lifecycle.viewModelScope
import com.mneme.app.ai.AskResult
import com.mneme.app.ai.LlmBridge
import com.mneme.app.ai.LocalModelManager
import com.mneme.app.ai.MemoryAsk
import com.mneme.app.accessibility.RetenqoAccessibilityService
import com.mneme.app.brief.BriefPrefs
import com.mneme.app.brief.CatchUpBriefBuilder
import com.mneme.app.data.IngestSources
import com.mneme.app.data.CardType
import com.mneme.app.data.CommitmentHistoryEvent
import com.mneme.app.data.MnemeBackupManager
import com.mneme.app.data.NoteEntity
import com.mneme.app.data.NoteRepository
import com.mneme.app.data.FollowThroughSuggestion
import com.mneme.app.media.MediaIngestPipeline
import com.mneme.app.media.MediaIngestSettings
import com.mneme.app.media.MnemePowerGates
import com.mneme.app.notif.MnemeNotificationListener
import com.mneme.app.notif.NotificationCaptureSettings
import com.mneme.app.notif.NotifIngestQueue
import com.mneme.app.notif.NotifIngestWorker
import com.mneme.app.pulse.PulsePipeline
import com.mneme.app.pulse.PulseResult
import com.mneme.app.remind.DueReminderScheduler
import com.mneme.app.settings.AppSetup
import com.mneme.app.settings.AppSetupStatus
import com.mneme.app.voice.AlarmScheduler
import com.mneme.app.voice.MicRecorder
import com.mneme.app.voice.TtsSpeaker
import com.mneme.app.voice.VoiceIntent
import com.mneme.app.voice.VoiceIntentRouter
import com.mneme.app.voice.WhisperBridge
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flowOf

enum class MnemeTab { Memory, Tasks, Ask, Settings }

data class MnemeUiState(
    val tab: MnemeTab = MnemeTab.Memory,
    val status: String = "Ready",
    val localModelReady: Boolean = false,
    val modelDownloading: Boolean = false,
    val modelDownloadProgress: Int = 0,
    val modelDownloadStatus: String = "",
    val backgroundMediaScan: Boolean = false,
    val notificationSources: Map<String, Boolean> = emptyMap(),
    val pulsing: Boolean = false,
    val mediaBusy: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<NoteEntity>? = null,
    val askQuery: String = "",
    val askBusy: Boolean = false,
    val askAnswer: String? = null,
    val askCitations: List<NoteEntity> = emptyList(),
    val useScreenContext: Boolean = false,
    val screenContextSource: String? = null,
    val taskHistoryNoteId: Long? = null,
    val taskHistory: List<CommitmentHistoryEvent> = emptyList(),
    val listening: Boolean = false,
    val notifAccess: Boolean = false,
    val setup: AppSetupStatus = AppSetupStatus(
        postNotifications = false,
        notifListener = false,
        mediaImages = false,
        microphone = false,
        exactAlarms = false,
    ),
    val showSetupBanner: Boolean = false,
    /** Non-LLM catch-up panel (since last open). */
    val showBrief: Boolean = false,
    val briefHeadline: String = "",
    val briefBody: String = "",
    /** Frontend-only pitch stills (debug intent extra `pitch`). */
    val pitchOverlay: PitchOverlay = PitchOverlay.None,
    val pinnedNotes: List<NoteEntity>? = null,
)

class MnemeViewModel(
    app: Application,
    private val notes: NoteRepository,
    private val pulse: PulsePipeline,
    private val media: MediaIngestPipeline,
    private val asker: MemoryAsk,
    private val llm: LlmBridge,
    private val whisper: WhisperBridge,
    private val tts: TtsSpeaker,
    private val alarms: AlarmScheduler,
) : AndroidViewModel(app) {
    private var pendingBackupExportPassword: CharArray? = null
    val timeline = notes.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val followThroughSuggestions = (notes.observeFollowThroughSuggestions() ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ui = MutableStateFlow(MnemeUiState())
    val ui: StateFlow<MnemeUiState> = _ui.asStateFlow()

    private val mic = MicRecorder()
    private var pumpJob: Job? = null
    private var modelDownloadJob: Job? = null

    init {
        refreshSetup(openSettingsIfNeeded = true)
        refreshModelStatus()
        refreshBrief(autoShow = true)
    }

    fun refreshModelStatus() {
        _ui.value = _ui.value.copy(localModelReady = llm.isAvailable)
    }

    fun refreshSetup(openSettingsIfNeeded: Boolean = false) {
        if (_ui.value.pitchOverlay != PitchOverlay.None || _ui.value.pinnedNotes != null) {
            return
        }
        val app = getApplication<Application>()
        val setup = AppSetup.snapshot(app)
        val backgroundMediaScan = MediaIngestSettings.isPeriodicEnabled(app)
        val unseen = !AppSetup.hasSeenSetup(app)
        val openSetup = openSettingsIfNeeded && unseen && setup.missingCritical
        _ui.value = _ui.value.copy(
            notifAccess = setup.notifListener,
            setup = setup,
            backgroundMediaScan = backgroundMediaScan,
            notificationSources = NotificationCaptureSettings.sources.associate { source ->
                source.id to (source.id in NotificationCaptureSettings.enabledSources(app))
            },
            showSetupBanner = setup.missingCritical || !setup.readyForDailyUse,
            tab = if (openSetup) MnemeTab.Settings else _ui.value.tab,
            status = when {
                openSetup -> "Finish setup so Retenqo can capture what matters"
                setup.readyForDailyUse -> _ui.value.status.ifBlank { "Ready" }
                else -> _ui.value.status
            },
        )
        if (openSetup) AppSetup.markSetupSeen(app)
    }

    fun setBackgroundMediaScan(enabled: Boolean) {
        val app = getApplication<Application>()
        if (enabled && !AppSetup.snapshot(app).canReadMedia) {
            _ui.value = _ui.value.copy(status = "Allow Photos & media access before enabling background scans")
            return
        }
        MediaIngestSettings.setPeriodicEnabled(app, enabled)
        (app as? com.mneme.app.MnemeApp)?.refreshMediaIngest()
        _ui.value = _ui.value.copy(backgroundMediaScan = enabled)
        _ui.value = _ui.value.copy(
            status = if (enabled) "Background media scan enabled" else "Background media scan paused",
        )
    }

    fun setNotificationSourceEnabled(sourceId: String, enabled: Boolean) {
        val app = getApplication<Application>()
        NotificationCaptureSettings.setEnabled(app, sourceId, enabled)
        if (!enabled) {
            NotifIngestQueue.removeDisabledSources(app)
            if (NotifIngestQueue.size(app) == 0) {
                androidx.work.WorkManager.getInstance(app)
                    .cancelUniqueWork(NotifIngestWorker.UNIQUE)
            }
        }
        _ui.value = _ui.value.copy(
            notificationSources = NotificationCaptureSettings.sources.associate { source ->
                source.id to (source.id in NotificationCaptureSettings.enabledSources(app))
            },
            status = if (enabled) "${NotificationCaptureSettings.sources.firstOrNull { it.id == sourceId }?.label ?: "App"} capture enabled"
                else "Notification source paused",
        )
    }

    fun downloadRecommendedModel() {
        if (modelDownloadJob?.isActive == true) return
        modelDownloadJob = viewModelScope.launch {
            _ui.value = _ui.value.copy(
                modelDownloading = true,
                modelDownloadProgress = 0,
                modelDownloadStatus = "Connecting to Hugging Face…",
            )
            try {
                val model = LocalModelManager(getApplication()).downloadRecommended { copied, total ->
                    val percent = if (total > 0L) ((copied * 100) / total).toInt().coerceIn(0, 100) else 0
                    _ui.value = _ui.value.copy(
                        modelDownloadProgress = percent,
                        modelDownloadStatus = "Downloading recommended model · $percent%",
                    )
                }
                refreshModelStatus()
                _ui.value = _ui.value.copy(
                    modelDownloading = false,
                    modelDownloadProgress = 100,
                    modelDownloadStatus = "${model.displayName} installed · ready for offline use",
                    status = "Local model installed",
                )
            } catch (_: CancellationException) {
                _ui.value = _ui.value.copy(modelDownloading = false, modelDownloadStatus = "Model download canceled")
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    modelDownloading = false,
                    modelDownloadStatus = "Model download failed: ${e.message ?: "network or storage error"}",
                    status = "Model download failed",
                )
            }
        }
    }

    fun cancelModelDownload() {
        modelDownloadJob?.cancel()
    }

    /** @deprecated use [refreshSetup] */
    fun refreshNotifAccess() = refreshSetup()

    fun openNotifAccessSettings() {
        MnemeNotificationListener.openSettings(getApplication())
    }

    fun openExactAlarmSettings() {
        AppSetup.openExactAlarmSettings(getApplication())
    }

    fun openAccessibilitySettings() {
        AppSetup.openAccessibilitySettings(getApplication())
    }

    fun openAppDetails() {
        AppSetup.openAppDetails(getApplication())
    }

    fun dismissSetupBanner() {
        AppSetup.markSetupSeen(getApplication())
        _ui.value = _ui.value.copy(showSetupBanner = false)
    }

    /** Debug/pitch stills: canned frontend, no LLM. Driven by MainActivity extra `pitch`. */
    fun applyPitchShot(shot: String) {
        AppSetup.markSetupSeen(getApplication())
        val name = shot.trim().lowercase()
        val base = _ui.value.copy(
            showSetupBanner = false,
            searchQuery = "",
            searchResults = null,
            askBusy = false,
            listening = false,
            mediaBusy = false,
            pulsing = false,
        )
        _ui.value = when (name) {
            "chat" -> base.copy(
                tab = MnemeTab.Memory,
                pitchOverlay = PitchOverlay.Chat,
                pinnedNotes = null,
                showBrief = false,
                status = "Assistant chat · local demo",
            )
            "brief" -> base.copy(
                tab = MnemeTab.Memory,
                pitchOverlay = PitchOverlay.Brief,
                pinnedNotes = null,
                showBrief = false,
                status = "Daily brief · Slack + WhatsApp",
            )
            "meeting" -> base.copy(
                tab = MnemeTab.Memory,
                pitchOverlay = PitchOverlay.Meeting,
                pinnedNotes = null,
                showBrief = false,
                status = "Meeting mode · 4 useful signals saved",
            )
            "live" -> base.copy(
                tab = MnemeTab.Memory,
                pitchOverlay = PitchOverlay.Live,
                pinnedNotes = null,
                showBrief = false,
                status = "Live product flow · local memory",
            )
            "action" -> base.copy(
                tab = MnemeTab.Memory,
                pitchOverlay = PitchOverlay.Action,
                pinnedNotes = null,
                showBrief = false,
                status = "One-tap action · close the commitment",
            )
            "messy" -> base.copy(
                tab = MnemeTab.Memory,
                pitchOverlay = PitchOverlay.Messy,
                pinnedNotes = null,
                showBrief = false,
                status = "5 apps · 0 cards",
            )
            "cards" -> base.copy(
                tab = MnemeTab.Memory,
                pitchOverlay = PitchOverlay.None,
                pinnedNotes = PitchCards.home,
                showBrief = false,
                status = PitchCards.BRIEF_HEADLINE,
            )
            "owe", "airplane" -> base.copy(
                tab = MnemeTab.Ask,
                pitchOverlay = PitchOverlay.None,
                pinnedNotes = PitchCards.dues,
                showBrief = false,
                askQuery = "what do I owe?",
                askAnswer = PitchCards.OWE_ANSWER,
                askCitations = PitchCards.dues,
                status = "Answered from 4 notes · on-device",
            )
            "conflict" -> base.copy(
                tab = MnemeTab.Ask,
                pitchOverlay = PitchOverlay.None,
                pinnedNotes = PitchCards.conflict,
                showBrief = false,
                askQuery = "When is Priya?",
                askAnswer = PitchCards.CONFLICT_ANSWER,
                askCitations = PitchCards.conflict,
                status = "Conflict · 2 sources · same person",
            )
            "refuse" -> base.copy(
                tab = MnemeTab.Ask,
                pitchOverlay = PitchOverlay.None,
                pinnedNotes = emptyList(),
                showBrief = false,
                askQuery = "What’s my passport number?",
                askAnswer = PitchCards.REFUSE_ANSWER,
                askCitations = emptyList(),
                status = "No matches",
            )
            "pulse" -> base.copy(
                tab = MnemeTab.Memory,
                pitchOverlay = PitchOverlay.Pulse,
                pinnedNotes = listOf(PitchCards.otp),
                showBrief = false,
                status = "Screenshot processed · image deleted",
            )
            "focus" -> base.copy(
                tab = MnemeTab.Memory,
                pitchOverlay = PitchOverlay.Focus,
                pinnedNotes = listOf(
                    PitchCards.deck,
                    PitchCards.priyaCal.copy(
                        id = 9,
                        title = "Priya · figures",
                        body = "Same session · person merge by name",
                        cardType = "waiting",
                    ),
                ),
                showBrief = false,
                status = "Session 0:12 / 0:15 · on-device",
            )
            "gallery" -> base.copy(
                tab = MnemeTab.Memory,
                pitchOverlay = PitchOverlay.Gallery,
                pinnedNotes = null,
                showBrief = false,
                status = "Gallery has no capture",
            )
            else -> base.copy(
                tab = MnemeTab.Memory,
                pitchOverlay = PitchOverlay.None,
                pinnedNotes = PitchCards.home,
                showBrief = false,
                status = PitchCards.BRIEF_HEADLINE,
            )
        }
    }

    fun setTab(tab: MnemeTab) {
        if (_ui.value.listening) stopListening(submit = false)
        if (tab == MnemeTab.Settings) {
            AppSetup.markSetupSeen(getApplication())
            refreshSetup()
        }
        _ui.value = _ui.value.copy(tab = tab)
        if (tab != MnemeTab.Ask) {
            viewModelScope.launch {
                llm.unload()
                whisper.unload()
            }
        }
    }

    fun setSearch(query: String) {
        _ui.value = _ui.value.copy(searchQuery = query)
        if (query.isBlank()) {
            _ui.value = _ui.value.copy(searchResults = null)
            return
        }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(searchResults = notes.search(query))
        }
    }

    fun setAskQuery(query: String) {
        _ui.value = _ui.value.copy(askQuery = query)
    }

    fun toggleScreenContext() {
        if (_ui.value.useScreenContext) {
            _ui.value = _ui.value.copy(useScreenContext = false, screenContextSource = null, status = "Screen context off")
            return
        }
        val snapshot = RetenqoAccessibilityService.currentSnapshot()
        if (snapshot == null) {
            if (!RetenqoAccessibilityService.isConnected()) openAccessibilitySettings()
            _ui.value = _ui.value.copy(
                useScreenContext = false,
                screenContextSource = null,
                status = if (RetenqoAccessibilityService.isConnected()) {
                    "Open the app you want to ask about, then return here"
                } else {
                    "Enable Retenqo screen access, then open the app you want to ask about"
                },
            )
            return
        }
        _ui.value = _ui.value.copy(
            useScreenContext = true,
            screenContextSource = snapshot.packageName,
            status = "Screen context ready from ${snapshot.packageName}",
        )
    }

    fun submitAsk() {
        val q = _ui.value.askQuery.trim()
        if (q.isBlank() || _ui.value.askBusy) return
        handleUtterance(q)
    }

    fun startListening() {
        if (_ui.value.listening || _ui.value.askBusy) return
        try {
            mic.start()
            _ui.value = _ui.value.copy(listening = true, status = "Listening… hold mic")
            pumpJob = viewModelScope.launch { mic.pump() }
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(status = "Mic failed: ${e.message}")
        }
    }

    fun stopListening(submit: Boolean = true) {
        if (!_ui.value.listening) return
        pumpJob?.cancel()
        pumpJob = null
        _ui.value = _ui.value.copy(listening = false)
        viewModelScope.launch {
            val samples = mic.stopAndGetSamples()
            if (!submit) return@launch
            if (samples.size < MicRecorder.SAMPLE_RATE / 2) {
                _ui.value = _ui.value.copy(status = "Too short — hold longer")
                return@launch
            }
            _ui.value = _ui.value.copy(askBusy = true, status = "Transcribing…")
            val text = try {
                whisper.transcribe(samples)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(askBusy = false, status = "Whisper failed: ${e.message}")
                return@launch
            } finally {
                // Release the speech model before a voice query loads the larger chat model.
                whisper.unload()
            }
            if (text.isBlank()) {
                _ui.value = _ui.value.copy(askBusy = false, status = "Heard nothing")
                return@launch
            }
            _ui.value = _ui.value.copy(askQuery = text)
            handleUtterance(text)
        }
    }

    private fun handleUtterance(text: String) {
        viewModelScope.launch {
            val useScreen = _ui.value.useScreenContext
            val screenSnapshot = if (useScreen) RetenqoAccessibilityService.currentSnapshot() else null
            _ui.value = _ui.value.copy(
                askBusy = true,
                askAnswer = null,
                askQuery = text,
                useScreenContext = false,
                screenContextSource = null,
            )
            when (val intent = VoiceIntentRouter.route(text)) {
                is VoiceIntent.Capabilities -> {
                    val msg = "I can save notes and tasks, set reminders, add calendar events, open Gmail, read supported WhatsApp and app notifications after you enable Notification access, search your saved memory, scan screens, and help with routes and accessibility."
                    _ui.value = _ui.value.copy(askBusy = false, askAnswer = msg, askCitations = emptyList(), status = "Phone assistant ready")
                    tts.speak(msg.take(220))
                }
                is VoiceIntent.CheckEmail -> {
                    val launch = getApplication<Application>().packageManager
                        .getLaunchIntentForPackage("com.google.android.gm")
                        ?: Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    getApplication<Application>().startActivity(launch)
                    val msg = "Opening Gmail."
                    _ui.value = _ui.value.copy(askBusy = false, askAnswer = msg, askCitations = emptyList(), status = msg)
                    tts.speak(msg)
                }
                is VoiceIntent.ReadNotifications -> {
                    if (!MnemeNotificationListener.isEnabled(getApplication())) {
                        MnemeNotificationListener.openSettings(getApplication())
                        val msg = "Enable Notification access so I can read supported WhatsApp, Gmail, SMS, and calendar notifications."
                        _ui.value = _ui.value.copy(askBusy = false, askAnswer = msg, askCitations = emptyList(), status = "Notification access needed")
                        tts.speak(msg.take(220))
                    } else {
                        MnemeNotificationListener.syncActiveNotifications()
                        val msg = "I’m reading the latest supported notifications into your memory. Ask again in a few seconds."
                        _ui.value = _ui.value.copy(askBusy = false, askAnswer = msg, askCitations = emptyList(), status = "Reading notifications…")
                        tts.speak(msg.take(220))
                    }
                }
                is VoiceIntent.AddCalendar -> {
                    val calendar = Intent(Intent.ACTION_INSERT).apply {
                        data = CalendarContract.Events.CONTENT_URI
                        putExtra(CalendarContract.Events.TITLE, intent.title)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    getApplication<Application>().startActivity(calendar)
                    val msg = "Opening Calendar to add ${intent.title}."
                    _ui.value = _ui.value.copy(askBusy = false, askAnswer = msg, askCitations = emptyList(), status = msg)
                    tts.speak(msg)
                }
                is VoiceIntent.CreateTask -> {
                    val id = notes.saveIfNew(
                        title = "Task: ${intent.text.take(80)}",
                        body = intent.text,
                        source = "Task",
                    )
                    val msg = if (id != null) "Task saved to Memory." else "That task is already saved."
                    _ui.value = _ui.value.copy(askBusy = false, askAnswer = msg, askCitations = emptyList(), status = msg)
                    tts.speak(msg)
                }
                is VoiceIntent.Route -> {
                    val msg = "Opening directions to ${intent.destination}"
                    try {
                        getApplication<Application>().startActivity(
                            Intent(Intent.ACTION_VIEW, "google.navigation:q=${Uri.encode(intent.destination)}".toUri())
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    } catch (_: Exception) {
                        getApplication<Application>().startActivity(
                            Intent(Intent.ACTION_VIEW, "geo:0,0?q=${Uri.encode(intent.destination)}".toUri())
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                    _ui.value = _ui.value.copy(askBusy = false, askAnswer = msg, askCitations = emptyList(), status = msg)
                    tts.speak(msg)
                }
                is VoiceIntent.AccessibilityHelp -> {
                    val msg = "Opening accessibility settings. You can enable TalkBack, magnification, and switch access there."
                    getApplication<Application>().startActivity(
                        Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                    _ui.value = _ui.value.copy(askBusy = false, askAnswer = msg, askCitations = emptyList(), status = "Accessibility help")
                    tts.speak(msg)
                }
                is VoiceIntent.WhatsDue -> {
                    _ui.value = _ui.value.copy(status = "Checking due dates…")
                    val result: AskResult = try {
                        asker.ask("what's due")
                    } catch (e: Exception) {
                        AskResult("Due check failed: ${e.message}", emptyList())
                    }
                    _ui.value = _ui.value.copy(
                        askBusy = false,
                        askAnswer = result.answer,
                        askCitations = result.citations,
                        status = if (result.citations.isEmpty()) "No dues" else "${result.citations.size} with due dates",
                    )
                    tts.speak(result.answer.take(160))
                }
                is VoiceIntent.SetAlarm -> {
                    val msg = try {
                        alarms.schedule(intent.hour, intent.minute, intent.label)
                    } catch (e: Exception) {
                        "Alarm failed: ${e.message}"
                    }
                    _ui.value = _ui.value.copy(
                        askBusy = false,
                        askAnswer = msg,
                        askCitations = emptyList(),
                        status = msg,
                    )
                    tts.speak(msg)
                }
                is VoiceIntent.Remember -> {
                    notes.saveIfNew(
                        title = intent.text.take(80),
                        body = intent.text,
                        source = "Voice",
                    )
                    val msg = "Saved to memory."
                    _ui.value = _ui.value.copy(
                        askBusy = false,
                        askAnswer = msg,
                        askCitations = emptyList(),
                        status = msg,
                    )
                    tts.speak(msg)
                }
                is VoiceIntent.Query -> {
                    _ui.value = _ui.value.copy(status = "Searching memory…")
                    val result: AskResult = try {
                        if (useScreen && screenSnapshot == null) {
                            AskResult("The screen snapshot expired. Open the app again and enable screen context.", emptyList())
                        } else {
                            asker.ask(intent.text, screenContext = screenSnapshot)
                        }
                    } catch (e: Exception) {
                        AskResult("Ask failed: ${e.message}", emptyList())
                    }
                    _ui.value = _ui.value.copy(
                        askBusy = false,
                        askAnswer = result.answer,
                        askCitations = result.citations,
                        status = if (useScreen && screenSnapshot == null) {
                            "Screen snapshot expired — enable it again"
                        } else if (screenSnapshot != null) {
                            "Answered with screen context${screenSnapshot.packageName.let { " from $it" }}"
                        } else if (result.citations.isEmpty()) {
                            "No matches"
                        } else {
                            "Answered from ${result.citations.size} notes"
                        },
                    )
                    tts.speak(result.answer)
                }
            }
        }
    }

    fun onPulseBitmap(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(pulsing = true, status = "Reading screen…")
            val result: PulseResult = try {
                pulse.runFromBitmap(bitmap)
            } catch (e: Exception) {
                PulseResult(null, "Pulse failed: ${e.message}", false)
            }
            _ui.value = _ui.value.copy(pulsing = false, status = result.preview)
        }
    }

    fun scanWhatsAppMedia() {
        if (_ui.value.mediaBusy || _ui.value.pulsing) return
        viewModelScope.launch {
            if (!AppSetup.snapshot(getApplication()).canReadMedia) {
                _ui.value = _ui.value.copy(status = "Allow Photos & media access before scanning WhatsApp files")
                return@launch
            }
            val gate = MnemePowerGates.canRunHeavyWork(getApplication(), userInitiated = true)
            if (!gate.ok && gate.reason.contains("RAM", ignoreCase = true)) {
                _ui.value = _ui.value.copy(status = gate.reason)
                return@launch
            }
            _ui.value = _ui.value.copy(mediaBusy = true, status = "Scanning WhatsApp media…")
            val result = try {
                media.scanWhatsAppRecent(limit = 4)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    mediaBusy = false,
                    status = "Media scan failed: ${e.message}",
                )
                return@launch
            }
            _ui.value = _ui.value.copy(mediaBusy = false, status = result.message)
        }
    }

    /** Imports files explicitly selected with Android's photo/document picker. */
    fun ingestSelectedMedia(uris: List<Uri>) {
        if (uris.isEmpty() || _ui.value.mediaBusy || _ui.value.pulsing) return
        val selected = uris.take(MAX_SELECTED_MEDIA_FILES)
        viewModelScope.launch {
            val gate = MnemePowerGates.canRunHeavyWork(getApplication(), userInitiated = true)
            if (!gate.ok && gate.reason.contains("RAM", ignoreCase = true)) {
                _ui.value = _ui.value.copy(status = gate.reason)
                return@launch
            }
            _ui.value = _ui.value.copy(
                mediaBusy = true,
                status = if (selected.size < uris.size) "Importing first $MAX_SELECTED_MEDIA_FILES selected files…"
                    else "Reading selected files on this phone…",
            )
            var saved = 0
            try {
                for ((index, uri) in selected.withIndex()) {
                    _ui.value = _ui.value.copy(status = "Reading selected file ${index + 1} of ${selected.size}…")
                    val result = media.ingestSelected(uri)
                    if (result.noteId != null) saved++
                }
                val extra = if (selected.size < uris.size) " · first ${selected.size} of ${uris.size}" else ""
                val summary = "Selected import complete · $saved saved · ${selected.size - saved} skipped$extra"
                _ui.value = _ui.value.copy(mediaBusy = false, status = summary)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    mediaBusy = false,
                    status = "Selected import stopped: ${e.message ?: "could not read the file"}",
                )
            }
        }
    }

    fun ingestSharedText(text: String) {
        val content = text.trim().take(MAX_SHARED_TEXT_CHARS)
        if (content.length < 8 || _ui.value.mediaBusy || _ui.value.pulsing) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(mediaBusy = true, status = "Saving shared text on this phone…")
            val result = try {
                pulse.ingestText(content, sourceLabel = "Shared from another app")
            } catch (e: Exception) {
                PulseResult(null, "Shared text could not be saved: ${e.message}", false)
            }
            _ui.value = _ui.value.copy(mediaBusy = false, status = result.preview)
        }
    }

    /** On-device E2E without real WA: sample chat → extract → card. */
    fun ingestDemoSample() {
        if (_ui.value.mediaBusy || _ui.value.pulsing) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(mediaBusy = true, status = "Saving demo card…")
            val sample = """
                From Rahul:
                Pay me ₹500 by Friday for dinner last week
            """.trimIndent()
            val result = try {
                pulse.ingestText(sample, sourceLabel = IngestSources.DEMO)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    mediaBusy = false,
                    status = "Demo ingest failed: ${e.message}",
                )
                return@launch
            }
            _ui.value = _ui.value.copy(
                mediaBusy = false,
                status = result.preview,
                tab = MnemeTab.Memory,
            )
        }
    }

    fun showWhatsDue() {
        if (_ui.value.tab != MnemeTab.Ask) {
            _ui.value = _ui.value.copy(tab = MnemeTab.Ask)
        }
        handleUtterance("what's due today")
    }

    /** Recompute catch-up brief. [autoShow] only opens panel when there is something new. */
    fun refreshBrief(autoShow: Boolean = false) {
        viewModelScope.launch {
            // Pitch states own their visible cards; startup refresh must not overwrite them.
            if (_ui.value.pitchOverlay != PitchOverlay.None || _ui.value.pinnedNotes != null) {
                return@launch
            }
            val app = getApplication<Application>()
            val last = BriefPrefs.lastOpenAt(app)
            val brief = CatchUpBriefBuilder.build(notes.getAll(), last)
            val body = CatchUpBriefBuilder.formatBody(brief)
            val openPanel = if (autoShow) !brief.isEmpty else true
            if (_ui.value.pitchOverlay != PitchOverlay.None || _ui.value.pinnedNotes != null) {
                return@launch
            }
            _ui.value = _ui.value.copy(
                showBrief = openPanel,
                briefHeadline = brief.headline,
                briefBody = body,
                status = when {
                    openPanel && !brief.isEmpty -> brief.headline
                    else -> _ui.value.status
                },
            )
            if (autoShow && brief.isEmpty) {
                BriefPrefs.markOpened(app)
            }
        }
    }

    fun showBrief() {
        if (_ui.value.tab != MnemeTab.Memory) {
            _ui.value = _ui.value.copy(tab = MnemeTab.Memory)
        }
        refreshBrief(autoShow = false)
    }

    fun dismissBrief() {
        BriefPrefs.markOpened(getApplication())
        _ui.value = _ui.value.copy(showBrief = false, status = "Ready")
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            notes.delete(id)
            DueReminderScheduler.cancel(getApplication(), id)
        }
    }

    fun deleteAllPersonalData() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            notes.getAll().asSequence().filter { it.dueAt != null }.forEach { note ->
                DueReminderScheduler.cancel(app, note.id)
            }
            notes.deleteAll()
            NotifIngestQueue.clear(app)
            androidx.work.WorkManager.getInstance(app).cancelAllWorkByTag("mneme_notif")
            androidx.work.WorkManager.getInstance(app).cancelAllWorkByTag("mneme_media")
            app.cacheDir.listFiles()?.filter { it.name.startsWith("selected-media-") || it.name.endsWith(".partial") }
                ?.forEach { it.delete() }
            media.clearHistory()
            com.mneme.app.accessibility.RetenqoAccessibilityService.clearSnapshot()
            NotificationCaptureSettings.sources.forEach { NotificationCaptureSettings.setEnabled(app, it.id, false) }
            MediaIngestSettings.setPeriodicEnabled(app, false)
            (app as? com.mneme.app.MnemeApp)?.refreshMediaIngest()
            _ui.value = _ui.value.copy(status = "All saved personal data deleted")
        }
    }

    fun markDone(id: Long) {
        viewModelScope.launch {
            DueReminderScheduler.cancel(getApplication(), id)
            notes.markDone(id)
            _ui.value = _ui.value.copy(status = "Marked done")
        }
    }

    fun snoozeOneDay(id: Long) {
        viewModelScope.launch {
            val until = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
            notes.snooze(id, until)
            _ui.value = _ui.value.copy(status = "Snoozed 1 day")
        }
    }

    fun reopenCard(id: Long) {
        viewModelScope.launch {
            notes.markOpen(id)
            _ui.value = _ui.value.copy(status = "Reopened")
        }
    }

    fun saveTaskEdits(id: Long, title: String, body: String, who: String?, dueAt: Long?, type: CardType) {
        viewModelScope.launch {
            try {
                val note = notes.updateCardFields(id, title, body, who, dueAt, type)
                if (note == null) {
                    _ui.value = _ui.value.copy(status = "This task no longer exists")
                } else {
                    val app = getApplication<Application>()
                    if (note.review() == com.mneme.app.data.CardReviewState.CONFIRMED &&
                        note.status() == com.mneme.app.data.CardStatus.OPEN && note.dueAt != null
                    ) {
                        DueReminderScheduler.schedule(app, note.id, note.title, note.dueAt)
                    } else {
                        DueReminderScheduler.cancel(app, note.id)
                    }
                    _ui.value = _ui.value.copy(status = "Task updated")
                }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(status = "Could not update task: ${e.message ?: "check the fields"}")
            }
        }
    }

    fun loadTaskHistory(id: Long) {
        _ui.value = _ui.value.copy(taskHistoryNoteId = id, taskHistory = emptyList())
        viewModelScope.launch {
            val history = notes.history(id)
            if (_ui.value.taskHistoryNoteId == id) {
                _ui.value = _ui.value.copy(taskHistory = history)
            }
        }
    }

    fun exportBackup(uri: Uri, password: CharArray) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(status = "Preparing local backup…")
            try {
                MnemeBackupManager(getApplication()).export(uri, password)
                _ui.value = _ui.value.copy(status = "Retenqo backup saved")
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(status = "Backup failed: ${e.message ?: "could not write file"}")
            } finally {
                password.fill('\u0000')
            }
        }
    }

    fun setPendingBackupExportPassword(password: CharArray) {
        pendingBackupExportPassword?.fill('\u0000')
        pendingBackupExportPassword = password
    }

    fun takePendingBackupExportPassword(): CharArray? = pendingBackupExportPassword.also {
        pendingBackupExportPassword = null
    }

    fun restoreBackup(uri: Uri, password: CharArray) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(status = "Restoring local backup…")
            try {
                val result = MnemeBackupManager(getApplication()).restore(uri, password)
                _ui.value = _ui.value.copy(
                    status = "Restored ${result.notesAdded} cards, ${result.historyEventsAdded} history events, ${result.suggestionsAdded} suggestions",
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(status = "Restore failed: ${e.message ?: "backup could not be read"}")
            } finally {
                password.fill('\u0000')
            }
        }
    }

    fun acceptCompletionSuggestion(suggestion: FollowThroughSuggestion) {
        viewModelScope.launch {
            DueReminderScheduler.cancel(getApplication(), suggestion.commitmentId)
            notes.acceptCompletionSuggestion(suggestion.id, suggestion.commitmentId)
            _ui.value = _ui.value.copy(status = "Commitment marked done from confirmed evidence")
        }
    }

    fun dismissCompletionSuggestion(id: Long) {
        viewModelScope.launch {
            notes.dismissCompletionSuggestion(id)
            _ui.value = _ui.value.copy(status = "Evidence suggestion dismissed")
        }
    }

    fun acceptCapturedCard(id: Long) {
        viewModelScope.launch {
            val note = notes.acceptReview(id)
            if (note?.dueAt != null) {
                DueReminderScheduler.schedule(getApplication(), note.id, note.title, note.dueAt)
            }
            _ui.value = _ui.value.copy(status = if (note == null) "This card was already reviewed" else "Added to your tasks")
        }
    }

    fun dismissCapturedCard(id: Long) {
        viewModelScope.launch {
            DueReminderScheduler.cancel(getApplication(), id)
            notes.dismissReview(id)
            _ui.value = _ui.value.copy(status = "Captured suggestion dismissed")
        }
    }

    override fun onCleared() {
        pendingBackupExportPassword?.fill('\u0000')
        pendingBackupExportPassword = null
        if (_ui.value.listening) stopListening(submit = false)
        tts.shutdown()
        super.onCleared()
    }

    companion object {
        private const val MAX_SHARED_TEXT_CHARS = 20_000
        private const val MAX_SELECTED_MEDIA_FILES = 8

        fun factory(
            app: Application,
            notes: NoteRepository,
            pulse: PulsePipeline,
            media: MediaIngestPipeline,
            asker: MemoryAsk,
            llm: LlmBridge,
            whisper: WhisperBridge,
            tts: TtsSpeaker,
            alarms: AlarmScheduler,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MnemeViewModel(app, notes, pulse, media, asker, llm, whisper, tts, alarms) as T
        }
    }
}
