package app.redflag.ui.settings

import androidx.fragment.app.FragmentActivity
import app.redflag.R
import app.redflag.container
import app.redflag.data.SettingsStore
import app.redflag.ui.common.OptionsSheet
import app.redflag.ui.common.SheetOption
import app.redflag.ui.common.formatPause

/** How long the "Don't ask again" link of the interruption screen pauses the interruption for one app. */
class SnoozeSheet : OptionsSheet() {
    override val titleRes = R.string.settings_pause_title
    override val confirmRes = R.string.save

    override fun subtitle() = getString(R.string.settings_pause_subtitle)

    override fun options() = SettingsStore.PAUSE_CHOICES.map { SheetOption(formatPause(requireContext(), it)) }

    override fun initialIndex() =
        SettingsStore.PAUSE_CHOICES.indexOf(requireContext().container.settings.pauseMinutes.value).coerceAtLeast(0)

    override fun onConfirm(index: Int) = requireContext().container.settings.setPauseMinutes(SettingsStore.PAUSE_CHOICES[index])

    companion object {
        private const val TAG = "snooze_sheet"

        fun show(activity: FragmentActivity) {
            if (activity.supportFragmentManager.findFragmentByTag(TAG) == null) SnoozeSheet().show(activity.supportFragmentManager, TAG)
        }
    }
}
