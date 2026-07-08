package com.jokobee.tts.core

/** Alphabet phonétique d'une prononciation */
public enum class PhoneticAlphabet {
    IPA,
    ARPABET,
    X_SAMPA;

    /** Convertit une prononciation de cet alphabet vers l'IPA */
    public fun toIpa(pronunciation: String, onWarn: (String) -> Unit = {}): String = when (this) {
        IPA -> pronunciation
        ARPABET -> ArpabetToIpa.toIpa(pronunciation, onWarn)
        X_SAMPA -> throw UnsupportedOperationException("X-SAMPA support planned — use IPA or ARPABET")
    }
}
