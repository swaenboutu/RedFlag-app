package fr.conscience.numerique.ui

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.conscience.numerique.R
import fr.conscience.numerique.databinding.ActivityProblemsManagerBinding
import fr.conscience.numerique.util.normalizeCustomProblem
import kotlinx.coroutines.launch

/** Liste toutes les problématiques ; chacune ouvre son écran de détail. On peut aussi en ajouter. */
class ProblemsManagerActivity : AppCompatActivity() {
    private val viewModel: ProblemsManagerViewModel by viewModels()
    private lateinit var binding: ActivityProblemsManagerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProblemsManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = ProblemsManagerAdapter { row ->
            row.toRef()?.let { startActivity(ProblemDetailActivity.intent(this, it)) }
        }
        binding.list.adapter = adapter

        binding.addCustom.setOnClickListener { addCustom() }
        binding.customInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) addCustom()
            true
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.rows.collect { adapter.submitList(it) }
            }
        }
    }

    private fun addCustom() {
        val text = normalizeCustomProblem(binding.customInput.text.toString()) ?: return
        viewModel.add(text) { added ->
            if (added) {
                binding.customInput.text.clear()
            } else {
                Toast.makeText(this, R.string.error_already_exists, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
