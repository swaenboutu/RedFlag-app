package fr.conscience.numerique.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Google Play exige un avertissement et un consentement explicite avant de demander le service d'accessibilité. Les réglages d'Android
 * ne doivent donc s'ouvrir que depuis l'écran des autorisations de l'accueil et depuis la fenêtre d'avertissement : ce test échoue si
 * un nouvel écran les ouvre directement.
 */
class AccessibilityDisclosureGuardTest {
    @Test
    fun `les reglages d'accessibilite ne s'ouvrent que depuis un ecran qui avertit`() {
        val sources = File("src/main/java").walkTopDown().filter { it.isFile && it.extension == "kt" }
        val opening = sources
            .filter { "ACTION_ACCESSIBILITY_SETTINGS" in it.readText() }
            .map { it.name }
            .sorted()
            .toList()

        assertEquals(listOf("Dialogs.kt", "OnboardingPermissionActivity.kt"), opening)
    }

    @Test
    fun `l'ecran principal et les reglages ouvrent la fenetre d'avertissement`() {
        val main = File("src/main/java/fr/conscience/numerique/ui/MainActivity.kt").readText()
        val settings = File("src/main/java/fr/conscience/numerique/ui/SettingsActivity.kt").readText()

        assertTrue("MainActivity", "AccessibilityDisclosureDialog.show" in main)
        assertTrue("SettingsActivity", "AccessibilityDisclosureDialog.show" in settings)
    }
}
