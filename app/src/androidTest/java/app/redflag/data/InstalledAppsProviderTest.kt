package app.redflag.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The list of the phone's apps, with the real PackageManager. */
@RunWith(AndroidJUnit4::class)
class InstalledAppsProviderTest {
    private val provider = InstalledAppsProvider(InstrumentationRegistry.getInstrumentation().targetContext)

    /** A system app that was never updated: in the full list, not in the list "without system apps". */
    private fun aHiddenSystemApp(): String {
        val all = provider.list(hideSystemApps = false).map { it.packageName }.toSet()
        val withoutSystem = provider.list(hideSystemApps = true).map { it.packageName }.toSet()
        return (all - withoutSystem).firstOrNull() ?: error("no system app to hide on this device")
    }

    @Test
    fun hidingSystemAppsShortensTheList() {
        assertTrue(provider.list(hideSystemApps = true).size < provider.list(hideSystemApps = false).size)
    }

    @Test
    fun aFlaggedSystemAppIsTheOnlyOneToStayListedWhenSystemAppsAreHidden() {
        val system = aHiddenSystemApp()
        val without = provider.list(hideSystemApps = true).map { it.packageName }.toSet()
        val withFlag = provider.list(hideSystemApps = true, alwaysInclude = setOf(system)).map { it.packageName }.toSet()

        assertFalse("not flagged, the system app is hidden", system in without)
        assertTrue("the flagged app $system must stay visible", system in withFlag)
        assertEquals("only the flagged app is added", setOf(system), withFlag - without)
    }

    @Test
    fun theAppItselfIsNeverListed() {
        val own = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        assertFalse(own in provider.list(hideSystemApps = false, alwaysInclude = setOf(own)).map { it.packageName })
    }

    @Test
    fun theCountMatchesTheListWithoutLoadingIt() {
        // The statistics count the apps without loading their icons: the number must be the one of the list, whatever the filters.
        val flagged = setOf(aHiddenSystemApp())
        for (hide in listOf(true, false)) {
            assertEquals(provider.list(hide).size, provider.count(hide))
            assertEquals(provider.list(hide, alwaysInclude = flagged).size, provider.count(hide, alwaysInclude = flagged))
        }
    }
}
