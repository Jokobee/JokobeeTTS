package com.jokobee.tts.core

/** Base de toutes les exceptions JokobeeTTS. */
public open class JokobeeTtsException(message: String) : RuntimeException(message)

/** Voix invalide au chargement (shape/dtype/lang/troncature) */
public class VoiceError(message: String) : JokobeeTtsException(message)

/** Une capacité demandée (pipeline d'une locale, moteur) n'est pas disponible. */
public class PipelineNotAvailableException(message: String) : JokobeeTtsException(message)

/** Locale hors des 10 supportées (fr, en_US, en_GB, es, it, pt_BR, hi, ja, zh, ko). */
public class UnsupportedLanguageException(lang: String) :
    JokobeeTtsException("Locale non supportée : $lang")
