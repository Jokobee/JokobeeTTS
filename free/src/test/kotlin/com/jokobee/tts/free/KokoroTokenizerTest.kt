package com.jokobee.tts.free

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Kokoro Tokenizer */
class KokoroTokenizerTest {

    private val vocab = mapOf('a' to 1, 'b' to 2, 'ɛ' to 3, 'e' to 5, ' ' to 9)
    private val tok = KokoroTokenizer(vocab)

    @Test fun bracketsWithZeroAndMaps() {
        assertArrayEquals(longArrayOf(0, 1, 2, 0), tok.encode("ab"))
        assertArrayEquals(longArrayOf(0, 1, 9, 3, 0), tok.encode("a ɛ"))
    }

    @Test fun dropsCharsOutsideVocab() {
        assertArrayEquals(longArrayOf(0, 1, 2, 0), tok.encode("azb"))
    }

    @Test fun appliesNfdBeforeLookup() {
        assertArrayEquals(longArrayOf(0, 5, 0), tok.encode("é"))
    }

    @Test fun nTokensExcludesBoundaries() {
        assertEquals(2, tok.nTokens(tok.encode("ab")))
        assertEquals(0, tok.nTokens(tok.encode("")))       // [0,0] -> 0
        assertEquals(0, tok.nTokens(tok.encode("zzz")))    // all outside vocab -> [0,0]
    }

    @Test fun containsReflectsVocab() {
        assertTrue('a' in tok)
        assertFalse('z' in tok)
    }
}
