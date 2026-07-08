package com.jokobee.tts.free

import android.content.Context
import org.json.JSONObject
import java.text.Normalizer

/**
 * Tokeniseur d'entrée du modèle **Kokoro-82M v1.0** : chaîne IPA → ids.
 *
 * Mécanisme autoritaire (identique au banc de référence) :
 *   ids = [0] + [vocab[c] pour c dans NFD(phonèmes) si c ∈ vocab] + [0]
 * Les deux `0` encadrants sont les marqueurs de frontière ; `nTokens` = taille − 2
 * (sert à indexer le style de la voix : `Voice.styleFor(nTokens)`).
 *
 * Le vocab (115 entrées, char→id) est embarqué dans `assets/kokoro/vocab.json`
 * (~1 Ko). NFD imposé : Kokoro attend les IPA décomposés (cf. [PhonemePost]).
 */
public class KokoroTokenizer(private val vocab: Map<Char, Int>) {

    /** Phonèmes → ids Kokoro, encadrés de `0`. Les chars hors vocab sont ignorés. */
    public fun encode(phonemes: String): LongArray {
        val nfd = Normalizer.normalize(phonemes, Normalizer.Form.NFD)
        val ids = ArrayList<Long>(nfd.length + 2)
        ids.add(0L)
        for (c in nfd) vocab[c]?.let { ids.add(it.toLong()) }
        ids.add(0L)
        return ids.toLongArray()
    }

    /** Nombre de tokens hors frontières (pour indexer le style de la voix). */
    public fun nTokens(ids: LongArray): Int = maxOf(0, ids.size - 2)

    /** Vrai si le char est dans le vocab Kokoro (diagnostic OOV). */
    public operator fun contains(c: Char): Boolean = c in vocab

    public companion object {
        /** Charge le vocab depuis `assets/kokoro/vocab.json` (map char→id, clés à 1 char). */
        public fun fromAsset(context: Context, path: String = "kokoro/vocab.json"): KokoroTokenizer {
            val json = context.assets.open(path).bufferedReader().use { it.readText() }
            val obj = JSONObject(json)
            val map = HashMap<Char, Int>(obj.length())
            for (k in obj.keys()) if (k.length == 1) map[k[0]] = obj.getInt(k)
            return KokoroTokenizer(map)
        }
    }
}
