package fr.conscience.numerique.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProblemRefTest {
    @Test
    fun `l'identifiant de favori d'une problematique du catalogue`() {
        assertEquals("catalog:sexism", ProblemRef(catalogKey = "sexism").favoriteId())
    }

    @Test
    fun `l'identifiant de favori d'une problematique personnalisee`() {
        assertEquals("custom:trop de pubs", ProblemRef(customLabel = "trop de pubs").favoriteId())
    }

    @Test
    fun `un identifiant de favori se relit tel quel`() {
        val refs = listOf(
            ProblemRef(catalogKey = "fomo"),
            ProblemRef(customLabel = "trop de pubs"),
            // Le texte libre peut contenir le séparateur : seul le premier préfixe compte.
            ProblemRef(customLabel = "a:b:c"),
            ProblemRef(customLabel = "catalog:piege"),
        )
        refs.forEach { assertEquals(it, favoriteRef(it.favoriteId())) }
    }

    @Test
    fun `un identifiant inconnu n'est pas un favori`() {
        assertNull(favoriteRef("autre:x"))
        assertNull(favoriteRef(""))
    }

    @Test
    fun `une problematique du catalogue et une personnalisee de meme texte restent distinctes`() {
        assertNotEquals(ProblemRef(catalogKey = "fomo").favoriteId(), ProblemRef(customLabel = "fomo").favoriteId())
    }

    @Test
    fun `une problematique associee se compare a sa reference`() {
        val problem = Problem(packageName = "app", catalogKey = "sexism")
        assertTrue(problem.matches(ProblemRef(catalogKey = "sexism")))
        assertFalse(problem.matches(ProblemRef(catalogKey = "racism")))
        assertFalse(problem.matches(ProblemRef(customLabel = "sexism")))
    }

    @Test
    fun `toRef reprend la cle ou le texte libre`() {
        assertEquals(ProblemRef(catalogKey = "fomo"), Problem(packageName = "a", catalogKey = "fomo").toRef())
        assertEquals(ProblemRef(customLabel = "x"), Problem(packageName = "a", customLabel = "x").toRef())
    }
}
