package com.jokobee.tts.free

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Writes an f32 [-1,1] waveform as 16-bit PCM mono WAV (24 kHz by default, Kokoro's native output) */
public object WavWriter {

    private const val HEADER_SIZE = 44

    /** Waveform */
    public fun toWav(samples: FloatArray, sampleRate: Int = 24000): ByteArray {
        val dataLen = samples.size * 2
        val buf = ByteBuffer.allocate(HEADER_SIZE + dataLen).order(ByteOrder.LITTLE_ENDIAN)

        buf.put('R'.code.toByte()).put('I'.code.toByte()).put('F'.code.toByte()).put('F'.code.toByte())
        buf.putInt(36 + dataLen)                 // file size minus 8
        buf.put('W'.code.toByte()).put('A'.code.toByte()).put('V'.code.toByte()).put('E'.code.toByte())
        buf.put('f'.code.toByte()).put('m'.code.toByte()).put('t'.code.toByte()).put(' '.code.toByte())
        buf.putInt(16)                           // fmt sub-chunk size (PCM)
        buf.putShort(1)                          // format = PCM
        buf.putShort(1)                          // channels = mono
        buf.putInt(sampleRate)                   // sample rate
        buf.putInt(sampleRate * 2)               // byte rate = sr × channels × 2
        buf.putShort(2)                          // block align = channels × 2
        buf.putShort(16)                         // bits per sample
        buf.put('d'.code.toByte()).put('a'.code.toByte()).put('t'.code.toByte()).put('a'.code.toByte())
        buf.putInt(dataLen)

        for (s in samples) {
            val v = (s.coerceIn(-1f, 1f) * 32767f).toInt()
            buf.putShort(v.toShort())
        }
        return buf.array()
    }

    /** Writes the waveform to a WAV file. */
    public fun write(file: File, samples: FloatArray, sampleRate: Int = 24000): Unit =
        file.writeBytes(toWav(samples, sampleRate))
}
