package fr.conscience.numerique.ui

import android.content.Context
import android.widget.EditText
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.conscience.numerique.R
import fr.conscience.numerique.util.normalizeCustomProblem

/** Dialogue « Nouvelle problématique » : saisie libre, dans la langue de son choix. */
fun showNewProblemDialog(context: Context, onLabel: (String) -> Unit) {
    val input = EditText(context).apply {
        hint = context.getString(R.string.picker_custom_hint)
        maxLines = 1
    }
    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.dialog_new_problem_title)
        .setView(input, 48, 16, 48, 0)
        .setPositiveButton(R.string.add) { _, _ ->
            normalizeCustomProblem(input.text.toString())?.let(onLabel)
        }
        .setNegativeButton(R.string.cancel, null)
        .show()
}
