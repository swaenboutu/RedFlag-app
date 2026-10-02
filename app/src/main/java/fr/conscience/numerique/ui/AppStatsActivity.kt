package fr.conscience.numerique.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.conscience.numerique.R
import fr.conscience.numerique.data.StatsRange
import fr.conscience.numerique.databinding.ActivityAppStatsBinding
import java.time.format.TextStyle
import kotlinx.coroutines.launch

/** Statistiques d'une app : tentatives, passages outre et mises en pause, par jour, par mois ou par an. */
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
        with(binding.header) {
            appIcon.clipToOutline = true
            appIcon.setImageDrawable(iconOf(packageName))
            appName.text = intent.getStringExtra(AppStatsArgs.LABEL) ?: packageName
        }

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
        binding.header.appSubtitle.text =
            resources.getQuantityString(R.plurals.stats_attempts, state.allTimeAttempts, state.allTimeAttempts)

        binding.metricAttempts.text = state.attempts.toString()
        binding.metricBypassed.text = state.bypassed.toString()
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

    companion object {
        fun intent(context: Context, packageName: String, label: String) =
            Intent(context, AppStatsActivity::class.java)
                .putExtra(AppStatsArgs.PACKAGE, packageName)
                .putExtra(AppStatsArgs.LABEL, label)
    }
}
