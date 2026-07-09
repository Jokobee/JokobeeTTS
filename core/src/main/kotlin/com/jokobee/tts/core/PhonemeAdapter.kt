package com.jokobee.tts.core

/** Adapts phonemes (accent) after G2P. */
public interface PhonemeAdapter {
    public val id: String
    public val baseLang: String
    public val targetLocale: String
    public fun adapt(phonemes: String, context: AdapterContext): String
}

/** Context provided to [PhonemeAdapter]. */
public data class AdapterContext(
    public val word: String,
    public val lang: String,
    public val syllables: List<Syllable>?,
)

/** Syllable: phonemes and openness. */
public data class Syllable(
    public val phonemes: String,
    public val isOpen: Boolean,
)
