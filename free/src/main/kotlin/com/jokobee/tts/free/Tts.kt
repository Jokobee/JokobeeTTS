package com.jokobee.tts.free

import com.jokobee.tts.core.AUTO
import com.jokobee.tts.core.AudioStitcher
import com.jokobee.tts.core.DefaultStyleResolver
import com.jokobee.tts.core.LanguageDetector
import com.jokobee.tts.core.ProRequiredException
import com.jokobee.tts.core.StitchConfig
import com.jokobee.tts.core.StreamChunk
import com.jokobee.tts.core.StreamingEngine
import com.jokobee.tts.core.StyleResolver
import com.jokobee.tts.core.SynthesisContext
import com.jokobee.tts.core.TextSplitter
import com.jokobee.tts.core.UnsupportedLanguageException
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

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
    /** Assemblage des segments (silence entre phrases, fondu, normalisation). Configurable par le dev. */
    public var stitchConfig: StitchConfig = StitchConfig(sampleRate = SAMPLE_RATE)

    /** Moteur de synthèse streaming (Pro). Installé par [installStreamingEngine]. */
    private var streamer: StreamingEngine? = null
    /** Installe le moteur streaming Pro (appelé par le tier Pro). */
    public fun installStreamingEngine(engine: StreamingEngine) { streamer = engine }

    /** Détecteur de langue (Pro). Installé par [installLanguageDetector]. */
    private var detector: LanguageDetector? = null
    /** Installe le détecteur de langue Pro (appelé par le tier Pro). */
    public fun installLanguageDetector(d: LanguageDetector) { detector = d }

    // Résout lang="auto" via le détecteur Pro ; sinon renvoie lang tel quel.
    private fun resolveLang(text: String, lang: String): String {
        if (lang != AUTO) return lang
        val d = detector ?: throw ProRequiredException(
            "Auto language detection requires JokobeeTTS Pro — jokobee.com/pro",
        )
        return d.detect(text) ?: throw UnsupportedLanguageException(AUTO)
    }

    // Synthèse d'un segment isolé (phonèmes → onde), sans assemblage ni padding.
    private fun synthSegmentRaw(segment: String, lang: String, style: Voice, speed: Float): FloatArray =
        synth.synth(frontend.toPhonemes(segment, lang), style, speed)

    /**
     * Synthèse streaming phrase par phrase, chaque segment livré à [onChunk] dès qu'il est prêt.
     * Retourner `false` depuis [onChunk] interrompt la synthèse. Fonctionnalité Pro.
     */
    public fun synthesizeStreaming(
        text: String,
        lang: String,
        voice: Voice,
        speed: Float = 1.0f,
        onChunk: (StreamChunk) -> Boolean,
    ) {
        val engine = streamer ?: throw ProRequiredException(
            "Streaming requires JokobeeTTS Pro — jokobee.com/pro",
        )
        val actualLang = resolveLang(text, lang)
        val style = styleResolver.resolve(SynthesisContext(text, actualLang, voice)).style
        engine.stream(
            text = text,
            lang = actualLang,
            config = stitchConfig,
            synthSegment = { seg, l -> synthSegmentRaw(seg, l, style, speed) },
            onChunk = onChunk,
        )
    }

    /**
     * Idem [synthesizeStreaming], exposé comme [Flow] coroutine (backpressure + annulation).
     * Fonctionnalité Pro : lève [ProRequiredException] à l'appel si le moteur Pro n'est pas installé.
     */
    public fun synthesizeFlow(
        text: String,
        lang: String,
        voice: Voice,
        speed: Float = 1.0f,
    ): Flow<StreamChunk> {
        val engine = streamer ?: throw ProRequiredException(
            "Streaming requires JokobeeTTS Pro — jokobee.com/pro",
        )
        val actualLang = resolveLang(text, lang)
        val style = styleResolver.resolve(SynthesisContext(text, actualLang, voice)).style
        return channelFlow {
            engine.stream(
                text = text,
                lang = actualLang,
                config = stitchConfig,
                synthSegment = { seg, l -> synthSegmentRaw(seg, l, style, speed) },
                // trySendBlocking échoue si le collecteur a annulé (canal fermé) -> onChunk=false -> arrêt du moteur.
                onChunk = { chunk -> trySendBlocking(chunk).isSuccess },
            )
        }
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
        val actualLang = resolveLang(text, lang)
        val resolved = styleResolver.resolve(SynthesisContext(text, actualLang, voice)).style
        val segments = TextSplitter().split(text)
        val waves = segments.map { synth.synth(frontend.toPhonemes(it, actualLang), resolved, speed) }
        val stitched = AudioStitcher.stitch(waves, stitchConfig)   // silence de tête = 1 seule fois via AudioPad
        return AudioPad.pad(stitched, SAMPLE_RATE, leadMs, trailMs)
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
            val adapters = AdapterRegistry()
            val g2p = CachingG2p(CharsiuG2p.fromAssetsOrCache(context, env))
            val en = MisakiEnG2p.fromAssets(
                context, fallback = g2p, british = false,
                dictionary = adapters.dictionary, accent = adapters.accent,
            )
            val enGb = MisakiEnG2p.fromAssets(
                context, fallback = g2p, british = true,
                dictionary = adapters.dictionary, accent = adapters.accent,
            )
            val frontend = Frontend(g2p, enG2p = { text, lang ->
                (if (lang == "en_GB") enGb else en).phonemize(text)
            }, adapters = adapters, loanwords = LoanwordsLexicon.fromAssets(context))
            val synth = KokoroSynth.fromModelFile(env, modelPath, KokoroTokenizer.fromAsset(context))
            return Tts(frontend, synth, styleResolver)
        }
    }
}
