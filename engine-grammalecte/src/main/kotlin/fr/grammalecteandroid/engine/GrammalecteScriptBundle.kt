package fr.grammalecteandroid.engine

internal object GrammalecteScriptBundle {
    const val HELPER_SCRIPT = "grammalecte/graphspell/helpers.js"
    const val BRIDGE_SCRIPT = "android_bridge.js"

    val scriptsAfterHelpers = listOf(
        "grammalecte/graphspell/str_transform.js",
        "grammalecte/graphspell/char_player.js",
        "grammalecte/graphspell/lexgraph_fr.js",
        "grammalecte/graphspell/ibdawg.js",
        "grammalecte/graphspell/spellchecker.js",
        "grammalecte/text.js",
        "grammalecte/graphspell/tokenizer.js",
        "grammalecte/fr/conj.js",
        "grammalecte/fr/mfsp.js",
        "grammalecte/fr/phonet.js",
        "grammalecte/fr/cregex.js",
        "grammalecte/fr/gc_options.js",
        "grammalecte/fr/gc_functions.js",
        "grammalecte/fr/gc_rules.js",
        "grammalecte/fr/gc_rules_graph.js",
        "grammalecte/fr/gc_engine.js",
    )
}
