package fr.zefut.grammalecte

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.grammalecteandroid.engine.GrammalecteDictionary
import fr.grammalecteandroid.engine.GrammalecteQuickJsEngine
import fr.grammalecteandroid.engine.GrammalecteRuleOption
import java.util.concurrent.Executors

class RuleSettingsActivity : ComponentActivity() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "GrammalecteRuleSettings")
        }

    private var engine: GrammalecteQuickJsEngine? = null
    private var selectedDictionary by mutableStateOf(GrammalecteDictionary.ALL_VARIANTS)
    private var options by mutableStateOf<List<GrammalecteRuleOption>>(emptyList())
    private var status by mutableStateOf("Chargement des réglages…")
    private var statusIsError by mutableStateOf(false)
    private var busy by mutableStateOf(true)
    private var loaded by mutableStateOf(false)
    private var pendingDocumentResult: Pair<Int, Uri>? = null

    @Volatile
    private var destroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RuleSettingsScreen(
                dictionary = selectedDictionary,
                options = options,
                status = status,
                statusIsError = statusIsError,
                loaded = loaded,
                busy = busy,
                onBack = { finish() },
                onDictionary = { saveDictionary(it) },
                onRule = { id, enabled -> saveOption(id, enabled) },
                onManagePersonal = {
                    startActivity(Intent(this, PersonalDictionaryActivity::class.java))
                },
                onResetRules = { resetOptions() },
                onResetAll = { resetAllSettings() },
                onExport = { launchExportDocument() },
                onImport = { launchImportDocument() },
                onRetry = { loadOptions() },
            )
        }
        loadOptions()
    }

    private fun loadOptions() {
        busy = true
        loaded = false
        statusIsError = false
        status = "Chargement des réglages…"
        options = emptyList()

        executor.execute {
            val result =
                runCatching {
                    val currentEngine = currentEngine()
                    currentEngine.selectedDictionary() to currentEngine.ruleOptions()
                }

            mainHandler.post {
                if (destroyed) return@post
                busy = false
                result
                    .onSuccess { (dictionary, updatedOptions) ->
                        selectedDictionary = dictionary
                        options = updatedOptions
                        loaded = true
                        showOptionCount()
                        pendingDocumentResult?.let { pending ->
                            pendingDocumentResult = null
                            processDocumentResult(pending.first, pending.second)
                        }
                    }.onFailure { showError(it) }
            }
        }
    }

    private fun showOptionCount() {
        statusIsError = false
        status =
            if (options.isEmpty()) {
                "Aucune règle configurable disponible."
            } else {
                resources.getQuantityString(
                    R.plurals.configurable_options,
                    options.size,
                    options.size,
                )
            }
    }

    private fun <T> runOperation(
        message: String,
        task: () -> T,
        onSuccess: (T) -> Unit,
    ) {
        if (!loaded || busy) return
        busy = true
        statusIsError = false
        status = message

        executor.execute {
            val result = runCatching(task)
            mainHandler.post {
                if (destroyed) return@post
                busy = false
                result
                    .onSuccess(onSuccess)
                    .onFailure { showError(it) }
            }
        }
    }

    private fun saveDictionary(dictionary: GrammalecteDictionary) {
        if (dictionary == selectedDictionary) return
        runOperation(
            message = "Changement de dictionnaire…",
            task = { currentEngine().setDictionary(dictionary) },
        ) {
            selectedDictionary = dictionary
            status = "Dictionnaire enregistré."
        }
    }

    private fun saveOption(
        optionId: String,
        enabled: Boolean,
    ) {
        if (options.none { it.id == optionId && it.enabled != enabled }) return
        runOperation(
            message = "Enregistrement…",
            task = { currentEngine().setRuleOption(id = optionId, enabled = enabled) },
        ) {
            options =
                options.map { option ->
                    if (option.id == optionId) option.copy(enabled = enabled) else option
                }
            status = "Réglage enregistré."
        }
    }

    private fun resetOptions() {
        if (options.isEmpty()) return
        runOperation(
            message = "Réinitialisation…",
            task = {
                val currentEngine = currentEngine()
                currentEngine.resetRuleOptions()
                currentEngine.ruleOptions()
            },
        ) { updatedOptions ->
            options = updatedOptions
            status = "Réglages par défaut restaurés."
        }
    }

    private fun resetAllSettings() {
        runOperation(
            message = "Réinitialisation de tous les réglages…",
            task = {
                val currentEngine = currentEngine()
                currentEngine.resetToDefaults()
                currentEngine.selectedDictionary() to currentEngine.ruleOptions()
            },
        ) { (dictionary, updatedOptions) ->
            selectedDictionary = dictionary
            options = updatedOptions
            status = "Tous les réglages ont été réinitialisés."
        }
    }

    @Suppress("DEPRECATION")
    private fun launchExportDocument() {
        val intent =
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = PREFERENCES_MIME_TYPE
                putExtra(Intent.EXTRA_TITLE, PREFERENCES_FILE_NAME)
            }
        startActivityForResult(intent, REQUEST_EXPORT_PREFERENCES)
    }

    @Suppress("DEPRECATION")
    private fun launchImportDocument() {
        val intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = PREFERENCES_MIME_TYPE
            }
        startActivityForResult(intent, REQUEST_IMPORT_PREFERENCES)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        if (requestCode != REQUEST_EXPORT_PREFERENCES && requestCode != REQUEST_IMPORT_PREFERENCES) {
            return
        }
        if (!loaded || busy) {
            // The activity may be recreated while Android's document picker is open.
            pendingDocumentResult = requestCode to uri
        } else {
            processDocumentResult(requestCode, uri)
        }
    }

    private fun processDocumentResult(
        requestCode: Int,
        uri: Uri,
    ) {
        when (requestCode) {
            REQUEST_EXPORT_PREFERENCES -> exportPreferencesTo(uri)
            REQUEST_IMPORT_PREFERENCES -> importPreferencesFrom(uri)
        }
    }

    private fun exportPreferencesTo(uri: Uri) {
        runOperation(
            message = "Exportation des réglages…",
            task = {
                val json = currentEngine().exportPreferences()
                writePreferencesDocument(uri, json)
            },
        ) {
            status = "Réglages exportés."
        }
    }

    private fun importPreferencesFrom(uri: Uri) {
        runOperation(
            message = "Importation des réglages…",
            task = {
                val json = readPreferencesDocument(uri)
                val currentEngine = currentEngine()
                currentEngine.importPreferences(json)
                currentEngine.selectedDictionary() to currentEngine.ruleOptions()
            },
        ) { (dictionary, updatedOptions) ->
            selectedDictionary = dictionary
            options = updatedOptions
            status = "Réglages importés."
        }
    }

    private fun writePreferencesDocument(
        uri: Uri,
        json: String,
    ) {
        val output =
            contentResolver.openOutputStream(uri, "w")
                ?: error("Impossible d’ouvrir le fichier de destination.")
        output.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(json)
        }
    }

    private fun readPreferencesDocument(uri: Uri): String {
        val input =
            contentResolver.openInputStream(uri)
                ?: error("Impossible d’ouvrir le fichier sélectionné.")
        return input.bufferedReader(Charsets.UTF_8).use { reader ->
            val result = StringBuilder()
            val buffer = CharArray(IMPORT_BUFFER_SIZE)
            while (true) {
                val count = reader.read(buffer)
                if (count < 0) break
                result.append(buffer, 0, count)
                require(result.length <= MAX_IMPORTED_PREFERENCES_CHARS) {
                    "Le fichier de réglages est trop volumineux."
                }
            }
            result.toString()
        }
    }

    private fun currentEngine(): GrammalecteQuickJsEngine =
        engine
            ?: GrammalecteQuickJsEngine(applicationContext).also {
                engine = it
            }

    private fun showError(error: Throwable) {
        statusIsError = true
        status = "Erreur : " + (error.message ?: error.javaClass.simpleName)
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
        const val REQUEST_EXPORT_PREFERENCES = 4101
        const val REQUEST_IMPORT_PREFERENCES = 4102
        const val PREFERENCES_MIME_TYPE = "application/json"
        const val PREFERENCES_FILE_NAME = "grammalecte-android-preferences.json"
        const val MAX_IMPORTED_PREFERENCES_CHARS = 64 * 1024
        const val IMPORT_BUFFER_SIZE = 4096
    }
}
