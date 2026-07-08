package com.jokobee.tts.free

import com.jokobee.tts.core.DefaultStyleResolver
import com.jokobee.tts.core.StyleResolver
import com.jokobee.tts.core.SynthesisContext

/**
 * Façade TTS complète : **texte → audio**. Assemble le [Frontend] (texte → IPA) et
 * un [Synthesizer] (IPA + voix → forme d'onde), plus l'export WAV.
 *
 *   texte ─[Frontend]→ IPA ─[Synthesizer(voix résolue)]→ forme d'onde 24 kHz ─[WavWriter]→ WAV
 *
 * La voix demandée passe TOUJOURS par le [styleResolver] (couche obligatoire) avant la
 * synthèse — v1.0 = [DefaultStyleResolver] pass-through, mais le point d'insertion existe
 * pour un futur moteur de style contextuel. Le pipeline ne résout JAMAIS le style en direct.
 *
 * En production : `Frontend(CachingG2p(CharsiuG2p.fromAssetsOrCache(ctx, env)))` +
 * `KokoroSynth.fromModelFile(env, modelPath, KokoroTokenizer.fromAsset(ctx))`, le
 * `modelPath` provenant du téléchargeur (`:core`) qui récupère `model_quantized`
 * (88 Mo) au 1er appel puis le met en cache.
 */
public class Tts(
    private val frontend: Frontend,
    private val synth: Synthesizer,
    private val styleResolver: StyleResolver<Voice> = DefaultStyleResolver(),
) {
    /**
     * Texte → forme d'onde f32 [-1,1] à 24 kHz, avec la [voice] et la vitesse données.
     * Encadrée par défaut d'un silence de tête/queue ([AudioPad]) — évite la troncature
     * du 1er mot (latence d'init du player) et le pop de fin. Mettre `leadMs=0, trailMs=0`
     * pour la forme d'onde brute.
     */
    public fun synthesize(
        text: String,
        lang: String,
        voice: Voice,
        speed: Float = 1.0f,
        leadMs: Int = 200,
        trailMs: Int = 100,
    ): FloatArray {
        // Couche obligatoire : le style passe TOUJOURS par le resolver (v1.0 = identité).
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

    private companion object {
        private const val SAMPLE_RATE = 24000   // sortie native Kokoro
    }
}
