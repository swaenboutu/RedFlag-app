package app.redflag.ui

import app.redflag.data.InstalledApp
import app.redflag.data.ProblemRef
import app.redflag.ui.apps.AppFilter
import app.redflag.ui.apps.AppPickerArgs
import app.redflag.ui.apps.AppPickerViewModel
import app.redflag.ui.apps.MainViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** La liste des apps (écran d'accueil de l'app) et le sélecteur d'apps, avec les vraies apps installées sur l'appareil. */
class AppListViewModelsTest {
    private lateinit var env: ViewModelEnv
    private lateinit var flagged: InstalledApp
    private lateinit var other: InstalledApp
    private val fomo = ProblemRef.catalog("fomo")

    @Before
    fun setUp() {
        env = ViewModelEnv()
        assumeUserApps(env.context, 2)
        env.container.settings.setHideSystemApps(true)
        // Deux apps visibles avec le réglage par défaut : l'une sera signalée, l'autre non.
        val visible = env.container.installedApps.list(hideSystemApps = true)
        flagged = visible[0]
        other = visible[1]
    }

    @After
    fun tearDown() = env.close()

    @Test
    fun flaggedAppsComeFirstAndFiltersSplitThem() = runBlocking {
        env.repository.linkProblem(flagged.packageName, flagged.label, fomo)
        val vm = env.viewModel { MainViewModel(env.container) }

        val all = vm.state.await { it.loaded && it.flaggedCount == 1 }
        assertEquals("l'app signalée est en tête", flagged.packageName, all.items.first().app.packageName)
        assertTrue(all.items.first().first)
        assertTrue(all.items.last().last)

        vm.setFilter(AppFilter.FLAGGED)
        assertEquals(listOf(flagged.packageName), vm.state.await { it.filter == AppFilter.FLAGGED }.items.map { it.app.packageName })

        vm.setFilter(AppFilter.UNFLAGGED)
        val unflagged = vm.state.await { it.filter == AppFilter.UNFLAGGED }
        assertFalse(flagged.packageName in unflagged.items.map { it.app.packageName })
        assertTrue(other.packageName in unflagged.items.map { it.app.packageName })
    }

    @Test
    fun searchIgnoresCaseAndSurroundingSpaces() = runBlocking {
        val vm = env.viewModel { MainViewModel(env.container) }
        val total = vm.state.await { it.loaded }.items.size

        vm.setQuery("  ${other.label.uppercase()}  ")

        val found = vm.state.await { it.items.size < total }
        assertTrue(other.packageName in found.items.map { it.app.packageName })
    }

    @Test
    fun aFlaggedSystemAppStaysListedWhateverTheSetting() = runBlocking {
        val shown = env.container.installedApps.list(true).map { it.packageName }.toSet()
        val system = env.container.installedApps.list(false).map { it.packageName }.first { it !in shown }
        env.repository.linkProblem(system, "Système", fomo)
        val vm = env.viewModel { MainViewModel(env.container) }

        val state = vm.state.await { it.loaded && it.flaggedCount == 1 }

        assertTrue(system in state.items.map { it.app.packageName })
    }

    @Test
    fun pickerHidesExcludedAppsAndCountsTheSelection() = runBlocking {
        val vm = env.viewModel(mapOf(AppPickerArgs.EXCLUDED to arrayListOf(flagged.packageName))) { AppPickerViewModel(env.container, it) }

        val rows = vm.rows.await { it.isNotEmpty() }
        assertFalse(flagged.packageName in rows.map { it.app.packageName })

        vm.toggle(other.packageName)
        assertEquals(1, vm.selectedCount.await { it == 1 })
        assertEquals(listOf(other.packageName), vm.selectedApps().map { it.packageName })

        vm.toggle(other.packageName)
        assertEquals(0, vm.selectedCount.await { it == 0 })
    }
}
