package app.redflag.ui.home

import android.content.Context
import androidx.annotation.StringRes
import app.redflag.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.redflag.AppContainer
import app.redflag.data.ProblemCatalog
import app.redflag.data.ProblemRef
import app.redflag.data.displayLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * What the home screen shows. [serviceEnabled]: Android's accessibility service is on; [paused]: the interruption is off by choice
 * ([resumeAt]: when it comes back on by itself, 0 = when the user turns it on); [flaggedApps]; [avoided]: launches the user chose
 * not to do since [startedAt]; [topApp]: the app that interrupted the most; [topIssue] with [topIssueTheme], the title of its theme (for the icon).
 */
data class HomeState(
    val serviceEnabled: Boolean = true,
    val paused: Boolean = false,
    val resumeAt: Long = 0L,
    val flaggedApps: Int = 0,
    val avoided: Int = 0,
    val startedAt: Long = 0L,
    val topApp: TopApp? = null,
    val topIssue: TopIssue? = null,
    @StringRes val topIssueTheme: Int = R.string.category_custom,
    val loaded: Boolean = false,
)

class HomeViewModel(private val container: AppContainer, private val context: Context) : ViewModel() {
    private val repository = container.repository
    private val settings = container.settings

    private val serviceEnabled = MutableStateFlow(true)
    private val startedAt = MutableStateFlow(0L)

    init {
        viewModelScope.launch {
            val first = repository.choiceEvents.first().firstOrNull()?.timestamp
            startedAt.value = settings.startedAt(first ?: System.currentTimeMillis())
        }
    }

    /** Called when the screen comes back: the user may have turned the service on or off in Android's settings. */
    fun setServiceEnabled(enabled: Boolean) {
        serviceEnabled.value = enabled
    }

    /** The settings side of the state: the service, the pause, the start date. */
    private data class SettingsPart(val service: Boolean, val paused: Boolean, val resumeAt: Long, val startedAt: Long)

    private val settingsPart = combine(serviceEnabled, settings.interceptionEnabled, settings.reenableAt, startedAt) { service, enabled, resumeAt, started ->
        SettingsPart(service, !enabled, resumeAt, started)
    }

    val state: StateFlow<HomeState> = combine(
        settingsPart,
        combine(repository.monitoredApps, repository.refusals, repository.choiceEvents) { monitored, refusals, events -> Triple(monitored, refusals, events) },
        combine(repository.labelOverrides, repository.customProblems) { overrides, customs -> overrides to customs },
    ) { part, data, labels ->
        val (monitored, refusals, events) = data
        val (overrides, customs) = labels
        val flagged = monitored.filter { it.problems.isNotEmpty() }
        val names = monitored.associate { it.app.packageName to it.app.appName }
        val issue = topIssue(flagged) { ref -> ref.displayLabel(context, overrides[ref.id]) }
        val themeTitle = issue?.let { themeOf(it.ref, customs.firstOrNull { c -> c.id == it.ref.id }?.category) }
        HomeState(
            serviceEnabled = part.service,
            paused = part.paused,
            resumeAt = part.resumeAt,
            flaggedApps = flagged.size,
            avoided = refusals.sumOf { it.refusals },
            startedAt = part.startedAt,
            topApp = topInterruptedApp(events, names),
            topIssue = issue,
            topIssueTheme = themeTitle ?: R.string.category_custom,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    /** The title of the theme of an issue: its catalog theme, or the theme a custom issue was put in, or "Custom". */
    private fun themeOf(ref: ProblemRef, customCategory: String?): Int =
        ProblemCatalog.categories.firstOrNull { c -> c.problems.any { it.key == ref.id } }?.title
            ?: customCategory?.let(ProblemCatalog::findCategory)?.title
            ?: R.string.category_custom
}
