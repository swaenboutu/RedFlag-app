package fr.conscience.numerique.ui.common

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.conscience.numerique.R
import fr.conscience.numerique.databinding.ItemProblemPillBinding

/** Pastilles de l'écran d'interruption : arrondi complet sur une ligne, coins de 16 dp sur plusieurs. */
class ProblemPillAdapter : ListAdapter<String, ProblemPillAdapter.ViewHolder>(Diff) {

    class ViewHolder(val binding: ItemProblemPillBinding) : RecyclerView.ViewHolder(binding.root) {
        private val background = GradientDrawable().apply {
            setColor(ContextCompat.getColor(binding.root.context, R.color.interstitial_accent))
        }

        init {
            binding.pill.background = background
            binding.pill.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                val density = view.resources.displayMetrics.density
                val singleLine = (view as TextView).lineCount <= 1
                background.cornerRadius = if (singleLine) view.height / 2f else MULTI_LINE_RADIUS_DP * density
            }
        }
    }

    fun submit(newLabels: List<String>) = submitList(newLabels)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemProblemPillBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.pill.text = getItem(position)
    }

    private object Diff : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(old: String, new: String) = old == new
        override fun areContentsTheSame(old: String, new: String) = old == new
    }

    private companion object {
        const val MULTI_LINE_RADIUS_DP = 16
    }
}
