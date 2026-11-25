package com.mneme.app.ai

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Calendar due dates only — never invent from clock times like 13:49 / 9:30.
 */
object DueDateGuess {
    private val MONTH_DATE = Regex(
        """(?i)\b(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\s+(\d{1,2})(?:,?\s*(\d{4}))?\b""",
    )
    private val ISO = Regex("""\b(20\d{2})-(\d{2})-(\d{2})\b""")
    private val SLASH_WITH_YEAR = Regex("""\b(\d{1,2})[/-](\d{1,2})[/-](20\d{2}|\d{2})\b""")
    private val DUE_NEAR_SLASH = Regex(
        """(?i)\b(?:due|deadline|submit(?:\s+by)?|by|on)\s+(\d{1,2})[/-](\d{1,2})(?:[/-](20\d{2}|\d{2}))?\b""",
    )
    private val CLOCK = Regex("""^\d{1,2}:\d{2}$""")

    fun guess(text: String, today: LocalDate = LocalDate.now()): Long? {
        ISO.find(text)?.let { m ->
            return localDateMillis(
                m.groupValues[1].toInt(),
                m.groupValues[2].toInt(),
                m.groupValues[3].toInt(),
            )
        }
        MONTH_DATE.find(text)?.let { m ->
            val month = monthNumber(m.groupValues[1]) ?: return@let
            val day = m.groupValues[2].toIntOrNull() ?: return@let
            val year = m.groupValues[3].toIntOrNull() ?: today.year
            return localDateMillis(year, month, day)
        }
        DUE_NEAR_SLASH.find(text)?.let { m ->
            val a = m.groupValues[1].toIntOrNull() ?: return@let
            val b = m.groupValues[2].toIntOrNull() ?: return@let
            val yRaw = m.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }
            val year = when {
                yRaw == null -> today.year
                yRaw.length == 2 -> 2000 + yRaw.toInt()
                else -> yRaw.toInt()
            }
            // Prefer D/M when day>12, else M/D (US) — clamp safely
            val (month, day) = if (a > 12) b to a else a to b
            return localDateMillis(year, month, day)
        }
        SLASH_WITH_YEAR.find(text)?.let { m ->
            val a = m.groupValues[1].toIntOrNull() ?: return@let
            val b = m.groupValues[2].toIntOrNull() ?: return@let
            val yRaw = m.groupValues[3]
            val year = if (yRaw.length == 2) 2000 + yRaw.toInt() else yRaw.toInt()
            val (month, day) = if (a > 12) b to a else a to b
            return localDateMillis(year, month, day)
        }
        return null
    }

    fun parseIsoOrEnglish(value: String?, today: LocalDate = LocalDate.now()): Long? {
        if (value.isNullOrBlank() || value == "null") return null
        val v = value.trim()
        if (CLOCK.matches(v)) return null
        // Reject bare times hidden in strings
        if (v.matches(Regex("""^\d{1,2}:\d{2}(\s*[AaPp][Mm])?$"""))) return null
        val formats = listOf("yyyy-MM-dd", "MMM d yyyy", "MMM d, yyyy", "MMMM d yyyy", "MMM d", "MMMM d")
        for (f in formats) {
            try {
                val formatter = DateTimeFormatter.ofPattern(f, Locale.ENGLISH)
                var d = LocalDate.parse(v, formatter)
                if (!f.contains("y")) {
                    d = LocalDate.of(today.year, d.month, d.dayOfMonth)
                }
                return d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: DateTimeParseException) {
            }
        }
        return guess(v, today)
    }

    fun normalizeYear(millis: Long?, today: LocalDate = LocalDate.now()): Long? {
        if (millis == null) return null
        val d = java.time.Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        if (kotlin.math.abs(d.year - today.year) <= 1) return millis
        val fixed = LocalDate.of(
            today.year,
            d.month,
            d.dayOfMonth.coerceAtMost(d.month.length(today.isLeapYear)),
        )
        return fixed.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun localDateMillis(year: Int, month: Int, day: Int): Long? {
        return try {
            LocalDate.of(year, month, day)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }

    private fun monthNumber(token: String): Int? {
        val t = token.lowercase().take(3)
        return when (t) {
            "jan" -> 1
            "feb" -> 2
            "mar" -> 3
            "apr" -> 4
            "may" -> 5
            "jun" -> 6
            "jul" -> 7
            "aug" -> 8
            "sep" -> 9
            "oct" -> 10
            "nov" -> 11
            "dec" -> 12
            else -> null
        }
    }
}
