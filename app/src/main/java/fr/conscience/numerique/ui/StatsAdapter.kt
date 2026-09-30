package fr.conscience.numerique.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.conscience.numerique.R
import fr.conscience.numerique.databinding.ItemStatsAppBinding

/** La liste des apps avec une interruption ; un appui ouvre le détail de leurs statistiques. */
class StatsAdapter(
    private val onClick: (StatsApp) -> Unit,
) : ListAdapter<StatsRow, StatsAdapter.ViewHolder>(Diff) {

    class ViewHolder(val binding: ItemStatsAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemStatsAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = getItem(position)
        with(holder.binding) {
            icon.clipToOutline = true
            icon.setImageDrawable(root.context.iconOf(row.app.packageName))
            name.text = row.app.appName
            attempts.text = root.resources.getQuantityString(R.plurals.stats_attempts, row.app.attempts, row.app.attempts)
            divider.visibility = if (row.first) View.GONE else View.VISIBLE
            root.applyCard(roundTop = row.first, roundBottom = row.last, gapAfterDp = 0)
            root.setOnClickListener { onClick(row.app) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<StatsRow>() {
        override fun areItemsTheSame(old: StatsRow, new: StatsRow) = old.app.packageName == new.app.packageName
        override fun areContentsTheSame(old: StatsRow, new: StatsRow) = old == new
    }
}
