package com.jokobee.tts.free

import com.jokobee.tts.core.G2p

/** [G2p] decorator with caching */
public class CachingG2p(
    private val delegate: G2p,
    private val maxEntries: Int = 5000,
) : G2p {
    private val cache = object : LinkedHashMap<String, String>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, String>): Boolean = size > maxEntries
    }

    override fun phonemize(word: String, lang: String): String {
        val key = "$lang $word"
        synchronized(cache) { cache[key]?.let { return it } }
        val ipa = delegate.phonemize(word, lang)
        synchronized(cache) { cache[key] = ipa }
        return ipa
    }

    /** Number of entries currently cached (diagnostic/test). */
    public fun size(): Int = synchronized(cache) { cache.size }
}
