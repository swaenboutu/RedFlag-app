package fr.conscience.numerique.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import fr.conscience.numerique.ConscienceApp
import fr.conscience.numerique.R
import fr.conscience.numerique.data.displayLabel
import fr.conscience.numerique.databinding.ActivityInterstitialBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Écran de friction : affiché avant qu'une app labelisée ne soit utilisée. */
class InterstitialActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInterstitialBinding
    private lateinit var targetPackage: String

    private val container get() = (application as ConscienceApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInterstitialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        targetPackage = intent.getStringExtra(EXTRA_PACKAGE) ?: return finish()

        // Le bouton retour équivaut à « Non » : ne jamais laisser l'app cible à découvert.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = decline()
        })

        binding.btnContinue.setOnClickListener { proceed() }
        binding.btnBack.setOnClickListener { decline() }
        binding.btnPause.setOnClickListener { pause() }

        lifecycleScope.launch {
            val monitored = container.repository.find(targetPackage)
            val overrides = container.repository.labelOverrides.first()
            binding.message.text = getString(R.string.interstitial_message, monitored?.app?.appName ?: targetPackage)
            binding.problems.text = monitored?.problems
                ?.mapNotNull { it.displayLabel(this@InterstitialActivity, overrides) }
                ?.joinToString("\n") { "• $it" }
                .orEmpty()
        }
    }

    private fun proceed() {
        container.frictionGate.allow(targetPackage)
        lifecycleScope.launch {
            container.repository.recordChoice(targetPackage, proceeded = true)
            finish()
        }
    }

    private fun decline() {
        lifecycleScope.launch {
            container.repository.recordChoice(targetPackage, proceeded = false)
            startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            finish()
        }
    }

    private fun pause() {
        lifecycleScope.launch {
            container.repository.snooze(targetPackage, System.currentTimeMillis() + PAUSE_MILLIS)
            proceed()
        }
    }

    companion object {
        private const val EXTRA_PACKAGE = "package"
        private const val PAUSE_MILLIS = 60 * 60 * 1000L

        fun intent(context: Context, packageName: String) =
            Intent(context, InterstitialActivity::class.java)
                .putExtra(EXTRA_PACKAGE, packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
    }
}
