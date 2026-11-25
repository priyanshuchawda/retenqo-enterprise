package com.mneme.app.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalAnswerCleanupTest {
    @Test
    fun removesModelReasoningTagsWithoutRemovingTheAnswer() {
        assertEquals(
            "The answer is four.",
            LocalAnswerCleanup.removeThinkingMarkup("<think>hidden reasoning</think>\nThe answer is four."),
        )
    }
}
