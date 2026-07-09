package com.jokobee.tts.free

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Anti-GPL guard */
class NoGplImportsTest {

    @Test fun noGplTokensInShippableSources() {
        // user.dir = the :free module's directory during the Gradle run.
        val moduleDir = File(System.getProperty("user.dir"))
        val roots = listOf(
            File(moduleDir, "src/main"),                 // :free
            File(moduleDir.parentFile, "core/src/main"), // :core
        ).filter { it.isDirectory }

        assertTrue("Aucun dossier source shippable trouvé (user.dir=$moduleDir)", roots.isNotEmpty())

        val forbidden = listOf("espeak", "phonemizer", "espeakng")
        val fails = ArrayList<String>()
        var scanned = 0

        for (root in roots) {
            root.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { f ->
                scanned++
                val low = f.readText().lowercase()
                for (word in forbidden) {
                    if (word in low) fails.add("${f.name} contient '$word'")
                }
            }
        }

        assertTrue("Au moins un fichier .kt shippable attendu", scanned > 0)
        assertTrue(
            "$scanned fichiers scannés — CONTAMINATION GPL :\n" + fails.joinToString("\n"),
            fails.isEmpty(),
        )
    }
}
