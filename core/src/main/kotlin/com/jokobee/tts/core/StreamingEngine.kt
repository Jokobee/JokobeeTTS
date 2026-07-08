package com.jokobee.tts.core

/** Un segment audio livré au fil de la synthèse streaming. */
public class StreamChunk(
    public val audio: FloatArray,
    public val index: Int,
    public val text: String,
    public val isFinal: Boolean,
)

/** Moteur de synthèse streaming (phrase par phrase). Implémenté par le tier Pro. */
public fun interface StreamingEngine {
    public fun stream(
        text: String,
        lang: String,
        config: StitchConfig,
        synthSegment: (segment: String, lang: String) -> FloatArray,
        onChunk: (StreamChunk) -> Boolean,
    )
}
