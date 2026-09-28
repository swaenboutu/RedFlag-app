package fr.conscience.numerique.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProblemParserTest {
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
}
