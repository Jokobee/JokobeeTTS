package com.jokobee.tts.free

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import com.jokobee.tts.core.G2p
import com.jokobee.tts.core.UnsupportedLanguageException
import java.io.Closeable
import java.io.File

/** JokobeeTTS locale mapping */
public object G2pLangTag {
    private val TAGS: Map<String, String> = mapOf(
        "fr" to "fra", "fr_CA" to "fra",
        "en_US" to "eng-us", "en_GB" to "eng-uk",
        "es" to "spa", "it" to "ita", "pt_BR" to "por-bz",
    )

    public fun of(lang: String): String =
        TAGS[lang] ?: throw UnsupportedLanguageException(lang)

    /** Builds the graphemic input expected by the model */
    public fun prompt(lang: String, word: String): String = "<${of(lang)}>: $word"
}

/** Free-tier implementation of [G2p]. */
public class CharsiuG2p(
    private val env: OrtEnvironment,
    private val encoder: OrtSession,
    private val decoder: OrtSession,
    private val maxTokens: Int = 64,
) : G2p, Closeable {

    override fun phonemize(word: String, lang: String): String {
        if (word.isEmpty()) return ""
        val inputIds = ByT5Tokenizer.encode(G2pLangTag.prompt(lang, word)).reshape1xN()
        val mask = LongArray(inputIds[0].size) { 1L }.reshape1xN()

        val idsTensor = OnnxTensor.createTensor(env, inputIds)
        val maskTensor = OnnxTensor.createTensor(env, mask)
        val encHidden = encoder.run(
            mapOf("input_ids" to idsTensor, "attention_mask" to maskTensor),
        )
        try {
            val hidden = encHidden[0] as OnnxTensor      // last_hidden_state [1,S,256]
            val generated = ArrayList<Long>(maxTokens)
            var decIn = longArrayOf(ByT5Tokenizer.PAD)   // decoder_start = pad(0)
            for (step in 0 until maxTokens) {
                val decTensor = OnnxTensor.createTensor(env, decIn.reshape1xN())
                val next: Long = try {
                    decoder.run(
                        mapOf(
                            "input_ids" to decTensor,
                            "encoder_attention_mask" to maskTensor,
                            "encoder_hidden_states" to hidden,
                        ),
                    ).use { result -> argmaxLastStep(result[0].value) }
                } finally {
                    decTensor.close()
                }
                if (next == ByT5Tokenizer.EOS) break
                generated.add(next)
                decIn += next
            }
            return ByT5Tokenizer.decode(generated.toLongArray())
        } finally {
            encHidden.close()
            idsTensor.close()
            maskTensor.close()
        }
    }

    /** Model output for a word. */
    @Suppress("UNCHECKED_CAST")
    private fun argmaxLastStep(value: Any?): Long {
        val logits = value as Array<Array<FloatArray>>   // [1][T][V]
        val last = logits[0][logits[0].size - 1]
        var bestId = 0
        var bestVal = last[0]
        for (i in 1 until last.size) if (last[i] > bestVal) { bestVal = last[i]; bestId = i }
        return bestId.toLong()
    }

    override fun close() {
        encoder.close()
        decoder.close()
    }

    public companion object {
        private fun LongArray.reshape1xN(): Array<LongArray> = arrayOf(this)

        /** Name of the assets subfolder AND the downloaded cache folder. */
        public const val G2P_DIR: String = "g2p"

        /** Loads the model from assets or cache. */
        public fun fromAssetsOrCache(
            context: Context,
            env: OrtEnvironment,
            options: OrtSession.SessionOptions = OrtSession.SessionOptions(),
            maxTokens: Int = 64,
        ): CharsiuG2p = CharsiuG2p(
            env,
            loadSession(context, env, "encoder_model.onnx", options),
            loadSession(context, env, "decoder_model.onnx", options),
            maxTokens,
        )

        private fun loadSession(
            context: Context,
            env: OrtEnvironment,
            name: String,
            options: OrtSession.SessionOptions,
        ): OrtSession {
            val cached = context.getExternalFilesDir(G2P_DIR)?.let { File(it, name) }
            return if (cached != null && cached.exists()) {
                env.createSession(cached.absolutePath, options)     // downloaded model (Pro)
            } else {
                context.assets.open("$G2P_DIR/$name").use {          // bundled asset (Free)
                    env.createSession(it.readBytes(), options)
                }
            }
        }
    }
}
