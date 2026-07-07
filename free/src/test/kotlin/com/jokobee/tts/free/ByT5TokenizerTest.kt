package com.jokobee.tts.free

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tokeniseur ByT5 (byte-level) — spec google/byt5 : octet b → id b+3, eos=1.
 * Déterministe, sans fichier de vocab → identique JVM ↔ device.
 */
class ByT5TokenizerTest {

    @Test fun asciiBytesOffsetPlusThreePlusEos() {
        // 'A'=0x41=65 → 68 ; 'B'=66 → 69 ; 'C'=67 → 70 ; puis </s>=1
        assertArrayEquals(longArrayOf(68, 69, 70, 1), ByT5Tokenizer.encode("ABC"))
    }

    @Test fun emptyStringIsJustEos() {
        assertArrayEquals(longArrayOf(1), ByT5Tokenizer.encode(""))
    }

    @Test fun multibyteUtf8CountsPerByte() {
        // 'é' = C3 A9 (2 octets) → 195+3=198, 169+3=172, puis eos
        assertArrayEquals(longArrayOf(198, 172, 1), ByT5Tokenizer.encode("é"))
    }

    @Test fun roundTripAscii() {
        val ids = ByT5Tokenizer.encode("bonjour")
        // decode ignore l'eos final → on retrouve le texte
        assertEquals("bonjour", ByT5Tokenizer.decode(ids))
    }

    @Test fun roundTripUnicode() {
        val txt = "paʁi ɛ 東京 안녕"
        assertEquals(txt, ByT5Tokenizer.decode(ByT5Tokenizer.encode(txt)))
    }

    @Test fun decodeIgnoresSpecialsAndSentinels() {
        // pad(0), eos(1), unk(2) et une sentinelle (300) sont ignorés ; 'A'=68 conservé
        assertEquals("A", ByT5Tokenizer.decode(longArrayOf(0, 1, 2, 68, 300)))
    }

    @Test fun promptFormatBytesAreStable() {
        // "<fra>: a" doit s'encoder octet à octet (pas de token spécial pour la balise)
        val expected = "<fra>: a".toByteArray(Charsets.UTF_8)
            .map { (it.toInt() and 0xFF).toLong() + 3 }.toLongArray() + longArrayOf(1)
        assertArrayEquals(expected, ByT5Tokenizer.encode(G2pLangTag.prompt("fr", "a")))
    }
}
