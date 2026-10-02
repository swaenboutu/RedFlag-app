package fr.conscience.numerique.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.conscience.numerique.data.FaqTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

/** « Comment ça marche » : la FAQ, avec ses thèmes et ses questions fermés au départ. */
class FaqViewModel(private val themes: List<FaqTheme>) : ViewModel() {
    private val openThemes = MutableStateFlow(emptySet<String>())
    private val openQuestions = MutableStateFlow(emptySet<String>())

    val rows: StateFlow<List<FaqRow>> = combine(openThemes, openQuestions) { themesOpen, questionsOpen ->
        faqRows(themes, themesOpen, questionsOpen)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), faqRows(themes, emptySet(), emptySet()))

    fun toggleTheme(id: String) = openThemes.update { if (id in it) it - id else it + id }

    fun toggleQuestion(themeId: String, entryId: String) {
        val key = faqKey(themeId, entryId)
        openQuestions.update { if (key in it) it - key else it + key }
    }
}
