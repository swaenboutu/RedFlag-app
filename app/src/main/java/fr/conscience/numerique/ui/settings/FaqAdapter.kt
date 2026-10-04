package fr.conscience.numerique.ui.settings

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import fr.conscience.numerique.R
import fr.conscience.numerique.databinding.ItemFaqAnswerBinding
import fr.conscience.numerique.databinding.ItemFaqQuestionBinding
import fr.conscience.numerique.databinding.ItemManagerThemeBinding
import fr.conscience.numerique.ui.common.CARD_GAP_DP
import fr.conscience.numerique.ui.common.applyCard
import fr.conscience.numerique.ui.common.bindThemeCard

/** Thèmes repliables en cartes blanches ; les questions d'un thème ouvert, et la réponse d'une question ouverte, prolongent sa carte. */
class FaqAdapter(
    private val onThemeClick: (FaqRow.Theme) -> Unit,
    private val onQuestionClick: (FaqRow.Question) -> Unit,
) : ListAdapter<FaqRow, RecyclerView.ViewHolder>(Diff) {

    private class ThemeHolder(val binding: ItemManagerThemeBinding) : RecyclerView.ViewHolder(binding.root)
    private class QuestionHolder(val binding: ItemFaqQuestionBinding) : RecyclerView.ViewHolder(binding.root)
    private class AnswerHolder(val binding: ItemFaqAnswerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is FaqRow.Theme -> TYPE_THEME
        is FaqRow.Question -> TYPE_QUESTION
        is FaqRow.Answer -> TYPE_ANSWER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_THEME -> ThemeHolder(ItemManagerThemeBinding.inflate(inflater, parent, false))
            TYPE_QUESTION -> QuestionHolder(ItemFaqQuestionBinding.inflate(inflater, parent, false))
            else -> AnswerHolder(ItemFaqAnswerBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is FaqRow.Theme -> with((holder as ThemeHolder).binding) {
                bindThemeCard(
                    title = row.title,
                    subtitle = root.resources.getQuantityString(R.plurals.faq_questions, row.questionCount, row.questionCount),
                    expanded = row.expanded,
                    hasContent = row.questionCount > 0,
                    onClick = { onThemeClick(row) },
                )
            }
            is FaqRow.Question -> with((holder as QuestionHolder).binding) {
                question.text = row.text
                questionDivider.visibility = if (row.first) View.GONE else View.VISIBLE
                questionChevron.rotation = if (row.expanded) 180f else 0f
                questionChevron.imageTintList = ColorStateList.valueOf(
                    MaterialColors.getColor(
                        root,
                        if (row.expanded) com.google.android.material.R.attr.colorOnSurface else com.google.android.material.R.attr.colorOnSurfaceVariant,
                    ),
                )
                root.setOnClickListener { onQuestionClick(row) }
                root.applyCard(roundTop = false, roundBottom = row.last, gapAfterDp = if (row.last) CARD_GAP_DP else 0)
            }
            is FaqRow.Answer -> with((holder as AnswerHolder).binding) {
                answer.text = row.text
                root.applyCard(roundTop = false, roundBottom = row.last, gapAfterDp = if (row.last) CARD_GAP_DP else 0)
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<FaqRow>() {
        override fun areItemsTheSame(old: FaqRow, new: FaqRow) = when {
            old is FaqRow.Theme && new is FaqRow.Theme -> old.id == new.id
            old is FaqRow.Question && new is FaqRow.Question -> old.themeId == new.themeId && old.id == new.id
            old is FaqRow.Answer && new is FaqRow.Answer -> old.themeId == new.themeId && old.entryId == new.entryId
            else -> false
        }

        override fun areContentsTheSame(old: FaqRow, new: FaqRow) = old == new
    }

    private companion object {
        const val TYPE_THEME = 0
        const val TYPE_QUESTION = 1
        const val TYPE_ANSWER = 2
    }
}
