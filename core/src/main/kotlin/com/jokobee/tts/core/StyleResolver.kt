package com.jokobee.tts.core

/** Synthesis context provided to the style resolver. */
public class SynthesisContext<V>(
    public val text: String,
    public val lang: String,
    public val requestedStyle: V,
)

/** Output of the style resolver. */
public class StyleOutput<V>(public val style: V)

/** Resolves the synthesis style from the context */
public fun interface StyleResolver<V> {
    public fun resolve(context: SynthesisContext<V>): StyleOutput<V>
}

/** Default resolver (returns the requested style). */
public class DefaultStyleResolver<V> : StyleResolver<V> {
    override fun resolve(context: SynthesisContext<V>): StyleOutput<V> = StyleOutput(context.requestedStyle)
}
