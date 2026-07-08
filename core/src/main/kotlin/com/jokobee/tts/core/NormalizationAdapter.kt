package com.jokobee.tts.core

/** Adapte le texte brut en amont du normaliseur standard. */
public interface NormalizationAdapter {
    public val id: String
    public val langs: Set<String>
    public fun adapt(text: String, lang: String, context: NormalizationContext): String
}

/** Contexte fourni au [NormalizationAdapter]. */
public data class NormalizationContext(
    public val locale: String,
    public val accent: String?,
)
