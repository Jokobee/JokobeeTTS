package com.jokobee.tts.free

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.Normalizer

/** PhonemePost — NFD imposé (attendu du tokeniseur Kokoro) + pas de stripping. */
class PhonemePostTest {

    @Test fun forcesNfdDecomposition() {
        // "é" composé (1 code point) → "e" + U+0301 (2 code points) après NFD
        val out = PhonemePost.apply("é", "fr")
        assertEquals(2, out.length)
        assertEquals('e', out[0])
        assertEquals('́', out[1])
        assertTrue(Normalizer.isNormalized(out, Normalizer.Form.NFD))
    }

    @Test fun keepsSuprasegmentals() {
        // stress ˈ ˌ et tons ne sont pas strippés
        val ipa = "ˈhɛˌloʊ"
        assertEquals(Normalizer.normalize(ipa, Normalizer.Form.NFD), PhonemePost.apply(ipa, "en_US"))
    }

    @Test fun unknownLangIsNfdOnly() {
        val ipa = "abc"
        assertEquals("abc", PhonemePost.apply(ipa, "xx"))
    }

    @Test fun idempotentModuloNfd() {
        val once = PhonemePost.apply("naïve ɑ̃", "fr")
        assertEquals(once, PhonemePost.apply(once, "fr"))
    }

    @Test fun mapsAsciiGToIpaG() {
        // 'g' ASCII (U+0067, sorti par CharsiuG2P) -> 'ɡ' IPA (U+0261, seul dans le vocab
        // Kokoro) ; sans ce mapping le g serait droppé (audit OOV : 64 mots fr + 42 es).
        assertEquals("ɡ", PhonemePost.apply("g", "fr"))          // g -> ɡ
        assertEquals("teŋɡo", PhonemePost.apply("teŋgo", "es"))  // teŋgo -> teŋɡo
        assertEquals("ɡʁɑ̃", PhonemePost.apply("gʁɑ̃", "fr")) // grã
    }

    @Test fun dropsHaspireMarker() {
        // ʼ (U+02BC) marqueur de h muet -> supprimé explicitement (« hommes→ʼɔm » -> « ɔm »)
        assertEquals("ɔm", PhonemePost.apply("ʼɔm", "fr"))
    }
}
