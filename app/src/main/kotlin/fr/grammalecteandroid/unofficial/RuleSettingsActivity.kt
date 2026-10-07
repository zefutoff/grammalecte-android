package fr.grammalecteandroid.unofficial

import android.app.Activity
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import fr.grammalecteandroid.engine.GrammalecteQuickJsEngine
import fr.grammalecteandroid.engine.GrammalecteRuleOption
import java.util.concurrent.Executors

class RuleSettingsActivity : Activity() {
    private val mainHandler =
        Handler(Looper.getMainLooper())

    private val executor =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(
                runnable,
                "GrammalecteRuleSettings",
            )
        }

    private lateinit var statusView: TextView
    private lateinit var optionsContainer: LinearLayout
    private lateinit var resetButton: Button

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
                text = "Règles de correction"
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
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        content.addView(
            header,
            matchWrapParams(),
        )

        content.addView(
            TextView(this).apply {
                text =
                    "Choisissez les contrôles Grammalecte à appliquer. " +
                    "Les réglages sont utilisés par le correcteur Android, " +
                    "la correction de sélection et le clavier Grammalecte."

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

        statusView =
            TextView(this).apply {
                text = "Chargement des règles..."
                textSize = 14f

                setTextColor(
                    getColor(R.color.surface_text_secondary),
                )

                setPadding(
                    0,
                    0,
                    0,
                    smallSpacing,
                )
            }

        content.addView(
            statusView,
            matchWrapParams(),
        )

        resetButton =
            Button(this).apply {
                text = "Réinitialiser les règles"
                isAllCaps = false
                isEnabled = false

                setOnClickListener {
                    resetOptions()
                }
            }

        content.addView(
            resetButton,
            matchWrapParams(),
        )

        optionsContainer =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL

                setPadding(
                    0,
                    spacing,
                    0,
                    0,
                )
            }

        content.addView(
            optionsContainer,
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

        loadOptions()
    }

    private fun loadOptions() {
        statusView.text =
            "Chargement des règles..."

        resetButton.isEnabled = false
        optionsContainer.removeAllViews()

        executor.execute {
            val result =
                runCatching {
                    currentEngine()
                        .ruleOptions()
                }

            mainHandler.post {
                if (destroyed) {
                    return@post
                }

                result
                    .onSuccess { options ->
                        renderOptions(options)
                    }.onFailure { error ->
                        showError(error)
                    }
            }
        }
    }

    private fun renderOptions(options: List<GrammalecteRuleOption>) {
        optionsContainer.removeAllViews()

        if (options.isEmpty()) {
            statusView.text =
                "Aucune règle configurable disponible."

            resetButton.isEnabled = false
            return
        }

        statusView.text =
            "${options.size} options configurables."

        resetButton.isEnabled = true

        options
            .groupBy { option ->
                option.groupId
            }.values
            .forEach { group ->
                val first =
                    group.first()

                optionsContainer.addView(
                    sectionTitle(
                        first.groupLabel,
                    ),
                    matchWrapParams(),
                )

                group.forEach { option ->
                    addOption(option)
                }
            }
    }

    private fun addOption(option: GrammalecteRuleOption) {
        val density =
            resources.displayMetrics.density

        val padding =
            (12 * density).toInt()

        val spacing =
            (8 * density).toInt()

        val card =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL

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

        val toggle =
            Switch(this).apply {
                text = option.label
                textSize = 16f
                isChecked = option.enabled

                setTextColor(
                    getColor(R.color.surface_text),
                )
            }

        toggle.setOnCheckedChangeListener { _, enabled ->
            saveOption(
                toggle = toggle,
                optionId = option.id,
                enabled = enabled,
            )
        }

        card.addView(
            toggle,
            matchWrapParams(),
        )

        card.addView(
            TextView(this).apply {
                text =
                    buildString {
                        if (option.description.isNotBlank()) {
                            append(option.description)
                            append("\n")
                        }

                        append("Par défaut : ")

                        append(
                            if (option.defaultEnabled) {
                                "activé"
                            } else {
                                "désactivé"
                            },
                        )
                    }

                textSize = 13f

                setTextColor(
                    getColor(R.color.surface_text_secondary),
                )

                setPadding(
                    0,
                    spacing,
                    0,
                    0,
                )
            },
            matchWrapParams(),
        )

        optionsContainer.addView(
            card,
            LinearLayout
                .LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = spacing
                },
        )
    }

    private fun saveOption(
        toggle: Switch,
        optionId: String,
        enabled: Boolean,
    ) {
        toggle.isEnabled = false

        statusView.text =
            "Enregistrement..."

        executor.execute {
            val result =
                runCatching {
                    currentEngine()
                        .setRuleOption(
                            id = optionId,
                            enabled = enabled,
                        )
                }

            mainHandler.post {
                if (destroyed) {
                    return@post
                }

                result
                    .onSuccess {
                        toggle.isEnabled = true

                        statusView.text =
                            "Réglage enregistré."
                    }.onFailure { error ->
                        showError(error)
                        loadOptions()
                    }
            }
        }
    }

    private fun resetOptions() {
        resetButton.isEnabled = false

        statusView.text =
            "Réinitialisation..."

        executor.execute {
            val result =
                runCatching {
                    val currentEngine =
                        currentEngine()

                    currentEngine.resetRuleOptions()
                    currentEngine.ruleOptions()
                }

            mainHandler.post {
                if (destroyed) {
                    return@post
                }

                result
                    .onSuccess { options ->
                        renderOptions(options)

                        statusView.text =
                            "Réglages par défaut restaurés."
                    }.onFailure { error ->
                        showError(error)
                    }
            }
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

        resetButton.isEnabled = true
    }

    private fun sectionTitle(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 18f

            setTypeface(
                typeface,
                Typeface.BOLD,
            )

            setTextColor(
                getColor(R.color.surface_text),
            )

            setPadding(
                0,
                8,
                0,
                8,
            )
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
