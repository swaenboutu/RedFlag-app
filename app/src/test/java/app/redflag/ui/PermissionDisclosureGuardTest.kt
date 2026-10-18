package app.redflag.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Android's permission screens ("Usage access", "Display over other apps") must only open after an explanation and an explicit
 * consent: from the permission screen of the welcome tour, or from the warning window. This test fails if another screen opens them
 * directly.
 */
class PermissionDisclosureGuardTest {
    private val sources = File("src/main/java").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun filesContaining(text: String) = sources.filter { text in it.readText() }.map { it.name }.sorted()

    @Test
    fun `the permission settings are only named where they are opened`() {
        assertEquals(listOf("Permissions.kt"), filesContaining("ACTION_USAGE_ACCESS_SETTINGS"))
        assertEquals(listOf("Permissions.kt"), filesContaining("ACTION_MANAGE_OVERLAY_PERMISSION"))
        assertEquals(listOf("Permissions.kt"), filesContaining("settingsAction("))
    }

    @Test
    fun `they are only opened from a screen that explains them`() {
        assertEquals(
            listOf("Dialogs.kt", "OnboardingPermissionActivity.kt", "Permissions.kt"),
            filesContaining("openPermissionSettings("),
        )
    }

    @Test
    fun `the home screen and the settings open the warning window`() {
        val home = File("src/main/java/app/redflag/ui/home/HomeActivity.kt").readText()
        val settings = File("src/main/java/app/redflag/ui/settings/SettingsActivity.kt").readText()
        assertTrue("HomeActivity", "PermissionDisclosureDialog.show" in home)
        assertTrue("SettingsActivity", "PermissionDisclosureDialog.show" in settings)
    }
}
