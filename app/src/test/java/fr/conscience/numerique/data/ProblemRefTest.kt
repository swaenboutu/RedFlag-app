package fr.conscience.numerique.data

import fr.conscience.numerique.ui.problems.toRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProblemRefTest {
    @Test
    fun `une problematique du catalogue a pour identifiant sa cle`() {
        val ref = ProblemRef.catalog("sexism")

        assertEquals("sexism", ref.id)
        assertEquals("sexism", ref.catalogKey)
        assertFalse(ref.isCustom)
    }

    @Test
    fun `une problematique personnalisee a un identifiant generé et pas de cle de catalogue`() {
        val ref = ProblemRef.newCustom()

        assertTrue(ref.id.startsWith(CUSTOM_PREFIX))
        assertTrue(ref.isCustom)
        assertNull(ref.catalogKey)
    }

    @Test
    fun `chaque nouvelle personnalisee recoit un identifiant different`() {
        val ids = List(200) { ProblemRef.newCustom().id }

        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `un identifiant personnalise ne peut pas etre confondu avec une cle du catalogue`() {
        // Les clés du catalogue ne contiennent jamais « : », le préfixe des personnalisées.
        ProblemCatalog.allRefs.forEach { ref ->
            assertFalse(ref.id, ref.isCustom)
            assertFalse(ref.id, ref.id.contains(':'))
        }
        assertNotEquals(ProblemRef.catalog("fomo"), ProblemRef.newCustom())
    }

    @Test
    fun `une problematique associee se compare a sa reference`() {
        val problem = Problem(packageName = "app", problemId = "sexism")

        assertTrue(problem.matches(ProblemRef.catalog("sexism")))
        assertFalse(problem.matches(ProblemRef.catalog("racism")))
        assertFalse(problem.matches(ProblemRef("custom:sexism")))
    }

    @Test
    fun `toRef reprend l'identifiant`() {
        assertEquals(ProblemRef.catalog("fomo"), Problem(packageName = "a", problemId = "fomo").toRef())
        assertEquals(ProblemRef("custom:x"), Problem(packageName = "a", problemId = "custom:x").toRef())
    }
}
