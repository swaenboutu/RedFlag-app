package fr.conscience.numerique.ui.stats

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.conscience.numerique.R
import fr.conscience.numerique.container
import fr.conscience.numerique.data.StatsRange
import fr.conscience.numerique.data.displayLabel
import fr.conscience.numerique.databinding.ActivityAppStatsBinding
import fr.conscience.numerique.ui.common.BottomNav
import fr.conscience.numerique.ui.common.iconOf
import fr.conscience.numerique.ui.common.screenViewModel
import java.time.format.TextStyle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Statistics of one app: closed, opened and postponed interruptions, per day, per month or per year. */
class AppStatsActivity : AppCompatActivity() {
    private val viewModel: AppStatsViewModel by screenViewModel { c, _, h -> AppStatsViewModel(c, h) }
    private lateinit var binding: ActivityAppStatsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        BottomNav.setup(this, binding.bottomBar.bottomNav, R.id.nav_stats, isTabRoot = false)
        binding.back.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val packageName = intent.getStringExtra(AppStatsArgs.PACKAGE).orEmpty()
        binding.appIcon.clipToOutline = true
        binding.appIcon.setImageDrawable(iconOf(packageName))
        binding.appName.text = intent.getStringExtra(AppStatsArgs.LABEL) ?: packageName
        showIssues(packageName)

        binding.rangeDay.setOnClickListener { viewModel.setRange(StatsRange.DAY) }
        binding.rangeMonth.setOnClickListener { viewModel.setRange(StatsRange.MONTH) }
        binding.rangeYear.setOnClickListener { viewModel.setRange(StatsRange.YEAR) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { show(it) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        BottomNav.select(binding.bottomBar.bottomNav, R.id.nav_stats)
    }

    private fun show(state: AppStatsState) {
        binding.rangeDay.isSelected = state.range == StatsRange.DAY
        binding.rangeMonth.isSelected = state.range == StatsRange.MONTH
        binding.rangeYear.isSelected = state.range == StatsRange.YEAR

        val caption = getString(
            when (state.range) {
                StatsRange.DAY -> R.string.stats_range_caption_day
                StatsRange.MONTH -> R.string.stats_range_caption_month
                StatsRange.YEAR -> R.string.stats_range_caption_year
            },
        )
        binding.rangeCaption.text = caption
        binding.chartTitle.setText(
            when (state.range) {
                StatsRange.DAY -> R.string.stats_chart_title_day
                StatsRange.MONTH -> R.string.stats_chart_title_month
                StatsRange.YEAR -> R.string.stats_chart_title_year
            },
        )
        binding.headline.text = if (state.attempts == 0) {
            getString(R.string.stats_headline_none)
        } else {
            getString(R.string.stats_headline, state.blocked, state.attempts)
        }

        binding.metricClosed.text = state.blocked.toString()
        binding.metricOpened.text = state.bypassed.toString()
        binding.metricSnoozed.text = state.snoozed.toString()

        val locale = resources.configuration.locales[0]
        binding.chart.setBars(
            state.buckets.map { bucket ->
                val label = when (state.range) {
                    StatsRange.DAY -> bucket.start.dayOfMonth.toString()
                    StatsRange.MONTH -> bucket.start.month.getDisplayName(TextStyle.SHORT, locale)
                    StatsRange.YEAR -> bucket.start.year.toString()
                }
                ChartBar(label, bucket.blocked, bucket.bypassed, bucket.snoozed)
            },
        )
        binding.chartEmpty.visibility = if (state.attempts == 0) View.VISIBLE else View.GONE
        binding.chart.contentDescription =
            getString(R.string.stats_chart_description, caption, state.attempts, state.bypassed, state.snoozed)
    }

    /** Under the name: the issues this app is flagged for, separated by dots (nothing for an app that is not flagged). */
    private fun showIssues(packageName: String) {
        lifecycleScope.launch {
            val overrides = container.repository.labelOverrides.first()
            val labels = container.repository.find(packageName)?.problems.orEmpty().mapNotNull { it.displayLabel(this@AppStatsActivity, overrides) }
            // Short names: what comes before the parenthesis ("Addictive design (dark patterns...)" -> "Addictive design").
            binding.appSubtitle.text = labels.joinToString(" · ") { it.substringBefore(" (") }
            binding.appSubtitle.visibility = if (labels.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    companion object {
        fun intent(context: Context, packageName: String, label: String) =
            Intent(context, AppStatsActivity::class.java)
                .putExtra(AppStatsArgs.PACKAGE, packageName)
                .putExtra(AppStatsArgs.LABEL, label)
    }
}
