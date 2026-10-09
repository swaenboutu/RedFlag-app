package app.redflag.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Quand l'écran d'accueil s'affiche : au premier lancement, ou à chaque lancement si le réglage de debug le force. */
class ShouldShowOnboardingTest {
    private fun show(done: Boolean, always: Boolean, fresh: Boolean) = SettingsStore.shouldShowOnboarding(done, always, fresh)

    @Test
    fun `au premier lancement l'accueil s'affiche`() {
        assertTrue(show(done = false, always = false, fresh = true))
    }

    @Test
    fun `tant que l'accueil n'est pas passe, il s'affiche aussi quand on revient sur l'ecran`() {
        assertTrue(show(done = false, always = false, fresh = false))
    }

    @Test
    fun `une fois passe, il ne revient pas`() {
        assertFalse(show(done = true, always = false, fresh = true))
        assertFalse(show(done = true, always = false, fresh = false))
    }

    @Test
    fun `force, il revient a chaque lancement depuis l'icone`() {
        assertTrue(show(done = true, always = true, fresh = true))
    }

    @Test
    fun `force, il ne revient pas quand on retourne sur un ecran deja ouvert`() {
        assertFalse(show(done = true, always = true, fresh = false))
    }
}
