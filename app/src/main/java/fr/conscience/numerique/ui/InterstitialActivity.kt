package fr.conscience.numerique.ui

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.text.Annotation
import android.text.SpannableString
import android.text.SpannedString
import android.text.style.ForegroundColorSpan
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import fr.conscience.numerique.container
import fr.conscience.numerique.R
import fr.conscience.numerique.data.displayLabel
import fr.conscience.numerique.databinding.ActivityInterstitialBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Écran de friction : affiché avant qu'une app labelisée ne soit utilisée. */
class InterstitialActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInterstitialBinding
    private val adapter = ProblemPillAdapter()
    private var loadJob: Job? = null

    /** App actuellement concernée. Change si l'écran est relancé pour une autre app (voir [onNewIntent]). */
    private var targetPackage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInterstitialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        targetPackage = intent.getStringExtra(EXTRA_PACKAGE) ?: return finish()

        // Le bouton retour équivaut à « Non » : ne jamais laisser l'app cible à découvert.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = decline()
        })

        binding.title.text = accentedTitle()
        binding.appIcon.clipToOutline = true
        binding.btnContinue.setOnClickListener { proceed() }
        binding.btnBack.setOnClickListener { decline() }
        binding.btnPause.text = getString(R.string.btn_pause, formatPause(this, container.settings.pauseMinutes.value))
        binding.btnPause.paintFlags = binding.btnPause.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.btnPause.setOnClickListener { pause() }

        binding.problemsList.adapter = adapter

        load(targetPackage)
    }

    /**
     * L'écran est en `singleTask` : si l'utilisateur le quitte sans répondre (Accueil) puis ouvre une autre app à
     * interrompre, Android réutilise cette instance au lieu d'en créer une, et appelle ceci. Sans cela, l'écran
     * resterait figé sur la première app.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: return
        if (pkg != targetPackage) {
            targetPackage = pkg
            load(pkg)
        }
    }

    /** Affiche l'app [pkg] ; une demande plus récente annule la précédente. */
    private fun load(pkg: String) {
        loadJob?.cancel()
        loadJob = lifecycleScope.launch {
            val monitored = container.repository.find(pkg)
            val overrides = container.repository.labelOverrides.first()
            val appName = monitored?.app?.appName ?: pkg
            val labels = monitored?.problems.orEmpty().mapNotNull { it.displayLabel(this@InterstitialActivity, overrides) }

            binding.appName.text = appName
            binding.appIcon.setImageDrawable(iconOf(pkg))
            binding.subtitle.text = resources.getQuantityString(R.plurals.interstitial_subtitle, labels.size, labels.size)
            binding.btnContinue.text = getString(R.string.btn_continue, appName)
            adapter.submit(labels)
        }
    }

    /** Le mot balisé `<annotation font="accent">` du titre passe en couleur d'accent. */
    private fun accentedTitle(): CharSequence {
        val source = getText(R.string.interstitial_title) as SpannedString
        val styled = SpannableString(source)
        val accent = ContextCompat.getColor(this, R.color.interstitial_accent)
        source.getSpans(0, source.length, Annotation::class.java)
            .filter { it.key == "font" && it.value == "accent" }
            .forEach {
                styled.setSpan(ForegroundColorSpan(accent), source.getSpanStart(it), source.getSpanEnd(it), 0)
            }
        return styled
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
            val pauseMillis = container.settings.pauseMinutes.value * 60_000L
            container.repository.snooze(targetPackage, System.currentTimeMillis() + pauseMillis)
            proceed()
        }
    }

    companion object {
        private const val EXTRA_PACKAGE = "package"

        fun intent(context: Context, packageName: String) =
            Intent(context, InterstitialActivity::class.java)
                .putExtra(EXTRA_PACKAGE, packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
    }
}
