package com.jokobee.tts.core

/** Priority phoneme source (layer #1) of the G2P pipeline */
public interface LexiconSource {
    public fun lookup(word: String, lang: String): String?
}

/** Default empty source. */
public object EmptyLexiconSource : LexiconSource {
    override fun lookup(word: String, lang: String): String? = null
}

/** Mutable custom lexicon, indexed by `(lang, word)` (case-insensitive key), always storing canonical IPA */
public class MapLexiconSource() : LexiconSource {
    private val entries = HashMap<String, String>()
    private val sources = ArrayList<LexiconSource>()

    /** Convenience constructor */
    public constructor(entries: Map<String, String>, lang: String) : this() {
        for ((w, ipa) in entries) add(w, ipa, lang)
    }

    private fun key(lang: String, word: String) = "$lang ${word.lowercase()}"

    /** Adds an IPA pronunciation. */
    public fun add(word: String, ipa: String, lang: String): MapLexiconSource {
        entries[key(lang, word)] = ipa
        return this
    }

    /** Adds a pronunciation in a given alphabet (converted to IPA before storage). */
    public fun add(word: String, pronunciation: String, alphabet: PhoneticAlphabet, lang: String): MapLexiconSource =
        add(word, alphabet.toIpa(pronunciation), lang)

    /** Chains an additional source. */
    public fun load(source: LexiconSource): MapLexiconSource {
        sources.add(source)
        return this
    }

    override fun lookup(word: String, lang: String): String? {
        for (s in sources.asReversed()) s.lookup(word, lang)?.let { return it }   // last loaded wins
        return entries[key(lang, word)]
    }
}
