package fr.grammalecteandroid.unofficial

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import fr.grammalecteandroid.core.GrammarIssue
import fr.grammalecteandroid.engine.GrammalecteQuickJsEngine

class GrammalecteImeService : InputMethodService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var selectedTextView: TextView
    private lateinit var statusView: TextView
    private lateinit var issuesLayout: LinearLayout
    private lateinit var applyAllButton: Button
    private lateinit var applyAllAndReturnButton: Button
    private lateinit var applyButton: Button
    private lateinit var applyAndReturnButton: Button

    private var workingText = ""
    private var originalSelectedText = ""
    private var latestIssues: List<GrammarIssue> = emptyList()

    private var engine: GrammalecteQuickJsEngine? = null
    private var analysisGeneration = 0

    override fun onCreateInputView(): View {
        val padding =
            (12 * resources.displayMetrics.density)
                .toInt()

        val spacing =
            (8 * resources.displayMetrics.density)
                .toInt()

        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    padding,
                    padding,
                    padding,
                    padding,
                )
            }

        root.addView(
            TextView(this).apply {
                text = "Grammalecte"
                textSize = 18f
                gravity = Gravity.CENTER
            },
            matchWrapParams(),
        )

        selectedTextView =
            TextView(this).apply {
                text = "Aucune sélection."
                textSize = 15f
                setPadding(
                    0,
                    spacing,
                    0,
                    spacing,
                )
            }

        root.addView(
            selectedTextView,
            matchWrapParams(),
        )

        statusView =
            TextView(this).apply {
                text = "Sélectionnez du texte à corriger."
            }

        root.addView(
            statusView,
            matchWrapParams(),
        )

        root.addView(
            Button(this).apply {
                text = "Analyser la sélection"

                setOnClickListener {
                    loadSelectionAndAnalyze()
                }
            },
            matchWrapParams(),
        )

        val scrollView =
            ScrollView(this)

        issuesLayout =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

        scrollView.addView(
            issuesLayout,
            matchWrapParams(),
        )

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )

        applyAllButton =
            Button(this).apply {
                text = "Tout corriger"
                isEnabled = false

                setOnClickListener {
                    applyAllSuggestions(
                        returnToPrevious = false,
                    )
                }
            }

        root.addView(
            applyAllButton,
            matchWrapParams(),
        )

        applyAllAndReturnButton =
            Button(this).apply {
                text = "Tout corriger et revenir"
                isEnabled = false

                setOnClickListener {
                    applyAllSuggestions(
                        returnToPrevious = true,
                    )
                }
            }

        root.addView(
            applyAllAndReturnButton,
            matchWrapParams(),
        )

        applyButton =
            Button(this).apply {
                text = "Appliquer la correction"
                isEnabled = false

                setOnClickListener {
                    applyCorrectedText(
                        returnToPrevious = false,
                    )
                }
            }

        root.addView(
            applyButton,
            matchWrapParams(),
        )

        applyAndReturnButton =
            Button(this).apply {
                text = "Appliquer et revenir au clavier"
                isEnabled = false

                setOnClickListener {
                    applyCorrectedText(
                        returnToPrevious = true,
                    )
                }
            }

        root.addView(
            applyAndReturnButton,
            matchWrapParams(),
        )

        root.addView(
            Button(this).apply {
                text = "Revenir au clavier précédent"

                setOnClickListener {
                    returnToPreviousInputMethod()
                }
            },
            matchWrapParams(),
        )

        return root
    }

    override fun onStartInputView(
        info: android.view.inputmethod.EditorInfo?,
        restarting: Boolean,
    ) {
        super.onStartInputView(
            info,
            restarting,
        )

        loadSelectionAndAnalyze()
    }

    private fun loadSelectionAndAnalyze() {
        val selectedText =
            currentInputConnection
                ?.getSelectedText(0)
                ?.toString()
                .orEmpty()

        if (selectedText.isEmpty()) {
            originalSelectedText = ""
            workingText = ""

            selectedTextView.text =
                "Aucune sélection."

            statusView.text =
                "Sélectionnez du texte à corriger."

            issuesLayout.removeAllViews()
            latestIssues = emptyList()
            setApplyButtonsEnabled(false)
            setApplyAllButtonsEnabled(false)
            return
        }

        originalSelectedText = selectedText
        workingText = selectedText

        selectedTextView.text =
            workingText

        setApplyButtonsEnabled(true)

        analyzeWorkingText()
    }

    private fun analyzeWorkingText() {
        val text = workingText
        val generation = ++analysisGeneration

        statusView.text =
            "Analyse en cours..."

        issuesLayout.removeAllViews()
        latestIssues = emptyList()
        setApplyAllButtonsEnabled(false)

        Thread {
            val result =
                runCatching {
                    val currentEngine =
                        engine
                            ?: GrammalecteQuickJsEngine(
                                applicationContext,
                            ).also {
                                engine = it
                            }

                    currentEngine.check(
                        text = text,
                        localeTag = "fr-FR",
                    )
                }

            mainHandler.post {
                if (generation != analysisGeneration) {
                    return@post
                }

                result
                    .onSuccess { issues ->
                        renderIssues(
                            text = text,
                            issues = issues,
                        )
                    }.onFailure { throwable ->
                        statusView.text =
                            "Erreur : ${throwable.message ?: throwable.javaClass.simpleName}"

                        setApplyButtonsEnabled(false)
                    }
            }
        }.start()
    }

    private fun renderIssues(
        text: String,
        issues: List<GrammarIssue>,
    ) {
        selectedTextView.text = text
        issuesLayout.removeAllViews()
        latestIssues = issues

        if (issues.isEmpty()) {
            setApplyAllButtonsEnabled(false)
            statusView.text =
                "Aucune erreur détectée."

            setApplyButtonsEnabled(
                text != originalSelectedText,
            )

            return
        }

        statusView.text =
            "${issues.size} problème(s) détecté(s)."

        setApplyAllButtonsEnabled(
            buildAutomaticCorrection(
                text = text,
                issues = issues,
            ) != text,
        )

        issues.forEach { issue ->
            if (
                issue.start < 0 ||
                issue.endExclusive > text.length ||
                issue.endExclusive <= issue.start
            ) {
                return@forEach
            }

            val faultyText =
                text.substring(
                    issue.start,
                    issue.endExclusive,
                )

            issuesLayout.addView(
                TextView(this).apply {
                    this.text =
                        buildString {
                            append("« ")
                            append(faultyText)
                            append(" »")

                            issue.message
                                ?.takeIf {
                                    it.isNotBlank()
                                }?.let {
                                    append("\n")
                                    append(it)
                                }
                        }

                    textSize = 15f

                    setPadding(
                        0,
                        12,
                        0,
                        4,
                    )
                },
                matchWrapParams(),
            )

            if (issue.suggestions.isEmpty()) {
                issuesLayout.addView(
                    TextView(this).apply {
                        this.text =
                            "Aucune proposition."
                    },
                    matchWrapParams(),
                )
            } else {
                issue.suggestions
                    .take(5)
                    .forEach { suggestion ->
                        issuesLayout.addView(
                            Button(this).apply {
                                this.text =
                                    suggestion

                                setOnClickListener {
                                    applySuggestion(
                                        issue = issue,
                                        suggestion = suggestion,
                                    )
                                }
                            },
                            matchWrapParams(),
                        )
                    }
            }
        }

        setApplyButtonsEnabled(
            workingText != originalSelectedText,
        )
    }

    private fun applySuggestion(
        issue: GrammarIssue,
        suggestion: String,
    ) {
        if (
            issue.start < 0 ||
            issue.endExclusive > workingText.length ||
            issue.endExclusive <= issue.start
        ) {
            analyzeWorkingText()
            return
        }

        workingText =
            workingText.replaceRange(
                issue.start,
                issue.endExclusive,
                suggestion,
            )

        selectedTextView.text =
            workingText

        analyzeWorkingText()
    }

    private fun applyAllSuggestions(returnToPrevious: Boolean) {
        val correctedText =
            buildAutomaticCorrection(
                text = workingText,
                issues = latestIssues,
            )

        if (correctedText == workingText) {
            return
        }

        workingText = correctedText
        selectedTextView.text = workingText

        if (returnToPrevious) {
            applyCorrectedText(
                returnToPrevious = true,
            )
        } else {
            analyzeWorkingText()
        }
    }

    private fun buildAutomaticCorrection(
        text: String,
        issues: List<GrammarIssue>,
    ): String {
        var correctedText = text
        var boundary = text.length

        issues
            .asSequence()
            .filter { issue ->
                issue.start >= 0 &&
                    issue.endExclusive <= text.length &&
                    issue.endExclusive > issue.start &&
                    issue.suggestions.isNotEmpty()
            }.sortedByDescending { issue ->
                issue.start
            }.forEach { issue ->
                if (issue.endExclusive <= boundary) {
                    correctedText =
                        correctedText.replaceRange(
                            issue.start,
                            issue.endExclusive,
                            issue.suggestions.first(),
                        )

                    boundary = issue.start
                }
            }

        return correctedText
    }

    private fun applyCorrectedText(returnToPrevious: Boolean) {
        if (
            workingText.isEmpty() ||
            workingText == originalSelectedText
        ) {
            return
        }

        val inputConnection =
            currentInputConnection
                ?: return

        val currentSelection =
            inputConnection
                .getSelectedText(0)
                ?.toString()
                .orEmpty()

        if (currentSelection != originalSelectedText) {
            statusView.text =
                "La sélection a changé. Sélectionnez de nouveau le texte."

            setApplyButtonsEnabled(false)
            return
        }

        val committed =
            inputConnection.commitText(
                workingText,
                1,
            )

        if (committed) {
            statusView.text =
                "Correction appliquée."

            originalSelectedText = ""
            workingText = ""
            latestIssues = emptyList()
            issuesLayout.removeAllViews()
            setApplyButtonsEnabled(false)
            setApplyAllButtonsEnabled(false)

            if (returnToPrevious) {
                mainHandler.post {
                    returnToPreviousInputMethod()
                }
            }
        } else {
            statusView.text =
                "Impossible d'appliquer la correction."
        }
    }

    private fun setApplyAllButtonsEnabled(enabled: Boolean) {
        applyAllButton.isEnabled = enabled
        applyAllAndReturnButton.isEnabled = enabled
    }

    private fun setApplyButtonsEnabled(enabled: Boolean) {
        applyButton.isEnabled = enabled
        applyAndReturnButton.isEnabled = enabled
    }

    private fun returnToPreviousInputMethod() {
        val switched =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                switchToPreviousInputMethod()
            } else {
                false
            }

        if (!switched) {
            getSystemService(InputMethodManager::class.java)
                .showInputMethodPicker()
        }
    }

    override fun onDestroy() {
        analysisGeneration++

        engine?.close()
        engine = null

        super.onDestroy()
    }

    private fun matchWrapParams(): ViewGroup.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
}
