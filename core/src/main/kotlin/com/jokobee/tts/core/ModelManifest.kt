package com.jokobee.tts.core

import org.json.JSONObject

/** A model file to resolve (ONNX model, voice pack...). */
public data class ModelFile(
    public val name: String,
    public val url: String,
    /** Expected hex SHA-256; empty or "TODO" = verification skipped (placeholder before upload). */
    public val sha256: String = "",
    /** Expected size in bytes (0 = unknown). */
    public val sizeBytes: Long = 0,
)

/** Manifest of a version's artifacts (downloaded from Cloudflare). */
public data class ModelManifest(
    public val version: String,
    public val files: List<ModelFile>,
) {
    public fun file(name: String): ModelFile? = files.firstOrNull { it.name == name }

    public companion object {
        /** Parses a `manifest.json` (`org.json`, available at Android runtime). */
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
