package app.redflag.ui.stats

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.redflag.R
import app.redflag.databinding.ActivityStatsBinding
import app.redflag.ui.common.AppDrawer
import app.redflag.ui.common.BottomNav
import app.redflag.ui.common.screenViewModel
import kotlinx.coroutines.launch

/** « Statistiques » : combien d'apps ont une interruption, et l'accès aux statistiques de chacune. */
class StatsActivity : AppCompatActivity() {
    private val viewModel: StatsViewModel by screenViewModel { c, _, _ -> StatsViewModel(c) }
    private lateinit var binding: ActivityStatsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        BottomNav.setup(this, binding.bottomBar.bottomNav, R.id.nav_stats)
        AppDrawer.setup(this, binding.menuButton)

        // Coins arrondis de la carte : `clipToOutline` en XML exige Android 12.
        binding.summaryCard.clipToOutline = true

        val adapter = StatsAdapter { app ->
            startActivity(AppStatsActivity.intent(this, app.packageName, app.appName))
        }
        binding.list.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    adapter.submitList(state.rows)
                    binding.emptyState.visibility = if (state.rows.isEmpty()) View.VISIBLE else View.GONE
                    binding.summaryCaption.setText(R.string.stats_summary_caption)
                    val installed = state.installedCount
                    if (installed == null) {
                        binding.summaryValue.text = state.interceptedCount.toString()
                        binding.summaryProgress.visibility = View.INVISIBLE
                    } else {
                        // Une app désinstallée depuis peut rester surveillée : le total ne descend jamais sous le nombre d'apps interrompues.
                        val total = maxOf(installed, state.interceptedCount)
                        binding.summaryValue.text = getString(R.string.stats_summary_value, state.interceptedCount, total)
                        binding.summaryProgress.visibility = View.VISIBLE
                        binding.summaryProgress.max = maxOf(total, 1)
                        binding.summaryProgress.progress = state.interceptedCount
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        BottomNav.select(binding.bottomBar.bottomNav, R.id.nav_stats)
    }
}
