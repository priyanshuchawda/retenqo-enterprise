package com.mneme.app.ai

import com.mneme.app.data.NoteEntity

data class GuardedAsk(
    val answer: String,
    val citations: List<NoteEntity>,
)

/**
 * Post-process Ask answers so we refuse instead of hallucinating.
 */
object AskAnswerGuard {
    const val REFUSE = "I don't have that in your memory."

    fun sanitize(raw: String, citations: List<NoteEntity>): GuardedAsk {
        val cleaned = stripNoise(raw)
        if (citations.isEmpty()) {
            return GuardedAsk(REFUSE, emptyList())
        }
        if (looksLikeRefuse(cleaned)) {
            return GuardedAsk(REFUSE, emptyList())
        }
        val citedIds = citationIds(cleaned)
        val availableIds = citations.indices.map { it + 1 }.toSet()
        if (citedIds.isEmpty() || citedIds.any { it !in availableIds }) {
            // Model claimed an answer without grounding — refuse
            return GuardedAsk(REFUSE, emptyList())
        }
        // Drop obviously invented multi-paragraph essays
        val oneLine = cleaned.lineSequence().firstOrNull { it.isNotBlank() }?.trim() ?: REFUSE
        val referenced = citedIds.map { citations[it - 1] }.distinctBy { it.id }
        return GuardedAsk(oneLine.take(400), referenced)
    }

    fun looksLikeRefuse(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("don't have that in your memory") ||
            t.contains("do not have that in your memory") ||
            t.contains("not in your memory") ||
            t.contains("i don't know") ||
            t.contains("no information") ||
            t.contains("cannot find") ||
            t.contains("can't find")
    }

    fun hasCitationMarker(text: String): Boolean =
        Regex("""\[\d+]""").containsMatchIn(text)

    /** Returns distinct positive citation numbers in appearance order. */
    fun citationIds(text: String): List<Int> =
        Regex("""\[(\d+)]""").findAll(text)
            .mapNotNull { it.groupValues[1].toIntOrNull()?.takeIf { id -> id > 0 } }
            .distinct()
            .toList()

    private fun stripNoise(raw: String): String =
        raw
            .replace(Regex("(?is)<think>.*?</think>"), " ")
            .replace(Regex("(?i)/no_think"), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
}
