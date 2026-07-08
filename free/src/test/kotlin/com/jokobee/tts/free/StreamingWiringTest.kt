package com.jokobee.tts.free

import com.jokobee.tts.core.G2p
import com.jokobee.tts.core.StreamChunk
import com.jokobee.tts.core.StreamingEngine
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Plomberie streaming côté :free (callback + Flow) : un moteur installé livre ses chunks à l'appelant. */
class StreamingWiringTest {

    private val frontend = Frontend(object : G2p { override fun phonemize(word: String, lang: String) = "" })
    private val synth = object : Synthesizer {
        override fun synth(phonemes: String, voice: Voice, speed: Float) = FloatArray(10)
    }
    private val voice = Voice.of("v", "fr", FloatArray(VoiceFormat.N_ROWS * VoiceFormat.STYLE_DIM))

    // Moteur factice : émet 2 chunks synthétiques sans appeler synthSegment (isole la plomberie).
    private val fakeEngine = StreamingEngine { _, _, _, _, onChunk ->
        var go = onChunk(StreamChunk(FloatArray(4) { 1f }, 0, "a", isFinal = false))
        if (go) onChunk(StreamChunk(FloatArray(4) { 2f }, 1, "b", isFinal = true))
    }

    private fun ttsWithEngine(): Tts = Tts(frontend, synth).also { it.installStreamingEngine(fakeEngine) }

    @Test fun synthesizeStreaming_deliversChunksInOrder() {
        val got = ArrayList<StreamChunk>()
        ttsWithEngine().synthesizeStreaming("x. y.", "fr", voice) { got.add(it); true }
        assertEquals(listOf(0, 1), got.map { it.index })
        assertEquals(listOf("a", "b"), got.map { it.text })
        assertTrue("dernier isFinal", got.last().isFinal)
    }

    @Test fun synthesizeStreaming_returningFalseInterrupts() {
        val got = ArrayList<Int>()
        ttsWithEngine().synthesizeStreaming("x. y.", "fr", voice) { got.add(it.index); false }
        assertEquals(listOf(0), got)   // interrompu après le 1ᵉʳ
    }

    @Test fun synthesizeFlow_collectsAllChunks() {
        val chunks = runBlocking { ttsWithEngine().synthesizeFlow("x. y.", "fr", voice).toList() }
        assertEquals(listOf(0, 1), chunks.map { it.index })
        assertEquals(4, chunks[0].audio.size)
        assertTrue(chunks.last().isFinal)
    }
}
