package fr.zefut.grammalecte

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.EditText

/**
 * Debug-only host to model distinct editor responses to ACTION_PROCESS_TEXT.
 * This does not represent Samsung Messages implementation.
 */
class ProcessTextHostTestActivity : Activity() {
    lateinit var editor: EditText
        private set

    var mode: HostMode = HostMode.REPLACE
    var lastResultCode: Int? = null
        private set
    var returnedText: String? = null
        private set

    private var requestedStart = -1
    private var requestedEnd = -1
    private var requestedText = ""

    enum class HostMode {
        REPLACE,
        APPEND,
        INSERT_AT_SELECTION_END,
        IGNORE,
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        editor = EditText(this)
        setContentView(editor)
    }

    fun startCorrection() {
        val selectionStart = editor.selectionStart
        val selectionEnd = editor.selectionEnd
        val start = minOf(selectionStart, selectionEnd)
        val end = maxOf(selectionStart, selectionEnd)

        require(start >= 0 && end > start) {
            "Select editable text before starting PROCESS_TEXT"
        }

        val selected = editor.text.substring(start, end)
        requestedStart = start
        requestedEnd = end
        requestedText = selected

        val intent =
            Intent(Intent.ACTION_PROCESS_TEXT).apply {
                setClass(
                    this@ProcessTextHostTestActivity,
                    ProcessTextActivity::class.java,
                )
                type = "text/plain"
                putExtra(Intent.EXTRA_PROCESS_TEXT, selected)
                putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)
            }

        @Suppress("DEPRECATION")
        startActivityForResult(intent, REQUEST_CODE)
    }

    @Deprecated("Used only by the debug fixture to simulate the host result callback")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != REQUEST_CODE) {
            return
        }

        lastResultCode = resultCode
        returnedText =
            data
                ?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
                ?.toString()

        if (resultCode != RESULT_OK) {
            return
        }

        val corrected = returnedText ?: return

        when (mode) {
            HostMode.REPLACE -> {
                val start = requestedStart
                val end = requestedEnd

                // A correct editor applies the result to the original selection,
                // not to the cursor position left after a focus change.
                if (
                    start >= 0 &&
                    end > start &&
                    end <= editor.text.length &&
                    editor.text.substring(start, end) == requestedText
                ) {
                    editor.text.replace(start, end, corrected)
                }
            }
            HostMode.APPEND -> editor.text.append(corrected)
            HostMode.INSERT_AT_SELECTION_END -> {
                // Intentionally incorrect host behavior, matching the observed
                // insertion position in Samsung Messages. This fixture does
                // not represent Samsung's unknown implementation.
                if (requestedEnd in 0..editor.text.length) {
                    editor.text.insert(requestedEnd, corrected)
                }
            }
            HostMode.IGNORE -> Unit
        }
    }

    private companion object {
        const val REQUEST_CODE = 45
    }
}
