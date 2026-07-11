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
    /** Official voice catalog (populated by `create(context)`, the zero-config path). Backs the default-voice lookup in [synthesize]/[synthesizeToWav] when no [Voice] is passed. */
    public var voices: VoiceCatalog? = null,
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

    // Short-form alias accepted by the zero-config API ("en" -> "en_US"); every other lang is unchanged.
    private fun resolveLangAlias(lang: String): String = if (lang == "en") "en_US" else lang

    // Looks up the default official voice for a language in the bundled catalog (zero-config API only).
    private fun defaultVoiceFor(lang: String): Voice {
        val catalog = voices ?: throw IllegalStateException(
            "No default voice available: this Tts instance has no bundled voice catalog " +
                "(use Tts.create(context) for the zero-config API, or pass a Voice explicitly).",
        )
        val id = VoiceCatalog.DEFAULT_VOICE_ID[lang] ?: throw UnsupportedLanguageException(lang)
        return catalog.get(id)
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

    /**
     * Synthesizes a waveform from a text. `lang` accepts the short alias `"en"` (-> `"en_US"`)
     * in addition to the full locale codes. If `voice` is omitted, a default official voice for
     * `lang` is used (requires the zero-config `Tts.create(context)`; see [voices]).
     */
    public fun synthesize(
        text: String,
        lang: String,
        voice: Voice? = null,
        speed: Float = 1.0f,
        leadMs: Int = 200,
        trailMs: Int = 100,
    ): FloatArray {
        val resolvedLang0 = resolveLangAlias(lang)
        val actualLang = resolveLang(text, resolvedLang0)
        val actualVoice = voice ?: defaultVoiceFor(actualLang)
        val resolved = styleResolver.resolve(SynthesisContext(text, actualLang, actualVoice)).style
        val segments = TextSplitter().split(text)
        val waves = segments.map { synth.synth(frontend.toPhonemes(it, actualLang), resolved, speed) }
        val stitched = AudioStitcher.stitch(waves, stitchConfig)   // leading silence = only once, via AudioPad
        return AudioPad.pad(stitched, SAMPLE_RATE, leadMs, trailMs)
    }

    /** Same, exported as 16-bit PCM mono 24 kHz WAV bytes (with leading/trailing silence). */
    public fun synthesizeToWav(
        text: String,
        lang: String,
        voice: Voice? = null,
        speed: Float = 1.0f,
        leadMs: Int = 200,
        trailMs: Int = 100,
    ): ByteArray = WavWriter.toWav(synthesize(text, lang, voice, speed, leadMs, trailMs), SAMPLE_RATE)

    public companion object {
        private const val SAMPLE_RATE = 24000   // native Kokoro output

        // Shared text-processing frontend (normalization, G2P, lexicon/dictionary/accent hooks).
        private fun buildFrontend(context: android.content.Context, env: ai.onnxruntime.OrtEnvironment): Frontend {
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
            // Correctif CharsiuG2P (2026-07-10) : "vraiment" perd sa terminaison nasale
            // ("vʁɛm" au lieu de "vʁɛmɑ̃"), seul mot en -ment observé avec ce défaut.
            frontend.lexicon.add("vraiment", "vʁɛmɑ̃", "fr")
            frontend.lexicon.add("vraiment", "vʁɛmɑ̃", "fr_CA")
            // Correctif CharsiuG2P (2026-07-10) : "ai" (verbe avoir, 1re pers.) lu
            // comme une diphtongue "aj" au lieu de "ɛ" -- casse "j'ai"/"n'ai".
            frontend.lexicon.add("ai", "ɛ", "fr")
            frontend.lexicon.add("ai", "ɛ", "fr_CA")
            // Correctif CharsiuG2P (2026-07-11) : "JokobeeTTS" (marque, un seul
            // mot) massacre par le G2P. Piege en plus : le rendu de secours
            // ("Jokobee TTS" avec espace) commence par "j" IPA = le son "y"
            // (yes), pas le "J" de la marque -- donnait "Yokobee". Corrige en
            // "dʒ" (comme "Jokobee" a l'anglaise).
            frontend.lexicon.add("JokobeeTTS", "dʒɔkɔbitetɛs", "fr")
            frontend.lexicon.add("JokobeeTTS", "dʒɔkɔbitetɛs", "fr_CA")
            // Correctif CharsiuG2P (2026-07-11) : "Android" lu "ɑ̃dʁwa" (perd
            // le "-oid") au lieu de "ɑ̃dʁɔid" (comme "androïde").
            frontend.lexicon.add("Android", "ɑ̃dʁɔid", "fr")
            frontend.lexicon.add("Android", "ɑ̃dʁɔid", "fr_CA")
            return frontend
        }

        /** Ready-to-use TTS pipeline, model loaded from an external file (e.g. Pro's own download path). */
        public fun create(
            context: android.content.Context,
            env: ai.onnxruntime.OrtEnvironment,
            modelPath: String,
            styleResolver: StyleResolver<Voice> = DefaultStyleResolver(),
        ): Tts {
            val frontend = buildFrontend(context, env)
            val synth = KokoroSynth.fromModelFile(env, modelPath, KokoroTokenizer.fromAsset(context))
            return Tts(frontend, synth, styleResolver)
        }

        /**
         * Zero-config pipeline: Kokoro model + 38 official voices are bundled in the AAR
         * assets — no file path, no download, no network. Just call [synthesize] with a
         * language (e.g. `"en"`, `"fr"`) and it uses a default official voice automatically.
         */
        public fun create(
            context: android.content.Context,
            styleResolver: StyleResolver<Voice> = DefaultStyleResolver(),
        ): Tts {
            val env = ai.onnxruntime.OrtEnvironment.getEnvironment()
            val frontend = buildFrontend(context, env)
            val synth = KokoroSynth.fromAsset(context, env, KokoroTokenizer.fromAsset(context))
            return Tts(frontend, synth, styleResolver, voices = VoiceCatalog.official(context))
        }
    }
}
