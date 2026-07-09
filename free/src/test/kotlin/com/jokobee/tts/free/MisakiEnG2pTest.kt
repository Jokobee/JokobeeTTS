package com.jokobee.tts.free

import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Port */
class MisakiEnG2pTest {

    private val g2p: MisakiEnG2p by lazy {
        val dir = File(System.getProperty("user.dir"), "src/main/assets/misaki")
        MisakiEnG2p(
            MisakiEnLexicon.fromStreams(
                File(dir, "us_gold.json").inputStream(),
                File(dir, "us_silver.json").inputStream(),
            ),
        )
    }

    @Test fun matchesSentenceGolden() {
        val json = javaClass.getResourceAsStream("/misaki_golden_en_sentences.json")!!.bufferedReader().readText()
        val golden = JSONObject(json)

        var sentTotal = 0; var sentMatch = 0
        var wordTotal = 0; var wordMatch = 0
        val diffs = ArrayList<String>()
        for (sent in golden.keys()) {
            val expected = golden.getString(sent)
            val got = g2p.phonemize(sent)
            sentTotal++
            if (got == expected) sentMatch++ else if (diffs.size < 20) diffs.add("«$sent»\n    exp: $expected\n    got: $got")
            // word-by-word (same space-based split)
            val ew = expected.split(" "); val gw = got.split(" ")
            if (ew.size == gw.size) for (k in ew.indices) { wordTotal++; if (ew[k] == gw[k]) wordMatch++ }
        }
        val sRate = 100.0 * sentMatch / sentTotal
        val wRate = if (wordTotal > 0) 100.0 * wordMatch / wordTotal else 0.0
        println("=== Phase 2 phrases : %d/%d exactes (%.0f%%) | mots %d/%d (%.1f%%) ===".format(
            sentMatch, sentTotal, sRate, wordMatch, wordTotal, wRate))
        diffs.forEach { println("  DIFF $it") }

        assertTrue("fidélité mot-à-mot trop basse : %.1f%%".format(wRate), wRate >= 99.0)
    }
}
