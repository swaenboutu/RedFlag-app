package app.redflag.util

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextOrderTest {
    private val french = alphabetical(Locale.FRENCH)

    @Test
    fun `un mot accentue se range avec sa lettre et pas apres le z`() {
        val sorted = listOf("TMobile", "Téléphone", "Zèbre", "Éclair", "Agenda").sortedWith(french)
        assertEquals(listOf("Agenda", "Éclair", "Téléphone", "TMobile", "Zèbre"), sorted)
    }

    @Test
    fun `la casse ne change pas l'ordre alphabetique`() {
        val sorted = listOf("banane", "Cerise", "abricot").sortedWith(french)
        assertEquals(listOf("abricot", "banane", "Cerise"), sorted)
    }

    @Test
    fun `le tri d'une meme liste est stable quelle que soit l'ordre de depart`() {
        val names = listOf("Éclair", "Eau", "Zèbre", "Étoile", "Agenda")
        assertEquals(names.sortedWith(french), names.reversed().sortedWith(french))
    }

    @Test
    fun `chercher sans accent trouve un nom accentue`() {
        assertTrue("Téléphone".matchesSearch("telephone"))
        assertTrue("Téléphone".matchesSearch("tele"))
    }

    @Test
    fun `chercher avec accent trouve un nom sans accent`() {
        assertTrue("Telephone".matchesSearch("téléphone"))
    }

    @Test
    fun `la recherche ignore la casse`() {
        assertTrue("Google".matchesSearch("GOOG"))
        assertTrue("google".matchesSearch("Goo"))
    }

    @Test
    fun `une recherche vide ou faite d'espaces trouve tout`() {
        assertTrue("Chrome".matchesSearch(""))
        assertTrue("Chrome".matchesSearch("   "))
    }

    @Test
    fun `les espaces autour de la recherche sont ignores`() {
        assertTrue("Chrome".matchesSearch("  chro "))
    }

    @Test
    fun `une recherche absente ne trouve rien`() {
        assertFalse("Chrome".matchesSearch("firefox"))
    }

    @Test
    fun `le texte replie perd accents et majuscules`() {
        assertEquals("ecole de l'ete", "École de l'Été".foldForSearch())
    }
}
