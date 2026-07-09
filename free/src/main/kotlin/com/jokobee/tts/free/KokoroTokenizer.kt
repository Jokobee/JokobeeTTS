package com.jokobee.tts.free

import android.content.Context
import org.json.JSONObject
import java.text.Normalizer

/** Input tokenizer for the Kokoro-82M v1.0 model */
public class KokoroTokenizer(private val vocab: Map<Char, Int>) {

    /** Encodes phonemes into ids. */
    public fun encode(phonemes: String): LongArray {
        val nfd = Normalizer.normalize(phonemes, Normalizer.Form.NFD)
        val ids = ArrayList<Long>(nfd.length + 2)
        ids.add(0L)
        for (c in nfd) vocab[c]?.let { ids.add(it.toLong()) }
        ids.add(0L)
        return ids.toLongArray()
    }

    /** Number of tokens excluding boundaries (used to index the voice style). */
    public fun nTokens(ids: LongArray): Int = maxOf(0, ids.size - 2)

    /** True if the character is in the vocabulary. */
    public operator fun contains(c: Char): Boolean = c in vocab

    public companion object {
        /** Loads the vocabulary from assets. */
        public fun fromAsset(context: Context, path: String = "kokoro/vocab.json"): KokoroTokenizer {
            val json = context.assets.open(path).bufferedReader().use { it.readText() }
            val obj = JSONObject(json)
            val map = HashMap<Char, Int>(obj.length())
            for (k in obj.keys()) if (k.length == 1) map[k[0]] = obj.getInt(k)
            return KokoroTokenizer(map)
        }
    }
}
