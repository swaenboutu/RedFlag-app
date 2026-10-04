package fr.conscience.numerique.ui

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.conscience.numerique.R
import fr.conscience.numerique.data.ProblemCatalog
import fr.conscience.numerique.databinding.DialogNewProblemBinding
import fr.conscience.numerique.util.MAX_CATALOG_LENGTH
import fr.conscience.numerique.util.MAX_CUSTOM_LENGTH
import fr.conscience.numerique.util.normalizeCustomProblem

/**
 * Menu déroulant qui montre toujours tous ses choix. Un menu de saisie ordinaire filtre ses lignes d'après le texte du champ :
 * après une rotation, le thème restauré (« Données ») ne laisserait plus que lui dans la liste.
 */
private class AllItemsAdapter(context: Context, private val items: List<String>) :
    ArrayAdapter<String>(context, R.layout.item_dropdown, items) {
    private val keepAll = object : Filter() {
        override fun performFiltering(constraint: CharSequence?) = FilterResults().apply {
            values = items
            count = items.size
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) = notifyDataSetChanged()
    }

    override fun getFilter(): Filter = keepAll
}

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
 *
 * C'est un DialogFragment : il survit à la rotation de l'écran (le texte déjà tapé et le thème choisi sont conservés).
 * Le résultat arrive par [REQUEST_KEY] : à écouter avec [listen].
 */
class NewProblemDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val binding = DialogNewProblemBinding.inflate(LayoutInflater.from(context))

        // Première entrée = pas de thème (« Personnalisé »), puis les thèmes du catalogue dans leur ordre habituel.
        val names = listOf(context.getString(R.string.category_custom)) +
            ProblemCatalog.categories.map { context.getString(it.title) }
        val keys = listOf<String?>(null) + ProblemCatalog.categories.map { it.key }
        binding.problemTheme.setAdapter(AllItemsAdapter(context, names))
        // Le thème choisi se lit dans le champ (restauré tout seul après une rotation), pas dans une variable perdue avec elle.
        binding.problemTheme.setText(names[0], false)

        return MaterialAlertDialogBuilder(context)
            .setTitle(R.string.dialog_new_problem_title)
            .setView(binding.root)
            .setPositiveButton(R.string.add) { _, _ ->
                val label = normalizeCustomProblem(binding.problemLabel.text.toString(), MAX_CUSTOM_LENGTH) ?: return@setPositiveButton
                val category = keys[names.indexOf(binding.problemTheme.text.toString()).coerceAtLeast(0)]
                parentFragmentManager.setFragmentResult(REQUEST_KEY, bundleOf(LABEL to label, CATEGORY to category))
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    companion object {
        private const val REQUEST_KEY = "new_problem"
        private const val LABEL = "label"
        private const val CATEGORY = "category"
        private const val TAG = "new_problem_dialog"

        fun show(activity: FragmentActivity) {
            if (activity.supportFragmentManager.findFragmentByTag(TAG) == null) {
                NewProblemDialog().show(activity.supportFragmentManager, TAG)
            }
        }

        /** [onSubmit] reçoit le texte et la clé du thème choisi ; null = « Personnalisé ». À appeler dans onCreate. */
        fun listen(activity: FragmentActivity, onSubmit: (label: String, category: String?) -> Unit) {
            activity.supportFragmentManager.setFragmentResultListener(REQUEST_KEY, activity) { _, result ->
                onSubmit(checkNotNull(result.getString(LABEL)), result.getString(CATEGORY))
            }
        }
    }
}

/**
 * Dialogue « Modifier la problématique » : survit à la rotation comme [NewProblemDialog].
 * Le résultat est soit un nouveau texte, soit la demande de rétablir l'intitulé d'origine ([reset]).
 */
class EditProblemDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val args = requireArguments()
        val maxLength = args.getInt(MAX_LENGTH)
        val input = EditText(context).apply {
            // Le texte tapé avant une rotation est restauré par Android (le champ a un id) ; sinon on part de l'intitulé actuel.
            id = R.id.problemLabel
            // Un intitulé long (jusqu'à 120 caractères) passe à la ligne au lieu de défiler sur une seule : on le voit en entier.
            // « Entrée » valide (pas de saut de ligne), comme avec un champ sur une ligne.
            setRawInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
            setHorizontallyScrolling(false)
            maxLines = MAX_VISIBLE_LINES
            imeOptions = EditorInfo.IME_ACTION_DONE
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setText(args.getString(CURRENT))
            setSelection(text.length)
        }
        val builder = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.dialog_edit_title)
            .setView(context.dialogInput(input))
            .setPositiveButton(R.string.save) { _, _ ->
                val label = normalizeCustomProblem(input.text.toString(), maxLength) ?: return@setPositiveButton
                parentFragmentManager.setFragmentResult(REQUEST_KEY, bundleOf(LABEL to label))
            }
            .setNegativeButton(R.string.cancel, null)
        // Une problématique du catalogue déjà renommée peut retrouver son intitulé traduit d'origine.
        if (args.getBoolean(CAN_RESET)) {
            builder.setNeutralButton(R.string.action_reset_label) { _, _ ->
                parentFragmentManager.setFragmentResult(REQUEST_KEY, bundleOf(RESET to true))
            }
        }
        return builder.create()
    }

    /** Ce que l'utilisateur a demandé : un nouveau texte, ou le retour à l'intitulé d'origine. */
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
        private const val MAX_VISIBLE_LINES = 4
        private const val TAG = "edit_problem_dialog"

        /** [catalog] : problématique du catalogue (intitulé plus long autorisé) ; [canReset] : elle a été renommée. */
        fun show(activity: FragmentActivity, current: String, catalog: Boolean, canReset: Boolean) {
            if (activity.supportFragmentManager.findFragmentByTag(TAG) != null) return
            EditProblemDialog().apply {
                arguments = bundleOf(
                    CURRENT to current,
                    MAX_LENGTH to if (catalog) MAX_CATALOG_LENGTH else MAX_CUSTOM_LENGTH,
                    CAN_RESET to canReset,
                )
            }.show(activity.supportFragmentManager, TAG)
        }

        /** À appeler dans onCreate : le résultat arrive aussi après une rotation. */
        fun listen(activity: FragmentActivity, onResult: (Result) -> Unit) {
            activity.supportFragmentManager.setFragmentResultListener(REQUEST_KEY, activity) { _, result ->
                onResult(if (result.getBoolean(RESET)) Result.Reset else Result.Rename(checkNotNull(result.getString(LABEL))))
            }
        }
    }
}

/**
 * Avertissement avant d'ouvrir les réglages d'accessibilité, depuis le bandeau de l'écran principal et depuis les Réglages : ce que le
 * service lit, à quoi il sert, qu'aucune donnée ne quitte l'appareil, et un consentement explicite. Google Play l'exige pour toute
 * application qui n'est pas un outil d'accessibilité ; l'écran des autorisations de l'accueil joue le même rôle avec le même texte.
 */
class AccessibilityDisclosureDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        return MaterialAlertDialogBuilder(context)
            .setTitle(R.string.accessibility_disclosure_title)
            .setMessage(getString(R.string.onboarding_permission_body, getString(R.string.app_name)))
            .setPositiveButton(R.string.accessibility_consent_action) { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
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
