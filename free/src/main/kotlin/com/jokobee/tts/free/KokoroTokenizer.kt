package com.jokobee.tts.free

import android.content.Context
import org.json.JSONObject
import java.text.Normalizer

/** Tokeniseur d'entrée du modèle Kokoro-82M v1.0 */
public class KokoroTokenizer(private val vocab: Map<Char, Int>) {

    /** Encode des phonèmes en identifiants. */
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

    /** Vrai si le caractère est dans le vocabulaire. */
    public operator fun contains(c: Char): Boolean = c in vocab

    public companion object {
        /** Charge le vocabulaire depuis les assets. */
        public fun fromAsset(context: Context, path: String = "kokoro/vocab.json"): KokoroTokenizer {
            val json = context.assets.open(path).bufferedReader().use { it.readText() }
            val obj = JSONObject(json)
            val map = HashMap<Char, Int>(obj.length())
            for (k in obj.keys()) if (k.length == 1) map[k[0]] = obj.getInt(k)
            return KokoroTokenizer(map)
        }
    }
}
