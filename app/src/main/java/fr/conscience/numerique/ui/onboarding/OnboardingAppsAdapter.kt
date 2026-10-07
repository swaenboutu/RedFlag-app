package fr.conscience.numerique.ui.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import fr.conscience.numerique.databinding.ItemOnboardingAppBinding
import fr.conscience.numerique.databinding.ItemOnboardingAppsHeaderBinding
import fr.conscience.numerique.databinding.ItemOnboardingEmptyBinding
import fr.conscience.numerique.ui.common.applyCard

class OnboardingAppsAdapter(
    private val onToggle: (String) -> Unit,
    private val onQueryChange: (String) -> Unit,
    private val currentQuery: () -> String,
) : ListAdapter<AppsRow, RecyclerView.ViewHolder>(Diff) {

    private class Holder<B : ViewBinding>(val binding: B) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is AppsRow.Header -> TYPE_HEADER
        AppsRow.Empty -> TYPE_EMPTY
        is AppsRow.Choice -> TYPE_APP
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> Holder(ItemOnboardingAppsHeaderBinding.inflate(inflater, parent, false)).also { holder ->
                holder.binding.search.doAfterTextChanged { onQueryChange(it?.toString().orEmpty()) }
            }
            TYPE_EMPTY -> Holder(ItemOnboardingEmptyBinding.inflate(inflater, parent, false))
            else -> Holder(ItemOnboardingAppBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding = (holder as Holder<*>).binding
        when (val row = getItem(position)) {
            is AppsRow.Header -> with(binding as ItemOnboardingAppsHeaderBinding) {
                // The text only changes when the step has changed (search reset), never while typing.
                if (search.text.toString() != currentQuery()) search.setText(currentQuery())
            }
            AppsRow.Empty -> Unit
            is AppsRow.Choice -> with(binding as ItemOnboardingAppBinding) {
                icon.clipToOutline = true
                icon.setImageDrawable(row.app.icon)
                choice.text = row.app.label
                choice.isChecked = row.checked
                choice.setOnClickListener { onToggle(row.app.packageName) }
                divider.visibility = if (row.first) View.GONE else View.VISIBLE
                // Une seule carte blanche pour toute la liste : arrondie en haut sur la première ligne, en bas sur la dernière.
                root.applyCard(roundTop = row.first, roundBottom = row.last, gapAfterDp = 0)
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<AppsRow>() {
        override fun areItemsTheSame(old: AppsRow, new: AppsRow) = when {
            old is AppsRow.Header && new is AppsRow.Header -> true
            old is AppsRow.Empty && new is AppsRow.Empty -> true
            old is AppsRow.Choice && new is AppsRow.Choice -> old.app.packageName == new.app.packageName
            else -> false
        }

        override fun areContentsTheSame(old: AppsRow, new: AppsRow) = old == new
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_APP = 1
        const val TYPE_EMPTY = 2
    }
}
