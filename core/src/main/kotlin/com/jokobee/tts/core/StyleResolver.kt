package com.jokobee.tts.core

/** Contexte de synthèse fourni au résolveur de style. */
public class SynthesisContext<V>(
    public val text: String,
    public val lang: String,
    public val requestedStyle: V,
)

/** Sortie du résolveur de style. */
public class StyleOutput<V>(public val style: V)

/** Résout le style de synthèse à partir du contexte */
public fun interface StyleResolver<V> {
    public fun resolve(context: SynthesisContext<V>): StyleOutput<V>
}

/** Résolveur par défaut (renvoie le style demandé). */
public class DefaultStyleResolver<V> : StyleResolver<V> {
    override fun resolve(context: SynthesisContext<V>): StyleOutput<V> = StyleOutput(context.requestedStyle)
}
