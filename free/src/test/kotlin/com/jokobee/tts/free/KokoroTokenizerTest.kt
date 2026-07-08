package com.jokobee.tts.free

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tokeniseur Kokoro : ids = [0] + chars du vocab (après NFD) + [0] ; nTokens = taille−2.
 * Testé avec un vocab injecté (le vrai vient de assets/kokoro/vocab.json, 115 entrées).
 */
class KokoroTokenizerTest {

    // vocab minimal ; 'e'=5 mais PAS le tilde combinant U+0303, ni l'accent U+0301
    private val vocab = mapOf('a' to 1, 'b' to 2, 'ɛ' to 3, 'e' to 5, ' ' to 9)
    private val tok = KokoroTokenizer(vocab)

    @Test fun bracketsWithZeroAndMaps() {
        assertArrayEquals(longArrayOf(0, 1, 2, 0), tok.encode("ab"))
        assertArrayEquals(longArrayOf(0, 1, 9, 3, 0), tok.encode("a ɛ"))
    }

    @Test fun dropsCharsOutsideVocab() {
        // 'z' absent → ignoré ; il reste a,b
        assertArrayEquals(longArrayOf(0, 1, 2, 0), tok.encode("azb"))
    }

    @Test fun appliesNfdBeforeLookup() {
        // "é" composé (U+00E9) -> NFD "e"+U+0301 ; 'e'=5 dans le vocab, l'accent non -> seul 'e'
        assertArrayEquals(longArrayOf(0, 5, 0), tok.encode("é"))
    }

    @Test fun nTokensExcludesBoundaries() {
        assertEquals(2, tok.nTokens(tok.encode("ab")))
        assertEquals(0, tok.nTokens(tok.encode("")))       // [0,0] -> 0
        assertEquals(0, tok.nTokens(tok.encode("zzz")))    // tout hors vocab -> [0,0]
    }

    @Test fun containsReflectsVocab() {
        assertTrue('a' in tok)
        assertFalse('z' in tok)
    }
}
