package com.jokobee.tts.free

import com.jokobee.tts.core.G2p
import org.junit.Assert.assertEquals
import org.junit.Test

/** Cache LRU du G2P : mémoïse (mot,lang), distingue la langue, borne la taille. */
class CachingG2pTest {

    /** Compte les appels réels au modèle (délégué). */
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
        assertEquals(1, inner.calls)   // modèle appelé une seule fois
    }

    @Test fun distinguishesLang() {
        val inner = CountingG2p()
        val g2p = CachingG2p(inner)
        assertEquals("[fr:sol]", g2p.phonemize("sol", "fr"))
        assertEquals("[es:sol]", g2p.phonemize("sol", "es"))   // même mot, autre langue
        assertEquals(2, inner.calls)
        assertEquals(2, g2p.size())
    }

    @Test fun evictsBeyondCapacity() {
        val inner = CountingG2p()
        val g2p = CachingG2p(inner, maxEntries = 2)
        g2p.phonemize("a", "fr")
        g2p.phonemize("b", "fr")
        g2p.phonemize("c", "fr")          // évince "a" (le plus ancien)
        assertEquals(2, g2p.size())
        g2p.phonemize("a", "fr")          // "a" évincé → re-calcul
        assertEquals(4, inner.calls)
    }

    @Test fun accessOrderKeepsHotEntries() {
        val inner = CountingG2p()
        val g2p = CachingG2p(inner, maxEntries = 2)
        g2p.phonemize("a", "fr")
        g2p.phonemize("b", "fr")
        g2p.phonemize("a", "fr")          // "a" redevient récent (hit, pas d'appel)
        g2p.phonemize("c", "fr")          // évince "b" (le moins récent), pas "a"
        assertEquals(3, inner.calls)      // a,b,c calculés une fois ; le 2e "a" = hit
        g2p.phonemize("a", "fr")          // toujours en cache → hit
        assertEquals(3, inner.calls)
    }
}
