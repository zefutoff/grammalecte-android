package fr.grammalecteandroid.unofficial

import android.app.Activity
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.provider.Settings
import android.text.InputType
import android.text.style.SuggestionSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class MainActivity :
    Activity(),
    SpellCheckerSession.SpellCheckerSessionListener {
    private lateinit var resultView: TextView
    private lateinit var systemEditText: EditText
    private lateinit var imeStatusView: TextView
    private lateinit var spellCheckerStatusView: TextView
    private lateinit var diagnosticsContainer: LinearLayout
    private var spellCheckerSession: SpellCheckerSession? = null

    private val testText = "Je sui aller au magazin hier."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val density = resources.displayMetrics.density
        val padding = (20 * density).toInt()
        val spacing = (12 * density).toInt()
        val smallSpacing = (6 * density).toInt()

        val content =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL

                setPadding(
                    padding,
                    padding,
                    padding,
                    padding,
                )

                setBackgroundColor(
                    getColor(R.color.surface_background),
                )
            }

        content.addView(
            TextView(this).apply {
                text = getString(R.string.title)
                textSize = 28f

                setTypeface(
                    typeface,
                    Typeface.BOLD,
                )

                setTextColor(
                    getColor(R.color.surface_text),
                )
            },
            matchWrapParams(),
        )

        content.addView(
            TextView(this).apply {
                text =
                    "Correction française locale avec Grammalecte. " +
                    "Aucun texte n'est envoyé sur Internet."

                textSize = 15f

                setTextColor(
                    getColor(R.color.surface_text_secondary),
                )

                setPadding(
                    0,
                    smallSpacing,
                    0,
                    spacing,
                )
            },
            matchWrapParams(),
        )

        content.addView(
            sectionTitle(
                "Configuration",
            ),
            matchWrapParams(),
        )

        content.addView(
            createCard(
                title = "État et activation",
                description =
                    "Vérifiez en un seul endroit l’état du correcteur Android " +
                        "et du clavier Grammalecte.",
                statusView = null,
                actions =
                    listOf(
                        "Voir l’état de la correction" to {
                            startActivity(
                                Intent(
                                    this@MainActivity,
                                    CorrectionStatusActivity::class.java,
                                ),
                            )
                        },
                    ),
            ),
            cardParams(spacing),
        )

        spellCheckerStatusView =
            TextView(this).apply {
                textSize = 14f
                setTextColor(
                    getColor(R.color.surface_text_secondary),
                )
            }

        content.addView(
            createCard(
                title = "Correcteur Android",
                description =
                    "Intègre Grammalecte au correcteur orthographique " +
                        "natif des applications compatibles.",
                statusView = spellCheckerStatusView,
                actions =
                    listOf(
                        "Configurer" to {
                            openSpellCheckerSettings()
                        },
                    ),
            ),
            cardParams(spacing),
        )

        imeStatusView =
            TextView(this).apply {
                textSize = 14f
                setTextColor(
                    getColor(R.color.surface_text_secondary),
                )
            }

        content.addView(
            createCard(
                title = "Correction rapide",
                description =
                    "Permet de corriger une sélection dans les applications " +
                        "qui n'utilisent pas le correcteur Android.",
                statusView = imeStatusView,
                actions =
                    listOf(
                        "Gérer la méthode de saisie" to {
                            openInputMethodSettings()
                        },
                        "Choisir Grammalecte" to {
                            showInputMethodPicker()
                        },
                    ),
            ),
            cardParams(spacing),
        )

        content.addView(
            createCard(
                title = "Réglages de correction",
                description =
                    "Choisissez le dictionnaire et les contrôles " +
                        "Grammalecte utilisés dans les analyses.",
                statusView = null,
                actions =
                    listOf(
                        "Configurer" to {
                            startActivity(
                                Intent(
                                    this@MainActivity,
                                    RuleSettingsActivity::class.java,
                                ),
                            )
                        },
                    ),
            ),
            cardParams(spacing),
        )

        content.addView(
            createCard(
                title = "Accès rapide",
                description =
                    "Ajoutez Grammalecte aux réglages rapides pour ouvrir " +
                        "le sélecteur de clavier depuis n'importe quelle application.",
                statusView = null,
                actions =
                    listOf(
                        "Ajouter la tuile" to {
                            requestQuickSettingsTile()
                        },
                    ),
            ),
            cardParams(spacing),
        )

        content.addView(
            sectionTitle(
                "Diagnostics",
            ),
            LinearLayout
                .LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin = spacing
                },
        )

        diagnosticsContainer =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
            }

        val diagnosticsButton =
            Button(this).apply {
                text = "Afficher les diagnostics"
                isAllCaps = false

                setOnClickListener {
                    val show =
                        diagnosticsContainer.visibility != View.VISIBLE

                    diagnosticsContainer.visibility =
                        if (show) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    text =
                        if (show) {
                            "Masquer les diagnostics"
                        } else {
                            "Afficher les diagnostics"
                        }
                }
            }

        content.addView(
            diagnosticsButton,
            matchWrapParams(),
        )

        diagnosticsContainer.addView(
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
                setTextColor(
                    getColor(R.color.surface_text_secondary),
                )

                setPadding(
                    0,
                    smallSpacing,
                    0,
                    smallSpacing,
                )
            },
            matchWrapParams(),
        )

        diagnosticsContainer.addView(
            Button(this).apply {
                text = "Tester Grammalecte"
                isAllCaps = false

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

                setPadding(
                    0,
                    spacing,
                    0,
                    spacing,
                )
            }

        diagnosticsContainer.addView(
            systemEditText,
            matchWrapParams(),
        )

        diagnosticsContainer.addView(
            Button(this).apply {
                text = "Tester dans un champ Android"
                isAllCaps = false

                setOnClickListener {
                    systemEditText.requestFocus()
                    systemEditText.setSelection(
                        systemEditText.text.length,
                    )

                    getSystemService(
                        InputMethodManager::class.java,
                    ).showSoftInput(
                        systemEditText,
                        InputMethodManager.SHOW_IMPLICIT,
                    )
                }
            },
            matchWrapParams(),
        )

        diagnosticsContainer.addView(
            Button(this).apply {
                text = "Inspecter les corrections Android"
                isAllCaps = false

                setOnClickListener {
                    inspectSuggestionSpans()
                }
            },
            matchWrapParams(),
        )

        resultView =
            TextView(this).apply {
                text = "Test : $testText"
                textSize = 15f

                setTextColor(
                    getColor(R.color.surface_text),
                )

                setPadding(
                    0,
                    spacing,
                    0,
                    0,
                )
            }

        diagnosticsContainer.addView(
            resultView,
            matchWrapParams(),
        )

        content.addView(
            diagnosticsContainer,
            matchWrapParams(),
        )

        val scrollView =
            ScrollView(this).apply {
                isFillViewport = true

                addView(
                    content,
                    matchWrapParams(),
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setOnApplyWindowInsetsListener { view, insets ->
                        val topInset: Int
                        val bottomInset: Int

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val systemBars =
                                insets.getInsets(
                                    WindowInsets.Type.systemBars(),
                                )

                            topInset = systemBars.top
                            bottomInset = systemBars.bottom
                        } else {
                            @Suppress("DEPRECATION")
                            topInset = insets.systemWindowInsetTop

                            @Suppress("DEPRECATION")
                            bottomInset = insets.systemWindowInsetBottom
                        }

                        view.setPadding(
                            0,
                            topInset,
                            0,
                            bottomInset,
                        )

                        insets
                    }
                }
            }

        setContentView(scrollView)
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
        updateSpellCheckerStatus()
        updateImeStatus()
    }

    override fun onDestroy() {
        spellCheckerSession?.close()
        spellCheckerSession = null
        super.onDestroy()
    }

    private fun updateSpellCheckerStatus() {
        val status =
            CorrectionStatusReader(this)
                .read()
                .spellChecker

        spellCheckerStatusView.text =
            when (status) {
                SpellCheckerStatus.SELECTED ->
                    "Activé et sélectionné comme correcteur Android."

                SpellCheckerStatus.NOT_SELECTED ->
                    "Disponible, mais non sélectionné comme correcteur Android."

                SpellCheckerStatus.DISABLED ->
                    "La correction orthographique Android est désactivée."

                SpellCheckerStatus.UNAVAILABLE ->
                    "Aucun correcteur Android actif n’a pu être détecté."
            }
    }

    private fun updateImeStatus() {
        val status =
            CorrectionStatusReader(this)
                .read()
                .ime

        imeStatusView.text =
            when (status) {
                ImeStatus.SELECTED ->
                    "Activé et actuellement sélectionné."

                ImeStatus.ENABLED ->
                    "Activé, mais un autre clavier est actuellement sélectionné."

                ImeStatus.DISABLED ->
                    "Non activé dans les méthodes de saisie Android."
            }
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

    private fun sectionTitle(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 18f

            setTypeface(
                typeface,
                Typeface.BOLD,
            )

            setTextColor(
                getColor(R.color.surface_text),
            )

            setPadding(
                0,
                8,
                0,
                8,
            )
        }

    private fun createCard(
        title: String,
        description: String,
        statusView: TextView?,
        actions: List<Pair<String, () -> Unit>>,
    ): LinearLayout {
        val density =
            resources.displayMetrics.density

        val padding =
            (14 * density).toInt()

        val spacing =
            (8 * density).toInt()

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                padding,
                padding,
                padding,
                padding,
            )

            background =
                GradientDrawable().apply {
                    shape =
                        GradientDrawable.RECTANGLE

                    cornerRadius =
                        14f * density

                    setColor(
                        getColor(R.color.surface_card),
                    )
                }

            addView(
                TextView(this@MainActivity).apply {
                    text = title
                    textSize = 17f

                    setTypeface(
                        typeface,
                        Typeface.BOLD,
                    )

                    setTextColor(
                        getColor(R.color.surface_text),
                    )
                },
                matchWrapParams(),
            )

            addView(
                TextView(this@MainActivity).apply {
                    text = description
                    textSize = 14f

                    setTextColor(
                        getColor(R.color.surface_text_secondary),
                    )

                    setPadding(
                        0,
                        spacing,
                        0,
                        spacing,
                    )
                },
                matchWrapParams(),
            )

            statusView?.let {
                addView(
                    it,
                    matchWrapParams(),
                )
            }

            actions.forEach { (label, action) ->
                addView(
                    Button(this@MainActivity).apply {
                        text = label
                        isAllCaps = false

                        setOnClickListener {
                            action()
                        }
                    },
                    matchWrapParams(),
                )
            }
        }
    }

    private fun cardParams(bottomMargin: Int): LinearLayout.LayoutParams =
        LinearLayout
            .LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                this.bottomMargin = bottomMargin
            }

    private fun matchWrapParams(): ViewGroup.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
}
