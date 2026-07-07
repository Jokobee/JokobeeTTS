package com.jokobee.tts.free

import com.jokobee.tts.core.VoiceError
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Registre de voix — surface FREE (Voice + VoiceCatalog). Port des invariants de
 * `test_voice_registry.py` accessibles hors banc : parsing/format autoritaire,
 * indexation du style, round-trip octets, consultation du catalogue, erreurs
 * explicites. L'équivalence AUDIO officiel/custom (onnxruntime + modèle HF) reste
 * un golden test du repo privé — hors scope :free (pas de réseau/modèle en CI).
 */
class VoiceCatalogTest {

    private val N = VoiceFormat.N_ROWS
    private val D = VoiceFormat.STYLE_DIM

    /** Embedding déterministe : la ligne i vaut (i + seed) × 0.001 sur ses 256 dims. */
    private fun emb(seed: Int = 0): FloatArray =
        FloatArray(N * D) { ((it / D) + seed) * 0.001f }

    private fun bytesOf(floats: FloatArray): ByteArray {
        val buf = ByteBuffer.allocate(floats.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        buf.asFloatBuffer().put(floats)
        return buf.array()
    }

    /** Sous-classe de test : expose le `add` protégé pour peupler un catalogue. */
    private class OpenCatalog : VoiceCatalog() {
        fun put(v: Voice): Voice = add(v)
    }

    // --- format / parsing -----------------------------------------------------

    @Test fun parseRoundTripBytes() {
        val v = Voice.of("off", "fr", bytesOf(emb()))
        val reparsed = Voice.of("off2", "fr", v.toBytes())
        assertEquals(VoiceFormat.EXPECTED_BYTES, v.toBytes().size)
        assertArrayEquals(v.copyEmbedding(), reparsed.copyEmbedding(), 0f)
    }

    @Test fun bytesEqualsFloatsPath() {
        // Voix construite depuis octets == construite depuis FloatArray (chemin unique).
        val floats = emb(3)
        val fromFloats = Voice.of("a", "en_US", floats)
        val fromBytes = Voice.of("b", "en_US", bytesOf(floats))
        assertArrayEquals(fromFloats.copyEmbedding(), fromBytes.copyEmbedding(), 0f)
    }

    @Test fun styleForClampsAndIndexes() {
        val v = Voice.of("v", "fr", emb())
        assertArrayEquals(FloatArray(D) { 0f }, v.styleFor(0), 0f)          // ligne 0
        assertArrayEquals(FloatArray(D) { 5 * 0.001f }, v.styleFor(5), 0f)  // ligne 5
        assertArrayEquals(FloatArray(D) { 0f }, v.styleFor(-7), 0f)         // < 0 → 0
        val last = FloatArray(D) { (N - 1) * 0.001f }
        assertArrayEquals(last, v.styleFor(N + 1000), 0f)                   // > 509 → 509
        assertArrayEquals(last, v.styleFor(N - 1), 0f)
    }

    // --- négatifs : erreurs explicites, jamais de crash silencieux -------------

    @Test fun truncatedBytesRejected() {
        val ex = assertThrows(VoiceError::class.java) {
            Voice.of("t", "fr", bytesOf(emb()).copyOf(VoiceFormat.EXPECTED_BYTES - 4))
        }
        assertTrue(ex.message!!.contains("attendu"))
    }

    @Test fun oversizedBytesRejected() {
        assertThrows(VoiceError::class.java) {
            Voice.of("t", "fr", bytesOf(emb()) + byteArrayOf(0, 0, 0, 0))
        }
    }

    @Test fun nonFiniteBytesRejected() {
        val nan = FloatArray(N * D) { Float.NaN }
        val ex = assertThrows(VoiceError::class.java) { Voice.of("t", "fr", bytesOf(nan)) }
        assertTrue(ex.message!!.lowercase().contains("non finies"))
    }

    @Test fun badShapeFloatsRejected() {
        val ex = assertThrows(VoiceError::class.java) { Voice.of("t", "fr", FloatArray(10)) }
        assertTrue(ex.message!!.contains("shape invalide"))
    }

    @Test fun unknownLangRejected() {
        val ex = assertThrows(VoiceError::class.java) { Voice.of("t", "xx", emb()) }
        assertTrue(ex.message!!.contains("lang inconnue"))
    }

    @Test fun emptyIdRejected() {
        assertThrows(VoiceError::class.java) { Voice.of("", "fr", emb()) }
    }

    // --- catalogue (FREE) ------------------------------------------------------

    @Test fun catalogGetListContains() {
        val cat = OpenCatalog()
        cat.put(Voice.of("ff_siwis", "fr", emb(1)))
        cat.put(Voice.of("am_adam", "en_US", emb(2)))
        assertEquals(2, cat.size)
        assertTrue("ff_siwis" in cat)
        assertFalse("nope" in cat)
        assertEquals("ff_siwis", cat.get("ff_siwis").id)
        assertEquals(listOf("am_adam", "ff_siwis"), cat.list().map { it.id }) // trié par id
    }

    @Test fun getUnknownRejected() {
        val ex = assertThrows(VoiceError::class.java) { OpenCatalog().get("inexistante") }
        assertTrue(ex.message!!.contains("voix inconnue"))
    }
}
