package fr.zefut.grammalecte

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.grammalecteandroid.engine.GrammalecteQuickJsEngine
import java.util.concurrent.Executors

class PersonalDictionaryActivity : ComponentActivity() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "GrammalectePersonalDictionary")
        }

    private var input by mutableStateOf("")
    private var words by mutableStateOf<List<String>>(emptyList())
    private var status by mutableStateOf("Chargement…")
    private var busy by mutableStateOf(true)
    private var engine: GrammalecteQuickJsEngine? = null

    @Volatile
    private var destroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        input = savedInstanceState?.getString(STATE_INPUT).orEmpty()
        setContent {
            PersonalDictionaryScreen(
                input = input,
                words = words,
                status = status,
                busy = busy,
                onInputChange = { input = it },
                onAdd = { addWord() },
                onRemove = { removeWord(it) },
                onBack = { finish() },
            )
        }
        loadWords()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_INPUT, input)
        super.onSaveInstanceState(outState)
    }

    private fun loadWords() {
        busy = true
        status = "Chargement…"
        executor.execute {
            val result = runCatching { currentEngine().personalDictionaryWords() }
            mainHandler.post {
                if (destroyed) return@post
                result
                    .onSuccess { loadedWords ->
                        renderWords(loadedWords)
                        busy = false
                    }.onFailure { showError(it) }
            }
        }
    }

    private fun addWord() {
        val word = input.trim()
        when {
            word.isEmpty() -> {
                status = "Saisissez un mot."
                return
            }
            word.length > 64 -> {
                status = "Le mot ne doit pas dépasser 64 caractères."
                return
            }
            word.any(Char::isWhitespace) -> {
                status = "Ajoutez un seul mot à la fois."
                return
            }
        }

        busy = true
        status = "Ajout en cours…"
        executor.execute {
            val result =
                runCatching {
                    val currentEngine = currentEngine()
                    val added = currentEngine.addPersonalDictionaryWord(word)
                    added to currentEngine.personalDictionaryWords()
                }
            mainHandler.post {
                if (destroyed) return@post
                result
                    .onSuccess { (added, updatedWords) ->
                        input = ""
                        renderWords(updatedWords)
                        busy = false
                        status = if (added) "Mot ajouté." else "Ce mot est déjà dans le dictionnaire."
                    }.onFailure { showError(it) }
            }
        }
    }

    private fun removeWord(word: String) {
        busy = true
        status = "Suppression en cours…"
        executor.execute {
            val result =
                runCatching {
                    val currentEngine = currentEngine()
                    currentEngine.removePersonalDictionaryWord(word)
                    currentEngine.personalDictionaryWords()
                }
            mainHandler.post {
                if (destroyed) return@post
                result
                    .onSuccess { updatedWords ->
                        renderWords(updatedWords)
                        busy = false
                        status = "Mot supprimé."
                    }.onFailure { showError(it) }
            }
        }
    }

    private fun renderWords(updatedWords: List<String>) {
        words = updatedWords
        status =
            if (updatedWords.isEmpty()) {
                "Aucun mot enregistré."
            } else {
                resources.getQuantityString(
                    R.plurals.personal_dictionary_word_count,
                    updatedWords.size,
                    updatedWords.size,
                )
            }
    }

    private fun currentEngine(): GrammalecteQuickJsEngine =
        engine
            ?: GrammalecteQuickJsEngine(applicationContext).also {
                engine = it
            }

    private fun showError(error: Throwable) {
        status = "Erreur : " + (error.message ?: error.javaClass.simpleName)
        busy = false
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

    private companion object {
        const val STATE_INPUT = "personal_dictionary_input"
    }
}
