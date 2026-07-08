package com.jokobee.tts.free

import com.jokobee.tts.core.DefaultStyleResolver
import com.jokobee.tts.core.G2p
import com.jokobee.tts.core.StyleResolver
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * BLOC 4 — crochet StyleResolver. Vérifie que le pipeline de synthèse passe TOUJOURS par
 * le StyleResolver (jamais de résolution directe du style), et que le DefaultStyleResolver
 * (v1.0) retourne exactement la voix demandée sans modification.
 */
class StyleResolverTest {

    private class StubG2p : G2p {
        override fun phonemize(word: String, lang: String) = "[$word]"
    }

    private fun voice() = Voice.of("v", "es", FloatArray(VoiceFormat.N_ROWS * VoiceFormat.STYLE_DIM) { 0.1f })

    @Test fun defaultResolverReturnsRequestedUnchanged() {
        val v = voice()
        assertSame(v, DefaultStyleResolver<Voice>().resolve("hola mundo", "es", v))
    }

    @Test fun pipelineAlwaysGoesThroughResolver() {
        val v = voice()
        var resolverCalled = false
        var synthGot: Voice? = null
        val resolver = StyleResolver<Voice> { _, _, req -> resolverCalled = true; req }
        val stubSynth = object : Synthesizer {
            override fun synth(phonemes: String, voice: Voice, speed: Float): FloatArray {
                synthGot = voice
                return FloatArray(24001)
            }
        }
        Tts(Frontend(StubG2p()), stubSynth, resolver).synthesize("hola mundo", "es", v)

        assertTrue("le pipeline doit passer par le StyleResolver", resolverCalled)
        assertNotNull(synthGot)
        assertSame("pass-through : le synth reçoit exactement la voix demandée", v, synthGot)
    }
}
