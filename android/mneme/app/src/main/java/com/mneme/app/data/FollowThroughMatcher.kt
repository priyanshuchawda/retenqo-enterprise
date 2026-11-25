package com.mneme.app.data

/** Conservative lexical matcher for completion evidence. It proposes; it never changes task state. */
object FollowThroughMatcher {
    private val completionLanguage = Regex(
        """(?i)\b(sent|send\s+you|shared|share\s+you|delivered|submitted|uploaded|finished|completed|done|received|got\s+it|all\s+set|sorted|thanks\s+for\s+(sending|sharing))\b""",
    )

    data class Match(val commitment: NoteEntity, val confidence: Float)

    fun find(evidence: String, commitments: List<NoteEntity>): Match? {
        if (!completionLanguage.containsMatchIn(evidence)) return null
        val evidenceTokens = MemoryLexicalSearch.tokens(evidence)
        if (evidenceTokens.isEmpty()) return null

        return commitments.asSequence()
            .filter { it.isActive() && it.type() in setOf(CardType.PROMISE, CardType.DUE, CardType.ASK_FOR_YOU, CardType.WAITING) }
            .mapNotNull { note ->
                val commitmentTokens = MemoryLexicalSearch.tokens("${note.title} ${note.body}")
                    .filterNot { it in GENERIC_COMMITMENT_WORDS }
                    .toSet()
                val shared = evidenceTokens.intersect(commitmentTokens)
                // A shared concrete subject is required. Generic completion language alone is weak.
                if (shared.isEmpty()) return@mapNotNull null
                val confidence = (0.58f + 0.12f * shared.size.coerceAtMost(3)).coerceAtMost(0.94f)
                Match(note, confidence)
            }
            .maxByOrNull { it.confidence }
    }

    private val GENERIC_COMMITMENT_WORDS = setOf(
        "send", "sent", "share", "shared", "please", "need", "could", "would", "will", "can",
        "you", "your", "me", "them", "they", "from", "with", "about", "when", "what",
    )
}
