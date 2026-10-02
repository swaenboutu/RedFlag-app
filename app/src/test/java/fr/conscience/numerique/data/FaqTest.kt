package fr.conscience.numerique.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** La lecture de la FAQ (`res/raw/faq.xml`) et la cohérence de ses deux langues. */
class FaqTest {
    private fun parse(xml: String) = FaqParser.parse(xml.byteInputStream())

    private fun resource(path: String): File {
        val file = File("src/main/res/$path")
        assertTrue("fichier introuvable : ${file.absolutePath}", file.exists())
        return file
    }

    @Test
    fun `une FAQ se lit avec ses themes, ses questions et ses reponses dans l'ordre`() {
        val themes = parse(
            """
            <faq>
              <theme id="a" title="Premier">
                <entry id="q1"><question>Pourquoi ?</question><answer>Parce que.</answer></entry>
                <entry id="q2"><question>Comment ?</question><answer>Ainsi.</answer></entry>
              </theme>
              <theme id="b" title="Second">
                <entry id="q1"><question>Quand ?</question><answer>Demain.</answer></entry>
              </theme>
            </faq>
            """.trimIndent(),
        )

        assertEquals(listOf("a", "b"), themes.map { it.id })
        assertEquals("Premier", themes[0].title)
        assertEquals(listOf("q1", "q2"), themes[0].entries.map { it.id })
        assertEquals(FaqEntry("q1", "Quand ?", "Demain."), themes[1].entries.single())
    }

    @Test
    fun `une ligne vide separe deux paragraphes, les retours a la ligne simples disparaissent`() {
        val raw = """

            Première ligne
               de la première phrase.

            Second paragraphe.

        """.trimIndent()

        assertEquals("Première ligne de la première phrase.\n\nSecond paragraphe.", FaqParser.paragraphs(raw))
    }

    @Test
    fun `les accents, les guillemets et les symboles sont conserves`() {
        val themes = parse(
            """<faq><theme id="a" title="Éthique"><entry id="q"><question>« Qu’est-ce que c’est ? »</question>""" +
                """<answer>Données &amp; vie privée : 100 %</answer></entry></theme></faq>""",
        )

        assertEquals("Éthique", themes.single().title)
        assertEquals("« Qu’est-ce que c’est ? »", themes.single().entries.single().question)
        assertEquals("Données & vie privée : 100 %", themes.single().entries.single().answer)
    }

    @Test
    fun `un fichier mal forme est refuse avec un message qui dit ou`() {
        fun message(xml: String) = assertThrows(IllegalArgumentException::class.java) { parse(xml) }.message.orEmpty()

        assertTrue(message("<autre/>").contains("<faq>"))
        assertTrue(message("""<faq><theme title="T"/></faq>""").contains("id"))
        assertTrue(message("""<faq><theme id="a" title="T"/></faq>""").contains("aucune question"))
        assertTrue(message("""<faq><theme id="a" title="T"><entry id="q"><answer>R</answer></entry></theme></faq>""").contains("<question>"))
        assertTrue(message("""<faq><theme id="a" title="T"><entry id="q"><question>Q</question><answer>  </answer></entry></theme></faq>""").contains("vide"))
    }

    @Test
    fun `deux themes ou deux questions du meme theme ne peuvent pas partager un identifiant`() {
        val entry = """<entry id="q"><question>Q</question><answer>R</answer></entry>"""

        assertThrows(IllegalArgumentException::class.java) {
            parse("""<faq><theme id="a" title="T">$entry</theme><theme id="a" title="U">$entry</theme></faq>""")
        }
        assertThrows(IllegalArgumentException::class.java) {
            parse("""<faq><theme id="a" title="T">$entry$entry</theme></faq>""")
        }
        // Le même identifiant de question dans deux thèmes différents est permis.
        assertEquals(2, parse("""<faq><theme id="a" title="T">$entry</theme><theme id="b" title="U">$entry</theme></faq>""").size)
    }

    @Test
    fun `les fichiers anglais et francais de la FAQ sont valides`() {
        listOf("raw/faq.xml", "raw-fr/faq.xml").forEach { path ->
            val themes = resource(path).inputStream().use(FaqParser::parse)
            assertTrue("$path : au moins un thème", themes.isNotEmpty())
        }
    }

    @Test
    fun `la FAQ francaise a les memes themes et les memes questions que l'anglaise, dans le meme ordre`() {
        val en = resource("raw/faq.xml").inputStream().use(FaqParser::parse)
        val fr = resource("raw-fr/faq.xml").inputStream().use(FaqParser::parse)

        assertEquals("thèmes", en.map { it.id }, fr.map { it.id })
        en.zip(fr).forEach { (english, french) ->
            assertEquals("questions du thème ${english.id}", english.entries.map { it.id }, french.entries.map { it.id })
        }
    }

    @Test
    fun `aucune question ni reponse de la FAQ n est restee non traduite`() {
        // Un texte resté en anglais dans le fichier français trahit une traduction oubliée.
        val en = resource("raw/faq.xml").inputStream().use(FaqParser::parse)
        val fr = resource("raw-fr/faq.xml").inputStream().use(FaqParser::parse)

        en.zip(fr).forEach { (english, french) ->
            english.entries.zip(french.entries).forEach { (e, f) ->
                assertFalse("question non traduite : ${e.id}", e.question == f.question)
                assertFalse("réponse non traduite : ${e.id}", e.answer == f.answer)
            }
        }
    }
}
