package app.redflag.ui.problems

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.redflag.R
import app.redflag.data.displayLabel
import app.redflag.databinding.ItemManagerProblemBinding
import app.redflag.databinding.ItemManagerThemeBinding
import app.redflag.ui.common.CARD_GAP_DP
import app.redflag.ui.common.applyCard
import app.redflag.ui.common.bindThemeCard
import app.redflag.ui.common.themeIcon
import app.redflag.ui.common.problemsSubtitle

/** Thèmes repliables sous forme de cartes blanches ; les problématiques d'un thème ouvert prolongent sa carte. */
class ProblemsManagerAdapter(
    private val onThemeClick: (ManagerRow.Theme) -> Unit,
    private val onProblemClick: (ManagerRow) -> Unit,
) : ListAdapter<ManagerRow, RecyclerView.ViewHolder>(Diff) {

    private class ThemeHolder(val binding: ItemManagerThemeBinding) : RecyclerView.ViewHolder(binding.root)
    private class ProblemHolder(val binding: ItemManagerProblemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is ManagerRow.Theme -> TYPE_THEME
        else -> TYPE_PROBLEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_THEME) {
            ThemeHolder(ItemManagerThemeBinding.inflate(inflater, parent, false))
        } else {
            ProblemHolder(ItemManagerProblemBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is ManagerRow.Theme -> bindTheme(holder as ThemeHolder, row)
            is ManagerRow.Catalog -> {
                val context = holder.itemView.context
                val label = row.problem.displayLabel(context, row.override)
                bindProblem(holder as ProblemHolder, row, label, row.apps, row.favorite, row.themeTitle, row.sectionId, row.first, row.last)
            }
            is ManagerRow.Custom -> bindProblem(holder as ProblemHolder, row, row.label, row.apps, row.favorite, row.themeTitle, row.sectionId, row.first, row.last)
        }
    }

    private fun bindTheme(holder: ThemeHolder, row: ManagerRow.Theme) = with(holder.binding) {
        val context = root.context
        bindThemeCard(
            title = context.getString(row.id),
            subtitle = themeSubtitle(context, row),
            expanded = row.expanded,
            hasContent = row.problemCount > 0,
            onClick = { onThemeClick(row) },
            icon = themeIcon(row.id),
        )
    }

    private fun themeSubtitle(context: Context, row: ManagerRow.Theme): String {
        if (row.id == R.string.category_custom && row.problemCount == 0) return context.getString(R.string.custom_empty)
        val problems = problemsSubtitle(context, row.problemCount)
        if (row.appCount == 0) return problems
        val apps = context.resources.getQuantityString(R.plurals.theme_apps_flagged, row.appCount, row.appCount)
        return "$problems · $apps"
    }

    private fun bindProblem(holder: ProblemHolder, row: ManagerRow, label: String, apps: List<LinkedApp>, favorite: Boolean, themeTitle: Int, sectionId: Int, first: Boolean, last: Boolean) =
        with(holder.binding) {
            val context = root.context
            name.text = label
            val appsText = if (apps.isEmpty()) {
                context.getString(R.string.apps_none)
            } else {
                context.resources.getQuantityString(R.plurals.apps_count_short, apps.size, apps.size)
            }
            // In the favorites, the theme of the issue; in a theme, a star for the issues that are favorites.
            val inFavorites = sectionId == R.string.category_favorites
            count.text = if (inFavorites) "${context.getString(themeTitle)} · $appsText" else appsText
            count.setCompoundDrawablesRelativeWithIntrinsicBounds(if (favorite && !inFavorites) R.drawable.ic_star_filled_small else 0, 0, 0, 0)
            problemDivider.visibility = if (first) View.GONE else View.VISIBLE
            root.setOnClickListener { onProblemClick(row) }

            root.applyCard(roundTop = false, roundBottom = last, gapAfterDp = if (last) CARD_GAP_DP else 0)
        }

    private object Diff : DiffUtil.ItemCallback<ManagerRow>() {
        override fun areItemsTheSame(old: ManagerRow, new: ManagerRow) = when {
            old is ManagerRow.Theme && new is ManagerRow.Theme -> old.id == new.id
            old is ManagerRow.Catalog && new is ManagerRow.Catalog -> old.sectionId == new.sectionId && old.problem.key == new.problem.key
            old is ManagerRow.Custom && new is ManagerRow.Custom -> old.sectionId == new.sectionId && old.id == new.id
            else -> false
        }

        override fun areContentsTheSame(old: ManagerRow, new: ManagerRow) = old == new
    }

    private companion object {
        const val TYPE_THEME = 0
        const val TYPE_PROBLEM = 1
    }
}
