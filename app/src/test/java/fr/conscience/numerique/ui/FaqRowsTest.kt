package fr.conscience.numerique.ui

import fr.conscience.numerique.data.FaqEntry
import fr.conscience.numerique.data.FaqTheme
import fr.conscience.numerique.ui.settings.FaqRow
import fr.conscience.numerique.ui.settings.faqKey
import fr.conscience.numerique.ui.settings.faqRows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Les lignes de la FAQ : quels thèmes, questions et réponses sont visibles selon ce qui est ouvert. */
class FaqRowsTest {
    private val themes = listOf(
        FaqTheme("a", "Premier", listOf(FaqEntry("q1", "Q1", "R1"), FaqEntry("q2", "Q2", "R2"))),
        FaqTheme("b", "Second", listOf(FaqEntry("q1", "Q1 bis", "R1 bis"))),
    )

    private fun rows(openThemes: Set<String> = emptySet(), openQuestions: Set<String> = emptySet()) =
        faqRows(themes, openThemes, openQuestions)

    @Test
    fun `tout est ferme au depart, seuls les themes sont visibles`() {
        val rows = rows()

        assertEquals(listOf("a", "b"), rows.map { (it as FaqRow.Theme).id })
        assertTrue(rows.none { (it as FaqRow.Theme).expanded })
        assertEquals(listOf(2, 1), rows.map { (it as FaqRow.Theme).questionCount })
    }

    @Test
    fun `un theme ouvert montre ses questions, pas leurs reponses`() {
        val rows = rows(openThemes = setOf("a"))

        assertEquals(listOf("Theme a", "Question q1", "Question q2", "Theme b"), rows.map(::label))
        assertTrue((rows.first() as FaqRow.Theme).expanded)
    }

    @Test
    fun `une question ouverte montre sa reponse juste apres elle`() {
        val rows = rows(openThemes = setOf("a"), openQuestions = setOf(faqKey("a", "q1")))

        assertEquals(listOf("Theme a", "Question q1", "Answer q1", "Question q2", "Theme b"), rows.map(::label))
        assertTrue((rows[1] as FaqRow.Question).expanded)
        assertFalse((rows[3] as FaqRow.Question).expanded)
    }

    @Test
    fun `refermer un theme cache ses questions mais garde leur etat ouvert`() {
        val open = setOf(faqKey("a", "q2"))

        assertEquals(listOf("Theme a", "Theme b"), rows(openThemes = emptySet(), openQuestions = open).map(::label))
        assertTrue(rows(openThemes = setOf("a"), openQuestions = open).any { it is FaqRow.Answer })
    }

    @Test
    fun `une question de meme identifiant dans un autre theme n'est pas ouverte avec elle`() {
        val rows = rows(openThemes = setOf("a", "b"), openQuestions = setOf(faqKey("a", "q1")))

        assertEquals(1, rows.count { it is FaqRow.Answer })
        val other = rows.filterIsInstance<FaqRow.Question>().first { it.themeId == "b" }
        assertFalse(other.expanded)
    }

    @Test
    fun `seule la derniere ligne de la carte arrondit le bas`() {
        // Réponse fermée : la dernière question ferme la carte.
        val closed = rows(openThemes = setOf("a")).filterIsInstance<FaqRow.Question>()
        assertEquals(listOf(true, false), closed.map { it.first })
        assertEquals(listOf(false, true), closed.map { it.last })

        // Réponse de la dernière question ouverte : c'est elle qui ferme la carte, pas la question.
        val open = rows(openThemes = setOf("a"), openQuestions = setOf(faqKey("a", "q2")))
        assertFalse(open.filterIsInstance<FaqRow.Question>().last().last)
        assertTrue(open.filterIsInstance<FaqRow.Answer>().single().last)

        // Réponse d'une question du milieu : elle ne ferme rien.
        val middle = rows(openThemes = setOf("a"), openQuestions = setOf(faqKey("a", "q1")))
        assertFalse(middle.filterIsInstance<FaqRow.Answer>().single().last)
    }

    private fun label(row: FaqRow) = when (row) {
        is FaqRow.Theme -> "Theme ${row.id}"
        is FaqRow.Question -> "Question ${row.id}"
        is FaqRow.Answer -> "Answer ${row.entryId}"
    }
}
