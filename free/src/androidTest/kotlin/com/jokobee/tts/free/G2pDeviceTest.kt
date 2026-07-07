package com.jokobee.tts.free

import ai.onnxruntime.OrtEnvironment
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Validation ON-DEVICE (arm64) du G2P CharsiuG2P tiny int8 embarqué.
 *
 * Mesure : (1) latence modèle PURE (sans cache) en ms/mot sur les mots uniques
 * normalisés des 62 cas fr ; (2) end-to-end 62 cas via [Frontend] + [CachingG2p].
 * Les chiffres sont journalisés sous le tag `JOKO_G2P` (lire via `adb logcat`).
 *
 * Cible : <50 ms/mot (sinon le cache LRU absorbe les répétitions). L'assertion reste
 * large — le but est la MESURE, pas un seuil dur qui casserait selon le device.
 */
@RunWith(AndroidJUnit4::class)
class G2pDeviceTest {

    private val tag = "JOKO_G2P"

    @Test fun g2pLatencyOnDevice() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val env = OrtEnvironment.getEnvironment()

        val cases = JSONArray(
            ctx.assets.open("test_cases_fr.json").bufferedReader().use { it.readText() },
        )

        // Mots uniques normalisés (ce que le G2P voit réellement, hors overrides).
        val norm = Normalizers.forLang("fr", IcuVerbalizer())
        val wordRe = Regex("""[\p{L}\p{M}']+""")
        val uniqueWords = LinkedHashSet<String>()
        for (i in 0 until cases.length()) {
            val src = cases.getJSONObject(i).getString("source")
            wordRe.findAll(norm.normalize(src)).forEach { uniqueWords.add(it.value) }
        }
        Log.i(tag, "mots uniques normalisés : ${uniqueWords.size}")

        // (1) COLD — latence modèle pure, sans cache
        val cold = CharsiuG2p.fromAssetsOrCache(ctx, env)
        cold.phonemize("bonjour", "fr")        // warmup graphe ORT
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

        // (2) END-TO-END — 62 cas via Frontend + cache LRU
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
