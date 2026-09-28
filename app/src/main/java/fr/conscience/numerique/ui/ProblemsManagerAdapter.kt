package fr.conscience.numerique.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.conscience.numerique.R
import fr.conscience.numerique.databinding.ItemManagerProblemBinding
import fr.conscience.numerique.databinding.ItemPickerHeaderBinding

class ProblemsManagerAdapter(
    private val onClick: (ManagerRow) -> Unit,
) : ListAdapter<ManagerRow, RecyclerView.ViewHolder>(Diff) {

    private class HeaderHolder(val binding: ItemPickerHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    private class ProblemHolder(val binding: ItemManagerProblemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is ManagerRow.Header -> TYPE_HEADER
        else -> TYPE_PROBLEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderHolder(ItemPickerHeaderBinding.inflate(inflater, parent, false))
        } else {
            ProblemHolder(ItemManagerProblemBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val row = getItem(position)
        when (row) {
            is ManagerRow.Header -> with((holder as HeaderHolder).binding.header) {
                text = "${row.emoji}  ${context.getString(row.title)}"
            }
            is ManagerRow.Catalog ->
                bindProblem(holder, row, row.override ?: holder.itemView.context.getString(row.problem.label), row.apps)
            is ManagerRow.Custom -> bindProblem(holder, row, row.label, row.apps)
        }
    }

    private fun bindProblem(holder: RecyclerView.ViewHolder, row: ManagerRow, label: String, apps: Int) {
        with((holder as ProblemHolder).binding) {
            name.text = label
            count.text = root.resources.getQuantityString(R.plurals.apps_count, apps, apps)
            root.setOnClickListener { onClick(row) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<ManagerRow>() {
        override fun areItemsTheSame(old: ManagerRow, new: ManagerRow) = when {
            old is ManagerRow.Header && new is ManagerRow.Header -> old.title == new.title
            old is ManagerRow.Catalog && new is ManagerRow.Catalog -> old.problem.key == new.problem.key
            old is ManagerRow.Custom && new is ManagerRow.Custom -> old.label == new.label
            else -> false
        }

        override fun areContentsTheSame(old: ManagerRow, new: ManagerRow) = old == new
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_PROBLEM = 1
    }
}
