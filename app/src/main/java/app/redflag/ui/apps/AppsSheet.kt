package app.redflag.ui.apps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.redflag.R
import app.redflag.container
import app.redflag.databinding.SheetAppsBinding
import app.redflag.ui.common.AppSheet
import app.redflag.ui.problems.LinkedApp
import kotlinx.coroutines.launch

/**
 * "Link apps": a bottom sheet to tick the apps to link to an issue (with a search). The apps already linked are not offered.
 * The result arrives through [REQUEST_KEY]: listen with [listen].
 */
class AppsSheet : AppSheet() {
    override val heightFraction = 0.82f

    private val viewModel: AppPickerViewModel by lazy {
        val factory = viewModelFactory { initializer { AppPickerViewModel(requireContext().container, createSavedStateHandle()) } }
        ViewModelProvider(this, factory)[AppPickerViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = SheetAppsBinding.inflate(inflater, container, false)
        binding.subtitle.text = getString(R.string.sheet_apps_subtitle, requireArguments().getString(ISSUE_LABEL))

        val adapter = AppPickerAdapter(viewModel::toggle)
        binding.list.adapter = adapter
        binding.search.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnConfirm.setOnClickListener {
            val apps = viewModel.selectedApps()
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                bundleOf(PACKAGES to ArrayList(apps.map { it.packageName }), LABELS to ArrayList(apps.map { it.label })),
            )
            dismiss()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.rows.collect { adapter.submitList(it) } }
                launch {
                    viewModel.selectedCount.collect { count ->
                        binding.btnConfirm.isEnabled = count > 0
                        binding.btnConfirm.text = if (count > 0) {
                            resources.getQuantityString(R.plurals.sheet_apps_confirm_count, count, count)
                        } else {
                            getString(R.string.sheet_apps_confirm)
                        }
                    }
                }
            }
        }
        return binding.root
    }

    companion object {
        private const val REQUEST_KEY = "link_apps"
        private const val PACKAGES = "packages"
        private const val LABELS = "labels"
        private const val ISSUE_LABEL = "issue_label"
        private const val TAG = "apps_sheet"

        /** [excluded]: packages already linked, not offered; [issueLabel]: the issue, in the subtitle. */
        fun show(activity: FragmentActivity, issueLabel: String, excluded: List<String>) {
            if (activity.supportFragmentManager.findFragmentByTag(TAG) != null) return
            AppsSheet().apply {
                arguments = bundleOf(ISSUE_LABEL to issueLabel, AppPickerArgs.EXCLUDED to ArrayList(excluded))
            }.show(activity.supportFragmentManager, TAG)
        }

        /** Call it in onCreate: the result also arrives after a rotation. */
        fun listen(activity: FragmentActivity, onLink: (List<LinkedApp>) -> Unit) {
            activity.supportFragmentManager.setFragmentResultListener(REQUEST_KEY, activity) { _, result ->
                val packages = result.getStringArrayList(PACKAGES).orEmpty()
                val labels = result.getStringArrayList(LABELS).orEmpty()
                onLink(packages.zip(labels) { pkg, label -> LinkedApp(pkg, label) })
            }
        }
    }
}
