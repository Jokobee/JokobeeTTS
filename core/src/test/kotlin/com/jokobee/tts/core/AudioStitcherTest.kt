package com.jokobee.tts.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AudioStitcherTest {

    private fun const(n: Int, v: Float) = FloatArray(n) { v }
    private fun peak(a: FloatArray): Float { var m = 0f; for (x in a) if (abs(x) > m) m = abs(x); return m }

    @Test fun singleSegment_passthrough() {
        val a = const(1000, 0.5f)
        assertSame(a, AudioStitcher.stitch(listOf(a)))   // returned as-is
    }

    @Test fun twoSegments_silenceInserted() {
        val a = const(500, 0.5f); val b = const(500, 0.5f)
        val cfg = StitchConfig(silenceBetweenMs = 300, crossfadeMs = 0, peakNormalize = false)
        val out = AudioStitcher.stitch(listOf(a, b), cfg)
        assertEquals(500 + 7200 + 500, out.size)          // 300 ms @ 24k = 7200 samples
        for (i in 500 until 500 + 7200) assertEquals(0f, out[i], 0f)  // silence
    }

    @Test fun crossfade_noClicks() {
        val a = const(2000, 0.5f); val b = const(2000, 0.5f)
        val cfg = StitchConfig(crossfadeMs = 20, silenceBetweenMs = 300, peakNormalize = false)
        val out = AudioStitcher.stitch(listOf(a, b), cfg)
        assertTrue("start edge faded", abs(out[0]) < 0.01f)
        assertTrue("segment end edge faded", abs(out[1999]) < 0.01f)  // last sample of a
        assertTrue("global last sample faded", abs(out[out.size - 1]) < 0.01f)
    }

    @Test fun peakNormalize_targetReached() {
        val a = const(500, 0.3f); val b = const(500, 0.5f)
        val out = AudioStitcher.stitch(listOf(a, b), StitchConfig(crossfadeMs = 0, peakTarget = 0.95f))
        assertEquals(0.95f, peak(out), 0.0095f)           // ±1%
    }

    @Test fun peakNormalize_disabled() {
        val a = const(500, 0.3f); val b = const(500, 0.5f)
        val out = AudioStitcher.stitch(listOf(a, b), StitchConfig(crossfadeMs = 0, peakNormalize = false))
        assertEquals(0.5f, peak(out), 1e-6f)              // signal unchanged
    }

    @Test fun configurable_silenceMs_zero() {
        val a = const(500, 0.5f); val b = const(500, 0.5f)
        val out = AudioStitcher.stitch(listOf(a, b), StitchConfig(silenceBetweenMs = 0, crossfadeMs = 0, peakNormalize = false))
        assertEquals(1000, out.size)                      // no silence
    }

    @Test fun empty_returnsEmpty() {
        assertEquals(0, AudioStitcher.stitch(emptyList()).size)
    }
}
