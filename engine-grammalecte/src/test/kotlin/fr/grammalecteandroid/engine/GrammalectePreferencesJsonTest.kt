package fr.grammalecteandroid.engine

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test

class GrammalectePreferencesJsonTest {
    @Test
    fun roundTripPreservesNonSensitivePreferences() {
        val snapshot =
            GrammalectePreferencesSnapshot(
                dictionary =
                    GrammalecteDictionary.CLASSIC,
                ruleOverrides =
                    mapOf(
                        "apos" to false,
                        "infi" to true,
                    ),
            )

        val encoded =
            GrammalectePreferencesJson.encode(
                snapshot,
            )

        val decoded =
            GrammalectePreferencesJson.decode(
                encoded,
            )

        assertEquals(
            snapshot,
            decoded,
        )
    }

    @Test
    fun exportedSchemaContainsOnlyNonSensitivePreferenceFamilies() {
        val encoded =
            GrammalectePreferencesJson.encode(
                GrammalectePreferencesSnapshot(
                    dictionary =
                        GrammalecteDictionary.REFORM_1990,
                    ruleOverrides =
                        mapOf(
                            "apos" to false,
                        ),
                ),
            )

        val root =
            JSONObject(encoded)

        assertEquals(
            setOf(
                "format",
                "version",
                "dictionary",
                "ruleOverrides",
            ),
            root
                .keys()
                .asSequence()
                .toSet(),
        )

        assertFalse(
            root.has(
                "personalDictionary",
            ),
        )

        assertFalse(
            root.has(
                "personalWords",
            ),
        )
    }

    @Test
    fun decodeRejectsUnsupportedVersion() {
        assertInvalid {
            GrammalectePreferencesJson.decode(
                """
                {
                  "format": "grammalecte-android-preferences",
                  "version": 999,
                  "dictionary": "all_variants",
                  "ruleOverrides": {}
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun decodeRejectsUnknownDictionary() {
        assertInvalid {
            GrammalectePreferencesJson.decode(
                """
                {
                  "format": "grammalecte-android-preferences",
                  "version": 1,
                  "dictionary": "unknown",
                  "ruleOverrides": {}
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun decodeRejectsNonBooleanRuleOverride() {
        assertInvalid {
            GrammalectePreferencesJson.decode(
                """
                {
                  "format": "grammalecte-android-preferences",
                  "version": 1,
                  "dictionary": "classic",
                  "ruleOverrides": {
                    "apos": "false"
                  }
                }
                """.trimIndent(),
            )
        }
    }

    private fun assertInvalid(block: () -> Unit) {
        try {
            block()

            fail(
                "Expected invalid preferences JSON to be rejected",
            )
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }
}
