package fr.conscience.numerique.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrictionGateTest {
    private val gate = FrictionGate()

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
}
