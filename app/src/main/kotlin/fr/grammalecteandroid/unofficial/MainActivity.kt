package fr.grammalecteandroid.unofficial

import android.app.Activity
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.provider.Settings
import android.text.InputType
import android.text.style.SuggestionSpan
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class MainActivity :
    Activity(),
    SpellCheckerSession.SpellCheckerSessionListener {
    private lateinit var resultView: TextView
    private lateinit var systemEditText: EditText
    private lateinit var imeStatusView: TextView
    private var spellCheckerSession: SpellCheckerSession? = null

    private val testText = "Je sui aller au magazin hier."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val padding = (24 * resources.displayMetrics.density).toInt()
        val spacing = (12 * resources.displayMetrics.density).toInt()

        val layout =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(padding, padding, padding, padding)
            }

        layout.addView(
            TextView(this).apply {
                text = getString(R.string.title)
                textSize = 26f
            },
            matchWrapParams(),
        )

        layout.addView(
            TextView(this).apply {
                text = getString(R.string.intro)
                textSize = 17f
                setPadding(0, spacing, 0, spacing)
            },
            matchWrapParams(),
        )

        layout.addView(
            TextView(this).apply {
                text = getString(R.string.setup_steps)
                textSize = 16f
            },
            matchWrapParams(),
        )

        layout.addView(
            TextView(this).apply {
                text =
                    getString(
                        if (grammalecteAssetsPresent()) {
                            R.string.engine_status_ready
                        } else {
                            R.string.engine_status_missing
                        },
                    )
                textSize = 14f
                setPadding(0, spacing, 0, spacing)
            },
            matchWrapParams(),
        )

        layout.addView(
            Button(this).apply {
                text = getString(R.string.open_settings)
                setOnClickListener {
                    openSpellCheckerSettings()
                }
            },
            matchWrapParams(),
        )

        imeStatusView =
            TextView(this).apply {
                textSize = 14f
                setPadding(0, spacing, 0, spacing)
            }

        layout.addView(imeStatusView, matchWrapParams())

        layout.addView(
            Button(this).apply {
                text = getString(R.string.enable_ime)
                setOnClickListener {
                    openInputMethodSettings()
                }
            },
            matchWrapParams(),
        )

        layout.addView(
            Button(this).apply {
                text = getString(R.string.choose_ime)
                setOnClickListener {
                    showInputMethodPicker()
                }
            },
            matchWrapParams(),
        )

        layout.addView(
            Button(this).apply {
                text = getString(R.string.add_qs_tile)
                setOnClickListener {
                    requestQuickSettingsTile()
                }
            },
            matchWrapParams(),
        )

        layout.addView(
            Button(this).apply {
                text = "Tester Grammalecte"
                setOnClickListener {
                    runSpellCheckerTest()
                }
            },
            matchWrapParams(),
        )

        systemEditText =
            EditText(this).apply {
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
                setPadding(0, spacing, 0, spacing)
            }

        layout.addView(systemEditText, matchWrapParams())

        layout.addView(
            Button(this).apply {
                text = "Tester dans un champ Android"
                setOnClickListener {
                    systemEditText.requestFocus()
                    systemEditText.setSelection(systemEditText.text.length)

                    getSystemService(InputMethodManager::class.java)
                        .showSoftInput(
                            systemEditText,
                            InputMethodManager.SHOW_IMPLICIT,
                        )
                }
            },
            matchWrapParams(),
        )

        layout.addView(
            Button(this).apply {
                text = "Inspecter les corrections Android"
                setOnClickListener {
                    inspectSuggestionSpans()
                }
            },
            matchWrapParams(),
        )

        resultView =
            TextView(this).apply {
                text = "Test : $testText"
                textSize = 16f
                setPadding(0, spacing, 0, 0)
            }

        layout.addView(resultView, matchWrapParams())

        setContentView(layout)
    }

    private fun runSpellCheckerTest() {
        spellCheckerSession?.close()
        spellCheckerSession = null

        resultView.text = "Test en cours...\n\n$testText"

        val manager = getSystemService(TextServicesManager::class.java)

        val session =
            manager.newSpellCheckerSession(
                null,
                Locale.FRANCE,
                this,
                false,
            )

        if (session == null) {
            resultView.text =
                "Android n'a pas pu créer de session de correction orthographique."
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
            resultView.text = "Session appelée, mais aucun résultat reçu."
            return
        }

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
            output.append("\nAucune erreur retournée.")
        }

        resultView.text = output.toString()
    }

    override fun onGetSuggestions(results: Array<SuggestionsInfo>?) {
        if (results.isNullOrEmpty()) {
            return
        }

        resultView.append(
            "\n\nLe service a retourné ${results.size} résultat(s) mot par mot.",
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
            resultView.text = "Aucun SuggestionSpan present dans le champ."
            return
        }

        val output = StringBuilder()
        output.append("SuggestionSpan trouves : ")
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

            output.append("\n   flags=")
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
        updateImeStatus()
    }

    override fun onDestroy() {
        spellCheckerSession?.close()
        spellCheckerSession = null
        super.onDestroy()
    }

    private fun updateImeStatus() {
        val manager = getSystemService(InputMethodManager::class.java)

        val enabled =
            manager.enabledInputMethodList.any { inputMethod ->
                inputMethod.packageName == packageName &&
                    inputMethod.serviceName == GrammalecteImeService::class.java.name
            }

        imeStatusView.text =
            getString(
                if (enabled) {
                    R.string.ime_status_enabled
                } else {
                    R.string.ime_status_disabled
                },
            )
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

    private fun matchWrapParams(): ViewGroup.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
}
