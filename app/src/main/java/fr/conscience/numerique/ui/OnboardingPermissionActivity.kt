package fr.conscience.numerique.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import fr.conscience.numerique.container
import fr.conscience.numerique.R
import fr.conscience.numerique.databinding.ActivityOnboardingPermissionBinding
import fr.conscience.numerique.service.isFrictionServiceEnabled

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

        binding.body.text = getString(R.string.onboarding_permission_body, getString(R.string.app_name))
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
            binding.btnMain.setText(R.string.onboarding_permission_action)
            binding.btnMain.setOnClickListener {
                openedSettings = true
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
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
