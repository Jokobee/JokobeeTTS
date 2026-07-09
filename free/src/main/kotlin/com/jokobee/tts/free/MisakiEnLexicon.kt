package com.jokobee.tts.free

import android.content.Context
import org.json.JSONObject
import java.io.InputStream

/** Lexicon + morphology + stress stage for an English word */
public class MisakiEnLexicon private constructor(
    private val golds: Map<String, Entry>,
    private val silvers: Map<String, Entry>,
    private val british: Boolean,
) {
    /** Lexicon entry */
    public sealed interface Entry
    @JvmInline public value class Str(public val ps: String) : Entry
    public class Dict(public val m: Map<String, String?>) : Entry

    private val capStresses = doubleArrayOf(0.5, 2.0)

    private fun getNNP(word: String): Pair<String?, Int> {
        val sb = StringBuilder()
        for (c in word) {
            if (!c.isLetter()) continue
            val e = golds[c.uppercase()] as? Str ?: return null to 0
            sb.append(e.ps)
        }
        var ps = applyStress(sb.toString(), 0.0)!!
        val idx = ps.lastIndexOf(SECONDARY_STRESS)
        ps = if (idx >= 0) ps.substring(0, idx) + PRIMARY_STRESS + ps.substring(idx + 1) else ps
        return ps to 3
    }

    private fun parentTag(tag: String?): String? = when {
        tag == null -> null
        tag.startsWith("VB") -> "VERB"
        tag.startsWith("NN") -> "NOUN"
        tag.startsWith("ADV") || tag.startsWith("RB") -> "ADV"
        tag.startsWith("ADJ") || tag.startsWith("JJ") -> "ADJ"
        else -> tag
    }

    private fun isKnown(word: String, tag: String?): Boolean {
        if (word in golds || word in SYMBOLS || word in silvers) return true
        if (!word.all { it.isLetter() } || !word.all { it.code in LEXICON_ORDS }) return false
        if (word.length == 1) return true
        if (word == word.uppercase() && word.lowercase() in golds) return true
        return word.substring(1) == word.substring(1).uppercase()
    }

    /** Contextual function words (depend on the tag / following vowel). */
    private fun getSpecialCase(word: String, tag: String?, stress: Double?, fv: Boolean?): Pair<String?, Int> {
        if (tag == "ADD" && word in ADD_SYMBOLS) return lookup(ADD_SYMBOLS.getValue(word), null, -0.5, fv)
        if (word in SYMBOLS) return lookup(SYMBOLS.getValue(word), null, null, fv)
        if (word == "a" || word == "A") return (if (tag == "DT") "ɐ" else "ˈA") to 4
        if (word == "an" || word == "An" || word == "AN") {
            if (word == "AN" && tag != null && tag.startsWith("NN")) return getNNP(word)
            return "ɐn" to 4
        }
        if (word == "I" && tag == "PRP") return "${SECONDARY_STRESS}I" to 4
        if ((word == "by" || word == "By" || word == "BY") && parentTag(tag) == "ADV") return "bˈI" to 4
        if (word == "to" || word == "To" || (word == "TO" && (tag == "TO" || tag == "IN"))) {
            val ps = when (fv) { null -> (golds["to"] as? Str)?.ps; false -> "tə"; true -> "tʊ" }
            return ps to 4
        }
        if (word == "in" || word == "In" || (word == "IN" && tag != "NNP")) {
            val s = if (fv == null || tag != "IN") PRIMARY_STRESS.toString() else ""
            return (s + "ɪn") to 4
        }
        if (word == "the" || word == "The" || (word == "THE" && tag == "DT")) {
            return (if (fv == true) "ði" else "ðə") to 4
        }
        if (word == "used" || word == "Used" || word == "USED") {
            val d = golds["used"] as? Dict
            if (d != null) {
                val key = if ((tag == "VBD" || tag == "JJ")) "VBD" else "DEFAULT"
                return d.m[key] to 4
            }
        }
        return null to 0
    }

    private fun lookup(word: String, tag: String?, stress: Double?, fv: Boolean?): Pair<String?, Int> {
        var w = word
        var isNNP: Boolean? = null
        if (w == w.uppercase() && w !in golds) {
            w = w.lowercase()
            isNNP = tag == "NNP"
        }
        var entry: Entry? = golds[w]
        var rating = 4
        if (entry == null && isNNP != true) {
            entry = silvers[w]; rating = 3
        }
        var ps: String? = when (entry) {
            is Str -> entry.ps
            is Dict -> {
                var t = tag
                if (fv == null && "None" in entry.m) t = "None"
                else if (t == null || t !in entry.m) t = parentTag(tag)
                entry.m[t] ?: entry.m["DEFAULT"]
            }
            null -> null
        }
        if (ps == null || (isNNP == true && PRIMARY_STRESS !in ps)) {
            val (nnp, r) = getNNP(w)
            if (nnp != null) return nnp to r
        }
        return applyStress(ps, stress) to rating
    }

    // ----- morphological suffixes (phoneme rules) ------------------------
    private fun sfx(stem: String?): String? {
        if (stem.isNullOrEmpty()) return null
        val last = stem.last()
        return when {
            last in "ptkfθ" -> stem + "s"
            last in "szʃʒʧʤ" -> stem + (if (british) "ɪ" else "ᵻ") + "z"
            else -> stem + "z"
        }
    }

    private fun edSfx(stem: String?): String? {
        if (stem.isNullOrEmpty()) return null
        val last = stem.last()
        return when {
            last in "pkfθʃsʧ" -> stem + "t"
            last == 'd' -> stem + (if (british) "ɪ" else "ᵻ") + "d"
            last != 't' -> stem + "d"
            british || stem.length < 2 -> stem + "ɪd"
            stem[stem.length - 2] in US_TAUS -> stem.dropLast(1) + "ɾᵻd"
            else -> stem + "ᵻd"
        }
    }

    private fun ingSfx(stem: String?): String? {
        if (stem.isNullOrEmpty()) return null
        if (british) { if (stem.last() in "əː") return null }
        else if (stem.length > 1 && stem.last() == 't' && stem[stem.length - 2] in US_TAUS)
            return stem.dropLast(1) + "ɾɪŋ"
        return stem + "ɪŋ"
    }

    private fun stemS(word: String, tag: String?, stress: Double?, fv: Boolean?): Pair<String?, Int> {
        if (word.length < 3 || !word.endsWith("s")) return null to 0
        val stem = when {
            !word.endsWith("ss") && isKnown(word.dropLast(1), tag) -> word.dropLast(1)
            (word.endsWith("'s") || (word.length > 4 && word.endsWith("es") && !word.endsWith("ies"))) &&
                isKnown(word.dropLast(2), tag) -> word.dropLast(2)
            word.length > 4 && word.endsWith("ies") && isKnown(word.dropLast(3) + "y", tag) -> word.dropLast(3) + "y"
            else -> return null to 0
        }
        val (s, r) = lookup(stem, tag, stress, fv)
        return sfx(s) to r
    }

    private fun stemEd(word: String, tag: String?, stress: Double?, fv: Boolean?): Pair<String?, Int> {
        if (word.length < 4 || !word.endsWith("d")) return null to 0
        val stem = when {
            !word.endsWith("dd") && isKnown(word.dropLast(1), tag) -> word.dropLast(1)
            word.length > 4 && word.endsWith("ed") && !word.endsWith("eed") && isKnown(word.dropLast(2), tag) -> word.dropLast(2)
            else -> return null to 0
        }
        val (s, r) = lookup(stem, tag, stress, fv)
        return edSfx(s) to r
    }

    private fun stemIng(word: String, tag: String?, stress: Double?, fv: Boolean?): Pair<String?, Int> {
        if (word.length < 5 || !word.endsWith("ing")) return null to 0
        val stem = when {
            word.length > 5 && isKnown(word.dropLast(3), tag) -> word.dropLast(3)
            isKnown(word.dropLast(3) + "e", tag) -> word.dropLast(3) + "e"
            word.length > 5 && DOUBLE_ING.containsMatchIn(word) && isKnown(word.dropLast(4), tag) -> word.dropLast(4)
            else -> return null to 0
        }
        val (s, r) = lookup(stem, tag, stress, fv)
        return ingSfx(s) to r
    }

    private fun getWord(word0: String, tag: String?, stress: Double?, fv: Boolean?): Pair<String?, Int> {
        val (sc, scr) = getSpecialCase(word0, tag, stress, fv)
        if (sc != null) return sc to scr
        var word = word0
        val wl = word.lowercase()
        if (word.length > 1 && word.replace("'", "").all { it.isLetter() } && word != word.lowercase() &&
            (tag != "NNP" || word.length > 7) && word !in golds && word !in silvers &&
            (word == word.uppercase() || word.substring(1) == word.substring(1).lowercase()) &&
            (wl in golds || wl in silvers ||
                stemS(wl, tag, stress, fv).first != null ||
                stemEd(wl, tag, stress, fv).first != null ||
                stemIng(wl, tag, stress, fv).first != null)
        ) word = wl
        if (isKnown(word, tag)) return lookup(word, tag, stress, fv)
        if (word.endsWith("s'") && isKnown(word.dropLast(2) + "'s", tag)) return lookup(word.dropLast(2) + "'s", tag, stress, fv)
        if (word.endsWith("'") && isKnown(word.dropLast(1), tag)) return lookup(word.dropLast(1), tag, stress, fv)
        stemS(word, tag, stress, fv).let { if (it.first != null) return it }
        stemEd(word, tag, stress, fv).let { if (it.first != null) return it }
        stemIng(word, tag, if (stress == null) 0.5 else stress, fv).let { if (it.first != null) return it }
        return null to 0
    }

    /** Converts a word to phonemes. */
    public fun phonemize(word0: String, tag: String? = null, futureVowel: Boolean? = null): Pair<String?, Int> {
        var word = java.text.Normalizer.normalize(word0.replace('‘', '\'').replace('’', '\''), java.text.Normalizer.Form.NFKC)
        val stress = if (word == word.lowercase()) null else capStresses[if (word == word.uppercase()) 1 else 0]
        val (ps, rating) = getWord(word, tag, stress, futureVowel)
        if (ps != null) return applyStress(ps, null) to rating
        if (!word.all { it.code in LEXICON_ORDS }) return null to 0
        return null to 0
    }

    public companion object {
        public const val PRIMARY_STRESS: Char = 'ˈ'
        public const val SECONDARY_STRESS: Char = 'ˌ'
        internal const val STRESSES: String = "ˌˈ"
        internal const val VOWELS: String = "AIOQWYaiuæɑɒɔəɛɜɪʊʌᵻ"
        internal val LEXICON_ORDS: Set<Int> = buildSet { add(39); add(45); addAll(65..90); addAll(97..122) }
        internal const val US_TAUS: String = "AIOWYiuæɑəɛɪɹʊʌ"
        private val DOUBLE_ING = Regex("([bcdgklmnprstvxz])\\1ing$|cking$")
        private val ADD_SYMBOLS = mapOf("." to "dot", "/" to "slash")
        private val SYMBOLS = mapOf("%" to "percent", "&" to "and", "+" to "plus", "@" to "at")

        /** Adjusts/places the stress */
        public fun applyStress(ps0: String?, stress: Double?): String? {
            val ps = ps0 ?: return null
            fun hasVowel() = ps.any { it in VOWELS }
            fun noStress() = STRESSES.none { it in ps }
            return when {
                stress == null -> ps
                stress < -1 -> ps.filterNot { it == PRIMARY_STRESS || it == SECONDARY_STRESS }
                stress == -1.0 || (stress in listOf(0.0, -0.5) && PRIMARY_STRESS in ps) ->
                    ps.map { c ->
                        when (c) {
                            SECONDARY_STRESS -> ""
                            PRIMARY_STRESS -> SECONDARY_STRESS.toString()
                            else -> c.toString()
                        }
                    }.joinToString("")
                stress in listOf(0.0, 0.5, 1.0) && noStress() -> if (!hasVowel()) ps else restress(SECONDARY_STRESS + ps)
                stress >= 1 && PRIMARY_STRESS !in ps && SECONDARY_STRESS in ps -> ps.replace(SECONDARY_STRESS, PRIMARY_STRESS)
                stress > 1 && noStress() -> if (!hasVowel()) ps else restress(PRIMARY_STRESS + ps)
                else -> ps
            }
        }

        private fun restress(ps: String): String {
            val pos = DoubleArray(ps.length) { it.toDouble() }
            for (i in ps.indices) {
                if (ps[i] in STRESSES) {
                    var j = -1
                    for (k in i until ps.length) if (ps[k] in VOWELS) { j = k; break }
                    if (j >= 0) pos[i] = j - 0.5
                }
            }
            return ps.indices.sortedBy { pos[it] }.map { ps[it] }.joinToString("")
        }

        private fun growDictionary(d: Map<String, Entry>): Map<String, Entry> {
            val e = HashMap<String, Entry>()
            for ((k, v) in d) {
                if (k.length < 2) continue
                if (k == k.lowercase()) {
                    val cap = k.replaceFirstChar { it.uppercase() }
                    if (k != cap) e[cap] = v
                } else if (k == k.lowercase().replaceFirstChar { it.uppercase() }) {
                    e[k.lowercase()] = v
                }
            }
            return e + d   // d overrides e (like {**e, **d})
        }

        private fun parse(json: String): Map<String, Entry> {
            val o = JSONObject(json)
            val m = HashMap<String, Entry>(o.length() * 2)
            for (k in o.keys()) {
                when (val v = o.get(k)) {
                    is String -> m[k] = Str(v)
                    is JSONObject -> {
                        val inner = HashMap<String, String?>()
                        for (kk in v.keys()) inner[kk] = if (v.isNull(kk)) null else v.getString(kk)
                        m[k] = Dict(inner)
                    }
                }
            }
            return m
        }

        public fun fromJson(goldJson: String, silverJson: String, british: Boolean = false): MisakiEnLexicon =
            MisakiEnLexicon(growDictionary(parse(goldJson)), growDictionary(parse(silverJson)), british)

        public fun fromStreams(gold: InputStream, silver: InputStream, british: Boolean = false): MisakiEnLexicon =
            fromJson(gold.bufferedReader().use { it.readText() }, silver.bufferedReader().use { it.readText() }, british)

        public fun fromAssets(context: Context, british: Boolean = false): MisakiEnLexicon {
            val p = if (british) "gb" else "us"
            return fromStreams(context.assets.open("misaki/${p}_gold.json"), context.assets.open("misaki/${p}_silver.json"), british)
        }
    }
}
