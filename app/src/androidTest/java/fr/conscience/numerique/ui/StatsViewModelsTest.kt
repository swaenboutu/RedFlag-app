package fr.conscience.numerique.ui

import fr.conscience.numerique.data.ProblemRef
import fr.conscience.numerique.data.StatsRange
import fr.conscience.numerique.ui.stats.AppStatsArgs
import fr.conscience.numerique.ui.stats.AppStatsViewModel
import fr.conscience.numerique.ui.stats.StatsViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** Les ViewModels des statistiques (liste des apps interceptées, détail d'une app), sur une base en mémoire. */
class StatsViewModelsTest {
    private lateinit var env: ViewModelEnv
    private val fomo = ProblemRef.catalog("fomo")

    @Before
    fun setUp() {
        env = ViewModelEnv()
    }

    @After
    fun tearDown() = env.close()

    @Test
    fun theListOrdersAppsByAttemptsThenByName() = runBlocking {
        env.repository.linkProblem("app.b", "Bravo", fomo)
        env.repository.linkProblem("app.a", "Alpha", fomo)
        env.repository.linkProblem("app.c", "Charlie", fomo)
        repeat(3) { env.repository.recordChoice("app.c", proceeded = false) }
        env.repository.recordChoice("app.a", proceeded = true)
        env.repository.recordChoice("app.b", proceeded = false)

        val vm = env.viewModel { StatsViewModel(env.container) }
        val state = vm.state.await { it.rows.size == 3 && it.installedCount != null }

        assertEquals(3, state.interceptedCount)
        assertEquals(listOf("Charlie", "Alpha", "Bravo"), state.rows.map { it.app.appName })
        assertEquals(listOf(3, 1, 1), state.rows.map { it.app.attempts })
        assertEquals("première ligne arrondie en haut", true, state.rows.first().first)
        assertEquals("dernière ligne arrondie en bas", true, state.rows.last().last)
    }

    @Test
    fun anAppWithoutAnyAttemptIsListedWithZero() = runBlocking {
        env.repository.linkProblem("app.a", "Alpha", fomo)

        val state = env.viewModel { StatsViewModel(env.container) }.state.await { it.rows.isNotEmpty() && it.installedCount != null }

        assertEquals(0, state.rows.single().app.attempts)
    }

    @Test
    fun appStatisticsCountOnlyThatAppAndFollowTheRange() = runBlocking {
        env.repository.recordChoice("app.a", proceeded = false)
        env.repository.recordChoice("app.a", proceeded = false)
        env.repository.recordChoice("app.a", proceeded = true)
        env.repository.recordChoice("app.a", proceeded = true, snoozed = true)
        env.repository.recordChoice("app.other", proceeded = false)

        val vm = env.viewModel(mapOf(AppStatsArgs.PACKAGE to "app.a")) { AppStatsViewModel(env.container, it) }
        val day = vm.state.await { it.allTimeAttempts == 4 }

        assertEquals(StatsRange.DAY, day.range)
        assertEquals(4, day.attempts)
        assertEquals(1, day.bypassed)
        assertEquals(1, day.snoozed)

        vm.setRange(StatsRange.YEAR)
        val year = vm.state.await { it.range == StatsRange.YEAR }
        assertEquals("l'échelle change les barres, pas le total depuis le début", 4, year.allTimeAttempts)
        assertEquals(StatsRange.YEAR.bucketCount, year.buckets.size)
        assertEquals(4, year.attempts)
    }
}
