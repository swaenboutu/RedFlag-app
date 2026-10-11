package app.redflag.ui.home

import androidx.fragment.app.FragmentActivity
import app.redflag.R
import app.redflag.container
import app.redflag.ui.common.OptionsSheet
import app.redflag.ui.common.SheetOption
import app.redflag.ui.common.formatPause
import app.redflag.util.PauseChoice
import app.redflag.util.resumeAt

/** "Pause": how long the interruption stays off. Opened from the home screen. */
class PauseSheet : OptionsSheet() {
    override val titleRes = R.string.pause_title
    override val confirmRes = R.string.pause_confirm

    override fun subtitle() = getString(R.string.pause_subtitle, getString(R.string.app_name))

    override fun options() = PauseChoice.ALL.map { choice ->
        when (choice) {
            is PauseChoice.Minutes -> SheetOption(formatPause(requireContext(), choice.minutes))
            PauseChoice.UntilMorning -> SheetOption(getString(R.string.pause_option_morning), getString(R.string.pause_option_morning_hint))
            PauseChoice.UntilTurnedBackOn -> SheetOption(getString(R.string.pause_option_forever), getString(R.string.pause_option_forever_hint))
        }
    }

    override fun initialIndex() = PauseChoice.ALL.indexOf(PauseChoice.DEFAULT)

    override fun onConfirm(index: Int) {
        val settings = requireContext().container.settings
        val at = PauseChoice.ALL[index].resumeAt(System.currentTimeMillis())
        if (at == null) settings.setInterceptionEnabled(false) else settings.disableInterceptionUntil(at)
    }

    companion object {
        private const val TAG = "pause_sheet"

        fun show(activity: FragmentActivity) {
            if (activity.supportFragmentManager.findFragmentByTag(TAG) == null) PauseSheet().show(activity.supportFragmentManager, TAG)
        }
    }
}
