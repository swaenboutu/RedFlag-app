package app.redflag.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.redflag.databinding.ActivityOnboardingAppListBinding
import app.redflag.ui.apps.AppListAdapter
import app.redflag.ui.apps.MainViewModel
import app.redflag.ui.common.screenViewModel
import app.redflag.ui.common.underline
import kotlinx.coroutines.launch

/**
 * Accueil, étape 2 passée : sans problématique choisie, on part des applications (sans les apps système, selon le réglage).
 * Toucher une application ouvre sa fiche, où cocher une ou plusieurs problématiques ; « Suivant » mène aux autorisations.
 */
class OnboardingAppListActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by screenViewModel { c, _, _ -> MainViewModel(c) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityOnboardingAppListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnSkip.underline()

        val adapter = AppListAdapter { item -> startActivity(OnboardingAppDetailActivity.intent(this, item.app.packageName, item.app.label)) }
        binding.appList.adapter = adapter
        binding.search.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }

        val toPermission = { startActivity(Intent(this, OnboardingPermissionActivity::class.java)) }
        binding.btnNext.setOnClickListener { toPermission() }
        binding.btnSkip.setOnClickListener { toPermission() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    adapter.submitList(state.items)
                    // « Suivant » dès qu'une application a une problématique ; sinon, il reste « Passer ».
                    binding.btnNext.isEnabled = state.flaggedCount >= 1
                }
            }
        }
    }
}
