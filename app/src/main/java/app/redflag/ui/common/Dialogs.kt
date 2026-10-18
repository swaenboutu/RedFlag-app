package app.redflag.ui.common

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import app.redflag.R
import app.redflag.service.FrictionPermission
import app.redflag.service.missingPermission
import app.redflag.service.openPermissionSettings

/**
 * Warning before opening one of Android's permission screens, from the Home screen and from Settings: which permission, what it is
 * used for, what is read and not, that no data leaves the device, and an explicit consent. Shown for the first permission still
 * missing ("Usage access", then "Display over other apps"): one explanation before each settings screen.
 */
class PermissionDisclosureDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val permission = FrictionPermission.valueOf(requireArguments().getString(ARG_PERMISSION)!!)
        val appName = getString(R.string.app_name)
        return MaterialAlertDialogBuilder(context)
            .setTitle(titleOf(permission))
            .setMessage(getString(bodyOf(permission), appName))
            .setPositiveButton(R.string.permission_consent_action) { _, _ ->
                openPermissionSettings(context, permission)
                Toast.makeText(context, getString(R.string.permission_find_app_toast, appName), Toast.LENGTH_LONG).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    companion object {
        private const val TAG = "permission_disclosure_dialog"
        private const val ARG_PERMISSION = "permission"

        fun titleOf(permission: FrictionPermission) = when (permission) {
            FrictionPermission.USAGE_ACCESS -> R.string.usage_disclosure_title
            FrictionPermission.OVERLAY -> R.string.overlay_disclosure_title
        }

        fun bodyOf(permission: FrictionPermission) = when (permission) {
            FrictionPermission.USAGE_ACCESS -> R.string.usage_disclosure_body
            FrictionPermission.OVERLAY -> R.string.overlay_disclosure_body
        }

        /** Explains the first missing permission before opening its settings (both granted: the first one, to review or withdraw it). */
        fun show(activity: FragmentActivity) {
            val permission = missingPermission(activity) ?: FrictionPermission.USAGE_ACCESS
            if (activity.supportFragmentManager.findFragmentByTag(TAG) == null) {
                PermissionDisclosureDialog().apply { arguments = bundleOf(ARG_PERMISSION to permission.name) }
                    .show(activity.supportFragmentManager, TAG)
            }
        }
    }
}
