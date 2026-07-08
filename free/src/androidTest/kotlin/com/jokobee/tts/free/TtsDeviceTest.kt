package com.jokobee.tts.free

import ai.onnxruntime.OrtEnvironment
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * PREUVE BOUT-EN-BOUT ON-DEVICE (arm64) : texte → WAV, tout en Kotlin sur le téléphone.
 *   texte → normalisation → G2P (CharsiuG2P tiny embarqué) → tokens Kokoro →
 *   Kokoro ONNX (model_quantized) → forme d'onde → WAV.
 *
 * Prérequis (poussés avant le run) : le modèle Kokoro dans
 *   getExternalFilesDir("kokoro")/model_quantized.onnx  (~88 Mo, non embarqué).
 * La voix `ff_siwis.bin` est un asset de test ; le G2P tiny et le vocab Kokoro sont
 * embarqués dans `:free`. WAV écrit dans getExternalFilesDir(null) (à `adb pull`).
 * Silence de tête 1 s (convention fichiers de test).
 */
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

        // Chaîne complète, assemblée en Kotlin sur le device.
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

        // Pipeline ANGLAIS : misaki (lexique embarqué) + fallback CharsiuG2P + lexique marque.
        val g2p = CachingG2p(CharsiuG2p.fromAssetsOrCache(ctx, env))
        val misakiEn = MisakiEnG2p.fromAssets(ctx, fallback = g2p, brandLexicon = mapOf("jokobee" to "dʒoʊkoʊbi"))
        val frontend = Frontend(g2p, enG2p = { misakiEn.phonemize(it) })
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
}
