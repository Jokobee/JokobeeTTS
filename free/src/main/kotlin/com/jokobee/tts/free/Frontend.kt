package com.jokobee.tts.free

import com.jokobee.tts.core.G2p
import com.jokobee.tts.core.MapLexiconSource
import com.jokobee.tts.free.HomographAnnotator.Ann

public class Frontend(
    private val g2p: G2p,
    private val enG2p: ((String, String) -> String)? = null,
    private val verbalizer: Verbalizer = IcuVerbalizer(),
    /** Custom lexicon (layer #1), universal */
    public val lexicon: MapLexiconSource = MapLexiconSource(),
    /** Pro extension points (normalization, dictionaries, accent). */
    public val adapters: AdapterRegistry = AdapterRegistry(),
    /** Internal loanwords (CharsiuG2P path), automatic. */
    private val loanwords: LoanwordsLexicon = LoanwordsLexicon.EMPTY,
) {

    /** Releases the G2P chain. Wrapped by [Tts.close]; call it if you built a Frontend
     *  yourself. */
    public fun close(): Unit = g2p.close()

    private val pipeline = PhonemePipeline(
        AccentG2p(
            adapters.accent,
            LexiconG2p(lexicon, DictionaryG2p(adapters.dictionary, LoanwordsG2p(loanwords, g2p))),
        ),
    )

    /** Phonemizes a text. */
    public fun toPhonemes(text: String, lang: String): String {
        val pre = adapters.normalization.apply(text, lang, adapters.accent.current?.id)
        val normalized = Normalizers.forLang(lang, verbalizer).normalize(pre)
        if ((lang == "en_US" || lang == "en_GB") && enG2p != null) {
            return englishPhonemes(normalized, lang, enG2p)
        }
        return pipeline.phonemizeAnnotations(mergeMultiWord(annotate(normalized, lang), lang), lang)
    }

    /**
     * English, with the custom lexicon honoured.
     *
     * ## The defect this repairs
     *
     * The English branch returned `enG2p(text)` directly, which skips [pipeline] — and
     * [pipeline] is where [LexiconG2p] lives. So `lexicon.add(...)` worked in French,
     * Spanish, Italian and Portuguese, and **did nothing at all in English**, silently.
     * Measured on device: the same sentence with and without an entry produced
     * byte-identical audio (116 ko both times), while French moved 106 → 100 ko.
     *
     * Silent is the whole problem. `add()` returned normally, the audio played, and
     * nothing anywhere said the entry had been ignored.
     *
     * ## Why it splits rather than routing English through [pipeline]
     *
     * The English G2P is a different, better path for English — misaki's lexicon and its
     * own handling — and sending English through the generic chain would trade a real
     * quality gain for a hook most callers never use. So the text is cut **only around
     * the words the lexicon actually claims**, and every remaining run still goes to the
     * English G2P whole.
     *
     * **When nothing matches, this returns exactly what the old code returned** — the
     * same call, on the same string. An app that never touches the lexicon cannot hear a
     * difference, which is the property that makes the fix safe to ship.
     *
     * The cost, stated rather than hidden: a claimed word cuts the sentence, so the
     * English G2P sees two shorter runs instead of one. That is the price of overriding
     * a word inside a sentence, and it is paid only by callers who asked for it.
     */
    private fun englishPhonemes(
        text: String,
        lang: String,
        en: (String, String) -> String,
    ): String {
        val tokens = TOKEN_RE.findall(text)
        if (tokens.none { isWord(it) && lexicon.lookup(it, lang) != null }) {
            return en(text, lang)   // chemin inchange, octet pour octet
        }

        val out = StringBuilder()
        val run = StringBuilder()

        fun append(piece: String) {
            if (piece.isEmpty()) return
            if (out.isNotEmpty()) out.append(' ')
            out.append(piece)
        }

        fun flushRun() {
            val pending = run.toString().trim()
            run.setLength(0)
            if (pending.isNotEmpty()) append(en(pending, lang))
        }

        for (token in tokens) {
            val forced = if (isWord(token)) lexicon.lookup(token, lang) else null
            if (forced == null) {
                run.append(token).append(' ')
            } else {
                flushRun()
                append(forced)
            }
        }
        flushRun()
        return out.toString()
    }

    /** Word-by-word annotations */
    private fun annotate(text: String, lang: String): List<Ann> =
        if (lang == "fr" || lang == "fr_CA") HomographAnnotator.annotate(text)
        else TOKEN_RE.findall(text).map { Ann(it, null) }

    // Greedy merge of dictionary multi-word sequences: consecutive words (IPA not forced)
    // whose phrase is in tts.dictionary -> a single Ann with forced IPA (dict + accent). The
    // pipeline's final post-processing clamps the whole set.
    private fun mergeMultiWord(anns: List<Ann>, lang: String): List<Ann> {
        val dict = adapters.dictionary
        val out = ArrayList<Ann>(anns.size)
        var i = 0
        while (i < anns.size) {
            if (isWord(anns[i].token) && anns[i].ipa == null) {
                var j = i
                while (j + 1 < anns.size && isWord(anns[j + 1].token) && anns[j + 1].ipa == null) j++
                var end = j
                var hit: String? = null
                while (end > i) {
                    val phrase = (i..end).joinToString(" ") { anns[it].token }.lowercase()
                    hit = dict.lookup(phrase, lang) ?: loanwords.lookup(phrase, lang)   // dict > loanwords
                    if (hit != null) break
                    end--
                }
                if (hit != null) {
                    val ipa = adapters.accent.apply(hit, anns[i].token, lang)
                    out.add(Ann((i..end).joinToString(" ") { anns[it].token }, ipa))
                    i = end + 1
                    continue
                }
            }
            out.add(anns[i]); i++
        }
        return out
    }

    private fun isWord(token: String): Boolean = token.any { it.isLetter() }

    private companion object {
        // Word (letters + marks + apostrophe) OR sequence of non-space non-letters (punctuation).
        private val TOKEN_RE = Regex("""[\p{L}\p{M}']+|[^\s\p{L}\p{M}']+""")
        private fun Regex.findall(s: String): List<String> = findAll(s).map { it.value }.toList()
    }
}
