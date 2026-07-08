package com.jokobee.tts.free

import com.jokobee.tts.core.DefaultStyleResolver
import com.jokobee.tts.core.StyleResolver
import com.jokobee.tts.core.SynthesisContext

/** Façade TTS complète */
public class Tts(
    private val frontend: Frontend,
    private val synth: Synthesizer,
    private val styleResolver: StyleResolver<Voice> = DefaultStyleResolver(),
) {
    /** Lexique custom prioritaire du pipeline, consulté avant le G2P pour toutes les langues, pour `add(...)`/`load(...)` à chaud (marques, corrections). */
    public val lexicon: com.jokobee.tts.core.MapLexiconSource get() = frontend.lexicon
    /** Adaptation du texte brut (Pro). */
    public val normalization: NormalizationRegistry get() = frontend.adapters.normalization
    /** Dictionnaires de terminologie (Pro). */
    public val dictionary: DictionaryRegistry get() = frontend.adapters.dictionary
    /** Adaptation des phonèmes/accent (Pro). */
    public val accent: AccentRegistry get() = frontend.adapters.accent
    /** Active le tier Pro en installant son loader d'adapters. */
    public fun installProLoader(loader: com.jokobee.tts.core.AdapterLoader) {
        frontend.adapters.installLoader(loader)
    }
    /** Synthétise une forme d'onde à partir d'un texte. */
    public fun synthesize(
        text: String,
        lang: String,
        voice: Voice,
        speed: Float = 1.0f,
        leadMs: Int = 200,
        trailMs: Int = 100,
    ): FloatArray {
        val resolved = styleResolver.resolve(SynthesisContext(text, lang, voice)).style
        val ipa = frontend.toPhonemes(text, lang)
        val wave = synth.synth(ipa, resolved, speed)
        return AudioPad.pad(wave, SAMPLE_RATE, leadMs, trailMs)
    }

    /** Idem, exporté en octets WAV PCM 16 bits mono 24 kHz (avec silence de tête/queue). */
    public fun synthesizeToWav(
        text: String,
        lang: String,
        voice: Voice,
        speed: Float = 1.0f,
        leadMs: Int = 200,
        trailMs: Int = 100,
    ): ByteArray = WavWriter.toWav(synthesize(text, lang, voice, speed, leadMs, trailMs), SAMPLE_RATE)

    public companion object {
        private const val SAMPLE_RATE = 24000   // sortie native Kokoro

        /** Pipeline TTS prêt à l'emploi */
        public fun create(
            context: android.content.Context,
            env: ai.onnxruntime.OrtEnvironment,
            modelPath: String,
            styleResolver: StyleResolver<Voice> = DefaultStyleResolver(),
        ): Tts {
            val g2p = CachingG2p(CharsiuG2p.fromAssetsOrCache(context, env))
            val en = MisakiEnG2p.fromAssets(context, fallback = g2p, british = false)
            val enGb = MisakiEnG2p.fromAssets(context, fallback = g2p, british = true)
            val frontend = Frontend(g2p, enG2p = { text, lang ->
                (if (lang == "en_GB") enGb else en).phonemize(text)
            })
            val synth = KokoroSynth.fromModelFile(env, modelPath, KokoroTokenizer.fromAsset(context))
            return Tts(frontend, synth, styleResolver)
        }
    }
}
