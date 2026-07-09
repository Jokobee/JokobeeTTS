package com.jokobee.tts.free

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tokenizer */
class ByT5TokenizerTest {

    @Test fun asciiBytesOffsetPlusThree() {
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
        assertEquals("A", ByT5Tokenizer.decode(longArrayOf(0, 1, 2, 68, 300)))
    }

    @Test fun promptFormatMatchesHfBench() {
        // "<fra>: bonjour" must encode byte-for-byte, like the reference
        val ids = ByT5Tokenizer.encode(G2pLangTag.prompt("fr", "bonjour"))
        assertArrayEquals(longArrayOf(63, 105, 117, 100, 65, 61, 35), ids.copyOfRange(0, 7))
        val expected = "<fra>: bonjour".toByteArray(Charsets.UTF_8)
            .map { (it.toInt() and 0xFF).toLong() + 3 }.toLongArray()
        assertArrayEquals(expected, ids)
    }
}
