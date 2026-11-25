package com.mneme.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HashEmbedderTest {
    @Test
    fun similarTextsScoreHigherThanUnrelated() {
        val a = HashEmbedder.embed("lab exam submission deadline August")
        val b = HashEmbedder.embed("submit lab exam by August deadline")
        val c = HashEmbedder.embed("pizza recipe tomato basil")
        val ab = HashEmbedder.cosine(a, b)
        val ac = HashEmbedder.cosine(a, c)
        assertTrue("similar=$ab unrelated=$ac", ab > ac)
    }

    @Test
    fun bytesRoundTrip() {
        val v = HashEmbedder.embed("hello mneme")
        val back = HashEmbedder.fromBytes(HashEmbedder.toBytes(v))!!
        assertEquals(v.size, back.size)
        for (i in v.indices) {
            assertEquals(v[i], back[i], 1e-5f)
        }
    }
}
