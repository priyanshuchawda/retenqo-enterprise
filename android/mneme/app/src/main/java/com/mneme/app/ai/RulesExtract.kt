package com.mneme.app.ai

import com.mneme.app.data.CardClassifier
import com.mneme.app.data.CardType
import java.util.Locale

/**
 * Cheap structured extract — no LLM.
 * Prefer this path; only call Qwen when [RulesHit.needsLlm] is true.
 */
object RulesExtract {
    private val RUPEE = Regex(
        """(?i)(?:₹|rs\.?|inr)\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""",
    )
    private val PAY_BY = Regex(
        """(?i)\b(pay\s+by|due\s+by|submit\s+by|deadline|due\s+on|pay\s+before)\b""",
    )
    private val FROM_LINE = Regex(
        """(?i)^(?:from\s+)?([^:\n]{2,40}):\s*(.+)$""",
        RegexOption.MULTILINE,
    )
    private val REMIND = Regex("""(?i)\bremind\s+me\b""")

    data class RulesHit(
        val title: String,
        val body: String,
        val dueAt: Long?,
        val who: String?,
        val amountText: String?,
        val cardType: CardType,
        val confidence: Float,
        val needsLlm: Boolean,
        val embedText: String,
        val kind: String,
        val important: Boolean,
    ) {
        fun toExtractedNote(usedLlm: Boolean = false) = ExtractedNote(
            title = title,
            body = body,
            dueAt = dueAt,
            embedText = embedText,
            kind = kind,
            important = important,
            who = who,
            cardType = cardType.storage,
            confidence = confidence,
            usedLlm = usedLlm,
        )
    }

    fun extract(raw: String, todayHint: java.time.LocalDate = java.time.LocalDate.now()): RulesHit {
        val text = raw.trim()
        val dueAt = DueDateGuess.normalizeYear(DueDateGuess.guess(text, todayHint), todayHint)
        val amount = RUPEE.find(text)?.let { "₹${it.groupValues[1]}" }
        val who = guessWho(text)
        val title = guessTitle(text, who)
        val cardType = CardClassifier.classify(title, text, dueAt, who)
        val hasPayBy = PAY_BY.containsMatchIn(text)
        val hasRemind = REMIND.containsMatchIn(text)
        val strongSignal = dueAt != null || amount != null || hasPayBy ||
            cardType == CardType.PROMISE || cardType == CardType.WAITING ||
            cardType == CardType.ASK_FOR_YOU || hasRemind

        val confidence = when {
            dueAt != null && (amount != null || hasPayBy) -> 0.92f
            dueAt != null || amount != null -> 0.85f
            strongSignal && text.length < 280 -> 0.8f
            strongSignal -> 0.72f
            who != null && text.length in 20..160 -> 0.62f
            text.length < 80 && PulsePipelineHints.looksImportant(text) -> 0.55f
            else -> 0.4f
        }

        val body = buildBody(text, who, amount, dueAt != null)
        val kind = when (cardType) {
            CardType.DUE -> "deadline"
            CardType.PROMISE, CardType.WAITING, CardType.ASK_FOR_YOU -> "chat"
            CardType.FACT -> if (amount != null) "receipt" else "other"
        }
        val embed = buildString {
            append(title).append(". ")
            who?.let { append("Who: ").append(it).append(". ") }
            amount?.let { append("Amount: ").append(it).append(". ") }
            append(body.take(600))
        }.take(1200)

        return RulesHit(
            title = title.take(120),
            body = body.take(2_000),
            dueAt = dueAt,
            who = who,
            amountText = amount,
            cardType = cardType,
            confidence = confidence,
            needsLlm = confidence < 0.7f,
            embedText = embed,
            kind = kind,
            important = PulsePipelineHints.looksImportant(text) || strongSignal,
        )
    }

    private fun guessWho(text: String): String? {
        FROM_LINE.find(text)?.groupValues?.getOrNull(1)?.trim()?.let { name ->
            if (name.length in 2..40 && !name.contains("http", ignoreCase = true)) {
                return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
            }
        }
        return null
    }

    private fun guessTitle(text: String, who: String?): String {
        val line = text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() && !PulsePipelineHints.looksLikeClockOnlyTitle(it) }
            ?.take(80)
        if (!line.isNullOrBlank()) {
            // "From Priya: pay by Friday" → prefer useful snippet after colon
            val after = line.substringAfter(':', missingDelimiterValue = "").trim()
            if (after.length >= 8) return after.take(80)
            if (who != null && line.startsWith("From ", ignoreCase = true)) {
                return after.ifBlank { line }.take(80)
            }
            return line
        }
        return who?.let { "Note from $it" } ?: "Note"
    }

    private fun buildBody(
        text: String,
        who: String?,
        amount: String?,
        hasDue: Boolean,
    ): String = buildString {
        val cleaned = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && !PulsePipelineHints.looksLikeClockOnlyTitle(it) }
            .joinToString("\n")
            .ifBlank { text.trim() }
        append(cleaned.take(1_500))
        if (who != null && !cleaned.contains(who, ignoreCase = true)) {
            append("\nWho: ").append(who)
        }
        if (amount != null && !cleaned.contains(amount.removePrefix("₹"))) {
            append("\nAmount: ").append(amount)
        }
        if (hasDue) append("\n(Has due date)")
    }
}
