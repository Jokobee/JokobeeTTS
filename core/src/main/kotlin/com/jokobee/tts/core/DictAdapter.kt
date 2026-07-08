package com.jokobee.tts.core

/** Dictionnaire de terminologie : mot vers IPA. */
public interface DictAdapter {
    public val id: String
    public val langs: Set<String>
    public val alphabet: PhoneticAlphabet
    public fun lookup(word: String, lang: String): String?
}
