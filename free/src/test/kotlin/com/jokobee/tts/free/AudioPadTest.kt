package com.jokobee.tts.free

import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Test

/** AudioPad */
class AudioPadTest {

    @Test fun addsLeadAndTrailSilence() {
        val out = AudioPad.pad(floatArrayOf(0.5f, -0.5f), sampleRate = 24000, leadMs = 200, trailMs = 100)
        assertEquals(4800 + 2 + 2400, out.size)           // 200ms + 2 + 100ms @ 24kHz
        assertEquals(0f, out[0], 0f)                       // head = silence
        assertEquals(0f, out[4799], 0f)
        assertEquals(0.5f, out[4800], 0f)                  // 1st audio sample after the head
        assertEquals(-0.5f, out[4801], 0f)
        assertEquals(0f, out[4802], 0f)                    // tail = silence
        assertEquals(0f, out[out.size - 1], 0f)
    }

    @Test fun zeroPaddingReturnsInput() {
        val input = floatArrayOf(1f, 2f, 3f)
        val out = AudioPad.pad(input, leadMs = 0, trailMs = 0)
        assertArrayEquals(input, out, 0f)
    }

    @Test fun scalesWithSampleRate() {
        val out = AudioPad.pad(floatArrayOf(1f), sampleRate = 16000, leadMs = 100, trailMs = 0)
        assertEquals(1600 + 1, out.size)                   // 100ms @ 16kHz = 1600
    }
}
