package com.mneme.app.data

/**
 * Cheap rules to classify a note into a [CardType] before / without LLM.
 */
object CardClassifier {
    private val promise = Regex(
        """(?i)\b(i('?ll| will)\s+(send|pay|do|share|call)|promise|i\s+owe|i'll\s+get\s+back)\b""",
    )
    private val waiting = Regex(
        """(?i)\b(waiting\s+(on|for)|let\s+me\s+know\s+when|pending|still\s+need|when\s+you\s+(can|get)|awaiting)\b""",
    )
    private val askForYou = Regex(
        """(?i)\b(can\s+you|could\s+you|please\s+(send|share|confirm|check)|need\s+you\s+to|remind\s+me\s+to\s+ask)\b""",
    )

    fun classify(
        title: String,
        body: String,
        dueAt: Long?,
        who: String? = null,
    ): CardType {
        if (dueAt != null) return CardType.DUE
        val text = "$title\n$body"
        when {
            promise.containsMatchIn(text) -> return CardType.PROMISE
            waiting.containsMatchIn(text) -> return CardType.WAITING
            askForYou.containsMatchIn(text) -> return CardType.ASK_FOR_YOU
            !who.isNullOrBlank() && waiting.containsMatchIn(text) -> return CardType.WAITING
        }
        return CardType.FACT
    }
}
