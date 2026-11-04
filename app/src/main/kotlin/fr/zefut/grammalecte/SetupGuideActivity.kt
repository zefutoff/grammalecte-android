package fr.zefut.grammalecte

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class SetupGuideActivity : Activity() {
    private lateinit var spellCheckerStatusView: TextView
    private lateinit var imeStatusView: TextView
    private lateinit var progressView: TextView
    private lateinit var previousButton: Button
    private lateinit var nextButton: Button
    private lateinit var leaveButton: Button
    private lateinit var stepCards: List<LinearLayout>
    private lateinit var offlineNote: TextView

    private var stepIndex = 0
    private var awaitingSpellCheckerSettings = false
    private var spellCheckerReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        stepIndex =
            (savedInstanceState?.getInt(STATE_STEP_INDEX) ?: 0)
                .coerceIn(0, STEP_COUNT - 1)
        awaitingSpellCheckerSettings =
            savedInstanceState?.getBoolean(STATE_WAITING_FOR_SETTINGS) ?: false

        val density = resources.displayMetrics.density
        val padding = (20 * density).toInt()
        val spacing = (12 * density).toInt()
        val smallSpacing = (6 * density).toInt()

        val content =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(padding, padding, padding, padding)
                setBackgroundColor(getColor(R.color.surface_background))
            }

        content.addView(
            TextView(this).apply {
                text = "Configurer Grammalecte"
                markAsAccessibilityHeading()
                textSize = 26f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(getColor(R.color.surface_text))
            },
            matchWrapParams(),
        )

        progressView =
            TextView(this).apply {
                enablePoliteAccessibilityUpdates()
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(getColor(R.color.brand_primary))
                setPadding(0, 0, 0, spacing)
            }
        content.addView(progressView, matchWrapParams())

        spellCheckerStatusView = statusTextView()
        val spellCheckerCard =
            createGuideCard(
                title = "Correcteur Android",
                description =
                    "Recommandé. Dans les réglages, activez la correction " +
                        "puis choisissez Grammalecte et Français.",
                statusView = spellCheckerStatusView,
                actions =
                    listOf(
                        "Réglages du correcteur" to {
                            openSpellCheckerSettings()
                        },
                    ),
            )

        val selectionStatusView =
            statusTextView().apply {
                text = "Prêt — aucun réglage nécessaire."
                setTextColor(getColor(R.color.setup_status_ready))
            }
        val selectionCard =
            createGuideCard(
                title = "Corriger une sélection",
                description =
                    "Sélectionnez du texte, puis choisissez " +
                        "« Corriger avec Grammalecte » dans le menu.",
                statusView = selectionStatusView,
                actions = emptyList(),
            )

        imeStatusView = statusTextView()
        val imeCard =
            createGuideCard(
                title = "Clavier Grammalecte · facultatif",
                description =
                    "Pour corriger une sélection lorsque la correction " +
                        "native n’est pas disponible.",
                statusView = imeStatusView,
                actions =
                    listOf(
                        "Gérer les claviers" to {
                            openInputMethodSettings()
                        },
                        "Choisir le clavier" to {
                            showInputMethodPicker()
                        },
                    ),
            )

        stepCards = listOf(spellCheckerCard, selectionCard, imeCard)
        stepCards.forEach { card ->
            content.addView(card, cardParams(spacing))
        }

        offlineNote =
            TextView(this).apply {
                text = "Corrections locales, sans connexion Internet."
                textSize = 13f
                setTextColor(getColor(R.color.surface_text_secondary))
                setPadding(0, smallSpacing, 0, spacing)
            }
        content.addView(offlineNote, matchWrapParams())

        val navigation =
            LinearLayout(this).apply {
                orientation =
                    if (resources.configuration.fontScale >= 1.5f) {
                        LinearLayout.VERTICAL
                    } else {
                        LinearLayout.HORIZONTAL
                    }
            }

        previousButton =
            Button(this).apply {
                text = "Précédent"
                isAllCaps = false
                setOnClickListener {
                    if (stepIndex > 0) {
                        stepIndex--
                        showStep()
                    }
                }
            }

        nextButton =
            Button(this).apply {
                isAllCaps = false
                backgroundTintList =
                    ColorStateList.valueOf(getColor(R.color.brand_primary))
                setTextColor(getColor(R.color.surface_background))
                setOnClickListener {
                    if (stepIndex == STEP_COUNT - 1) {
                        finish()
                    } else {
                        stepIndex++
                        showStep()
                    }
                }
            }

        val navigationParams =
            if (navigation.orientation == LinearLayout.VERTICAL) {
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            } else {
                LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f,
                )
            }

        navigation.addView(previousButton, navigationParams)
        navigation.addView(
            nextButton,
            LinearLayout.LayoutParams(
                navigationParams.width,
                navigationParams.height,
                navigationParams.weight,
            ),
        )
        content.addView(navigation, matchWrapParams())

        leaveButton =
            Button(this).apply {
                text = "Plus tard"
                isAllCaps = false
                textSize = 14f
                backgroundTintList =
                    ColorStateList.valueOf(getColor(R.color.surface_background))
                setTextColor(getColor(R.color.surface_text_secondary))
                setOnClickListener { finish() }
            }
        content.addView(leaveButton, matchWrapParams())

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
        showStep()
    }

    override fun onResume() {
        super.onResume()

        if (!::spellCheckerStatusView.isInitialized) {
            return
        }

        val status = updateStatus()
        if (awaitingSpellCheckerSettings) {
            awaitingSpellCheckerSettings = false
            if (
                stepIndex == 0 &&
                status.spellChecker == SpellCheckerStatus.SELECTED
            ) {
                stepIndex = 1
            }
        }
        showStep()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && ::spellCheckerStatusView.isInitialized) {
            updateStatus()
            showStep()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_STEP_INDEX, stepIndex)
        outState.putBoolean(
            STATE_WAITING_FOR_SETTINGS,
            awaitingSpellCheckerSettings,
        )
        super.onSaveInstanceState(outState)
    }

    private fun showStep() {
        if (!::stepCards.isInitialized) {
            return
        }

        progressView.text = "Étape ${stepIndex + 1}/$STEP_COUNT"
        stepCards.forEachIndexed { index, card ->
            card.visibility = if (index == stepIndex) View.VISIBLE else View.GONE
        }
        offlineNote.visibility =
            if (stepIndex == STEP_COUNT - 1) View.VISIBLE else View.GONE
        previousButton.visibility =
            if (stepIndex == 0) View.GONE else View.VISIBLE
        nextButton.text =
            when {
                stepIndex == STEP_COUNT - 1 -> "Terminer"
                stepIndex == 0 && !spellCheckerReady -> "Passer"
                else -> "Suivant"
            }
        leaveButton.visibility =
            if (stepIndex == STEP_COUNT - 1) View.GONE else View.VISIBLE
    }

    private fun updateStatus(): CorrectionStatus {
        val status = CorrectionStatusReader(this).read()
        spellCheckerReady = status.spellChecker == SpellCheckerStatus.SELECTED

        spellCheckerStatusView.text =
            when (status.spellChecker) {
                SpellCheckerStatus.SELECTED ->
                    "Activé — prêt à corriger."

                SpellCheckerStatus.FRENCH_SESSION_UNAVAILABLE ->
                    "À compléter — choisissez Français."

                SpellCheckerStatus.NOT_SELECTED ->
                    "À activer — choisissez Grammalecte."

                SpellCheckerStatus.DISABLED ->
                    "À activer — correction Android désactivée."

                SpellCheckerStatus.UNAVAILABLE ->
                    "Aucun correcteur Android actif détecté."
            }
        spellCheckerStatusView.setTextColor(
            getColor(
                if (spellCheckerReady) {
                    R.color.setup_status_ready
                } else {
                    R.color.setup_status_attention
                },
            ),
        )

        imeStatusView.text =
            when (status.ime) {
                ImeStatus.SELECTED ->
                    "Activé — clavier Grammalecte sélectionné."

                ImeStatus.ENABLED ->
                    "Activé — un autre clavier est utilisé."

                ImeStatus.DISABLED ->
                    "Non activé — facultatif."
            }
        imeStatusView.setTextColor(
            getColor(
                if (status.ime == ImeStatus.DISABLED) {
                    R.color.surface_text_secondary
                } else {
                    R.color.setup_status_ready
                },
            ),
        )

        return status
    }

    private companion object {
        const val STEP_COUNT = 3
        const val STATE_STEP_INDEX = "onboarding_step"
        const val STATE_WAITING_FOR_SETTINGS = "onboarding_waiting_settings"
    }

    private fun openSpellCheckerSettings() {
        awaitingSpellCheckerSettings = true
        val directIntent =
            Intent(Intent.ACTION_MAIN).apply {
                component =
                    ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings\$SpellCheckersSettingsActivity",
                    )

                addCategory(
                    Intent.CATEGORY_DEFAULT,
                )
            }

        val intent =
            if (
                directIntent.resolveActivity(
                    packageManager,
                ) != null
            ) {
                directIntent
            } else {
                Intent(
                    Settings.ACTION_INPUT_METHOD_SETTINGS,
                )
            }

        runCatching {
            startActivity(intent)
        }.onFailure {
            startActivity(
                Intent(
                    Settings.ACTION_SETTINGS,
                ),
            )
        }
    }

    private fun openInputMethodSettings() {
        runCatching {
            startActivity(
                Intent(
                    Settings.ACTION_INPUT_METHOD_SETTINGS,
                ),
            )
        }.onFailure {
            startActivity(
                Intent(
                    Settings.ACTION_SETTINGS,
                ),
            )
        }
    }

    private fun showInputMethodPicker() {
        getSystemService(
            InputMethodManager::class.java,
        ).showInputMethodPicker()
    }

    private fun statusTextView(): TextView =
        TextView(this).apply {
            enablePoliteAccessibilityUpdates()
            textSize = 15f

            setTypeface(
                typeface,
                Typeface.BOLD,
            )

            setTextColor(
                getColor(R.color.surface_text),
            )
        }

    private fun createGuideCard(
        title: String,
        description: String,
        statusView: TextView,
        actions: List<Pair<String, () -> Unit>>,
    ): LinearLayout {
        val density =
            resources.displayMetrics.density

        val padding =
            (14 * density).toInt()

        val spacing =
            (8 * density).toInt()

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                padding,
                padding,
                padding,
                padding,
            )

            background =
                roundedBackground(
                    14f,
                )

            addView(
                TextView(this@SetupGuideActivity).apply {
                    text = title
                    markAsAccessibilityHeading()
                    textSize = 18f

                    setTypeface(
                        typeface,
                        Typeface.BOLD,
                    )

                    setTextColor(
                        getColor(R.color.surface_text),
                    )
                },
                matchWrapParams(),
            )

            addView(
                TextView(this@SetupGuideActivity).apply {
                    text = description
                    textSize = 14f

                    setTextColor(
                        getColor(R.color.surface_text_secondary),
                    )

                    setPadding(
                        0,
                        spacing,
                        0,
                        spacing,
                    )
                },
                matchWrapParams(),
            )

            addView(
                statusView,
                matchWrapParams(),
            )

            actions.forEach { (label, action) ->
                addView(
                    Button(this@SetupGuideActivity).apply {
                        text = label
                        isAllCaps = false

                        setOnClickListener {
                            action()
                        }
                    },
                    matchWrapParams(),
                )
            }
        }
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

    private fun cardParams(spacing: Int): LinearLayout.LayoutParams =
        LinearLayout
            .LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = spacing
            }

    private fun matchWrapParams(): ViewGroup.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
}
