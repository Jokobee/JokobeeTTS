package com.jokobee.tts.free

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.Closeable

/**
 * Synthèse **Kokoro-82M v1.0** : phonèmes IPA + voix → forme d'onde 24 kHz mono.
 *
 * Contrat ONNX (validé au banc) :
 *   entrées : `input_ids` i64 [1,N], `style` f32 [1,256], `speed` f32 [1]
 *   sortie  : `waveform` f32 [1, num_samples]
 * `style` = `Voice.styleFor(nTokens)` (banque de styles indexée par longueur d'énoncé —
 * le registre de voix déjà porté s'emboîte directement).
 *
 * ⚠ Le modèle (~88 Mo, `model_quantized` int8 — q8f16 segfaulte ORT, écarté) n'est PAS
 * embarqué : il est TÉLÉCHARGÉ au 1er lancement puis mis en cache (voir la façade [Tts]
 * et le téléchargeur `:core`). Cette classe reçoit une session déjà ouverte → testable
 * par injection ; aucun test unitaire ne l'instancie avec un vrai modèle.
 */
public class KokoroSynth(
    private val env: OrtEnvironment,
    private val session: OrtSession,
    private val tokenizer: KokoroTokenizer,
) : Closeable {

    /** Phonèmes IPA + voix → forme d'onde f32 [-1,1] à 24 kHz. `speed` : 1.0 = normal. */
    public fun synth(phonemes: String, voice: Voice, speed: Float = 1.0f): FloatArray {
        val ids = tokenizer.encode(phonemes)
        val style = voice.styleFor(tokenizer.nTokens(ids))     // FloatArray[256]

        val idsT = OnnxTensor.createTensor(env, arrayOf(ids))          // [1,N]
        val styleT = OnnxTensor.createTensor(env, arrayOf(style))      // [1,256]
        val speedT = OnnxTensor.createTensor(env, floatArrayOf(speed)) // [1]
        try {
            session.run(
                mapOf("input_ids" to idsT, "style" to styleT, "speed" to speedT),
            ).use { result ->
                @Suppress("UNCHECKED_CAST")
                val wav = result[0].value as Array<FloatArray>          // [1, num_samples]
                return wav[0]
            }
        } finally {
            idsT.close(); styleT.close(); speedT.close()
        }
    }

    override fun close() {
        session.close()
    }

    public companion object {
        /** Ouvre une session sur un fichier modèle local (déjà téléchargé) + vocab embarqué. */
        public fun fromModelFile(
            env: OrtEnvironment,
            modelPath: String,
            tokenizer: KokoroTokenizer,
            options: OrtSession.SessionOptions = OrtSession.SessionOptions(),
        ): KokoroSynth = KokoroSynth(env, env.createSession(modelPath, options), tokenizer)
    }
}
