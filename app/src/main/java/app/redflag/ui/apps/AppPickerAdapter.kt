package app.redflag.ui.apps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.redflag.databinding.ItemOnboardingAppBinding

/** The apps to tick in the "Link apps" sheet: the same rows as the welcome tour, without the card background. */
class AppPickerAdapter(
    private val onToggle: (String) -> Unit,
) : ListAdapter<AppPickRow, AppPickerAdapter.ViewHolder>(Diff) {

    class ViewHolder(val binding: ItemOnboardingAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemOnboardingAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)).also {
            // The sheet already has its own margins.
            (it.binding.root.layoutParams as ViewGroup.MarginLayoutParams).apply { marginStart = 0; marginEnd = 0 }
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = getItem(position)
        with(holder.binding) {
            icon.clipToOutline = true
            icon.setImageDrawable(row.app.icon)
            choice.text = row.app.label
            choice.isChecked = row.checked
            choice.setTypeface(null, if (row.checked) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            choice.setOnClickListener { onToggle(row.app.packageName) }
            divider.visibility = if (position == 0) View.GONE else View.VISIBLE
        }
    }

    private object Diff : DiffUtil.ItemCallback<AppPickRow>() {
        override fun areItemsTheSame(old: AppPickRow, new: AppPickRow) = old.app.packageName == new.app.packageName
        override fun areContentsTheSame(old: AppPickRow, new: AppPickRow) = old == new
    }
}
