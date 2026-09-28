package fr.grammalecteandroid.unofficial

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
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
    private var engine: GrammalecteQuickJsEngine? = null
    private var analysisGeneration = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.action != Intent.ACTION_PROCESS_TEXT) {
            finish()
            return
        }

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
                text = "Corriger avec Grammalecte"
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

        setContentView(layout)

        analyzeCurrentText()
    }

    private fun analyzeCurrentText() {
        val text = editText.text.toString()
        val generation = ++analysisGeneration

        statusView.text = "Analyse en cours..."
        issuesLayout.removeAllViews()

        Thread {
            val result =
                runCatching {
                    val currentEngine =
                        engine ?: GrammalecteQuickJsEngine(applicationContext).also {
                            engine = it
                        }

                    currentEngine.check(
                        text = text,
                        localeTag = "fr-FR",
                    )
                }

            runOnUiThread {
                if (generation != analysisGeneration) {
                    return@runOnUiThread
                }

                result
                    .onSuccess { issues ->
                        showIssues(text, issues)
                    }.onFailure { error ->
                        statusView.text =
                            "Erreur pendant l'analyse : ${error.message ?: error.javaClass.simpleName}"
                    }
            }
        }.start()
    }

    private fun showIssues(
        analyzedText: String,
        issues: List<GrammarIssue>,
    ) {
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

        statusView.text = "${validIssues.size} erreur(s) détectée(s)."

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
        issue: GrammarIssue,
        suggestion: String,
    ) {
        val current = editText.text

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

        setResult(RESULT_OK, result)
        finish()
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
