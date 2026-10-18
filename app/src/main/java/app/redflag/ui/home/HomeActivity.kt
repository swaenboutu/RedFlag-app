package app.redflag.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.redflag.R
import app.redflag.container
import app.redflag.data.SettingsStore
import app.redflag.databinding.ActivityHomeBinding
import app.redflag.service.FrictionMonitorService
import app.redflag.service.hasFrictionPermissions
import app.redflag.ui.common.PermissionDisclosureDialog
import app.redflag.ui.common.AppDrawer
import app.redflag.ui.common.BottomNav
import app.redflag.ui.common.dp
import app.redflag.ui.common.formatResumeTime
import app.redflag.ui.common.screenViewModel
import app.redflag.ui.common.themeIcon
import app.redflag.ui.common.underline
import app.redflag.ui.onboarding.OnboardingActivity
import app.redflag.ui.problems.ProblemDetailActivity
import app.redflag.ui.stats.AppStatsActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * The home screen, the first one of the app: whether the interruption is on (and a way to pause it), or why it cannot be; how many
 * launches the user chose not to do; the app they avoided the most; the issue linked to the most apps.
 */
class HomeActivity : AppCompatActivity() {
    private val viewModel: HomeViewModel by screenViewModel { c, ctx, _ -> HomeViewModel(c, ctx) }
    private lateinit var binding: ActivityHomeBinding

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
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        BottomNav.setup(this, binding.bottomBar.bottomNav, R.id.nav_home)
        AppDrawer.setup(this, binding.menuButton)
        binding.activateText.underline()

        binding.btnPause.setOnClickListener { PauseSheet.show(this) }
        binding.btnResume.setOnClickListener { settings.setInterceptionEnabled(true) }
        binding.btnActivate.setOnClickListener { PermissionDisclosureDialog.show(this) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                // A timed pause ends while the screen is open: turn the interruption back on at that moment.
                launch {
                    settings.reenableAt.collectLatest { at ->
                        if (at > 0L) {
                            delay((at - System.currentTimeMillis()).coerceAtLeast(0L) + 200L)
                            settings.isInterceptionActive()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!::binding.isInitialized) return
        BottomNav.select(binding.bottomBar.bottomNav, R.id.nav_home)
        val granted = hasFrictionPermissions(this)
        viewModel.setServiceEnabled(granted)
        if (granted) FrictionMonitorService.startIfPermitted(this)
    }

    private fun render(state: HomeState) = with(binding) {
        if (!state.loaded) return@with
        val appName = getString(R.string.app_name)

        // One card for the state of the interruption: off at the Android level, paused by the user, or on.
        cardInactive.visibility = if (!state.serviceEnabled) View.VISIBLE else View.GONE
        cardPaused.visibility = if (state.serviceEnabled && state.paused) View.VISIBLE else View.GONE
        cardActive.visibility = if (state.serviceEnabled && !state.paused) View.VISIBLE else View.GONE
        inactiveText.text = getString(R.string.home_inactive_text, appName)
        pausedText.text = if (state.resumeAt > 0L) {
            getString(R.string.home_paused_until, formatResumeTime(this@HomeActivity, state.resumeAt), appName)
        } else {
            getString(R.string.home_paused_forever, appName)
        }
        activeText.text = if (state.flaggedApps == 0) {
            getString(R.string.home_watching_none)
        } else {
            resources.getQuantityString(R.plurals.home_watching, state.flaggedApps, appName, state.flaggedApps)
        }

        avoidedValue.text = state.avoided.toString()
        avoidedLabel.text = resources.getQuantityString(R.plurals.home_avoided_label, state.avoided)
        avoidedSince.text = getString(R.string.home_avoided_since, startedDate(state.startedAt))

        // The two blocks below are always shown; until there is something to say they invite the user to start.
        val app = state.topApp
        topAppChevron.visibility = if (app == null) View.GONE else View.VISIBLE
        if (app != null) {
            topAppIcon.background = ContextCompat.getDrawable(this@HomeActivity, R.drawable.bg_icon_12)
            topAppIcon.setPadding(0, 0, 0, 0)
            topAppIcon.imageTintList = null
            topAppIcon.clipToOutline = true
            topAppIcon.setImageDrawable(container.icons.get(app.packageName))
            topAppName.text = app.label
            topAppCount.text = resources.getQuantityString(R.plurals.home_top_app_count, app.interruptions, app.interruptions)
            topAppCard.isClickable = true
            topAppCard.setOnClickListener { startActivity(AppStatsActivity.intent(this@HomeActivity, app.packageName, app.label)) }
        } else {
            topAppIcon.background = ContextCompat.getDrawable(this@HomeActivity, R.drawable.bg_app_icon_tile)
            topAppIcon.setPadding(14.dp(topAppIcon), 14.dp(topAppIcon), 14.dp(topAppIcon), 14.dp(topAppIcon))
            topAppIcon.imageTintList = ColorStateList.valueOf(MaterialColors.getColor(topAppIcon, com.google.android.material.R.attr.colorOnSurface))
            topAppIcon.setImageResource(R.drawable.ic_nav_apps)
            topAppName.setText(R.string.home_empty_name)
            topAppCount.setText(R.string.home_top_app_empty)
            topAppCard.isClickable = false
            topAppCard.setOnClickListener(null)
        }

        val issue = state.topIssue
        topIssueChevron.visibility = if (issue == null) View.GONE else View.VISIBLE
        if (issue != null) {
            topIssueIcon.setImageResource(themeIcon(state.topIssueTheme) ?: R.drawable.ic_nav_tag)
            topIssueName.text = issue.label
            topIssueApps.text = resources.getQuantityString(R.plurals.home_top_issue_apps, issue.apps, issue.apps, state.flaggedApps)
            showShare(issue.apps, state.flaggedApps)
            topIssueCard.isClickable = true
            topIssueCard.setOnClickListener { startActivity(ProblemDetailActivity.intent(this@HomeActivity, issue.ref)) }
        } else {
            topIssueIcon.setImageResource(R.drawable.ic_nav_tag)
            topIssueName.setText(R.string.home_empty_name)
            topIssueApps.setText(R.string.home_top_issue_empty)
            showShare(0, 0)
            topIssueCard.isClickable = false
            topIssueCard.setOnClickListener(null)
        }
    }

    /** The bar under the top issue: how many of the flagged apps have it. No bar until there is something to show. */
    private fun showShare(apps: Int, ofTotal: Int) {
        binding.topIssueBar.visibility = if (ofTotal > 0) View.VISIBLE else View.GONE
        if (ofTotal > 0) {
            binding.topIssueBar.max = ofTotal
            binding.topIssueBar.setProgressCompat(apps.coerceIn(0, ofTotal), false)
        }
    }

    /** "October 11" / "11 octobre", in the language of the app (DateUtils would follow the system language instead). */
    private fun startedDate(millis: Long): String =
        android.icu.text.DateFormat.getInstanceForSkeleton("MMMMd", resources.configuration.locales[0]).format(millis)
}
