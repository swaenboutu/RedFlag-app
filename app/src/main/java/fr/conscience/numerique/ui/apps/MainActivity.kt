package fr.conscience.numerique.ui.apps

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.conscience.numerique.R
import fr.conscience.numerique.container
import fr.conscience.numerique.data.SettingsStore
import fr.conscience.numerique.databinding.ActivityMainBinding
import fr.conscience.numerique.service.isFrictionServiceEnabled
import fr.conscience.numerique.ui.common.AccessibilityDisclosureDialog
import fr.conscience.numerique.ui.common.BottomNav
import fr.conscience.numerique.ui.common.screenViewModel
import fr.conscience.numerique.ui.onboarding.OnboardingActivity
import kotlinx.coroutines.launch

/** "Your apps": search, filters, and a card with the issues linked to each app. */
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by screenViewModel { c, _, _ -> MainViewModel(c) }
    private lateinit var binding: ActivityMainBinding
    private var resumedBefore = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // "Launch" = from the app icon, on a fresh screen: not a rotation, not a return from another screen of the app.
        val freshLaunch = savedInstanceState == null && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
        val settings = container.settings
        if (SettingsStore.shouldShowOnboarding(settings.onboardingDone, settings.alwaysShowOnboarding.value, freshLaunch)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = AppListAdapter { item ->
            startActivity(AppDetailActivity.intent(this, item.app.packageName, item.app.label))
        }
        binding.appList.adapter = adapter

        BottomNav.setup(this, binding.bottomBar.bottomNav, R.id.nav_apps)
        binding.enableService.setOnClickListener {
            AccessibilityDisclosureDialog.show(this)
        }
        binding.search.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }
        binding.filterAll.setOnClickListener { changeFilter(AppFilter.ALL) }
        binding.filterFlagged.setOnClickListener { changeFilter(AppFilter.FLAGGED) }
        binding.filterUnflagged.setOnClickListener { changeFilter(AppFilter.UNFLAGGED) }

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

    /**
     * Switches the filter and makes the change visible: the list restarts from the top and fades in. Without it, nothing seems to
     * happen when the first rows are the same before and after (for instance "All" -> "Flagged" when the flagged apps come first).
     */
    private fun changeFilter(filter: AppFilter) {
        if (viewModel.state.value.filter == filter) return
        viewModel.setFilter(filter)
        val list = binding.appList
        val shift = RELOAD_SHIFT_DP * resources.displayMetrics.density
        list.animate().cancel()
        list.scrollToPosition(0)
        list.alpha = RELOAD_START_ALPHA
        list.translationY = shift
        list.animate().alpha(1f).translationY(0f).setDuration(RELOAD_MS).start()
    }

    override fun onResume() {
        super.onResume()
        BottomNav.select(binding.bottomBar.bottomNav, R.id.nav_apps)
        binding.serviceBanner.visibility = if (isFrictionServiceEnabled(this)) View.GONE else View.VISIBLE
        // An app may have been installed or removed while the screen was in the background. The first display already loads the
        // list: only reload on later returns.
        if (resumedBefore) viewModel.reload()
        resumedBefore = true
    }

    private companion object {
        const val RELOAD_START_ALPHA = 0.2f
        const val RELOAD_SHIFT_DP = 16
        const val RELOAD_MS = 260L
    }
}
