package com.jokobee.tts.free

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tokeniseur ByT5 (byte-level) — spec google/byt5 : octet b → id b+3.
 * CharsiuG2P encode SANS eos (add_special_tokens=False, validé au banc d'export) :
 * `encode` n'ajoute donc PAS d'eos par défaut. Déterministe, sans fichier de vocab.
 */
class ByT5TokenizerTest {

    @Test fun asciiBytesOffsetPlusThree() {
        // 'A'=0x41=65 → 68 ; 'B'=66 → 69 ; 'C'=67 → 70 ; pas d'eos par défaut
        assertArrayEquals(longArrayOf(68, 69, 70), ByT5Tokenizer.encode("ABC"))
    }

    @Test fun addEosAppendsOne() {
        assertArrayEquals(longArrayOf(68, 69, 70, 1), ByT5Tokenizer.encode("ABC", addEos = true))
    }

    @Test fun emptyStringIsEmpty() {
        assertArrayEquals(longArrayOf(), ByT5Tokenizer.encode(""))
        assertArrayEquals(longArrayOf(1), ByT5Tokenizer.encode("", addEos = true))
    }

    @Test fun multibyteUtf8CountsPerByte() {
        // 'é' = C3 A9 (2 octets) → 195+3=198, 169+3=172 (sans eos)
        assertArrayEquals(longArrayOf(198, 172), ByT5Tokenizer.encode("é"))
    }

    @Test fun roundTripAscii() {
        assertEquals("bonjour", ByT5Tokenizer.decode(ByT5Tokenizer.encode("bonjour")))
    }

    @Test fun roundTripUnicode() {
        val txt = "paʁi ɛ 東京 안녕"
        assertEquals(txt, ByT5Tokenizer.decode(ByT5Tokenizer.encode(txt)))
    }

    @Test fun decodeIgnoresSpecialsAndSentinels() {
        // pad(0), eos(1), unk(2) et une sentinelle (300) sont ignorés ; 'A'=68 conservé
        assertEquals("A", ByT5Tokenizer.decode(longArrayOf(0, 1, 2, 68, 300)))
    }

    @Test fun promptFormatMatchesHfBench() {
        // "<fra>: bonjour" doit s'encoder octet à octet, comme le banc d'export HF
        // (ids bruts validés : [63,105,117,100,65,61,35,101,114,113,109,114,...])
        val ids = ByT5Tokenizer.encode(G2pLangTag.prompt("fr", "bonjour"))
        assertArrayEquals(longArrayOf(63, 105, 117, 100, 65, 61, 35), ids.copyOfRange(0, 7))
        val expected = "<fra>: bonjour".toByteArray(Charsets.UTF_8)
            .map { (it.toInt() and 0xFF).toLong() + 3 }.toLongArray()
        assertArrayEquals(expected, ids)
    }
}
