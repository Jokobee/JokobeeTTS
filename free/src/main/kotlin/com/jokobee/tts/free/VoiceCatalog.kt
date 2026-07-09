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

    public companion object {
        /** Default official voice id per supported language, used when no [Voice] is passed explicitly. */
        internal val DEFAULT_VOICE_ID: Map<String, String> = mapOf(
            "en_US" to "af_heart", "en_GB" to "bf_emma", "es" to "ef_dora",
            "fr" to "ff_siwis", "fr_CA" to "ff_siwis", "it" to "if_sara", "pt_BR" to "pf_dora",
        )

        // Kokoro voice-id prefix -> JokobeeTTS locale (only the 6 languages/7 locales this
        // library supports; other official Kokoro prefixes, e.g. Japanese/Chinese/Hindi, are
        // not bundled since normalization/G2P for those languages isn't implemented here).
        private val PREFIX_TO_LANG: Map<String, String> = mapOf(
            "af" to "en_US", "am" to "en_US", "bf" to "en_GB", "bm" to "en_GB",
            "ef" to "es", "em" to "es", "ff" to "fr", "if" to "it", "im" to "it",
            "pf" to "pt_BR", "pm" to "pt_BR",
        )

        /** Loads every official voice bundled in the AAR assets (the `voices` folder) — no network. */
        public fun official(context: android.content.Context): VoiceCatalog {
            val catalog = VoiceCatalog()
            for (name in context.assets.list("voices").orEmpty()) {
                if (!name.endsWith(".bin")) continue
                val id = name.removeSuffix(".bin")
                val lang = PREFIX_TO_LANG[id.substringBefore('_')] ?: continue
                val bytes = context.assets.open("voices/$name").use { it.readBytes() }
                catalog.add(Voice.of(id, lang, bytes))
            }
            return catalog
        }
    }
}
