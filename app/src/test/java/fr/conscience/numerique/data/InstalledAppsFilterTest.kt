package fr.conscience.numerique.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Quelles apps la liste affiche selon le réglage « Liste affichée » et les apps signalées. */
class InstalledAppsFilterTest {
    private fun listed(purelySystem: Boolean, hide: Boolean, flagged: Set<String> = emptySet(), pkg: String = "app") =
        InstalledAppsProvider.isListed(pkg, purelySystem, hide, flagged)

    @Test
    fun `une app normale est toujours listee`() {
        assertTrue(listed(purelySystem = false, hide = true))
        assertTrue(listed(purelySystem = false, hide = false))
    }

    @Test
    fun `une app systeme est masquee quand le reglage le demande`() {
        assertFalse(listed(purelySystem = true, hide = true))
    }

    @Test
    fun `une app systeme est listee quand le reglage les affiche`() {
        assertTrue(listed(purelySystem = true, hide = false))
    }

    @Test
    fun `une app systeme signalee reste listee meme si le reglage les masque`() {
        assertTrue(listed(purelySystem = true, hide = true, flagged = setOf("app")))
    }

    @Test
    fun `seule l'app signalee est concernee, pas les autres apps systeme`() {
        assertFalse(listed(purelySystem = true, hide = true, flagged = setOf("autre"), pkg = "app"))
    }

    @Test
    fun `une app signalee qui n'est pas systeme est listee dans tous les cas`() {
        assertTrue(listed(purelySystem = false, hide = true, flagged = setOf("app")))
    }
}
