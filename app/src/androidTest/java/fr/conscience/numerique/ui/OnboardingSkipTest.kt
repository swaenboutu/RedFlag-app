package fr.conscience.numerique.ui

import android.app.Activity
import android.app.Instrumentation
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.conscience.numerique.R
import fr.conscience.numerique.container
import fr.conscience.numerique.data.ProblemCatalog
import fr.conscience.numerique.data.ProblemRef
import fr.conscience.numerique.ui.apps.AppDetailActivity
import fr.conscience.numerique.ui.apps.MainActivity
import fr.conscience.numerique.ui.onboarding.OnboardingAppDetailActivity
import fr.conscience.numerique.ui.onboarding.OnboardingAppListActivity
import fr.conscience.numerique.ui.onboarding.OnboardingAppsActivity
import fr.conscience.numerique.ui.onboarding.OnboardingPermissionActivity
import fr.conscience.numerique.ui.onboarding.OnboardingProblemsActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Le parcours d'accueil peut être passé à chaque étape : problématiques, applications, autorisations. */
@RunWith(AndroidJUnit4::class)
class OnboardingSkipTest {
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val repository get() = context.container.repository
    private val refs = ProblemCatalog.allRefs
    private var flaggedPackage: String? = null
    private val favoritesBefore = mutableSetOf<ProblemRef>()

    @Before
    fun setUp() = runBlocking {
        favoritesBefore += repository.favorites.first()
    }

    @After
    fun tearDown() = runBlocking {
        refs.forEach { repository.setFavorite(it, it in favoritesBefore) }
        flaggedPackage?.let { repository.unlinkProblem(it, ProblemRef.catalog("fomo")) }
        Unit
    }

