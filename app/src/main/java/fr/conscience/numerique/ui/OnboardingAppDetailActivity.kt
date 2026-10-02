package fr.conscience.numerique.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.conscience.numerique.databinding.ActivityOnboardingAppDetailBinding
import kotlinx.coroutines.launch

/**
 * Accueil : les problématiques d'une application choisie dans la liste. Comme la fiche d'une app (chaque case cochée est
 * enregistrée aussitôt), mais sans la barre de navigation du bas : « Suivant » (une problématique cochée au moins) ou
 * « Passer » ramènent à la liste des applications.
 */
class OnboardingAppDetailActivity : AppCompatActivity() {
    private val viewModel: AppDetailViewModel by screenViewModel { c, _, h -> AppDetailViewModel(c, h) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityOnboardingAppDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = AppDetailAdapter(onThemeClick = viewModel::toggleSection, onToggle = viewModel::toggle)
        binding.list.adapter = adapter
        binding.btnNext.setOnClickListener { finish() }
        binding.btnSkip.setOnClickListener { finish() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.rows.collect { rows ->
                    adapter.submitList(rows)
                    binding.btnNext.isEnabled = ((rows.firstOrNull() as? DetailRow.Header)?.count ?: 0) >= 1
                }
            }
        }
    }

    companion object {
        fun intent(context: Context, packageName: String, label: String) =
            Intent(context, OnboardingAppDetailActivity::class.java)
                .putExtra(AppDetailArgs.PACKAGE, packageName)
                .putExtra(AppDetailArgs.LABEL, label)
    }
}
