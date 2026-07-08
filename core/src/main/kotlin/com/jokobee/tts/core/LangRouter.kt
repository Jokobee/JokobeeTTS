package com.jokobee.tts.core

/** Résout la locale à utiliser pour un segment de texte */
public interface LangRouter {
    public fun resolve(segment: String): String
}

/** Routeur renvoyant toujours une locale fixe. */
public class ExplicitLangRouter(private val lang: String) : LangRouter {
    override fun resolve(segment: String): String = lang
}
