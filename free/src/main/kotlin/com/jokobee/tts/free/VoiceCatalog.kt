package com.jokobee.tts.free

import com.jokobee.tts.core.VoiceError

/** Catalogue des voix disponibles */
public open class VoiceCatalog {
    private val voices = LinkedHashMap<String, Voice>()

    /** Peuplement interne à la lib (voix officielles) et point d'extension du tier Pro */
    protected open fun add(voice: Voice): Voice {
        voices[voice.id] = voice
        return voice
    }

    /** Voix par identifiant (FREE). */
    public fun get(id: String): Voice = voices[id]
        ?: throw VoiceError("voix inconnue : '$id'. Disponibles : ${voices.keys.sorted()}.")

    /** Toutes les voix (officielles + custom Pro), triées par id, sans distinction d'usage. */
    public fun list(): List<Voice> = voices.keys.sorted().map { voices.getValue(it) }

    public operator fun contains(id: String): Boolean = id in voices

    public val size: Int get() = voices.size
}
