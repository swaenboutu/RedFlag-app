package app.redflag.ui.common

import android.graphics.Paint
import android.widget.TextView
import androidx.core.text.HtmlCompat

/** Underlines the text: used for the quiet links under the main buttons ("Skip this step"). */
fun TextView.underline() {
    paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
}

/** Sets a string resource whose (escaped) HTML tags, such as <b>, are applied; [args] fill its placeholders. */
fun TextView.setHtml(resId: Int, vararg args: Any) {
    text = HtmlCompat.fromHtml(context.getString(resId, *args), HtmlCompat.FROM_HTML_MODE_LEGACY)
}
