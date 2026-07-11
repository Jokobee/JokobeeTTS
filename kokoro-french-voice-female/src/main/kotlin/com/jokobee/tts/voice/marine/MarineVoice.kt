package com.jokobee.tts.voice.marine

/**
 * Marine — free French female voice for Kokoro-82M.
 *
 * No Android [android.content.Context] required (classloader resource loading) —
 * works in any JVM or Android project.
 */
public object MarineVoice {
    private fun read(path: String): ByteArray =
        requireNotNull(MarineVoice::class.java.classLoader?.getResourceAsStream(path)) {
            "resource not found: $path"
        }.use { it.readBytes() }

    /**
     * Flat little-endian float32, no header, 510*256*4 bytes — the format
     * JokobeeTTS reads directly: `Voice.of("ff_marine", "fr", MarineVoice.bin)`.
     */
    public val bin: ByteArray by lazy { read("jokobee/voices/ff_marine.bin") }

    /**
     * Raw PyTorch tensor `(510, 256)` float32 — the format used directly by
     * Kokoro-82M / `kokoro-onnx`, no JokobeeTTS SDK required.
     */
    public val pt: ByteArray by lazy { read("jokobee/voices/ff_marine.pt") }
}
