package com.jokobee.tts.core

/**
 * Découpe un texte NORMALISÉ en segments <= [maxChars], pour tenir dans le
 * contexte de 510 tokens utiles de Kokoro. Port de `text_splitter.py`.
 *
 * Cascade : phrase (. ! ? 。！？) → ponctuation faible (, ; : 、；：) → dernier
 * espace, jamais au milieu d'un mot. Vide/null → liste vide (fallback gracieux).
 *
 * Seuil en CARACTÈRES (proxy ; ratio tokens/char mesuré ≈ 1.01, défaut 400). Le
 * comptage RÉEL post-tokenisation viendra quand le G2P sera branché.
 */
public class TextSplitter(private val maxChars: Int = DEFAULT_MAX_CHARS) {

    public fun split(text: String?): List<String> {
        val t = text?.trim().orEmpty()
        if (t.isEmpty()) return emptyList()
        if (t.length <= maxChars) return listOf(t)
        val out = ArrayList<String>()
        for (sent in by(t, SENT_RE)) out += fit(sent)      // a) ponctuation forte
        return out
    }

    private fun fit(segIn: String): List<String> {
        val seg = segIn.trim()
        if (seg.isEmpty()) return emptyList()
        if (seg.length <= maxChars) return listOf(seg)
        val parts = by(seg, SOFT_RE)                        // b) ponctuation faible
        if (parts.size > 1) {
            val res = ArrayList<String>()
            for (p in parts) res += fit(p)
            return res
        }
        return bySpace(seg)                                 // c) dernier espace ; jamais mi-mot
    }

    private fun bySpace(segIn: String): List<String> {
        var seg = segIn
        val out = ArrayList<String>()
        while (seg.length > maxChars) {
            var cut = seg.lastIndexOf(' ', maxChars)        // dernier espace à index <= maxChars
            if (cut <= 0) cut = maxChars                    // mot unique > seuil : coupe dure (rare)
            out += seg.substring(0, cut).trim()
            seg = seg.substring(cut).trim()
        }
        if (seg.isNotEmpty()) out += seg
        return out
    }

    private fun by(text: String, re: Regex): List<String> =
        re.findAll(text).map { it.value.trim() }.filter { it.isNotEmpty() }.toList()

    public companion object {
        public const val DEFAULT_MAX_CHARS: Int = 400
        private val SENT_RE = Regex("[^.!?。！？]*[.!?。！？]+|[^.!?。！？]+$")
        private val SOFT_RE = Regex("[^,;:、；：]*[,;:、；：]+|[^,;:、；：]+$")
    }
}