    /** Touche [id] et dit si l'écran [next] s'est ouvert en réponse. */
    private inline fun <reified A : Activity, reified N : Activity> ActivityScenario<A>.tapOpens(id: Int): Boolean {
        val monitor = instrumentation.addMonitor(N::class.java.name, null, false)
        try {
            onActivity { it.findViewById<View>(id).performClick() }
            val opened = monitor.waitForActivityWithTimeout(3_000)
            opened?.let { instrumentation.runOnMainSync { it.finish() } }
            return opened != null
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    private fun <A : Activity> ActivityScenario<A>.isEnabled(id: Int): Boolean {
        var enabled = false
        onActivity { enabled = it.findViewById<View>(id).isEnabled }
        return enabled
    }

    private fun <A : Activity> ActivityScenario<A>.visibility(id: Int): Int {
        var value = -1
        onActivity { value = it.findViewById<View>(id).visibility }
        return value
    }

    private fun <A : Activity> ActivityScenario<A>.await(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(50)
    }

    @Test
    fun skippingTheProblemsLeadsToTheListOfAllApps() {
        ActivityScenario.launch(OnboardingProblemsActivity::class.java).use { scenario ->
            assertTrue(scenario.tapOpens<OnboardingProblemsActivity, OnboardingAppListActivity>(R.id.btnSkip))
        }
    }

    @Test
    fun theProblemsStepStillNeedsAChoiceToContinue() = runBlocking {
        refs.forEach { repository.setFavorite(it, false) }
        ActivityScenario.launch(OnboardingProblemsActivity::class.java).use { scenario ->
            scenario.await { !scenario.isEnabled(R.id.btnContinue) }
            assertFalse("rien de choisi : « Continuer » reste désactivé", scenario.isEnabled(R.id.btnContinue))

            repository.setFavorite(refs[0], true)
            scenario.await { scenario.isEnabled(R.id.btnContinue) }
            assertTrue(scenario.isEnabled(R.id.btnContinue))
            assertTrue(scenario.tapOpens<OnboardingProblemsActivity, OnboardingAppsActivity>(R.id.btnContinue))
        }
    }

    @Test
    fun moreThanThreeProblemsCanBeChosenFromTheScreen() = runBlocking {
        refs.forEach { repository.setFavorite(it, false) }
        refs.take(5).forEach { repository.setFavorite(it, true) }

        ActivityScenario.launch(OnboardingProblemsActivity::class.java).use { scenario ->
            scenario.await { scenario.isEnabled(R.id.btnContinue) }

            assertTrue(scenario.isEnabled(R.id.btnContinue))
            assertEquals("plus de compteur ni de limite à l'écran", View.VISIBLE, scenario.visibility(R.id.btnSkip))
        }
    }

    @Test
    fun theListOfAllAppsLeadsToThePermissionsWhateverTheChoice() {
        assumeUserApps(context, 1)
        ActivityScenario.launch(OnboardingAppListActivity::class.java).use { scenario ->
            scenario.await {
                var count = 0
                scenario.onActivity { count = it.findViewById<RecyclerView>(R.id.appList).adapter?.itemCount ?: 0 }
                count > 0
            }
            var count = 0
            scenario.onActivity { count = it.findViewById<RecyclerView>(R.id.appList).adapter?.itemCount ?: 0 }
            assertTrue("les applications installées sont listées", count > 0)

            assertTrue(scenario.tapOpens<OnboardingAppListActivity, OnboardingPermissionActivity>(R.id.btnSkip))
        }
    }

    @Test
    fun nextOnTheListOfAllAppsIsEnabledOnceAnAppIsFlagged() = runBlocking {
        assumeUserApps(context, 1)
        val pkg = context.container.installedApps.list(hideSystemApps = true)
            .first { repository.find(it.packageName) == null }.packageName
        flaggedPackage = pkg

        ActivityScenario.launch(OnboardingAppListActivity::class.java).use { scenario ->
            scenario.await { true }
            val alreadyFlagged = repository.monitoredApps.first().isNotEmpty()
            if (!alreadyFlagged) assertFalse("aucune app signalée : « Suivant » désactivé", scenario.isEnabled(R.id.btnNext))

            repository.linkProblem(pkg, "App", ProblemRef.catalog("fomo"))
            scenario.await { scenario.isEnabled(R.id.btnNext) }

            assertTrue(scenario.isEnabled(R.id.btnNext))
            assertTrue(scenario.tapOpens<OnboardingAppListActivity, OnboardingPermissionActivity>(R.id.btnNext))
        }
    }


    @Test
    fun anAppOfTheListOpensTheOnboardingVersionOfItsDetail() {
        assumeUserApps(context, 1)
        val onboarding = instrumentation.addMonitor(OnboardingAppDetailActivity::class.java.name, null, false)
        val regular = instrumentation.addMonitor(AppDetailActivity::class.java.name, null, false)
        try {
            ActivityScenario.launch(OnboardingAppListActivity::class.java).use { scenario ->
                scenario.await {
                    var ready = false
                    scenario.onActivity { ready = (it.findViewById<RecyclerView>(R.id.appList).adapter?.itemCount ?: 0) > 0 }
                    ready
                }
                scenario.onActivity {
                    it.findViewById<RecyclerView>(R.id.appList).findViewHolderForAdapterPosition(0)!!.itemView.performClick()
                }

                val opened = onboarding.waitForActivityWithTimeout(3_000)

                assertNotNull("l'écran de l'accueil s'ouvre", opened)
                assertEquals("pas la fiche habituelle, avec sa barre du bas", 0, regular.hits)
                instrumentation.runOnMainSync { opened.finish() }
            }
        } finally {
            instrumentation.removeMonitor(onboarding)
            instrumentation.removeMonitor(regular)
        }
    }

    @Test
    fun theOnboardingAppDetailHasNoBottomBarAndNextNeedsAProblem() = runBlocking {
        assumeUserApps(context, 1)
        val app = context.container.installedApps.list(hideSystemApps = true).first { repository.find(it.packageName) == null }
        flaggedPackage = app.packageName
        val intent = OnboardingAppDetailActivity.intent(context, app.packageName, app.label)

        ActivityScenario.launch<OnboardingAppDetailActivity>(intent).use { scenario ->
            var hasBottomBar = true
            scenario.onActivity { hasBottomBar = it.findViewById<View>(R.id.bottomNav) != null }
            assertFalse("pas de menu en bas pendant l'accueil", hasBottomBar)

            scenario.await { true }
            Thread.sleep(300)
            assertFalse("aucune problématique : « Suivant » désactivé", scenario.isEnabled(R.id.btnNext))

            repository.linkProblem(app.packageName, app.label, ProblemRef.catalog("fomo"))
            scenario.await { scenario.isEnabled(R.id.btnNext) }
            assertTrue("une problématique cochée : « Suivant » actif", scenario.isEnabled(R.id.btnNext))

            var finishing = false
            scenario.onActivity {
                it.findViewById<View>(R.id.btnNext).performClick()
                finishing = it.isFinishing // finish() est immédiat : pas besoin d'attendre la destruction, lente sur un émulateur chargé
            }
            assertTrue("« Suivant » ramène à la liste", finishing)
        }
    }

    @Test
    fun skippingTheOnboardingAppDetailGoesBackWithoutChoosing() {
        val intent = OnboardingAppDetailActivity.intent(context, "test.skip.app", "Une app")

        ActivityScenario.launch<OnboardingAppDetailActivity>(intent).use { scenario ->
            var finishing = false
            scenario.onActivity {
                it.findViewById<View>(R.id.btnSkip).performClick()
                finishing = it.isFinishing
            }
            assertTrue("« Passer » ramène à la liste", finishing)
        }
    }

    @Test
    fun theAppsStepCanBeSkippedWholeAndGoesToThePermissions() = runBlocking {
        refs.forEach { repository.setFavorite(it, false) }
        refs.take(3).forEach { repository.setFavorite(it, true) }

        ActivityScenario.launch(OnboardingAppsActivity::class.java).use { scenario ->
            scenario.await { scenario.visibility(R.id.btnSkipRemaining) == View.VISIBLE }
            assertEquals("plusieurs problématiques : on peut passer le reste", View.VISIBLE, scenario.visibility(R.id.btnSkipRemaining))

            assertTrue(scenario.tapOpens<OnboardingAppsActivity, OnboardingPermissionActivity>(R.id.btnSkipRemaining))
        }
    }


    @Test
    fun withTheServiceAlreadyOnTheScreenStaysAndOffersToFinish() {
        ActivityScenario.launch(OnboardingPermissionActivity::class.java).use { scenario ->
            assumeTrue(fr.conscience.numerique.service.isFrictionServiceEnabled(context))
            Thread.sleep(500)

            // Pas passé par les réglages : l'écran n'a pas terminé l'accueil de lui-même.
            assertEquals(androidx.lifecycle.Lifecycle.State.RESUMED, scenario.state)
            assertEquals(View.VISIBLE, scenario.visibility(R.id.status))
        }
    }

    @Test
    fun theSkipButtonOfThePermissionsStepFinishesTheOnboarding() {
        val before = context.container.settings.onboardingDone
        try {
            ActivityScenario.launch(OnboardingPermissionActivity::class.java).use { scenario ->
                // Le bouton « Passer » n'existe que si le service d'accessibilité est désactivé : sinon, le test est ignoré.
                assumeTrue(scenario.visibility(R.id.btnLater) == View.VISIBLE)

                assertTrue(scenario.tapOpens<OnboardingPermissionActivity, MainActivity>(R.id.btnLater))
                assertTrue("l'accueil est marqué comme terminé", context.container.settings.onboardingDone)
            }
        } finally {
            context.container.settings.onboardingDone = before
        }
    }
}
