package com.jokobee.tts.core

/**
 * Contexte minimal de synthèse fourni au [StyleResolver]. v1.0 : strictement le nécessaire
 * (texte, langue, style/voix demandé par le développeur). Extensible plus tard via un
 * sous-type ou des propriétés optionnelles — **aucun champ spéculatif** (émotion, sentiment,
 * narration…) pour l'instant.
 *
 * `V` = type de style/voix (la `Voice` concrète vit dans `:free`, d'où le paramètre de type).
 */
public class SynthesisContext<V>(
    public val text: String,
    public val lang: String,
    public val requestedStyle: V,
)

/**
 * Sortie du [StyleResolver] : encapsule le style (vecteur/voix) à passer au moteur de synthèse.
 * v1.0 : simplement le style demandé. Plus tard : un style dynamique, ou un signal de délégation
 * à un autre moteur.
 */
public class StyleOutput<V>(public val style: V)

/**
 * Résout le **style de synthèse** à partir du contexte. Point d'insertion réservé pour un futur
 * moteur **neuronal contextuel** qui DÉCIDE le style/intonation en temps réel (lit le texte).
 * L'interface est la porte ; l'implémentation v1.0 ([DefaultStyleResolver]) est un pass-through.
 *
 * RÈGLE : le pipeline de synthèse ne résout JAMAIS le style directement — il passe TOUJOURS par
 * un `StyleResolver`, même quand c'est l'identité (même principe que le crochet [LexiconSource]).
 */
public fun interface StyleResolver<V> {
    public fun resolve(context: SynthesisContext<V>): StyleOutput<V>
}

/** v1.0 : pass-through pur — retourne exactement le style demandé, zéro logique, zéro overhead. */
public class DefaultStyleResolver<V> : StyleResolver<V> {
    override fun resolve(context: SynthesisContext<V>): StyleOutput<V> = StyleOutput(context.requestedStyle)
}
