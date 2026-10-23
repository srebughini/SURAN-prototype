package app.suran

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class Screen { HOME, CHOOSE }
enum class ChooseMode { FIRST_RUN, EDIT }

data class UiState(
    val config: Config? = null,
    val installed: List<InstalledApp> = emptyList(),
    val catalog: List<Pair<CatalogItem, List<InstalledApp>>> = emptyList(),
    val screen: Screen = Screen.CHOOSE,
    val chooseMode: ChooseMode = ChooseMode.FIRST_RUN,
    val draft: List<Entry> = emptyList(),
    val ambiguity: CatalogItem? = null,
    val longTailExpanded: Boolean = false,
    val readyLine: Boolean = false,
    val homeSheet: Boolean = false,   // explains the default Home step; shown after first-run Start
    val launchError: String? = null,
    val saveError: Boolean = false,
    val isDefaultHome: Boolean = false,
)

class SuranViewModel(private val app: Application) : AndroidViewModel(app) {
    private val store = ConfigStore(app.filesDir)
    private val log = EventLog(app)
    private val _state = MutableStateFlow(initial())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private fun initial(): UiState {
        val cfg = store.load()
        val inst = queryInstalled(app)
        val cat = catalogCandidates(app, inst)
        return if (cfg != null && cfg.setupDone && cfg.entries.isNotEmpty()) {
            UiState(config = cfg, installed = inst, catalog = cat, screen = Screen.HOME, isDefaultHome = isDefaultHome(app))
        } else {
            UiState(config = null, installed = inst, catalog = cat, screen = Screen.CHOOSE, chooseMode = ChooseMode.FIRST_RUN, isDefaultHome = isDefaultHome(app))
        }
    }

    /** Called from Activity.onResume: refresh what is installed and whether we are the Home app. */
    fun onResume() {
        val inst = queryInstalled(app)
        val home = isDefaultHome(app)
        _state.update {
            it.copy(installed = inst, catalog = catalogCandidates(app, inst), isDefaultHome = home,
                homeSheet = it.homeSheet && !home)   // the sheet closes itself once SURAN is the Home app
        }
    }

    /** Home button pressed while SURAN is already in front. */
    fun onHomePressed() {
        val s = _state.value
        if (s.screen == Screen.CHOOSE && s.chooseMode == ChooseMode.EDIT) cancelEdit()
    }

    // ---- Home ----

    fun launch(entry: Entry) {
        val s = _state.value
        if (s.installed.none { it.pkg == entry.pkg }) { openEdit(); return }
        if (launchApp(app, entry.pkg)) {
            log.log("launch", "pkg" to entry.pkg, "intentId" to entry.intentId)
            _state.update { it.copy(readyLine = false, launchError = null) }
        } else {
            val name = s.installed.firstOrNull { it.pkg == entry.pkg }?.label ?: entry.appLabel
            _state.update { it.copy(launchError = "Couldn't open $name.") }
        }
    }

    fun clearLaunchError() = _state.update { it.copy(launchError = null) }
    fun clearReadyLine() = _state.update { it.copy(readyLine = false) }
    fun logConnectivity(target: String, action: String) =
        log.log("connectivity_open", "target" to target, "action" to action)

    fun reportError(message: String) = _state.update { it.copy(launchError = message) }

    fun showHomeSheet() = _state.update { it.copy(homeSheet = true) }
    fun hideHomeSheet() = _state.update { it.copy(homeSheet = false) }

    // ---- Choose ----

    fun openEdit() {
        val cfg = _state.value.config ?: return
        _state.update {
            it.copy(screen = Screen.CHOOSE, chooseMode = ChooseMode.EDIT, draft = cfg.entries,
                ambiguity = null, longTailExpanded = false, saveError = false)
        }
    }

    fun cancelEdit() = _state.update {
        it.copy(screen = Screen.HOME, draft = emptyList(), ambiguity = null, longTailExpanded = false, saveError = false)
    }

    fun toggleCatalog(item: CatalogItem) {
        val s = _state.value
        if (s.draft.any { it.intentId == item.id }) {
            _state.update { it.copy(draft = it.draft.filterNot { e -> e.intentId == item.id }) }
            return
        }
        val cands = s.catalog.firstOrNull { it.first.id == item.id }?.second ?: return
        if (cands.size == 1) addCatalog(item, cands[0]) else _state.update { it.copy(ambiguity = item) }
    }

    fun pickCandidate(item: CatalogItem, app: InstalledApp) {
        addCatalog(item, app)
        _state.update { it.copy(ambiguity = null) }
    }

    private fun addCatalog(item: CatalogItem, a: InstalledApp) = _state.update {
        it.copy(draft = it.draft.filterNot { e -> e.pkg == a.pkg } + Entry(a.pkg, item.id, a.label))
    }

    fun dismissAmbiguity() = _state.update { it.copy(ambiguity = null) }

    fun toggleApp(a: InstalledApp) = _state.update {
        if (it.draft.any { e -> e.pkg == a.pkg }) it.copy(draft = it.draft.filterNot { e -> e.pkg == a.pkg })
        else it.copy(draft = it.draft + Entry(a.pkg, null, a.label))
    }

    fun toggleEntry(e: Entry) = _state.update { it.copy(draft = it.draft.filterNot { x -> x.pkg == e.pkg && x.intentId == e.intentId }) }

    fun toggleLongTail() = _state.update { it.copy(longTailExpanded = !it.longTailExpanded) }

    /** True when the saved config would change. */
    fun hasChanges(s: UiState): Boolean {
        val a = normalize(s.draft).map { it.pkg to it.intentId }.toSet()
        val b = s.config?.entries?.map { it.pkg to it.intentId }?.toSet() ?: emptySet()
        return a != b
    }

    /** Returns true when saved; the caller requests the Home role after a first-run save. */
    fun save(): Boolean {
        val s = _state.value
        val entries = normalize(s.draft)
        if (entries.isEmpty()) return false
        val cfg = Config(setupDone = true, entries = entries)
        if (!store.save(cfg)) {
            _state.update { it.copy(saveError = true) }
            return false
        }
        val first = s.chooseMode == ChooseMode.FIRST_RUN
        val home = isDefaultHome(app)
        if (first) {
            log.log("setup_done", "entries" to entries.map { it.pkg })
        } else {
            val old = s.config?.entries?.map { it.pkg }.orEmpty().toSet()
            val new = entries.map { it.pkg }.toSet()
            log.log("config_saved", "added" to (new - old).toList(), "removed" to (old - new).toList())
        }
        _state.update {
            it.copy(config = cfg, screen = Screen.HOME, draft = emptyList(), ambiguity = null,
                longTailExpanded = false, saveError = false,
                readyLine = first && home, homeSheet = first && !home)
        }
        return true
    }
}
