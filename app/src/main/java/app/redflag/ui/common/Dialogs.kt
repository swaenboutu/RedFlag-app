package app.redflag.ui.common

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import app.redflag.R

/**
 * Warning before opening the accessibility settings, from the Home screen and from Settings: what the service reads, what it is for,
 * that no data leaves the device, and an explicit consent. Google Play requires it for any app that is not an accessibility tool; the
 * permission screen of the welcome tour plays the same role with the same text.
 */
class AccessibilityDisclosureDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        return MaterialAlertDialogBuilder(context)
            .setTitle(R.string.accessibility_disclosure_title)
            .setMessage(getString(R.string.onboarding_permission_body, getString(R.string.app_name)))
            .setPositiveButton(R.string.accessibility_consent_action) { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                Toast.makeText(context, getString(R.string.accessibility_find_app_toast, getString(R.string.app_name)), Toast.LENGTH_LONG).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    companion object {
        private const val TAG = "accessibility_disclosure_dialog"

        fun show(activity: FragmentActivity) {
            if (activity.supportFragmentManager.findFragmentByTag(TAG) == null) {
                AccessibilityDisclosureDialog().show(activity.supportFragmentManager, TAG)
            }
        }
    }
}
