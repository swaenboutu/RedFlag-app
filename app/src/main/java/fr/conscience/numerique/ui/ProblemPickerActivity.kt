package fr.conscience.numerique.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.conscience.numerique.databinding.ActivityProblemPickerBinding
import fr.conscience.numerique.util.normalizeCustomProblem
import kotlinx.coroutines.launch

/** Choix des problématiques d'une app : catalogue prédéfini (traduit) + textes libres. */
class ProblemPickerActivity : AppCompatActivity() {
    private val viewModel: ProblemPickerViewModel by viewModels()
    private lateinit var binding: ActivityProblemPickerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProblemPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.title.text = intent.getStringExtra(PickerArgs.LABEL)

        val adapter = ProblemPickerAdapter(viewModel::toggleKey, viewModel::toggleCustom)
        binding.list.adapter = adapter

        binding.addCustom.setOnClickListener { addCustom() }
        binding.customInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) addCustom()
            true
        }
        binding.save.setOnClickListener { viewModel.save(onDone = ::finish) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.rows.collect { adapter.submitList(it) }
            }
        }
    }

    private fun addCustom() {
        val text = normalizeCustomProblem(binding.customInput.text.toString()) ?: return
        viewModel.addCustom(text)
        binding.customInput.text.clear()
    }

    companion object {
        fun intent(context: Context, packageName: String, label: String) =
            Intent(context, ProblemPickerActivity::class.java)
                .putExtra(PickerArgs.PACKAGE, packageName)
                .putExtra(PickerArgs.LABEL, label)
    }
}
