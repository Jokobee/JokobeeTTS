package com.jokobee.tts.free

/** Phoneme + voice synthesis engine */
public interface Synthesizer {
    /** IPA phonemes + voice + speed */
    public fun synth(phonemes: String, voice: Voice, speed: Float): FloatArray
}
