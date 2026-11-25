package com.mneme.app.notif

import com.mneme.app.ai.PulsePipelineHints

/**
 * Notification text triage. Third-party app notifications are accepted after
 * Notification Access is granted; known system noise is still discarded.
 */
object NotifTextTriage {
    /** package → short source label for the note */
    private val ALLOWED = mapOf(
        "com.whatsapp" to "WA",
        "com.whatsapp.w4b" to "WA",
        "com.google.android.gm" to "Gmail",
        "com.google.android.apps.gmail" to "Gmail",
        "com.google.android.apps.messaging" to "SMS",
        "com.android.mms" to "SMS",
        "com.samsung.android.messaging" to "SMS",
        "org.telegram.messenger" to "Telegram",
        "org.telegram.messenger.web" to "Telegram",
        "com.microsoft.office.outlook" to "Outlook",
        "com.google.android.calendar" to "Calendar",
        "com.android.calendar" to "Calendar",
        "com.Slack" to "Slack",
        "com.slack" to "Slack",
    )

    private val SYSTEM_NOISE = Regex(
        """(?i)(checking for new messages|backup in progress|whatsapp web is currently|missed (voice|video) call|ongoing (voice|video) call|calling…|ringing…|you may have new (notifications|messages)|tap to (load|download)|this message was deleted|waiting for this message|download complete|charging|battery (full|low)|screenshot captured|connected to|wifi|bluetooth|software update|otp is|is your (otp|code)|verification code|one[- ]time (password|code))""",
    )

    private val MEDIA_ONLY = Regex(
        """(?i)^(📷|🎥|🎙|🎵|📄|sticker|gif|contact card|location)$""",
    )

    private val SYSTEM_PACKAGES = Regex(
        """^(android|com\.android\.systemui|com\.samsung\.android\.systemui|com\.mneme\.app(?:\.debug)?)$""",
    )

    /** @deprecated prefer [sourceLabel]; kept for tests / WA-only checks */
    fun isWhatsApp(packageName: String?): Boolean =
        packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b"

    fun sourceLabel(packageName: String?): String? =
        packageName?.takeUnless { SYSTEM_PACKAGES.matches(it) }?.let { ALLOWED[it] ?: it }

    fun isAllowed(packageName: String?): Boolean = sourceLabel(packageName) != null

    data class Candidate(
        val sourceLabel: String,
        val text: String,
    )

    /**
     * Returns text worth queuing for ingest, or null to ignore.
     */
    fun candidate(packageName: String?, title: String?, body: String?): Candidate? {
        val source = sourceLabel(packageName) ?: return null
        return candidate(title, body)?.let { Candidate(source, it) }
    }

    /** Package-agnostic text filter (tests + shared). */
    fun candidate(title: String?, body: String?): String? {
        val t = title?.trim().orEmpty()
        val b = body?.trim().orEmpty()
        if (b.isEmpty() && t.isEmpty()) return null
        if (SYSTEM_NOISE.containsMatchIn("$t $b")) return null
        if (b.isNotEmpty() && MEDIA_ONLY.containsMatchIn(b)) return null
        val combined = when {
            t.isNotEmpty() && b.isNotEmpty() -> "From $t: $b"
            b.isNotEmpty() -> b
            else -> t
        }
        if (combined.length < 12) return null
        if (PulsePipelineHints.shouldSkipIngest(combined)) return null
        return combined.take(1_200)
    }
}
