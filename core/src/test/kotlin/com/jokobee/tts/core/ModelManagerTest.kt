package com.jokobee.tts.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest
import kotlin.random.Random

class ModelManagerTest {

    @get:Rule val tmp = TemporaryFolder()

    private val payload = Random(7).nextBytes(200_000)   // > buffer 64 Ko : exerce la boucle
    private fun sha(b: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }

    private fun cacheDir() = tmp.newFolder("cache")
    private fun fileOf(name: String, sha: String = sha(payload), size: Long = 0) =
        ModelFile(name, "https://models.jokobee.com/$name", sha, size)

    // HTTP factice servant [payload], avec ou sans support Range.
    private fun serving(body: ByteArray = payload, honorRange: Boolean = true, calls: MutableList<Long>? = null) =
        HttpClient { _, rangeStart ->
            calls?.add(rangeStart)
            val start = if (honorRange && rangeStart > 0) rangeStart.toInt() else 0
            val slice = body.copyOfRange(start, body.size)
            HttpResponse(ByteArrayInputStream(slice), slice.size.toLong(), if (honorRange && rangeStart > 0) rangeStart else 0L)
        }
    private val throwingHttp = HttpClient { _, _ -> throw AssertionError("HTTP ne devrait pas être appelé") }

    @Test fun download_whenAbsent_verifiesReturnsAndReportsProgress() {
        val progress = ArrayList<Pair<Long, Long>>()
        val mgr = ModelManager(cacheDir(), http = serving())
        val out = mgr.resolve(fileOf("kokoro.onnx", size = payload.size.toLong())) { _, done, total -> progress.add(done to total) }
        assertArrayEquals(payload, out.readBytes())
        assertEquals(payload.size.toLong(), progress.last().first)   // done final == taille
        assertEquals(payload.size.toLong(), progress.last().second)  // total connu
        assertTrue("progression monotone", progress.map { it.first }.zipWithNext().all { it.first <= it.second })
    }

    @Test fun usesCache_whenValid_noDownload() {
        val cache = cacheDir()
        File(cache, "kokoro.onnx").writeBytes(payload)
        val out = ModelManager(cache, http = throwingHttp).resolve(fileOf("kokoro.onnx"))
        assertArrayEquals(payload, out.readBytes())
    }

    @Test fun usesAsset_whenNoCache_noDownload() {
        val assets = AssetProvider { name -> if (name == "voices.bin") ByteArrayInputStream(payload) else null }
        val out = ModelManager(cacheDir(), http = throwingHttp, assets = assets).resolve(fileOf("voices.bin"))
        assertArrayEquals(payload, out.readBytes())
    }

    @Test fun resumesPartialDownload_withRange() {
        val cache = cacheDir()
        val half = payload.size / 2
        File(cache, "kokoro.onnx.part").writeBytes(payload.copyOfRange(0, half))
        val calls = ArrayList<Long>()
        val out = ModelManager(cache, http = serving(calls = calls)).resolve(fileOf("kokoro.onnx"))
        assertArrayEquals(payload, out.readBytes())
        assertEquals(half.toLong(), calls.single())   // reprise demandée à l'octet 'half'
        assertFalse(File(cache, "kokoro.onnx.part").exists())
    }

    @Test fun serverIgnoresRange_restartsFromZero() {
        val cache = cacheDir()
        File(cache, "m.bin.part").writeBytes(payload.copyOfRange(0, payload.size / 2))
        val out = ModelManager(cache, http = serving(honorRange = false)).resolve(fileOf("m.bin"))
        assertArrayEquals(payload, out.readBytes())   // repart de 0, fichier complet et correct
    }

    @Test fun redownloads_whenCacheCorrupt() {
        val cache = cacheDir()
        File(cache, "kokoro.onnx").writeBytes(Random(1).nextBytes(500))   // mauvais contenu
        val out = ModelManager(cache, http = serving()).resolve(fileOf("kokoro.onnx"))
        assertArrayEquals(payload, out.readBytes())
    }

    @Test fun placeholderSha_skipsVerification() {
        val junk = Random(2).nextBytes(1000)
        val out = ModelManager(cacheDir(), http = serving(body = junk)).resolve(fileOf("x.bin", sha = "TODO"))
        assertArrayEquals(junk, out.readBytes())   // accepté sans vérif (artefact pas encore uploadé)
    }

    @Test(expected = ProRequiredException::class)
    fun authorizeFalse_blocksDownload() {
        ModelManager(cacheDir(), http = serving(), authorizer = { false }).resolve(fileOf("kokoro.onnx"))
    }

    @Test fun shaMismatch_afterDownload_throwsAndCleansUp() {
        val cache = cacheDir()
        val mgr = ModelManager(cache, http = serving())
        val t = try { mgr.resolve(fileOf("kokoro.onnx", sha = sha(Random(9).nextBytes(10)))); null }
        catch (e: ModelResolutionException) { e }
        assertTrue("doit lever ModelResolutionException", t != null)
        assertFalse("aucun fichier corrompu laissé", File(cache, "kokoro.onnx").exists())
    }

    @Test fun ensureAll_resolvesEveryFile() {
        val manifest = ModelManifest(
            "1.0",
            listOf(fileOf("kokoro.onnx", size = payload.size.toLong()), fileOf("voices.bin")),
        )
        val map = ModelManager(cacheDir(), http = serving()).ensureAll(manifest)
        assertEquals(setOf("kokoro.onnx", "voices.bin"), map.keys)
        map.values.forEach { assertArrayEquals(payload, it.readBytes()) }
    }

    @Test(expected = ProRequiredException::class)
    fun ensureAll_authorizeFalse_throwsBeforeAnyDownload() {
        val manifest = ModelManifest("1.0", listOf(fileOf("kokoro.onnx")))
        ModelManager(cacheDir(), http = throwingHttp, authorizer = { false }).ensureAll(manifest)
    }

    @Test fun manifest_fromJson_parsesFieldsAndDefaults() {
        val json = """
            {"version":"1.0","files":[
              {"name":"kokoro.onnx","url":"https://x/k.onnx","sha256":"abc","size":123},
              {"name":"voices.bin","url":"https://x/v.bin"}
            ]}
        """.trimIndent()
        val m = ModelManifest.fromJson(json)
        assertEquals("1.0", m.version)
        assertEquals(2, m.files.size)
        assertEquals("abc", m.file("kokoro.onnx")!!.sha256)
        assertEquals(123L, m.file("kokoro.onnx")!!.sizeBytes)
        assertEquals("", m.file("voices.bin")!!.sha256)   // défaut : placeholder
        assertEquals(0L, m.file("voices.bin")!!.sizeBytes)
    }
}
