package fr.conscience.numerique.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.conscience.numerique.databinding.ItemPickerAppBinding

class AppPickerAdapter(
    private val onToggle: (String) -> Unit,
) : ListAdapter<AppPickRow, AppPickerAdapter.ViewHolder>(Diff) {

    class ViewHolder(val binding: ItemPickerAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemPickerAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = getItem(position)
        with(holder.binding) {
            icon.setImageDrawable(row.app.icon)
            check.text = row.app.label
            check.isChecked = row.checked
            check.setOnClickListener { onToggle(row.app.packageName) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<AppPickRow>() {
        override fun areItemsTheSame(old: AppPickRow, new: AppPickRow) = old.app.packageName == new.app.packageName
        override fun areContentsTheSame(old: AppPickRow, new: AppPickRow) = old == new
    }
}
