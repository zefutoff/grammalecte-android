package fr.grammalecteandroid.unofficial

import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import fr.grammalecteandroid.core.GrammarIssue
import fr.grammalecteandroid.engine.GrammalecteQuickJsEngine
import java.util.concurrent.Executors

class GrammalecteImeService : InputMethodService() {
    private val mainHandler = Handler(Looper.getMainLooper())

    private val selectionRefreshRunnable =
        Runnable {
            if (::selectedTextView.isInitialized) {
                requestSelectionAndAnalyze()
            }
        }

    private val selectionExecutor =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "GrammalecteImeSelection")
        }

    private val analysisExecutor =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "GrammalecteImeAnalysis")
        }
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

    private var latestSelectionStart = -1
    private var latestSelectionEnd = -1
    private var analyzedSelectionStart = -1
    private var analyzedSelectionEnd = -1

    // The engine is created, used and closed only on analysisExecutor.
    private var engine: GrammalecteQuickJsEngine? = null

    @Volatile
    private var selectionRequestGeneration = 0

    @Volatile
    private var analysisGeneration = 0

    override fun onCreateInputView(): View {
        val density = resources.displayMetrics.density
        val padding = (12 * density).toInt()
        val smallSpacing = (6 * density).toInt()
        val spacing = (10 * density).toInt()

        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    padding,
                    smallSpacing,
                    padding,
                    padding,
                )

                setBackgroundColor(
                    getColor(R.color.surface_background),
                )
            }

        val header =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

        header.addView(
            TextView(this).apply {
                text = "Grammalecte"
                textSize = 20f
                setTextColor(
                    getColor(R.color.surface_text),
                )

                setTypeface(
                    typeface,
                    android.graphics.Typeface.BOLD,
                )
            },
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )

        header.addView(
            Button(this).apply {
                text = "← Retour clavier"
                isAllCaps = false
                minHeight = 0
                minimumHeight = 0

                setOnClickListener {
                    returnToPreviousInputMethod()
                }
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        root.addView(
            header,
            matchWrapParams(),
        )

        selectedTextView =
            TextView(this).apply {
                text = "Aucune sélection."
                textSize = 15f
                maxLines = 3
                setTextColor(
                    getColor(R.color.surface_text),
                )

                setPadding(
                    padding,
                    spacing,
                    padding,
                    spacing,
                )

                background =
                    roundedBackground(
                        radiusDp = 12f,
                    )
            }

        root.addView(
            selectedTextView,
            LinearLayout
                .LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin = smallSpacing
                },
        )

        statusView =
            TextView(this).apply {
                text = "Sélectionnez du texte à corriger."
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
            }

        root.addView(
            statusView,
            matchWrapParams(),
        )

        val scrollView =
            ScrollView(this).apply {
                isFillViewport = false
            }

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

        val automaticActions =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

        applyAllButton =
            Button(this).apply {
                text = "Tout corriger"
                isAllCaps = false
                visibility = View.GONE

                setOnClickListener {
                    applyAllSuggestions(
                        returnToPrevious = false,
                    )
                }
            }

        automaticActions.addView(
            applyAllButton,
            weightedButtonParams(),
        )

        applyAllAndReturnButton =
            Button(this).apply {
                text = "Corriger tout et revenir"
                isAllCaps = false
                visibility = View.GONE

                setOnClickListener {
                    applyAllSuggestions(
                        returnToPrevious = true,
                    )
                }
            }

        automaticActions.addView(
            applyAllAndReturnButton,
            weightedButtonParams(),
        )

        root.addView(
            automaticActions,
            matchWrapParams(),
        )

        val manualActions =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

        applyButton =
            Button(this).apply {
                text = "Appliquer"
                isAllCaps = false
                visibility = View.GONE

                setOnClickListener {
                    applyCorrectedText(
                        returnToPrevious = false,
                    )
                }
            }

        manualActions.addView(
            applyButton,
            weightedButtonParams(),
        )

        applyAndReturnButton =
            Button(this).apply {
                text = "Appliquer et revenir"
                isAllCaps = false
                visibility = View.GONE

                setOnClickListener {
                    applyCorrectedText(
                        returnToPrevious = true,
                    )
                }
            }

        manualActions.addView(
            applyAndReturnButton,
            weightedButtonParams(),
        )

        root.addView(
            manualActions,
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

        mainHandler.removeCallbacks(
            selectionRefreshRunnable,
        )

        selectionRequestGeneration++

        updateLatestSelection(
            info?.initialSelStart ?: -1,
            info?.initialSelEnd ?: -1,
        )

        val initialSelectedText =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                info
                    ?.getInitialSelectedText(0)
                    ?.toString()
            } else {
                null
            }

        if (initialSelectedText != null) {
            applySelectedTextAndAnalyze(
                initialSelectedText,
            )
        } else {
            clearSelectionState()
            requestSelectionAndAnalyze()
        }
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int,
    ) {
        super.onUpdateSelection(
            oldSelStart,
            oldSelEnd,
            newSelStart,
            newSelEnd,
            candidatesStart,
            candidatesEnd,
        )

        if (
            oldSelStart == newSelStart &&
            oldSelEnd == newSelEnd
        ) {
            return
        }

        selectionRequestGeneration++

        updateLatestSelection(
            newSelStart,
            newSelEnd,
        )

        mainHandler.removeCallbacks(
            selectionRefreshRunnable,
        )

        if (
            newSelStart < 0 ||
            newSelEnd < 0 ||
            newSelStart == newSelEnd
        ) {
            if (::selectedTextView.isInitialized) {
                clearSelectionState()
            }

            return
        }

        mainHandler.postDelayed(
            selectionRefreshRunnable,
            SELECTION_REFRESH_DELAY_MILLIS,
        )
    }

    private fun requestSelectionAndAnalyze() {
        val inputConnection =
            currentInputConnection
                ?: run {
                    clearSelectionState()
                    return
                }

        val requestGeneration =
            ++selectionRequestGeneration

        selectionExecutor.execute {
            val selectedText =
                runCatching {
                    inputConnection
                        .getSelectedText(0)
                        ?.toString()
                        .orEmpty()
                }.getOrDefault("")

            mainHandler.post {
                if (
                    requestGeneration != selectionRequestGeneration ||
                    !::selectedTextView.isInitialized
                ) {
                    return@post
                }

                applySelectedTextAndAnalyze(
                    selectedText,
                )
            }
        }
    }

    private fun applySelectedTextAndAnalyze(selectedText: String) {
        if (selectedText.isEmpty()) {
            clearSelectionState()
            return
        }

        analyzedSelectionStart = latestSelectionStart
        analyzedSelectionEnd = latestSelectionEnd

        originalSelectedText = selectedText
        workingText = selectedText

        selectedTextView.text =
            workingText

        setApplyButtonsEnabled(true)

        analyzeWorkingText()
    }

    private fun clearSelectionState() {
        analysisGeneration++

        originalSelectedText = ""
        workingText = ""
        analyzedSelectionStart = -1
        analyzedSelectionEnd = -1

        selectedTextView.text =
            "Aucune sélection."

        statusView.text =
            "Sélectionnez du texte à corriger."

        issuesLayout.removeAllViews()
        latestIssues = emptyList()
        setApplyButtonsEnabled(false)
        setApplyAllButtonsEnabled(false)
    }

    private fun updateLatestSelection(
        start: Int,
        end: Int,
    ) {
        if (start < 0 || end < 0) {
            latestSelectionStart = -1
            latestSelectionEnd = -1
            return
        }

        latestSelectionStart = minOf(start, end)
        latestSelectionEnd = maxOf(start, end)
    }

    private fun analyzeWorkingText() {
        val text = workingText
        val generation = ++analysisGeneration

        statusView.text =
            "Analyse en cours..."

        issuesLayout.removeAllViews()
        latestIssues = emptyList()
        setApplyAllButtonsEnabled(false)

        analysisExecutor.execute {
            if (generation != analysisGeneration) {
                return@execute
            }

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
        }
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

        val density =
            resources.displayMetrics.density

        val cardPadding =
            (10 * density).toInt()

        val cardSpacing =
            (8 * density).toInt()

        val chipSpacing =
            (6 * density).toInt()

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

            val card =
                LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL

                    setPadding(
                        cardPadding,
                        cardPadding,
                        cardPadding,
                        cardPadding,
                    )

                    background =
                        roundedBackground(
                            radiusDp = 14f,
                        )
                }

            card.addView(
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
                    setTextColor(
                        getColor(R.color.surface_text),
                    )
                },
                matchWrapParams(),
            )

            if (issue.suggestions.isEmpty()) {
                card.addView(
                    TextView(this).apply {
                        this.text =
                            "Aucune proposition."

                        setTextColor(
                            getColor(R.color.surface_text_secondary),
                        )

                        setPadding(
                            0,
                            cardSpacing,
                            0,
                            0,
                        )
                    },
                    matchWrapParams(),
                )
            } else {
                val horizontalScroll =
                    HorizontalScrollView(this).apply {
                        isHorizontalScrollBarEnabled = false

                        setPadding(
                            0,
                            cardSpacing,
                            0,
                            0,
                        )
                    }

                val suggestionsRow =
                    LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                    }

                issue.suggestions
                    .take(5)
                    .forEach { suggestion ->
                        suggestionsRow.addView(
                            Button(this).apply {
                                this.text = suggestion
                                isAllCaps = false

                                minWidth = 0
                                minimumWidth = 0
                                minHeight = 0
                                minimumHeight = 0

                                setPadding(
                                    (16 * density).toInt(),
                                    (5 * density).toInt(),
                                    (16 * density).toInt(),
                                    (5 * density).toInt(),
                                )

                                setOnClickListener {
                                    applySuggestion(
                                        issue = issue,
                                        suggestion = suggestion,
                                    )
                                }
                            },
                            LinearLayout
                                .LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                ).apply {
                                    marginEnd = chipSpacing
                                },
                        )
                    }

                horizontalScroll.addView(
                    suggestionsRow,
                    matchWrapParams(),
                )

                card.addView(
                    horizontalScroll,
                    matchWrapParams(),
                )
            }

            issuesLayout.addView(
                card,
                LinearLayout
                    .LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        bottomMargin = cardSpacing
                    },
            )
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

        if (
            analyzedSelectionStart < 0 ||
            analyzedSelectionEnd <= analyzedSelectionStart
        ) {
            statusView.text =
                "La sélection n'est plus disponible."

            setApplyButtonsEnabled(false)
            return
        }

        if (
            latestSelectionStart != analyzedSelectionStart ||
            latestSelectionEnd != analyzedSelectionEnd
        ) {
            statusView.text =
                "La sélection a changé. Sélectionnez de nouveau le texte."

            setApplyButtonsEnabled(false)
            return
        }

        val selectionRestored =
            inputConnection.setSelection(
                analyzedSelectionStart,
                analyzedSelectionEnd,
            )

        if (!selectionRestored) {
            statusView.text =
                "Impossible de restaurer la sélection."

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
        val visibility =
            if (enabled) {
                View.VISIBLE
            } else {
                View.GONE
            }

        applyAllButton.isEnabled = enabled
        applyAllButton.visibility = visibility

        applyAllAndReturnButton.isEnabled = enabled
        applyAllAndReturnButton.visibility = visibility
    }

    private fun setApplyButtonsEnabled(enabled: Boolean) {
        val visibility =
            if (enabled) {
                View.VISIBLE
            } else {
                View.GONE
            }

        applyButton.isEnabled = enabled
        applyButton.visibility = visibility

        applyAndReturnButton.isEnabled = enabled
        applyAndReturnButton.visibility = visibility
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
        mainHandler.removeCallbacks(
            selectionRefreshRunnable,
        )

        selectionRequestGeneration++
        analysisGeneration++

        selectionExecutor.shutdown()

        analysisExecutor.execute {
            engine?.close()
            engine = null
        }
        analysisExecutor.shutdown()

        super.onDestroy()
    }

    private fun themeColor(attribute: Int): Int {
        val value = TypedValue()

        check(
            theme.resolveAttribute(
                attribute,
                value,
                true,
            ),
        )

        return if (value.resourceId != 0) {
            getColor(value.resourceId)
        } else {
            value.data
        }
    }

    private fun roundedBackground(radiusDp: Float): GradientDrawable =
        GradientDrawable().apply {
            shape =
                GradientDrawable.RECTANGLE

            cornerRadius =
                radiusDp *
                resources.displayMetrics.density

            setColor(
                getColor(R.color.surface_card),
            )
        }

    private companion object {
        const val SELECTION_REFRESH_DELAY_MILLIS = 150L
    }

    private fun weightedButtonParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f,
        )

    private fun matchWrapParams(): ViewGroup.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
}
