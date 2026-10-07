package fr.grammalecteandroid.engine

data class GrammalecteRuleOption(
    val id: String,
    val groupId: String,
    val groupLabel: String,
    val label: String,
    val description: String,
    val enabled: Boolean,
    val defaultEnabled: Boolean,
)
