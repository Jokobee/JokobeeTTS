package com.jokobee.tts.free

import com.jokobee.tts.core.G2p
import com.jokobee.tts.core.UnsupportedLanguageException
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Frontend (normalisation → G2P) — teste le CÂBLAGE avec un G2P stub (le vrai
 * CharsiuG2p exige le modèle exporté). Prouve que la normalisation s'exécute AVANT
 * le G2P, et que les locales non-fr délèguent chaque token au G2P.
 */
class FrontendTest {

    private class StubG2p : G2p {
        val seen = mutableListOf<String>()
        override fun phonemize(word: String, lang: String): String {
            seen += word
            return "[${word.lowercase()}]"
        }
    }

    @Test fun normalizesBeforeG2p() {
        val stub = StubG2p()
        Frontend(stub).toPhonemes("Il reste 5 pommes", "fr")
        assertTrue("le G2P doit voir des mots", stub.seen.isNotEmpty())
        assertTrue("aucun chiffre ne doit atteindre le G2P", stub.seen.none { it.any(Char::isDigit) })
        assertTrue("5 doit devenir 'cinq' avant le G2P", "cinq" in stub.seen)
    }

    @Test fun nonFrenchDelegatesTokensToG2p() {
        val stub = StubG2p()
        Frontend(stub).toPhonemes("東京", "ja")
        assertTrue("東京" in stub.seen)
    }

    @Test fun outputIsNonEmptyIpa() {
        val out = Frontend(StubG2p()).toPhonemes("hola mundo", "es")
        assertTrue(out.isNotBlank())
    }

    @Test fun unsupportedLangThrows() {
        assertThrows(UnsupportedLanguageException::class.java) {
            Frontend(StubG2p()).toPhonemes("hello", "xx")
        }
    }

    @Test fun englishRoutesToEnPhonemizer() {
        // en_US -> le phonémiseur misaki (niveau phrase), PAS le G2P mot-à-mot
        val stub = StubG2p()
        val out = Frontend(stub, enG2p = { "MISAKI:$it" }).toPhonemes("hello world", "en_US")
        assertTrue(out.startsWith("MISAKI:"))
        assertTrue("le G2P mot-à-mot ne doit pas être utilisé en anglais misaki", stub.seen.isEmpty())
    }
}
