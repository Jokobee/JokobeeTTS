package com.jokobee.tts.free

/** Tokeniseur d'octets UTF-8. */
public object ByT5Tokenizer {
    public const val PAD: Long = 0L
    public const val EOS: Long = 1L
    public const val UNK: Long = 2L
    private const val BYTE_OFFSET: Int = 3

    /** Encode un texte en identifiants. */
    public fun encode(text: String, addEos: Boolean = false): LongArray {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val ids = LongArray(bytes.size + if (addEos) 1 else 0)
        for (i in bytes.indices) ids[i] = (bytes[i].toInt() and 0xFF).toLong() + BYTE_OFFSET
        if (addEos) ids[bytes.size] = EOS
        return ids
    }

    /** Décode des identifiants en texte. */
    public fun decode(ids: LongArray): String {
        val out = ArrayList<Byte>(ids.size)
        for (id in ids) {
            val b = id - BYTE_OFFSET
            if (b in 0..255) out.add(b.toByte())
        }
        return String(out.toByteArray(), Charsets.UTF_8)
    }
}
