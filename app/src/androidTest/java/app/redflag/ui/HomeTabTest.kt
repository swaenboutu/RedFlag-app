package app.redflag.ui

import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.redflag.R
import app.redflag.container
import app.redflag.ui.home.HomeActivity
import com.google.android.material.navigation.NavigationBarView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The Home tab: its own entry in the menu, the first one, selected on its screen. */
@RunWith(AndroidJUnit4::class)
class HomeTabTest {
    private val settings get() = InstrumentationRegistry.getInstrumentation().targetContext.container.settings
    private var doneBefore = false

    @Before
    fun setUp() {
        doneBefore = settings.onboardingDone
        settings.onboardingDone = true // otherwise the home screen sends a first launch to the welcome tour
    }

    @After
    fun tearDown() {
        settings.onboardingDone = doneBefore
    }

    @Test
    fun theHomeScreenHasItsEntryFirstInTheMenuAndItIsSelected() {
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val nav = activity.findViewById<NavigationBarView>(R.id.bottomNav)
                assertNotNull("the menu is there", nav)
                assertEquals("Home is the first entry", R.id.nav_home, nav.menu.getItem(0).itemId)
                assertTrue("and it is the selected one", nav.menu.findItem(R.id.nav_home).isChecked)
                assertEquals(5, nav.menu.size())
            }
        }
    }

    @Test
    fun theOtherScreensOfTheMenuStillOpenFromTheHomeEntry() {
        val ids = listOf(R.id.nav_apps, R.id.nav_problems, R.id.nav_stats, R.id.nav_settings)
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val nav = activity.findViewById<NavigationBarView>(R.id.bottomNav)
                ids.forEach { assertNotNull(nav.menu.findItem(it)) }
                assertEquals(View.VISIBLE, nav.visibility)
            }
        }
    }
}
