package com.jokobee.tts.free

/** Ajoute un silence de tête et de queue à une forme d'onde */
public object AudioPad {

    /** Forme d'onde encadrée de `leadMs` ms de silence en tête et `trailMs` ms en queue. */
    public fun pad(
        samples: FloatArray,
        sampleRate: Int = 24000,
        leadMs: Int = 200,
        trailMs: Int = 100,
    ): FloatArray {
        val lead = sampleRate * leadMs / 1000      // ex. 200 ms @ 24 kHz = 4800 zéros
        val trail = sampleRate * trailMs / 1000
        if (lead == 0 && trail == 0) return samples
        val out = FloatArray(lead + samples.size + trail)   // FloatArray est initialisé à 0.0
        System.arraycopy(samples, 0, out, lead, samples.size)
        return out
    }
}
