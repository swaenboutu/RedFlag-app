package fr.conscience.numerique.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrictionGateTest {
    private var time = 1_000_000L
    private val gate = FrictionGate { time }

    private fun advance(millis: Long) {
        time += millis
    }

    @Test
    fun `rien n'est autorise au depart`() {
        assertFalse(gate.isAllowed("a"))
    }

    @Test
    fun `apres un Oui, l'app reste autorisee`() {
        gate.allow("a")
        assertTrue(gate.isAllowed("a"))
        assertFalse(gate.isAllowed("b"))
    }

    @Test
    fun `tant que l'app reste au premier plan, l'autorisation est conservee`() {
        gate.allow("a")
        gate.onOtherAppForeground("a")
        assertTrue(gate.isAllowed("a"))
    }

    @Test
    fun `des qu'une autre app passe devant, l'autorisation est oubliee`() {
        gate.allow("a")
        gate.onOtherAppForeground("b")
        assertFalse(gate.isAllowed("a"))
    }

    @Test
    fun `sans autorisation, un changement d'app ne fait rien`() {
        gate.onOtherAppForeground("b")
        assertFalse(gate.isAllowed("b"))
    }

    @Test
    fun `autoriser une deuxieme app remplace la premiere`() {
        gate.allow("a")
        gate.allow("b")
        assertFalse(gate.isAllowed("a"))
        assertTrue(gate.isAllowed("b"))
    }

    // --- Un « Oui » n'est pas éternel ---

    @Test
    fun `un Oui expire apres un long moment sans activite`() {
        gate.allow("a")
        advance(FrictionGate.ALLOW_MILLIS - 1)
        assertTrue(gate.isAllowed("a"))
    }

    @Test
    fun `un Oui n'est plus valable une fois la duree ecoulee`() {
        gate.allow("a")
        advance(FrictionGate.ALLOW_MILLIS)
        assertFalse(gate.isAllowed("a"))
    }

    @Test
    fun `chaque passage de l'app prolonge le Oui tant qu'elle est utilisee`() {
        gate.allow("a")
        repeat(5) {
            advance(FrictionGate.ALLOW_MILLIS - 1)
            assertTrue("l'app est toujours utilisée", gate.isAllowed("a"))
        }
    }

    @Test
    fun `un Oui expire reste oublie meme si l'app revient`() {
        gate.allow("a")
        advance(FrictionGate.ALLOW_MILLIS)
        assertFalse(gate.isAllowed("a"))
        assertFalse(gate.isAllowed("a"))
    }

    @Test
    fun `l'extinction de l'ecran oublie tous les Oui`() {
        gate.allow("a")
        gate.revokeAll()
        assertFalse(gate.isAllowed("a"))
    }

    // --- Un « Non » ne doit pas relancer l'écran en boucle ---

    @Test
    fun `juste apres un Non, l'app est ignoree`() {
        gate.declined("a")
        assertTrue(gate.isDeclineGuarded("a"))
        assertFalse("une autre app n'est pas concernée", gate.isDeclineGuarded("b"))
    }

    @Test
    fun `le delai de garde apres un Non se termine`() {
        gate.declined("a")
        advance(FrictionGate.DECLINE_GUARD_MILLIS)
        assertFalse(gate.isDeclineGuarded("a"))
    }

    @Test
    fun `un Non retire aussi un Oui precedent`() {
        gate.allow("a")
        gate.declined("a")
        assertFalse(gate.isAllowed("a"))
    }

    @Test
    fun `sans Non, aucune app n'est protegee par le delai de garde`() {
        assertFalse(gate.isDeclineGuarded("a"))
    }
}
