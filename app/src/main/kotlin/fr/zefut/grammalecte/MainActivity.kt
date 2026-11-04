package fr.zefut.grammalecte

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.provider.Settings
import android.text.InputType
import android.text.style.SuggestionSpan
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

class MainActivity :
    ComponentActivity(),
    SpellCheckerSession.SpellCheckerSessionListener {
    private lateinit var resultView: TextView
    private lateinit var systemEditText: EditText
    private var correctionStatus by mutableStateOf<CorrectionStatus?>(null)
    private var showTestResult by mutableStateOf(false)
    private var testPending by mutableStateOf(false)
    private var testError by mutableStateOf<String?>(null)
    private var testIssues by mutableStateOf<List<HomeCorrectionIssue>>(emptyList())
    private var diagnosticsVisible by mutableStateOf(false)
    private var spellCheckerSession: SpellCheckerSession? = null

    private val testText = "Je sui aller au magazin hier."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retain the real Android text field/theme for native suggestion testing.
        val darkMode =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        val diagnosticTheme =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                android.R.style.Theme_DeviceDefault_DayNight
            } else if (darkMode) {
                android.R.style.Theme_DeviceDefault
            } else {
                android.R.style.Theme_DeviceDefault_Light
            }
        systemEditText =
            EditText(ContextThemeWrapper(this, diagnosticTheme)).apply {
                textLocales = LocaleList(Locale.FRANCE)
                hint = "Zone de test du correcteur Android"
                textSize = 18f
                minLines = 4
                gravity = Gravity.TOP
                inputType =
                    InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                    InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
                setText(testText)
                setSelection(text.length)
            }

        resultView =
            TextView(this).apply {
                text = "Test : $testText"
                textSize = 14f
                enablePoliteAccessibilityUpdates()
                setTextColor(getColor(R.color.surface_text))
            }

        setContent {
            GrammalecteDashboard(
                status = correctionStatus,
                showTestResult = showTestResult,
                testPending = testPending,
                testError = testError,
                testIssues = testIssues,
                diagnosticsVisible = diagnosticsVisible,
                engineReady = grammalecteAssetsPresent(),
                systemEditText = systemEditText,
                resultView = resultView,
                onTest = {
                    showTestResult = true
                    runSpellCheckerTest()
                },
                onDismissTest = { showTestResult = false },
                onConfigure = { openSpellCheckerSettings() },
                onGuide = {
                    startActivity(Intent(this, SetupGuideActivity::class.java))
                },
                onChooseKeyboard = {
                    if (correctionStatus?.ime == ImeStatus.DISABLED) {
                        openInputMethodSettings()
                    } else {
                        showInputMethodPicker()
                    }
                },
                onManageKeyboards = { openInputMethodSettings() },
                onDictionary = {
                    startActivity(Intent(this, PersonalDictionaryActivity::class.java))
                },
                onRules = {
                    startActivity(Intent(this, RuleSettingsActivity::class.java))
                },
                onStatus = {
                    startActivity(Intent(this, CorrectionStatusActivity::class.java))
                },
                onDiagnostics = { diagnosticsVisible = !diagnosticsVisible },
                onTestInField = {
                    systemEditText.requestFocus()
                    systemEditText.setSelection(systemEditText.text.length)
                    getSystemService(InputMethodManager::class.java)
                        .showSoftInput(systemEditText, InputMethodManager.SHOW_IMPLICIT)
                },
                onInspect = { inspectSuggestionSpans() },
                onQuickTile = { requestQuickSettingsTile() },
                aboutDescription = getString(R.string.project_info_description),
                onProjectWebsite = {
                    runCatching {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(getString(R.string.project_info_website_url)),
                            ),
                        )
                    }.onFailure {
                        Toast
                            .makeText(
                                this,
                                R.string.project_info_open_failed,
                                Toast.LENGTH_LONG,
                            ).show()
                    }
                },
            )
        }

        showSetupGuideIfNeeded()
    }

    private fun showSetupGuideIfNeeded() {
        val preferences =
            SetupGuidePreferences(this)

        if (!preferences.shouldShowAutomatically()) {
            return
        }

        preferences.markShown()

        startActivity(
            Intent(
                this,
                SetupGuideActivity::class.java,
            ),
        )
    }

    private fun runSpellCheckerTest() {
        testPending = true
        testError = null
        testIssues = emptyList()
        spellCheckerSession?.close()
        spellCheckerSession = null

        resultView.text = "Test en cours…\n\n$testText"

        val manager = getSystemService(TextServicesManager::class.java)

        val session =
            manager.newSpellCheckerSession(
                null,
                Locale.FRANCE,
                this,
                false,
            )

        if (session == null) {
            testPending = false
            testError = "Android n’a pas pu ouvrir le correcteur. Vérifiez son activation."
            resultView.text =
                "Android n’a pas pu créer de session de correction orthographique."
            return
        }

        spellCheckerSession = session

        session.getSentenceSuggestions(
            arrayOf(TextInfo(testText)),
            5,
        )
    }

    override fun onGetSentenceSuggestions(results: Array<SentenceSuggestionsInfo>?) {
        if (results.isNullOrEmpty()) {
            testPending = false
            testError = "La session est ouverte, mais Android n’a retourné aucun résultat."
            resultView.text = "Session ouverte, mais aucun résultat reçu."
            return
        }

        val visibleIssues = mutableListOf<HomeCorrectionIssue>()
        val output = StringBuilder()
        output.append("Texte :\n")
        output.append(testText)
        output.append("\n\nRésultats :\n")

        var issueCount = 0

        results.forEach { sentence ->
            for (index in 0 until sentence.suggestionsCount) {
                val info = sentence.getSuggestionsInfoAt(index)
                val offset = sentence.getOffsetAt(index)
                val length = sentence.getLengthAt(index)

                if (offset < 0 || length <= 0 || offset + length > testText.length) {
                    continue
                }

                issueCount++

                val fragment = testText.substring(offset, offset + length)
                val suggestions =
                    buildList {
                        for (suggestionIndex in 0 until info.suggestionsCount) {
                            add(info.getSuggestionAt(suggestionIndex))
                        }
                    }

                visibleIssues.add(HomeCorrectionIssue(fragment, suggestions))
                output.append("\n")
                output.append(issueCount)
                output.append(". « ")
                output.append(fragment)
                output.append(" »")
                output.append("\n   position=")
                output.append(offset)
                output.append(" longueur=")
                output.append(length)
                output.append("\n   suggestions=")
                output.append(
                    if (suggestions.isEmpty()) {
                        "(aucune)"
                    } else {
                        suggestions.joinToString(", ")
                    },
                )
                output.append("\n   attributs=")
                output.append(info.suggestionsAttributes)
                output.append("\n")
            }
        }

        if (issueCount == 0) {
            output.append("\nAucune erreur détectée.")
        }

        resultView.text = output.toString()
        testIssues = visibleIssues
        testPending = false
    }

    override fun onGetSuggestions(results: Array<SuggestionsInfo>?) {
        if (results.isNullOrEmpty()) {
            return
        }

        resultView.append(
            "\n\n" +
                resources.getQuantityString(
                    R.plurals.word_by_word_results,
                    results.size,
                    results.size,
                ),
        )
    }

    private fun inspectSuggestionSpans() {
        val text = systemEditText.text

        val spans =
            text.getSpans(
                0,
                text.length,
                SuggestionSpan::class.java,
            )

        if (spans.isEmpty()) {
            resultView.text = "Aucun SuggestionSpan présent dans le champ."
            return
        }

        val output = StringBuilder()
        output.append("Nombre de SuggestionSpan : ")
        output.append(spans.size)
        output.append("\n")

        spans.forEachIndexed { index, span ->
            val start = text.getSpanStart(span)
            val end = text.getSpanEnd(span)

            output.append("\n")
            output.append(index + 1)
            output.append(". plage=")
            output.append(start)
            output.append("..")
            output.append(end)

            if (start >= 0 && end >= start && end <= text.length) {
                output.append(" texte=«")
                output.append(text.substring(start, end))
                output.append("»")
            }

            output.append("\n   indicateurs=")
            output.append(span.flags)

            output.append("\n   suggestions=")
            output.append(
                span.suggestions.joinToString(" | ").ifBlank { "(aucune)" },
            )
            output.append("\n")
        }

        resultView.text = output.toString()
    }

    override fun onResume() {
        super.onResume()
        correctionStatus = CorrectionStatusReader(this).read()
    }

    override fun onDestroy() {
        spellCheckerSession?.close()
        spellCheckerSession = null
        super.onDestroy()
    }

    private fun openInputMethodSettings() {
        runCatching {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }.onFailure {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun showInputMethodPicker() {
        getSystemService(InputMethodManager::class.java)
            .showInputMethodPicker()
    }

    private fun requestQuickSettingsTile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast
                .makeText(
                    this,
                    R.string.qs_tile_manual_add,
                    Toast.LENGTH_LONG,
                ).show()
            return
        }

        val manager =
            getSystemService(StatusBarManager::class.java)

        manager.requestAddTileService(
            ComponentName(
                this,
                GrammalecteTileService::class.java,
            ),
            getString(R.string.qs_tile_label),
            Icon.createWithResource(
                this,
                R.drawable.ic_qs_grammalecte,
            ),
            mainExecutor,
        ) { result ->
            val message =
                when (result) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                        R.string.qs_tile_added

                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                        R.string.qs_tile_already_added

                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED ->
                        R.string.qs_tile_not_added

                    else -> null
                }

            Toast
                .makeText(
                    this,
                    message?.let(::getString)
                        ?: getString(
                            R.string.qs_tile_add_error,
                            result,
                        ),
                    Toast.LENGTH_LONG,
                ).show()
        }
    }

    private fun openSpellCheckerSettings() {
        val directIntent =
            Intent(Intent.ACTION_MAIN).apply {
                component =
                    ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings\$SpellCheckersSettingsActivity",
                    )
                addCategory(Intent.CATEGORY_DEFAULT)
            }

        val intent =
            if (directIntent.resolveActivity(packageManager) != null) {
                directIntent
            } else {
                Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            }

        runCatching {
            startActivity(intent)
        }.onFailure {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun grammalecteAssetsPresent(): Boolean =
        runCatching {
            assets.open("grammalecte/fr/gc_engine.js").close()
            true
        }.getOrDefault(false)
}
