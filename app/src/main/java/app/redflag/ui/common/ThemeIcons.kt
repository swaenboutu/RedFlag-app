package app.redflag.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.redflag.R

/** The outline icon of a theme card, from the resource id of its title; null when the card has none. */
@DrawableRes
fun themeIcon(@StringRes title: Int): Int? = when (title) {
    R.string.category_mental -> R.drawable.ic_theme_mental
    R.string.category_social -> R.drawable.ic_theme_social
    R.string.category_environment -> R.drawable.ic_theme_environment
    R.string.category_exploitation -> R.drawable.ic_theme_exploitation
    R.string.category_privacy -> R.drawable.ic_theme_privacy
    R.string.category_economic -> R.drawable.ic_theme_economic
    R.string.category_political -> R.drawable.ic_theme_political
    R.string.category_favorites -> R.drawable.ic_star_outline
    R.string.category_linked -> R.drawable.ic_nav_apps
    R.string.category_custom -> R.drawable.ic_edit
    else -> null
}
