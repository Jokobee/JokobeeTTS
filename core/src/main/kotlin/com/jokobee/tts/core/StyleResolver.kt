package com.jokobee.tts.core

/**
 * Résout le **style de synthèse** à partir de la voix demandée par le développeur.
 * Point d'insertion réservé (couche obligatoire du pipeline) pour, plus tard, un moteur
 * contextuel qui lit le texte et choisit style/intonation en temps réel. L'interface est
 * la porte ; l'implémentation v1.0 ([DefaultStyleResolver]) est un pass-through pur.
 *
 * RÈGLE : le pipeline de synthèse ne résout JAMAIS le style directement — il passe
 * TOUJOURS par un `StyleResolver`, même quand c'est l'identité (même principe que le
 * crochet [LexiconSource] devant misaki).
 *
 * Générique sur le type de voix/style `V` (la `Voice` concrète vit dans `:free`).
 *
 * @param requested la voix/le vecteur de style choisi par le développeur.
 * @return le style à utiliser pour la synthèse (v1.0 : `requested`, inchangé).
 */
public fun interface StyleResolver<V> {
    public fun resolve(text: String, lang: String, requested: V): V
}

/** v1.0 : pass-through pur, zéro logique, zéro overhead — retourne la voix demandée. */
public class DefaultStyleResolver<V> : StyleResolver<V> {
    override fun resolve(text: String, lang: String, requested: V): V = requested
}
