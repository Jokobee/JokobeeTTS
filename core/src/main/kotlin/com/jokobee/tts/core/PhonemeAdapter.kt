package com.jokobee.tts.core

/** Adapte les phonèmes (accent) après le G2P. */
public interface PhonemeAdapter {
    public val id: String
    public val baseLang: String
    public val targetLocale: String
    public fun adapt(phonemes: String, context: AdapterContext): String
}

/** Contexte fourni au [PhonemeAdapter]. */
public data class AdapterContext(
    public val word: String,
    public val lang: String,
    public val syllables: List<Syllable>?,
)

/** Syllabe : phonèmes et ouverture. */
public data class Syllable(
    public val phonemes: String,
    public val isOpen: Boolean,
)
