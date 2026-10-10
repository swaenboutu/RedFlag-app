package app.redflag.ui.apps

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import app.redflag.R
import app.redflag.data.ProblemRef
import app.redflag.data.displayDescription
import app.redflag.data.displayLabel
import app.redflag.databinding.ItemDetailChoiceBinding
import app.redflag.databinding.ItemDetailHeaderBinding
import app.redflag.databinding.ItemDetailInfoBinding
import app.redflag.databinding.ItemDetailSectionBinding
import app.redflag.databinding.ItemManagerThemeBinding
import app.redflag.ui.common.CARD_GAP_DP
import app.redflag.ui.common.applyCard
import app.redflag.ui.common.bindThemeCard
import app.redflag.ui.common.styledIssueLabel
import app.redflag.ui.common.themeIcon
import app.redflag.ui.common.checkedSubtitle
import app.redflag.ui.common.iconOf

class AppDetailAdapter(
    private val onThemeClick: (Int) -> Unit,
    private val onToggle: (ProblemRef, Boolean) -> Unit,
) : ListAdapter<DetailRow, RecyclerView.ViewHolder>(Diff) {

    private class Holder<B : ViewBinding>(val binding: B) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is DetailRow.Header -> TYPE_HEADER
        DetailRow.Section -> TYPE_SECTION
        is DetailRow.Theme -> TYPE_THEME
        is DetailRow.Choice -> TYPE_CHOICE
        is DetailRow.Info -> TYPE_INFO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> Holder(ItemDetailHeaderBinding.inflate(inflater, parent, false))
            TYPE_SECTION -> Holder(ItemDetailSectionBinding.inflate(inflater, parent, false))
            TYPE_THEME -> Holder(ItemManagerThemeBinding.inflate(inflater, parent, false))
            TYPE_CHOICE -> Holder(ItemDetailChoiceBinding.inflate(inflater, parent, false))
            else -> Holder(ItemDetailInfoBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding = (holder as Holder<*>).binding
        when (val row = getItem(position)) {
            is DetailRow.Header -> bindHeader(binding as ItemDetailHeaderBinding, row)
            DetailRow.Section -> Unit
            is DetailRow.Theme -> bindTheme(binding as ItemManagerThemeBinding, row)
            is DetailRow.Choice -> bindChoice(binding as ItemDetailChoiceBinding, row)
            is DetailRow.Info -> bindInfo(binding as ItemDetailInfoBinding, row)
        }
    }

    private fun bindHeader(binding: ItemDetailHeaderBinding, row: DetailRow.Header) = with(binding) {
        appIcon.clipToOutline = true
        appIcon.setImageDrawable(root.context.iconOf(row.packageName))
        appName.text = row.appName
        appSubtitle.text = root.resources.getQuantityString(R.plurals.app_detail_subtitle, row.count, row.count)
    }

    private fun bindTheme(binding: ItemManagerThemeBinding, row: DetailRow.Theme) = with(binding) {
        bindThemeCard(
            title = root.context.getString(row.id),
            subtitle = themeSubtitle(root.context, row),
            expanded = row.expanded,
            hasContent = row.problemCount > 0,
            onClick = { onThemeClick(row.id) },
            icon = themeIcon(row.id),
        )
    }

    /** La carte des problématiques associées n'a pas besoin du nombre de cochées. */
    private fun themeSubtitle(context: Context, row: DetailRow.Theme): String {
        if (row.id == R.string.category_custom && row.problemCount == 0) return context.getString(R.string.custom_empty)
        return checkedSubtitle(context, row.problemCount, if (row.id == R.string.category_linked) 0 else row.checkedCount)
    }

    private fun bindChoice(binding: ItemDetailChoiceBinding, row: DetailRow.Choice) = with(binding) {
        val context = root.context
        choice.text = styledIssueLabel(context, row.ref.displayLabel(context, row.override), row.ref.displayDescription(context))
        choice.isChecked = row.checked
        choice.setOnClickListener { onToggle(row.ref, row.checked) }
        choiceDivider.visibility = if (row.first) View.GONE else View.VISIBLE
        root.applyCard(roundTop = false, roundBottom = row.last, gapAfterDp = if (row.last) CARD_GAP_DP else 0)
    }

    private fun bindInfo(binding: ItemDetailInfoBinding, row: DetailRow.Info) = with(binding) {
        info.text = if (row.count == 0) {
            root.context.getString(R.string.app_detail_info_none, row.appName)
        } else {
            root.resources.getQuantityString(R.plurals.app_detail_info, row.count, row.appName, row.count)
        }
    }

    private object Diff : DiffUtil.ItemCallback<DetailRow>() {
        override fun areItemsTheSame(old: DetailRow, new: DetailRow) = when {
            old is DetailRow.Header && new is DetailRow.Header -> true
            old is DetailRow.Theme && new is DetailRow.Theme -> old.id == new.id
            old is DetailRow.Choice && new is DetailRow.Choice -> old.sectionId == new.sectionId && old.ref == new.ref
            old is DetailRow.Info && new is DetailRow.Info -> true
            else -> old == new
        }

        override fun areContentsTheSame(old: DetailRow, new: DetailRow) = old == new
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_SECTION = 1
        const val TYPE_THEME = 2
        const val TYPE_CHOICE = 3
        const val TYPE_INFO = 4
    }
}
