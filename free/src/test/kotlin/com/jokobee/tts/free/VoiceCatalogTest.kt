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

/** Voice registry */
class VoiceCatalogTest {

    private val N = VoiceFormat.N_ROWS
    private val D = VoiceFormat.STYLE_DIM

    /** Deterministic embedding */
    private fun emb(seed: Int = 0): FloatArray =
        FloatArray(N * D) { ((it / D) + seed) * 0.001f }

    private fun bytesOf(floats: FloatArray): ByteArray {
        val buf = ByteBuffer.allocate(floats.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        buf.asFloatBuffer().put(floats)
        return buf.array()
    }

    /** Test subclass */
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
        // Voice built from bytes == built from FloatArray (single path).
        val floats = emb(3)
        val fromFloats = Voice.of("a", "en_US", floats)
        val fromBytes = Voice.of("b", "en_US", bytesOf(floats))
        assertArrayEquals(fromFloats.copyEmbedding(), fromBytes.copyEmbedding(), 0f)
    }

    @Test fun styleForClampsAndIndexes() {
        val v = Voice.of("v", "fr", emb())
        assertArrayEquals(FloatArray(D) { 0f }, v.styleFor(0), 0f)          // row 0
        assertArrayEquals(FloatArray(D) { 5 * 0.001f }, v.styleFor(5), 0f)  // row 5
        assertArrayEquals(FloatArray(D) { 0f }, v.styleFor(-7), 0f)         // < 0 → 0
        val last = FloatArray(D) { (N - 1) * 0.001f }
        assertArrayEquals(last, v.styleFor(N + 1000), 0f)                   // > 509 → 509
        assertArrayEquals(last, v.styleFor(N - 1), 0f)
    }


    @Test fun truncatedBytesRejected() {
        val ex = assertThrows(VoiceError::class.java) {
            Voice.of("t", "fr", bytesOf(emb()).copyOf(VoiceFormat.EXPECTED_BYTES - 4))
        }
        assertTrue(ex.message!!.contains("expected"))
    }

    @Test fun oversizedBytesRejected() {
        assertThrows(VoiceError::class.java) {
            Voice.of("t", "fr", bytesOf(emb()) + byteArrayOf(0, 0, 0, 0))
        }
    }

    @Test fun nonFiniteBytesRejected() {
        val nan = FloatArray(N * D) { Float.NaN }
        val ex = assertThrows(VoiceError::class.java) { Voice.of("t", "fr", bytesOf(nan)) }
        assertTrue(ex.message!!.lowercase().contains("non-finite"))
    }

    @Test fun badShapeFloatsRejected() {
        val ex = assertThrows(VoiceError::class.java) { Voice.of("t", "fr", FloatArray(10)) }
        assertTrue(ex.message!!.contains("invalid shape"))
    }

    @Test fun unknownLangRejected() {
        val ex = assertThrows(VoiceError::class.java) { Voice.of("t", "xx", emb()) }
        assertTrue(ex.message!!.contains("unknown lang"))
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
        assertEquals(listOf("am_adam", "ff_siwis"), cat.list().map { it.id }) // sorted by id
    }

    @Test fun getUnknownRejected() {
        val ex = assertThrows(VoiceError::class.java) { OpenCatalog().get("inexistante") }
        assertTrue(ex.message!!.contains("unknown voice"))
    }
}
