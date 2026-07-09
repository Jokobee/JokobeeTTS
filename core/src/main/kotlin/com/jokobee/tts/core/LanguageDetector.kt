package com.jokobee.tts.core

/** Value of `lang` requesting automatic locale detection. */
public const val AUTO: String = "auto"

/** Detects the locale of a text among the supported locales. Implemented by the Pro tier. */
public fun interface LanguageDetector {
    /** Most likely supported locale, or `null` if undetermined. */
    public fun detect(text: String): String?
}
