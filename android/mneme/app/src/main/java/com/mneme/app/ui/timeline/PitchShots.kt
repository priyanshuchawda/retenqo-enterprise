package com.mneme.app.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mneme.app.data.CardType
import com.mneme.app.data.IngestSources
import com.mneme.app.data.NoteEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PitchOverlay { None, Messy, Pulse, Focus, Gallery, Action, Live, Meeting, Brief, Chat }

@Composable
fun PitchAssistantChatScreen() {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    var query by remember { mutableStateOf("so whats status updates after my meeting") }
    var showResponse by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        delay(1800)
        showResponse = true
    }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()
            .verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("MNEME ASSISTANT", color = accent, fontWeight = FontWeight.Bold)
                Text("Your day, made clear", color = muted)
            }
            Text("LOCAL", color = muted, style = MaterialTheme.typography.labelSmall)
        }
        Text(query, color = ink, modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp)).padding(16.dp))
        if (!showResponse) {
            Text("Mneme is reading your saved context…", color = muted)
        }
        if (showResponse) {
            Text("Here’s what matters", color = accent, fontWeight = FontWeight.Bold)
        BriefSection("PROJECT NEXUS · SLACK", Color(0xFFB98CFF)) {
            BriefItem("Delivery", "Export fix before Friday · build Thursday · client tests Friday")
            BriefItem("Review", "Tomorrow at 3 PM · prepare graphs")
        }
        BriefSection("ISRO · WHATSAPP", Color(0xFF72B7FF)) {
            BriefItem("Progress", "Preprocessing and thresholds complete")
            BriefItem("Next", "Test-data review tomorrow at 4 PM · bring graphs and architecture diagram")
        }
        BriefSection("FORT · WHATSAPP", Color(0xFFFFB38A)) {
            BriefItem("Plan", "4 PM outing · metro route and pickup shared")
        }
        BriefSection("SUGGESTION", Color(0xFFFFD166)) {
            BriefItem("Conflict", "ISRO review overlaps the Fort plan")
            BriefItem("Recommended", "Move Fort to 6 PM")
            Text("Calendar: reschedule Fort  ·  Tasks: prepare graphs", color = muted, style = MaterialTheme.typography.bodySmall)
        }
        }
        Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp)).border(1.dp, accent.copy(alpha = .28f), RoundedCornerShape(22.dp)).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink),
                    singleLine = true,
                    decorationBox = { field -> if (query.isBlank()) Text("Ask Mneme…", color = muted) else field },
                )
                Text("↑", color = accent, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp).clickable {
                    showResponse = false
                    scope.launch {
                        delay(1800)
                        showResponse = true
                    }
                })
            }
        }
    }
}

