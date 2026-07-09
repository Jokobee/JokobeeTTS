package com.jokobee.tts.free

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** WavWriter */
class WavWriterTest {

    private fun ascii(b: ByteArray, off: Int, len: Int) = String(b, off, len, Charsets.US_ASCII)

    @Test fun headerAndSizes() {
        val wav = WavWriter.toWav(floatArrayOf(0f, 1f, -1f), 24000)
        assertEquals(44 + 6, wav.size)                       // 3 samples × 2 bytes
        assertEquals("RIFF", ascii(wav, 0, 4))
        assertEquals("WAVE", ascii(wav, 8, 4))
        assertEquals("fmt ", ascii(wav, 12, 4))
        assertEquals("data", ascii(wav, 36, 4))
        val bb = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(36 + 6, bb.getInt(4))                   // file size − 8
        assertEquals(16, bb.getInt(16))                      // fmt size
        assertEquals(1.toShort(), bb.getShort(20))           // PCM
        assertEquals(1.toShort(), bb.getShort(22))           // mono
        assertEquals(24000, bb.getInt(24))                   // sample rate
        assertEquals(48000, bb.getInt(28))                   // byte rate
        assertEquals(16.toShort(), bb.getShort(34))          // bits
        assertEquals(6, bb.getInt(40))                       // data len
    }

    @Test fun samplesClampAndScale() {
        val wav = WavWriter.toWav(floatArrayOf(0f, 1f, -1f, 2f), 24000)
        val bb = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(0.toShort(), bb.getShort(44))           // 0f -> 0
        assertEquals(32767.toShort(), bb.getShort(46))       // 1f -> +max
        assertEquals((-32767).toShort(), bb.getShort(48))    // -1f -> -max
        assertEquals(32767.toShort(), bb.getShort(50))       // 2f clamped -> +max
    }
}
