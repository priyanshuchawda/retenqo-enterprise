package com.mneme.app.ai

import com.mneme.app.data.CardClassifier
import com.mneme.app.data.CardType
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class ExtractedNote(
    val title: String,
    val body: String,
    val dueAt: Long?,
    /** Text used for embeddings / RAG (cleaner than raw OCR). */
    val embedText: String = body,
    val kind: String = "other",
    val important: Boolean = true,
    val who: String? = null,
    /** [com.mneme.app.data.CardType.storage] */
    val cardType: String = CardType.FACT.storage,
    val confidence: Float = 1f,
    val usedLlm: Boolean = false,
)

/**
 * Text → structured memory card.
 *
 * Pipeline: deterministic [RulesExtract] first; use a local model only when one is available.
 */
object NoteExtractor {
    suspend fun extract(llm: LlmBridge?, ocrText: String): ExtractedNote {
        val rules = RulesExtract.extract(ocrText)
        if (!rules.needsLlm) {
            return rules.toExtractedNote(usedLlm = false)
        }
        if (llm == null) {
            return rules.toExtractedNote(usedLlm = false)
        }

        val today = LocalDate.now()
        val prompt = """
            Extract ONE personal memory card as JSON only (no markdown, no thinking).
            Keys:
            - title (short, searchable — prefer group/person/topic; NEVER a clock time alone like "18:54")
            - kind (meeting|deadline|receipt|chat|reminder|other)
            - people (array of names)
            - chat_with (person or group name, or null)
            - body (2-4 cleaned sentences: who / what / why — no OCR junk, no UI chrome, no status bar times)
            - when_text (e.g. "9:30" or "Thu 4pm" or null — times without a calendar date)
            - due_date (YYYY-MM-DD only if a real calendar date is present; else null)
            - tags (2-5 keywords)
            - important (true for meet/deadline/money/notice/exam; false for casual chat)
            - embed_text (one clear English paragraph for search: names + facts + times)
            Today is $today. If a date has no year, use ${today.year}.
            Do NOT invent a due_date from clock times like 13:49 or 9:30 alone.
            If the text is mostly an app's own UI (settings, home screen chrome), set important=false.
            Text:
            ${ocrText.take(1400)}
            /no_think
        """.trimIndent()

        return try {
            llm.ensureLoaded()
            val raw = llm.complete(prompt, maxTokens = 220)
            mergeWithRules(parseJson(raw, ocrText), rules)
        } catch (_: Exception) {
            rules.toExtractedNote(usedLlm = false)
        }
    }

    /** Rules-only path (tests / no LLM). */
    fun fallback(ocrText: String): ExtractedNote =
        RulesExtract.extract(ocrText).toExtractedNote(usedLlm = false)

