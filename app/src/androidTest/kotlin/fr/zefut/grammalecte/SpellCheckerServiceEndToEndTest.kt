package fr.zefut.grammalecte

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.LocaleList
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import fr.grammalecteandroid.spellchecker.GrammalecteSpellCheckerService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class SpellCheckerServiceEndToEndTest {
    @Test
    fun systemSpellCheckerSessionReturnsRealGrammalecteSuggestion() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        val originalSelected =
            readSecureSetting(SELECTED_SPELL_CHECKER)

        val originalEnabled =
            readSecureSetting(SPELL_CHECKER_ENABLED)

        val component =
            ComponentName(
                context,
                GrammalecteSpellCheckerService::class.java,
            ).flattenToString()

        try {
            writeSecureSetting(
                SELECTED_SPELL_CHECKER,
                component,
            )
            writeSecureSetting(
                SPELL_CHECKER_ENABLED,
                "1",
            )

            val latch = CountDownLatch(1)

            val received =
                AtomicReference<Array<SentenceSuggestionsInfo>>()

            val listener =
                object :
                    SpellCheckerSession.SpellCheckerSessionListener {
                    override fun onGetSuggestions(results: Array<SuggestionsInfo>) = Unit

                    override fun onGetSentenceSuggestions(results: Array<SentenceSuggestionsInfo>) {
                        received.set(results)
                        latch.countDown()
                    }
                }

            val manager =
                context.getSystemService(
                    TextServicesManager::class.java,
                )

            val session =
                createGrammalecteSession(
                    manager = manager,
                    listener = listener,
                    expectedPackageName = context.packageName,
                )

            try {
                val text =
                    "Je suis aller au magazin hier."

                InstrumentationRegistry
                    .getInstrumentation()
                    .runOnMainSync {
                        session.getSentenceSuggestions(
                            arrayOf(TextInfo(text)),
                            SUGGESTION_LIMIT,
                        )
                    }

                assertTrue(
                    "Timed out waiting for SpellCheckerService",
                    latch.await(
                        RESULT_TIMEOUT_SECONDS,
                        TimeUnit.SECONDS,
                    ),
                )

                val results =
                    requireNotNull(received.get()) {
                        "No sentence suggestions received"
                    }

                assertTrue(
                    "Expected one sentence result, got ${results.size}",
                    results.isNotEmpty(),
                )

                val found =
                    results.any { result ->
                        containsCorrection(
                            text = text,
                            result = result,
                            source = "magazin",
                            expectedSuggestion = "magasin",
                        )
                    }

                assertTrue(
                    "Expected «magazin» -> «magasin» through Android SpellCheckerService",
                    found,
                )
            } finally {
                InstrumentationRegistry
                    .getInstrumentation()
                    .runOnMainSync {
                        session.close()
                    }
            }
        } finally {
            restoreSecureSetting(
                SELECTED_SPELL_CHECKER,
                originalSelected,
            )
            restoreSecureSetting(
                SPELL_CHECKER_ENABLED,
                originalEnabled,
            )
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun statusReflectsFrenchSubtypeSelection() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return
        }

        val context =
            ApplicationProvider.getApplicationContext<Context>()

        val manager =
            context.getSystemService(TextServicesManager::class.java)

        val originalSelected =
            readSecureSetting(SELECTED_SPELL_CHECKER)

        val originalEnabled =
            readSecureSetting(SPELL_CHECKER_ENABLED)

        val originalSubtype =
            readSecureSetting(SELECTED_SPELL_CHECKER_SUBTYPE)

        val component =
            ComponentName(
                context,
                GrammalecteSpellCheckerService::class.java,
            ).flattenToString()

        try {
            writeSecureSetting(SELECTED_SPELL_CHECKER, component)
            writeSecureSetting(SPELL_CHECKER_ENABLED, "1")

            var selectedInfo: SpellCheckerInfo? = null

            for (attempt in 0 until SESSION_SELECTION_ATTEMPTS) {
                val candidate = manager.currentSpellCheckerInfo

                if (candidate?.packageName == context.packageName) {
                    selectedInfo = candidate
                    break
                }

                SystemClock.sleep(SESSION_SELECTION_DELAY_MILLIS)
            }

            val info =
                requireNotNull(selectedInfo) {
                    "Grammalecte was not selected by Android"
                }

            @Suppress("DEPRECATION")
            val frenchSubtype =
                (0 until info.subtypeCount)
                    .map(info::getSubtypeAt)
                    .single { it.locale == "fr_FR" }

            if (LocaleList.getDefault()[0].language != "fr") {
                writeSecureSetting(
                    SELECTED_SPELL_CHECKER_SUBTYPE,
                    "0",
                )

                assertSpellCheckerStatusEventually(
                    context,
                    SpellCheckerStatus.FRENCH_SESSION_UNAVAILABLE,
                )
            }

            writeSecureSetting(
                SELECTED_SPELL_CHECKER_SUBTYPE,
                frenchSubtype.hashCode().toString(),
            )

            assertSpellCheckerStatusEventually(
                context,
                SpellCheckerStatus.SELECTED,
            )
        } finally {
            restoreSecureSetting(
                SELECTED_SPELL_CHECKER,
                originalSelected,
            )

            restoreSecureSetting(
                SELECTED_SPELL_CHECKER_SUBTYPE,
                originalSubtype,
            )

            restoreSecureSetting(
                SPELL_CHECKER_ENABLED,
                originalEnabled,
            )
        }
    }

    private fun assertSpellCheckerStatusEventually(
        context: Context,
        expected: SpellCheckerStatus,
    ) {
        var actual: SpellCheckerStatus? = null

        repeat(SESSION_SELECTION_ATTEMPTS) {
            val result = AtomicReference<SpellCheckerStatus>()

            InstrumentationRegistry
                .getInstrumentation()
                .runOnMainSync {
                    result.set(
                        CorrectionStatusReader(context)
                            .read()
                            .spellChecker,
                    )
                }

            actual = result.get()

            if (actual == expected) {
                return
            }

            SystemClock.sleep(SESSION_SELECTION_DELAY_MILLIS)
        }

        assertEquals(
            "Unexpected native French session status",
            expected,
            actual,
        )
    }

    private fun createGrammalecteSession(
        manager: TextServicesManager,
        listener: SpellCheckerSession.SpellCheckerSessionListener,
        expectedPackageName: String,
    ): SpellCheckerSession {
        repeat(SESSION_SELECTION_ATTEMPTS) {
            val selectedSession =
                AtomicReference<SpellCheckerSession?>()

            InstrumentationRegistry
                .getInstrumentation()
                .runOnMainSync {
                    val session =
                        manager.newSpellCheckerSession(
                            null,
                            Locale.FRANCE,
                            listener,
                            false,
                        )

                    if (
                        session != null &&
                        session.spellChecker.packageName ==
                        expectedPackageName
                    ) {
                        selectedSession.set(session)
                    } else {
                        session?.close()
                    }
                }

            selectedSession.get()?.let {
                return it
            }

            SystemClock.sleep(
                SESSION_SELECTION_DELAY_MILLIS,
            )
        }

        error(
            "Android did not select the Grammalecte SpellCheckerService",
        )
    }

    private fun containsCorrection(
        text: String,
        result: SentenceSuggestionsInfo,
        source: String,
        expectedSuggestion: String,
    ): Boolean =
        (0 until result.suggestionsCount).any { index ->
            val offset =
                result.getOffsetAt(index)

            val length =
                result.getLengthAt(index)

            if (
                offset < 0 ||
                length <= 0 ||
                offset + length > text.length
            ) {
                false
            } else {
                val fragment =
                    text.substring(
                        offset,
                        offset + length,
                    )

                val suggestions =
                    suggestions(
                        result.getSuggestionsInfoAt(index),
                    )

                fragment == source &&
                    expectedSuggestion in suggestions
            }
        }

    private fun suggestions(info: SuggestionsInfo): List<String> {
        val count =
            info.suggestionsCount.coerceAtLeast(0)

        return List(count) { index ->
            info.getSuggestionAt(index)
        }
    }

    private fun readSecureSetting(name: String): String =
        shell(
            "settings get secure $name",
        )

    private fun writeSecureSetting(
        name: String,
        value: String,
    ) {
        shell(
            "settings put secure $name $value",
        )
    }

    private fun restoreSecureSetting(
        name: String,
        value: String,
    ) {
        if (
            value.isBlank() ||
            value == "null"
        ) {
            shell(
                "settings delete secure $name",
            )
        } else {
            writeSecureSetting(
                name,
                value,
            )
        }
    }

    private fun shell(command: String): String {
        val descriptor =
            InstrumentationRegistry
                .getInstrumentation()
                .uiAutomation
                .executeShellCommand(command)

        return ParcelFileDescriptor
            .AutoCloseInputStream(descriptor)
            .bufferedReader()
            .use {
                it.readText().trim()
            }
    }

    private companion object {
        const val SELECTED_SPELL_CHECKER =
            "selected_spell_checker"

        const val SPELL_CHECKER_ENABLED =
            "spell_checker_enabled"

        const val SELECTED_SPELL_CHECKER_SUBTYPE =
            "selected_spell_checker_subtype"

        const val SUGGESTION_LIMIT = 8

        const val SESSION_SELECTION_ATTEMPTS = 20

        const val SESSION_SELECTION_DELAY_MILLIS =
            250L

        const val RESULT_TIMEOUT_SECONDS =
            15L
    }
}
