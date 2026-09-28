package fr.conscience.numerique.ui

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.conscience.numerique.R
import fr.conscience.numerique.databinding.ActivityMainBinding
import fr.conscience.numerique.service.FrictionAccessibilityService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = AppListAdapter { item ->
            startActivity(ProblemPickerActivity.intent(this, item.app.packageName, item.app.label))
        }
        binding.appList.adapter = adapter
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_problems) {
                startActivity(Intent(this, ProblemsManagerActivity::class.java))
                true
            } else {
                false
            }
        }
        binding.enableService.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.items.collect { adapter.submitList(it) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.serviceBanner.visibility = if (isServiceEnabled()) View.GONE else View.VISIBLE
    }

    private fun isServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        val mine = ComponentName(this, FrictionAccessibilityService::class.java)
        // Android stocke la forme courte ("pkg/.Classe") ou complète : comparer les composants, pas les chaînes.
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == mine }
    }
}
