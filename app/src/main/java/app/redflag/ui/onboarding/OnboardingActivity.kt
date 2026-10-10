package app.redflag.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import app.redflag.R
import app.redflag.databinding.ActivityOnboardingBinding

/** Écran d'accueil, affiché au tout premier lancement seulement. */
class OnboardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val appName = getString(R.string.app_name)
        binding.topbar.topbarName.visibility = View.VISIBLE
        binding.topbar.topbarStep.text = getString(R.string.onboarding_step, 1, 4)
        binding.flagTitle.setText(R.string.onboarding_welcome_flag_title)
        binding.flagBody.setText(R.string.onboarding_welcome_flag_body)
        binding.pauseTitle.setText(R.string.onboarding_welcome_pause_title)
        binding.pauseBody.text = getString(R.string.onboarding_welcome_pause_body, appName)
        binding.decideTitle.setText(R.string.onboarding_welcome_decide_title)
        binding.decideBody.setText(R.string.onboarding_welcome_decide_body)
        binding.privateBody.text = getString(R.string.onboarding_welcome_private_body, appName)

        binding.btnStart.setOnClickListener {
            startActivity(Intent(this, OnboardingProblemsActivity::class.java))
            finish()
        }
    }
}
