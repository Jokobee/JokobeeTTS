package com.jokobee.tts.core

/**
 * Résout la locale à utiliser pour un segment de texte.
 * - Free : [ExplicitLangRouter] — le développeur fournit la locale.
 * - Pro  : AutoLangRouter (module :pro) — détection automatique.
 */
public interface LangRouter {
    public fun resolve(segment: String): String
}

/** Free : retourne toujours la locale fournie par le développeur (pas de détection). */
public class ExplicitLangRouter(private val lang: String) : LangRouter {
    override fun resolve(segment: String): String = lang
}
