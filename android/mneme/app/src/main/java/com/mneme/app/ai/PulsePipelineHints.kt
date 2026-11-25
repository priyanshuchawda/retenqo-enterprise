package com.mneme.app.ai

/**
 * Shared filters for Pulse / WA media / notifications.
 * Pure Kotlin — unit-tested without Android.
 */
object PulsePipelineHints {
    private val SAVE_HINTS = Regex(
        """(?i)(due|deadline|submit|exam|chapter|meeting|meet|homework|reminder|tomorrow|notice|audition|festival|lab|cie|₹|rs\.?\s*\d|paid|uber|receipt|certificate|invoice|assignment|aug\s+\d|sept?\s+\d|oct\s+\d|nov\s+\d|dec\s+\d|jan\s+\d|feb\s+\d|mar\s+\d|apr\s+\d|may\s+\d|jun\s+\d|jul\s+\d|@|\.com|https?://)""",
    )

    private val SELF_UI = Regex(
        """(?i)(your private memory stays|search memory|ask only from what you saved|hold mic to ask|ask memory|cited notes|answered from|enable notifs|scan wa|finish setup|notification access|due reminders|whatsapp media|retenqo settings)""",
    )

    private val BROWSER_OR_AI = Regex(
        """(?i)(\bchrome\b|\bfirefox\b|\bsafari\b|new tab|incognito|search google|chatgpt|openai|\bclaude\b|anthropic|gemini apps|copilot|regenerate response|ask claude|you said\s*$)""",
    )

    private val STATUS_LINE = Regex("""^\s*(\d{1,2}:\d{2}(\s*[AP]M)?|100%|\d{1,3}%|LTE|5G|Wi-?Fi)\s*$""", RegexOption.IGNORE_CASE)

    private val CASUAL = setOf(
        "ok", "okay", "thanks", "thank you", "lol", "haha", "yes", "no", "k", "kk", "hmm", "nice",
    )

    fun looksLikeSelfUi(text: String): Boolean {
        val t = text.trim()
        if (t.isEmpty()) return false
        val hits = SELF_UI.findAll(t).count()
        val lower = t.lowercase()
        val brandHits = Regex("""(?i)\b(retenqo|mneme)\b""").findAll(t).count()
        return hits >= 1 || (brandHits >= 2 && "pulse" in lower)
    }

    fun looksLikeBrowserOrAiChrome(text: String): Boolean {
        val t = text.trim()
        if (t.length < 8) return false
        val hits = BROWSER_OR_AI.findAll(t).count()
        if (hits >= 2) return true
        // Single strong marker + short body → screenshot chrome
        if (hits == 1 && t.length < 120 && !SAVE_HINTS.containsMatchIn(t)) return true
        return false
    }

    /** Mostly status-bar / clock OCR with no real content. */
    fun looksLikeStatusBarJunk(text: String): Boolean {
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        if (lines.isEmpty()) return true
        val statusish = lines.count { STATUS_LINE.matches(it) || it.matches(Regex("""^\d{1,2}:\d{2}$""")) }
        return statusish >= lines.size - 1 && lines.size <= 4 && !SAVE_HINTS.containsMatchIn(text)
    }

    fun looksLikeClockOnlyTitle(title: String): Boolean =
        title.trim().matches(Regex("""^\d{1,2}:\d{2}$"""))

    /**
     * True when content should not become a memory (any path).
     */
    fun shouldSkipIngest(text: String): Boolean {
        val t = text.trim()
        if (t.length < 10) return true
        if (looksLikeSelfUi(t)) return true
        if (looksLikeBrowserOrAiChrome(t)) return true
        if (looksLikeStatusBarJunk(t)) return true
        return false
    }

    fun looksImportant(text: String): Boolean {
        val t = text.trim()
        if (shouldSkipIngest(t)) return false
        val lower = t.lowercase()
        if (lower in CASUAL) return false
        // Clock-only fragments are not important
        if (t.matches(Regex("""^[\d:\sAPMapm%]+$"""))) return false
        return SAVE_HINTS.containsMatchIn(t) || t.length >= 48
    }
}
