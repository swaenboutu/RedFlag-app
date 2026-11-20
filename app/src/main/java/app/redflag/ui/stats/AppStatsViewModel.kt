package app.redflag.ui.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.redflag.AppContainer
import app.redflag.data.Stats
import app.redflag.data.StatsBucket
import app.redflag.data.StatsRange
import app.redflag.data.UninstallSuggestion
import app.redflag.data.UninstallSuggestions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Clés des extras de l'intent, lues aussi par le SavedStateHandle du ViewModel. */
object AppStatsArgs {
    const val PACKAGE = "package"
    const val LABEL = "label"
}

/** [allTimeAttempts] : toutes les tentatives de cette app depuis le début, quelle que soit l'échelle affichée. */
data class AppStatsState(val range: StatsRange, val buckets: List<StatsBucket>, val allTimeAttempts: Int) {
    val attempts: Int get() = buckets.sumOf { it.attempts }
    val blocked: Int get() = buckets.sumOf { it.blocked }
    val bypassed: Int get() = buckets.sumOf { it.bypassed }
    val snoozed: Int get() = buckets.sumOf { it.snoozed }
}

class AppStatsViewModel(private val container: AppContainer, handle: SavedStateHandle) : ViewModel() {
    private val packageName: String = checkNotNull(handle[AppStatsArgs.PACKAGE])

    /**
     * Par mois par défaut : sur 14 jours seulement, les chiffres pouvaient contredire la suggestion de désinstallation, qui compte
     * sur 3 mois.
     */
    private val range = MutableStateFlow(StatsRange.MONTH)

    val state: StateFlow<AppStatsState> = combine(container.repository.choiceEvents, range) { events, range ->
        val mine = events.filter { it.packageName == packageName }
        AppStatsState(range, Stats.buckets(mine, range), allTimeAttempts = mine.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppStatsState(StatsRange.MONTH, emptyList(), 0))

    /**
     * A suggestion to uninstall this app, when the user almost always refuses to open it (see [UninstallSuggestions]); null
     * otherwise, or once the user chose "Don't suggest again" for it.
     */
    val suggestion: StateFlow<UninstallSuggestion?> = combine(
        container.repository.choiceEvents,
        container.settings.dismissedUninstallSuggestions,
    ) { events, dismissed ->
        UninstallSuggestions.suggest(events.filter { it.packageName == packageName }, System.currentTimeMillis(), dismissed)
            .firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun dismissSuggestion() = container.settings.dismissUninstallSuggestion(packageName)

    fun setRange(value: StatsRange) {
        range.value = value
    }
}
