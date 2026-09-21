package com.jokobee.tts.core

/** Grapheme-to-phoneme (IPA) conversion. */
public interface G2p : java.io.Closeable {
    /**
     * Releases whatever this implementation holds. **No-op by default**, so a
     * dictionary or rule-based G2p needs no change.
     *
     * It exists because the neural implementation holds two ONNX sessions, and those
     * hold native memory a garbage collector does not reclaim promptly. Every wrapper
     * in the chain forwards, so closing the outermost releases the innermost.
     */
    override fun close() {}

    public fun phonemize(word: String, lang: String): String
}
