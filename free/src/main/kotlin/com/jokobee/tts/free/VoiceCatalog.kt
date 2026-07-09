package com.jokobee.tts.free

import com.jokobee.tts.core.VoiceError

/** Catalog of available voices */
public open class VoiceCatalog {
    private val voices = LinkedHashMap<String, Voice>()

    /** Internal library population (official voices) and Pro tier extension point */
    protected open fun add(voice: Voice): Voice {
        voices[voice.id] = voice
        return voice
    }

    /** Voice by identifier (FREE). */
    public fun get(id: String): Voice = voices[id]
        ?: throw VoiceError("unknown voice: '$id'. Available: ${voices.keys.sorted()}.")

    /** All voices (official + custom Pro), sorted by id, with no usage distinction. */
    public fun list(): List<Voice> = voices.keys.sorted().map { voices.getValue(it) }

    public operator fun contains(id: String): Boolean = id in voices

    public val size: Int get() = voices.size
}
