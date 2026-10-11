package app.redflag.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import app.redflag.databinding.SheetEditProblemBinding
import app.redflag.util.MAX_CATALOG_LENGTH
import app.redflag.util.MAX_CUSTOM_LENGTH
import app.redflag.util.normalizeCustomProblem

/**
 * "Edit issue": a bottom sheet to change the title of an issue, like the one that adds an issue. It survives a rotation (the text typed
 * is restored by Android, the field has an id). The result is either a new text or the request to restore the original title.
 */
class EditProblemSheet : AppSheet() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = SheetEditProblemBinding.inflate(inflater, container, false)
        val args = requireArguments()
        val maxLength = args.getInt(MAX_LENGTH)

        // Without a saved text (first display), start from the current title.
        if (savedInstanceState == null) {
            binding.problemLabel.setText(args.getString(CURRENT))
            binding.problemLabel.setSelection(binding.problemLabel.text.length)
        }
        // A catalog issue that was renamed can get its original (translated) title back.
        if (args.getBoolean(CAN_RESET)) {
            binding.btnReset.visibility = View.VISIBLE
            binding.btnReset.setOnClickListener {
                parentFragmentManager.setFragmentResult(REQUEST_KEY, bundleOf(RESET to true))
                dismiss()
            }
        }
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnConfirm.setOnClickListener {
            val label = normalizeCustomProblem(binding.problemLabel.text.toString(), maxLength) ?: return@setOnClickListener
            parentFragmentManager.setFragmentResult(REQUEST_KEY, bundleOf(LABEL to label))
            dismiss()
        }
        return binding.root
    }

    /** What the user asked for: a new text, or the original title back. */
    sealed interface Result {
        data class Rename(val label: String) : Result
        data object Reset : Result
    }

    companion object {
        private const val REQUEST_KEY = "edit_problem"
        private const val LABEL = "label"
        private const val RESET = "reset"
        private const val CURRENT = "current"
        private const val MAX_LENGTH = "max_length"
        private const val CAN_RESET = "can_reset"
        private const val TAG = "edit_problem_sheet"

        /** [catalog]: an issue of the catalog (a longer title is allowed); [canReset]: it was renamed. */
        fun show(activity: FragmentActivity, current: String, catalog: Boolean, canReset: Boolean) {
            if (activity.supportFragmentManager.findFragmentByTag(TAG) != null) return
            EditProblemSheet().apply {
                arguments = bundleOf(
                    CURRENT to current,
                    MAX_LENGTH to if (catalog) MAX_CATALOG_LENGTH else MAX_CUSTOM_LENGTH,
                    CAN_RESET to canReset,
                )
            }.show(activity.supportFragmentManager, TAG)
        }

        /** Call it in onCreate: the result also arrives after a rotation. */
        fun listen(activity: FragmentActivity, onResult: (Result) -> Unit) {
            activity.supportFragmentManager.setFragmentResultListener(REQUEST_KEY, activity) { _, result ->
                onResult(if (result.getBoolean(RESET)) Result.Reset else Result.Rename(checkNotNull(result.getString(LABEL))))
            }
        }
    }
}
