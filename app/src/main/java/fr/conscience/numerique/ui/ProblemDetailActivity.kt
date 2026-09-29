package fr.conscience.numerique.ui

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.conscience.numerique.R
import fr.conscience.numerique.data.ProblemCatalog
import fr.conscience.numerique.data.ProblemRef
import fr.conscience.numerique.databinding.ActivityProblemDetailBinding
import fr.conscience.numerique.util.normalizeCustomProblem
import kotlinx.coroutines.launch

/** Détail d'une problématique : intitulé modifiable, apps liées (dissociables), ajout de liens. */
class ProblemDetailActivity : AppCompatActivity() {
    private val viewModel: ProblemDetailViewModel by viewModels()
    private lateinit var binding: ActivityProblemDetailBinding
    private var state: DetailState? = null

    private val pickApps = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data ?: return@registerForActivityResult
        val packages = data.getStringArrayListExtra(AppPickerActivity.RESULT_PACKAGES).orEmpty()
        val labels = data.getStringArrayListExtra(AppPickerActivity.RESULT_LABELS).orEmpty()
        viewModel.link(packages.zip(labels) { pkg, label -> LinkedApp(pkg, label) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProblemDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = LinkedAppsAdapter { viewModel.unlink(it.packageName) }
        binding.linkedApps.adapter = adapter

        binding.back.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.edit.setOnClickListener { showEditDialog() }
        binding.favorite.setOnClickListener { viewModel.toggleFavorite() }
        binding.linkApps.setOnClickListener {
            val linked = ArrayList(state?.linkedApps.orEmpty().map { it.packageName })
            pickApps.launch(AppPickerActivity.intent(this, linked))
        }
        binding.deleteProblem.setOnClickListener { showDeleteDialog() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { current ->
                    state = current ?: return@collect
                    binding.title.text = titleOf(current)
                    showFavorite(current.favorite)
                    binding.noApps.visibility = if (current.linkedApps.isEmpty()) View.VISIBLE else View.GONE
                    binding.deleteProblem.visibility = if (current.ref.customLabel != null) View.VISIBLE else View.GONE
                    adapter.submitList(current.linkedApps)
                }
            }
        }
    }

    /** Étoile pleine et ambre quand la problématique est en favori, contour sinon. */
    private fun showFavorite(favorite: Boolean) = with(binding.favorite) {
        setImageResource(if (favorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
        imageTintList = ColorStateList.valueOf(
            if (favorite) {
                ContextCompat.getColor(context, R.color.favorite_star)
            } else {
                MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface)
            },
        )
        contentDescription = getString(if (favorite) R.string.action_favorite_remove else R.string.action_favorite_add)
    }

    private fun titleOf(state: DetailState): String {
        val predefined = state.ref.catalogKey?.let(ProblemCatalog::find)
        return if (predefined != null) state.override ?: getString(predefined.label) else state.ref.customLabel.orEmpty()
    }

    private fun showEditDialog() {
        val current = state ?: return
        val input = EditText(this).apply {
            setText(titleOf(current))
            setSelection(text.length)
            maxLines = 1
        }
        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_edit_title)
            .setView(dialogInput(input))
            .setPositiveButton(R.string.save) { _, _ ->
                val label = normalizeCustomProblem(input.text.toString()) ?: return@setPositiveButton
                viewModel.rename(label) { ok ->
                    if (!ok) Toast.makeText(this, R.string.error_already_exists, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
        // Une problématique du catalogue déjà renommée peut retrouver son intitulé traduit d'origine.
        if (current.override != null) builder.setNeutralButton(R.string.action_reset_label) { _, _ -> viewModel.resetLabel() }
        builder.show()
    }

    private fun showDeleteDialog() {
        val current = state ?: return
        val apps = current.linkedApps.size
        val label = titleOf(current)
        val message = if (apps == 0) {
            label
        } else {
            getString(R.string.dialog_delete_message, label, resources.getQuantityString(R.plurals.apps_count, apps, apps))
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_delete_title)
            .setMessage(message)
            .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.deleteCustom(onDone = ::finish) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    companion object {
        fun intent(context: Context, ref: ProblemRef) =
            Intent(context, ProblemDetailActivity::class.java)
                .putExtra(DetailArgs.CATALOG_KEY, ref.catalogKey)
                .putExtra(DetailArgs.CUSTOM_LABEL, ref.customLabel)
    }
}
