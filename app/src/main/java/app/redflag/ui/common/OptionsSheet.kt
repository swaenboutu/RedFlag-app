package app.redflag.ui.common

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import app.redflag.R
import app.redflag.databinding.ItemSheetOptionBinding
import app.redflag.databinding.SheetOptionsBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * The bottom sheet of the app: the card color of the app instead of Material's tint, fully open (also with the keyboard up).
 * Every choice or entry that is not a simple confirmation is shown this way (pause, "Don't ask again" duration, new issue).
 */
abstract class AppSheet : BottomSheetDialogFragment() {
    /** Fraction of the screen height the sheet takes (a sheet with a long list); null = as high as its content. */
    protected open val heightFraction: Float? = null

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? com.google.android.material.bottomsheet.BottomSheetDialog ?: return
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.surface_warm))
        heightFraction?.let { fraction ->
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
                sheet.layoutParams = sheet.layoutParams.apply { height = (resources.displayMetrics.heightPixels * fraction).toInt() }
            }
        }
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
    }
}

/** One line of an [OptionsSheet]: [title], and an optional smaller [hint] under it. */
data class SheetOption(val title: String, val hint: String? = null)

/**
 * A bottom sheet with a title, a sentence, a list of radio choices and Cancel / confirm buttons. Subclasses say what the choices
 * are and what the confirmation does; the selected choice survives a rotation.
 */
abstract class OptionsSheet : AppSheet() {
    protected abstract val titleRes: Int
    protected abstract val confirmRes: Int
    protected abstract fun subtitle(): String
    protected abstract fun options(): List<SheetOption>

    /** The choice selected when the sheet opens. */
    protected abstract fun initialIndex(): Int

    /** The user confirmed the choice at [index]; the sheet closes after this. */
    protected abstract fun onConfirm(index: Int)

    private var chosen = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = SheetOptionsBinding.inflate(inflater, container, false)
        binding.sheetTitle.setText(titleRes)
        binding.subtitle.text = subtitle()
        binding.btnConfirm.setText(confirmRes)
        chosen = savedInstanceState?.getInt(KEY_CHOSEN) ?: initialIndex()

        var baseTypeface: Typeface? = null
        val rows = options().mapIndexed { index, option ->
            val row = ItemSheetOptionBinding.inflate(inflater, binding.options, false)
            row.divider.visibility = if (index == 0) View.GONE else View.VISIBLE
            row.radio.isSaveEnabled = false // the choice is restored by the sheet itself, see KEY_CHOSEN
            row.title.text = option.title
            if (baseTypeface == null) baseTypeface = row.title.typeface
            option.hint?.let {
                row.hint.text = it
                row.hint.visibility = View.VISIBLE
            }
            binding.options.addView(row.root)
            row
        }
        fun refresh() = rows.forEachIndexed { index, row ->
            row.radio.isChecked = index == chosen
            row.title.setTypeface(baseTypeface, if (index == chosen) Typeface.BOLD else Typeface.NORMAL)
        }
        rows.forEachIndexed { index, row ->
            row.row.setOnClickListener {
                chosen = index
                refresh()
            }
        }
        refresh()

        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnConfirm.setOnClickListener {
            onConfirm(chosen)
            dismiss()
        }
        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CHOSEN, chosen)
    }

    private companion object {
        const val KEY_CHOSEN = "chosen"
    }
}
