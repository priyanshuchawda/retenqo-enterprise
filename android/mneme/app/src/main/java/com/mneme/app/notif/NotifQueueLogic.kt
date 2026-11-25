package com.mneme.app.notif

/**
 * Pure queue ops. New entries keep consent identity separate from the card display source.
 */
object NotifQueueLogic {
    private const val SEP = '\u001f'

    data class Entry(
        val source: String,
        val text: String,
        val sourceEventId: String? = null,
        val sourceId: String? = null,
    )

    data class Claim(val batch: List<String>, val pending: String, val inFlight: String)

    fun encode(source: String, text: String, sourceEventId: String? = null): String =
        if (sourceEventId.isNullOrBlank()) "$source$SEP$text" else "$source$SEP$text$SEP$sourceEventId"

    fun encodeStable(sourceId: String, source: String, text: String, sourceEventId: String? = null): String =
        listOfNotNull("v2", sourceId, source, text, sourceEventId?.takeIf(String::isNotBlank))
            .joinToString(SEP.toString())

    fun decodeEntry(entry: String): Entry {
        if (entry.startsWith("v2$SEP")) return decodeStableEntry(entry)
        val i = entry.indexOf(SEP)
        if (i <= 0) return Entry("Notif", entry)
        val eventSeparator = entry.lastIndexOf(SEP)
        val eventId = entry.substring(eventSeparator + 1).takeIf { eventSeparator > i && it.matches(Regex("[a-fA-F0-9]{64}")) }
        val textEnd = if (eventId == null) entry.length else eventSeparator
        return Entry(entry.substring(0, i), entry.substring(i + 1, textEnd), eventId)
    }

    private fun decodeStableEntry(entry: String): Entry {
        val idEnd = entry.indexOf(SEP, startIndex = 3)
        val sourceEnd = if (idEnd < 0) -1 else entry.indexOf(SEP, startIndex = idEnd + 1)
        if (idEnd <= 3 || sourceEnd <= idEnd) return Entry("Notif", entry)
        val eventSeparator = entry.lastIndexOf(SEP)
        val eventId = entry.substring(eventSeparator + 1)
            .takeIf { eventSeparator > sourceEnd && it.matches(Regex("[a-fA-F0-9]{64}")) }
        val textEnd = if (eventId == null) entry.length else eventSeparator
        return Entry(
            source = entry.substring(idEnd + 1, sourceEnd),
            text = entry.substring(sourceEnd + 1, textEnd),
            sourceEventId = eventId,
            sourceId = entry.substring(3, idEnd),
        )
    }

    /** Pair-compatible adapter retained for existing queue callers. */
    fun decode(entry: String): Pair<String, String> = decodeEntry(entry).let { it.source to it.text }

    fun offer(existingJson: String, source: String, text: String, max: Int = 12, sourceEventId: String? = null): String {
        val entry = encode(source, text, sourceEventId)
        val items = parse(existingJson).toMutableList()
        if (items.lastOrNull() == entry) return encodeList(items)
        // Drop spam: same message body already queued (any source)
        val norm = text.replace(Regex("\\s+"), " ").trim().lowercase()
        if (items.any { decodeEntry(it).text.replace(Regex("\\s+"), " ").trim().lowercase() == norm }) {
            return encodeList(items)
        }
        items.add(entry)
        val trimmed = if (items.size > max) items.takeLast(max) else items
        return encodeList(trimmed)
    }

    /** Back-compat: treat bare text as source=Notif */
    fun offer(existingJson: String, text: String, max: Int = 12): String =
        offer(existingJson, "Notif", text, max)

    fun drain(existingJson: String, limit: Int): Pair<List<String>, String> {
        val items = parse(existingJson)
        if (items.isEmpty()) return emptyList<String>() to "[]"
        val take = minOf(limit, items.size)
        val out = items.take(take)
        val rest = items.drop(take)
        return out to encodeList(rest)
    }

    /** Requeues an interrupted claim before atomically assigning the next batch. */
    fun claim(pendingJson: String, inFlightJson: String, limit: Int): Claim {
        val recovered = (entries(inFlightJson) + entries(pendingJson)).distinct()
        val (batch, pending) = drain(encodeEntries(recovered), limit)
        return Claim(batch, pending, encodeEntries(batch))
    }

    fun size(existingJson: String): Int = parse(existingJson).size

    fun entries(existingJson: String): List<String> = parse(existingJson)

    fun encodeEntries(items: List<String>): String = encodeList(items)

    private fun encodeList(items: List<String>): String =
        items.joinToString(prefix = "[", postfix = "]", separator = ",") { "\"${escape(it)}\"" }

    private fun parse(json: String): List<String> {
        val t = json.trim()
        if (t.isEmpty() || t == "[]") return emptyList()
        if (!t.startsWith("[") || !t.endsWith("]")) return emptyList()
        val inner = t.substring(1, t.length - 1).trim()
        if (inner.isEmpty()) return emptyList()
        val out = mutableListOf<String>()
        var i = 0
        while (i < inner.length) {
            while (i < inner.length && (inner[i] == ',' || inner[i].isWhitespace())) i++
            if (i >= inner.length) break
            if (inner[i] != '"') break
            i++
            val sb = StringBuilder()
            while (i < inner.length) {
                val c = inner[i]
                when {
                    c == '\\' && i + 1 < inner.length -> {
                        sb.append(inner[i + 1])
                        i += 2
                    }
                    c == '"' -> {
                        i++
                        break
                    }
                    else -> {
                        sb.append(c)
                        i++
                    }
                }
            }
            out.add(sb.toString())
        }
        return out
    }

    private fun escape(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"")
}
