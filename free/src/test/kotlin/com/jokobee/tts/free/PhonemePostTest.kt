package com.jokobee.tts.free

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.Normalizer

class PhonemePostTest {

    @Test fun forcesNfdDecomposition() {
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
        assertEquals("ɡ", PhonemePost.apply("g", "fr"))          // g -> ɡ
        assertEquals("teŋɡo", PhonemePost.apply("teŋgo", "es"))  // teŋgo -> teŋɡo
        assertEquals("ɡʁɑ̃", PhonemePost.apply("gʁɑ̃", "fr")) // grã
    }

    @Test fun dropsHaspireMarker() {
        assertEquals("ɔm", PhonemePost.apply("ʼɔm", "fr"))
    }

    @Test fun mapsEnglishRhoticAndDarkL() {
        assertEquals("pˈɔl", PhonemePost.apply("pˈɔɫ", "en_US"))       // Paul
        assertEquals("wˈɜɹld", PhonemePost.apply("wˈɝld", "en_US"))    // world
        assertEquals("bˈɛtəɹ", PhonemePost.apply("bˈɛtɚ", "en_US"))    // better
    }

    @Test fun tieBarAffricatesToLigatures() {
        assertEquals("ʤiɐ", PhonemePost.apply("d͡ʒiɐ", "pt_BR"))   // dia (pt)
        assertEquals("ʧao", PhonemePost.apply("t͡ʃao", "it"))       // ciao (it)
        assertEquals("ʣ", PhonemePost.apply("d͡z", "it"))
        assertEquals("ɡratʦje", PhonemePost.apply("ɡratt͡sje", "it")) // grazie (géminée tt + t͡s→ʦ)
    }

    @Test fun strayTieBarRemoved() {
        assertEquals("ab", PhonemePost.apply("a͡b", "es"))          // tie-bar résiduel supprimé
    }

    @Test fun italianSilentH() {
        // le h italien n'est jamais phonémique (h muet) -> supprimé pour "it" uniquement
        assertEquals("o", PhonemePost.apply("ho", "it"))                 // ho -> o
        assertEquals("anno", PhonemePost.apply("hanno", "it"))           // hanno -> anno
        assertEquals("ho", PhonemePost.apply("ho", "es"))                // conservé ailleurs
    }
}
