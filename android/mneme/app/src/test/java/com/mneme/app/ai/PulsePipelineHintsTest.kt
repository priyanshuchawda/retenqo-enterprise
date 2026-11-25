package com.mneme.app.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PulsePipelineHintsTest {
    @Test
    fun skipsRetenqoSelfUi() {
        assertTrue(
            PulsePipelineHints.shouldSkipIngest(
                "Your private memory stays on this phone. Search memory. Pulse.",
            ),
        )
        assertTrue(
            PulsePipelineHints.looksLikeSelfUi(
                "Retenqo Retenqo pulse capture status",
            ),
        )
    }

    @Test
    fun skipsBrowserChrome() {
        assertTrue(
            PulsePipelineHints.shouldSkipIngest(
                "Chrome New tab\nSearch Google or type a URL",
            ),
        )
    }

    @Test
    fun skipsClaudeScreenshotWithoutDeadline() {
        assertTrue(
            PulsePipelineHints.shouldSkipIngest(
                "Claude Anthropic\nHow can I help you today?",
            ),
        )
    }

    @Test
    fun keepsDeadlineNotice() {
        assertFalse(
            PulsePipelineHints.shouldSkipIngest(
                "Part Submission Notice: submit the lab report by Aug 28 for CIE.",
            ),
        )
        assertTrue(
            PulsePipelineHints.looksImportant(
                "Part Submission Notice: submit the lab report by Aug 28 for CIE.",
            ),
        )
    }

    @Test
    fun rejectsCasualAndClock() {
        assertFalse(PulsePipelineHints.looksImportant("ok"))
        assertFalse(PulsePipelineHints.looksImportant("18:54"))
        assertTrue(PulsePipelineHints.looksLikeClockOnlyTitle("18:54"))
        assertTrue(PulsePipelineHints.looksLikeStatusBarJunk("18:54\n67%\n5G"))
    }
}
