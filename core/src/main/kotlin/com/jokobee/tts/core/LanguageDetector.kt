package com.jokobee.tts.core

/** Valeur de `lang` demandant la détection automatique de la locale. */
public const val AUTO: String = "auto"

/** Détection de la locale d'un texte parmi les locales supportées. Implémentée par le tier Pro. */
public fun interface LanguageDetector {
    /** Locale supportée la plus probable, ou `null` si indéterminée. */
    public fun detect(text: String): String?
}
