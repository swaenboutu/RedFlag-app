package fr.conscience.numerique.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.conscience.numerique.R
import fr.conscience.numerique.databinding.ActivityProblemsManagerBinding
import kotlinx.coroutines.launch

/** « Vos problématiques » : thèmes repliables, chaque problématique ouvre son détail. */
class ProblemsManagerActivity : AppCompatActivity() {
    private val viewModel: ProblemsManagerViewModel by viewModels()
    private lateinit var binding: ActivityProblemsManagerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProblemsManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = ProblemsManagerAdapter(
            onThemeClick = { viewModel.toggle(it.id) },
            onProblemClick = { row -> row.toRef()?.let { startActivity(ProblemDetailActivity.intent(this, it)) } },
        )
        binding.list.adapter = adapter
        binding.newProblem.setOnClickListener { addProblem() }
        BottomNav.setup(this, binding.bottomBar.bottomNav, R.id.nav_problems)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.rows.collect { rows ->
                    // Si la liste est en haut, elle y reste : sans cela, une carte ajoutée en tête (« Vos favoris »)
                    // apparaîtrait au-dessus de la partie visible.
                    val atTop = !binding.list.canScrollVertically(-1)
                    adapter.submitList(rows) { if (atTop) binding.list.scrollToPosition(0) }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        BottomNav.select(binding.bottomBar.bottomNav, R.id.nav_problems)
    }

    private fun addProblem() = showNewProblemDialog(this) { label ->
        viewModel.add(label) { added ->
            if (added) {
                viewModel.expandCustom()
            } else {
                Toast.makeText(this, R.string.error_already_exists, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
