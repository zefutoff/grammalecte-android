package fr.zefut.grammalecte

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import fr.grammalecteandroid.core.GrammarIssue
import fr.grammalecteandroid.engine.GrammalecteQuickJsEngine

class ProcessTextActivity : ComponentActivity() {
    private var readOnly = false
    private lateinit var analysisCoordinator: ProcessTextAnalysisCoordinator
    private var textRevision = 0L

    private var editorValue by mutableStateOf(TextFieldValue())
    private var visibleIssues by mutableStateOf<List<ProcessTextIssuePresentation>>(emptyList())
    private var status by mutableStateOf("Prêt.")
    private var statusIsError by mutableStateOf(false)
    private var analyzing by mutableStateOf(false)

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

        val restoredText = savedInstanceState?.getString(STATE_EDITOR_TEXT)
        val initialText = restoredText ?: selectedText
        val start =
            (savedInstanceState?.getInt(STATE_SELECTION_START) ?: 0)
                .coerceIn(0, initialText.length)
        val end =
            (savedInstanceState?.getInt(STATE_SELECTION_END) ?: start)
                .coerceIn(0, initialText.length)
        editorValue = TextFieldValue(initialText, selection = TextRange(start, end))

        traceProcessText(
            phase = "opened",
            textLength = selectedText.length,
        )

        setContent {
            ProcessTextScreen(
                text = editorValue,
                readOnlySelection = readOnly,
                status = status,
                statusIsError = statusIsError,
                analyzing = analyzing,
                issues = visibleIssues,
                onBack = { finish() },
                onTextChange = { updateEditor(it) },
                onAnalyze = { analyzeCurrentText() },
                onSuggestion = { entry, suggestion -> applySuggestion(entry, suggestion) },
                onCopy = { copyCorrectedText() },
                onReplace = { returnCorrectedText() },
            )
        }

        analyzeCurrentText()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_EDITOR_TEXT, editorValue.text)
        outState.putInt(STATE_SELECTION_START, editorValue.selection.start)
        outState.putInt(STATE_SELECTION_END, editorValue.selection.end)
        super.onSaveInstanceState(outState)
    }

    private fun updateEditor(value: TextFieldValue) {
        val textChanged = value.text != editorValue.text
        editorValue = value
        if (textChanged) {
            textRevision++
            visibleIssues = emptyList()
            status = "Texte modifié. Relancez l’analyse."
            statusIsError = false
            analyzing = false
        }
    }

    private fun analyzeCurrentText() {
        val text = editorValue.text
        val revision = textRevision

        status = "Analyse en cours…"
        statusIsError = false
        analyzing = true
        visibleIssues = emptyList()

        analysisCoordinator.analyze(text) { result ->
            if (textRevision != revision || editorValue.text != text) {
                return@analyze
            }

            analyzing = false
            result
                .onSuccess { issues ->
                    showIssues(text, revision, issues)
                }.onFailure { error ->
                    statusIsError = true
                    status =
                        "Erreur pendant l’analyse : ${error.message ?: error.javaClass.simpleName}"
                }
        }
    }

    private fun showIssues(
        analyzedText: String,
        analyzedRevision: Long,
        issues: List<GrammarIssue>,
    ) {
        val validIssues =
            issues.filter { issue ->
                issue.start >= 0 &&
                    issue.endExclusive > issue.start &&
                    issue.endExclusive <= analyzedText.length
            }

        visibleIssues =
            validIssues.map { issue ->
                ProcessTextIssuePresentation(
                    analyzedText = analyzedText,
                    analyzedRevision = analyzedRevision,
                    issue = issue,
                    fragment = analyzedText.substring(issue.start, issue.endExclusive),
                )
            }

        statusIsError = false
        status =
            if (validIssues.isEmpty()) {
                "Aucune erreur détectée."
            } else {
                resources.getQuantityString(
                    R.plurals.detected_problems,
                    validIssues.size,
                    validIssues.size,
                )
            }
    }

    private fun applySuggestion(
        entry: ProcessTextIssuePresentation,
        suggestion: String,
    ) {
        val currentText = editorValue.text
        if (
            entry.analyzedRevision != textRevision ||
            currentText != entry.analyzedText
        ) {
            visibleIssues = emptyList()
            status = "Texte modifié. Relancez l’analyse."
            statusIsError = false
            return
        }

        val issue = entry.issue
        if (
            issue.start < 0 ||
            issue.endExclusive > currentText.length ||
            issue.endExclusive <= issue.start
        ) {
            analyzeCurrentText()
            return
        }

        val correctedText =
            currentText.replaceRange(
                issue.start,
                issue.endExclusive,
                suggestion,
            )
        val newSelection = (issue.start + suggestion.length).coerceAtMost(correctedText.length)
        updateEditor(
            TextFieldValue(
                text = correctedText,
                selection = TextRange(newSelection),
            ),
        )
        analyzeCurrentText()
    }

    private fun copyCorrectedText() {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "Texte corrigé",
                editorValue.text,
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
            textLength = editorValue.text.length,
        )

        finish()
    }

    private fun returnCorrectedText() {
        val result =
            Intent().apply {
                putExtra(
                    Intent.EXTRA_PROCESS_TEXT,
                    editorValue.text,
                )
            }

        traceProcessText(
            phase = "result_ok",
            textLength = editorValue.text.length,
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

    private companion object {
        const val PROCESS_TEXT_TRACE_TAG = "GrammalectePT"
        const val STATE_EDITOR_TEXT = "process_text_editor"
        const val STATE_SELECTION_START = "process_text_selection_start"
        const val STATE_SELECTION_END = "process_text_selection_end"
    }
}
