package fr.conscience.numerique.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.conscience.numerique.databinding.ActivityOnboardingProblemsBinding
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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.rows.collect { adapter.submitList(it) } }
                launch { viewModel.selectedCount.collect { binding.btnContinue.isEnabled = it >= 1 } }
            }
        }
    }
}
