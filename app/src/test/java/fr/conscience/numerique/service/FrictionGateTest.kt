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
    fun `quitter l'app puis y revenir dans le delai ne redemande rien`() {
        gate.allow("a")
        advance(5 * 60_000L) // l'app est fermée par erreur, puis rouverte quelques minutes plus tard
        assertTrue(gate.isAllowed("a"))
    }

    @Test
    fun `une autre app qui passe devant n'efface pas le Oui`() {
        gate.allow("a")
        assertFalse("l'autre app n'est pas autorisée pour autant", gate.isAllowed("b"))
        advance(60_000L)
        assertTrue(gate.isAllowed("a"))
    }

    @Test
    fun `plusieurs apps peuvent etre autorisees en meme temps`() {
        gate.allow("a")
        gate.allow("b")
        assertTrue(gate.isAllowed("a"))
        assertTrue(gate.isAllowed("b"))
    }

    @Test
    fun `le delai court depuis le dernier passage de l'app`() {
        gate.allow("a")
        advance(10 * 60_000L)
        assertTrue("un passage prolonge le Oui", gate.isAllowed("a"))
        advance(10 * 60_000L)
        assertTrue("10 minutes après le dernier passage, c'est encore valable", gate.isAllowed("a"))
    }

    @Test
    fun `revenir apres le delai redemande`() {
        gate.allow("a")
        advance(FrictionGate.ALLOW_MILLIS + 1)
        assertFalse(gate.isAllowed("a"))
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
