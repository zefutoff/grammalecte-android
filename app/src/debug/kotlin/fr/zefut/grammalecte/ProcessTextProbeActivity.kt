package fr.zefut.grammalecte

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Debug-only independent PROCESS_TEXT provider for a synthetic fixture.
 *
 * It has no grammar engine, clipboard, IME, networking or persistent storage.
 * Unrecognized selections and read-only editors cannot produce a result.
 */
class ProcessTextProbeActivity : Activity() {
    private var replacement: String? = null
    private var readOnly = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        readOnly =
            intent.getBooleanExtra(
                Intent.EXTRA_PROCESS_TEXT_READONLY,
                true,
            )

        val selectedText =
            if (intent.action == Intent.ACTION_PROCESS_TEXT && intent.type == "text/plain") {
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            } else {
                null
            }

        replacement =
            if (readOnly) {
                null
            } else {
                when (selectedText) {
                    "Je vais au magazin." -> "Je vais au magasin."
                    "je vais au magazin." -> "je vais au magasin."
                    else -> null
                }
            }

        trace("opened")

        val padding = (24 * resources.displayMetrics.density).toInt()
        val layout =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(padding, padding, padding, padding)
            }

        layout.addView(
            TextView(this).apply {
                text =
                    if (replacement != null) {
                        getString(R.string.process_text_probe_supported)
                    } else {
                        getString(R.string.process_text_probe_unsupported)
                    }
                textSize = 18f
            },
            matchWrapParams(),
        )

        layout.addView(
            Button(this).apply {
                text = getString(R.string.process_text_probe_return)
                isEnabled = replacement != null
                setOnClickListener {
                    val corrected = replacement ?: return@setOnClickListener

                    val result =
                        Intent().apply {
                            putExtra(Intent.EXTRA_PROCESS_TEXT, corrected)
                        }

                    trace("result_ok")
                    setResult(RESULT_OK, result)
                    finish()
                }
            },
            matchWrapParams(),
        )

        layout.addView(
            Button(this).apply {
                text = getString(R.string.process_text_probe_cancel)
                setOnClickListener {
                    trace("cancelled")
                    finish()
                }
            },
            matchWrapParams(),
        )

        setContentView(layout)
    }

    private fun trace(phase: String) {
        if (!Log.isLoggable(TRACE_TAG, Log.DEBUG)) {
            return
        }

        Log.d(
            TRACE_TAG,
            "phase=$phase fixtureAccepted=${replacement != null} " +
                "readOnly=$readOnly callingActivityPresent=${callingActivity != null}",
        )
    }

    private fun matchWrapParams(): ViewGroup.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )

    private companion object {
        const val TRACE_TAG = "GrammalecteProbe"
    }
}
