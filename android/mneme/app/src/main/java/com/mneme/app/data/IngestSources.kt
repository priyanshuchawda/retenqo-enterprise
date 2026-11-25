package com.mneme.app.data

/**
 * Stable source labels written on cards (citations / timeline).
 */
object IngestSources {
    const val PULSE = "Pulse"
    const val WA_NOTIF = "WA notif"
    const val WA_MEDIA = "WA media"
    const val GMAIL_NOTIF = "Gmail notif"
    const val SMS_NOTIF = "SMS notif"
    const val TELEGRAM_NOTIF = "Telegram notif"
    const val CALENDAR_NOTIF = "Calendar notif"
    const val OUTLOOK_NOTIF = "Outlook notif"
    const val SLACK_NOTIF = "Slack notif"
    const val DEMO = "Demo"

    fun fromNotifLabel(short: String): String = when (short) {
        "WA" -> WA_NOTIF
        "Gmail" -> GMAIL_NOTIF
        "SMS" -> SMS_NOTIF
        "Telegram" -> TELEGRAM_NOTIF
        "Calendar" -> CALENDAR_NOTIF
        "Outlook" -> OUTLOOK_NOTIF
        "Slack" -> SLACK_NOTIF
        else -> "$short notif"
    }

    fun waMedia(fileName: String): String =
        "$WA_MEDIA · ${fileName.take(40)}"

    /** Fingerprint for dedup — ignores source so Pulse + WA don't double-save the same text. */
    fun contentFingerprint(title: String, body: String): String {
        val norm = "$title\n$body"
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()
        return sha256(norm)
    }

    /** Hash a platform event key so cards can be deduped without retaining the raw notification key. */
    fun notificationEventFingerprint(packageName: String, eventKey: String): String =
        sha256("notification\n${packageName.trim()}\n${eventKey.trim()}")

    private fun sha256(text: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
