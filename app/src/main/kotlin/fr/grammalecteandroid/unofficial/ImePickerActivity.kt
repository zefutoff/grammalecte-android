package fr.grammalecteandroid.unofficial

import android.app.Activity
import android.os.Bundle
import android.view.inputmethod.InputMethodManager

class ImePickerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getSystemService(InputMethodManager::class.java)
            .showInputMethodPicker()

        finish()
    }
}
