package fr.conscience.numerique.ui.apps

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.conscience.numerique.databinding.ActivityAppPickerBinding
import fr.conscience.numerique.ui.common.screenViewModel
import kotlinx.coroutines.launch

/** Choix de plusieurs apps (avec recherche) ; renvoie les paquets et noms choisis. */
class AppPickerActivity : AppCompatActivity() {
    private val viewModel: AppPickerViewModel by screenViewModel { c, _, h -> AppPickerViewModel(c, h) }
    private lateinit var binding: ActivityAppPickerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = AppPickerAdapter(viewModel::toggle)
        binding.list.adapter = adapter
        binding.search.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }
        binding.link.setOnClickListener { finishWithSelection() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.rows.collect { adapter.submitList(it) } }
                launch { viewModel.selectedCount.collect { binding.link.isEnabled = it > 0 } }
            }
        }
    }

    private fun finishWithSelection() {
        val apps = viewModel.selectedApps()
        setResult(
            RESULT_OK,
            Intent()
                .putStringArrayListExtra(RESULT_PACKAGES, ArrayList(apps.map { it.packageName }))
                .putStringArrayListExtra(RESULT_LABELS, ArrayList(apps.map { it.label })),
        )
        finish()
    }

    companion object {
        const val RESULT_PACKAGES = "packages"
        const val RESULT_LABELS = "labels"

        /** [excluded] : paquets déjà liés, à ne pas proposer. */
        fun intent(context: Context, excluded: ArrayList<String>) =
            Intent(context, AppPickerActivity::class.java).putStringArrayListExtra(AppPickerArgs.EXCLUDED, excluded)
    }
}
