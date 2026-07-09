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
            return enG2p.invoke(normalized, lang)   // English G2P (us/gb depending on lang), no PhonemePost
        }
        return pipeline.phonemizeAnnotations(mergeMultiWord(annotate(normalized, lang), lang), lang)
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
