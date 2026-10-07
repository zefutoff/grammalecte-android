package fr.grammalecteandroid.unofficial

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import fr.grammalecteandroid.engine.GrammalecteDictionary
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
    private lateinit var dictionaryGroup: RadioGroup
    private lateinit var optionsContainer: LinearLayout
    private lateinit var resetButton: Button
    private lateinit var resetAllButton: Button

    private var engine: GrammalecteQuickJsEngine? = null
    private var selectedDictionary =
        GrammalecteDictionary.ALL_VARIANTS

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
                text = "Réglages de correction"
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
                    "Choisissez le dictionnaire et les contrôles Grammalecte " +
                    "à appliquer. Les réglages sont partagés par le correcteur " +
                    "Android, la correction de sélection et le clavier Grammalecte."

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

        content.addView(
            sectionTitle(
                "Réinitialisation",
            ),
            matchWrapParams(),
        )

        content.addView(
            TextView(this).apply {
                text =
                    "Restaure le dictionnaire principal, les règles " +
                    "Grammalecte et vide le dictionnaire personnel."

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
            },
            matchWrapParams(),
        )

        resetAllButton =
            Button(this).apply {
                text = "Réinitialiser tous les réglages"
                isAllCaps = false
                isEnabled = false

                setOnClickListener {
                    confirmResetAllSettings()
                }
            }

        content.addView(
            resetAllButton,
            matchWrapParams(),
        )

        content.addView(
            sectionTitle(
                "Dictionnaire",
            ),
            matchWrapParams(),
        )

        content.addView(
            TextView(this).apply {
                text =
                    "Choisissez les graphies françaises reconnues " +
                    "par le correcteur orthographique."

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
            },
            matchWrapParams(),
        )

        dictionaryGroup =
            RadioGroup(this).apply {
                orientation = RadioGroup.VERTICAL

                setPadding(
                    spacing,
                    smallSpacing,
                    spacing,
                    smallSpacing,
                )

                background =
                    roundedBackground(
                        radiusDp = 14f,
                    )
            }

        content.addView(
            dictionaryGroup,
            matchWrapParams(),
        )

        content.addView(
            sectionTitle(
                "Dictionnaire personnel",
            ),
            matchWrapParams(),
        )

        content.addView(
            TextView(this).apply {
                text =
                    "Ajoutez vos noms propres, termes techniques " +
                    "ou mots absents du dictionnaire Grammalecte."

                textSize = 14f

                setTextColor(
                    getColor(R.color.surface_text_secondary),
                )
            },
            matchWrapParams(),
        )

        content.addView(
            Button(this).apply {
                text = "Gérer le dictionnaire personnel"
                isAllCaps = false

                setOnClickListener {
                    startActivity(
                        Intent(
                            this@RuleSettingsActivity,
                            PersonalDictionaryActivity::class.java,
                        ),
                    )
                }
            },
            matchWrapParams(),
        )

        content.addView(
            sectionTitle(
                "Règles de correction",
            ),
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
            "Chargement des réglages..."

        resetButton.isEnabled = false
        resetAllButton.isEnabled = false
        dictionaryGroup.removeAllViews()
        optionsContainer.removeAllViews()

        executor.execute {
            val result =
                runCatching {
                    val currentEngine =
                        currentEngine()

                    currentEngine.selectedDictionary() to
                        currentEngine.ruleOptions()
                }

            mainHandler.post {
                if (destroyed) {
                    return@post
                }

                result
                    .onSuccess { (dictionary, options) ->
                        renderDictionary(dictionary)
                        renderOptions(options)
                        resetAllButton.isEnabled = true
                    }.onFailure { error ->
                        showError(error)
                    }
            }
        }
    }

    private fun renderDictionary(dictionary: GrammalecteDictionary) {
        selectedDictionary =
            dictionary

        dictionaryGroup.removeAllViews()

        addDictionaryOption(
            dictionary = GrammalecteDictionary.ALL_VARIANTS,
            label = "Toutes variantes",
            description =
                "Accepte les graphies classiques et celles issues " +
                    "des rectifications de 1990.",
        )

        addDictionaryOption(
            dictionary = GrammalecteDictionary.CLASSIC,
            label = "Orthographe classique",
            description =
                "Utilise les graphies traditionnelles.",
        )

        addDictionaryOption(
            dictionary = GrammalecteDictionary.REFORM_1990,
            label = "Réforme de 1990",
            description =
                "Utilise les graphies issues des rectifications " +
                    "orthographiques de 1990.",
        )
    }

    private fun addDictionaryOption(
        dictionary: GrammalecteDictionary,
        label: String,
        description: String,
    ) {
        val spacing =
            (6 * resources.displayMetrics.density).toInt()

        dictionaryGroup.addView(
            RadioButton(this).apply {
                id = View.generateViewId()

                text =
                    buildString {
                        append(label)
                        append("\n")
                        append(description)
                    }

                textSize = 15f
                isChecked =
                    dictionary == selectedDictionary

                setTextColor(
                    getColor(R.color.surface_text),
                )

                setPadding(
                    0,
                    spacing,
                    0,
                    spacing,
                )

                setOnClickListener {
                    if (dictionary != selectedDictionary) {
                        saveDictionary(dictionary)
                    }
                }
            },
            matchWrapParams(),
        )
    }

    private fun saveDictionary(dictionary: GrammalecteDictionary) {
        setDictionaryControlsEnabled(false)

        statusView.text =
            "Changement de dictionnaire..."

        executor.execute {
            val result =
                runCatching {
                    currentEngine()
                        .setDictionary(dictionary)
                }

            mainHandler.post {
                if (destroyed) {
                    return@post
                }

                result
                    .onSuccess {
                        selectedDictionary =
                            dictionary

                        setDictionaryControlsEnabled(true)

                        statusView.text =
                            "Dictionnaire enregistré."
                    }.onFailure { error ->
                        showError(error)
                        loadOptions()
                    }
            }
        }
    }

    private fun setDictionaryControlsEnabled(enabled: Boolean) {
        for (index in 0 until dictionaryGroup.childCount) {
            dictionaryGroup
                .getChildAt(index)
                .isEnabled = enabled
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

    private fun confirmResetAllSettings() {
        AlertDialog
            .Builder(this)
            .setTitle(
                "Réinitialiser tous les réglages ?",
            ).setMessage(
                "Le dictionnaire principal reviendra à « Toutes variantes », " +
                    "les règles Grammalecte retrouveront leurs valeurs par défaut " +
                    "et tous les mots du dictionnaire personnel seront supprimés.\n\n" +
                    "Cette action est irréversible.",
            ).setNegativeButton(
                "Annuler",
                null,
            ).setPositiveButton(
                "Réinitialiser",
            ) { _, _ ->
                resetAllSettings()
            }.show()
    }

    private fun resetAllSettings() {
        resetAllButton.isEnabled = false
        resetButton.isEnabled = false
        setDictionaryControlsEnabled(false)

        statusView.text =
            "Réinitialisation de tous les réglages..."

        executor.execute {
            val result =
                runCatching {
                    val currentEngine =
                        currentEngine()

                    currentEngine.resetToDefaults()

                    currentEngine.selectedDictionary() to
                        currentEngine.ruleOptions()
                }

            mainHandler.post {
                if (destroyed) {
                    return@post
                }

                result
                    .onSuccess { (dictionary, options) ->
                        renderDictionary(dictionary)
                        renderOptions(options)

                        resetAllButton.isEnabled = true

                        statusView.text =
                            "Tous les réglages ont été réinitialisés."
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
        resetAllButton.isEnabled = true
        setDictionaryControlsEnabled(true)
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
