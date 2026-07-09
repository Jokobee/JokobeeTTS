package com.jokobee.tts.free

/** Adds leading and trailing silence to a waveform */
public object AudioPad {

    /** Waveform padded with `leadMs` ms of silence at the start and `trailMs` ms at the end. */
    public fun pad(
        samples: FloatArray,
        sampleRate: Int = 24000,
        leadMs: Int = 200,
        trailMs: Int = 100,
    ): FloatArray {
        val lead = sampleRate * leadMs / 1000      // e.g. 200 ms @ 24 kHz = 4800 zeros
        val trail = sampleRate * trailMs / 1000
        if (lead == 0 && trail == 0) return samples
        val out = FloatArray(lead + samples.size + trail)   // FloatArray is initialized to 0.0
        System.arraycopy(samples, 0, out, lead, samples.size)
        return out
    }
}
