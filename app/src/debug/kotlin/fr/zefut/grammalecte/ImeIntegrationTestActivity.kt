package fr.zefut.grammalecte

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.widget.EditText
import java.util.concurrent.atomic.AtomicInteger

class ImeIntegrationTestActivity : Activity() {
    lateinit var editor: ObservedEditText
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        editor =
            ObservedEditText(this).apply {
                inputType =
                    InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE

                textSize = 18f
            }

        setContentView(editor)
    }
}

// Debug-only fixture: counts calls, never records editor contents.
class ObservedEditText(
    context: Context,
) : EditText(context) {
    private val reads = AtomicInteger(0)
    private val commits = AtomicInteger(0)

    val selectedTextReadCount: Int
        get() = reads.get()

    val commitTextCount: Int
        get() = commits.get()

    fun resetImeCounters() {
        reads.set(0)
        commits.set(0)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val delegate = super.onCreateInputConnection(outAttrs) ?: return null

        return object : InputConnectionWrapper(delegate, false) {
            override fun getSelectedText(flags: Int): CharSequence? {
                reads.incrementAndGet()
                return super.getSelectedText(flags)
            }

            override fun commitText(
                text: CharSequence?,
                newCursorPosition: Int,
            ): Boolean {
                commits.incrementAndGet()
                return super.commitText(text, newCursorPosition)
            }
        }
    }
}
