package fr.conscience.numerique.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import fr.conscience.numerique.databinding.ActivityOnboardingBinding

/** Écran d'accueil, affiché au tout premier lancement seulement. */
class OnboardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStart.setOnClickListener {
            startActivity(Intent(this, OnboardingProblemsActivity::class.java))
            finish()
        }
    }
}
