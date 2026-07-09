package com.jokobee.tts.core

/** An audio segment delivered during streaming synthesis. */
public class StreamChunk(
    public val audio: FloatArray,
    public val index: Int,
    public val text: String,
    public val isFinal: Boolean,
)

/** Streaming synthesis engine (sentence by sentence). Implemented by the Pro tier. */
public fun interface StreamingEngine {
    public fun stream(
        text: String,
        lang: String,
        config: StitchConfig,
        synthSegment: (segment: String, lang: String) -> FloatArray,
        onChunk: (StreamChunk) -> Boolean,
    )
}
