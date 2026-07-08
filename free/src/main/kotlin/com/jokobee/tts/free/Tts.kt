package com.jokobee.tts.free

/**
 * Façade TTS complète : **texte → audio**. Assemble le [Frontend] (texte → IPA) et
 * [KokoroSynth] (IPA + voix → forme d'onde), plus l'export WAV.
 *
 *   texte ─[Frontend]→ IPA ─[KokoroSynth(voix)]→ forme d'onde 24 kHz ─[WavWriter]→ WAV
 *
 * En production : `Frontend(CachingG2p(CharsiuG2p.fromAssetsOrCache(ctx, env)))` +
 * `KokoroSynth.fromModelFile(env, modelPath, KokoroTokenizer.fromAsset(ctx))`, le
 * `modelPath` provenant du téléchargeur (`:core`) qui récupère `model_quantized`
 * (88 Mo) au 1er appel puis le met en cache.
 */
public class Tts(
    private val frontend: Frontend,
    private val synth: KokoroSynth,
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
        val ipa = frontend.toPhonemes(text, lang)
        val wave = synth.synth(ipa, voice, speed)
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
