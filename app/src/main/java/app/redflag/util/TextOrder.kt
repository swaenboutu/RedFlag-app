package app.redflag.util

import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/**
 * Ordre alphabétique selon la langue du téléphone : « Téléphone » se range avec les T, pas après « TMobile »
 * (le simple ordre des caractères placerait tout accent après le « z »). La langue est relue à chaque appel.
 */
fun alphabetical(locale: Locale = Locale.getDefault()): Comparator<String> {
    val collator = Collator.getInstance(locale)
    return Comparator { a, b -> collator.compare(a, b) }
}

/** Le texte sans accents ni majuscules, pour comparer ce qu'on tape à ce qui est affiché : « Téléphone » → « telephone ». */
fun String.foldForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

/** Vrai si ce texte contient [query], sans tenir compte des accents ni de la casse ; une recherche vide trouve tout. */
fun String.matchesSearch(query: String): Boolean = query.isBlank() || foldForSearch().contains(query.trim().foldForSearch())
