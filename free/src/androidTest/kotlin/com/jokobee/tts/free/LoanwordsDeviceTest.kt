package com.jokobee.tts.free

import ai.onnxruntime.OrtEnvironment
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** ON-DEVICE : loanwords (anglicismes) + phonémisation des langues romanes via CharsiuG2P réel. */
@RunWith(AndroidJUnit4::class)
class LoanwordsDeviceTest {

    private val tag = "JOKO_LOAN"

    private fun frontend(): Pair<Frontend, LoanwordsLexicon> {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val env = OrtEnvironment.getEnvironment()
        val g2p = CachingG2p(CharsiuG2p.fromAssetsOrCache(ctx, env))
        val loan = LoanwordsLexicon.fromAssets(ctx)
        return Frontend(g2p, loanwords = loan) to loan
    }

    @Test fun loanwordPronouncedEnglishOnDevice() {
        val (fe, loan) = frontend()
        // "package" en fr -> prononciation anglaise (loanwords), pas la française.
        val out = fe.toPhonemes("package", "fr")
        Log.i(tag, "package(fr) = $out")
        assertEquals(PhonemePost.apply(loan.lookup("package", "fr")!!, "fr"), out)
        assertTrue("phonème anglais æ attendu", out.contains("æ"))
    }

    @Test fun multiWordLoanwordOnDevice() {
        val (fe, loan) = frontend()
        assertEquals(PhonemePost.apply(loan.lookup("machine learning", "fr")!!, "fr"),
            fe.toPhonemes("machine learning", "fr"))
    }

    @Test fun perLanguageExclusionOnDevice() {
        val (fe, loan) = frontend()
        val chatFr = fe.toPhonemes("chat", "fr")   // exclu fr (félin) -> natif CharsiuG2P
        val chatEs = fe.toPhonemes("chat", "es")   // gardé es (anglicisme) -> anglais
        Log.i(tag, "chat fr=$chatFr es=$chatEs")
        assertTrue("chat(fr) doit être natif (pas d'æ anglais) : $chatFr", !chatFr.contains("æ"))
        assertEquals(PhonemePost.apply(loan.lookup("chat", "es")!!, "es"), chatEs)
    }

    @Test fun romanceLanguagesPhonemizeOnDevice() {
        val (fe, _) = frontend()
        for ((lang, word) in listOf("es" to "gato", "it" to "gatto", "pt_BR" to "gato", "fr" to "chat")) {
            val ph = fe.toPhonemes(word, lang)
            Log.i(tag, "$lang $word = $ph")
            assertTrue("$lang: phonémisation vide", ph.isNotBlank())
        }
    }
}
