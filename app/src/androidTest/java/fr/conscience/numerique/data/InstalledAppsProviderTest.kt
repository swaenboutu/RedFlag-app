package fr.conscience.numerique.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** La liste des apps du téléphone, avec le vrai PackageManager. */
@RunWith(AndroidJUnit4::class)
class InstalledAppsProviderTest {
    private val provider = InstalledAppsProvider(InstrumentationRegistry.getInstrumentation().targetContext)

    /** Une app système non mise à jour : présente dans la liste complète, absente de la liste « sans apps système ». */
    private fun aHiddenSystemApp(): String {
        val all = provider.list(hideSystemApps = false).map { it.packageName }.toSet()
        val withoutSystem = provider.list(hideSystemApps = true).map { it.packageName }.toSet()
        return (all - withoutSystem).firstOrNull() ?: error("aucune app système à masquer sur cet appareil")
    }

    @Test
    fun hidingSystemAppsShortensTheList() {
        assertTrue(provider.list(hideSystemApps = true).size < provider.list(hideSystemApps = false).size)
    }

    @Test
    fun aFlaggedSystemAppStaysListedWhenSystemAppsAreHidden() {
        val system = aHiddenSystemApp()

        val shown = provider.list(hideSystemApps = true, alwaysInclude = setOf(system)).map { it.packageName }

        assertTrue("l'app signalée $system doit rester visible", system in shown)
    }

    @Test
    fun onlyTheFlaggedSystemAppIsAdded() {
        val system = aHiddenSystemApp()

        val withFlag = provider.list(hideSystemApps = true, alwaysInclude = setOf(system)).map { it.packageName }.toSet()
        val without = provider.list(hideSystemApps = true).map { it.packageName }.toSet()

        assertTrue(withFlag - without == setOf(system))
    }

    @Test
    fun withoutFlagTheSystemAppIsHidden() {
        assertFalse(aHiddenSystemApp() in provider.list(hideSystemApps = true).map { it.packageName })
    }

    @Test
    fun theAppItselfIsNeverListed() {
        val own = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        assertFalse(own in provider.list(hideSystemApps = false, alwaysInclude = setOf(own)).map { it.packageName })
    }
}
