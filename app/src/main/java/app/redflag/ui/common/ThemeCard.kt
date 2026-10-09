package app.redflag.ui.common

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import com.google.android.material.color.MaterialColors
import app.redflag.R
import app.redflag.databinding.ItemManagerThemeBinding

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
    @androidx.annotation.DrawableRes icon: Int? = null,
) {
    themeIcon.visibility = if (icon == null) View.GONE else View.VISIBLE
    if (icon != null) {
        themeIcon.setImageResource(icon)
        themeIcon.imageTintList = ColorStateList.valueOf(MaterialColors.getColor(root, com.google.android.material.R.attr.colorOnSurface))
    }
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

/** « 4 problématiques » : le début de tous les sous-titres de thème. */
fun problemsSubtitle(context: Context, problemCount: Int): String =
    context.resources.getQuantityString(R.plurals.theme_problems, problemCount, problemCount)

/** « 4 problématiques, 2 cochées » ; le nombre de cochées n'est ajouté que s'il n'est pas nul. */
fun checkedSubtitle(context: Context, problemCount: Int, checkedCount: Int): String {
    val problems = problemsSubtitle(context, problemCount)
    if (checkedCount == 0) return problems
    return "$problems, ${context.resources.getQuantityString(R.plurals.theme_checked, checkedCount, checkedCount)}"
}
