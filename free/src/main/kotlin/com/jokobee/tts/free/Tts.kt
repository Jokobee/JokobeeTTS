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

/** Complete TTS facade */
public class Tts(
    private val frontend: Frontend,
    private val synth: Synthesizer,
    private val styleResolver: StyleResolver<Voice> = DefaultStyleResolver(),
) {
    /** Pipeline-priority custom lexicon, consulted before the G2P for all languages, for hot `add(...)`/`load(...)` (brands, corrections). */
    public val lexicon: com.jokobee.tts.core.MapLexiconSource get() = frontend.lexicon
    /** Raw text adaptation (Pro). */
    public val normalization: NormalizationRegistry get() = frontend.adapters.normalization
    /** Terminology dictionaries (Pro). */
    public val dictionary: DictionaryRegistry get() = frontend.adapters.dictionary
    /** Phoneme/accent adaptation (Pro). */
    public val accent: AccentRegistry get() = frontend.adapters.accent
    /** Activates the Pro tier by installing its adapter loader. */
    public fun installProLoader(loader: com.jokobee.tts.core.AdapterLoader) {
        frontend.adapters.installLoader(loader)
    }
    /** Segment stitching (silence between sentences, fade, normalization). Configurable by the developer. */
    public var stitchConfig: StitchConfig = StitchConfig(sampleRate = SAMPLE_RATE)

    /** Streaming synthesis engine (Pro). Installed by [installStreamingEngine]. */
    private var streamer: StreamingEngine? = null
    /** Installs the Pro streaming engine (called by the Pro tier). */
    public fun installStreamingEngine(engine: StreamingEngine) { streamer = engine }

    /** Language detector (Pro). Installed by [installLanguageDetector]. */
    private var detector: LanguageDetector? = null
    /** Installs the Pro language detector (called by the Pro tier). */
    public fun installLanguageDetector(d: LanguageDetector) { detector = d }

    // Resolves lang="auto" via the Pro detector; otherwise returns lang unchanged.
    private fun resolveLang(text: String, lang: String): String {
        if (lang != AUTO) return lang
        val d = detector ?: throw ProRequiredException(
            "Auto language detection requires JokobeeTTS Pro — jokobee.com/pro",
        )
        return d.detect(text) ?: throw UnsupportedLanguageException(AUTO)
    }

    // Synthesis of an isolated segment (phonemes -> waveform), without stitching or padding.
    private fun synthSegmentRaw(segment: String, lang: String, style: Voice, speed: Float): FloatArray =
        synth.synth(frontend.toPhonemes(segment, lang), style, speed)

    /** Streaming synthesis: each sentence delivered to [onChunk] as soon as it's ready (`false` stops it). Pro. */
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

    /** Streaming exposed as a [Flow] of [StreamChunk] (backpressure + cancellation). Pro. */
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
                // trySendBlocking fails if the collector has cancelled (channel closed) -> onChunk=false -> engine stops.
                onChunk = { chunk -> trySendBlocking(chunk).isSuccess },
            )
        }
    }

    /** Synthesizes a waveform from a text. */
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
        val stitched = AudioStitcher.stitch(waves, stitchConfig)   // leading silence = only once, via AudioPad
        return AudioPad.pad(stitched, SAMPLE_RATE, leadMs, trailMs)
    }

    /** Same, exported as 16-bit PCM mono 24 kHz WAV bytes (with leading/trailing silence). */
    public fun synthesizeToWav(
        text: String,
        lang: String,
        voice: Voice,
        speed: Float = 1.0f,
        leadMs: Int = 200,
        trailMs: Int = 100,
    ): ByteArray = WavWriter.toWav(synthesize(text, lang, voice, speed, leadMs, trailMs), SAMPLE_RATE)

    public companion object {
        private const val SAMPLE_RATE = 24000   // native Kokoro output

        /** Ready-to-use TTS pipeline */
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
