package app.redflag.ui

import app.redflag.data.ProblemCatalog
import app.redflag.data.ProblemRef
import app.redflag.data.toRef
import app.redflag.ui.onboarding.AppsRow
import app.redflag.ui.onboarding.OnboardingAppsViewModel
import app.redflag.ui.onboarding.OnboardingProblemsViewModel
import app.redflag.ui.onboarding.OnboardingRow
import app.redflag.ui.problems.toRef
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** L'accueil : choix des problématiques (limité à trois), puis des applications concernées. */
class OnboardingViewModelsTest {
    private lateinit var env: ViewModelEnv
    private val refs = ProblemCatalog.allRefs

    @Before
    fun setUp() {
        env = ViewModelEnv()
    }

    @After
    fun tearDown() = env.close()

    @Test
    fun anyNumberOfProblemsCanBeSelected() = runBlocking {
        val vm = env.viewModel { OnboardingProblemsViewModel(env.container) }
        vm.rows.await { true } // abonne le ViewModel
        val chosen = refs.take(8)

        chosen.forEachIndexed { i, ref ->
            vm.toggle(ref, checked = false)
            vm.selectedCount.await { it == i + 1 }
        }

        assertEquals("plus de limite à trois", 8, vm.selectedCount.value)
        assertEquals(chosen.toSet(), env.repository.favorites.first())
    }

    @Test
    fun uncheckingAProblemRemovesItFromTheSelection() = runBlocking {
        val vm = env.viewModel { OnboardingProblemsViewModel(env.container) }
        vm.rows.await { true }
        refs.take(5).forEach { env.repository.setFavorite(it, true) }
        vm.selectedCount.await { it == 5 }
        vm.toggleSection(ProblemCatalog.categories.first().title) // ouvre le thème pour voir ses cases

        val rows = vm.rows.await { rows -> rows.any { it is OnboardingRow.Choice } }
        assertTrue("des cases sont cochées", rows.filterIsInstance<OnboardingRow.Choice>().any { it.checked })

        vm.toggle(refs[0], checked = true)

        assertEquals(4, vm.selectedCount.await { it == 4 })
        assertFalse(refs[0] in env.repository.favorites.first())
    }

    @Test
    fun theAppsStepsFollowTheChosenProblemsAndNextSavesTheChoice() = runBlocking {
        assumeUserApps(env.context, 1)
        env.repository.setFavorite(refs[0], true)
        env.repository.setFavorite(refs[1], true)
        val vm = env.viewModel { OnboardingAppsViewModel(env.container, env.context) }

        assertEquals(2, vm.stepCount.await { it != null })
        val rows = vm.rows.await { it.any { row -> row is AppsRow.Choice } }
        val header = rows.filterIsInstance<AppsRow.Header>().single()
        assertEquals(1, header.step.position)
        assertEquals(2, header.step.total)
        assertEquals(refs[0], header.step.ref)
        assertFalse("rien de coché : « Suivant » n'a rien à enregistrer", vm.canSave.await { true })

        val app = rows.filterIsInstance<AppsRow.Choice>().first().app
        vm.toggle(app.packageName)
        vm.canSave.await { it }
        var finished = false
        vm.next { finished = true }
        vm.rows.await { (it.first() as AppsRow.Header).step.position == 2 }

        val saved = env.repository.find(app.packageName)
        assertEquals(listOf(refs[0]), saved?.problems?.map { p -> p.toRef() })
        assertFalse("il reste une étape", finished)
    }

    @Test
    fun skippingLeavesNothingBehindAndLastStepFinishes() = runBlocking {
        env.repository.setFavorite(refs[0], true)
        val vm = env.viewModel { OnboardingAppsViewModel(env.container, env.context) }
        vm.stepCount.await { it == 1 }
        var finished = false

        vm.skip { finished = true }

        withTimeoutPoll { finished }
        assertTrue(finished)
        assertTrue(env.repository.monitoredApps.first().isEmpty())
    }

    @Test
    fun theTourHandlesAtMostThreeIssuesAndTheLastPageOffersToFinishLater() = runBlocking {
        refs.take(5).forEach { env.repository.setFavorite(it, true) }
        val vm = env.viewModel { OnboardingAppsViewModel(env.container, env.context) }

        assertEquals("only the first three are handled", 3, vm.stepCount.await { it != null })
        val first = vm.bottom.await { it.chosenTotal == 5 }
        assertFalse("not the last page yet", first.finishLater)
        assertEquals("the other two wait in the Issues tab", 2, first.remaining)

        vm.skip { }
        vm.rows.await { (it.firstOrNull() as? AppsRow.Header)?.step?.position == 2 }
        vm.skip { }
        vm.rows.await { (it.firstOrNull() as? AppsRow.Header)?.step?.position == 3 }

        assertTrue("last page with issues left: finish later", vm.bottom.await { it.finishLater }.finishLater)
    }

    private fun withTimeoutPoll(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(50)
    }
}
