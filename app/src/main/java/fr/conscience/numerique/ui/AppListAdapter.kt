package fr.conscience.numerique.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.conscience.numerique.R
import fr.conscience.numerique.data.displayLabel
import fr.conscience.numerique.databinding.ItemAppBinding

class AppListAdapter(
    private val onClick: (AppItem) -> Unit,
) : ListAdapter<AppItem, AppListAdapter.ViewHolder>(Diff) {

    class ViewHolder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            icon.setImageDrawable(item.app.icon)
            name.text = item.app.label
            val labels = item.problems.mapNotNull { it.displayLabel(root.context, item.overrides) }
            problems.text = if (labels.isEmpty()) {
                root.context.getString(R.string.problems_none)
            } else {
                labels.joinToString(", ")
            }
            root.setOnClickListener { onClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<AppItem>() {
        override fun areItemsTheSame(old: AppItem, new: AppItem) = old.app.packageName == new.app.packageName
        override fun areContentsTheSame(old: AppItem, new: AppItem) = old == new
    }
}
