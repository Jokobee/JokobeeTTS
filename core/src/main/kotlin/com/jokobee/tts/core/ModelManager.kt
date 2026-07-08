package com.jokobee.tts.core

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/** Progression du téléchargement d'un fichier (octets reçus / total, -1 si inconnu). */
public fun interface ProgressListener {
    public fun onProgress(name: String, bytesDone: Long, totalBytes: Long)
}

/** Ouvre un asset embarqué par nom, ou `null` s'il n'existe pas. */
public fun interface AssetProvider {
    public fun open(name: String): InputStream?
}

/** Autorise (ou non) le téléchargement d'une ressource. Stub Pro (licence) — brancher plus tard. */
public fun interface Authorizer {
    public fun authorize(resource: String): Boolean
}

/** Réponse HTTP en flux, avec support éventuel de la reprise (Range). */
public class HttpResponse(
    public val stream: InputStream,
    /** Longueur du corps de CETTE réponse (-1 si inconnue). */
    public val bodyLength: Long,
    /** Octet de départ effectif : `rangeStart` si le serveur a honoré Range (206), sinon 0. */
    public val startsAtByte: Long,
    private val onClose: () -> Unit = {},
) : AutoCloseable {
    override fun close(): Unit = onClose()
}

/** Client HTTP minimal (injectable pour les tests). */
public fun interface HttpClient {
    /** Ouvre [url] ; si [rangeStart] > 0, demande une reprise (`Range: bytes=rangeStart-`). */
    public fun open(url: String, rangeStart: Long): HttpResponse
}

/** Échec de résolution d'un artefact de modèle (téléchargement/vérification). */
public class ModelResolutionException(message: String) : JokobeeTtsException(message)

/**
 * Résout les artefacts de modèle (modèle ONNX + voix) vers des fichiers locaux.
 *
 * Priorité : **cache** (déjà téléchargé et vérifié) > **assets** (embarqués) > **téléchargement**
 * (Cloudflare). Téléchargement repris (`.part` + Range), progression, vérification SHA-256.
 * Aucun upload : les artefacts sont poussés manuellement côté serveur.
 */
public class ModelManager(
    private val cacheDir: File,
    private val http: HttpClient = HttpUrlConnectionClient(),
    private val assets: AssetProvider = AssetProvider { null },
    private val authorizer: Authorizer = Authorizer { true },
) {
    /** Résout tous les fichiers du manifeste ; retourne name → fichier local. */
    public fun ensureAll(manifest: ModelManifest, progress: ProgressListener? = null): Map<String, File> {
        if (!authorizer.authorize(manifest.version)) throw ProRequiredException(
            "Model download not authorized (v${manifest.version}) — JokobeeTTS Pro required.",
        )
        return manifest.files.associate { it.name to resolve(it, progress) }
    }

    /** Résout un fichier : cache valide, sinon asset embarqué, sinon téléchargement. */
    public fun resolve(file: ModelFile, progress: ProgressListener? = null): File {
        cacheDir.mkdirs()
        val target = File(cacheDir, file.name)

        // 1) Cache : présent et intègre.
        if (target.exists() && verify(target, file.sha256)) return target

        // 2) Asset embarqué : copie vers le cache si intègre.
        assets.open(file.name)?.use { input ->
            input.copyTo(target.outputStream())
            if (verify(target, file.sha256)) return target
            target.delete()   // asset corrompu : on tente le téléchargement
        }

        // 3) Téléchargement (autorisé), avec reprise.
        if (!authorizer.authorize(file.name)) throw ProRequiredException(
            "Model download not authorized ('${file.name}') — JokobeeTTS Pro required.",
        )
        download(file, target, progress)
        if (!verify(target, file.sha256)) {
            target.delete()
            throw ModelResolutionException("SHA-256 mismatch après téléchargement de '${file.name}'.")
        }
        return target
    }

    // Télécharge vers <name>.part (repris si présent), puis renomme atomiquement.
    private fun download(file: ModelFile, target: File, progress: ProgressListener?) {
        val part = File(cacheDir, "${file.name}.part")
        var have = if (part.exists()) part.length() else 0L
        http.open(file.url, rangeStart = have).use { resp ->
            if (resp.startsAtByte == 0L && have > 0L) { part.delete(); have = 0L }  // serveur a ignoré Range
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
            "Impossible de finaliser '${file.name}' (renommage du .part échoué).",
        )
    }

    // Vérifie le SHA-256 ; placeholder (vide/"TODO") = accepté (artefact pas encore uploadé).
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
