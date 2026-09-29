package fr.conscience.numerique.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.conscience.numerique.R
import fr.conscience.numerique.data.displayLabel
import fr.conscience.numerique.databinding.ItemAppBinding

/** Liste des applications : une seule carte blanche, avec les problématiques de chaque app en pastilles. */
class AppListAdapter(
    private val onClick: (AppItem) -> Unit,
) : ListAdapter<AppItem, AppListAdapter.ViewHolder>(Diff) {

    class ViewHolder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            // Icône aux coins arrondis : `clipToOutline` en XML exige Android 12.
            binding.icon.clipToOutline = true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            icon.setImageDrawable(item.app.icon)
            name.text = item.app.label
            appDivider.visibility = if (item.first) View.GONE else View.VISIBLE

            val labels = item.problems.mapNotNull { it.displayLabel(root.context, item.overrides) }
            noProblems.visibility = if (labels.isEmpty()) View.VISIBLE else View.GONE
            chips.visibility = if (labels.isEmpty()) View.GONE else View.VISIBLE
            bindChips(chips, labels)

            root.setOnClickListener { onClick(item) }
            root.applyCard(roundTop = item.first, roundBottom = item.last, gapAfterDp = 0)
        }
    }

    /**
     * Une seule ligne de pastilles : la première prend la place qu'il lui faut (jusqu'à ~200 dp),
     * la suivante se contente du reste, puis « +N » pour les autres.
     */
    private fun bindChips(container: LinearLayout, labels: List<String>) {
        container.removeAllViews()
        val shown = labels.take(MAX_CHIPS)
        shown.forEachIndexed { index, label ->
            container.addView(chip(container, label, maxWidthDp = if (index == 0 && shown.size > 1) 200 else 150))
        }
        val more = labels.size - shown.size
        if (more > 0) container.addView(chip(container, "+$more", maxWidthDp = 48))
    }

    private fun chip(parent: View, text: String, maxWidthDp: Int) = TextView(parent.context).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 20.dp(parent)).apply {
            marginEnd = 6.dp(parent)
        }
        this.text = text
        background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_chip_warning)
        setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.on_warning_container))
        typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.inter_medium)
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10.5f)
        gravity = android.view.Gravity.CENTER
        setPadding(8.dp(parent), 0, 8.dp(parent), 0)
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
        maxWidth = maxWidthDp.dp(parent)
        includeFontPadding = false
    }

    private object Diff : DiffUtil.ItemCallback<AppItem>() {
        override fun areItemsTheSame(old: AppItem, new: AppItem) = old.app.packageName == new.app.packageName
        override fun areContentsTheSame(old: AppItem, new: AppItem) = old == new
    }

    private companion object {
        const val MAX_CHIPS = 2
    }
}
