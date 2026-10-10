package app.redflag.ui

import app.redflag.data.ProblemRef
import app.redflag.ui.home.HomeViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** What the home screen computes: launches avoided, most avoided app, most present issue, state of the interruption. */
class HomeViewModelTest {
    private lateinit var env: ViewModelEnv
    private val fomo = ProblemRef.catalog("fomo")
    private val sexism = ProblemRef.catalog("sexism")

    @Before
    fun setUp() {
        env = ViewModelEnv()
    }

    @After
    fun tearDown() {
        env.close()
    }

    /**
     * The view model, once it has finished loading (including its start-up work, which reads the database): the database must not be
     * closed under a job still running, which would fail an unrelated test that runs next.
     */
    private suspend fun viewModel(): HomeViewModel {
        val vm = env.viewModel { HomeViewModel(env.container, env.context) }
        vm.state.await { it.loaded && it.startedAt > 0L }
        return vm
    }

    @Test
    fun withNothingFlaggedTheHomeScreenIsEmpty() = runBlocking {
        val state = viewModel().state.value

        assertEquals(0, state.avoided)
        assertEquals(0, state.flaggedApps)
        assertNull(state.topApp)
        assertNull(state.topIssue)
    }

    @Test
    fun theLaunchesAvoidedAreTheRefusalsOnly() = runBlocking {
        env.repository.linkProblem("app.a", "Alpha", fomo)
        env.repository.recordChoice("app.a", proceeded = false)
        env.repository.recordChoice("app.a", proceeded = false)
        env.repository.recordChoice("app.a", proceeded = true)
        env.repository.recordChoice("app.a", proceeded = true, snoozed = true)

        val state = viewModel().state.await { it.avoided > 0 }

        assertEquals("only the \"No\" answers count", 2, state.avoided)
    }

    @Test
    fun theMostInterruptedAppWinsAndATieGoesToTheFirstInAlphabeticalOrder() = runBlocking {
        env.repository.linkProblem("app.z", "Zebra", fomo)
        env.repository.linkProblem("app.a", "Alpha", fomo)
        repeat(2) { env.repository.recordChoice("app.z", proceeded = false) }
        repeat(2) { env.repository.recordChoice("app.a", proceeded = true) }

        assertEquals("Alpha", viewModel().state.await { it.topApp != null }.topApp?.label)

        env.repository.recordChoice("app.z", proceeded = true)

        val state = viewModel().state.await { it.topApp?.label == "Zebra" }
        assertEquals("every interruption counts, whatever the answer", 3, state.topApp?.interruptions)
        assertEquals("but only the \"No\" answers are launches avoided", 2, state.avoided)
    }

    @Test
    fun theMostPresentIssueIsTheOneLinkedToTheMostApps() = runBlocking {
        env.repository.linkProblem("app.a", "Alpha", fomo)
        env.repository.linkProblem("app.b", "Beta", fomo)
        env.repository.linkProblem("app.b", "Beta", sexism)

        val state = viewModel().state.await { it.topIssue != null }

        assertEquals(fomo, state.topIssue?.ref)
        assertEquals(2, state.topIssue?.apps)
        assertEquals(2, state.flaggedApps)
    }

    @Test
    fun theStateFollowsTheMainSwitchAndTheAccessibilityService() = runBlocking {
        val vm = viewModel()
        vm.setServiceEnabled(true)
        assertFalse(vm.state.await { it.loaded }.paused)

        env.container.settings.disableInterceptionUntil(System.currentTimeMillis() + 60_000)
        val paused = vm.state.await { it.paused }
        assertTrue("a timed pause says when it ends", paused.resumeAt > System.currentTimeMillis())

        env.container.settings.setInterceptionEnabled(true)
        assertFalse(vm.state.await { !it.paused }.paused)

        vm.setServiceEnabled(false)
        assertFalse("the service is off", vm.state.await { !it.serviceEnabled }.serviceEnabled)
    }
}
