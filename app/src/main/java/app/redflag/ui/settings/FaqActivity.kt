package app.redflag.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.redflag.R
import app.redflag.data.loadFaq
import app.redflag.databinding.ActivityFaqBinding
import app.redflag.ui.common.BottomNav
import app.redflag.ui.common.screenViewModel
import kotlinx.coroutines.launch

/** « Comment ça marche » : la FAQ, des thèmes repliables qui contiennent des questions repliables. Le contenu est dans `res/raw/faq.xml`. */
class FaqActivity : AppCompatActivity() {
    private val viewModel: FaqViewModel by screenViewModel { _, _, _ -> FaqViewModel() }

    // Lue avec les ressources de l'écran (et non celles de l'application) : elles suivent la langue choisie pour l'app.
    private val faq by lazy { loadFaq() }
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
                viewModel.open.collect { adapter.submitList(faqRows(faq, it.themes, it.questions)) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        BottomNav.select(binding.bottomBar.bottomNav, R.id.nav_settings)
    }
}
