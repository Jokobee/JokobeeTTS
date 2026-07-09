package com.jokobee.tts.free

import com.jokobee.tts.core.VoiceError
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Format of a voice embedding. */
public object VoiceFormat {
    /** Styles indexed by token length [0..509]. */
    public const val N_ROWS: Int = 510
    /** Dimension of a style vector. */
    public const val STYLE_DIM: Int = 256
    /** Expected size of the voice file */
    public const val EXPECTED_BYTES: Int = N_ROWS * STYLE_DIM * 4

    /** Locales routed by the pipeline. */
    public val SUPPORTED_LANGS: Set<String> = setOf(
        "fr", "fr_CA", "en_US", "en_GB", "es", "it", "pt_BR",
    )

    internal fun checkLang(lang: String): String {
        if (lang !in SUPPORTED_LANGS) throw VoiceError(
            "lang inconnue : '$lang'. Attendu l'une de ${SUPPORTED_LANGS.sorted()}. " +
                "La lang route le pipeline frontend ; une locale non gérée n'a pas de " +
                "chemin normaliseur/G2P/voix.",
        )
        return lang
    }

    /** Raw bytes (format above) */
    public fun parseEmbedding(raw: ByteArray): FloatArray {
        val n = raw.size
        if (n != EXPECTED_BYTES) {
            val unit = STYLE_DIM * 4
            val detail = if (n % unit == 0) " (≈ ${n / unit} lignes de $STYLE_DIM)" else ""
            throw VoiceError(
                "taille invalide : attendu $EXPECTED_BYTES octets ([$N_ROWS,$STYLE_DIM] " +
                    "float32 LE), reçu $n$detail. Fichier tronqué ou mauvais format ?",
            )
        }
        val fb = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        val arr = FloatArray(N_ROWS * STYLE_DIM)
        fb.get(arr)
        for (x in arr) if (!x.isFinite()) throw VoiceError(
            "valeurs non finies (NaN/Inf) détectées : mauvais dtype ou endianness ? " +
                "Attendu float32 little-endian.",
        )
        return arr
    }

    /** Validates an in-memory FloatArray and returns a copy of it. */
    public fun coerceEmbedding(embedding: FloatArray): FloatArray {
        if (embedding.size != N_ROWS * STYLE_DIM) throw VoiceError(
            "shape invalide : attendu ${N_ROWS * STYLE_DIM} floats ([$N_ROWS,$STYLE_DIM]), " +
                "reçu ${embedding.size}.",
        )
        for (x in embedding) if (!x.isFinite()) throw VoiceError(
            "valeurs non finies (NaN/Inf) dans l'embedding.",
        )
        return embedding.copyOf()
    }
}

/** A voice */
public class Voice private constructor(
    public val id: String,
    public val lang: String,
    private val embedding: FloatArray,
) {
    public companion object {
        /** From raw bytes in [VoiceFormat] format (voice .bin file). */
        public fun of(id: String, lang: String, raw: ByteArray): Voice =
            build(id, lang, VoiceFormat.parseEmbedding(raw))

        /** From a FloatArray already in memory. */
        public fun of(id: String, lang: String, embedding: FloatArray): Voice =
            build(id, lang, VoiceFormat.coerceEmbedding(embedding))

        private fun build(id: String, lang: String, emb: FloatArray): Voice {
            if (id.isEmpty()) throw VoiceError("id de voix requis (chaîne non vide).")
            return Voice(id, VoiceFormat.checkLang(lang), emb)
        }
    }

    /** Style vector for an utterance of `nTokens` tokens. */
    public fun styleFor(nTokens: Int): FloatArray {
        val idx = nTokens.coerceIn(0, VoiceFormat.N_ROWS - 1)
        val off = idx * VoiceFormat.STYLE_DIM
        return embedding.copyOfRange(off, off + VoiceFormat.STYLE_DIM)
    }

    /** Defensive copy of the embedding. */
    public fun copyEmbedding(): FloatArray = embedding.copyOf()

    /** Serializes to the voice binary format. */
    public fun toBytes(): ByteArray {
        val buf = ByteBuffer.allocate(VoiceFormat.EXPECTED_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        buf.asFloatBuffer().put(embedding)
        return buf.array()
    }

    public fun save(file: File): Unit = file.writeBytes(toBytes())

    override fun toString(): String =
        "Voice(id='$id', lang='$lang', embedding=[${VoiceFormat.N_ROWS},${VoiceFormat.STYLE_DIM}])"
}
