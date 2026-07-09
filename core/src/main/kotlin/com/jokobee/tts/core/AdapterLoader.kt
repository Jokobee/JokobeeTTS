package com.jokobee.tts.core

/** Loads adapters from assets. Implemented by the Pro tier. */
public interface AdapterLoader {
    public fun loadNormalization(id: String): NormalizationAdapter
    public fun loadDictionary(id: String): DictAdapter
    public fun loadAccent(id: String): PhonemeAdapter
}
