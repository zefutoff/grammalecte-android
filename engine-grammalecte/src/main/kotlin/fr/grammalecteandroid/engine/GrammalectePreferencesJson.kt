package fr.grammalecteandroid.engine

import org.json.JSONException
import org.json.JSONObject

internal data class GrammalectePreferencesSnapshot(
    val dictionary: GrammalecteDictionary,
    val ruleOverrides: Map<String, Boolean>,
)

internal object GrammalectePreferencesJson {
    const val FORMAT_NAME =
        "grammalecte-android-preferences"

    const val FORMAT_VERSION =
        1

    private const val MAX_JSON_LENGTH =
        64 * 1024

    private const val MAX_RULE_ID_LENGTH =
        128

    fun encode(snapshot: GrammalectePreferencesSnapshot): String {
        val rules =
            JSONObject()

        snapshot.ruleOverrides
            .toSortedMap()
            .forEach { (id, enabled) ->
                validateRuleId(id)
                rules.put(id, enabled)
            }

        return JSONObject()
            .put(
                "format",
                FORMAT_NAME,
            ).put(
                "version",
                FORMAT_VERSION,
            ).put(
                "dictionary",
                snapshot.dictionary.preferenceValue,
            ).put(
                "ruleOverrides",
                rules,
            ).toString(2)
    }

    fun decode(json: String): GrammalectePreferencesSnapshot {
        require(
            json.length <= MAX_JSON_LENGTH,
        ) {
            "Preferences JSON exceeds $MAX_JSON_LENGTH characters"
        }

        val root =
            try {
                JSONObject(json)
            } catch (error: JSONException) {
                throw IllegalArgumentException(
                    "Invalid preferences JSON",
                    error,
                )
            }

        require(
            root.optString(
                "format",
                "",
            ) == FORMAT_NAME,
        ) {
            "Unsupported preferences format"
        }

        val version =
            root.opt("version")

        require(
            version is Number &&
                version.toInt() == FORMAT_VERSION &&
                version.toDouble() ==
                FORMAT_VERSION.toDouble(),
        ) {
            "Unsupported preferences format version"
        }

        val dictionaryValue =
            root.opt("dictionary") as? String
                ?: throw IllegalArgumentException(
                    "Missing dictionary preference",
                )

        val dictionary =
            GrammalecteDictionary.entries
                .firstOrNull { candidate ->
                    candidate.preferenceValue ==
                        dictionaryValue
                } ?: throw IllegalArgumentException(
                "Unknown dictionary preference: $dictionaryValue",
            )

        val rules =
            root.optJSONObject(
                "ruleOverrides",
            ) ?: throw IllegalArgumentException(
                "Missing ruleOverrides object",
            )

        val overrides =
            mutableMapOf<String, Boolean>()

        val keys =
            rules.keys()

        while (keys.hasNext()) {
            val id =
                keys.next()

            validateRuleId(id)

            val enabled =
                rules.opt(id)

            require(enabled is Boolean) {
                "Rule override must be boolean: $id"
            }

            overrides[id] =
                enabled
        }

        return GrammalectePreferencesSnapshot(
            dictionary = dictionary,
            ruleOverrides = overrides.toSortedMap(),
        )
    }

    private fun validateRuleId(id: String) {
        require(id.isNotBlank()) {
            "Rule option id must not be blank"
        }

        require(
            id.length <= MAX_RULE_ID_LENGTH,
        ) {
            "Rule option id is too long"
        }
    }
}
