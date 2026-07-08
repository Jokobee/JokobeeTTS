package com.jokobee.tts.free

import com.jokobee.tts.core.AdapterContext
import com.jokobee.tts.core.AdapterIncompatibleException
import com.jokobee.tts.core.AdapterLoader
import com.jokobee.tts.core.DictAdapter
import com.jokobee.tts.core.NormalizationAdapter
import com.jokobee.tts.core.NormalizationContext
import com.jokobee.tts.core.PhonemeAdapter
import com.jokobee.tts.core.ProRequiredException

/** Conteneur des points d'insertion Pro et de leur loader. */
public class AdapterRegistry {
    private var loader: AdapterLoader? = null

    internal fun requireLoader(): AdapterLoader =
        loader ?: throw ProRequiredException("This feature requires JokobeeTTS Pro — jokobee.com/pro")

    /** Installe le loader Pro (appelé par le tier Pro). */
    public fun installLoader(loader: AdapterLoader) { this.loader = loader }

    public val normalization: NormalizationRegistry = NormalizationRegistry(this)
    public val dictionary: DictionaryRegistry = DictionaryRegistry(this)
    public val accent: AccentRegistry = AccentRegistry(this)
}

private fun checkCompatible(langs: Set<String>, lang: String, id: String) {
    if (lang !in langs) throw AdapterIncompatibleException(
        "adapter '$id' incompatible avec '$lang' (attendu : ${langs.sorted()})",
    )
}

/** Point d'insertion normalisation (0 ou 1 actif). */
public class NormalizationRegistry internal constructor(private val host: AdapterRegistry) {
    public var current: NormalizationAdapter? = null
        private set

    public fun load(id: String) { current = host.requireLoader().loadNormalization(id) }
    public fun unload() { current = null }

    internal fun apply(text: String, lang: String, accentId: String?): String {
        val a = current ?: return text
        checkCompatible(a.langs, lang, a.id)
        return a.adapt(text, lang, NormalizationContext(lang, accentId))
    }
}

/** Point d'insertion dictionnaires (cumulable, ordre = priorité). */
public class DictionaryRegistry internal constructor(private val host: AdapterRegistry) {
    private val stack = ArrayList<DictAdapter>()

    public val loaded: List<String> get() = stack.map { it.id }

    public fun load(id: String) { stack.add(host.requireLoader().loadDictionary(id)) }
    public fun unload(id: String) { stack.removeAll { it.id == id } }
    public fun unloadAll() { stack.clear() }

    internal fun lookup(word: String, lang: String): String? {
        for (d in stack) {
            checkCompatible(d.langs, lang, d.id)
            d.lookup(word, lang)?.let { return it }
        }
        return null
    }
}

/** Point d'insertion accent (0 ou 1 actif). */
public class AccentRegistry internal constructor(private val host: AdapterRegistry) {
    public var current: PhonemeAdapter? = null
        private set

    public fun load(id: String) { current = host.requireLoader().loadAccent(id) }
    public fun unload() { current = null }

    internal fun apply(phonemes: String, word: String, lang: String): String {
        val a = current ?: return phonemes
        if (a.baseLang != lang && a.baseLang != lang.substringBefore('_')) {
            throw AdapterIncompatibleException(
                "accent '${a.id}' incompatible avec '$lang' (base attendue : ${a.baseLang})",
            )
        }
        return a.adapt(phonemes, AdapterContext(word, lang, null))
    }
}
