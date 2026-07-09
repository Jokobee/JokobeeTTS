package com.jokobee.tts.core

/** Grapheme-to-phoneme (IPA) conversion. */
public interface G2p {
    public fun phonemize(word: String, lang: String): String
}
