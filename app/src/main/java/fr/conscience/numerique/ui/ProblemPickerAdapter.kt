package fr.conscience.numerique.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.conscience.numerique.databinding.ItemPickerHeaderBinding
import fr.conscience.numerique.databinding.ItemPickerProblemBinding

class ProblemPickerAdapter(
    private val onToggleKey: (String) -> Unit,
    private val onToggleCustom: (String) -> Unit,
) : ListAdapter<PickerRow, RecyclerView.ViewHolder>(Diff) {

    private class HeaderHolder(val binding: ItemPickerHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    private class ProblemHolder(val binding: ItemPickerProblemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is PickerRow.Header -> TYPE_HEADER
        else -> TYPE_PROBLEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderHolder(ItemPickerHeaderBinding.inflate(inflater, parent, false))
        } else {
            ProblemHolder(ItemPickerProblemBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is PickerRow.Header -> with((holder as HeaderHolder).binding.header) {
                text = "${row.emoji}  ${context.getString(row.title)}"
            }
            is PickerRow.Predefined -> bindCheck(holder, row.checked, row.override ?: holder.itemView.context.getString(row.problem.label)) {
                onToggleKey(row.problem.key)
            }
            is PickerRow.Custom -> bindCheck(holder, row.checked, row.text) { onToggleCustom(row.text) }
        }
    }

    private fun bindCheck(holder: RecyclerView.ViewHolder, checked: Boolean, label: String, onClick: () -> Unit) {
        with((holder as ProblemHolder).binding.check) {
            text = label
            isChecked = checked
            setOnClickListener { onClick() }
        }
    }

    private object Diff : DiffUtil.ItemCallback<PickerRow>() {
        override fun areItemsTheSame(old: PickerRow, new: PickerRow) = when {
            old is PickerRow.Header && new is PickerRow.Header -> old.title == new.title
            old is PickerRow.Predefined && new is PickerRow.Predefined -> old.problem.key == new.problem.key
            old is PickerRow.Custom && new is PickerRow.Custom -> old.text == new.text
            else -> false
        }

        override fun areContentsTheSame(old: PickerRow, new: PickerRow) = old == new
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_PROBLEM = 1
    }
}
