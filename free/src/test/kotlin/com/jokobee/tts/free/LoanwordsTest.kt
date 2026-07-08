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
        LoanwordsLexicon.fromStream(File(assets(), "jokobeetts/loanwords_en_ipa.tsv").inputStream())

    private fun vocabChars(): Set<Char> {
        val o = JSONObject(File(assets(), "kokoro/vocab.json").readText(Charsets.UTF_8))
        val s = HashSet<Char>()
        for (k in o.keys()) k.toString().forEach { s.add(it) }
        return s
    }

    // ----- audit qualité : 402 IPA -> PhonemePost -> 0 hors-vocab -----
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

    // ----- pipeline : anglicisme prononcé en anglais -----
    private fun expect(loan: LoanwordsLexicon, word: String, lang: String) =
        PhonemePost.apply(loan.lookup(word)!!, lang)

    @Test fun loanwordPronouncedInEnglish() {
        val loan = loanwords()
        val fe = Frontend(stub, loanwords = loan)
        assertEquals(expect(loan, "package", "fr"), fe.toPhonemes("package", "fr"))
        assertEquals(expect(loan, "cloud", "es"), fe.toPhonemes("cloud", "es"))
        assertEquals(expect(loan, "weekend", "it"), fe.toPhonemes("weekend", "it"))
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
        assertEquals("zzz", fe.toPhonemes("parking", "fr"))   // tts.lexicon prime
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
        assertEquals("zzz", fe.toPhonemes("cloud", "es"))     // tts.dictionary prime
    }

    // ----- non-interférence -----
    @Test fun loanwordsNotOnMisaki() {
        val fe = Frontend(stub, enG2p = { text, _ -> "MISAKI:$text" }, loanwords = loanwords())
        assertEquals("MISAKI:package", fe.toPhonemes("package", "en_US"))  // loanwords NON consulté
    }

    @Test fun loanwordsNoRegressionOnNativeWords() {
        val fe = Frontend(stub, loanwords = loanwords())
        assertEquals("X", fe.toPhonemes("bonjour", "fr"))     // mot natif absent -> CharsiuG2P (stub)
    }

    // ----- multi-mots -----
    @Test fun loanwordsMultiWord() {
        val loan = loanwords()
        val fe = Frontend(stub, loanwords = loan)
        assertEquals(expect(loan, "machine learning", "fr"), fe.toPhonemes("machine learning", "fr"))
    }
}
