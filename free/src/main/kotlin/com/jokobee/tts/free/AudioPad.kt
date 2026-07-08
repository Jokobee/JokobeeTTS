package com.jokobee.tts.free

/**
 * Ajoute un silence de **tête** et de **queue** à une forme d'onde.
 *
 * Pourquoi (pratique standard TTS) : Kokoro attaque l'audio dès le 1er échantillon.
 * Or un player a une latence d'initialisation au premier `play()` → le début est
 * tronqué. Un silence de tête (~200 ms) absorbe cette latence ; un silence de queue
 * (~100 ms) évite un « pop » de fin. Les silences sont des zéros (f32 0.0).
 */
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
