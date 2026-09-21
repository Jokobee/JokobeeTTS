package com.jokobee.tts.free

/** Phoneme + voice synthesis engine */
public interface Synthesizer : java.io.Closeable {
    /** Releases whatever this implementation holds. No-op by default. */
    override fun close() {}

    /** IPA phonemes + voice + speed */
    public fun synth(phonemes: String, voice: Voice, speed: Float): FloatArray
}
