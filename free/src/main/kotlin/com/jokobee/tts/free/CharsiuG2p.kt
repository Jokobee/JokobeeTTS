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
 * ⚠ NÉCESSITE un modèle exporté (`charsiu_g2p.onnx`) + validation ON-DEVICE : tant
 * que l'export/asset n'existe pas, cette classe compile mais n'est pas exerçable
 * (aucun test unitaire ne l'instancie — la logique testable, ByT5Tokenizer /
 * PhonemePipeline / PhonemePost, l'est séparément).
 *
 * Contrat d'export attendu (optimum, `use_cache=False`, encodeur+décodeur fusionnés) :
 *   entrées  : [inputIdsName] int64 [1,S], [attentionMaskName] int64 [1,S],
 *              [decoderInputIdsName] int64 [1,T]
 *   sortie 0 : logits float32 [1,T,V]
 * Le décodeur est ré-alimenté avec toute la séquence à chaque pas (O(n²) mais les
 * mots sont courts) — pas de gestion de cache KV côté Kotlin.
 */
public class CharsiuG2p(
    private val env: OrtEnvironment,
    private val session: OrtSession,
    private val maxTokens: Int = 64,
    private val inputIdsName: String = "input_ids",
    private val attentionMaskName: String = "attention_mask",
    private val decoderInputIdsName: String = "decoder_input_ids",
) : G2p, Closeable {

    override fun phonemize(word: String, lang: String): String {
        if (word.isEmpty()) return ""
        val enc = ByT5Tokenizer.encode(G2pLangTag.prompt(lang, word))
        val encIds = enc.reshape1xN()
        val encMask = LongArray(enc.size) { 1L }.reshape1xN()

        val encTensor = OnnxTensor.createTensor(env, encIds)
        val maskTensor = OnnxTensor.createTensor(env, encMask)
        try {
            val generated = ArrayList<Long>(maxTokens)
            var decoder = longArrayOf(ByT5Tokenizer.PAD)   // decoder_start_token_id = pad(0)
            for (step in 0 until maxTokens) {
                val decTensor = OnnxTensor.createTensor(env, decoder.reshape1xN())
                val next: Long = try {
                    session.run(
                        mapOf(
                            inputIdsName to encTensor,
                            attentionMaskName to maskTensor,
                            decoderInputIdsName to decTensor,
                        ),
                    ).use { result -> argmaxLastStep(result[0].value) }
                } finally {
                    decTensor.close()
                }
                if (next == ByT5Tokenizer.EOS) break
                generated.add(next)
                decoder += next
            }
            return ByT5Tokenizer.decode(generated.toLongArray())
        } finally {
            encTensor.close()
            maskTensor.close()
        }
    }

    /** logits [1,T,V] → argmax du dernier pas T-1. */
    @Suppress("UNCHECKED_CAST")
    private fun argmaxLastStep(value: Any?): Long {
        val logits = value as Array<Array<FloatArray>>   // [1][T][V]
        val step = logits[0]
        val last = step[step.size - 1]
        var bestId = 0
        var bestVal = last[0]
        for (i in 1 until last.size) if (last[i] > bestVal) { bestVal = last[i]; bestId = i }
        return bestId.toLong()
    }

    override fun close() {
        session.close()
    }

    private companion object {
        private fun LongArray.reshape1xN(): Array<LongArray> = arrayOf(this)
    }
}
