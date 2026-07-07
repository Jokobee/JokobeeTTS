package com.jokobee.tts.free

import com.jokobee.tts.core.UnsupportedLanguageException

/**
 * Fabrique de normaliseurs par locale. Route une des 10 locales supportées vers son
 * [BaseNormalizer]. Toutes les langues sont disponibles en Free (pas de paywall langue —
 * le split Free/Pro se fait sur les features, cf. charte produit).
 */
public object Normalizers {

    /** Les 10 locales gérées (fr et fr_CA partagent [FrenchNormalizer]). */
    public val SUPPORTED: Set<String> = setOf(
        "fr", "fr_CA", "en_US", "en_GB", "es", "it", "pt_BR", "hi", "ja", "zh", "ko",
    )

    public fun forLang(lang: String, verbalizer: Verbalizer): BaseNormalizer = when (lang) {
        "fr", "fr_CA" -> FrenchNormalizer(verbalizer)
        "en_US" -> EnglishNormalizer(verbalizer)
        "en_GB" -> BritishEnglishNormalizer(verbalizer)
        "es" -> SpanishNormalizer(verbalizer)
        "it" -> ItalianNormalizer(verbalizer)
        "pt_BR" -> BrazilianPortugueseNormalizer(verbalizer)
        "hi" -> HindiNormalizer(verbalizer)
        "ja" -> JapaneseNormalizer(verbalizer)
        "zh" -> ChineseNormalizer(verbalizer)
        "ko" -> KoreanNormalizer(verbalizer)
        else -> throw UnsupportedLanguageException(lang)
    }
}
