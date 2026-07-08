package com.jokobee.tts.free

import com.jokobee.tts.core.G2p

/** G2P décorateur : dictionnaires (couche #2) avant le délégué. */
internal class DictionaryG2p(
    private val registry: DictionaryRegistry,
    private val delegate: G2p,
) : G2p {
    override fun phonemize(word: String, lang: String): String =
        registry.lookup(word, lang) ?: delegate.phonemize(word, lang)
}

/** G2P décorateur : accent (IPA vers IPA) après le délégué. */
internal class AccentG2p(
    private val registry: AccentRegistry,
    private val delegate: G2p,
) : G2p {
    override fun phonemize(word: String, lang: String): String =
        registry.apply(delegate.phonemize(word, lang), word, lang)
}
