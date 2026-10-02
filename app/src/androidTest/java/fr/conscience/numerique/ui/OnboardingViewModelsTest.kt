package fr.conscience.numerique.ui

import fr.conscience.numerique.data.ProblemCatalog
import fr.conscience.numerique.data.ProblemRef
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
    fun atMostThreeProblemsCanBeSelected() = runBlocking {
        val vm = env.viewModel { OnboardingProblemsViewModel(env.container) }
        vm.rows.await { true } // abonne le ViewModel
        repeat(OnboardingProblemsViewModel.MAX_SELECTION) { i ->
            vm.toggle(refs[i], checked = false)
            vm.selectedCount.await { it == i + 1 }
        }

        vm.toggle(refs[3], checked = false)
        Thread.sleep(300)

        assertEquals(OnboardingProblemsViewModel.MAX_SELECTION, vm.selectedCount.value)
        assertFalse("la quatrième n'est pas enregistrée", refs[3] in env.repository.favorites.first())
    }

    @Test
    fun uncheckingFreesASlotAndOtherBoxesAreDisabledWhenFull() = runBlocking {
        val vm = env.viewModel { OnboardingProblemsViewModel(env.container) }
        vm.rows.await { true }
        refs.take(3).forEach { env.repository.setFavorite(it, true) }
        vm.selectedCount.await { it == 3 }
        vm.toggleSection(ProblemCatalog.categories.first().title) // ouvre le thème pour voir ses cases

        val full = vm.rows.await { rows -> rows.any { it is OnboardingRow.Choice } }
        val choices = full.filterIsInstance<OnboardingRow.Choice>()
        assertTrue("cases cochées toujours décochables", choices.filter { it.checked }.all { it.selectable })
        assertTrue("limite atteinte : les autres sont grisées", choices.filter { !it.checked }.none { it.selectable })

        vm.toggle(refs[0], checked = true)
        vm.selectedCount.await { it == 2 }
        vm.toggle(refs[3], checked = false)
        assertEquals(3, vm.selectedCount.await { it == 3 })
    }

    @Test
    fun theAppsStepsFollowTheChosenProblemsAndNextSavesTheChoice() = runBlocking {
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
        assertEquals(listOf(refs[0]), saved?.problems?.map { p -> ProblemRef(p.catalogKey, p.customLabel) })
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

    private fun withTimeoutPoll(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(50)
    }
}
