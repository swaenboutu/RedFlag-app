package app.redflag.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import app.redflag.R
import app.redflag.container
import app.redflag.ui.common.setHtml
import app.redflag.ui.common.underline
import app.redflag.databinding.ActivityOnboardingPermissionBinding
import app.redflag.service.FrictionMonitorService
import app.redflag.service.FrictionPermission
import app.redflag.service.missingPermission
import app.redflag.service.openPermissionSettings
import app.redflag.ui.home.HomeActivity

/**
 * Back from Android's settings ([openedSettings]), the welcome tour is over as soon as both permissions are granted: the user goes
 * straight into the app. Without going through the settings, the screen stays with its "Finish" button.
 */
fun shouldFinishOnboardingOnReturn(openedSettings: Boolean, permissionsGranted: Boolean) = openedSettings && permissionsGranted

/**
 * Welcome tour, last step: explains the two permissions ("Usage access", then "Display over other apps") and asks for them one after
 * the other. The screen itself is the explanation shown before each of Android's settings screens.
 */
class OnboardingPermissionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingPermissionBinding

    /** True once Android's settings were opened from this screen (kept across a rotation). */
    private var openedSettings = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openedSettings = savedInstanceState?.getBoolean(KEY_OPENED_SETTINGS) ?: false
        binding = ActivityOnboardingPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnLater.underline()
        binding.topbar.topbarStep.text = getString(R.string.onboarding_step, 4, 4)

        val appName = getString(R.string.app_name)
        binding.body.setHtml(R.string.onboarding_permission_intro, appName)
        binding.step1.setHtml(R.string.onboarding_permission_step1, getString(R.string.permission_consent_action))
        binding.step2.setHtml(R.string.onboarding_permission_step2, appName)
        binding.step3.setHtml(R.string.onboarding_permission_step3, appName)
        binding.sampleName.text = appName
        binding.note1.setHtml(R.string.onboarding_permission_note1, appName)
        binding.note2.setHtml(R.string.onboarding_permission_note2)
        binding.note3.setHtml(R.string.onboarding_permission_note3)
        binding.btnLater.setOnClickListener { finishOnboarding() }
    }

    /** The user comes back from Android's settings: the screen shows the real state of the permissions. */
    override fun onResume() {
        super.onResume()
        val missing = missingPermission(this)
        if (missing == null) FrictionMonitorService.startIfPermitted(this)
        if (shouldFinishOnboardingOnReturn(openedSettings, missing == null)) {
            finishOnboarding()
            return
        }
        binding.status.visibility = if (missing != FrictionPermission.USAGE_ACCESS) View.VISIBLE else View.GONE
        binding.status.setText(if (missing == null) R.string.onboarding_permission_enabled else R.string.onboarding_permission_next_overlay)
        binding.btnLater.visibility = if (missing == null) View.GONE else View.VISIBLE
        if (missing == null) {
            binding.btnMain.setText(R.string.onboarding_finish)
            binding.btnMain.setOnClickListener { finishOnboarding() }
        } else {
            binding.btnMain.setText(R.string.permission_consent_action)
            binding.btnMain.setOnClickListener {
                openedSettings = true
                openPermissionSettings(this, missing)
                Toast.makeText(this, getString(R.string.permission_find_app_toast, getString(R.string.app_name)), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_OPENED_SETTINGS, openedSettings)
    }

    /** End of the welcome tour (with or without the permissions): it will not show again, and Back no longer leads to it. */
    private fun finishOnboarding() {
        container.settings.onboardingDone = true
        startActivity(
            Intent(this, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
    }

    private companion object {
        const val KEY_OPENED_SETTINGS = "openedSettings"
    }
}
