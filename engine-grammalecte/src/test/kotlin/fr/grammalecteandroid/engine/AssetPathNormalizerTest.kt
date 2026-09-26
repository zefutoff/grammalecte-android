package fr.grammalecteandroid.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class AssetPathNormalizerTest {
    @Test
    fun `resource URI maps to bundled Grammalecte assets`() {
        assertEquals(
            "grammalecte/graphspell/_dictionaries/fr-allvars.json",
            AssetPathNormalizer.normalize(
                "resource://grammalecte/graphspell/_dictionaries/fr-allvars.json",
            ),
        )
    }

    @Test
    fun `regular asset path remains regular`() {
        assertEquals(
            "grammalecte/fr/gc_engine.js",
            AssetPathNormalizer.normalize("grammalecte/fr/gc_engine.js"),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parent traversal is rejected`() {
        AssetPathNormalizer.normalize("../secrets")
    }
}
