package com.jokobee.tts.core

/** Charge les adapters depuis les assets. Implémenté par le tier Pro. */
public interface AdapterLoader {
    public fun loadNormalization(id: String): NormalizationAdapter
    public fun loadDictionary(id: String): DictAdapter
    public fun loadAccent(id: String): PhonemeAdapter
}
