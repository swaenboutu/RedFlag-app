package fr.conscience.numerique.ui

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.conscience.numerique.container
import fr.conscience.numerique.R
import fr.conscience.numerique.data.DebugTap
import fr.conscience.numerique.data.DebugUnlock
import fr.conscience.numerique.data.SettingsStore
import fr.conscience.numerique.databinding.ActivitySettingsBinding
import fr.conscience.numerique.databinding.ItemSettingRowBinding
import fr.conscience.numerique.service.isFrictionServiceEnabled
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Réglages : interrupteur général, durée de la pause, liste des apps, autorisations, aide. */
class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private val settings get() = container.settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        BottomNav.setup(this, binding.bottomBar.bottomNav, R.id.nav_settings)

        // Coins arrondis + effets tactiles contenus dans les cartes : `clipToOutline` en XML exige Android 12.
        listOf(binding.activeCard, binding.cardInterruption, binding.cardApps, binding.cardHelp)
            .forEach { it.clipToOutline = true }
        binding.cardDebug.clipToOutline = true

        // Toute la carte bascule l'interrupteur, pas seulement le bouton. Le réglage est enregistré à chaque changement d'état,
        // qu'il vienne d'un appui ou d'un glissement du curseur (un glissement ne déclenche pas de « clic »).
        binding.activeCard.setOnClickListener { binding.activeSwitch.toggle() }
        binding.activeSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked != settings.interceptionEnabled.value) settings.setInterceptionEnabled(checked)
        }

        setupRow(binding.rowPause, R.string.settings_pause_title, R.string.settings_pause_subtitle) { showPauseDialog() }
        setupRow(binding.rowList, R.string.settings_list_title) { showListDialog() }
        setupRow(binding.rowPermissions, R.string.settings_permissions_title) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        setupRow(binding.rowHelp, R.string.settings_help_title) { startActivity(Intent(this, FaqActivity::class.java)) }
        // Sept appuis sur le numéro de version activent le mode debug, comme pour le mode développeur d'Android.
        setupRow(binding.rowVersion, R.string.settings_version) { onVersionTapped() }
        binding.rowVersion.value.text = versionName()
        binding.rowVersion.chevron.visibility = View.GONE

        setupDebug()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(settings.interceptionEnabled, settings.pauseMinutes, settings.hideSystemApps) { enabled, pause, hide ->
                        Triple(enabled, pause, hide)
                    }.collect { (enabled, pause, hide) ->
                        showEnabled(enabled)
                        binding.rowPause.value.text = formatPause(this@SettingsActivity, pause)
                        binding.rowList.value.text = getString(if (hide) R.string.list_no_system else R.string.list_all)
                    }
                }
                launch { settings.debugMode.collect { binding.debugGroup.visibility = if (it) View.VISIBLE else View.GONE } }
                launch { settings.alwaysShowOnboarding.collect { binding.rowOnboarding.toggle.isChecked = it } }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        BottomNav.select(binding.bottomBar.bottomNav, R.id.nav_settings)
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

    private val debugUnlock = DebugUnlock()

    private fun onVersionTapped() {
        val message = when (val tap = debugUnlock.tap(alreadyEnabled = settings.debugMode.value)) {
            DebugTap.Nothing -> return
            is DebugTap.Remaining -> resources.getQuantityString(R.plurals.settings_debug_steps, tap.taps, tap.taps)
            DebugTap.Unlocked -> {
                settings.enableDebugMode()
                getString(R.string.settings_debug_enabled)
            }
            DebugTap.AlreadyEnabled -> getString(R.string.settings_debug_already)
        }
        debugToast?.cancel()
        debugToast = Toast.makeText(this, message, Toast.LENGTH_SHORT).also { it.show() }
    }

    private var debugToast: Toast? = null

    /** Section « Debug » : cachée tant que le mode debug n'est pas activé (voir [onVersionTapped]) ; elle l'est ensuite, pour de bon. */
    private fun setupDebug() {
        with(binding.rowOnboarding) {
            title.setText(R.string.settings_debug_onboarding_title)
            subtitle.setText(R.string.settings_debug_onboarding_subtitle)
            // Toute la ligne bascule l'interrupteur, pas seulement le bouton.
            root.setOnClickListener { toggle.toggle() }
            toggle.setOnCheckedChangeListener { _, checked -> settings.setAlwaysShowOnboarding(checked) }
        }
        setupRow(binding.rowReset, R.string.settings_debug_reset_title, R.string.settings_debug_reset_subtitle) { confirmReset() }
        binding.rowReset.chevron.visibility = View.GONE
    }

    /** Efface toutes les données de l'app (base, réglages), comme « Effacer les données » d'Android ; l'app se ferme. */
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

    private fun showEnabled(enabled: Boolean) {
        binding.activeSwitch.isChecked = enabled
        binding.activeTitle.text = getString(
            if (enabled) R.string.settings_active_title else R.string.settings_inactive_title,
            getString(R.string.app_name),
        )
        binding.activeDescription.setText(if (enabled) R.string.settings_active_desc else R.string.settings_inactive_desc)
    }

    /** Pastille verte « Accordées » ou rouge « Non accordées » selon l'état du service d'accessibilité. */
    private fun showPermissionStatus() = with(binding.rowPermissions) {
        val granted = isFrictionServiceEnabled(this@SettingsActivity)
        value.setText(if (granted) R.string.permissions_granted else R.string.permissions_missing)
        dot.visibility = View.VISIBLE
        dot.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this@SettingsActivity, if (granted) R.color.status_ok else R.color.status_warning),
        )
    }

    private fun showPauseDialog() {
        val choices = SettingsStore.PAUSE_CHOICES
        val labels = choices.map { formatPause(this, it) }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_pause_title)
            .setSingleChoiceItems(labels, choices.indexOf(settings.pauseMinutes.value)) { dialog, which ->
                settings.setPauseMinutes(choices[which])
                dialog.dismiss()
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

    private fun versionName(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
    } catch (_: Exception) {
        ""
    }
}
