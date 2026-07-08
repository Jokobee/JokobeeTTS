package com.jokobee.tts.core

/** Conversion graphème vers phonème (IPA). */
public interface G2p {
    public fun phonemize(word: String, lang: String): String
}
