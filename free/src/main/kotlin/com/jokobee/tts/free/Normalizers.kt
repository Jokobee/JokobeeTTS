package com.jokobee.tts.free

import com.jokobee.tts.core.UnsupportedLanguageException

/** Factory of normalizers per locale */
public object Normalizers {

    /** The supported locales (fr and fr_CA share [FrenchNormalizer]). */
    public val SUPPORTED: Set<String> = setOf(
        "fr", "fr_CA", "en_US", "en_GB", "es", "it", "pt_BR",
    )

    public fun forLang(lang: String, verbalizer: Verbalizer): BaseNormalizer = when (lang) {
        "fr", "fr_CA" -> FrenchNormalizer(verbalizer)
        "en_US" -> EnglishNormalizer(verbalizer)
        "en_GB" -> BritishEnglishNormalizer(verbalizer)
        "es" -> SpanishNormalizer(verbalizer)
        "it" -> ItalianNormalizer(verbalizer)
        "pt_BR" -> BrazilianPortugueseNormalizer(verbalizer)
        else -> throw UnsupportedLanguageException(lang)
    }
}