    fun parseJson(raw: String, fallbackText: String): ExtractedNote {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return fallback(fallbackText)
        return try {
            val obj = JSONObject(raw.substring(start, end + 1))
            val title = obj.optString("title").ifBlank {
                fallbackText.lineSequence().firstOrNull { it.isNotBlank() }?.take(80) ?: "Note"
            }.let { t ->
                if (PulsePipelineHints.looksLikeClockOnlyTitle(t)) {
                    fallbackText.lineSequence()
                        .map { it.trim() }
                        .firstOrNull { it.isNotBlank() && !PulsePipelineHints.looksLikeClockOnlyTitle(it) }
                        ?.take(80) ?: "Screen note"
                } else t
            }
            val kind = obj.optString("kind").ifBlank { "other" }
            val chatWith = obj.optString("chat_with").takeIf { it.isNotBlank() && it != "null" }
            val people = jsonStringList(obj.opt("people"))
            val whenText = obj.optString("when_text").takeIf { it.isNotBlank() && it != "null" }
            val tags = jsonStringList(obj.opt("tags"))
            val bodyRaw = when (val b = obj.opt("body")) {
                is JSONArray -> (0 until b.length()).joinToString(" ") { b.optString(it) }
                is String -> b
                else -> obj.optString("body")
            }.ifBlank { fallbackText.trim() }
            val embedText = obj.optString("embed_text").ifBlank {
                buildEmbedText(title, kind, chatWith, people, bodyRaw, whenText, tags)
            }
            val due = obj.optString("due_date").takeIf { it.isNotBlank() && it != "null" }
            val important = if (obj.has("important")) obj.optBoolean("important", true) else true

            val storedBody = formatStoredBody(
                kind = kind,
                chatWith = chatWith,
                people = people,
                body = bodyRaw,
                whenText = whenText,
                tags = tags,
            )

            val dueAt = DueDateGuess.normalizeYear(DueDateGuess.parseIsoOrEnglish(due))
                ?: DueDateGuess.guess(fallbackText)

            val who = chatWith ?: people.firstOrNull()
            val cardType = CardClassifier.classify(title, storedBody, dueAt, who)

            ExtractedNote(
                title = title.take(120),
                body = storedBody,
                dueAt = dueAt,
                embedText = embedText.take(1200),
                kind = kind,
                important = important,
                who = who,
                cardType = cardType.storage,
                confidence = 0.75f,
                usedLlm = true,
            )
        } catch (_: Exception) {
            fallback(fallbackText)
        }
    }

    /** Prefer rule-anchored due / amount / who when the model is vague. */
    internal fun mergeWithRules(llm: ExtractedNote, rules: RulesExtract.RulesHit): ExtractedNote {
        val dueAt = rules.dueAt ?: llm.dueAt
        val who = rules.who ?: llm.who
        val body = buildString {
            append(llm.body.trim())
            if (rules.amountText != null && !llm.body.contains(rules.amountText.removePrefix("₹"))) {
                append("\nAmount: ").append(rules.amountText)
            }
        }
        val cardType = CardClassifier.classify(llm.title, body, dueAt, who)
        return llm.copy(
            dueAt = dueAt,
            who = who,
            body = body,
            cardType = cardType.storage,
            confidence = maxOf(llm.confidence, rules.confidence),
            usedLlm = true,
            embedText = llm.embedText.ifBlank { rules.embedText },
            important = llm.important || rules.important,
        )
    }

    private fun jsonStringList(any: Any?): List<String> = when (any) {
        is JSONArray -> (0 until any.length()).mapNotNull {
            any.optString(it).takeIf { s -> s.isNotBlank() && s != "null" }
        }
        is String -> any.split(',').map { it.trim() }.filter { it.isNotBlank() }
        else -> emptyList()
    }

    private fun formatStoredBody(
        kind: String,
        chatWith: String?,
        people: List<String>,
        body: String,
        whenText: String?,
        tags: List<String>,
    ): String = buildString {
        append('[').append(kind).append(']')
        if (!chatWith.isNullOrBlank()) append(' ').append(chatWith)
        append('\n').append(body.trim())
        if (!whenText.isNullOrBlank()) append("\nWhen: ").append(whenText)
        if (people.isNotEmpty()) append("\nPeople: ").append(people.joinToString(", "))
        if (tags.isNotEmpty()) append("\nTags: ").append(tags.take(5).joinToString(", "))
    }

    private fun buildEmbedText(
        title: String,
        kind: String,
        chatWith: String?,
        people: List<String>,
        body: String,
        whenText: String?,
        tags: List<String>,
    ): String = buildString {
        append(title).append(". ")
        append(kind).append(". ")
        if (!chatWith.isNullOrBlank()) append("Chat: ").append(chatWith).append(". ")
        if (people.isNotEmpty()) append("People: ").append(people.joinToString(", ")).append(". ")
        append(body.trim())
        if (!whenText.isNullOrBlank()) append(" When: ").append(whenText).append('.')
        if (tags.isNotEmpty()) append(" Tags: ").append(tags.take(5).joinToString(", ")).append('.')
    }.take(1200)
}
