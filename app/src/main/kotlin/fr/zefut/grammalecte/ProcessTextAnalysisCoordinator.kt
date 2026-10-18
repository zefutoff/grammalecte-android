package fr.zefut.grammalecte

import fr.grammalecteandroid.core.GrammarEngine
import fr.grammalecteandroid.core.GrammarIssue
import java.util.concurrent.Executors

/**
 * Serializes PROCESS_TEXT engine creation, analysis and shutdown.
 *
 * Engine operations always run on the same worker thread.
 * Callbacks are delivered only while their request is current.
 */
class ProcessTextAnalysisCoordinator(
    private val engineFactory: () -> GrammarEngine,
    private val callbackDispatcher: (() -> Unit) -> Unit,
) : AutoCloseable {
    private val stateLock = Any()

    private val executor =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "GrammalecteProcessTextAnalysis")
        }

    // Protected by stateLock.
    private var generation = 0L
    private var closed = false

    // Accessed only by executor's worker thread.
    private var engine: GrammarEngine? = null

    fun analyze(
        text: String,
        onResult: (Result<List<GrammarIssue>>) -> Unit,
    ) {
        synchronized(stateLock) {
            if (closed) {
                return
            }

            val requestGeneration = ++generation

            executor.execute {
                if (!isCurrent(requestGeneration)) {
                    return@execute
                }

                val result: Result<List<GrammarIssue>> =
                    runCatching {
                        val currentEngine =
                            engine
                                ?: engineFactory().also {
                                    engine = it
                                }

                        currentEngine.check(
                            text = text,
                            localeTag = "fr-FR",
                        )
                    }

                callbackDispatcher {
                    if (isCurrent(requestGeneration)) {
                        onResult(result)
                    }
                }
            }
        }
    }

    private fun isCurrent(requestGeneration: Long): Boolean =
        synchronized(stateLock) {
            !closed && requestGeneration == generation
        }

    override fun close() {
        synchronized(stateLock) {
            if (closed) {
                return
            }

            closed = true
            generation++

            // Shutdown is queued after any in-progress analysis.
            // close() itself remains non-blocking.
            executor.execute {
                try {
                    engine?.close()
                } finally {
                    engine = null
                }
            }

            executor.shutdown()
        }
    }
}
