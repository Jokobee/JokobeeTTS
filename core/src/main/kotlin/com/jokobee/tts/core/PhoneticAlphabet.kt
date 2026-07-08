package com.jokobee.tts.core

/**
 * Alphabet phonétique d'une prononciation. **Déclaré par l'appelant, jamais deviné.**
 * Chaque alphabet a un adaptateur vers l'IPA (format canonique interne) ; le stockage du
 * lexique est TOUJOURS en IPA.
 *
 * - [IPA] : pass-through (le clamp de vocab est appliqué en aval du pipeline).
 * - [ARPABET] : via [ArpabetToIpa] (CMUdict, 39 phonèmes).
 * - [X_SAMPA] : différé (lève [UnsupportedOperationException]).
 */
public enum class PhoneticAlphabet {
    IPA,
    ARPABET,
    X_SAMPA;

    /** Convertit une prononciation de cet alphabet vers l'IPA. `onWarn` : tokens inconnus. */
    public fun toIpa(pronunciation: String, onWarn: (String) -> Unit = {}): String = when (this) {
        IPA -> pronunciation
        ARPABET -> ArpabetToIpa.toIpa(pronunciation, onWarn)
        X_SAMPA -> throw UnsupportedOperationException("X-SAMPA support planned — use IPA or ARPABET")
    }
}
