package com.jokobee.tts.free

import com.jokobee.tts.core.G2p

/** G2P decorator: dictionaries (layer #2) before the delegate. */
internal class DictionaryG2p(
    private val registry: DictionaryRegistry,
    private val delegate: G2p,
) : G2p {
    override fun phonemize(word: String, lang: String): String =
        registry.lookup(word, lang) ?: delegate.phonemize(word, lang)
}

/** G2P decorator: internal loanwords before the delegate (CharsiuG2P path). */
internal class LoanwordsG2p(
    private val loanwords: LoanwordsLexicon,
    private val delegate: G2p,
) : G2p {
    override fun phonemize(word: String, lang: String): String =
        loanwords.lookup(word, lang) ?: delegate.phonemize(word, lang)
}

/** G2P decorator: accent (IPA to IPA) after the delegate. */
internal class AccentG2p(
    private val registry: AccentRegistry,
    private val delegate: G2p,
) : G2p {
    override fun phonemize(word: String, lang: String): String =
        registry.apply(delegate.phonemize(word, lang), word, lang)
}
