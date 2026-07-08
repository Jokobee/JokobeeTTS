package com.jokobee.tts.free

/**
 * Moteur de synthèse phonèmes + voix → forme d'onde. Abstraction du moteur concret
 * ([KokoroSynth]) pour découpler la façade [Tts] (testable par stub, remplaçable).
 */
public interface Synthesizer {
    /** Phonèmes IPA + voix + vitesse → forme d'onde f32 [-1,1] à 24 kHz. */
    public fun synth(phonemes: String, voice: Voice, speed: Float): FloatArray
}
