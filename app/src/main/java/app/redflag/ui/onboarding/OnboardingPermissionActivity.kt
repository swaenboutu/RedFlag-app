package app.redflag.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import app.redflag.R
import app.redflag.container
import app.redflag.ui.common.setHtml
import app.redflag.ui.common.underline
import app.redflag.databinding.ActivityOnboardingPermissionBinding
import app.redflag.service.isFrictionServiceEnabled
import app.redflag.ui.apps.MainActivity

/**
 * Au retour des réglages d'Android ([openedSettings]), l'accueil est terminé dès que le service est activé : on arrive
 * directement dans l'application. Sans être passé par les réglages, l'écran reste affiché avec son bouton « Terminer ».
 */
fun shouldFinishOnboardingOnReturn(openedSettings: Boolean, serviceEnabled: Boolean) = openedSettings && serviceEnabled

/** Accueil, dernière étape : expliquer et demander l'activation du service d'accessibilité. */
class OnboardingPermissionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingPermissionBinding

    /** Vrai une fois les réglages d'accessibilité ouverts depuis cet écran (conservé en cas de rotation). */
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
        binding.step1.setHtml(R.string.onboarding_permission_step1, getString(R.string.accessibility_consent_action))
        binding.step2.setHtml(R.string.onboarding_permission_step2, appName)
        binding.step3.setHtml(R.string.onboarding_permission_step3)
        binding.sampleName.text = appName
        binding.note1.setHtml(R.string.onboarding_permission_note1, appName)
        binding.note2.setHtml(R.string.onboarding_permission_note2)
        binding.note3.setHtml(R.string.onboarding_permission_note3)
        binding.btnLater.setOnClickListener { finishOnboarding() }
    }

    /** L'utilisateur revient des réglages Android : on reflète l'état réel du service. */
    override fun onResume() {
        super.onResume()
        val enabled = isFrictionServiceEnabled(this)
        if (shouldFinishOnboardingOnReturn(openedSettings, enabled)) {
            finishOnboarding()
            return
        }
        binding.status.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.btnLater.visibility = if (enabled) View.GONE else View.VISIBLE
        if (enabled) {
            binding.btnMain.setText(R.string.onboarding_finish)
            binding.btnMain.setOnClickListener { finishOnboarding() }
        } else {
            binding.btnMain.setText(R.string.accessibility_consent_action)
            binding.btnMain.setOnClickListener {
                openedSettings = true
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                Toast.makeText(this, getString(R.string.accessibility_find_app_toast, getString(R.string.app_name)), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_OPENED_SETTINGS, openedSettings)
    }

    /** Fin de l'accueil (avec ou sans le service) : il ne s'affichera plus, et le retour ne mène plus à l'accueil. */
    private fun finishOnboarding() {
        container.settings.onboardingDone = true
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
    }

    private companion object {
        const val KEY_OPENED_SETTINGS = "openedSettings"
    }
}
