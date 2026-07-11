package com.jokobee.tts.free

import ai.onnxruntime.OrtEnvironment
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** END-TO-END ON-DEVICE PROOF (arm64) */
@RunWith(AndroidJUnit4::class)
class TtsDeviceTest {

    private val tag = "JOKO_TTS"

    @Test fun bonjourLeMondeToWavOnDevice() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val testCtx = InstrumentationRegistry.getInstrumentation().context
        val env = OrtEnvironment.getEnvironment()

        val modelFile = File(ctx.getExternalFilesDir("kokoro"), "model_quantized.onnx")
        assertTrue("modèle Kokoro absent (${modelFile.absolutePath}) — le pousser avant le run",
            modelFile.exists())

        // Full pipeline, assembled in Kotlin on the device.
        val g2p = CachingG2p(CharsiuG2p.fromAssetsOrCache(ctx, env))
        val frontend = Frontend(g2p)
        val tokenizer = KokoroTokenizer.fromAsset(ctx)
        val synth = KokoroSynth.fromModelFile(env, modelFile.absolutePath, tokenizer)
        val tts = Tts(frontend, synth)

        val voiceBytes = testCtx.assets.open("ff_siwis.bin").use { it.readBytes() }
        val voice = Voice.of("ff_siwis", "fr", voiceBytes)

        val t0 = System.nanoTime()
        val wav = tts.synthesizeToWav("Bonjour le monde", "fr", voice, leadMs = 1000, trailMs = 200)
        val ms = (System.nanoTime() - t0) / 1e6

        val out = File(ctx.getExternalFilesDir(null), "bonjour_kotlin_device.wav")
        out.writeBytes(wav)

        Log.i(tag, "WAV écrit : ${out.absolutePath} (${wav.size} octets, synth ${"%.0f".format(ms)} ms)")
        assertTrue("WAV trop court", wav.size > 44 + 24000)   // > en-tête + 0.5 s
        synth.close()
    }

    @Test fun welcomeEnglishToWavOnDevice() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val testCtx = InstrumentationRegistry.getInstrumentation().context
        val env = OrtEnvironment.getEnvironment()

        val modelFile = File(ctx.getExternalFilesDir("kokoro"), "model_quantized.onnx")
        assertTrue("modèle Kokoro absent — le pousser avant le run", modelFile.exists())

        val g2p = CachingG2p(CharsiuG2p.fromAssetsOrCache(ctx, env))
        val brands = com.jokobee.tts.core.MapLexiconSource(mapOf("jokobee" to "dʒoʊkoʊbi"), "en_US")
        val misakiUs = MisakiEnG2p.fromAssets(ctx, fallback = g2p, customLexicon = brands)
        val misakiGb = MisakiEnG2p.fromAssets(ctx, fallback = g2p, british = true)
        val frontend = Frontend(g2p, enG2p = { t, l -> if (l == "en_GB") misakiGb.phonemize(t) else misakiUs.phonemize(t) })
        val synth = KokoroSynth.fromModelFile(env, modelFile.absolutePath, KokoroTokenizer.fromAsset(ctx))
        val tts = Tts(frontend, synth)

        val voice = Voice.of("af_heart", "en_US", testCtx.assets.open("af_heart.bin").use { it.readBytes() })

        val wav = tts.synthesizeToWav("Welcome to Jokobee text to speech", "en_US", voice, leadMs = 1000, trailMs = 200)
        val out = File(ctx.getExternalFilesDir(null), "welcome_kotlin_device_en.wav")
        out.writeBytes(wav)

        Log.i(tag, "EN WAV écrit : ${out.absolutePath} (${wav.size} octets)")
        assertTrue("WAV EN trop court", wav.size > 44 + 24000)
        synth.close()
    }

    /** Zero-config path: Tts.create(context) — model + voices loaded from the bundled AAR assets, no file push. */
    @Test fun zeroConfigCreateSynthesizesOnDevice() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext

        val t0 = System.nanoTime()
        val tts = Tts.create(ctx)
        val initMs = (System.nanoTime() - t0) / 1e6

        assertTrue("bundled voice catalog missing", (tts.voices?.size ?: 0) >= 30)

        val wav = tts.synthesizeToWav("Bonjour le monde", "fr", leadMs = 1000, trailMs = 200)
        val out = File(ctx.getExternalFilesDir(null), "zero_config_fr.wav")
        out.writeBytes(wav)

        val wavEn = tts.synthesizeToWav("Hello world", "en")   // short-locale alias + default voice
        val outEn = File(ctx.getExternalFilesDir(null), "zero_config_en.wav")
        outEn.writeBytes(wavEn)

        Log.i(tag, "Zero-config: init ${"%.0f".format(initMs)} ms, fr ${wav.size}o, en ${wavEn.size}o")
        assertTrue("zero-config WAV (fr) too short", wav.size > 44 + 24000)
        assertTrue("zero-config WAV (en) too short", wavEn.size > 44 + 24000)
    }

    /** Marine (ff_marine) must be auto-discovered exactly like the 37 original
     * official voices — same VoiceCatalog.official(context) path, no special-casing. */
    @Test fun marineIsWiredLikeOfficialVoicesOnDevice() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val tts = Tts.create(ctx)

        assertTrue("38 voices expected (37 original + Marine), got ${tts.voices?.size}",
            (tts.voices?.size ?: 0) >= 38)
        assertTrue("ff_marine not in catalog", tts.voices?.contains("ff_marine") == true)

        val marine = tts.voices?.get("ff_marine")
        assertTrue("ff_marine wrong id", marine?.id == "ff_marine")

        // Same call path as any other official voice — no special-casing.
        val wav = tts.synthesizeToWav("Bonjour, je m'appelle Marine.", "fr", marine, leadMs = 500, trailMs = 200)
        val out = File(ctx.getExternalFilesDir(null), "marine_official_catalog_device.wav")
        out.writeBytes(wav)

        Log.i(tag, "Marine (official catalog path): ${out.absolutePath} (${wav.size} octets)")
        assertTrue("Marine WAV too short", wav.size > 44 + 24000)
    }
}
