package com.jokobee.tts.core

/** Phonetic alphabet of a pronunciation */
public enum class PhoneticAlphabet {
    IPA,
    ARPABET,
    X_SAMPA;

    /** Converts a pronunciation from this alphabet to IPA */
    public fun toIpa(pronunciation: String, onWarn: (String) -> Unit = {}): String = when (this) {
        IPA -> pronunciation
        ARPABET -> ArpabetToIpa.toIpa(pronunciation, onWarn)
        X_SAMPA -> throw UnsupportedOperationException("X-SAMPA support planned — use IPA or ARPABET")
    }
}
