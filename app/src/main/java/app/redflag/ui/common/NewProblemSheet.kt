package app.redflag.ui.common

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import app.redflag.R
import app.redflag.data.ProblemCatalog
import app.redflag.databinding.SheetNewProblemBinding
import app.redflag.util.MAX_CUSTOM_LENGTH
import app.redflag.util.normalizeCustomProblem

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

/**
 * "New issue": a bottom sheet to type the issue, in the language of one's choice, and pick the theme to put it in.
 *
 * It survives a rotation (the text typed and the theme chosen are kept). The result arrives through [REQUEST_KEY]: listen with [listen].
 */
class NewProblemSheet : AppSheet() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val binding = SheetNewProblemBinding.inflate(inflater, container, false)

        // First entry = no theme ("Custom"), then the catalog themes in their usual order.
        val names = listOf(context.getString(R.string.theme_none)) +
            ProblemCatalog.categories.map { context.getString(it.title) }
        val keys = listOf<String?>(null) + ProblemCatalog.categories.map { it.key }
        binding.problemTheme.setAdapter(AllItemsAdapter(context, names))
        // The chosen theme is read from the field (restored by itself after a rotation), not from a variable lost with it.
        binding.problemTheme.setText(names[0], false)
        binding.themeHint.text = context.getString(R.string.new_issue_theme_hint, context.getString(R.string.category_custom))

        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnConfirm.setOnClickListener {
            val label = normalizeCustomProblem(binding.problemLabel.text.toString(), MAX_CUSTOM_LENGTH) ?: return@setOnClickListener
            val category = keys[names.indexOf(binding.problemTheme.text.toString()).coerceAtLeast(0)]
            parentFragmentManager.setFragmentResult(REQUEST_KEY, bundleOf(LABEL to label, CATEGORY to category))
            dismiss()
        }
        return binding.root
    }

    companion object {
        private const val REQUEST_KEY = "new_problem"
        private const val LABEL = "label"
        private const val CATEGORY = "category"
        private const val TAG = "new_problem_sheet"

        fun show(activity: FragmentActivity) {
            if (activity.supportFragmentManager.findFragmentByTag(TAG) == null) NewProblemSheet().show(activity.supportFragmentManager, TAG)
        }

        /** [onSubmit] gets the text and the key of the chosen theme (null = "Custom"). Call it in onCreate. */
        fun listen(activity: FragmentActivity, onSubmit: (label: String, category: String?) -> Unit) {
            activity.supportFragmentManager.setFragmentResultListener(REQUEST_KEY, activity) { _, result ->
                onSubmit(checkNotNull(result.getString(LABEL)), result.getString(CATEGORY))
            }
        }
    }
}
