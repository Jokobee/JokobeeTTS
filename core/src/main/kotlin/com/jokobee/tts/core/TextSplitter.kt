package com.jokobee.tts.core

/** Splits normalized text into segments of at most [maxChars] characters. */
public class TextSplitter(private val maxChars: Int = DEFAULT_MAX_CHARS) {

    public fun split(text: String?): List<String> {
        val t = text?.trim().orEmpty()
        if (t.isEmpty()) return emptyList()
        if (t.length <= maxChars) return listOf(t)
        val out = ArrayList<String>()
        for (sent in by(t, SENT_RE)) out += fit(sent)      // a) strong punctuation
        return out
    }

    /** Always splits by sentence (strong punctuation), falling back to [maxChars] per sentence. */
    public fun splitSentences(text: String?): List<String> {
        val t = text?.trim().orEmpty()
        if (t.isEmpty()) return emptyList()
        val out = ArrayList<String>()
        for (sent in by(t, SENT_RE)) out += fit(sent)
        return out
    }

    private fun fit(segIn: String): List<String> {
        val seg = segIn.trim()
        if (seg.isEmpty()) return emptyList()
        if (seg.length <= maxChars) return listOf(seg)
        val parts = by(seg, SOFT_RE)                        // b) weak punctuation
        if (parts.size > 1) {
            val res = ArrayList<String>()
            for (p in parts) res += fit(p)
            return res
        }
        return bySpace(seg)                                 // c) last space; never mid-word
    }

    private fun bySpace(segIn: String): List<String> {
        var seg = segIn
        val out = ArrayList<String>()
        while (seg.length > maxChars) {
            var cut = seg.lastIndexOf(' ', maxChars)        // last space at index <= maxChars
            if (cut <= 0) cut = maxChars                    // single word > threshold: hard cut (rare)
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
