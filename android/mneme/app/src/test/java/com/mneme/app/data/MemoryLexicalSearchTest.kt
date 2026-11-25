package com.mneme.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryLexicalSearchTest {
    @Test
    fun unrelatedQuestionDoesNotMatchAnAccidentalHashedVectorCollision() {
        val query = MemoryLexicalSearch.tokens("What is two plus two?")

        assertEquals(0f, MemoryLexicalSearch.score(query, "YouTube Hmmmmmm"), 0f)
    }

    @Test
    fun exactMeaningfulTermsRankAboveQuestionFillers() {
        val query = MemoryLexicalSearch.tokens("When is my assignment due?")

        assertEquals(1f, MemoryLexicalSearch.score(query, "Assignment due Friday"), 0f)
        assertEquals(0f, MemoryLexicalSearch.score(query, "A casual note about today"), 0f)
    }
}
