package fr.grammalecteandroid.unofficial

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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

        content.addView(
            TextView(this).apply {
                text = "Bien démarrer avec Grammalecte"
                markAsAccessibilityHeading()
                textSize = 26f

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

        content.addView(
            TextView(this).apply {
                text =
                    "Grammalecte peut intervenir de trois façons. " +
                    "Vous n’avez pas besoin d’activer les trois modes."

                textSize = 15f

                setTextColor(
                    getColor(R.color.surface_text_secondary),
                )

                setPadding(
                    0,
                    smallSpacing,
                    0,
                    smallSpacing,
                )
            },
            matchWrapParams(),
        )

        content.addView(
            TextView(this).apply {
                text =
                    "Pour commencer, activez de préférence le correcteur Android. " +
                    "Les deux autres modes servent de complément lorsque " +
                    "l’application utilisée ne prend pas correctement en charge " +
                    "la correction native."

                textSize = 14f

                setTextColor(
                    getColor(R.color.surface_text_secondary),
                )

                setPadding(
                    0,
                    0,
                    0,
                    spacing,
                )
            },
            matchWrapParams(),
        )

        spellCheckerStatusView =
            statusTextView()

        content.addView(
            createGuideCard(
                number = "1",
                title = "Correcteur Android",
                description =
                    "Mode recommandé. Grammalecte devient le correcteur " +
                        "orthographique Android dans les applications compatibles.",
                statusView = spellCheckerStatusView,
                actions =
                    listOf(
                        "Configurer le correcteur Android" to {
                            openSpellCheckerSettings()
                        },
                    ),
            ),
            cardParams(spacing),
        )

        content.addView(
            createGuideCard(
                number = "2",
                title = "Corriger une sélection",
                description =
                    "Aucune activation nécessaire. Sélectionnez du texte dans " +
                        "une application compatible, ouvrez le menu de sélection, " +
                        "puis choisissez « Corriger avec Grammalecte ».",
                statusView =
                    TextView(this).apply {
                        text = "État : disponible sans activation."
                        textSize = 15f

                        setTypeface(
                            typeface,
                            Typeface.BOLD,
                        )

                        setTextColor(
                            getColor(R.color.surface_text),
                        )
                    },
                actions = emptyList(),
            ),
            cardParams(spacing),
        )

        imeStatusView =
            statusTextView()

        content.addView(
            createGuideCard(
                number = "3",
                title = "Clavier Grammalecte",
                description =
                    "Mode de repli pour corriger directement une sélection " +
                        "dans les applications qui ne gèrent pas correctement " +
                        "le correcteur Android ou le retour de texte corrigé.",
                statusView = imeStatusView,
                actions =
                    listOf(
                        "Gérer les méthodes de saisie" to {
                            openInputMethodSettings()
                        },
                        "Choisir le clavier" to {
                            showInputMethodPicker()
                        },
                    ),
            ),
            cardParams(spacing),
        )

        content.addView(
            TextView(this).apply {
                text =
                    "Tout le traitement est effectué localement sur l’appareil. " +
                    "Grammalecte Android ne nécessite pas de connexion Internet."

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
            Button(this).apply {
                text = "Continuer vers l’application"
                isAllCaps = false

                setOnClickListener {
                    finish()
                }
            },
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
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (
            hasFocus &&
            this::spellCheckerStatusView.isInitialized &&
            this::imeStatusView.isInitialized
        ) {
            updateStatus()
        }
    }

    private fun updateStatus() {
        val status =
            CorrectionStatusReader(this)
                .read()

        spellCheckerStatusView.text =
            when (status.spellChecker) {
                SpellCheckerStatus.SELECTED ->
                    "État : activé et sélectionné comme correcteur Android."

                SpellCheckerStatus.NOT_SELECTED ->
                    "État : disponible, mais non sélectionné."

                SpellCheckerStatus.DISABLED ->
                    "État : correction orthographique Android désactivée."

                SpellCheckerStatus.UNAVAILABLE ->
                    "État : aucun correcteur Android actif détecté."
            }

        imeStatusView.text =
            when (status.ime) {
                ImeStatus.SELECTED ->
                    "État : activé et actuellement sélectionné."

                ImeStatus.ENABLED ->
                    "État : activé, mais un autre clavier est sélectionné."

                ImeStatus.DISABLED ->
                    "État : non activé dans les méthodes de saisie."
            }
    }

    private fun openSpellCheckerSettings() {
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
        number: String,
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
                    text = "$number. $title"
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
