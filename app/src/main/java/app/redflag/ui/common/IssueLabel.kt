package app.redflag.ui.common

import android.content.Context
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import com.google.android.material.color.MaterialColors

/** An issue label with its parenthesis (the detail, e.g. "(Fear Of Missing Out)") in the secondary text color. */
fun styledIssueLabel(context: Context, label: String): CharSequence {
    val start = label.indexOf('(')
    if (start <= 0) return label
    val secondary = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
    return SpannableString(label).apply { setSpan(ForegroundColorSpan(secondary), start, label.length, 0) }
}
