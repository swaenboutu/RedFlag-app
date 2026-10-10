package app.redflag.ui.common

import android.content.Intent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import app.redflag.R
import app.redflag.container
import app.redflag.data.DebugTap
import app.redflag.data.DebugUnlock
import app.redflag.databinding.ViewDrawerBinding
import app.redflag.ui.about.AboutActivity
import app.redflag.ui.settings.FaqActivity
import app.redflag.ui.settings.SettingsActivity

/**
 * The drawer of the main screens, opened by the menu button at the top left: the Settings, "How it works" (the FAQ), "About" and the
 * version of the app. It is added around the content of the screen once it is inflated ([setup]), so no layout has to carry it.
 * Seven taps on the version turn on the debug mode, like Android's developer mode.
 */
object AppDrawer {
    fun setup(activity: AppCompatActivity, menuButton: View) {
        val content = activity.findViewById<ViewGroup>(android.R.id.content)
        val root = content.getChildAt(0) ?: return
        content.removeView(root)

        val drawer = DrawerLayout(activity)
        drawer.addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val panel = ViewDrawerBinding.inflate(activity.layoutInflater, drawer, false)
        (panel.root.layoutParams as DrawerLayout.LayoutParams).gravity = Gravity.START
        drawer.addView(panel.root)
        content.addView(drawer, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // The panel keeps clear of the status bar and the navigation bar.
        ViewCompat.setOnApplyWindowInsetsListener(panel.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }

        menuButton.setOnClickListener { drawer.openDrawer(GravityCompat.START) }
        panel.drawerClose.setOnClickListener { drawer.closeDrawer(GravityCompat.START) }
        // Back closes the drawer first.
        val back = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() = drawer.closeDrawer(GravityCompat.START)
        }
        activity.onBackPressedDispatcher.addCallback(activity, back)
        drawer.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                back.isEnabled = true
            }

            override fun onDrawerClosed(drawerView: View) {
                back.isEnabled = false
            }
        })

        fun open(target: Class<*>) {
            activity.startActivity(Intent(activity, target))
            drawer.closeDrawers()
        }
        panel.drawerSettings.setOnClickListener { open(SettingsActivity::class.java) }
        panel.drawerFaq.setOnClickListener { open(FaqActivity::class.java) }
        panel.drawerAbout.setOnClickListener { open(AboutActivity::class.java) }

        panel.drawerVersion.text = activity.getString(R.string.drawer_version, versionName(activity))
        setupDebugUnlock(activity, panel.drawerVersion)
    }

    private fun versionName(activity: AppCompatActivity): String = try {
        activity.packageManager.getPackageInfo(activity.packageName, 0).versionName.orEmpty()
    } catch (_: Exception) {
        ""
    }

    private var debugToast: Toast? = null

    /** Seven taps on the version turn on the debug mode (the Debug section of the Settings). */
    private fun setupDebugUnlock(activity: AppCompatActivity, version: View) {
        val unlock = DebugUnlock()
        version.setOnClickListener {
            val settings = activity.container.settings
            val message = when (val tap = unlock.tap(alreadyEnabled = settings.debugMode.value)) {
                DebugTap.Nothing -> return@setOnClickListener
                is DebugTap.Remaining -> activity.resources.getQuantityString(R.plurals.settings_debug_steps, tap.taps, tap.taps)
                DebugTap.Unlocked -> {
                    settings.enableDebugMode()
                    activity.getString(R.string.settings_debug_enabled)
                }
                DebugTap.AlreadyEnabled -> activity.getString(R.string.settings_debug_already)
            }
            debugToast?.cancel()
            debugToast = Toast.makeText(activity, message, Toast.LENGTH_SHORT).also { it.show() }
        }
    }
}
