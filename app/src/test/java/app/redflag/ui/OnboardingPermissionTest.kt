package app.redflag.ui

import app.redflag.ui.onboarding.shouldFinishOnboardingOnReturn
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** End of the welcome tour when coming back from Android's permission settings. */
class OnboardingPermissionTest {
    @Test
    fun `de retour des reglages avec le service active, l'accueil se termine tout seul`() {
        assertTrue(shouldFinishOnboardingOnReturn(openedSettings = true, permissionsGranted = true))
    }

    @Test
    fun `de retour des reglages sans avoir active le service, l'ecran reste`() {
        assertFalse(shouldFinishOnboardingOnReturn(openedSettings = true, permissionsGranted = false))
    }

    @Test
    fun `service deja actif sans etre passe par les reglages, l'ecran reste avec son bouton Terminer`() {
        assertFalse(shouldFinishOnboardingOnReturn(openedSettings = false, permissionsGranted = true))
    }

    @Test
    fun `ni reglages ni service, l'ecran reste`() {
        assertFalse(shouldFinishOnboardingOnReturn(openedSettings = false, permissionsGranted = false))
    }
}
