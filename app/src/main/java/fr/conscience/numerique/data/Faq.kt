package fr.conscience.numerique.data

import android.content.Context
import fr.conscience.numerique.R
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** Une question de la FAQ et sa réponse ; [answer] : paragraphes séparés par une ligne vide. */
data class FaqEntry(val id: String, val question: String, val answer: String)

/** Une zone pliable de la FAQ ; [id] est stable (il relie les langues entre elles) et n'est jamais affiché. */
data class FaqTheme(val id: String, val title: String, val entries: List<FaqEntry>)

/**
 * Lit la FAQ, écrite dans `res/raw/faq.xml` (anglais) et `res/raw-fr/faq.xml` : Android choisit le fichier selon la langue,
 * comme pour les textes. Un fichier mal formé (identifiant manquant ou en double, texte vide) lève une `IllegalArgumentException`
 * qui dit où : le test des ressources l'attrape avant la publication.
 */
object FaqParser {
    fun parse(input: InputStream): List<FaqTheme> {
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input).documentElement
        require(root.tagName == "faq") { "la racine doit être <faq>, pas <${root.tagName}>" }

        val themes = root.children("theme").map { theme ->
            val themeId = theme.requiredAttribute("id", "un <theme>")
            val title = theme.requiredAttribute("title", "le thème « $themeId »")
            val entries = theme.children("entry").map { entry ->
                val entryId = entry.requiredAttribute("id", "une <entry> du thème « $themeId »")
                val where = "« $themeId/$entryId »"
                FaqEntry(entryId, entry.requiredText("question", where), entry.requiredText("answer", where))
            }
            requireUnique(entries.map { it.id }, "identifiants de questions du thème « $themeId »")
            require(entries.isNotEmpty()) { "le thème « $themeId » n'a aucune question" }
            FaqTheme(themeId, title, entries)
        }
        requireUnique(themes.map { it.id }, "identifiants de thèmes")
        return themes
    }

    /** Une ligne vide sépare deux paragraphes ; à l'intérieur d'un paragraphe, retours à la ligne et indentation disparaissent. */
    fun paragraphs(raw: String): String = raw.trim()
        .split(Regex("\\n\\s*\\n"))
        .map { paragraph -> paragraph.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ") }
        .filter { it.isNotEmpty() }
        .joinToString("\n\n")

    private fun Element.children(tag: String): List<Element> {
        val nodes = childNodes
        return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }.filter { it.tagName == tag }
    }

    private fun Element.requiredAttribute(name: String, owner: String): String =
        getAttribute(name).trim().also { require(it.isNotEmpty()) { "$owner n'a pas d'attribut « $name »" } }

    private fun Element.requiredText(tag: String, where: String): String {
        val element = children(tag).singleOrNull() ?: throw IllegalArgumentException("$where doit avoir exactement un <$tag>")
        return paragraphs(element.textContent).also { require(it.isNotEmpty()) { "<$tag> de $where est vide" } }
    }

    private fun requireUnique(ids: List<String>, what: String) {
        val duplicates = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        require(duplicates.isEmpty()) { "$what en double : $duplicates" }
    }
}

/** La FAQ dans la langue courante. */
fun Context.loadFaq(): List<FaqTheme> = resources.openRawResource(R.raw.faq).use(FaqParser::parse)
