package fr.conscience.numerique.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProblemCatalogTest {
    private val all = ProblemCatalog.categories.flatMap { it.problems }

    @Test
    fun keysAreUnique() {
        assertEquals(all.size, all.map { it.key }.toSet().size)
    }

    @Test
    fun hasSevenCategoriesAndTwentyThreeProblems() {
        assertEquals(7, ProblemCatalog.categories.size)
        assertEquals(23, all.size)
    }

    @Test
    fun findResolvesKnownKeysOnly() {
        assertNotNull(ProblemCatalog.find("sexism"))
        assertNull(ProblemCatalog.find("unknown"))
    }

    @Test
    fun `les cles de theme sont uniques et retrouvables`() {
        val keys = ProblemCatalog.categories.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        keys.forEach { assertNotNull(ProblemCatalog.findCategory(it)) }
        assertNull(ProblemCatalog.findCategory("inconnu"))
    }

    @Test
    fun `allRefs liste tout le catalogue dans l'ordre des themes`() {
        assertEquals(all.map { ProblemRef(catalogKey = it.key) }, ProblemCatalog.allRefs)
    }

    @Test
    fun `une personnalisee rangee dans un theme suit les problematiques du catalogue`() {
        val mental = ProblemCatalog.categories.first()
        val custom = listOf(CustomProblem("mon souci", category = mental.key))

        val content = ProblemCatalog.themeContents(custom).first()

        assertEquals(mental.refs + ProblemRef(customLabel = "mon souci"), content.refs)
    }

    @Test
    fun `sans personnalisee, un theme ne contient que le catalogue`() {
        ProblemCatalog.themeContents(emptyList()).forEach { assertEquals(it.category.refs, it.refs) }
    }

    @Test
    fun `une personnalisee sans theme n'apparait dans aucun theme mais dans Personnalise`() {
        val custom = listOf(CustomProblem("sans theme"))

        assertTrue(ProblemCatalog.themeContents(custom).none { ProblemRef(customLabel = "sans theme") in it.refs })
        assertEquals(listOf(ProblemRef(customLabel = "sans theme")), ProblemCatalog.customWithoutTheme(custom))
    }

    @Test
    fun `une personnalisee dont le theme n'existe plus retombe dans Personnalise`() {
        val custom = listOf(CustomProblem("orpheline", category = "theme_supprime"))

        assertEquals(listOf(ProblemRef(customLabel = "orpheline")), ProblemCatalog.customWithoutTheme(custom))
        assertFalse(ProblemCatalog.themeContents(custom).any { ProblemRef(customLabel = "orpheline") in it.refs })
    }

    @Test
    fun `une personnalisee n'est rangee qu'a un seul endroit`() {
        val mental = ProblemCatalog.categories.first()
        val custom = listOf(
            CustomProblem("rangee", category = mental.key),
            CustomProblem("libre"),
            CustomProblem("orpheline", category = "theme_supprime"),
        )
        val placed = ProblemCatalog.themeContents(custom).flatMap { it.refs } + ProblemCatalog.customWithoutTheme(custom)

        listOf("rangee", "libre", "orpheline").forEach { label ->
            assertEquals(label, 1, placed.count { it == ProblemRef(customLabel = label) })
        }
    }
}
