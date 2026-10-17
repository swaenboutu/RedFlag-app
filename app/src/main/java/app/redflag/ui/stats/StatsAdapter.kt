package app.redflag.ui.stats

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.redflag.R
import app.redflag.databinding.ItemAppBinding
import app.redflag.ui.common.applyCard
import app.redflag.ui.common.iconOf

/** The list of the apps with an interruption; a tap opens the detail of their statistics. */
class StatsAdapter(
    private val onClick: (StatsApp) -> Unit,
) : ListAdapter<StatsRow, StatsAdapter.ViewHolder>(Diff) {

    class ViewHolder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = getItem(position)
        with(holder.binding) {
            icon.clipToOutline = true
            icon.setImageDrawable(root.context.iconOf(row.app.packageName))
            name.text = row.app.appName
            subtitle.text = root.resources.getQuantityString(R.plurals.stats_attempts, row.app.attempts, row.app.attempts)
            appDivider.visibility = if (row.first) View.GONE else View.VISIBLE
            root.applyCard(roundTop = row.first, roundBottom = row.last, gapAfterDp = 0)
            root.setOnClickListener { onClick(row.app) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<StatsRow>() {
        override fun areItemsTheSame(old: StatsRow, new: StatsRow) = old.app.packageName == new.app.packageName
        override fun areContentsTheSame(old: StatsRow, new: StatsRow) = old == new
    }
}
