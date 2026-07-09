package com.jokobee.tts.core

/** Adapts raw text upstream of the standard normalizer. */
public interface NormalizationAdapter {
    public val id: String
    public val langs: Set<String>
    public fun adapt(text: String, lang: String, context: NormalizationContext): String
}

/** Context provided to [NormalizationAdapter]. */
public data class NormalizationContext(
    public val locale: String,
    public val accent: String?,
)
