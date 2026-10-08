package fr.grammalecteandroid.unofficial

import android.app.Activity
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import fr.grammalecteandroid.engine.GrammalecteQuickJsEngine
import java.util.concurrent.Executors

class PersonalDictionaryActivity : Activity() {
    private val mainHandler =
        Handler(Looper.getMainLooper())

    private val executor =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(
                runnable,
                "GrammalectePersonalDictionary",
            )
        }

    private lateinit var input: EditText
    private lateinit var addButton: Button
    private lateinit var statusView: TextView
    private lateinit var wordsContainer: LinearLayout

    private var engine: GrammalecteQuickJsEngine? = null

    @Volatile
    private var destroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val density =
            resources.displayMetrics.density

        val padding =
            (20 * density).toInt()

        val spacing =
            (12 * density).toInt()

        val smallSpacing =
            (6 * density).toInt()

        val content =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL

                setPadding(
                    padding,
                    padding,
                    padding,
                    padding,
                )

                setBackgroundColor(
                    getColor(R.color.surface_background),
                )
            }

        val header =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

        header.addView(
            TextView(this).apply {
                text = "Dictionnaire personnel"
                markAsAccessibilityHeading()
                textSize = 24f

                setTypeface(
                    typeface,
                    Typeface.BOLD,
                )

                setTextColor(
                    getColor(R.color.surface_text),
                )
            },
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )

        header.addView(
            Button(this).apply {
                text = "← Retour"
                isAllCaps = false

                setOnClickListener {
                    finish()
                }
            },
        )

        content.addView(
            header,
            matchWrapParams(),
        )

        content.addView(
            TextView(this).apply {
                text =
                    "Ajoutez les mots que Grammalecte doit considérer " +
                    "comme correctement orthographiés. Ils sont utilisés " +
                    "par tous les modes de correction."

                textSize = 14f

                setTextColor(
                    getColor(R.color.surface_text_secondary),
                )

                setPadding(
                    0,
                    smallSpacing,
                    0,
                    spacing,
                )
            },
            matchWrapParams(),
        )

        input =
            EditText(this).apply {
                hint = "Mot à ajouter"
                inputType = InputType.TYPE_CLASS_TEXT
                maxLines = 1
                isSingleLine = true

                setTextColor(
                    getColor(R.color.surface_text),
                )

                setHintTextColor(
                    getColor(R.color.surface_text_secondary),
                )
            }

        content.addView(
            input,
            matchWrapParams(),
        )

        addButton =
            Button(this).apply {
                text = "Ajouter"
                isAllCaps = false

                setOnClickListener {
                    addWord()
                }
            }

        content.addView(
            addButton,
            matchWrapParams(),
        )

        statusView =
            TextView(this).apply {
                text = "Chargement..."
                enablePoliteAccessibilityUpdates()
                textSize = 14f

                setTextColor(
                    getColor(R.color.surface_text_secondary),
                )

                setPadding(
                    0,
                    spacing,
                    0,
                    smallSpacing,
                )
            }

        content.addView(
            statusView,
            matchWrapParams(),
        )

        wordsContainer =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

        content.addView(
            wordsContainer,
            matchWrapParams(),
        )

        val scrollView =
            ScrollView(this).apply {
                isFillViewport = true

                addView(
                    content,
                    matchWrapParams(),
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setOnApplyWindowInsetsListener { view, insets ->
                        val topInset: Int
                        val bottomInset: Int

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val systemBars =
                                insets.getInsets(
                                    WindowInsets.Type.systemBars(),
                                )

                            topInset = systemBars.top
                            bottomInset = systemBars.bottom
                        } else {
                            @Suppress("DEPRECATION")
                            topInset = insets.systemWindowInsetTop

                            @Suppress("DEPRECATION")
                            bottomInset = insets.systemWindowInsetBottom
                        }

                        view.setPadding(
                            0,
                            topInset,
                            0,
                            bottomInset,
                        )

                        insets
                    }
                }
            }

        setContentView(scrollView)

        loadWords()
    }

    private fun loadWords() {
        setControlsEnabled(false)

        statusView.text =
            "Chargement..."

        executor.execute {
            val result =
                runCatching {
                    currentEngine()
                        .personalDictionaryWords()
                }

            mainHandler.post {
                if (destroyed) {
                    return@post
                }

                result
                    .onSuccess { words ->
                        renderWords(words)
                        setControlsEnabled(true)
                    }.onFailure { error ->
                        showError(error)
                    }
            }
        }
    }

    private fun addWord() {
        val word =
            input.text
                .toString()
                .trim()

        if (word.isEmpty()) {
            statusView.text =
                "Saisissez un mot."
            return
        }

        if (word.length > 64) {
            statusView.text =
                "Le mot ne doit pas dépasser 64 caractères."
            return
        }

        if (word.any(Char::isWhitespace)) {
            statusView.text =
                "Ajoutez un seul mot à la fois."
            return
        }

        setControlsEnabled(false)

        statusView.text =
            "Ajout en cours..."

        executor.execute {
            val result =
                runCatching {
                    val currentEngine =
                        currentEngine()

                    val added =
                        currentEngine
                            .addPersonalDictionaryWord(word)

                    added to
                        currentEngine.personalDictionaryWords()
                }

            mainHandler.post {
                if (destroyed) {
                    return@post
                }

                result
                    .onSuccess { (added, words) ->
                        input.text.clear()
                        renderWords(words)
                        setControlsEnabled(true)

                        statusView.text =
                            if (added) {
                                "Mot ajouté."
                            } else {
                                "Ce mot est déjà présent."
                            }
                    }.onFailure { error ->
                        showError(error)
                    }
            }
        }
    }

    private fun removeWord(word: String) {
        setControlsEnabled(false)

        statusView.text =
            "Suppression en cours..."

        executor.execute {
            val result =
                runCatching {
                    val currentEngine =
                        currentEngine()

                    currentEngine
                        .removePersonalDictionaryWord(word)

                    currentEngine.personalDictionaryWords()
                }

            mainHandler.post {
                if (destroyed) {
                    return@post
                }

                result
                    .onSuccess { words ->
                        renderWords(words)
                        setControlsEnabled(true)

                        statusView.text =
                            "Mot supprimé."
                    }.onFailure { error ->
                        showError(error)
                    }
            }
        }
    }

    private fun renderWords(words: List<String>) {
        wordsContainer.removeAllViews()

        if (words.isEmpty()) {
            statusView.text =
                "Aucun mot personnel."

            return
        }

        statusView.text =
            if (words.size == 1) {
                "1 mot personnel."
            } else {
                "${words.size} mots personnels."
            }

        words.forEach { word ->
            addWordRow(word)
        }
    }

    private fun addWordRow(word: String) {
        val density =
            resources.displayMetrics.density

        val padding =
            (12 * density).toInt()

        val spacing =
            (8 * density).toInt()

        val row =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                setPadding(
                    padding,
                    padding,
                    padding,
                    padding,
                )

                background =
                    roundedBackground(
                        radiusDp = 14f,
                    )
            }

        row.addView(
            TextView(this).apply {
                text = word
                textSize = 16f

                setTextColor(
                    getColor(R.color.surface_text),
                )
            },
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )

        row.addView(
            Button(this).apply {
                text = "Supprimer"
                isAllCaps = false

                setOnClickListener {
                    removeWord(word)
                }
            },
        )

        wordsContainer.addView(
            row,
            LinearLayout
                .LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = spacing
                },
        )
    }

    private fun setControlsEnabled(enabled: Boolean) {
        input.isEnabled = enabled
        addButton.isEnabled = enabled

        for (index in 0 until wordsContainer.childCount) {
            wordsContainer
                .getChildAt(index)
                .isEnabled = enabled
        }
    }

    private fun currentEngine(): GrammalecteQuickJsEngine =
        engine
            ?: GrammalecteQuickJsEngine(
                applicationContext,
            ).also {
                engine = it
            }

    private fun showError(error: Throwable) {
        statusView.text =
            "Erreur : " +
            (
                error.message
                    ?: error.javaClass.simpleName
            )

        setControlsEnabled(true)
    }

    private fun roundedBackground(radiusDp: Float): GradientDrawable =
        GradientDrawable().apply {
            shape =
                GradientDrawable.RECTANGLE

            cornerRadius =
                radiusDp *
                resources.displayMetrics.density

            setColor(
                getColor(R.color.surface_card),
            )
        }

    override fun onDestroy() {
        destroyed = true

        executor.execute {
            engine?.close()
            engine = null
        }

        executor.shutdown()

        super.onDestroy()
    }

    private fun matchWrapParams(): ViewGroup.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
}
