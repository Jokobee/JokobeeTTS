package com.jokobee.tts.free

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Écrit une forme d'onde f32 [-1,1] en **WAV PCM 16 bits mono** (24 kHz par défaut,
 * la sortie native de Kokoro). En-tête RIFF canonique de 44 octets + données little-endian.
 */
public object WavWriter {

    private const val HEADER_SIZE = 44

    /** Forme d'onde → octets WAV complets (en-tête + PCM 16 bits). */
    public fun toWav(samples: FloatArray, sampleRate: Int = 24000): ByteArray {
        val dataLen = samples.size * 2
        val buf = ByteBuffer.allocate(HEADER_SIZE + dataLen).order(ByteOrder.LITTLE_ENDIAN)

        buf.put('R'.code.toByte()).put('I'.code.toByte()).put('F'.code.toByte()).put('F'.code.toByte())
        buf.putInt(36 + dataLen)                 // taille du fichier − 8
        buf.put('W'.code.toByte()).put('A'.code.toByte()).put('V'.code.toByte()).put('E'.code.toByte())
        buf.put('f'.code.toByte()).put('m'.code.toByte()).put('t'.code.toByte()).put(' '.code.toByte())
        buf.putInt(16)                           // taille du sous-chunk fmt (PCM)
        buf.putShort(1)                          // format = PCM
        buf.putShort(1)                          // canaux = mono
        buf.putInt(sampleRate)                   // fréquence d'échantillonnage
        buf.putInt(sampleRate * 2)               // débit octets = sr × canaux × 2
        buf.putShort(2)                          // alignement bloc = canaux × 2
        buf.putShort(16)                         // bits par échantillon
        buf.put('d'.code.toByte()).put('a'.code.toByte()).put('t'.code.toByte()).put('a'.code.toByte())
        buf.putInt(dataLen)

        for (s in samples) {
            val v = (s.coerceIn(-1f, 1f) * 32767f).toInt()
            buf.putShort(v.toShort())
        }
        return buf.array()
    }

    /** Écrit la forme d'onde dans un fichier WAV. */
    public fun write(file: File, samples: FloatArray, sampleRate: Int = 24000): Unit =
        file.writeBytes(toWav(samples, sampleRate))
}
