package fr.conscience.numerique

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Vérifie la cohérence des traductions sans lancer l'app : mêmes clés en anglais (par défaut) et en français,
 * mêmes codes de format (%1$s, %d…), formes de pluriel attendues. Un oubli de traduction fait échouer ce test.
 */
class StringResourcesTest {
    private class Resources(val strings: Map<String, String>, val plurals: Map<String, Map<String, String>>)

    private fun load(path: String): Resources {
        val file = File("src/main/res/$path")
        assertTrue("fichier introuvable : ${file.absolutePath}", file.exists())
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val strings = linkedMapOf<String, String>()
        val plurals = linkedMapOf<String, Map<String, String>>()
        val nodes = doc.documentElement.childNodes
        for (i in 0 until nodes.length) {
            val node = nodes.item(i) as? Element ?: continue
            when (node.tagName) {
                "string" -> if (node.getAttribute("translatable") != "false") strings[node.getAttribute("name")] = node.textContent
                "plurals" -> {
                    val items = node.getElementsByTagName("item")
                    plurals[node.getAttribute("name")] = (0 until items.length)
                        .map { items.item(it) as Element }
                        .associate { it.getAttribute("quantity") to it.textContent }
                }
            }
        }
        return Resources(strings, plurals)
    }

    private val en = load("values/strings.xml")
    private val fr = load("values-fr/strings.xml")

    /** Les codes de format d'un texte : `%1$s`, `%2$d`, `%d`… (le `%` seul d'un pourcentage n'en est pas un). */
    private fun placeholders(text: String) =
        Regex("%(\\d+\\$)?[sd]").findAll(text).map { it.value }.sorted().toList()

    @Test
    fun `tous les textes anglais ont leur version francaise`() {
        val missing = en.strings.keys - fr.strings.keys
        assertTrue("textes sans version française : $missing", missing.isEmpty())
    }

    @Test
    fun `aucun texte francais n'est orphelin`() {
        val orphans = fr.strings.keys - en.strings.keys
        assertTrue("textes français sans version anglaise : $orphans", orphans.isEmpty())
    }

    @Test
    fun `tous les pluriels anglais ont leur version francaise`() {
        val missing = en.plurals.keys - fr.plurals.keys
        assertTrue("pluriels sans version française : $missing", missing.isEmpty())
        val orphans = fr.plurals.keys - en.plurals.keys
        assertTrue("pluriels français sans version anglaise : $orphans", orphans.isEmpty())
    }

    @Test
    fun `les codes de format sont les memes dans les deux langues`() {
        val mismatches = en.strings.keys.intersect(fr.strings.keys).filter {
            placeholders(en.strings.getValue(it)) != placeholders(fr.strings.getValue(it))
        }
        assertTrue("codes de format différents entre EN et FR : $mismatches", mismatches.isEmpty())
    }

    @Test
    fun `les pluriels de chaque langue ont les formes attendues`() {
        // Anglais : un / autre. Français : un / many (millions, milliards) / autre.
        en.plurals.forEach { (name, items) ->
            assertEquals("formes EN de $name", setOf("one", "other"), items.keys)
        }
        fr.plurals.forEach { (name, items) ->
            assertEquals("formes FR de $name", setOf("one", "many", "other"), items.keys)
        }
    }

    @Test
    fun `les formes many et other francaises disent la meme chose`() {
        val different = fr.plurals.filter { (_, items) -> items["many"] != items["other"] }.keys
        assertTrue("« many » différent de « other » : $different", different.isEmpty())
    }

    @Test
    fun `les codes de format des pluriels sont les memes dans les deux langues`() {
        // La forme « one » peut s'écrire sans nombre (« la problématique suivante ») : on compare la forme « other ».
        val mismatches = en.plurals.keys.intersect(fr.plurals.keys).filter {
            placeholders(en.plurals.getValue(it).getValue("other")) != placeholders(fr.plurals.getValue(it).getValue("other"))
        }
        assertTrue("codes de format différents dans les pluriels : $mismatches", mismatches.isEmpty())
    }

    @Test
    fun `aucune traduction n'est vide`() {
        val empty = (en.strings + fr.strings).filterValues { it.isBlank() }.keys
        assertTrue("textes vides : $empty", empty.isEmpty())
    }

    @Test
    fun `chaque problematique du catalogue a un libelle dans les deux langues`() {
        val catalogKeys = fr.strings.keys.filter { it.startsWith("problem_") }
        assertEquals("23 problématiques", 23, catalogKeys.size)
        assertEquals(catalogKeys.toSet(), en.strings.keys.filter { it.startsWith("problem_") }.toSet())
    }
}
