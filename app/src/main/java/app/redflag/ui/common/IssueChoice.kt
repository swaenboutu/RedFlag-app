package app.redflag.ui.common

import android.view.View
import app.redflag.data.ProblemRef
import app.redflag.data.displayDescription
import app.redflag.data.displayLabel
import app.redflag.databinding.ItemDetailChoiceBinding

/**
 * One issue to tick inside a theme card (detail of an app, issues of the welcome tour): its title and explanation, its box, the
 * divider and the rounded bottom of the last row. [override]: the title the user gave it, if any.
 *
 * The box flips by itself when touched: it is put back to the real state at once, the database update then redraws it (a choice
 * refused, for example over the limit of the welcome tour, changes nothing).
 */
fun ItemDetailChoiceBinding.bindIssueChoice(
    ref: ProblemRef,
    override: String?,
    checked: Boolean,
    first: Boolean,
    last: Boolean,
    onToggle: (ProblemRef, Boolean) -> Unit,
) {
    val context = root.context
    choice.text = styledIssueLabel(context, ref.displayLabel(context, override), ref.displayDescription(context))
    choice.isChecked = checked
    choice.setOnClickListener {
        choice.isChecked = checked
        onToggle(ref, checked)
    }
    choiceDivider.visibility = if (first) View.GONE else View.VISIBLE
    root.applyCard(roundTop = false, roundBottom = last, gapAfterDp = if (last) CARD_GAP_DP else 0)
}
