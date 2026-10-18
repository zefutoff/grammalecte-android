package fr.zefut.grammalecte

import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.grammalecteandroid.core.GrammarEngine
import fr.grammalecteandroid.core.GrammarIssue
import fr.grammalecteandroid.core.WordCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class ProcessTextAnalysisCoordinatorTest {
    @Test
    fun concurrentRequestsReuseOneEngineAndPublishOnlyLatestResult() {
        val firstStarted = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val secondDelivered = CountDownLatch(1)
        val engineClosed = CountDownLatch(1)

        val creations = AtomicInteger()
        val activeChecks = AtomicInteger()
        val maxConcurrentChecks = AtomicInteger()

        val checkedTexts =
            Collections.synchronizedList(mutableListOf<String>())

        val deliveredTexts =
            Collections.synchronizedList(mutableListOf<String>())

        val engine =
            FakeEngine(
                onCheck = { text ->
                    val active = activeChecks.incrementAndGet()
                    maxConcurrentChecks.updateAndGet { maxOf(it, active) }

                    try {
                        checkedTexts.add(text)

                        if (text == "premier") {
                            firstStarted.countDown()
                            releaseFirst.await(10, TimeUnit.SECONDS)
                        }

                        emptyList()
                    } finally {
                        activeChecks.decrementAndGet()
                    }
                },
                onClose = {
                    engineClosed.countDown()
                },
            )

        val coordinator =
            ProcessTextAnalysisCoordinator(
                engineFactory = {
                    creations.incrementAndGet()
                    engine
                },
                callbackDispatcher = { callback ->
                    callback()
                },
            )

        try {
            coordinator.analyze("premier") {
                deliveredTexts.add("premier")
            }

            assertTrue(firstStarted.await(10, TimeUnit.SECONDS))

            coordinator.analyze("second") { result ->
                result.getOrThrow()
                deliveredTexts.add("second")
                secondDelivered.countDown()
            }

            assertEquals(1, activeChecks.get())

            releaseFirst.countDown()

            assertTrue(secondDelivered.await(10, TimeUnit.SECONDS))
            assertEquals(1, creations.get())
            assertEquals(1, maxConcurrentChecks.get())
            assertEquals(listOf("premier", "second"), checkedTexts.toList())
            assertEquals(listOf("second"), deliveredTexts.toList())
        } finally {
            releaseFirst.countDown()
            coordinator.close()
            assertTrue(engineClosed.await(10, TimeUnit.SECONDS))
        }
    }

    @Test
    fun closingDuringAnalysisDefersEngineCloseAndDiscardsResult() {
        val checkStarted = CountDownLatch(1)
        val releaseCheck = CountDownLatch(1)
        val engineClosed = CountDownLatch(1)
        val closeReturned = CountDownLatch(1)

        val closedCount = AtomicInteger()
        val deliveredCount = AtomicInteger()
        val checkThread = AtomicReference<Thread>()
        val closeThread = AtomicReference<Thread>()

        val engine =
            FakeEngine(
                onCheck = {
                    checkThread.set(Thread.currentThread())
                    checkStarted.countDown()
                    releaseCheck.await(10, TimeUnit.SECONDS)
                    emptyList()
                },
                onClose = {
                    closeThread.set(Thread.currentThread())
                    closedCount.incrementAndGet()
                    engineClosed.countDown()
                },
            )

        val coordinator =
            ProcessTextAnalysisCoordinator(
                engineFactory = { engine },
                callbackDispatcher = { callback ->
                    callback()
                },
            )

        try {
            coordinator.analyze("analyse longue") {
                deliveredCount.incrementAndGet()
            }

            assertTrue(checkStarted.await(10, TimeUnit.SECONDS))

            Thread {
                coordinator.close()
                closeReturned.countDown()
            }.start()

            assertTrue(
                "Closing the coordinator blocked while analysis was running",
                closeReturned.await(2, TimeUnit.SECONDS),
            )

            assertEquals(0, closedCount.get())

            releaseCheck.countDown()

            assertTrue(engineClosed.await(10, TimeUnit.SECONDS))
            assertEquals(1, closedCount.get())
            assertEquals(0, deliveredCount.get())
            assertEquals(checkThread.get(), closeThread.get())
        } finally {
            releaseCheck.countDown()
            coordinator.close()
        }
    }

    @Test
    fun callbackQueuedBeforeCloseIsDiscardedAfterClose() {
        val callbackQueued = CountDownLatch(1)
        val engineClosed = CountDownLatch(1)
        val deliveries = AtomicInteger()
        val pendingCallback = AtomicReference<(() -> Unit)?>(null)

        val coordinator =
            ProcessTextAnalysisCoordinator(
                engineFactory = {
                    FakeEngine(
                        onCheck = { emptyList() },
                        onClose = { engineClosed.countDown() },
                    )
                },
                callbackDispatcher = { callback ->
                    pendingCallback.set(callback)
                    callbackQueued.countDown()
                },
            )

        try {
            coordinator.analyze("texte de test") { result ->
                result.getOrThrow()
                deliveries.incrementAndGet()
            }

            assertTrue(
                "Analysis callback was not queued",
                callbackQueued.await(10, TimeUnit.SECONDS),
            )

            coordinator.close()

            assertTrue(
                "Engine was not closed",
                engineClosed.await(10, TimeUnit.SECONDS),
            )

            requireNotNull(pendingCallback.get()).invoke()

            assertEquals(
                "Callback was delivered after close",
                0,
                deliveries.get(),
            )
        } finally {
            coordinator.close()
        }
    }

    private class FakeEngine(
        private val onCheck: (String) -> List<GrammarIssue>,
        private val onClose: () -> Unit,
    ) : GrammarEngine {
        override fun check(
            text: String,
            localeTag: String,
        ): List<GrammarIssue> = onCheck(text)

        override fun checkWord(
            word: String,
            localeTag: String,
            suggestionLimit: Int,
        ): WordCheck = WordCheck(valid = true)

        override fun close() {
            onClose()
        }
    }
}
