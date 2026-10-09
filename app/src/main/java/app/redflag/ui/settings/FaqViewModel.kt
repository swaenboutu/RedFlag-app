package app.redflag.ui.settings

import androidx.lifecycle.ViewModel
import app.redflag.data.FaqTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Une ligne de la FAQ : un thème (carte repliable), une de ses questions (repliable), puis sa réponse.
 * [last] : dernière ligne de la carte du thème, dont elle arrondit le bas.
 */
sealed interface FaqRow {
    data class Theme(val id: String, val title: String, val questionCount: Int, val expanded: Boolean) : FaqRow

    data class Question(
        val themeId: String,
        val id: String,
        val text: String,
        val expanded: Boolean,
        val first: Boolean,
        val last: Boolean,
    ) : FaqRow

    data class Answer(val themeId: String, val entryId: String, val text: String, val last: Boolean) : FaqRow
}

/** Clé d'une question dans l'ensemble des questions ouvertes : un identifiant de question n'est unique que dans son thème. */
fun faqKey(themeId: String, entryId: String) = "$themeId/$entryId"

/** Les lignes à afficher : les questions d'un thème ouvert, et la réponse d'une question ouverte juste après elle. */
fun faqRows(themes: List<FaqTheme>, openThemes: Set<String>, openQuestions: Set<String>): List<FaqRow> = buildList {
    themes.forEach { theme ->
        val open = theme.id in openThemes
        add(FaqRow.Theme(theme.id, theme.title, theme.entries.size, open))
        if (!open) return@forEach
        theme.entries.forEachIndexed { index, entry ->
            val answered = faqKey(theme.id, entry.id) in openQuestions
            val lastEntry = index == theme.entries.lastIndex
            add(FaqRow.Question(theme.id, entry.id, entry.question, answered, first = index == 0, last = lastEntry && !answered))
            if (answered) add(FaqRow.Answer(theme.id, entry.id, entry.answer, last = lastEntry))
        }
    }
}

/** Ce qui est ouvert : des thèmes et des questions (clés de [faqKey]). */
data class FaqOpen(val themes: Set<String> = emptySet(), val questions: Set<String> = emptySet())

/**
 * « Comment ça marche » : retient ce qui est ouvert. La FAQ elle-même est lue par l'écran, avec ses propres ressources : le ViewModel
 * survit à un changement de langue, mais pas l'écran, qui relit alors la FAQ dans la nouvelle langue.
 */
class FaqViewModel : ViewModel() {
    private val _open = MutableStateFlow(FaqOpen())
    val open: StateFlow<FaqOpen> = _open

    fun toggleTheme(id: String) = _open.update { it.copy(themes = it.themes.toggled(id)) }

    fun toggleQuestion(themeId: String, entryId: String) = _open.update { it.copy(questions = it.questions.toggled(faqKey(themeId, entryId))) }

    private fun Set<String>.toggled(key: String) = if (key in this) this - key else this + key
}
