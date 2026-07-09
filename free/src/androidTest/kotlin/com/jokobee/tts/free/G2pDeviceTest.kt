package com.jokobee.tts.free

import ai.onnxruntime.OrtEnvironment
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** ON-DEVICE (arm64) validation of the G2P. */
@RunWith(AndroidJUnit4::class)
class G2pDeviceTest {

    private val tag = "JOKO_G2P"

    @Test fun g2pLatencyOnDevice() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val env = OrtEnvironment.getEnvironment()

        val cases = JSONArray(
            ctx.assets.open("test_cases_fr.json").bufferedReader().use { it.readText() },
        )

        // Unique normalized words (what the G2P actually sees, excluding overrides).
        val norm = Normalizers.forLang("fr", IcuVerbalizer())
        val wordRe = Regex("""[\p{L}\p{M}']+""")
        val uniqueWords = LinkedHashSet<String>()
        for (i in 0 until cases.length()) {
            val src = cases.getJSONObject(i).getString("source")
            wordRe.findAll(norm.normalize(src)).forEach { uniqueWords.add(it.value) }
        }
        Log.i(tag, "mots uniques normalisés : ${uniqueWords.size}")

        val cold = CharsiuG2p.fromAssetsOrCache(ctx, env)
        cold.phonemize("bonjour", "fr")        // warmup ORT graph
        var nanos = 0L
        var sample = ""
        for ((idx, w) in uniqueWords.withIndex()) {
            val t0 = System.nanoTime()
            val ipa = cold.phonemize(w, "fr")
            nanos += System.nanoTime() - t0
            if (idx < 3) sample += "$w→$ipa  "
        }
        val coldMsPerWord = nanos / 1e6 / uniqueWords.size
        Log.i(tag, "COLD (sans cache) : %.1f ms/mot | échantillon : %s".format(coldMsPerWord, sample))
        cold.close()

        val frontend = Frontend(CachingG2p(CharsiuG2p.fromAssetsOrCache(ctx, env)))
        frontend.toPhonemes("bonjour le monde", "fr")   // warmup
        val t0 = System.nanoTime()
        var first = ""
        for (i in 0 until cases.length()) {
            val ipa = frontend.toPhonemes(cases.getJSONObject(i).getString("source"), "fr")
            if (i == 0) first = ipa
        }
        val e2eMs = (System.nanoTime() - t0) / 1e6
        Log.i(tag, "END-TO-END 62 cas : %.0f ms total | %.1f ms/cas".format(e2eMs, e2eMs / cases.length()))
        Log.i(tag, "cas[0] IPA : $first")

        assertTrue("latence modèle anormale (>500 ms/mot)", coldMsPerWord < 500)
    }
}
