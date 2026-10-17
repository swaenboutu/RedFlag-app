package app.redflag.ui

import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.redflag.R
import app.redflag.container
import app.redflag.ui.home.HomeActivity
import app.redflag.ui.settings.FaqActivity
import com.google.android.material.navigation.NavigationBarView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The Home screen, in one scenario: its entry first in the menu and selected, the other tabs there, the drawer opening the FAQ. */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val settings get() = instrumentation.targetContext.container.settings
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
    fun theMenuStartsWithHomeAndTheDrawerOpensTheFaq() {
        val faq = instrumentation.addMonitor(FaqActivity::class.java.name, null, false)
        try {
            ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    val nav = activity.findViewById<NavigationBarView>(R.id.bottomNav)
                    assertNotNull("the menu is there", nav)
                    assertEquals(View.VISIBLE, nav.visibility)
                    assertEquals("four entries", 4, nav.menu.size())
                    assertEquals("Home is the first entry", R.id.nav_home, nav.menu.getItem(0).itemId)
                    assertTrue("and it is the selected one", nav.menu.findItem(R.id.nav_home).isChecked)
                    listOf(R.id.nav_apps, R.id.nav_problems, R.id.nav_stats).forEach {
                        assertNotNull("the other screens still open from the menu", nav.menu.findItem(it))
                    }
                }

                // The FAQ entry of the drawer opens the FAQ screen.
                scenario.onActivity { it.findViewById<View>(R.id.drawerFaq).performClick() }
                val opened = faq.waitForActivityWithTimeout(5_000)
                assertNotNull("the drawer entry opens the FAQ screen", opened)
                assertTrue(opened is FaqActivity)
                instrumentation.runOnMainSync { opened.finish() }
            }
        } finally {
            instrumentation.removeMonitor(faq)
        }
    }
}
