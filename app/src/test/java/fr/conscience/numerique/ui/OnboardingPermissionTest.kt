package fr.conscience.numerique.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fin de l'accueil au retour des réglages d'accessibilité. */
class OnboardingPermissionTest {
    @Test
    fun `de retour des reglages avec le service active, l'accueil se termine tout seul`() {
        assertTrue(shouldFinishOnboardingOnReturn(openedSettings = true, serviceEnabled = true))
    }

    @Test
    fun `de retour des reglages sans avoir active le service, l'ecran reste`() {
        assertFalse(shouldFinishOnboardingOnReturn(openedSettings = true, serviceEnabled = false))
    }

    @Test
    fun `service deja actif sans etre passe par les reglages, l'ecran reste avec son bouton Terminer`() {
        assertFalse(shouldFinishOnboardingOnReturn(openedSettings = false, serviceEnabled = true))
    }

    @Test
    fun `ni reglages ni service, l'ecran reste`() {
        assertFalse(shouldFinishOnboardingOnReturn(openedSettings = false, serviceEnabled = false))
    }
}
