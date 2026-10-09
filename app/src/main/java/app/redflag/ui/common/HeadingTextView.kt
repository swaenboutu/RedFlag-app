package app.redflag.ui.common

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.ViewCompat

/**
 * Texte annoncé comme « titre » par TalkBack. L'attribut XML `accessibilityHeading` n'existe qu'à partir
 * d'Android 9 ; ViewCompat fait la même chose dès Android 8 (minSdk 26).
 */
class HeadingTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle,
) : AppCompatTextView(context, attrs, defStyleAttr) {
    init {
        ViewCompat.setAccessibilityHeading(this, true)
    }
}
