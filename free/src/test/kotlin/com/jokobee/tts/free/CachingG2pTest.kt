package com.jokobee.tts.free

import com.jokobee.tts.core.G2p
import org.junit.Assert.assertEquals
import org.junit.Test

/** Cache */
class CachingG2pTest {

    /** Counts actual calls to the (delegate) model. */
    private class CountingG2p : G2p {
        var calls = 0
        override fun phonemize(word: String, lang: String): String {
            calls++
            return "[$lang:$word]"
        }
    }

    @Test fun memoizesRepeatedWord() {
        val inner = CountingG2p()
        val g2p = CachingG2p(inner)
        assertEquals("[fr:chat]", g2p.phonemize("chat", "fr"))
        assertEquals("[fr:chat]", g2p.phonemize("chat", "fr"))
        assertEquals("[fr:chat]", g2p.phonemize("chat", "fr"))
        assertEquals(1, inner.calls)   // model called only once
    }

    @Test fun distinguishesLang() {
        val inner = CountingG2p()
        val g2p = CachingG2p(inner)
        assertEquals("[fr:sol]", g2p.phonemize("sol", "fr"))
        assertEquals("[es:sol]", g2p.phonemize("sol", "es"))   // same word, different language
        assertEquals(2, inner.calls)
        assertEquals(2, g2p.size())
    }

    @Test fun evictsBeyondCapacity() {
        val inner = CountingG2p()
        val g2p = CachingG2p(inner, maxEntries = 2)
        g2p.phonemize("a", "fr")
        g2p.phonemize("b", "fr")
        g2p.phonemize("c", "fr")          // evicts "a" (the oldest)
        assertEquals(2, g2p.size())
        g2p.phonemize("a", "fr")          // "a" evicted → recompute
        assertEquals(4, inner.calls)
    }

    @Test fun accessOrderKeepsHotEntries() {
        val inner = CountingG2p()
        val g2p = CachingG2p(inner, maxEntries = 2)
        g2p.phonemize("a", "fr")
        g2p.phonemize("b", "fr")
        g2p.phonemize("a", "fr")          // "a" becomes recent again (hit, no call)
        g2p.phonemize("c", "fr")          // evicts "b" (the least recent), not "a"
        assertEquals(3, inner.calls)      // a,b,c computed once; the 2nd "a" = hit
        g2p.phonemize("a", "fr")          // still cached → hit
        assertEquals(3, inner.calls)
    }
}
