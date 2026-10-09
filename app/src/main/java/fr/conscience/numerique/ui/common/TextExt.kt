package fr.conscience.numerique.ui.common

import android.graphics.Paint
import android.widget.TextView

/** Underlines the text: used for the quiet links under the main buttons ("Skip this step"). */
fun TextView.underline() {
    paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
}
