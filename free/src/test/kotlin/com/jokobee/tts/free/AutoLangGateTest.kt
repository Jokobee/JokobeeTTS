package com.jokobee.tts.free

import com.jokobee.tts.core.AUTO
import com.jokobee.tts.core.G2p
import com.jokobee.tts.core.LanguageDetector
import com.jokobee.tts.core.ProRequiredException
import com.jokobee.tts.core.UnsupportedLanguageException
import org.junit.Assert.assertEquals
import org.junit.Test

/** lang="auto" est une capacité Pro : sans détecteur installé → ProRequired ; détecteur muet → UnsupportedLanguage. */
class AutoLangGateTest {

    private val frontend = Frontend(object : G2p { override fun phonemize(word: String, lang: String) = "" })
    // Le synth enregistre la lang effective résolue (via les phonèmes n'est pas possible ; on capte la lang par un fake détecteur).
    private val synth = object : Synthesizer {
        override fun synth(phonemes: String, voice: Voice, speed: Float) = FloatArray(4)
    }
    private val voice = Voice.of("v", "fr", FloatArray(VoiceFormat.N_ROWS * VoiceFormat.STYLE_DIM))

    @Test(expected = ProRequiredException::class)
    fun auto_withoutDetector_throwsProRequired() {
        Tts(frontend, synth).synthesize("Bonjour le monde", AUTO, voice)
    }

    @Test(expected = UnsupportedLanguageException::class)
    fun auto_detectorReturnsNull_throwsUnsupported() {
        val tts = Tts(frontend, synth).also { it.installLanguageDetector(LanguageDetector { null }) }
        tts.synthesize("￥￥￥", AUTO, voice)
    }

    @Test fun auto_detectorResolves_routesToDetectedLang() {
        // Détecteur factice renvoyant "es" ; la synthèse ne doit pas lever (pipeline es).
        val tts = Tts(frontend, synth).also { it.installLanguageDetector(LanguageDetector { "es" }) }
        val out = tts.synthesize("Hola mundo", AUTO, voice)
        assertEquals(true, out.isNotEmpty())   // routé sans exception
    }
}
