package fr.grammalecteandroid.engine

enum class GrammalecteDictionary(
    val preferenceValue: String,
    internal val fileName: String,
) {
    ALL_VARIANTS(
        preferenceValue = "all_variants",
        fileName = "fr-allvars.json",
    ),

    CLASSIC(
        preferenceValue = "classic",
        fileName = "fr-classic.json",
    ),

    REFORM_1990(
        preferenceValue = "reform_1990",
        fileName = "fr-reform.json",
    ),
    ;

    companion object {
        fun fromPreferenceValue(value: String?): GrammalecteDictionary =
            entries.firstOrNull { dictionary ->
                dictionary.preferenceValue == value
            } ?: ALL_VARIANTS
    }
}
