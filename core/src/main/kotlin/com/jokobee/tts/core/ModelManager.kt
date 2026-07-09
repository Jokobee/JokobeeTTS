package com.jokobee.tts.core

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/** Download progress for a file (bytes received / total, -1 if unknown). */
public fun interface ProgressListener {
    public fun onProgress(name: String, bytesDone: Long, totalBytes: Long)
}

/** Opens a bundled asset by name, or `null` if it doesn't exist. */
public fun interface AssetProvider {
    public fun open(name: String): InputStream?
}

/** Authorizes (or not) the download of a resource. Pro stub (license) — wire up later. */
public fun interface Authorizer {
    public fun authorize(resource: String): Boolean
}

/** Streamed HTTP response, with optional resume support (Range). */
public class HttpResponse(
    public val stream: InputStream,
    /** Length of THIS response's body (-1 if unknown). */
    public val bodyLength: Long,
    /** Effective starting byte: `rangeStart` if the server honored Range (206), otherwise 0. */
    public val startsAtByte: Long,
    private val onClose: () -> Unit = {},
) : AutoCloseable {
    override fun close(): Unit = onClose()
}

/** Minimal HTTP client (injectable for tests). */
public fun interface HttpClient {
    /** Opens [url]; if [rangeStart] > 0, requests a resume (`Range: bytes=rangeStart-`). */
    public fun open(url: String, rangeStart: Long): HttpResponse
}

/** Failure resolving a model artifact (download/verification). */
public class ModelResolutionException(message: String) : JokobeeTtsException(message)

/** Resolves model artifacts (ONNX + voices) to local files: cache > assets > download. */
public class ModelManager(
    private val cacheDir: File,
    private val http: HttpClient = HttpUrlConnectionClient(),
    private val assets: AssetProvider = AssetProvider { null },
    private val authorizer: Authorizer = Authorizer { true },
) {
    /** Resolves all files in the manifest; returns name → local file. */
    public fun ensureAll(manifest: ModelManifest, progress: ProgressListener? = null): Map<String, File> {
        if (!authorizer.authorize(manifest.version)) throw ProRequiredException(
            "Model download not authorized (v${manifest.version}) — JokobeeTTS Pro required.",
        )
        return manifest.files.associate { it.name to resolve(it, progress) }
    }

    /** Resolves a file: valid cache, otherwise bundled asset, otherwise download. */
    public fun resolve(file: ModelFile, progress: ProgressListener? = null): File {
        cacheDir.mkdirs()
        val target = File(cacheDir, file.name)

        // 1) Cache: present and intact.
        if (target.exists() && verify(target, file.sha256)) return target

        // 2) Bundled asset: copy to cache if intact.
        assets.open(file.name)?.use { input ->
            input.copyTo(target.outputStream())
            if (verify(target, file.sha256)) return target
            target.delete()   // corrupted asset: fall back to download
        }

        // 3) Download (authorized), with resume.
        if (!authorizer.authorize(file.name)) throw ProRequiredException(
            "Model download not authorized ('${file.name}') — JokobeeTTS Pro required.",
        )
        download(file, target, progress)
        if (!verify(target, file.sha256)) {
            target.delete()
            throw ModelResolutionException("SHA-256 mismatch after downloading '${file.name}'.")
        }
        return target
    }

    // Downloads to <name>.part (resumed if present), then renames atomically.
    private fun download(file: ModelFile, target: File, progress: ProgressListener?) {
        val part = File(cacheDir, "${file.name}.part")
        var have = if (part.exists()) part.length() else 0L
        http.open(file.url, rangeStart = have).use { resp ->
            if (resp.startsAtByte == 0L && have > 0L) { part.delete(); have = 0L }  // server ignored Range
            val total = if (file.sizeBytes > 0) file.sizeBytes
            else if (resp.bodyLength >= 0) resp.startsAtByte + resp.bodyLength else -1L
            java.io.FileOutputStream(part, /* append = */ have > 0L).use { out ->
                val buf = ByteArray(64 * 1024)
                var done = have
                progress?.onProgress(file.name, done, total)
                while (true) {
                    val n = resp.stream.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    done += n
                    progress?.onProgress(file.name, done, total)
                }
            }
        }
        target.delete()
        if (!part.renameTo(target)) throw ModelResolutionException(
            "Unable to finalize '${file.name}' (renaming the .part file failed).",
        )
    }

    // Verifies the SHA-256; placeholder (empty/"TODO") = accepted (artifact not yet uploaded).
    private fun verify(f: File, sha256: String): Boolean {
        val expected = sha256.trim()
        if (expected.isEmpty() || expected.equals("TODO", ignoreCase = true)) return true
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        val hex = md.digest().joinToString("") { "%02x".format(it) }
        return hex.equals(expected, ignoreCase = true)
    }
}
