package fr.zefut.grammalecte

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import fr.grammalecteandroid.core.GrammarIssue
import fr.grammalecteandroid.engine.GrammalecteQuickJsEngine

class ProcessTextActivity : Activity() {
    private lateinit var editText: EditText
    private lateinit var statusView: TextView
    private lateinit var issuesLayout: LinearLayout

    private var readOnly = false
    private lateinit var analysisCoordinator: ProcessTextAnalysisCoordinator
    private var textRevision = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.action != Intent.ACTION_PROCESS_TEXT) {
            finish()
            return
        }

        analysisCoordinator =
            ProcessTextAnalysisCoordinator(
                engineFactory = {
                    GrammalecteQuickJsEngine(applicationContext)
                },
                callbackDispatcher = { callback ->
                    runOnUiThread { callback() }
                },
            )

        val selectedText =
            intent
                .getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
                ?.toString()
                .orEmpty()

        readOnly =
            intent.getBooleanExtra(
                Intent.EXTRA_PROCESS_TEXT_READONLY,
                false,
            )

        traceProcessText(
            phase = "opened",
            textLength = selectedText.length,
        )

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
                text = getString(R.string.process_text_label)
                markAsAccessibilityHeading()
                textSize = 24f
            },
            matchWrapParams(),
        )

        editText =
            EditText(this).apply {
                setText(selectedText)
                textSize = 18f
                minLines = 5
                gravity = Gravity.TOP
                setPadding(0, spacing, 0, spacing)
            }

        layout.addView(editText, matchWrapParams())

        layout.addView(
            Button(this).apply {
                text = "Analyser"
                setOnClickListener {
                    analyzeCurrentText()
                }
            },
            matchWrapParams(),
        )

        statusView =
            TextView(this).apply {
                text = "Prêt."
                enablePoliteAccessibilityUpdates()
                textSize = 16f
                setPadding(0, spacing, 0, spacing)
            }

        layout.addView(statusView, matchWrapParams())

        issuesLayout =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

        layout.addView(issuesLayout, matchWrapParams())

        layout.addView(
            Button(this).apply {
                text =
                    if (readOnly) {
                        "Copier et revenir"
                    } else {
                        "Remplacer le texte sélectionné"
                    }

                setOnClickListener {
                    if (readOnly) {
                        copyCorrectedText()
                    } else {
                        returnCorrectedText()
                    }
                }
            },
            matchWrapParams(),
        )

        // Some third-party editors ignore or incorrectly apply the result.
        // Offer a non-replacing clipboard path even for editable selections.
        if (!readOnly) {
            layout.addView(
                Button(this).apply {
                    text = "Copier et revenir"
                    setOnClickListener {
                        copyCorrectedText()
                    }
                },
                matchWrapParams(),
            )
        }

        setContentView(layout)

        editText.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    text: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) = Unit

                override fun onTextChanged(
                    text: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int,
                ) = Unit

                override fun afterTextChanged(text: Editable?) {
                    textRevision++
                    issuesLayout.removeAllViews()
                    statusView.text = "Texte modifié. Relancez l’analyse."
                }
            },
        )

        analyzeCurrentText()
    }

    private fun analyzeCurrentText() {
        val text = editText.text.toString()
        val revision = textRevision

        statusView.text = "Analyse en cours…"
        issuesLayout.removeAllViews()

        analysisCoordinator.analyze(text) { result ->
            if (textRevision != revision || editText.text.toString() != text) {
                return@analyze
            }

            result
                .onSuccess { issues ->
                    showIssues(text, issues)
                }.onFailure { error ->
                    statusView.text =
                        "Erreur pendant l’analyse : ${error.message ?: error.javaClass.simpleName}"
                }
        }
    }

    private fun showIssues(
        analyzedText: String,
        issues: List<GrammarIssue>,
    ) {
        val analyzedRevision = textRevision
        issuesLayout.removeAllViews()

        val validIssues =
            issues.filter { issue ->
                issue.start >= 0 &&
                    issue.endExclusive > issue.start &&
                    issue.endExclusive <= analyzedText.length
            }

        if (validIssues.isEmpty()) {
            statusView.text = "Aucune erreur détectée."
            return
        }

        statusView.text =
            resources.getQuantityString(
                R.plurals.detected_problems,
                validIssues.size,
                validIssues.size,
            )

        validIssues.forEach { issue ->
            val fragment =
                analyzedText.substring(
                    issue.start,
                    issue.endExclusive,
                )

            issuesLayout.addView(
                TextView(this).apply {
                    text =
                        buildString {
                            append("« ")
                            append(fragment)
                            append(" »")

                            issue.message?.takeIf { it.isNotBlank() }?.let {
                                append("\n")
                                append(it)
                            }
                        }
                    textSize = 16f
                    setPadding(0, 12, 0, 6)
                },
                matchWrapParams(),
            )

            if (issue.suggestions.isEmpty()) {
                issuesLayout.addView(
                    TextView(this).apply {
                        text = "Aucune proposition."
                    },
                    matchWrapParams(),
                )
            } else {
                issue.suggestions.take(5).forEach { suggestion ->
                    issuesLayout.addView(
                        Button(this).apply {
                            text = suggestion
                            setOnClickListener {
                                applySuggestion(
                                    analyzedText = analyzedText,
                                    analyzedRevision = analyzedRevision,
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
    }

    private fun applySuggestion(
        analyzedText: String,
        analyzedRevision: Long,
        issue: GrammarIssue,
        suggestion: String,
    ) {
        val current = editText.text

        if (
            analyzedRevision != textRevision ||
            current.toString() != analyzedText
        ) {
            issuesLayout.removeAllViews()
            statusView.text = "Texte modifié. Relancez l’analyse."
            return
        }

        if (
            issue.start < 0 ||
            issue.endExclusive > current.length ||
            issue.endExclusive <= issue.start
        ) {
            analyzeCurrentText()
            return
        }

        current.replace(
            issue.start,
            issue.endExclusive,
            suggestion,
        )

        editText.setSelection(
            (issue.start + suggestion.length)
                .coerceAtMost(current.length),
        )

        analyzeCurrentText()
    }

    private fun copyCorrectedText() {
        val clipboard = getSystemService(ClipboardManager::class.java)

        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "Texte corrigé",
                editText.text.toString(),
            ),
        )

        Toast
            .makeText(
                this,
                "Texte corrigé copié.",
                Toast.LENGTH_SHORT,
            ).show()

        traceProcessText(
            phase = "clipboard_copied",
            textLength = editText.length(),
        )

        finish()
    }

    private fun returnCorrectedText() {
        val result =
            Intent().apply {
                putExtra(
                    Intent.EXTRA_PROCESS_TEXT,
                    editText.text.toString(),
                )
            }

        traceProcessText(
            phase = "result_ok",
            textLength = editText.length(),
        )

        setResult(RESULT_OK, result)
        finish()
    }

    // Opt-in, debug-only contract diagnostics. Never log selected or corrected text.
    private fun traceProcessText(
        phase: String,
        textLength: Int,
    ) {
        if (
            (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) == 0 ||
            !Log.isLoggable(PROCESS_TEXT_TRACE_TAG, Log.DEBUG)
        ) {
            return
        }

        Log.d(
            PROCESS_TEXT_TRACE_TAG,
            "phase=$phase readOnly=$readOnly " +
                "readOnlyExtraPresent=${intent.hasExtra(Intent.EXTRA_PROCESS_TEXT_READONLY)} " +
                "textLength=$textLength callingActivityPresent=${callingActivity != null}",
        )
    }

    override fun onDestroy() {
        if (::analysisCoordinator.isInitialized) {
            analysisCoordinator.close()
        }
        super.onDestroy()
    }

    private fun matchWrapParams(): ViewGroup.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )

    private companion object {
        const val PROCESS_TEXT_TRACE_TAG = "GrammalectePT"
    }
}
