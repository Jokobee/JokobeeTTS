package com.jokobee.tts.free

import org.json.JSONArray
import org.junit.Assert.fail

/** Charge un fichier de cas JSON (resources/cases/) et vérifie normalize == attendu. */
object CaseRunner {

    data class Case(val source: String, val expected: String, val category: String)

    fun load(fileName: String): List<Case> {
        val stream = CaseRunner::class.java.getResourceAsStream("/cases/$fileName")
            ?: error("resource introuvable : /cases/$fileName")
        val arr = JSONArray(stream.readBytes().toString(Charsets.UTF_8))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Case(o.getString("source"), o.getString("attendu"), o.optString("categorie"))
        }
    }

    fun run(fileName: String, normalizer: BaseNormalizer) {
        val fails = ArrayList<String>()
        var pass = 0
        for (c in load(fileName)) {
            val got = normalizer.normalize(c.source)
            if (got == c.expected) pass++
            else fails.add("[${c.category}] ${c.source}\n    attendu : ${c.expected}\n    obtenu  : $got")
        }
        if (fails.isNotEmpty()) {
            fail("$fileName : ${fails.size} échec(s) sur ${pass + fails.size} :\n" + fails.joinToString("\n"))
        }
    }
}
