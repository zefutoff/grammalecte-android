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

    enum class HostMode {
        REPLACE,
        APPEND,
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
        val start = minOf(editor.selectionStart, editor.selectionEnd)
        val end = maxOf(editor.selectionStart, editor.selectionEnd)

        when (mode) {
            HostMode.REPLACE -> {
                if (start >= 0 && end > start) {
                    editor.text.replace(start, end, corrected)
                }
            }
            HostMode.APPEND -> editor.text.append(corrected)
            HostMode.IGNORE -> Unit
        }
    }

    private companion object {
        const val REQUEST_CODE = 45
    }
}
