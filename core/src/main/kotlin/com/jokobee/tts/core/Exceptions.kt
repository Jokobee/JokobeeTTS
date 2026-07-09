package com.jokobee.tts.core

/** Base class for all JokobeeTTS exceptions. */
public open class JokobeeTtsException(message: String) : RuntimeException(message)

/** Invalid voice at load time (shape/dtype/lang/truncation) */
public class VoiceError(message: String) : JokobeeTtsException(message)

/** A requested capability (locale pipeline, engine) is not available. */
public class PipelineNotAvailableException(message: String) : JokobeeTtsException(message)

/** Locale outside the supported set (fr, en_US, en_GB, es, it, pt_BR). */
public class UnsupportedLanguageException(lang: String) :
    JokobeeTtsException("Locale non supportée : $lang")

/** Feature requiring JokobeeTTS Pro. */
public class ProRequiredException(message: String) : JokobeeTtsException(message)

/** Adapter incompatible with the synthesis language. */
public class AdapterIncompatibleException(message: String) : JokobeeTtsException(message)
