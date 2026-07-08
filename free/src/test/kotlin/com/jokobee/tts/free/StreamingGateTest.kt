package com.jokobee.tts.free

import com.jokobee.tts.core.G2p
import com.jokobee.tts.core.ProRequiredException
import org.junit.Test

/** En build Free (aucun moteur streaming installé), les API streaming lèvent ProRequiredException. */
class StreamingGateTest {

    private val frontend = Frontend(object : G2p { override fun phonemize(word: String, lang: String) = "" })
    private val synth = object : Synthesizer {
        override fun synth(phonemes: String, voice: Voice, speed: Float) = FloatArray(10)
    }
    private val tts = Tts(frontend, synth)
    private val voice = Voice.of("v", "fr", FloatArray(VoiceFormat.N_ROWS * VoiceFormat.STYLE_DIM))

    @Test(expected = ProRequiredException::class)
    fun synthesizeStreaming_free_throwsProRequired() {
        tts.synthesizeStreaming("Bonjour. Salut.", "fr", voice) { true }
    }

    @Test(expected = ProRequiredException::class)
    fun synthesizeFlow_free_throwsProRequired() {
        tts.synthesizeFlow("Bonjour.", "fr", voice)   // levée immédiate (avant collecte)
    }
}
