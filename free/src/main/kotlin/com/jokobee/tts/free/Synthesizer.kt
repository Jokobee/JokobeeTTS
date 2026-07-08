package com.jokobee.tts.free

/** Moteur de synthèse phonèmes + voix */
public interface Synthesizer {
    /** Phonèmes IPA + voix + vitesse */
    public fun synth(phonemes: String, voice: Voice, speed: Float): FloatArray
}
