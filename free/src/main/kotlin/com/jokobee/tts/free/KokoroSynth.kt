package com.jokobee.tts.free

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.Closeable

/** Kokoro-82M v1.0 synthesis */
public class KokoroSynth(
    private val env: OrtEnvironment,
    private val session: OrtSession,
    private val tokenizer: KokoroTokenizer,
) : Synthesizer, Closeable {

    /** Synthesizes a waveform from phonemes and a voice. */
    override fun synth(phonemes: String, voice: Voice, speed: Float): FloatArray {
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
        /** Opens a session on a local model file (already downloaded) + embedded vocab. */
        public fun fromModelFile(
            env: OrtEnvironment,
            modelPath: String,
            tokenizer: KokoroTokenizer,
            options: OrtSession.SessionOptions = OrtSession.SessionOptions(),
        ): KokoroSynth = KokoroSynth(env, env.createSession(modelPath, options), tokenizer)

        /** Opens a session directly on the model bundled in the AAR assets — no file, no download. */
        public fun fromAsset(
            context: android.content.Context,
            env: OrtEnvironment,
            tokenizer: KokoroTokenizer,
            assetPath: String = "kokoro/model_quantized.onnx",
            options: OrtSession.SessionOptions = OrtSession.SessionOptions(),
        ): KokoroSynth {
            val bytes = context.assets.open(assetPath).use { it.readBytes() }
            return KokoroSynth(env, env.createSession(bytes, options), tokenizer)
        }
    }
}
