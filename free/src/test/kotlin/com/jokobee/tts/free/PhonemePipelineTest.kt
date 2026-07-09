package com.jokobee.tts.free

import com.jokobee.tts.core.G2p
import com.jokobee.tts.free.HomographAnnotator.Ann
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.Normalizer

/** Orchestrator */
class PhonemePipelineTest {

    /** Deterministic G2P stub */
    private class StubG2p : G2p {
        val seen = mutableListOf<String>()
        override fun phonemize(word: String, lang: String): String {
            seen += word
            return "[${word.lowercase()}]"
        }
    }

    private val identity: (String, String) -> String = { s, _ -> s }

    @Test fun overridesBypassG2p() {
        val stub = StubG2p()
        val out = PhonemePipeline(stub, post = identity).phonemizeAnnotations(
            listOf(Ann("est", "ɛ"), Ann("chat", null)), "fr",   // "est" -> ɛ
        )
        assertEquals("ɛ [chat]", out)
        assertEquals(listOf("chat"), stub.seen)   // "est" (override) did NOT touch the G2P
    }

    @Test fun punctuationIsLiteralAndReattached() {
        val out = PhonemePipeline(StubG2p(), post = identity).phonemizeAnnotations(
            listOf(Ann("salut", null), Ann(",", null), Ann("toi", null), Ann("!", null)), "fr",
        )
        assertEquals("[salut], [toi]!", out)
    }

    @Test fun emptyG2pOutputSkipped() {
        val g2p = object : G2p {
            override fun phonemize(word: String, lang: String) = if (word == "x") "" else "[$word]"
        }
        val out = PhonemePipeline(g2p, post = identity).phonemizeAnnotations(
            listOf(Ann("a", null), Ann("x", null), Ann("b", null)), "fr",
        )
        assertEquals("[a] [b]", out)
    }

    @Test fun postProcessingIsApplied() {
        val composed = "é"
        val out = PhonemePipeline(StubG2p()).phonemizeAnnotations(listOf(Ann("mot", composed)), "fr")
        assertEquals(Normalizer.normalize(composed, Normalizer.Form.NFD), out)
    }

    @Test fun nonFrenchDelegatesAllWords() {
        val stub = StubG2p()
        val anns = listOf(Ann("hola", null), Ann("mundo", null))
        val out = PhonemePipeline(stub, post = identity).phonemizeAnnotations(anns, "es")
        assertEquals("[hola] [mundo]", out)
        assertEquals(listOf("hola", "mundo"), stub.seen)
    }
}
