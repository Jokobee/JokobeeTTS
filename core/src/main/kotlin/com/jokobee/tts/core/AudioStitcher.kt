package com.jokobee.tts.core

import kotlin.math.abs

/** Configuration d'assemblage audio de segments. */
public data class StitchConfig(
    public val sampleRate: Int = 24000,
    public val silenceBetweenMs: Int = 300,
    public val crossfadeMs: Int = 20,
    public val peakNormalize: Boolean = true,
    public val peakTarget: Float = 0.95f,
)

/** Assemble des segments PCM en un flux : fondu anti-clic, silence inter-segment, normalisation de crête. */
public object AudioStitcher {

    public fun stitch(segments: List<FloatArray>, config: StitchConfig = StitchConfig()): FloatArray {
        if (segments.isEmpty()) return FloatArray(0)
        if (segments.size == 1) return segments[0]

        val fade = config.sampleRate * config.crossfadeMs / 1000
        val gap = config.sampleRate * config.silenceBetweenMs / 1000
        val faded = segments.map { fadeEdges(it, fade) }
        val total = faded.sumOf { it.size } + gap * (faded.size - 1)
        val out = FloatArray(total)
        var pos = 0
        for ((i, seg) in faded.withIndex()) {
            seg.copyInto(out, pos)
            pos += seg.size
            if (i < faded.size - 1) pos += gap   // silence (zéros) entre segments
        }
        return if (config.peakNormalize) normalizePeak(out, config.peakTarget) else out
    }

    // Fondu linéaire vers zéro sur les [fade] échantillons de bord (jointures propres).
    private fun fadeEdges(seg: FloatArray, fade: Int): FloatArray {
        if (fade <= 0 || seg.size < 2 * fade) return seg
        val out = seg.copyOf()
        for (i in 0 until fade) {
            val g = i.toFloat() / fade
            out[i] *= g
            out[out.size - 1 - i] *= g
        }
        return out
    }

    private fun normalizePeak(a: FloatArray, target: Float): FloatArray {
        var max = 0f
        for (v in a) { val av = abs(v); if (av > max) max = av }
        if (max <= 0f) return a
        val g = target / max
        for (i in a.indices) a[i] *= g
        return a
    }
}
