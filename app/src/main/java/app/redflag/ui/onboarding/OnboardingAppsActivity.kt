package app.redflag.ui.onboarding

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.redflag.R
import app.redflag.databinding.ActivityOnboardingAppsBinding
import app.redflag.ui.common.dp
import app.redflag.ui.common.screenViewModel
import app.redflag.ui.common.themeIcon
import app.redflag.ui.common.underline
import kotlinx.coroutines.launch

/** Welcome tour, step 3: one page per chosen issue (the first few), to tick the apps concerned. The issue stays pinned above the list. */
class OnboardingAppsActivity : AppCompatActivity() {
    private val viewModel: OnboardingAppsViewModel by screenViewModel { c, ctx, _ -> OnboardingAppsViewModel(c, ctx) }
    private lateinit var binding: ActivityOnboardingAppsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingAppsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.topbar.topbarStep.text = getString(R.string.onboarding_step, 3, 4)
        binding.btnSkip.underline()

        val adapter = OnboardingAppsAdapter(viewModel::toggle, viewModel::setQuery) { viewModel.query.value }
        binding.list.adapter = adapter

        val finished = { startActivity(Intent(this, OnboardingPermissionActivity::class.java)) }
        binding.btnSkip.setOnClickListener { viewModel.skip(finished) }
        binding.btnNext.setOnClickListener { viewModel.next(finished) }

        // Retour : étape précédente ; sur la première, retour à la sélection des problématiques.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!viewModel.back()) finish()
            }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.rows.collect { rows ->
                    adapter.submitList(rows)
                    (rows.firstOrNull() as? AppsRow.Header)?.step?.let(::showStep)
                } }
                launch { viewModel.bottom.collect(::showBottom) }
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

    /** The pinned part: "Issue 1 of 2", the progress bar, the theme and the title of the issue. */
    private fun showStep(step: AppsStep) = with(binding) {
        this.step.text = getString(R.string.onboarding_issue_progress, step.position, step.total)
        problem.text = step.label
        themeName.setText(step.themeTitle)
        themeIcon(step.themeTitle)?.let { themeIcon.setImageResource(it) }
        showProgress(step.position, step.total)
    }

    /** One segment per issue, filled up to the current one. */
    private fun showProgress(position: Int, total: Int) = with(binding.progress) {
        if (childCount != total) {
            removeAllViews()
            repeat(total) { i ->
                addView(View(context), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                    if (i > 0) marginStart = 8.dp(this@with)
                })
            }
        }
        for (i in 0 until total) {
            getChildAt(i).background = GradientDrawable().apply {
                cornerRadius = 2.dp(this@with).toFloat()
                setColor(ContextCompat.getColor(context, if (i < position) R.color.ink else R.color.line))
            }
        }
    }

    /** The bottom: the button ("Next · 3 apps", or "Finish later" with its explanation on the last page) and the link. */
    private fun showBottom(bottom: AppsBottom) = with(binding) {
        moreInfo.visibility = if (bottom.finishLater) View.VISIBLE else View.GONE
        btnSkip.visibility = if (bottom.finishLater) View.GONE else View.VISIBLE
        bottomGap.visibility = if (bottom.finishLater) View.VISIBLE else View.GONE
        if (bottom.finishLater) {
            moreText.text = HtmlCompat.fromHtml(
                resources.getQuantityString(R.plurals.onboarding_apps_more, bottom.remaining, bottom.chosenTotal, bottom.remaining),
                HtmlCompat.FROM_HTML_MODE_LEGACY,
            )
            btnNext.setText(R.string.onboarding_finish_later)
            btnNext.isEnabled = true
        } else {
            btnNext.text = if (bottom.selectedCount > 0) {
                resources.getQuantityString(R.plurals.onboarding_next_count, bottom.selectedCount, bottom.selectedCount)
            } else {
                getString(R.string.onboarding_next)
            }
            btnNext.isEnabled = bottom.canSave
        }
    }
}
