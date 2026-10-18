package app.redflag.ui.settings

import android.app.ActivityManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import app.redflag.R
import app.redflag.container
import app.redflag.data.Appearance
import app.redflag.data.SettingsStore
import app.redflag.databinding.ActivitySettingsBinding
import app.redflag.databinding.ItemSettingRowBinding
import app.redflag.service.FrictionMonitorService
import app.redflag.service.hasFrictionPermissions
import app.redflag.ui.common.PermissionDisclosureDialog
import app.redflag.ui.common.formatPause
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Settings: appearance, "Don't ask again" duration, apps list, permissions, help. (The pause of the interruption is on the home screen.) */
class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private val settings get() = container.settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.back.setOnClickListener { finish() }

        // Rounded corners + touch ripples kept inside the cards: `clipToOutline` in XML needs Android 12.
        listOf(binding.cardDisplay, binding.cardInterruption, binding.cardApps)
            .forEach { it.clipToOutline = true }
        binding.cardDebug.clipToOutline = true

        setupRow(binding.rowAppearance, R.string.settings_appearance_title) { showAppearanceDialog() }
        setupRow(binding.rowPause, R.string.settings_pause_title, R.string.settings_pause_subtitle) { SnoozeSheet.show(this) }
        setupRow(binding.rowList, R.string.settings_list_title) { showListDialog() }
        setupRow(binding.rowPermissions, R.string.settings_permissions_title) {
            PermissionDisclosureDialog.show(this)
        }
        setupDebug()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(settings.pauseMinutes, settings.hideSystemApps) { pause, hide -> pause to hide }.collect { (pause, hide) ->
                        binding.rowPause.value.text = formatPause(this@SettingsActivity, pause)
                        binding.rowList.value.text = getString(if (hide) R.string.list_no_system else R.string.list_all)
                    }
                }
                launch { settings.appearance.collect { binding.rowAppearance.value.setText(appearanceLabel(it)) } }
                launch { settings.debugMode.collect { binding.debugGroup.visibility = if (it) View.VISIBLE else View.GONE } }
                launch { settings.alwaysShowOnboarding.collect { binding.rowOnboarding.toggle.isChecked = it } }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        showPermissionStatus()
    }

    private fun setupRow(row: ItemSettingRowBinding, title: Int, subtitle: Int? = null, clickable: Boolean = true, onClick: () -> Unit = {}) {
        row.title.setText(title)
        if (subtitle != null) {
            row.subtitle.setText(subtitle)
            row.subtitle.visibility = View.VISIBLE
        }
        row.root.isClickable = clickable
        row.root.isFocusable = clickable
        if (clickable) row.root.setOnClickListener { onClick() } else row.root.background = null
    }

    /** "Debug" section: hidden until debug mode is turned on (see the drawer, `AppDrawer`); then shown for good. */
    private fun setupDebug() {
        with(binding.rowOnboarding) {
            title.setText(R.string.settings_debug_onboarding_title)
            subtitle.setText(R.string.settings_debug_onboarding_subtitle)
            // The whole row flips the switch, not only the switch itself.
            root.setOnClickListener { toggle.toggle() }
            toggle.setOnCheckedChangeListener { _, checked -> settings.setAlwaysShowOnboarding(checked) }
        }
        setupRow(binding.rowReset, R.string.settings_debug_reset_title, R.string.settings_debug_reset_subtitle) { confirmReset() }
        binding.rowReset.chevron.visibility = View.GONE
    }

    /** Clears all the app's data (database, settings), like Android's "Clear data"; the app closes. */
    private fun confirmReset() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_debug_reset_confirm_title)
            .setMessage(R.string.settings_debug_reset_confirm_message)
            .setPositiveButton(R.string.settings_debug_reset_confirm_action) { _, _ ->
                (getSystemService(ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Green "Granted" or red "Not granted" dot, depending on the two Android permissions; the watch starts once both are there. */
    private fun showPermissionStatus() = with(binding.rowPermissions) {
        val granted = hasFrictionPermissions(this@SettingsActivity)
        if (granted) FrictionMonitorService.startIfPermitted(this@SettingsActivity)
        value.setText(if (granted) R.string.permissions_granted else R.string.permissions_missing)
        dot.visibility = View.VISIBLE
        dot.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this@SettingsActivity, if (granted) R.color.status_ok else R.color.status_warning),
        )
    }

    private fun appearanceLabel(appearance: Appearance): Int = when (appearance) {
        Appearance.SYSTEM -> R.string.appearance_system
        Appearance.LIGHT -> R.string.appearance_light
        Appearance.DARK -> R.string.appearance_dark
    }

    private fun showAppearanceDialog() {
        val choices = Appearance.entries
        val labels = choices.map { getString(appearanceLabel(it)) }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_appearance_title)
            .setSingleChoiceItems(labels, choices.indexOf(settings.appearance.value)) { dialog, which ->
                dialog.dismiss()
                // Applying recreates the open screens, so the choice is saved first.
                settings.setAppearance(choices[which])
                choices[which].apply()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showListDialog() {
        val labels = arrayOf(getString(R.string.list_all), getString(R.string.list_no_system))
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_list_title)
            .setSingleChoiceItems(labels, if (settings.hideSystemApps.value) 1 else 0) { dialog, which ->
                settings.setHideSystemApps(which == 1)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
