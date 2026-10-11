package app.redflag.ui.apps

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import app.redflag.databinding.ItemLinkedAppBinding
import app.redflag.ui.common.iconOf
import app.redflag.ui.problems.LinkedApp

/**
 * Fills [container] with one row per linked app. The rows are plain views, not a RecyclerView: inside the scrolling page, a RecyclerView
 * with a wrapped height only shows the rows that fit on the screen, so a long list could not be scrolled to its end.
 */
fun bindLinkedApps(container: LinearLayout, apps: List<LinkedApp>, onUnlink: (LinkedApp) -> Unit) {
    container.removeAllViews()
    val inflater = LayoutInflater.from(container.context)
    apps.forEachIndexed { index, app ->
        val row = ItemLinkedAppBinding.inflate(inflater, container, false)
        // The app may have been uninstalled since: keep its name and a default icon.
        row.icon.setImageDrawable(container.context.iconOf(app.packageName))
        row.icon.clipToOutline = true
        row.name.text = app.appName
        row.divider.visibility = if (index == 0) View.GONE else View.VISIBLE
        row.unlink.setOnClickListener { onUnlink(app) }
        container.addView(row.root)
    }
}
