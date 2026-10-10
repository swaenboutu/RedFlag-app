package app.redflag.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import app.redflag.R
import app.redflag.container
import app.redflag.databinding.ItemPauseOptionBinding
import app.redflag.databinding.SheetPauseBinding
import app.redflag.ui.common.formatPause
import app.redflag.util.PauseChoice
import app.redflag.util.resumeAt
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * "Pause": how long the interruption stays off. Opened from the home screen and from the main switch of the Settings, so both
 * offer the same choices and turn it off the same way.
 */
class PauseSheet : BottomSheetDialogFragment() {
    private var chosen: PauseChoice = PauseChoice.DEFAULT

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = SheetPauseBinding.inflate(inflater, container, false)
        binding.subtitle.text = getString(R.string.pause_subtitle, getString(R.string.app_name))
        savedInstanceState?.getInt(KEY_CHOSEN)?.let { chosen = PauseChoice.ALL[it] }

        var baseTypeface: android.graphics.Typeface? = null
        val rows = PauseChoice.ALL.mapIndexed { index, choice ->
            val row = ItemPauseOptionBinding.inflate(inflater, binding.options, false)
            row.divider.visibility = if (index == 0) View.GONE else View.VISIBLE
            row.radio.isSaveEnabled = false // the choice is restored by the sheet itself, see KEY_CHOSEN
            row.title.text = titleOf(choice)
            if (baseTypeface == null) baseTypeface = row.title.typeface
            hintOf(choice)?.let {
                row.hint.text = it
                row.hint.visibility = View.VISIBLE
            }
            binding.options.addView(row.root)
            choice to row
        }
        fun refresh() = rows.forEach { (choice, row) ->
            row.radio.isChecked = choice == chosen
            row.title.setTypeface(baseTypeface, if (choice == chosen) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
        rows.forEach { (choice, row) ->
            row.row.setOnClickListener {
                chosen = choice
                refresh()
            }
        }
        refresh()

        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnConfirm.setOnClickListener {
            val settings = requireContext().container.settings
            val at = chosen.resumeAt(System.currentTimeMillis())
            if (at == null) settings.setInterceptionEnabled(false) else settings.disableInterceptionUntil(at)
            dismiss()
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        // The sheet takes the card color of the app (cream white / dark card), not Material's default tint.
        val sheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        sheet?.backgroundTintList = android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.surface_warm))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CHOSEN, PauseChoice.ALL.indexOf(chosen))
    }

    private fun titleOf(choice: PauseChoice): String = when (choice) {
        is PauseChoice.Minutes -> formatPause(requireContext(), choice.minutes)
        PauseChoice.UntilMorning -> getString(R.string.pause_option_morning)
        PauseChoice.UntilTurnedBackOn -> getString(R.string.pause_option_forever)
    }

    private fun hintOf(choice: PauseChoice): String? = when (choice) {
        is PauseChoice.Minutes -> null
        PauseChoice.UntilMorning -> getString(R.string.pause_option_morning_hint)
        PauseChoice.UntilTurnedBackOn -> getString(R.string.pause_option_forever_hint)
    }

    companion object {
        private const val TAG = "pause_sheet"
        private const val KEY_CHOSEN = "chosen"

        fun show(activity: FragmentActivity) {
            if (activity.supportFragmentManager.findFragmentByTag(TAG) == null) {
                PauseSheet().show(activity.supportFragmentManager, TAG)
            }
        }
    }
}
