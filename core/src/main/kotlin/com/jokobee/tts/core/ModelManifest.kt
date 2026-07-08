package com.jokobee.tts.core

import org.json.JSONObject

/** Un fichier de modèle à résoudre (modèle ONNX, pack de voix…). */
public data class ModelFile(
    public val name: String,
    public val url: String,
    /** SHA-256 hex attendu ; vide ou "TODO" = vérification ignorée (placeholder avant upload). */
    public val sha256: String = "",
    /** Taille attendue en octets (0 = inconnue). */
    public val sizeBytes: Long = 0,
)

/** Manifeste des artefacts d'une version (téléchargés depuis Cloudflare). */
public data class ModelManifest(
    public val version: String,
    public val files: List<ModelFile>,
) {
    public fun file(name: String): ModelFile? = files.firstOrNull { it.name == name }

    public companion object {
        /** Parse un `manifest.json` (`org.json`, disponible au runtime Android). */
        public fun fromJson(json: String): ModelManifest {
            val root = JSONObject(json)
            val arr = root.getJSONArray("files")
            val files = ArrayList<ModelFile>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                files += ModelFile(
                    name = o.getString("name"),
                    url = o.getString("url"),
                    sha256 = o.optString("sha256", ""),
                    sizeBytes = o.optLong("size", 0),
                )
            }
            return ModelManifest(root.getString("version"), files)
        }
    }
}
