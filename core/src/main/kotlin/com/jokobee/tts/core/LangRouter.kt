package com.jokobee.tts.core

/** Resolves the locale to use for a text segment */
public interface LangRouter {
    public fun resolve(segment: String): String
}

/** Router that always returns a fixed locale. */
public class ExplicitLangRouter(private val lang: String) : LangRouter {
    override fun resolve(segment: String): String = lang
}
