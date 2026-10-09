package app.redflag.ui.apps

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.redflag.databinding.ItemLinkedAppBinding
import app.redflag.ui.common.iconOf
import app.redflag.ui.problems.LinkedApp

class LinkedAppsAdapter(
    private val onUnlink: (LinkedApp) -> Unit,
) : ListAdapter<LinkedApp, LinkedAppsAdapter.ViewHolder>(Diff) {

    class ViewHolder(val binding: ItemLinkedAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemLinkedAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = getItem(position)
        with(holder.binding) {
            // L'app a pu être désinstallée depuis : on garde son nom et une icône par défaut.
            icon.setImageDrawable(root.context.iconOf(app.packageName))
            name.text = app.appName
            unlink.setOnClickListener { onUnlink(app) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<LinkedApp>() {
        override fun areItemsTheSame(old: LinkedApp, new: LinkedApp) = old.packageName == new.packageName
        override fun areContentsTheSame(old: LinkedApp, new: LinkedApp) = old == new
    }
}
