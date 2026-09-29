package fr.conscience.numerique.ui

import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.conscience.numerique.R
import fr.conscience.numerique.util.normalizeCustomProblem

/** Champ de saisie avec les marges habituelles d'un dialogue (sans passer par `setView(view, l, t, r, b)`, API réservée). */
fun Context.dialogInput(input: EditText): View {
    val density = resources.displayMetrics.density
    return FrameLayout(this).apply {
        setPadding((24 * density).toInt(), (8 * density).toInt(), (24 * density).toInt(), 0)
        addView(input)
    }
}

/** Dialogue « Nouvelle problématique » : saisie libre, dans la langue de son choix. */
fun showNewProblemDialog(context: Context, onLabel: (String) -> Unit) {
    val input = EditText(context).apply {
        hint = context.getString(R.string.picker_custom_hint)
        maxLines = 1
    }
    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.dialog_new_problem_title)
        .setView(context.dialogInput(input))
        .setPositiveButton(R.string.add) { _, _ ->
            normalizeCustomProblem(input.text.toString())?.let(onLabel)
        }
        .setNegativeButton(R.string.cancel, null)
        .show()
}
