package fr.conscience.numerique.ui

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.conscience.numerique.R
import fr.conscience.numerique.databinding.ActivityMainBinding
import fr.conscience.numerique.service.FrictionAccessibilityService
import kotlinx.coroutines.launch

/** « Vos applications » : recherche, filtres, et une carte avec les problématiques associées à chaque app. */
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = AppListAdapter { item ->
            startActivity(AppDetailActivity.intent(this, item.app.packageName, item.app.label))
        }
        binding.appList.adapter = adapter

        BottomNav.setup(this, binding.bottomBar.bottomNav, R.id.nav_apps)
        binding.enableService.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        binding.search.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }
        binding.filterAll.setOnClickListener { viewModel.setFilter(AppFilter.ALL) }
        binding.filterFlagged.setOnClickListener { viewModel.setFilter(AppFilter.FLAGGED) }
        binding.filterUnflagged.setOnClickListener { viewModel.setFilter(AppFilter.UNFLAGGED) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    adapter.submitList(state.items)
                    binding.emptyState.visibility = if (state.loaded && state.items.isEmpty()) View.VISIBLE else View.GONE
                    binding.filterAll.isSelected = state.filter == AppFilter.ALL
                    binding.filterFlagged.isSelected = state.filter == AppFilter.FLAGGED
                    binding.filterUnflagged.isSelected = state.filter == AppFilter.UNFLAGGED
                    binding.filterFlagged.text = getString(R.string.filter_flagged, state.flaggedCount)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        BottomNav.select(binding.bottomBar.bottomNav, R.id.nav_apps)
        binding.serviceBanner.visibility = if (isServiceEnabled()) View.GONE else View.VISIBLE
    }

    private fun isServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        val mine = ComponentName(this, FrictionAccessibilityService::class.java)
        // Android stocke la forme courte ("pkg/.Classe") ou complète : comparer les composants, pas les chaînes.
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == mine }
    }
}
