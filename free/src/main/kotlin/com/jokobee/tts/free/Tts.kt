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
    /** Texte → forme d'onde f32 [-1,1] à 24 kHz, avec la [voice] et la vitesse données. */
    public fun synthesize(text: String, lang: String, voice: Voice, speed: Float = 1.0f): FloatArray {
        val ipa = frontend.toPhonemes(text, lang)
        return synth.synth(ipa, voice, speed)
    }

    /** Idem, exporté en octets WAV PCM 16 bits mono 24 kHz. */
    public fun synthesizeToWav(
        text: String,
        lang: String,
        voice: Voice,
        speed: Float = 1.0f,
    ): ByteArray = WavWriter.toWav(synthesize(text, lang, voice, speed))
}
