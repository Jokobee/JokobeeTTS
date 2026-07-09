package com.jokobee.tts.free

import com.jokobee.tts.core.AdapterLoader
import com.jokobee.tts.core.DictAdapter
import com.jokobee.tts.core.G2p
import com.jokobee.tts.core.PhoneticAlphabet
import com.jokobee.tts.core.NormalizationAdapter
import com.jokobee.tts.core.PhonemeAdapter
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LoanwordsTest {

    private val stub = object : G2p { override fun phonemize(word: String, lang: String) = "X" }

    private fun assets() = File(System.getProperty("user.dir"), "src/main/assets")
    private fun loanwords() =
        LoanwordsLexicon.fromStreams(
            File(assets(), "jokobeetts/loanwords_en_ipa.tsv").inputStream(),
            File(assets(), "jokobeetts/loanwords_exclude.tsv").inputStream(),
        )

    private fun vocabChars(): Set<Char> {
        val o = JSONObject(File(assets(), "kokoro/vocab.json").readText(Charsets.UTF_8))
        val s = HashSet<Char>()
        for (k in o.keys()) k.toString().forEach { s.add(it) }
        return s
    }

    // ----- quality audit: 402 IPA -> PhonemePost -> 0 out-of-vocab -----
    @Test fun loanwordsAllInVocab() {
        val vocab = vocabChars()
        val loan = loanwords()
        assertEquals("402 entrées attendues", 402, loan.size)
        val oov = LinkedHashMap<String, String>()
        for ((word, ipa) in loan.all) {
            val bad = PhonemePost.apply(ipa, "en_US").filter { it !in vocab && it != ' ' }
            if (bad.isNotEmpty()) oov[word] = bad.map { "U+%04X".format(it.code) }.joinToString(" ")
        }
        assertTrue("phonèmes hors-vocab après clamp : $oov", oov.isEmpty())
    }

    // ----- pipeline: anglicism pronounced in English -----
    private fun expect(loan: LoanwordsLexicon, word: String, lang: String) =
        PhonemePost.apply(loan.lookup(word, lang)!!, lang)

    @Test fun loanwordPronouncedInEnglish() {
        val loan = loanwords()
        val fe = Frontend(stub, loanwords = loan)
        assertEquals(expect(loan, "package", "fr"), fe.toPhonemes("package", "fr"))
        assertEquals(expect(loan, "cloud", "es"), fe.toPhonemes("cloud", "es"))
        assertEquals(expect(loan, "streaming", "it"), fe.toPhonemes("streaming", "it"))
        assertEquals(expect(loan, "marketing", "pt_BR"), fe.toPhonemes("marketing", "pt_BR"))
    }

    @Test fun loanwordsOverridesCharsiu() {
        val fe = Frontend(stub, loanwords = loanwords())
        assertTrue("package via loanwords, pas le stub", fe.toPhonemes("package", "fr") != "X")
    }

    // ----- priorité : dev et Pro gagnent -----
    @Test fun lexiconOverridesLoanwords() {
        val fe = Frontend(stub, loanwords = loanwords())
        fe.lexicon.add("parking", "zzz", "fr")
        assertEquals("zzz", fe.toPhonemes("parking", "fr"))   // tts.lexicon takes priority
    }

    @Test fun dictOverridesLoanwords() {
        val reg = AdapterRegistry()
        reg.installLoader(object : AdapterLoader {
            override fun loadNormalization(id: String): NormalizationAdapter = throw NotImplementedError()
            override fun loadDictionary(id: String): DictAdapter = object : DictAdapter {
                override val id = "d"; override val langs = setOf("es")
                override val alphabet = PhoneticAlphabet.IPA
                override fun lookup(word: String, lang: String) = if (word == "cloud") "zzz" else null
            }
            override fun loadAccent(id: String): PhonemeAdapter = throw NotImplementedError()
        })
        reg.dictionary.load("d")
        val fe = Frontend(stub, adapters = reg, loanwords = loanwords())
        assertEquals("zzz", fe.toPhonemes("cloud", "es"))     // tts.dictionary takes priority
    }

    // ----- non-interference -----
    @Test fun loanwordsNotOnMisaki() {
        val fe = Frontend(stub, enG2p = { text, _ -> "MISAKI:$text" }, loanwords = loanwords())
        assertEquals("MISAKI:package", fe.toPhonemes("package", "en_US"))  // loanwords NOT consulted
    }

    @Test fun loanwordsNoRegressionOnNativeWords() {
        val fe = Frontend(stub, loanwords = loanwords())
        assertEquals("X", fe.toPhonemes("bonjour", "fr"))     // missing native word -> CharsiuG2P (stub)
    }

    // ----- multi-word -----
    @Test fun loanwordsMultiWord() {
        val loan = loanwords()
        val fe = Frontend(stub, loanwords = loan)
        assertEquals(expect(loan, "machine learning", "fr"), fe.toPhonemes("machine learning", "fr"))
    }

    // ----- per-language exclusion: native wins on collision -----
    @Test fun nativeWinsOnCollisionPerLanguage() {
        val loan = loanwords()
        assertEquals(null, loan.lookup("chat", "fr"))       // "chat" = feline in French -> excluded
        assertEquals(null, loan.lookup("chat", "fr_CA"))    // French variant
        assertTrue("chat gardé en es (anglicisme)", loan.lookup("chat", "es") != null)
        assertEquals(null, loan.lookup("pain", "fr"))       // French false friend
        assertEquals(null, loan.lookup("coin", "fr"))
        assertTrue("package : anglicisme partout", loan.lookup("package", "fr") != null)
        assertTrue(loan.lookup("cloud", "es") != null)
    }

    @Test fun excludedLoanwordFallsToNative() {
        val loan = loanwords()
        val fe = Frontend(stub, loanwords = loan)
        assertEquals("X", fe.toPhonemes("chat", "fr"))                    // excluded fr -> CharsiuG2P (stub)
        assertEquals(expect(loan, "chat", "es"), fe.toPhonemes("chat", "es"))  // kept es -> English
    }

    @Test fun devLexiconOverridesExclusion() {
        val fe = Frontend(stub, loanwords = loanwords())
        fe.lexicon.add("chat", "tʃæt", "fr")               // dev forces English
        assertEquals("tʃæt", fe.toPhonemes("chat", "fr"))  // tts.lexicon takes priority over exclusion
    }
}
