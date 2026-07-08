package com.jokobee.tts.free

import com.jokobee.tts.core.G2p
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.text.Normalizer

/** AUDIT QUALITÉ anglais */
class MisakiEnAuditTest {

    private val misakiDir = File(System.getProperty("user.dir"), "src/main/assets/misaki")

    private val vocab: Set<Char> by lazy {
        val f = File(System.getProperty("user.dir"), "src/main/assets/kokoro/vocab.json")
        JSONObject(f.readText()).keys().asSequence().filter { it.length == 1 }.map { it[0] }.toSet()
    }

    private fun g2p(fallback: G2p? = null) = MisakiEnG2p(
        MisakiEnLexicon.fromStreams(
            File(misakiDir, "us_gold.json").inputStream(),
            File(misakiDir, "us_silver.json").inputStream(),
        ),
        fallback = fallback,
    )

    private fun oovChars(ipa: String): Set<Char> =
        Normalizer.normalize(ipa, Normalizer.Form.NFD).toSet()
            .filter { !it.isWhitespace() && it !in vocab && it != '❓' }.toSet()

    @Test fun misakiPathNeverEmitsOutOfVocab() {
        val g2p = g2p(fallback = null)
        val words = JSONObject(res("/misaki_golden_en.json")).keys().asSequence().toList()
        val sents = JSONObject(res("/misaki_golden_en_sentences.json")).keys().asSequence().toList()

        val offenders = LinkedHashMap<Char, String>()
        for (input in words + sents) {
            for (c in oovChars(g2p.phonemize(input))) offenders.putIfAbsent(c, input)
        }
        offenders.forEach { (c, ex) -> println("  OOV '$c' (U+%04X) ex: %s".format(c.code, ex)) }
        assertTrue("phonèmes hors-vocab Kokoro sur le chemin misaki : ${offenders.keys}", offenders.isEmpty())
    }

    @Test fun fallbackIsClampedToVocab() {
        val rawFallback = object : G2p {
            override fun phonemize(word: String, lang: String) = when (word.lowercase()) {
                "paul" -> "pˈɔɫ"          // ɫ hors-vocab
                "kubernetes" -> "kˈubɝnits" // ɝ hors-vocab
                else -> "tˈɛɡ"             // g ASCII hors-vocab
            }
        }
        val g2p = g2p(fallback = rawFallback)
        for (w in listOf("Paul", "Kubernetes", "zzqx")) {
            val oov = oovChars(g2p.phonemize(w))
            assertTrue("fallback non clampé pour '$w' : $oov", oov.isEmpty())
        }
    }

    private fun res(path: String) = javaClass.getResourceAsStream(path)!!.bufferedReader().readText()
}
