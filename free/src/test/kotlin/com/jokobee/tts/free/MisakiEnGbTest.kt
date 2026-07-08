package com.jokobee.tts.free

import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.text.Normalizer

/**
 * en_GB — le moteur misaki british était déjà porté (flag + branches suffixes) ; il ne
 * manquait que les DONNÉES (gb_gold/gb_silver, maintenant embarquées). Ce test valide la
 * fidélité GB (lexique + phrases) contre un golden misaki british, et l'absence d'OOV.
 */
class MisakiEnGbTest {

    private val dir = File(System.getProperty("user.dir"), "src/main/assets/misaki")
    private val lexicon by lazy {
        MisakiEnLexicon.fromStreams(
            File(dir, "gb_gold.json").inputStream(),
            File(dir, "gb_silver.json").inputStream(),
            british = true,
        )
    }
    private val g2p by lazy { MisakiEnG2p(lexicon, lang = "en_GB") }
    private val vocab: Set<Char> by lazy {
        val f = File(System.getProperty("user.dir"), "src/main/assets/kokoro/vocab.json")
        JSONObject(f.readText()).keys().asSequence().filter { it.length == 1 }.map { it[0] }.toSet()
    }
    private fun res(p: String) = javaClass.getResourceAsStream(p)!!.bufferedReader().readText()
    private fun fin(s: String) = s.replace('ɾ', 'T').replace('ʔ', 't')

    @Test fun lexiconMatchesGbGolden() {
        val golden = JSONObject(res("/misaki_golden_gb.json"))
        var total = 0; var match = 0
        for (w in golden.keys()) {
            total++
            if (fin(lexicon.phonemize(w).first ?: "") == golden.getString(w)) match++
        }
        val rate = 100.0 * match / total
        println("=== Fidélité lexique GB : $match/$total (%.1f%%) ===".format(rate))
        assertTrue("fidélité GB trop basse : %.1f%%".format(rate), rate >= 99.0)
    }

    @Test fun sentencesMatchGbGolden() {
        val golden = JSONObject(res("/misaki_golden_gb_sentences.json"))
        var wt = 0; var wm = 0
        for (s in golden.keys()) {
            val exp = golden.getString(s).split(" "); val got = g2p.phonemize(s).split(" ")
            if (exp.size == got.size) for (k in exp.indices) { wt++; if (exp[k] == got[k]) wm++ }
        }
        val rate = if (wt > 0) 100.0 * wm / wt else 0.0
        println("=== GB phrases mot-à-mot : $wm/$wt (%.1f%%) ===".format(rate))
        assertTrue("fidélité GB phrases trop basse : %.1f%%".format(rate), rate >= 97.0)
    }

    @Test fun gbNeverEmitsOutOfVocab() {
        val words = JSONObject(res("/misaki_golden_gb.json")).keys().asSequence().toList()
        val sents = JSONObject(res("/misaki_golden_gb_sentences.json")).keys().asSequence().toList()
        val offenders = LinkedHashMap<Char, String>()
        for (input in words + sents) {
            for (c in Normalizer.normalize(g2p.phonemize(input), Normalizer.Form.NFD)) {
                if (!c.isWhitespace() && c !in vocab && c != '❓') offenders.putIfAbsent(c, input)
            }
        }
        offenders.forEach { (c, ex) -> println("  OOV '$c' (U+%04X) ex: %s".format(c.code, ex)) }
        assertTrue("phonèmes GB hors-vocab Kokoro : ${offenders.keys}", offenders.isEmpty())
    }
}
