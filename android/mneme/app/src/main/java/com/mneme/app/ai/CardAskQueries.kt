package com.mneme.app.ai

import com.mneme.app.data.CardType
import com.mneme.app.data.NoteEntity
import java.text.DateFormat
import java.util.Date

/**
 * Structured Ask shortcuts over cards — no LLM when the question maps to card kinds.
 */
object CardAskQueries {
    private val owe = Regex(
        """(?i)\b(what\s+do\s+i\s+owe|what'?s?\s+owe|owings?|who\s+do\s+i\s+owe|money\s+i\s+owe|pay\s+back|unpaid)\b""",
    )
    private val waiting = Regex(
        """(?i)\b(waiting\s+on|who\s+am\s+i\s+waiting|still\s+waiting|pending\s+from|awaiting)\b""",
    )
    private val promises = Regex(
        """(?i)\b(my\s+promises?|what\s+did\s+i\s+promise|promises?\s+i\s+made|i\s+promised)\b""",
    )
    private val askForYou = Regex(
        """(?i)\b(asks?\s+for\s+me|asked\s+me\s+to|people\s+asking|requests?\s+for\s+me)\b""",
    )

    enum class Intent { OWE, WAITING, PROMISES, ASK_FOR_YOU }

    fun intent(question: String): Intent? {
        val q = question.trim()
        if (q.isEmpty()) return null
        return when {
            owe.containsMatchIn(q) -> Intent.OWE
            waiting.containsMatchIn(q) -> Intent.WAITING
            promises.containsMatchIn(q) -> Intent.PROMISES
            askForYou.containsMatchIn(q) -> Intent.ASK_FOR_YOU
            else -> null
        }
    }

    /** Card types to boost in semantic retrieval for a free-form question. */
    fun boostTypes(question: String): Set<CardType> {
        val q = question.lowercase()
        val out = mutableSetOf<CardType>()
        if (owe.containsMatchIn(q) || q.contains("due") || q.contains("deadline") || q.contains("₹") || q.contains("rs")) {
            out += CardType.DUE
        }
        if (waiting.containsMatchIn(q) || q.contains("waiting")) out += CardType.WAITING
        if (promises.containsMatchIn(q) || q.contains("promise")) out += CardType.PROMISE
        if (askForYou.containsMatchIn(q) || q.contains("asked me")) out += CardType.ASK_FOR_YOU
        return out
    }

    fun answerFromCards(
        intent: Intent,
        notes: List<NoteEntity>,
        now: Long = System.currentTimeMillis(),
    ): AskResult {
        val active = notes.filter { it.isActive(now) }
        val hits = when (intent) {
            Intent.OWE -> active.filter {
                it.type() == CardType.DUE || looksLikeMoney(it)
            }.sortedBy { it.dueAt ?: Long.MAX_VALUE }
            Intent.WAITING -> active.filter { it.type() == CardType.WAITING }
                .sortedBy { it.createdAt }
            Intent.PROMISES -> active.filter { it.type() == CardType.PROMISE }
                .sortedByDescending { it.createdAt }
            Intent.ASK_FOR_YOU -> active.filter { it.type() == CardType.ASK_FOR_YOU }
                .sortedByDescending { it.createdAt }
        }.take(12)

        if (hits.isEmpty()) {
            return AskResult(AskAnswerGuard.REFUSE, emptyList())
        }

        val header = when (intent) {
            Intent.OWE -> "You owe / have due:"
            Intent.WAITING -> "Still waiting:"
            Intent.PROMISES -> "Your open promises:"
            Intent.ASK_FOR_YOU -> "People asked you:"
        }
        val body = hits.mapIndexed { i, n ->
            val who = n.who?.takeIf { it.isNotBlank() }?.let { " — $it" }.orEmpty()
            val due = n.dueAt?.let {
                " (due ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it))})"
            }.orEmpty()
            "[${i + 1}] ${n.title}$who$due"
        }.joinToString("\n")

        return AskResult("$header\n$body", hits)
    }

    fun formatCardContext(hits: List<NoteEntity>): String =
        hits.mapIndexed { i, n ->
            val whenStr = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(n.createdAt))
            val who = n.who?.takeIf { it.isNotBlank() } ?: "-"
            val due = n.dueAt?.let {
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it))
            } ?: "-"
            buildString {
                append("[${i + 1}] type=${n.type().storage} status=${n.status().storage}")
                append(" who=$who due=$due source=${n.source} saved=$whenStr\n")
                append("${n.title}\n${n.body}")
            }
        }.joinToString("\n\n")

    private fun looksLikeMoney(n: NoteEntity): Boolean {
        val t = "${n.title}\n${n.body}"
        return Regex("""(?i)(₹|rs\.?\s*\d|inr|owe|pay\s+me|pay\s+you|refund)""").containsMatchIn(t)
    }
}
