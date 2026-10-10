package app.redflag.ui.apps

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.redflag.R
import app.redflag.container
import app.redflag.data.SettingsStore
import app.redflag.databinding.ActivityMainBinding
import app.redflag.service.isFrictionServiceEnabled
import app.redflag.ui.common.AccessibilityDisclosureDialog
import app.redflag.ui.common.AppDrawer
import app.redflag.ui.common.BottomNav
import app.redflag.ui.common.screenViewModel
import app.redflag.ui.onboarding.OnboardingActivity
import kotlinx.coroutines.launch

/** "Your apps": search, filters, and a card with the issues linked to each app. */
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by screenViewModel { c, _, _ -> MainViewModel(c) }
    private lateinit var binding: ActivityMainBinding
    private var resumedBefore = false

    /** The filter the user just picked, until the list for it has been shown (then the list goes back to the top). */
    private var pendingFilter: AppFilter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = AppListAdapter { item ->
            startActivity(AppDetailActivity.intent(this, item.app.packageName, item.app.label))
        }
        binding.appList.adapter = adapter

        BottomNav.setup(this, binding.bottomBar.bottomNav, R.id.nav_apps)
        AppDrawer.setup(this, binding.menuButton)
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
                    // After a filter change, go back to the top once the new list is in place: scrolling before would leave the
                    // old position when rows are inserted above (for instance "Not flagged" -> "All" brings the flagged apps first).
                    val toTop = pendingFilter == state.filter
                    if (toTop) pendingFilter = null
                    adapter.submitList(state.items) { if (toTop) binding.appList.scrollToPosition(0) }
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
        pendingFilter = filter
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
