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

class GrammalecteQuickJsEngine internal constructor(
    private val assetLoader: AssetTextLoader,
) : GrammarEngine {
    constructor(context: Context) : this(AndroidAssetTextLoader(context))

    private val lock = Any()
    private var runtime: QuickJs? = null

    override fun check(
        text: String,
        localeTag: String,
    ): List<GrammarIssue> {
        if (text.isBlank()) return emptyList()

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

    override fun close() {
        synchronized(lock) {
            runtime?.close()
            runtime = null
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
        private const val MAX_MEMORY_BYTES = 256L * 1024L * 1024L
        private const val INITIALIZATION_TIMEOUT_MILLIS = 5_000L
        private const val ANALYSIS_TIMEOUT_MILLIS = 2_000L
    }
}

class GrammalecteEngineException(
    message: String,
    cause: Throwable,
) : IllegalStateException(message, cause)
