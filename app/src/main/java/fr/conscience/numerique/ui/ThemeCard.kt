package fr.conscience.numerique.ui

import android.content.res.ColorStateList
import android.view.View
import com.google.android.material.color.MaterialColors
import fr.conscience.numerique.databinding.ItemManagerThemeBinding

/**
 * En-tête de carte repliable, commun à « Vos problématiques » et au détail d'une application.
 * Carte ouverte : angles du bas droits pour se raccorder aux lignes ; sinon carte complète.
 */
fun ItemManagerThemeBinding.bindThemeCard(
    title: String,
    subtitle: String,
    expanded: Boolean,
    hasContent: Boolean,
    onClick: () -> Unit,
) {
    themeTitle.text = title
    themeSubtitle.text = subtitle

    themeChevron.rotation = if (expanded) 180f else 0f
    themeChevron.imageTintList = ColorStateList.valueOf(
        MaterialColors.getColor(
            root,
            if (expanded) com.google.android.material.R.attr.colorOnSurface else com.google.android.material.R.attr.colorOnSurfaceVariant,
        ),
    )

    val open = expanded && hasContent
    themeDivider.visibility = if (open) View.VISIBLE else View.GONE
    root.isClickable = hasContent
    root.setOnClickListener { if (hasContent) onClick() }
    root.applyCard(roundTop = true, roundBottom = !open, gapAfterDp = if (open) 0 else CARD_GAP_DP)
}
