package com.mneme.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

data class ScreenContextSnapshot(
    val packageName: String,
    val text: String,
    val capturedAt: Long,
)

/** User-enabled, in-memory bridge for the last visible screen outside Retenqo. */
class RetenqoAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString()?.takeIf(String::isNotBlank) ?: return
        if (pkg == packageName) return // Keep the previous app's snapshot while the user asks about it.

        val now = System.currentTimeMillis()
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && now - lastCaptureAt < CAPTURE_INTERVAL_MS) return
        val root = rootInActiveWindow ?: return
        val rootPackage = root.packageName?.toString()
        if (rootPackage == null || rootPackage != pkg || rootPackage == packageName || rootPackage == "com.android.systemui" ||
            rootPackage.contains("inputmethod", ignoreCase = true) ||
            rootPackage.contains("keyboard", ignoreCase = true)
        ) {
            return
        }
        val text = readVisibleText(root)
        if (text.isNotBlank()) {
            snapshot = ScreenContextSnapshot(pkg, text, now)
            lastCaptureAt = now
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    private fun readVisibleText(root: AccessibilityNodeInfo): String {
        if (isSensitivePackage(root.packageName?.toString())) return ""
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        val lines = LinkedHashSet<String>()
        queue.add(root)
        var visited = 0
        var characters = 0
        while (queue.isNotEmpty() && visited < MAX_NODES && characters < MAX_CHARS) {
            val node = queue.removeFirst()
            visited++
            val className = node.className?.toString().orEmpty()
            val sensitiveInput = node.isEditable ||
                className.contains("EditText", ignoreCase = true) ||
                className.contains("Password", ignoreCase = true)
            if (node.isVisibleToUser && !node.isPassword && !sensitiveInput) {
                val label = sequenceOf(node.text, node.contentDescription)
                    .mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotBlank) }
                    .firstOrNull()
                if (label != null && label !in lines) {
                    val bounded = label.take((MAX_CHARS - characters).coerceAtLeast(0))
                    if (bounded.isNotBlank()) {
                        lines.add(bounded)
                        characters += bounded.length
                    }
                }
            }
            // Traverse children even when the parent itself is an editable field.
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(queue::addLast)
            }
        }
        return lines.joinToString("\n").take(MAX_CHARS)
    }

    companion object {
        private const val MAX_NODES = 220
        private const val MAX_CHARS = 5_000
        private const val CAPTURE_INTERVAL_MS = 500L
        private const val MAX_AGE_MS = 2 * 60 * 1000L

        @Volatile private var instance: RetenqoAccessibilityService? = null
        @Volatile private var snapshot: ScreenContextSnapshot? = null
        @Volatile private var lastCaptureAt: Long = 0

        fun isConnected(): Boolean = instance != null

        fun clearSnapshot() {
            snapshot = null
            lastCaptureAt = 0L
        }

        /** Returns a fresh external-app snapshot only when the user explicitly requests it. */
        fun currentSnapshot(): ScreenContextSnapshot? {
            if (instance == null) return null
            return snapshot?.takeIf { System.currentTimeMillis() - it.capturedAt <= MAX_AGE_MS }
        }

        private val sensitivePackageMarkers = setOf(
            "bank", "wallet", "pay", "payment", "authenticator", "password", "credential",
            "1password", "bitwarden", "keepass", "vault", "login", "sbi", "hdfc", "icici",
            "axis", "phonepe", "googlepay", "gpay", "paypal", "paytm",
        )

        private fun isSensitivePackage(packageName: String?): Boolean {
            val normalized = packageName?.lowercase().orEmpty()
            return sensitivePackageMarkers.any { marker -> normalized.contains(marker) }
        }
    }
}
