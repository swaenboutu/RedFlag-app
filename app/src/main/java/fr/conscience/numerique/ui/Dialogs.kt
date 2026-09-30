package fr.conscience.numerique.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.conscience.numerique.R
import fr.conscience.numerique.data.ProblemCatalog
import fr.conscience.numerique.databinding.DialogNewProblemBinding
import fr.conscience.numerique.util.normalizeCustomProblem

/** Champ de saisie avec les marges habituelles d'un dialogue (sans passer par `setView(view, l, t, r, b)`, API réservée). */
fun Context.dialogInput(input: EditText): View {
    val density = resources.displayMetrics.density
    return FrameLayout(this).apply {
        setPadding((24 * density).toInt(), (8 * density).toInt(), (24 * density).toInt(), 0)
        addView(input)
    }
}

/**
 * Dialogue « Nouvelle problématique » : saisie libre, dans la langue de son choix, et thème où la ranger.
 * [onSubmit] reçoit le texte et la clé du thème choisi ; null = « Personnalisé » (choix par défaut).
 */
fun showNewProblemDialog(context: Context, onSubmit: (label: String, category: String?) -> Unit) {
    val binding = DialogNewProblemBinding.inflate(LayoutInflater.from(context))

    // Première entrée = pas de thème (« Personnalisé »), puis les thèmes du catalogue dans leur ordre habituel.
    val names = listOf(context.getString(R.string.category_custom)) +
        ProblemCatalog.categories.map { context.getString(it.title) }
    val keys = listOf<String?>(null) + ProblemCatalog.categories.map { it.key }
    var selected = 0
    binding.problemTheme.setSimpleItems(names.toTypedArray())
    binding.problemTheme.setText(names[0], false)
    // Retrouvé par le nom affiché (et non par la position), au cas où la liste serait filtrée.
    binding.problemTheme.setOnItemClickListener { parent, _, position, _ ->
        selected = names.indexOf(parent.getItemAtPosition(position).toString()).coerceAtLeast(0)
    }

    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.dialog_new_problem_title)
        .setView(binding.root)
        .setPositiveButton(R.string.add) { _, _ ->
            normalizeCustomProblem(binding.problemLabel.text.toString())?.let { onSubmit(it, keys[selected]) }
        }
        .setNegativeButton(R.string.cancel, null)
        .show()
}
