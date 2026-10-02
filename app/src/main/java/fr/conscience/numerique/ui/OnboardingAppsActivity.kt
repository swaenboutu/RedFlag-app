package fr.conscience.numerique.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.conscience.numerique.databinding.ActivityOnboardingAppsBinding
import kotlinx.coroutines.launch

/** Accueil, étape 3 : une page par problématique choisie, pour cocher les applications concernées. */
class OnboardingAppsActivity : AppCompatActivity() {
    private val viewModel: OnboardingAppsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityOnboardingAppsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = OnboardingAppsAdapter(viewModel::toggle, viewModel::setQuery) { viewModel.query.value }
        binding.list.adapter = adapter

        val finished = { startActivity(Intent(this, OnboardingPermissionActivity::class.java)) }
        binding.btnNext.setOnClickListener { viewModel.next(finished) }
        binding.btnSkip.setOnClickListener { viewModel.skip(finished) }

        // Retour : étape précédente ; sur la première, retour à la sélection des problématiques.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!viewModel.back()) finish()
            }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.rows.collect { adapter.submitList(it) } }
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
