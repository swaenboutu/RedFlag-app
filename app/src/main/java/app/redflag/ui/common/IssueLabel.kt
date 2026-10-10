package app.redflag.ui.common

import android.content.Context
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import com.google.android.material.color.MaterialColors

/** An issue's title followed by its explanation, when it has one ("Addictive design (dark patterns…)"), in the secondary text color. */
fun styledIssueLabel(context: Context, title: String, description: String?): CharSequence {
    if (description.isNullOrBlank()) return title
    val text = "$title ($description)"
    val secondary = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
    return SpannableString(text).apply { setSpan(ForegroundColorSpan(secondary), title.length + 1, text.length, 0) }
}
