package com.jokobee.tts.free

import com.jokobee.tts.core.DefaultStyleResolver
import com.jokobee.tts.core.G2p
import com.jokobee.tts.core.StyleOutput
import com.jokobee.tts.core.StyleResolver
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** BLOC A */
class StyleResolverTest {

    private class StubG2p : G2p {
        override fun phonemize(word: String, lang: String) = "[$word]"
    }

    private fun voice() = Voice.of("v", "es", FloatArray(VoiceFormat.N_ROWS * VoiceFormat.STYLE_DIM) { 0.1f })

    @Test fun defaultStyleResolverPassthrough() {
        val v = voice()
        val out = DefaultStyleResolver<Voice>().resolve(com.jokobee.tts.core.SynthesisContext("hola", "es", v))
        assertSame(v, out.style)   // exactement la voix demandée, inchangée
    }

    @Test fun styleResolverCalledInPipeline() {
        val v = voice()
        var resolverCalled = false
        var synthGot: Voice? = null
        val resolver = StyleResolver<Voice> { ctx -> resolverCalled = true; StyleOutput(ctx.requestedStyle) }
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
