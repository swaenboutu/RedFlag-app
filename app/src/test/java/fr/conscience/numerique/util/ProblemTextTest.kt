package fr.conscience.numerique.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProblemTextTest {
    @Test
    fun trimsAndCollapsesWhitespace() {
        assertEquals("trop de pubs", normalizeCustomProblem("  trop   de\tpubs \n"))
    }

    @Test
    fun blankInputGivesNull() {
        assertNull(normalizeCustomProblem("   \n "))
    }

    @Test
    fun truncatesLongInput() {
        assertEquals(60, normalizeCustomProblem("x".repeat(200))?.length)
    }

    @Test
    fun `la coupe ne laisse pas d'espace final`() {
        // Le 60e caractère est une espace : elle est retirée après la coupe.
        val text = "a".repeat(59) + " " + "b".repeat(10)
        assertEquals("a".repeat(59), normalizeCustomProblem(text))
    }

    @Test
    fun `un texte de longueur exacte n'est pas modifie`() {
        val text = "x".repeat(60)
        assertEquals(text, normalizeCustomProblem(text))
    }

    @Test
    fun `les accents et les emojis sont conserves`() {
        assertEquals("Trop d'écrans 📱", normalizeCustomProblem("  Trop d'écrans   📱 "))
    }

    @Test
    fun `un intitule du catalogue de plus de 60 caracteres n'est pas coupe`() {
        // « Predatory monetization (loot boxes, in-app purchases targeting children) » : 75 caractères.
        val original = "Predatory monetization (loot boxes, in-app purchases targeting children)"
        assertEquals(original, normalizeCustomProblem(original, MAX_CATALOG_LENGTH))
    }

    @Test
    fun `les intitules du catalogue tiennent dans la limite large`() {
        assertEquals(true, MAX_CATALOG_LENGTH > 75)
    }

    @Test
    fun `la limite des personnalisees reste de 60`() {
        assertEquals(60, normalizeCustomProblem("x".repeat(200))?.length)
        assertEquals(MAX_CATALOG_LENGTH, normalizeCustomProblem("x".repeat(500), MAX_CATALOG_LENGTH)?.length)
    }

    @Test
    fun `la casse est conservee`() {
        assertEquals("FOMO Maison", normalizeCustomProblem("FOMO   Maison"))
    }
}
