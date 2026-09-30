package fr.conscience.numerique.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import fr.conscience.numerique.container
import fr.conscience.numerique.data.Stats
import fr.conscience.numerique.data.StatsBucket
import fr.conscience.numerique.data.StatsRange
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
    val bypassed: Int get() = buckets.sumOf { it.bypassed }
    val snoozed: Int get() = buckets.sumOf { it.snoozed }
}

class AppStatsViewModel(application: Application, handle: SavedStateHandle) : AndroidViewModel(application) {
    private val packageName: String = checkNotNull(handle[AppStatsArgs.PACKAGE])

    private val range = MutableStateFlow(StatsRange.DAY)

    val state: StateFlow<AppStatsState> = combine(application.container.repository.choiceEvents, range) { events, range ->
        val mine = events.filter { it.packageName == packageName }
        AppStatsState(range, Stats.buckets(mine, range), allTimeAttempts = mine.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppStatsState(StatsRange.DAY, emptyList(), 0))

    fun setRange(value: StatsRange) {
        range.value = value
    }
}
