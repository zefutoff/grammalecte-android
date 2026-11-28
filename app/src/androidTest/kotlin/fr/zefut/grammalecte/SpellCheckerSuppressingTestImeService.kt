package fr.zefut.grammalecte

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.TextView

// This input method is packaged only in the instrumentation APK.
class SpellCheckerSuppressingTestImeService : InputMethodService() {
    override fun onCreateInputView(): View =
        TextView(this).apply {
            text = "Spellchecker suppression test IME"
        }
}
