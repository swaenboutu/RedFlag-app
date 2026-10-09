package app.redflag.ui.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import app.redflag.R
import app.redflag.data.ProblemRef
import app.redflag.data.displayLabel
import app.redflag.databinding.ItemDetailChoiceBinding
import app.redflag.databinding.ItemManagerThemeBinding
import app.redflag.databinding.ItemOnboardingHeaderBinding
import app.redflag.ui.common.CARD_GAP_DP
import app.redflag.ui.common.applyCard
import app.redflag.ui.common.bindThemeCard
import app.redflag.ui.common.styledIssueLabel
import app.redflag.ui.common.themeIcon
import app.redflag.ui.common.checkedSubtitle

class OnboardingProblemsAdapter(
    private val onThemeClick: (Int) -> Unit,
    private val onToggle: (ProblemRef, Boolean) -> Unit,
) : ListAdapter<OnboardingRow, RecyclerView.ViewHolder>(Diff) {

    private class Holder<B : ViewBinding>(val binding: B) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        OnboardingRow.Header -> TYPE_HEADER
        is OnboardingRow.Theme -> TYPE_THEME
        is OnboardingRow.Choice -> TYPE_CHOICE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> Holder(ItemOnboardingHeaderBinding.inflate(inflater, parent, false))
            TYPE_THEME -> Holder(ItemManagerThemeBinding.inflate(inflater, parent, false))
            else -> Holder(ItemDetailChoiceBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding = (holder as Holder<*>).binding
        when (val row = getItem(position)) {
            OnboardingRow.Header -> with(binding as ItemOnboardingHeaderBinding) {
                topbar.topbarStep.text = root.context.getString(R.string.onboarding_step, 2, 4)
            }
            is OnboardingRow.Theme -> bindTheme(binding as ItemManagerThemeBinding, row)
            is OnboardingRow.Choice -> bindChoice(binding as ItemDetailChoiceBinding, row)
        }
    }

    private fun bindTheme(binding: ItemManagerThemeBinding, row: OnboardingRow.Theme) = with(binding) {
        bindThemeCard(
            title = root.context.getString(row.id),
            subtitle = checkedSubtitle(root.context, row.problemCount, row.checkedCount),
            expanded = row.expanded,
            hasContent = true,
            onClick = { onThemeClick(row.id) },
            icon = themeIcon(row.id),
        )
    }

    private fun bindChoice(binding: ItemDetailChoiceBinding, row: OnboardingRow.Choice) = with(binding) {
        choice.text = styledIssueLabel(root.context, row.ref.displayLabel(root.context, row.override))
        choice.isChecked = row.checked
        choice.setOnClickListener {
            // La case se bascule seule au toucher : on la remet dans l'état réel (un choix refusé ne change rien en base).
            choice.isChecked = row.checked
            onToggle(row.ref, row.checked)
        }
        choiceDivider.visibility = if (row.first) View.GONE else View.VISIBLE
        root.applyCard(roundTop = false, roundBottom = row.last, gapAfterDp = if (row.last) CARD_GAP_DP else 0)
    }

    private object Diff : DiffUtil.ItemCallback<OnboardingRow>() {
        override fun areItemsTheSame(old: OnboardingRow, new: OnboardingRow) = when {
            old is OnboardingRow.Theme && new is OnboardingRow.Theme -> old.id == new.id
            old is OnboardingRow.Choice && new is OnboardingRow.Choice -> old.sectionId == new.sectionId && old.ref == new.ref
            else -> old == new
        }

        override fun areContentsTheSame(old: OnboardingRow, new: OnboardingRow) = old == new
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_THEME = 1
        const val TYPE_CHOICE = 2
    }
}
