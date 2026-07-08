package com.jokobee.tts.free

/** Désambiguïsation contextuelle française + lexique des mots-outils, en amont d'un G2P mot-à-mot */
public object HomographAnnotator {

    /** Un token annoté */
    public data class Ann(val token: String, val ipa: String?)

    /** Règle contextuelle */
    private class Rule(val test: (List<String>, Int) -> Boolean, val ipa: String)

    private class Entry(val default: String, val rules: List<Rule>)

    private fun prev(w: List<String>, i: Int, k: Int = 1): String =
        if (i - k >= 0) w[i - k].lowercase() else ""

    private fun next(w: List<String>, i: Int, k: Int = 1): String =
        if (i + k < w.size) w[i + k].lowercase() else ""

    private fun hasNeBefore(w: List<String>, i: Int, window: Int = 3): Boolean {
        val lo = maxOf(0, i - window)
        return (lo until i).any { w[it].lowercase() in NE_WORDS }
    }

    public fun annotate(text: String): List<Ann> {
        val words = tokenize(text)
        return words.mapIndexed { i, w ->
            val lw = w.lowercase()
            val entry = HOMOGRAPHS[lw]
            when {
                entry != null -> {
                    val ipa = entry.rules.firstOrNull { it.test(words, i) }?.ipa ?: entry.default
                    Ann(w, ipa)
                }
                lw in FUNCTION_WORDS -> Ann(w, FUNCTION_WORDS.getValue(lw))
                else -> Ann(w, null)
            }
        }
    }

    /** Tokenisation simple mots/ponctuation, apostrophe collée au clitique. */
    internal fun tokenize(text: String): List<String> {
        val nfc = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFC)
        val out = ArrayList<String>()
        for (tok in WORD_RE.findAll(nfc).map { it.value }) {
            val m = CLITIC_RE.matchEntire(tok)
            if (m != null) {
                out.add(m.groupValues[1]) // « l' »
                out.add(m.groupValues[2]) // « euro »
            } else {
                out.add(tok)
            }
        }
        return out
    }

    private val NE_WORDS = setOf("ne", "n'")

    private val DET = setOf(
        "le", "la", "les", "un", "une", "du", "des", "ce", "cette",
        "ces", "mon", "ton", "son", "au", "aux", "l'", "d'",
    )

    private val WORD_RE = Regex("[a-zA-ZÀ-ÿ']+|[^\\sa-zA-ZÀ-ÿ']+")
    private val CLITIC_RE = Regex("^([cdjlnst]'|qu')(.+)$", RegexOption.IGNORE_CASE)

    /** Mots-outils */
    private val FUNCTION_WORDS: Map<String, String> = mapOf(
        "le" to "lə", "la" to "la", "les" to "le", "l'" to "l",
        "de" to "də", "des" to "de", "du" to "dy", "d'" to "d",
        "un" to "œ̃", "une" to "yn", "au" to "o", "aux" to "o", "à" to "a",
        "et" to "e", "en" to "ɑ̃", "y" to "i",
        "mes" to "me", "tes" to "te", "ses" to "se", "ces" to "se",
        "mon" to "mɔ̃", "ton" to "tɔ̃", "son" to "sɔ̃",
        "ma" to "ma", "ta" to "ta", "sa" to "sa",
        "ne" to "nə", "n'" to "n", "se" to "sə", "s'" to "s", "ce" to "sə", "c'" to "s",
        "je" to "ʒə", "j'" to "ʒ", "tu" to "ty", "il" to "il", "elle" to "ɛl",
        "on" to "ɔ̃", "nous" to "nu", "vous" to "vu", "ils" to "il", "elles" to "ɛl",
        "que" to "kə", "qu'" to "k", "qui" to "ki", "quoi" to "kwa",
        "dans" to "dɑ̃", "sans" to "sɑ̃", "sous" to "su", "sur" to "syʁ",
        "pour" to "puʁ", "par" to "paʁ", "avec" to "avɛk",
        "mais" to "mɛ", "ou" to "u", "où" to "u", "si" to "si", "pas" to "pa",
        "très" to "tʁɛ", "chez" to "ʃe", "vers" to "vɛʁ",
    )

    private val HOMOGRAPHS: Map<String, Entry> = mapOf(
        "est" to Entry("ɛ", listOf(
            Rule({ w, i -> prev(w, i) in setOf("l'", "d'") }, "ɛst"),
            Rule({ w, i -> prev(w, i) == "vent" || prev(w, i, 2) == "vent" }, "ɛst"),
            Rule({ w, i -> prev(w, i) in setOf("nord", "sud", "grand") }, "ɛst"),
        )),
        "es" to Entry("ɛ", emptyList()),
        "plus" to Entry("plys", listOf(Rule(::hasNeBefore, "ply"))),
        "fils" to Entry("fis", emptyList()),
        "os" to Entry("ɔs", listOf(
            Rule({ w, i -> prev(w, i) in setOf("les", "des", "ses", "mes", "tes", "ces", "aux") }, "o"),
        )),
        "tous" to Entry("tus", listOf(Rule({ w, i -> next(w, i) in DET }, "tu"))),
        "couvent" to Entry("kuvɑ̃", listOf(
            Rule({ w, i ->
                prev(w, i) in setOf("elles", "ils") ||
                    (prev(w, i) !in DET && prev(w, i).endsWith("s"))
            }, "kuv"),
        )),
        "président" to Entry("pʁezidɑ̃", listOf(
            Rule({ w, i -> prev(w, i) in setOf("ils", "elles") }, "pʁezid"),
        )),
        "portions" to Entry("pɔʁsjɔ̃", listOf(Rule({ w, i -> prev(w, i) == "nous" }, "pɔʁtjɔ̃"))),
        "sens" to Entry("sɑ̃s", listOf(Rule({ w, i -> prev(w, i) in setOf("je", "tu") }, "sɑ̃"))),
    )
}
