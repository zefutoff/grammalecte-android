package fr.grammalecteandroid.engine

import android.content.Context
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.function
import fr.grammalecteandroid.core.GrammarEngine
import fr.grammalecteandroid.core.GrammarIssue
import fr.grammalecteandroid.core.IssueKind
import fr.grammalecteandroid.core.IssueNormalizer
import fr.grammalecteandroid.core.WordCheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

internal data class GrammalecteRuntimeMemorySnapshot(
    val memoryUsedBytes: Long,
    val mallocBytes: Long,
    val objectCount: Long,
    val stringCount: Long,
)

class GrammalecteQuickJsEngine internal constructor(
    private val assetLoader: AssetTextLoader,
    private val rulePreferences: GrammalecteRulePreferences? = null,
    private val dictionaryPreferences: GrammalecteDictionaryPreferences? = null,
    private val personalDictionaryPreferences: GrammalectePersonalDictionaryPreferences? = null,
) : GrammarEngine {
    constructor(context: Context) :
        this(
            AndroidAssetTextLoader(context),
            GrammalecteRulePreferences(context),
            GrammalecteDictionaryPreferences(context),
            GrammalectePersonalDictionaryPreferences(context),
        )

    private val lock = Any()
    private var runtime: QuickJs? = null
    private var appliedRuleOverrides: Map<String, Boolean>? = null
    private var appliedDictionary: GrammalecteDictionary? = null
    private var appliedPersonalDictionaryWords: List<String>? = null

    override fun check(
        text: String,
        localeTag: String,
    ): List<GrammarIssue> {
        if (text.isBlank()) return emptyList()

        synchronizeStoredDictionary()
        synchronizeStoredPersonalDictionary()
        synchronizeStoredRuleOptions()

        val raw = callBridge("__grammalecteAndroid.check", text, localeTag)
        return IssueNormalizer.normalize(
            text = text,
            issues = parseIssues(raw),
            suggestionLimit = DEFAULT_SUGGESTION_LIMIT,
        )
    }

    override fun checkWord(
        word: String,
        localeTag: String,
        suggestionLimit: Int,
    ): WordCheck {
        if (word.isBlank()) return WordCheck(valid = true)

        synchronizeStoredDictionary()
        synchronizeStoredPersonalDictionary()

        val safeLimit = suggestionLimit.coerceAtLeast(0)

        val raw =
            callBridge(
                function = "__grammalecteAndroid.checkWord",
                args = arrayOf<Any>(word, localeTag, safeLimit),
            )
        val json = JSONObject(raw)
        return WordCheck(
            valid = json.getBoolean("valid"),
            suggestions = json.getJSONArray("suggestions").toStringList().take(safeLimit),
        )
    }

    fun selectedDictionary(): GrammalecteDictionary {
        synchronizeStoredDictionary()

        return synchronized(lock) {
            appliedDictionary
                ?: GrammalecteDictionary.ALL_VARIANTS
        }
    }

    fun setDictionary(dictionary: GrammalecteDictionary) {
        synchronized(lock) {
            applyDictionary(dictionary)

            dictionaryPreferences?.select(
                dictionary,
            )

            appliedDictionary =
                dictionary
        }
    }

    fun resetDictionary() {
        synchronized(lock) {
            applyDictionary(
                GrammalecteDictionary.ALL_VARIANTS,
            )

            dictionaryPreferences?.reset()

            appliedDictionary =
                GrammalecteDictionary.ALL_VARIANTS
        }
    }

    fun personalDictionaryWords(): List<String> {
        synchronizeStoredPersonalDictionary()

        return synchronized(lock) {
            appliedPersonalDictionaryWords.orEmpty()
        }
    }

    fun setPersonalDictionaryWords(words: Collection<String>) {
        val normalized =
            normalizePersonalDictionaryWords(words)

        synchronized(lock) {
            applyPersonalDictionaryWords(
                normalized,
            )

            personalDictionaryPreferences
                ?.replace(normalized)

            appliedPersonalDictionaryWords =
                normalized
        }
    }

    fun addPersonalDictionaryWord(word: String): Boolean {
        val normalizedWord =
            normalizePersonalDictionaryWords(
                listOf(word),
            ).single()

        synchronized(lock) {
            synchronizeStoredPersonalDictionary()

            val current =
                appliedPersonalDictionaryWords.orEmpty()

            if (normalizedWord in current) {
                return false
            }

            val updated =
                normalizePersonalDictionaryWords(
                    current + normalizedWord,
                )

            applyPersonalDictionaryWords(updated)

            personalDictionaryPreferences
                ?.replace(updated)

            appliedPersonalDictionaryWords =
                updated

            return true
        }
    }

    fun removePersonalDictionaryWord(word: String): Boolean {
        val normalizedWord =
            word.trim()

        if (normalizedWord.isEmpty()) {
            return false
        }

        synchronized(lock) {
            synchronizeStoredPersonalDictionary()

            val current =
                appliedPersonalDictionaryWords.orEmpty()

            if (normalizedWord !in current) {
                return false
            }

            val updated =
                current.filterNot { storedWord ->
                    storedWord == normalizedWord
                }

            applyPersonalDictionaryWords(updated)

            personalDictionaryPreferences
                ?.replace(updated)

            appliedPersonalDictionaryWords =
                updated

            return true
        }
    }

    fun ruleOptions(): List<GrammalecteRuleOption> {
        synchronizeStoredRuleOptions()

        val raw =
            callBridge(
                "__grammalecteAndroid.ruleOptions",
            )

        val array = JSONArray(raw)

        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)

                add(
                    GrammalecteRuleOption(
                        id = item.getString("id"),
                        groupId = item.getString("groupId"),
                        groupLabel = item.getString("groupLabel"),
                        label = item.getString("label"),
                        description = item.optString("description"),
                        enabled = item.getBoolean("enabled"),
                        defaultEnabled = item.getBoolean("defaultEnabled"),
                    ),
                )
            }
        }
    }

    fun setRuleOption(
        id: String,
        enabled: Boolean,
    ) {
        require(id.isNotBlank()) {
            "Grammalecte rule option id must not be blank"
        }

        synchronized(lock) {
            synchronizeStoredRuleOptions()

            val updated =
                callBridge(
                    "__grammalecteAndroid.setRuleOption",
                    id,
                    enabled,
                ).toBooleanStrictOrNull() ?: false

            require(updated) {
                "Unknown Grammalecte rule option: $id"
            }

            rulePreferences?.let { preferences ->
                preferences.setRuleOption(
                    id = id,
                    enabled = enabled,
                )

                appliedRuleOverrides =
                    preferences.overrides()
            }
        }
    }

    fun resetRuleOptions() {
        synchronized(lock) {
            callBridge(
                "__grammalecteAndroid.resetRuleOptions",
            )

            rulePreferences?.reset()

            appliedRuleOverrides =
                rulePreferences?.overrides()
        }
    }

    fun exportPreferences(): String =
        synchronized(lock) {
            val dictionary =
                selectedDictionary()

            val overrides =
                ruleOverridesFrom(
                    ruleOptions(),
                )

            GrammalectePreferencesJson.encode(
                GrammalectePreferencesSnapshot(
                    dictionary = dictionary,
                    ruleOverrides = overrides,
                ),
            )
        }

    fun importPreferences(json: String) {
        val snapshot =
            GrammalectePreferencesJson.decode(
                json,
            )

        synchronized(lock) {
            val currentOptions =
                ruleOptions()

            val supportedOptions =
                currentOptions.associateBy { option ->
                    option.id
                }

            val unknownRuleIds =
                snapshot.ruleOverrides.keys -
                    supportedOptions.keys

            require(unknownRuleIds.isEmpty()) {
                "Unknown Grammalecte rule options: " +
                    unknownRuleIds
                        .sorted()
                        .joinToString(", ")
            }

            val importedOverrides =
                snapshot.ruleOverrides
                    .filter { (id, enabled) ->
                        enabled !=
                            supportedOptions
                                .getValue(id)
                                .defaultEnabled
                    }.toSortedMap()

            val previousDictionary =
                selectedDictionary()

            val previousOverrides =
                ruleOverridesFrom(
                    currentOptions,
                )

            try {
                applyDictionary(
                    snapshot.dictionary,
                )

                applyRuleOverrides(
                    importedOverrides,
                )
            } catch (error: Throwable) {
                runCatching {
                    applyDictionary(
                        previousDictionary,
                    )

                    applyRuleOverrides(
                        previousOverrides,
                    )
                }.exceptionOrNull()
                    ?.let(error::addSuppressed)

                throw error
            }

            if (
                snapshot.dictionary ==
                GrammalecteDictionary.ALL_VARIANTS
            ) {
                dictionaryPreferences?.reset()
            } else {
                dictionaryPreferences?.select(
                    snapshot.dictionary,
                )
            }

            rulePreferences?.replace(
                importedOverrides,
            )

            appliedDictionary =
                snapshot.dictionary

            appliedRuleOverrides =
                rulePreferences?.overrides()
                    ?: importedOverrides
        }
    }

    fun resetToDefaults() {
        synchronized(lock) {
            applyDictionary(
                GrammalecteDictionary.ALL_VARIANTS,
            )

            applyPersonalDictionaryWords(
                emptyList(),
            )

            callBridge(
                "__grammalecteAndroid.resetRuleOptions",
            )

            dictionaryPreferences?.reset()
            personalDictionaryPreferences?.reset()
            rulePreferences?.reset()

            appliedDictionary =
                GrammalecteDictionary.ALL_VARIANTS

            appliedPersonalDictionaryWords =
                emptyList()

            appliedRuleOverrides =
                rulePreferences?.overrides()
        }
    }

    private fun synchronizeStoredDictionary() {
        val preferences =
            dictionaryPreferences ?: return

        val selected =
            preferences.selected()

        synchronized(lock) {
            if (appliedDictionary == selected) {
                return
            }

            if (
                runtime == null &&
                selected == GrammalecteDictionary.ALL_VARIANTS
            ) {
                appliedDictionary =
                    GrammalecteDictionary.ALL_VARIANTS

                return
            }

            applyDictionary(selected)

            appliedDictionary =
                selected
        }
    }

    private fun applyDictionary(dictionary: GrammalecteDictionary) {
        val updated =
            callBridge(
                "__grammalecteAndroid.setDictionary",
                dictionary.fileName,
            ).toBooleanStrictOrNull() ?: false

        check(updated) {
            "Unable to load Grammalecte dictionary: ${dictionary.fileName}"
        }
    }

    private fun synchronizeStoredPersonalDictionary() {
        val preferences =
            personalDictionaryPreferences ?: return

        val words =
            preferences.words()

        synchronized(lock) {
            if (appliedPersonalDictionaryWords == words) {
                return
            }

            if (
                runtime == null &&
                words.isEmpty()
            ) {
                appliedPersonalDictionaryWords =
                    emptyList()

                return
            }

            applyPersonalDictionaryWords(words)

            appliedPersonalDictionaryWords =
                words
        }
    }

    private fun applyPersonalDictionaryWords(words: List<String>) {
        val updated =
            callBridge(
                "__grammalecteAndroid.setPersonalWords",
                JSONArray(words).toString(),
            ).toBooleanStrictOrNull() ?: false

        check(updated) {
            "Unable to update Grammalecte personal dictionary"
        }
    }

    private fun normalizePersonalDictionaryWords(words: Collection<String>): List<String> {
        val normalized =
            words
                .map(String::trim)
                .distinct()
                .sorted()

        require(
            normalized.size <=
                MAX_PERSONAL_DICTIONARY_WORDS,
        ) {
            "Personal dictionary exceeds " +
                "$MAX_PERSONAL_DICTIONARY_WORDS words"
        }

        normalized.forEach { word ->
            require(word.isNotEmpty()) {
                "Personal dictionary words must not be blank"
            }

            require(
                word.length <=
                    MAX_PERSONAL_WORD_LENGTH,
            ) {
                "Personal dictionary word is too long: $word"
            }

            require(
                word.none(Char::isWhitespace),
            ) {
                "Personal dictionary entries must contain " +
                    "a single word: $word"
            }
        }

        return normalized
    }

    private fun ruleOverridesFrom(options: List<GrammalecteRuleOption>): Map<String, Boolean> =
        options
            .asSequence()
            .filter { option ->
                option.enabled !=
                    option.defaultEnabled
            }.associate { option ->
                option.id to option.enabled
            }.toSortedMap()

    private fun applyRuleOverrides(overrides: Map<String, Boolean>) {
        callBridge(
            "__grammalecteAndroid.resetRuleOptions",
        )

        overrides.forEach { (id, enabled) ->
            val updated =
                callBridge(
                    "__grammalecteAndroid.setRuleOption",
                    id,
                    enabled,
                ).toBooleanStrictOrNull() ?: false

            check(updated) {
                "Unable to apply Grammalecte rule option: $id"
            }
        }
    }

    private fun synchronizeStoredRuleOptions() {
        val preferences =
            rulePreferences ?: return

        val overrides =
            preferences.overrides()

        synchronized(lock) {
            if (appliedRuleOverrides == overrides) {
                return
            }

            callBridge(
                "__grammalecteAndroid.resetRuleOptions",
            )

            overrides.forEach { (id, enabled) ->
                val updated =
                    callBridge(
                        "__grammalecteAndroid.setRuleOption",
                        id,
                        enabled,
                    ).toBooleanStrictOrNull() ?: false

                if (!updated) {
                    return@forEach
                }
            }

            appliedRuleOverrides = overrides
        }
    }

    internal fun collectRuntimeGarbageAndSnapshot(): GrammalecteRuntimeMemorySnapshot =
        synchronized(lock) {
            val js =
                runtime
                    ?: createRuntime().also {
                        runtime = it
                    }

            js.gc()

            val usage =
                js.memoryUsage

            GrammalecteRuntimeMemorySnapshot(
                memoryUsedBytes = usage.memoryUsedSize,
                mallocBytes = usage.mallocSize,
                objectCount = usage.objCount,
                stringCount = usage.strCount,
            )
        }

    override fun close() {
        synchronized(lock) {
            runtime?.close()
            runtime = null
            appliedRuleOverrides = null
            appliedDictionary = null
            appliedPersonalDictionaryWords = null
        }
    }

    private fun callBridge(
        function: String,
        vararg args: Any,
    ): String =
        synchronized(lock) {
            val js = runtime ?: createRuntime().also { runtime = it }
            val encodedArguments = args.joinToString(",") { arg -> JSONObject.wrap(arg).toJsonLiteral() }
            runBlocking {
                js.evaluate<String>("$function($encodedArguments)")
            }
        }

    private fun createRuntime(): QuickJs {
        val js =
            QuickJs.create(Dispatchers.Default).apply {
                memoryLimit = MAX_MEMORY_BYTES
                evaluationTimeoutMillis = INITIALIZATION_TIMEOUT_MILLIS
            }

        try {
            js.function<String, String>("__androidReadAsset") { path -> assetLoader.readText(path) }

            runBlocking {
                js.evaluate<Any?>(
                    """
                    globalThis.self = globalThis;
                    globalThis.__grammalecteConsoleErrors = [];
                    globalThis.console = {
                      log: function () {},
                      warn: function () {},
                      error: function (value) {
                        globalThis.__grammalecteConsoleErrors.push(String(value));
                      }
                    };
                    """.trimIndent(),
                )

                js.evaluate<Any?>(assetLoader.readText(GrammalecteScriptBundle.HELPER_SCRIPT))

                js.evaluate<Any?>(
                    """
                    helpers.loadFile = function (path) {
                      return __androidReadAsset(path);
                    };
                    """.trimIndent(),
                )

                for (script in GrammalecteScriptBundle.scriptsAfterHelpers) {
                    js.evaluate<Any?>(assetLoader.readText(script))
                }

                js.evaluate<Any?>(assetLoader.readText(GrammalecteScriptBundle.BRIDGE_SCRIPT))
                js.evaluate<Any?>("__grammalecteAndroid.init()")

                val initializationErrors =
                    js.evaluate<String>(
                        "globalThis.__grammalecteConsoleErrors.join(' | ')",
                    )
                check(initializationErrors.isBlank()) {
                    "Grammalecte initialization reported errors: $initializationErrors"
                }

                appliedDictionary =
                    GrammalecteDictionary.ALL_VARIANTS

                appliedPersonalDictionaryWords =
                    emptyList()

                js.evaluationTimeoutMillis = ANALYSIS_TIMEOUT_MILLIS
            }
        } catch (error: Throwable) {
            js.close()
            throw GrammalecteEngineException("Unable to initialize the embedded Grammalecte runtime", error)
        }

        return js
    }

    private fun parseIssues(json: String): List<GrammarIssue> {
        val array = JSONArray(json)
        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    GrammarIssue(
                        start = item.getInt("start"),
                        endExclusive = item.getInt("end"),
                        suggestions = item.optJSONArray("suggestions")?.toStringList().orEmpty(),
                        message = item.optString("message").takeIf(String::isNotBlank),
                        ruleId = item.optString("ruleId").takeIf(String::isNotBlank),
                        kind = item.optString("kind").toIssueKind(),
                    ),
                )
            }
        }
    }

    private fun JSONArray.toStringList(): List<String> =
        buildList(length()) {
            for (index in 0 until length()) {
                optString(index).takeIf(String::isNotBlank)?.let(::add)
            }
        }

    private fun String.toIssueKind(): IssueKind = runCatching { IssueKind.valueOf(this) }.getOrDefault(IssueKind.UNKNOWN)

    private fun Any?.toJsonLiteral(): String =
        when (this) {
            null, JSONObject.NULL -> "null"
            is Number, is Boolean -> toString()
            else -> JSONObject.quote(toString())
        }

    companion object {
        private const val DEFAULT_SUGGESTION_LIMIT = 8
        private const val MAX_PERSONAL_DICTIONARY_WORDS = 2048
        private const val MAX_PERSONAL_WORD_LENGTH = 64
        private const val MAX_MEMORY_BYTES = 256L * 1024L * 1024L
        private const val INITIALIZATION_TIMEOUT_MILLIS = 5_000L
        private const val ANALYSIS_TIMEOUT_MILLIS = 2_000L
    }
}

class GrammalecteEngineException(
    message: String,
    cause: Throwable,
) : IllegalStateException(message, cause)
