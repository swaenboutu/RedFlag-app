package fr.conscience.numerique.ui

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.conscience.numerique.databinding.ItemLinkedAppBinding

class LinkedAppsAdapter(
    private val onUnlink: (LinkedApp) -> Unit,
) : ListAdapter<LinkedApp, LinkedAppsAdapter.ViewHolder>(Diff) {

    class ViewHolder(val binding: ItemLinkedAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemLinkedAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = getItem(position)
        with(holder.binding) {
            val pm = root.context.packageManager
            // L'app a pu être désinstallée depuis : on garde son nom et une icône par défaut.
            icon.setImageDrawable(
                try {
                    pm.getApplicationIcon(app.packageName)
                } catch (_: PackageManager.NameNotFoundException) {
                    pm.defaultActivityIcon
                },
            )
            name.text = app.appName
            unlink.setOnClickListener { onUnlink(app) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<LinkedApp>() {
        override fun areItemsTheSame(old: LinkedApp, new: LinkedApp) = old.packageName == new.packageName
        override fun areContentsTheSame(old: LinkedApp, new: LinkedApp) = old == new
    }
}
