package com.jokobee.tts.free

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.jokobee.tts.core.G2p
import com.jokobee.tts.core.UnsupportedLanguageException
import java.io.Closeable

/**
 * Correspondance locale JokobeeTTS → étiquette de langue CharsiuG2P (préfixe `<tag>`).
 *
 * CharsiuG2P multilingue attend une entrée de la forme `"<tag>: mot"` (codes ~ISO 639-3
 * + région). ⚠ Les valeurs exactes doivent être VALIDÉES contre le vocab du modèle
 * exporté (banc d'export) ; la structure et le format `"<tag>: "` sont, eux, stables.
 */
public object G2pLangTag {
    private val TAGS: Map<String, String> = mapOf(
        "fr" to "fra", "fr_CA" to "fra",
        "en_US" to "eng-us", "en_GB" to "eng-uk",
        "es" to "spa", "it" to "ita", "pt_BR" to "por-bz",
        "hi" to "hin", "ja" to "jpn", "zh" to "cmn", "ko" to "kor",
    )

    public fun of(lang: String): String =
        TAGS[lang] ?: throw UnsupportedLanguageException(lang)

    /** Construit l'entrée graphémique attendue par le modèle : `"<tag>: mot"`. */
    public fun prompt(lang: String, word: String): String = "<${of(lang)}>: $word"
}

/**
 * Implémentation Free du [G2p] : modèle **CharsiuG2P** (ByT5 seq2seq, licence MIT)
 * exécuté via onnxruntime, décodage glouton (argmax) mot par mot.
 *
 * Contrat d'export (optimum, `use_cache=False`) — VALIDÉ au banc : deux modèles ONNX.
 *   encoder_model.onnx : `input_ids` i64 [1,S], `attention_mask` i64 [1,S]
 *                        → `last_hidden_state` f32 [1,S,256]
 *   decoder_model.onnx : `input_ids` i64 [1,T], `encoder_attention_mask` i64 [1,S],
 *                        `encoder_hidden_states` f32 [1,S,256] → `logits` f32 [1,T,384]
 * L'encodeur tourne une fois ; le décodeur est ré-alimenté avec toute la séquence
 * générée à chaque pas (O(n²), mais les mots sont courts — pas de cache KV côté Kotlin).
 *
 * Prétraitement VALIDÉ : entrée encodeur SANS `eos` (`ByT5Tokenizer.encode`, addEos=false)
 * → décodage identique à PyTorch HF (6/6). `decoder_start_token_id = pad(0)`.
 *
 * ⚠ Nécessite les assets exportés + validation on-device ; aucun test unitaire ne
 * l'instancie (la logique testable — ByT5Tokenizer / PhonemePipeline / PhonemePost —
 * l'est séparément).
 */
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

    /** logits [1,T,384] → argmax du dernier pas T-1. */
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

    private companion object {
        private fun LongArray.reshape1xN(): Array<LongArray> = arrayOf(this)
    }
}
