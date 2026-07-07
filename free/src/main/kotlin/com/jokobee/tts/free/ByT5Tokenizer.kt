package com.jokobee.tts.free

/**
 * Tokeniseur **ByT5** (byte-level) — CharsiuG2P est un modèle ByT5 seq2seq.
 *
 * ByT5 n'a PAS de vocabulaire appris : il opère directement sur les octets UTF-8.
 * Le vocabulaire (spec google/byt5) :
 *   - id 0 = `<pad>` (aussi `decoder_start_token_id`)
 *   - id 1 = `</s>`  (eos)
 *   - id 2 = `<unk>`
 *   - ids 3..258 = octets 0..255 (octet `b` → id `b + 3`)
 *   - ids 259..383 = 125 sentinelles `extra_id` (non utilisées ici)
 *
 * Déterministe et sans dépendance : IDENTIQUE JVM ↔ device. Aucun fichier de vocab
 * à embarquer (contrairement à un SentencePiece).
 */
public object ByT5Tokenizer {
    public const val PAD: Long = 0L
    public const val EOS: Long = 1L
    public const val UNK: Long = 2L
    private const val BYTE_OFFSET: Int = 3

    /**
     * Texte → ids d'entrée de l'encodeur : octets UTF-8 décalés de +3, puis `</s>`.
     * (T5 ajoute l'eos en fin d'entrée ; pas de bos.)
     */
    public fun encode(text: String): LongArray {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val ids = LongArray(bytes.size + 1)
        for (i in bytes.indices) ids[i] = (bytes[i].toInt() and 0xFF).toLong() + BYTE_OFFSET
        ids[bytes.size] = EOS
        return ids
    }

    /**
     * ids générés → texte. Les tokens spéciaux (pad/eos/unk) et les sentinelles sont
     * ignorés ; seuls les octets 3..258 sont reconstitués puis décodés en UTF-8.
     */
    public fun decode(ids: LongArray): String {
        val out = ArrayList<Byte>(ids.size)
        for (id in ids) {
            val b = id - BYTE_OFFSET
            if (b in 0..255) out.add(b.toByte())
        }
        return String(out.toByteArray(), Charsets.UTF_8)
    }
}
