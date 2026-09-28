package fr.conscience.numerique.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
}
