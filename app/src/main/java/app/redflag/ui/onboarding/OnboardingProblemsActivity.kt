package app.redflag.ui.onboarding

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.redflag.R
import app.redflag.databinding.ActivityOnboardingProblemsBinding
import app.redflag.ui.common.screenViewModel
import kotlinx.coroutines.launch

/** Accueil, étape 2 : choisir les problématiques importantes pour soi (elles deviennent des favoris). */
class OnboardingProblemsActivity : AppCompatActivity() {
    private val viewModel: OnboardingProblemsViewModel by screenViewModel { c, _, _ -> OnboardingProblemsViewModel(c) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityOnboardingProblemsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = OnboardingProblemsAdapter(
            onThemeClick = viewModel::toggleSection,
            onToggle = viewModel::toggle,
        )
        binding.list.adapter = adapter

        // L'accueil n'est marqué comme terminé qu'à la dernière étape : quitter avant le recommence au prochain lancement.
        binding.btnContinue.setOnClickListener {
            startActivity(Intent(this, OnboardingAppsActivity::class.java))
        }

        // Passer cette étape : sans problématique choisie, on associe directement des problématiques à des applications.
        binding.btnSkip.setOnClickListener { startActivity(Intent(this, OnboardingAppListActivity::class.java)) }
        binding.btnSkip.paintFlags = binding.btnSkip.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.rows.collect { adapter.submitList(it) } }
                launch {
                    viewModel.selectedCount.collect { count ->
                        binding.btnContinue.isEnabled = count >= 1
                        // "Continue · 2 selected": the number is part of the button once something is chosen.
                        binding.btnContinue.text = if (count >= 1) {
                            resources.getQuantityString(R.plurals.onboarding_continue_count, count, count)
                        } else {
                            getString(R.string.onboarding_continue)
                        }
                    }
                }
            }
        }
    }
}
