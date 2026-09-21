package com.jokobee.tts.free

/**
 * French words whose spelling does not predict their pronunciation.
 *
 * ## Why a list at all
 *
 * The G2P is statistical: it generalises from spelling, which is exactly right for the
 * regular language and exactly wrong for the exceptions. "monsieur" is read as it is
 * written because nothing in the letters says `/məsjø/`, and no amount of training data
 * fixes a word that contradicts its own spelling.
 *
 * This is the **free tier's** dictionary: the exceptions a listener notices in the first
 * minute. It is deliberately not the Pro dictionary, which corrects the loanword lexicon
 * entry by entry and carries the encrypted adapters.
 *
 * ## How to add to it
 *
 * Entries are grouped **by rule**, not alphabetically, and every group says what the
 * rule is. A group is worth more than a word: it tells the next person which other words
 * belong there, and it makes a wrong entry visible — an entry that does not fit its
 * group's rule is probably wrong.
 *
 * Each word is registered lowercase **and** capitalised: lookup is case-sensitive and
 * sentence-initial capitals are the common case.
 *
 * ⚠️ **Context-dependent words are deliberately absent.** "plus" is `/ply/` or `/plys/`
 * depending on meaning, "tous" is `/tu/` or `/tus/` depending on whether it is an
 * adjective or a pronoun, and "fils" is `/fis/` (son) or `/fil/` (threads). A single
 * entry would fix half the cases and break the other half, which is worse than the
 * status quo — the error would become systematic instead of occasional.
 */
internal object FrenchExceptions {

    /** A rule, and the words that follow it. */
    private class Group(val rule: String, val words: List<Pair<String, String>>)

    private val GROUPS = listOf(
        Group(
            "Adverbs in -emment are /amɑ̃/, not /emɑ̃/. The single most audible French " +
                "exception, and a regular one: the whole family follows it.",
            listOf(
                "évidemment" to "evidamɑ̃",
                "notamment" to "nɔtamɑ̃",
                "récemment" to "ʁesamɑ̃",
                "fréquemment" to "fʁekamɑ̃",
                "apparemment" to "apaʁamɑ̃",
                "précédemment" to "pʁesedamɑ̃",
                "prudemment" to "pʁydamɑ̃",
                "différemment" to "difeʁamɑ̃",
                "violemment" to "vjɔlamɑ̃",
                "patiemment" to "pasjamɑ̃",
                "intelligemment" to "ɛ̃teliʒamɑ̃",
            ),
        ),
        Group(
            "Silent letters the spelling keeps and the mouth drops.",
            listOf(
                "automne" to "otɔn",          // m
                "condamner" to "kɔ̃dane",      // m
                "condamnation" to "kɔ̃danasjɔ̃",
                "damner" to "dane",
                "baptême" to "batɛm",         // p
                "baptiser" to "batize",
                "sculpteur" to "skyltœʁ",     // p
                "sculpture" to "skyltyʁ",
                "compter" to "kɔ̃te",          // p
                "sept" to "sɛt",              // the p is not said, the t is
                "gars" to "ɡɑ",               // r and s
            ),
        ),
        Group(
            "-ill- is /il/ here, not the /ij/ glide it usually marks.",
            listOf(
                "ville" to "vil",
                "village" to "vilaʒ",
                "mille" to "mil",
                "million" to "miljɔ̃",
                "milliard" to "miljaʁ",
                "tranquille" to "tʁɑ̃kil",
                "tranquillement" to "tʁɑ̃kilmɑ̃",
                "illusion" to "ilyzjɔ̃",
            ),
        ),
        Group(
            "Words that simply contradict their spelling. No rule covers them — that is " +
                "what makes them exceptions rather than a pattern.",
            listOf(
                "monsieur" to "məsjø",
                "messieurs" to "mesjø",
                "femme" to "fam",
                "oignon" to "ɔɲɔ̃",
                "second" to "səɡɔ̃",           // c is /ɡ/
                "seconde" to "səɡɔ̃d",
                "faisons" to "fəzɔ̃",          // unstressed ai- is /ə/
                "faisait" to "fəzɛ",
                "faisaient" to "fəzɛ",
                "août" to "ut",
                "zinc" to "zɛ̃ɡ",
            ),
        ),
        Group(
            "Loanwords kept in French spelling but said the source way — or halfway. " +
                "These are the ones a statistical G2P mangles most visibly, because it " +
                "applies French letter rules to English letters.",
            listOf(
                "week-end" to "wikɛnd",
                "weekend" to "wikɛnd",
                "parking" to "paʁkiŋ",
                "shopping" to "ʃɔpiŋ",
                "sandwich" to "sɑ̃dwitʃ",
                "football" to "futbol",
                "yacht" to "jɔt",
                "clown" to "klun",
                "jazz" to "dʒaz",
                "podcast" to "pɔdkast",
                "wifi" to "wifi",
                "bluetooth" to "blutus",      // said the French way, not /θ/
                "email" to "imɛl",
                "budget" to "bydʒɛ",
                "puzzle" to "pœzəl",
            ),
        ),
    )

    /**
     * Registers every exception for `fr` and `fr_CA`.
     *
     * Both locales get the same entries: none of these differ between France and Quebec
     * in a way this list captures. Where they do differ — vowel length, diphthongs — the
     * difference belongs in an accent adapter, not in a word list.
     */
    fun installInto(frontend: Frontend) {
        for (group in GROUPS) {
            for ((word, ipa) in group.words) {
                val capitalised = word.replaceFirstChar { it.uppercase() }
                for (lang in listOf("fr", "fr_CA")) {
                    frontend.lexicon.add(word, ipa, lang)
                    frontend.lexicon.add(capitalised, ipa, lang)
                }
            }
        }
    }

    /** How many words this tier corrects. Used by the tests and worth quoting. */
    val size: Int get() = GROUPS.sumOf { it.words.size }
}
