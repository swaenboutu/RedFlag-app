package fr.conscience.numerique.ui.apps

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.conscience.numerique.R
import fr.conscience.numerique.databinding.ActivityAppDetailBinding
import fr.conscience.numerique.ui.common.BottomNav
import fr.conscience.numerique.ui.common.screenViewModel
import kotlinx.coroutines.launch

/** Détail d'une application : chaque case cochée est enregistrée aussitôt. */
class AppDetailActivity : AppCompatActivity() {
    private val viewModel: AppDetailViewModel by screenViewModel { c, _, h -> AppDetailViewModel(c, h) }
    private lateinit var binding: ActivityAppDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = AppDetailAdapter(
            onThemeClick = viewModel::toggleSection,
            onToggle = viewModel::toggle,
        )
        binding.list.adapter = adapter
        binding.back.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        BottomNav.setup(this, binding.bottomBar.bottomNav, R.id.nav_apps, isTabRoot = false)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.rows.collect { adapter.submitList(it) }
            }
        }
    }

    companion object {
        fun intent(context: Context, packageName: String, label: String) =
            Intent(context, AppDetailActivity::class.java)
                .putExtra(AppDetailArgs.PACKAGE, packageName)
                .putExtra(AppDetailArgs.LABEL, label)
    }
}
