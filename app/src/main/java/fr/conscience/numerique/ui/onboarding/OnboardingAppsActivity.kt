package fr.conscience.numerique.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.conscience.numerique.R
import fr.conscience.numerique.databinding.ActivityOnboardingAppsBinding
import fr.conscience.numerique.ui.common.screenViewModel
import fr.conscience.numerique.ui.common.underline
import kotlinx.coroutines.launch

/** Welcome tour, step 3: one page per chosen issue, to tick the apps concerned. The issue title stays pinned above the list. */
class OnboardingAppsActivity : AppCompatActivity() {
    private val viewModel: OnboardingAppsViewModel by screenViewModel { c, ctx, _ -> OnboardingAppsViewModel(c, ctx) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityOnboardingAppsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnSkip.underline()
        binding.btnSkipRemaining.underline()

        val adapter = OnboardingAppsAdapter(viewModel::toggle, viewModel::setQuery) { viewModel.query.value }
        binding.list.adapter = adapter

        val finished = { startActivity(Intent(this, OnboardingPermissionActivity::class.java)) }
        binding.btnSkip.setOnClickListener { viewModel.skip(finished) }
        binding.btnNext.setOnClickListener { viewModel.next(finished) }
        // Plusieurs problématiques à traiter : on peut aussi passer toutes celles qui restent d'un coup.
        binding.btnSkipRemaining.setOnClickListener { finished() }

        // Retour : étape précédente ; sur la première, retour à la sélection des problématiques.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!viewModel.back()) finish()
            }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.rows.collect { rows ->
                    adapter.submitList(rows)
                    val step = (rows.firstOrNull() as? AppsRow.Header)?.step
                    if (step != null) {
                        binding.step.text = getString(R.string.onboarding_apps_step, step.position, step.total)
                        binding.problem.text = step.label
                    }
                    binding.btnSkipRemaining.visibility = if (step != null && step.position < step.total) View.VISIBLE else View.GONE
                } }
                launch { viewModel.canSave.collect { binding.btnNext.isEnabled = it } }
                // Rien à traiter (aucune problématique choisie) : directement à la suite.
                launch {
                    viewModel.stepCount.collect { count ->
                        if (count == 0) {
                            finished()
                            finish()
                        }
                    }
                }
            }
        }
    }
}
