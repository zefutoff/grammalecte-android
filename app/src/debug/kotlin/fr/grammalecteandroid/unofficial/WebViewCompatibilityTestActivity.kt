package fr.grammalecteandroid.unofficial

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView

class WebViewCompatibilityTestActivity : Activity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView =
            WebView(this).apply {
                settings.javaScriptEnabled = false

                loadDataWithBaseURL(
                    "https://compatibility.invalid/",
                    HTML,
                    "text/html",
                    "UTF-8",
                    null,
                )
            }

        setContentView(webView)
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    private companion object {
        val HTML =
            """
            <!doctype html>
            <html lang="fr">
            <head>
                <meta charset="utf-8">
                <meta
                    name="viewport"
                    content="width=device-width, initial-scale=1"
                >
                <style>
                    body {
                        box-sizing: border-box;
                        margin: 0;
                        padding: 24px;
                        font-family: sans-serif;
                    }

                    textarea {
                        box-sizing: border-box;
                        width: 100%;
                        min-height: 240px;
                        padding: 16px;
                        font-size: 20px;
                        line-height: 1.5;
                    }
                </style>
            </head>
            <body>
                <textarea
                    lang="fr"
                    spellcheck="true"
                >Je vais au magazin.</textarea>
            </body>
            </html>
            """.trimIndent()
    }
}
