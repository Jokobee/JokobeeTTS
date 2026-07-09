package com.jokobee.tts.free

import com.jokobee.tts.core.AUTO
import com.jokobee.tts.core.G2p
import com.jokobee.tts.core.LanguageDetector
import com.jokobee.tts.core.ProRequiredException
import com.jokobee.tts.core.UnsupportedLanguageException
import org.junit.Assert.assertEquals
import org.junit.Test

/** lang="auto" is a Pro capability: without an installed detector → ProRequired; silent detector → UnsupportedLanguage. */
class AutoLangGateTest {

    private val frontend = Frontend(object : G2p { override fun phonemize(word: String, lang: String) = "" })
    // The synth records the effective resolved lang (not possible via the phonemes; we capture the lang via a fake detector).
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
        // Fake detector returning "es"; synthesis must not throw (es pipeline).
        val tts = Tts(frontend, synth).also { it.installLanguageDetector(LanguageDetector { "es" }) }
        val out = tts.synthesize("Hola mundo", AUTO, voice)
        assertEquals(true, out.isNotEmpty())   // routed without exception
    }
}