@Composable
fun PitchDailyBriefScreen() {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("DAILY BRIEF", color = accent, fontWeight = FontWeight.Bold)
        Text("What matters across your conversations", style = MaterialTheme.typography.headlineMedium, color = ink)
        Text("Retenqo merged duplicate updates and left casual chat out.", color = muted)
        BriefSection("TODAY'S ATTENTION", accent) {
            BriefItem("Tonight", "Send the revised presentation to Aditya")
            BriefItem("Tomorrow · 11:30", "Meeting moved. Bring Q3 projections")
            BriefItem("Before Friday", "Fix the dashboard export issue")
            BriefItem("Tomorrow · 3 PM", "Prepare graphs for the project review")
        }
        BriefSection("PROJECT NEXUS · SLACK", Color(0xFFB98CFF)) {
            BriefItem("Client feedback", "Dashboard is strong; export issue must be fixed")
            BriefItem("Delivery", "Revised build Thursday evening · client tests Friday")
            BriefItem("Review", "Tomorrow's review moved from 4 PM to 3 PM")
        }
        BriefSection("ISRO LUNAR PROJECT · WHATSAPP", Color(0xFF72B7FF)) {
            BriefItem("Progress", "Training and preprocessing complete; thresholds calculated")
            BriefItem("Next", "Testing on test data · review tomorrow at 4 PM")
            BriefItem("Your part", "Prepare graphs and present in the meeting")
        }
        BriefSection("FORT PLAN · WHATSAPP", Color(0xFFFFB38A)) {
            BriefItem("Saturday · 4 PM", "Fort outing planned")
            BriefItem("Travel", "Metro route and pickup location shared")
        }
        BriefSection("ASSISTANT SUGGESTION", Color(0xFFFFD166)) {
            BriefItem("Conflict", "ISRO review and Fort plan both land at 4 PM")
            BriefItem("Suggestion", "Move the Fort plan to 6 PM")
            BriefItem("Next step", "Update Calendar and save the reminder to Tasks")
        }
        Text("18 casual messages filtered · no action needed", color = muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun BriefSection(title: String, color: Color, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp)).border(1.dp, color.copy(alpha = .28f), RoundedCornerShape(18.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Text(title, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        content()
    }
}

@Composable
private fun BriefItem(label: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        Text(text, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun PitchMeetingScreen() {
    var ended by remember { mutableStateOf(false) }
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("MEETING MODE", color = accent, fontWeight = FontWeight.Bold)
                Text(if (ended) "Your catch-up is ready" else "You stay present. Retenqo keeps up.", style = MaterialTheme.typography.titleLarge, color = ink)
            }
            Text(if (ended) "DONE" else "LIVE", color = if (ended) accent else MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
        }
        if (!ended) {
            Text("Google Meet · Project sync · 19:20–19:50", color = muted)
            Column(Modifier.fillMaxWidth().background(surface, RoundedCornerShape(18.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Incoming group chat", color = accent, fontWeight = FontWeight.Bold)
                MeetingMessage("Aditya", "Tomorrow's meeting moved to 11:30 instead of 10. Bring the latest numbers and Q3 projections 📊")
                MeetingMessage("Prof. Sharma", "Assignment is due Monday at 9 AM 📚")
                MeetingMessage("Neha", "Please remind me to call the supplier tomorrow afternoon about the 50 units.")
                MeetingMessage("Rahul", "Client liked the prototype. Revised notification flow needed by Thursday.")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("4 useful signals saved · 7 casual messages ignored", color = muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Button(onClick = { ended = true }) { Text("End meeting") }
            }
        } else {
            Text("While you were in the meeting, Retenqo prepared this brief.", color = muted)
            Column(Modifier.fillMaxWidth().background(surface, RoundedCornerShape(18.dp)).border(1.dp, accent.copy(alpha = .28f), RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("MEETING BRIEF · 4 IMPORTANT ITEMS", color = accent, fontWeight = FontWeight.Bold)
                SummaryRow("11:30 tomorrow", "Meeting moved from 10:00 · bring Q3 projections")
                SummaryRow("Monday · 9:00 AM", "Assignment deadline · Prof. Sharma")
                SummaryRow("Tomorrow afternoon", "Call supplier about 50 units")
                SummaryRow("By Thursday", "Revise notification flow · client feedback")
            }
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Saved to your memory", color = accent, fontWeight = FontWeight.Bold)
                Text("4 actionable cards · source: project group · processed on device", color = ink)
                Text("Casual chat, emojis and spam were left out.", color = muted)
            }
            Button(onClick = { ended = false }, modifier = Modifier.fillMaxWidth()) { Text("Replay meeting capture") }
        }
    }
}

@Composable
private fun MeetingMessage(sender: String, message: String) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(10.dp)) {
        Text(sender, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(message, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SummaryRow(label: String, detail: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Column(Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PitchLiveFlowScreen() {
    var stage by remember { mutableStateOf(0) }
    val paper = MaterialTheme.colorScheme.background
    val ink = MaterialTheme.colorScheme.onBackground
    val green = MaterialTheme.colorScheme.primary
    val stages = listOf("Everything important, in one place", "Your daily Brief", "Ask your assistant", "An answer you can trust", "Conflicts stay visible", "It knows what it doesn’t know", "Close the loop")
    val captions = listOf("Messages, plans and reminders become useful context.", "Retenqo turns scattered signals into a calm next step.", "Ask one question instead of searching everywhere.", "Every answer keeps its source visible.", "Two sources disagree? Both stay visible.", "No match? Your assistant refuses to invent one.", "One tap removes the commitment from today’s queue.")
    Column(Modifier.fillMaxSize().background(paper).statusBarsPadding().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("PERSONAL ASSISTANT", color = green, fontWeight = FontWeight.Bold)
            Text("RETENQO", color = ink.copy(alpha = .55f), fontWeight = FontWeight.Bold)
        }
        Text(stages[stage], style = MaterialTheme.typography.headlineMedium, color = ink)
        Text(captions[stage], style = MaterialTheme.typography.titleMedium, color = ink.copy(alpha = .7f))
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (stage) {
                0 -> { Text("Signals from your day", color = ink, fontWeight = FontWeight.Bold); MessyBubble("Rahul: pay me ₹500 by Friday for dinner", "WhatsApp · Today 09:14"); MessyBubble("Priya: can we do 5 instead?", "WhatsApp · Today 11:02") }
                1 -> { Text("BRIEF · 3 OPEN · READY", color = green, fontWeight = FontWeight.Bold); Text("Pay Rahul ₹500 · Friday", color = ink); Text("Submit Chapter 11 · 25 Aug", color = ink); Text("Priya · 4pm Calendar vs 5pm WhatsApp", color = ink) }
                2 -> { Text("Ask your assistant", color = ink, fontWeight = FontWeight.Bold); Text("what do I owe?", color = ink, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(12.dp)) }
                3 -> { Text("You owe / have due:", color = ink, fontWeight = FontWeight.Bold); Text("[1] Rahul — ₹500 by Friday\n[2] Priya — Thursday 4pm\n[3] Chapter 11 — submit by 25 Aug\n[4] Netflix — ₹1,299 charged", color = ink); Text("4 cited notes · answered on-device", color = green, fontWeight = FontWeight.Bold) }
                4 -> { Text("Two times, two sources:", color = ink, fontWeight = FontWeight.Bold); Text("Calendar — Priya, Thu 4:00pm\nWhatsApp — Priya, Thu 5:00pm", color = ink); Text("Conflict preserved · no silent overwrite", color = Color(0xFFB55B2A), fontWeight = FontWeight.Bold) }
                5 -> { Text("What’s my passport number?", color = ink); Text("I don’t have that in your memory.", color = ink, modifier = Modifier.background(Color(0xFFE9EEE9), RoundedCornerShape(12.dp)).padding(12.dp)); Text("No match · no hallucination", color = green, fontWeight = FontWeight.Bold) }
                else -> { Text("Pay Rahul ₹500", style = MaterialTheme.typography.titleLarge, color = ink); Text(if (stage == 6) "Due Friday · WhatsApp source [1]" else "Removed from today’s due list ✓", color = if (stage == 6) ink.copy(alpha = .7f) else green, fontWeight = if (stage == 6) FontWeight.Normal else FontWeight.Bold); if (stage == 6) Button(onClick = { stage = 7 }) { Text("Mark done") } else Text("No more reminder · today’s queue is focused.", color = green) }
            }
        }
        if (stage < 6) Button(onClick = { stage++ }, modifier = Modifier.fillMaxWidth()) { Text(if (stage == 0) "Build my Brief" else "Continue") }
        if (stage == 6) Text("ONE-TAP ACTION · close the commitment", color = Color(0xFFB55B2A), fontWeight = FontWeight.Bold)
        if (stage == 7) Text("ACTION COMPLETE · attention queue reduced", color = green, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PitchActionScreen(done: Boolean, onDone: () -> Unit) {
    val paper = Color(0xFFF3F1EB)
    val ink = Color(0xFF14202B)
    val green = Color(0xFF2E806D)
    Column(
        modifier = Modifier.fillMaxSize().background(paper).statusBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("BRIEF · 3 OPEN · CATCH-UP READY", color = green, fontWeight = FontWeight.Bold)
        Text("Close the loop in one tap.", style = MaterialTheme.typography.displaySmall, color = ink)
        Text("The commitment becomes an outcome—not another item to remember.", style = MaterialTheme.typography.titleMedium, color = ink.copy(alpha = .7f))
        Column(
            modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).border(1.dp, Color(0xFFD7DDD9), RoundedCornerShape(18.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Pay Rahul ₹500", style = MaterialTheme.typography.headlineSmall, color = ink)
            Text("Due Friday · WhatsApp source [1]", color = ink.copy(alpha = .7f))
            if (!done) {
                Button(onClick = onDone) { Text("Mark done") }
            } else {
                Text("Removed from today’s due list ✓", color = green, fontWeight = FontWeight.Bold)
                Text("No more reminder for this item · the next Brief stays focused.", color = green)
            }
        }
        Text(if (done) "ACTION COMPLETE · attention queue reduced" else "ONE-TAP ACTION · close the commitment", color = if (done) green else Color(0xFFB55B2A), fontWeight = FontWeight.Bold)
    }
}

object PitchCards {
    private fun note(
        id: Long,
        title: String,
        body: String,
        source: String,
        type: CardType,
        who: String? = null,
        createdAgoMs: Long = 3_600_000L,
    ) = NoteEntity(
        id = id,
        title = title,
        body = body,
        source = source,
        createdAt = System.currentTimeMillis() - createdAgoMs,
        contentHash = "pitch-$id",
        cardType = type.storage,
        who = who,
    )

    val rahul = note(
        1, "Pay Rahul ₹500",
        "Friday · dinner last week · WhatsApp",
        IngestSources.WA_NOTIF, CardType.DUE, "Rahul",
    )
    val priyaCal = note(
        2, "Priya · Thursday 4pm",
        "Calendar · cafe near gate 2",
        IngestSources.CALENDAR_NOTIF, CardType.ASK_FOR_YOU, "Priya",
    )
    val priyaWa = note(
        3, "Priya · Thursday 5pm",
        "WhatsApp · “can we do 5 instead?”",
        IngestSources.WA_NOTIF, CardType.WAITING, "Priya",
        createdAgoMs = 2_000_000L,
    )
    val neha = note(
        4, "Invoice from Neha",
        "Promised “tonight” · 3 days quiet",
        IngestSources.WA_NOTIF, CardType.WAITING, "Neha",
        createdAgoMs = 4L * 24 * 3600_000,
    )
    val chapter = note(
        5, "Submit Chapter 11",
        "LMS · by 25 Aug · viva Monday",
        "LMS notif", CardType.DUE,
    )
    val netflix = note(
        6, "Netflix ₹1,299",
        "Gmail · INR 1,299 charged",
        IngestSources.GMAIL_NOTIF, CardType.DUE,
    )
    val otp = note(
        7, "OTP screen (dummy)",
        "Text card kept. Pixel file unlinked. Gallery has no capture.",
        IngestSources.PULSE, CardType.FACT,
    )
    val deck = note(
        8, "Ship deck · Monday",
        "From Focus · spoken standup",
        "Focus", CardType.DUE, "Priya",
    )

    val home = listOf(rahul, priyaCal, priyaWa, neha)
    val dues = listOf(rahul, priyaCal, chapter, netflix)
    val conflict = listOf(priyaCal, priyaWa)

    const val OWE_ANSWER = """You owe / have due:
[1] Rahul — ₹500 by Friday (dinner)
[2] Priya — Thursday 4pm (Calendar)
[3] Chapter 11 — submit by 25 Aug
[4] Netflix — ₹1,299 charged"""

    const val CONFLICT_ANSWER = """Two times, two sources:
[1] Calendar — Priya, Thu 4:00pm, cafe near gate 2
[2] WhatsApp — Priya, Thu 5:00pm “running late, 5?”"""

    const val REFUSE_ANSWER = "I don't have that in your memory."

    const val BRIEF_HEADLINE = "3 open · catch-up ready"
    const val BRIEF_BODY = """Due
• Pay Rahul ₹500 — Friday · WA notif
• Submit Chapter 11 — 25 Aug · LMS

Conflict
• Priya Thu 4pm (Calendar) vs 5pm (WhatsApp)

Waiting
• Invoice from Neha — 3 days quiet"""
}

@Composable
fun PitchMessyScreen() {
    val paper = Color(0xFFF3F0E8)
    val ink = Color(0xFF121A17)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(paper)
            .statusBarsPadding()
            .padding(20.dp),
    ) {
        Text("Chats — unread 47", style = MaterialTheme.typography.titleLarge, color = ink)
        Spacer(Modifier.height(14.dp))
        MessyBubble("Priya: cafe near gate 2 at 4pm thu?", "Calendar ping forwarded · Yesterday")
        MessyBubble("Priya: wait can we do 5 instead I’m in lab", "WhatsApp · Today 11:02")
        MessyBubble("Rahul: pay me ₹500 by Friday for dinner", "WhatsApp · Today 09:14")
        MessyBubble("Mam: submit ch-11 by 25/08 and viva monday", "TY BTech · 2d")
        MessyBubble("Netflix: INR 1,299 charged", "Email notification")
        Spacer(Modifier.weight(1f))
        Text(
            "Five apps. Zero cards. Friday is the cost.",
            color = Color(0xFFB55B2A),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MessyBubble(text: String, meta: String) {
    Column(
        modifier = Modifier
            .padding(bottom = 10.dp)
            .fillMaxWidth(0.92f)
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFCFC8BC), RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF121A17))
        Text(meta, style = MaterialTheme.typography.labelLarge, color = Color(0xFF5C584E))
    }
}

@Composable
fun PitchPulseBody() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Text(
                "DUMMY SCREEN — not real\n\nOTP 4821 for login\nValid 30s · do not share",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
fun PitchFocusBody() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Text("Hearing + seeing", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(
            "“Ship the deck by Monday, Priya has the figures.”\n\nFrame sample · Whisper · RulesExtract",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
fun PitchGalleryScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F0E8))
            .statusBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Recents", style = MaterialTheme.typography.headlineMedium)
            Text("Photos", style = MaterialTheme.typography.labelLarge, color = Color(0xFF5C584E))
        }
        Spacer(Modifier.weight(1f))
        Text("No Pulse capture here", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Screenshot was processed, then deleted.\nThe commitment lives as a text card.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF5C584E),
        )
        Spacer(Modifier.weight(1f))
    }
}
