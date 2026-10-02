package fr.conscience.numerique.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.conscience.numerique.R
import fr.conscience.numerique.data.loadFaq
import fr.conscience.numerique.databinding.ActivityFaqBinding
import kotlinx.coroutines.launch

/** « Comment ça marche » : la FAQ, des thèmes repliables qui contiennent des questions repliables. Le contenu est dans `res/raw/faq.xml`. */
class FaqActivity : AppCompatActivity() {
    private val viewModel: FaqViewModel by screenViewModel { _, ctx, _ -> FaqViewModel(ctx.loadFaq()) }
    private lateinit var binding: ActivityFaqBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaqBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Écran ouvert depuis les Réglages : toucher cet onglet referme la FAQ et ramène à la liste des réglages.
        BottomNav.setup(this, binding.bottomBar.bottomNav, R.id.nav_settings, isTabRoot = false)

        val adapter = FaqAdapter(
            onThemeClick = { viewModel.toggleTheme(it.id) },
            onQuestionClick = { viewModel.toggleQuestion(it.themeId, it.id) },
        )
        binding.list.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.rows.collect { adapter.submitList(it) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        BottomNav.select(binding.bottomBar.bottomNav, R.id.nav_settings)
    }
}
