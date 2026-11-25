package com.mneme.app.data

import com.mneme.app.ai.HashEmbedder
import com.mneme.app.ai.MnemeEmbedder
import com.mneme.app.ai.CardAskQueries
import com.mneme.app.ai.CompletionEvidenceAgent
import com.mneme.app.ai.LlmBridge

data class ScoredNote(
    val note: NoteEntity,
    val score: Float,
)

class NoteRepository(
    private val dao: NoteDao,
    private val embedder: MnemeEmbedder? = null,
    private val followThroughDao: FollowThroughDao? = null,
) {
    fun observeNotes() = dao.observeAll()
    fun observeFollowThroughSuggestions() = followThroughDao?.observePending()

    suspend fun getAll() = dao.getAll()

    suspend fun search(query: String) = dao.search(query.trim())

    suspend fun hasSeenSourceEvent(sourceEventId: String): Boolean =
        dao.countSourceEventId(sourceEventId) > 0

    /** Save a source-backed completion candidate; never marks the commitment done automatically. */
    suspend fun suggestCompletion(evidence: String, source: String, llm: LlmBridge? = null): Long? {
        val store = followThroughDao ?: return null
        val match = CompletionEvidenceAgent.find(llm, evidence, dao.getAll()) ?: return null
        val boundedEvidence = evidence.trim().take(1_200)
        val hash = IngestSources.contentFingerprint(source, boundedEvidence)
        val suggestionId = store.insertSuggestionWithHistory(
            FollowThroughSuggestion(
                commitmentId = match.commitment.id,
                evidenceSource = source.take(120),
                evidenceText = boundedEvidence,
                evidenceHash = hash,
                confidence = match.confidence,
            ),
        )
        if (suggestionId <= 0L) return null
        return suggestionId
    }

    suspend fun acceptCompletionSuggestion(id: Long, commitmentId: Long) {
        val store = followThroughDao ?: return
        // Keep the supplied id pairing honest and make accepting/closing atomic.
        if (store.pendingById(id)?.commitmentId != commitmentId) return
        store.acceptAndComplete(id, System.currentTimeMillis())
    }

    suspend fun dismissCompletionSuggestion(id: Long) {
        followThroughDao?.setSuggestionStateAndRecord(id, FollowThroughSuggestion.State.DISMISSED, System.currentTimeMillis())
    }

    suspend fun retrieve(query: String, limit: Int = 5): List<NoteEntity> =
        retrieveScored(query, limit).map { it.note }

    /**
     * Semantic + keyword retrieval with scores for Ask relevance gating.
     * Prefers active cards; boosts types matching the question intent.
     */
    suspend fun retrieveScored(query: String, limit: Int = 5): List<ScoredNote> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()

        val now = System.currentTimeMillis()
        val boost = CardAskQueries.boostTypes(q)
        // MnemeEmbedder currently writes hashed n-gram vectors, not learned
        // sentence embeddings. Their cosine scores collide on short queries,
        // so they must not be used to decide which personal cards to cite.
        val queryTokens = MemoryLexicalSearch.tokens(q)
        val candidateLimit = (limit.coerceAtLeast(1) * CANDIDATE_MULTIPLIER).coerceAtMost(MAX_RETRIEVAL_CANDIDATES)
        val scored = dao.recentCandidates(candidateLimit).asSequence()
            .filter { it.review() != CardReviewState.PENDING }
            .mapNotNull { note ->
                var score = MemoryLexicalSearch.score(
                    queryTokens,
                    "${note.title} ${note.body} ${note.who.orEmpty()}",
                )
                if (!note.isActive(now)) score *= 0.35f
                if (note.type() in boost) score *= 1.28f
                if (score >= MIN_LEXICAL_SCORE) ScoredNote(note, score) else null
            }.sortedByDescending { it.score }

        return scored.toList().take(limit)
    }

    /**
     * Saves a card unless the same content hash was saved in the last 10 minutes.
     * @param embedText preferred text for the vector (cleaner than raw OCR body)
     * @return inserted id, or null if duplicate skipped
     */
    suspend fun saveIfNew(
        title: String,
        body: String,
        source: String,
        dueAt: Long? = null,
        embedText: String? = null,
        who: String? = null,
        cardType: CardType? = null,
        confidence: Float = 1f,
        requiresReview: Boolean = false,
        sourceEventId: String? = null,
    ): Long? {
        if (sourceEventId != null && dao.countSourceEventId(sourceEventId) > 0) return null
        val forEmbed = (embedText?.takeIf { it.isNotBlank() } ?: "$title\n$body")
        val vec = embed(forEmbed)
        val type = cardType ?: CardClassifier.classify(title, body, dueAt, who)
        val hash = IngestSources.contentFingerprint(
            title.ifBlank { body.lineSequence().firstOrNull()?.take(80) ?: "Note" },
            body,
        )
        val createdAt = System.currentTimeMillis()
        val since = createdAt - 10 * 60_000L
        if (dao.countRecentHash(hash, since) > 0) return null
        val entity = NoteEntity(
            title = title.ifBlank { body.lineSequence().firstOrNull()?.take(80) ?: "Note" },
            body = body,
            source = source,
            createdAt = createdAt,
            dueAt = dueAt,
            contentHash = hash,
            embedding = toBytes(vec),
            cardType = type.storage,
            cardStatus = CardStatus.OPEN.storage,
            who = who?.takeIf { it.isNotBlank() },
            confidence = confidence.coerceIn(0f, 1f),
            reviewState = if (requiresReview) CardReviewState.PENDING.storage else CardReviewState.CONFIRMED.storage,
            sourceEventId = sourceEventId,
        )
        val id = dao.insertNoteWithHistory(
            entity,
            CommitmentHistoryEvent(
                noteId = 0,
                eventType = CommitmentHistoryEvent.Type.CAPTURED,
                summary = if (requiresReview) "Captured as a suggestion; waiting for user review." else "Saved as a commitment or memory card.",
                source = source.take(120),
                evidenceText = body.take(1_200),
                createdAt = createdAt,
            ),
        )
        return id.takeIf { it > 0L }
    }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun deleteAll() = dao.deleteAllNotes()

    suspend fun acceptReview(id: Long): NoteEntity? {
        val note = dao.getById(id) ?: return null
        val acceptedAt = System.currentTimeMillis()
        val changed = dao.updateReviewWithHistory(
            id,
            CardReviewState.CONFIRMED.storage,
            CommitmentHistoryEvent(
                noteId = id,
                eventType = CommitmentHistoryEvent.Type.REVIEW_ACCEPTED,
                summary = "User accepted this captured card as a commitment.",
                source = "User review",
                evidenceText = note.body.take(1_200),
                createdAt = acceptedAt,
            ),
        )
        return if (changed > 0) dao.getById(id) else null
    }

    suspend fun dismissReview(id: Long) {
        val note = dao.getById(id) ?: return
        dao.updateReviewWithHistory(
            id,
            CardReviewState.DISMISSED.storage,
            CommitmentHistoryEvent(
                noteId = id,
                eventType = CommitmentHistoryEvent.Type.REVIEW_DISMISSED,
                summary = "User dismissed this as a task; the source remains in memory.",
                source = "User review",
                evidenceText = note.body.take(1_200),
            ),
        )
    }

    suspend fun markDone(id: Long) {
        updateStatus(id, CardStatus.DONE)
    }

    suspend fun markOpen(id: Long) {
        updateStatus(id, CardStatus.OPEN)
    }

    /** Snooze until [untilMs]; card stays out of active dues until then. */
    suspend fun snooze(id: Long, untilMs: Long) {
        updateStatus(id, CardStatus.SNOOZED, untilMs)
    }

    /** Update the editable task fields and keep the index/hash data in sync. */
    suspend fun updateCardFields(
        id: Long,
        title: String,
        body: String,
        who: String?,
        dueAt: Long?,
        cardType: CardType,
    ): NoteEntity? {
        val cleanTitle = title.trim()
        val cleanBody = body.trim()
        require(cleanTitle.isNotBlank()) { "Task title cannot be empty" }
        val cleanWho = who?.trim()?.takeIf(String::isNotBlank)
        val hash = IngestSources.contentFingerprint(cleanTitle, cleanBody)
        val vector = embed("$cleanTitle\n$cleanBody")
        val old = dao.getById(id) ?: return null
        val changed = buildList {
            if (old.title != cleanTitle) add("title")
            if (old.body != cleanBody) add("details")
            if (old.who != cleanWho) add("person")
            if (old.dueAt != dueAt) add("due date")
            if (old.cardType != cardType.storage) add("type")
        }
        val summary = if (changed.isEmpty()) "Task details reviewed; no fields changed." else "Updated ${changed.joinToString()}"
        dao.updateCardFieldsWithHistory(
            id = id,
            title = cleanTitle,
            body = cleanBody,
            who = cleanWho,
            dueAt = dueAt,
            cardType = cardType.storage,
            contentHash = hash,
            embedding = toBytes(vector),
            event = CommitmentHistoryEvent(
                noteId = id,
                eventType = CommitmentHistoryEvent.Type.EDITED,
                summary = summary,
                source = "User edit",
                evidenceText = "$cleanTitle\n$cleanBody".take(1_200),
            ),
        )
        return dao.getById(id)
    }

    suspend fun dueBetween(startMs: Long, endMs: Long): List<NoteEntity> =
        dao.dueBetween(startMs, endMs, System.currentTimeMillis())

    suspend fun history(noteId: Long): List<CommitmentHistoryEvent> = dao.recentHistory(noteId)

    private suspend fun updateStatus(id: Long, status: CardStatus, snoozedUntil: Long? = null) {
        val now = System.currentTimeMillis()
        val eventType = when (status) {
            CardStatus.DONE -> CommitmentHistoryEvent.Type.COMPLETED
            CardStatus.OPEN -> CommitmentHistoryEvent.Type.REOPENED
            CardStatus.SNOOZED -> CommitmentHistoryEvent.Type.SNOOZED
            CardStatus.EXPIRED -> CommitmentHistoryEvent.Type.BASELINE
        }
        val summary = when (status) {
            CardStatus.DONE -> "User marked this commitment done."
            CardStatus.OPEN -> "User reopened this commitment."
            CardStatus.SNOOZED -> "User snoozed this commitment until later."
            CardStatus.EXPIRED -> "Commitment expired."
        }
        val note = dao.getById(id) ?: return
        dao.updateStatusWithHistory(
            id,
            status.storage,
            now,
            snoozedUntil,
            CommitmentHistoryEvent(
                noteId = id,
                eventType = eventType,
                summary = summary,
                source = "User action · ${note.source}".take(160),
                createdAt = now,
            ),
        )
    }

    private suspend fun embed(text: String): FloatArray =
        embedder?.embed(text) ?: HashEmbedder.embed(text)

    private fun toBytes(vec: FloatArray): ByteArray =
        embedder?.toBytes(vec) ?: HashEmbedder.toBytes(vec)

    private fun fromBytes(bytes: ByteArray?): FloatArray? =
        embedder?.fromBytes(bytes) ?: HashEmbedder.fromBytes(bytes)

    private fun cosine(a: FloatArray, b: FloatArray): Float =
        embedder?.cosine(a, b) ?: HashEmbedder.cosine(a, b)

    companion object {
        private const val MIN_LEXICAL_SCORE = 0.25f
        private const val CANDIDATE_MULTIPLIER = 20
        private const val MAX_RETRIEVAL_CANDIDATES = 1_000
    }
}

/** Token overlap only; hashed n-gram vectors are not valid semantic similarity scores. */
internal object MemoryLexicalSearch {
    private val stopWords = setOf(
        "about", "after", "again", "also", "and", "are", "before", "between", "did", "does",
        "for", "from", "have", "how", "into", "just", "like", "much", "need", "please", "that",
        "the", "their", "them", "there", "these", "they", "this", "those", "through", "today",
        "tomorrow", "was", "were", "what", "when", "where", "which", "while", "who", "will",
        "with", "would", "you", "your", "our", "out", "over", "under", "then", "than",
    )

    fun tokens(text: String): Set<String> = text.lowercase()
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.length > 2 && it !in stopWords }
        .toSet()

    fun score(queryTokens: Set<String>, noteText: String): Float {
        if (queryTokens.isEmpty()) return 0f
        val noteTokens = tokens(noteText)
        val hits = queryTokens.count(noteTokens::contains)
        return hits.toFloat() / queryTokens.size
    }
}
