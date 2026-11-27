package fr.zefut.grammalecte

import android.inputmethodservice.InputMethodService
import android.view.View

/** A Debug/DEV-only IME that never reads or modifies editor text. */
class SuppressingTestImeService : InputMethodService() {
    override fun onCreateInputView(): View =
        View(this).apply {
            minimumHeight = 48
        }
}
